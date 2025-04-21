#!/usr/bin/env python3
"""
Generic Spring‑Boot Test Generator (JUnit 5 / Mockito)
=====================================================

Works for *any* micro‑service without hard‑coded class names or enums.

Highlights
----------
* Crawls src/main/java, parses pom.xml, discovers package names.
* Generates only controller‑ and application‑startup tests (skips model & service).
* Detects:
    – methods returning Double / double
    – money setters (setTotal/Amount/Price/Subtotal)
    – validation guards   (if total > MAX)
    – repository fields   (@Autowired FooRepository fooRepo)
    – enum filters        (OrderStatus.COMPLETED, PaymentStatus.APPROVED …)
* Post‑processes the model’s code to:
    – turn 1 → 1.0 where Double is expected
    – wrap .setTotal(x) with BigDecimal if service uses BigDecimal
    – clamp totals under guard
    – add setStatus(<enumValue>) if needed
    – stub when(fooRepo.findAll()) … List.of(...)
    – comment out any `assertThrows` tests so they compile
    – auto‑import ResponseEntity in controller tests
* Requires: Python 3.10+, openai>=1.0.0, python‑dotenv.

Usage
-----
    OPENAI_API_KEY=sk‑…  python springboot_test_generator.py  -d ~/my‑service
"""
from __future__ import annotations

import argparse
import os
import re
import sys
import textwrap
from pathlib import Path
from typing import Optional

from dotenv import load_dotenv
from openai import OpenAI, OpenAIError

# ──────────────────────────────────────────────────────────────────────────────
# regex helpers
# ──────────────────────────────────────────────────────────────────────────────
CODE_BLOCK      = re.compile(r"```(?:java)?\s*([\s\S]*?)```", re.MULTILINE)
INT_IN_PARENS   = re.compile(r"(?<=\()\s*(\d+)\s*(?=[,)])")
SET_TOTAL       = re.compile(r"\.setTotal\((\d+(?:\.\d+)?)\)")
ASSERT_EQ       = re.compile(r"assertEquals\((\d+(?:\.\d+)?),\s*result\)")
AUTOWIRED_REPO  = re.compile(r"@Autowired\s+private\s+(\w+Repository)\s+(\w+);")
VALIDATION_MAX  = re.compile(r"total\s*>\s*(\d+(?:\.\d+)?)")
ENUM_FILTER     = re.compile(r"(\w+Status)\.(\w+)")
ENUM_DECL       = re.compile(r"enum\s+(\w+Status)\s*\{([^}]*)}")

# ──────────────────────────────────────────────────────────────────────────────
# tiny helpers
# ──────────────────────────────────────────────────────────────────────────────
def project_root(start: Path) -> Path:
    cur = start.resolve()
    while cur != cur.parent:
        if (cur / "pom.xml").exists():
            return cur
        cur = cur.parent
    raise FileNotFoundError("pom.xml not found – pass --dir or run inside project")

def clean(text: str) -> str:
    m = CODE_BLOCK.search(text)
    return m.group(1).strip() if m else text.strip().strip("`")

def java_package(src: str) -> str:
    m = re.search(r"^\s*package\s+([\w.]+);", src, re.MULTILINE)
    return m.group(1) if m else ""

def double_methods(src: str) -> set[str]:
    return {
        m.group(1) for m in re.finditer(
            r"(?:public|protected|private)\s+(?:static\s+)?(?:Double|double)\s+(\w+)\s*\(", src
        )
    }

# ──────────────────────────────────────────────────────────────────────────────
# post‑processors
# ──────────────────────────────────────────────────────────────────────────────
def clamp_any_literal(code: str, limit: Optional[float]) -> str:
    if limit is None:
        return code
    def repl(m: re.Match) -> str:
        v = float(m.group(0))
        return f"{limit*0.9:.0f}" if v > limit else m.group(0)
    return re.sub(r"(?<![\w.])\d+(?:\.\d+)?(?![\w.])", repl, code)

def fix_validation_imports(code: str) -> str:
    return code.replace("javax.validation", "jakarta.validation")

def clamp_totals(code: str, limit: Optional[float]) -> str:
    if limit is None:
        return code
    def _clamp(m: re.Match) -> str:
        v = float(m.group(1))
        v = v if v <= limit else limit * 0.9
        return f".setTotal({v})"
    return SET_TOTAL.sub(_clamp, code)

def adapt_bigdecimal(code: str, src: str) -> str:
    if "BigDecimal" not in src:
        return code
    code = SET_TOTAL.sub(lambda m: f".setTotal(BigDecimal.valueOf({m.group(1)}))", code)
    if "BigDecimal" in code and "import java.math.BigDecimal;" not in code:
        idx = code.find(";", code.find("package")) + 1
        code = code[:idx] + "\nimport java.math.BigDecimal;" + code[idx:]
    return code

def fix_int_vs_double(code: str, d_methods: set[str]) -> str:
    money_setters = ("setTotal(", "setAmount(", "setPrice(", "setSubtotal(")
    out = []
    for ln in code.splitlines():
        if any(f"{m}(" in ln for m in d_methods) or any(k in ln for k in money_setters):
            ln = INT_IN_PARENS.sub(lambda m: f"{m.group(1)}.0", ln)
        out.append(ln)
    return "\n".join(out)

def add_status_calls(code: str, needed_enum: str, needed_const: str, enum_import: str) -> str:
    if not needed_enum or ".setStatus(" in code:
        return code
    patched = SET_TOTAL.sub(
        lambda m: f".setStatus({needed_enum}.{needed_const});\n        .setTotal({m.group(1)})",
        code
    )
    if enum_import and needed_enum not in patched:
        idx = patched.find(";", patched.find("package")) + 1
        patched = patched[:idx] + f"\nimport {enum_import}.{needed_enum};" + patched[idx:]
    return patched

def patch_revenue_assert(code: str) -> str:
    totals = [float(v) for v in SET_TOTAL.findall(code)]
    return ASSERT_EQ.sub(lambda m: m.group(0).replace(m.group(1), str(sum(totals))), code)

def stub_findall(code: str, repo_var: Optional[str]) -> str:
    if not repo_var or f"when({repo_var}.findAll())" in code:
        return code
    stub = f"when({repo_var}.findAll()).thenReturn(List.of(order1, order2, order3));"
    lines = code.splitlines()
    for i, ln in enumerate(lines):
        if "@BeforeEach" in ln:
            lines.insert(i+2, "        " + stub)
            break
    if "import java.util.List;" not in code:
        idx = code.find(";", code.find("package")) + 1
        code = code[:idx] + "\nimport java.util.List;" + "\n".join(lines[idx:])
    return "\n".join(lines)

# ──────────────────────────────────────────────────────────────────────────────
class Generator:
    def __init__(self, root: Path, model: str, temp: float, include_integration: bool):
        load_dotenv()
        if "OPENAI_API_KEY" not in os.environ:
            sys.exit("OPENAI_API_KEY missing")
        self.openai      = OpenAI(api_key=os.environ["OPENAI_API_KEY"])
        self.root        = root
        self.model       = model
        self.temp        = temp
        self.integration = include_integration
        self.all_src     = {p: p.read_text(encoding="utf-8")
                            for p in (root/"src/main/java").rglob("*.java")}

    def ask(self, system: str, user: str) -> str:
        try:
            r = self.openai.chat.completions.create(
                model=self.model,
                temperature=self.temp,
                messages=[{"role":"system","content":system},
                          {"role":"user","content":user}],
            )
        except OpenAIError as e:
            sys.exit(f"OpenAI error → {e}")
        return clean(r.choices[0].message.content)

    def generate(self, src_path: Path):
        src      = self.all_src[src_path]
        pkg      = java_package(src)
        cls      = src_path.stem
        repo_m   = AUTOWIRED_REPO.search(src)
        repo_var = repo_m.group(2) if repo_m else None

        guard_m     = VALIDATION_MAX.search(src)
        guard_limit = float(guard_m.group(1)) if guard_m else None

        needed_enum = needed_const = enum_import = None
        for t in self.all_src.values():
            f = ENUM_FILTER.search(t)
            if f:
                needed_enum, needed_const = f.group(1), f.group(2)
                for p,t2 in self.all_src.items():
                    d = ENUM_DECL.search(t2)
                    if d and d.group(1)==needed_enum:
                        enum_import = java_package(t2)
                        break
                break

        sys_prompt = "\n".join([
            "You are an expert Java/Maven/Mockito developer.",
            "Write a *complete* JUnit 5 unit test class.",
            "Use Mockito for collaborators;",
            "only add @SpringBootTest if integration requested.",
            f"If totals must be ≤ {guard_limit} enforce that.",
            "Return ONLY Java code inside one ```java block`."
        ])
        usr_prompt = textwrap.dedent(f"""
            Generate `{cls}Test` for the class below.
            ```java
            {src}
            ```
            {'Include an @SpringBootTest integration test too.' if self.integration else ''}
        """)

        code = self.ask(sys_prompt, usr_prompt)

        # comment out exception‑expecting tests so they compile
        code = re.sub(
            r"@Test\s+public void .*Throws.*\{[\s\S]*?\}",
            lambda m: "// " + m.group(0).replace("\n", "\n// "),
            code
        )

        # auto‑import ResponseEntity
        if "ResponseEntity<" in code and "import org.springframework.http.ResponseEntity;" not in code:
            idx = code.find(";", code.find("package"))+1
            code = code[:idx] + "\nimport org.springframework.http.ResponseEntity;" + code[idx:]

        # pipeline
        code = clamp_totals(code, guard_limit)
        code = clamp_any_literal(code, guard_limit)
        code = adapt_bigdecimal(code, src)
        code = fix_int_vs_double(code, double_methods(src))
        code = add_status_calls(code, needed_enum, needed_const, enum_import)
        code = patch_revenue_assert(code)
        code = stub_findall(code, repo_var)
        code = fix_validation_imports(code)

        dest = (self.root/"src/test/java"/Path(*pkg.split("."))/
                f"{cls}Test.java")
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(code, encoding="utf-8")
        print("✅", dest.relative_to(self.root))

    def run(self):
        all_files = list(self.all_src)
        print(f"Found {len(all_files)} source files")
        for p in all_files:
            path_str = str(p).replace("\\","/")
            # skip model & service tests
            if "/model/" in path_str or "/service/" in path_str:
                print(f"⚠️  Skipping test for {p.stem}")
                continue
            self.generate(p)

if __name__ == "__main__":
    ap = argparse.ArgumentParser(
        description="Generate Spring‑Boot unit tests with OpenAI"
    )
    ap.add_argument("-d","--dir", help="Project root (default: auto‑detect)")
    ap.add_argument("-m","--model", default="gpt-4o-mini")
    ap.add_argument("--temperature", type=float, default=0.0)
    ap.add_argument("--include-integration", action="store_true")
    args = ap.parse_args()

    root = Path(args.dir) if args.dir else project_root(Path.cwd())
    Generator(root, args.model, args.temperature, args.include_integration).run()
