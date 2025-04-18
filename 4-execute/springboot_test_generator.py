#!/usr/bin/env python3
"""
Generate JUnit 5 test cases for a Spring Boot project using the OpenAI Python SDK v1+.
Automatically fixes numeric‑literal mismatches (e.g., int vs Double) in the
generated code to avoid common compilation errors.
"""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path
from typing import List

from dotenv import load_dotenv
from openai import OpenAI, APIConnectionError, AuthenticationError, OpenAIError

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
_CODE_BLOCK_RE = re.compile(r"```(?:java)?\s*([\s\S]*?)```", re.MULTILINE)
_INT_LITERAL_RE = re.compile(r"(?<=\()(\s*)(\d+)(\s*)(?=[,)])")


def _extract_package(content: str) -> str:
    match = re.search(r"^\s*package\s+([\w.]+);", content, re.MULTILINE)
    return match.group(1) if match else ""


def _clean_response(text: str) -> str:
    block = _CODE_BLOCK_RE.search(text)
    if block:
        return block.group(1).strip()
    return re.sub(r"```", "", text).strip()


def _method_returns_double(java_src: str) -> set[str]:
    """Return a set of method names that declare `Double` (boxed) or `double`."""
    patt = re.compile(r"(?:public|private|protected)\s+((?:static\s+)?)((?:Double|double))\s+(\w+)\s*\(")
    return {m.group(3) for m in patt.finditer(java_src)}


def _fix_double_literals(test_code: str, double_methods: set[str]) -> str:
    """Patch lines calling stubs/asserters for methods known to return Double."""
    if not double_methods:
        return test_code

    def fix_line(line: str) -> str:
        # thenReturn(<int>) or when(...).<method>(...).thenReturn(<int>)
        if any(f"{m}(" in line for m in double_methods):
            line = _INT_LITERAL_RE.sub(lambda m: f"{m.group(1)}{m.group(2)}.0{m.group(3)}", line)
        return line

    return "\n".join(fix_line(l) for l in test_code.splitlines())


# ---------------------------------------------------------------------------
# Core generator
# ---------------------------------------------------------------------------
class SpringBootTestGenerator:
    def __init__(self, project_root: Path, model: str = "gpt-4o-mini", temperature: float = 0.0):
        load_dotenv()
        api_key = os.getenv("OPENAI_API_KEY")
        if not api_key:
            raise EnvironmentError("OPENAI_API_KEY not found in environment or .env file")
        self.client = OpenAI(api_key=api_key)
        self.project_root = project_root
        self.model = model
        self.temperature = temperature

    # --------------------------------------------
    # Discovery helpers
    # --------------------------------------------
    def _java_source_files(self) -> List[Path]:
        src_main = self.project_root / "src" / "main" / "java"
        if not src_main.exists():
            raise FileNotFoundError(f"Directory {src_main} does not exist; is this a Spring Boot project?")
        return [p for p in src_main.rglob("*.java") if "src/test" not in str(p)]

    def _target_test_path(self, src_path: Path, package: str, class_name: str) -> Path:
        rel = Path(*package.split(".")) / f"{class_name}Test.java"
        return self.project_root / "src" / "test" / "java" / rel

    # --------------------------------------------
    # OpenAI call
    # --------------------------------------------
    def _ask_model(self, system_prompt: str, user_prompt: str) -> str:
        try:
            response = self.client.chat.completions.create(
                model=self.model,
                temperature=self.temperature,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
            )
            return response.choices[0].message.content
        except (APIConnectionError, AuthenticationError, OpenAIError) as exc:
            raise RuntimeError(f"OpenAI API call failed: {exc}") from exc

    # --------------------------------------------
    # Generation per file
    # --------------------------------------------
    def generate_test_for_file(self, java_file: Path) -> Path:
        source_code = java_file.read_text(encoding="utf‑8")
        package = _extract_package(source_code)
        class_name = java_file.stem

        system_prompt = (
            "You are an expert Java developer. Write a COMPLETE JUnit 5 test class "
            "for a Spring Boot application. Use Mockito for mocking and @SpringBootTest "
            "for integration when appropriate. Ensure numeric literals match the method "
            "return types (e.g., use 1.0 when the method returns Double). The resulting "
            "code must compile and tests must pass."
        )

        user_prompt = (
            f"Generate a test class named {class_name}Test for the code below. "
            "Respond **only** with the Java code within a single ```java block.\n\n"
            f"```java\n{source_code}\n```"
        )

        raw = self._ask_model(system_prompt, user_prompt)
        test_code = _clean_response(raw)

        # Post‑process for Double literals
        double_methods = _method_returns_double(source_code)
        test_code = _fix_double_literals(test_code, double_methods)

        target = self._target_test_path(java_file, package, class_name)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(test_code, encoding="utf‑8")
        print(f"✅ Generated {target.relative_to(self.project_root)}")
        return target

    # --------------------------------------------
    # Public API
    # --------------------------------------------
    def generate_all_tests(self) -> None:
        java_files = self._java_source_files()
        print(f"Found {len(java_files)} source file(s)")
        for f in java_files:
            try:
                self.generate_test_for_file(f)
            except Exception as exc:
                print(f"⚠️  Skipped {f} — {exc}")


# ---------------------------------------------------------------------------
# CLI entry‑point
# ---------------------------------------------------------------------------

def main() -> None:
    load_dotenv()
    default_dir = os.getenv("SPRINGBOOT_DIR", ".")

    parser = argparse.ArgumentParser(
        description="Generate JUnit 5 tests for a Spring Boot project using the OpenAI API."
    )
    parser.add_argument("-d", "--dir", default=default_dir, help="Path to the project root (defaults to SPRINGBOOT_DIR).")
    parser.add_argument("-m", "--model", default="gpt-4o-mini", help="OpenAI model name (default: gpt-4o-mini)")
    parser.add_argument("--temperature", type=float, default=0.0, help="Sampling temperature (default: 0.0 — deterministic)")
    args = parser.parse_args()

    project_root = Path(args.dir).resolve()
    if not project_root.exists():
        print(f"❌ Project directory '{project_root}' does not exist", file=sys.stderr)
        sys.exit(1)

    generator = SpringBootTestGenerator(project_root, model=args.model, temperature=args.temperature)
    generator.generate_all_tests()


if __name__ == "__main__":
    main()