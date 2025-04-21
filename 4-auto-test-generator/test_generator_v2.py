"""
test_generator_v2.py
────────────────────
Creates JUnit 5 test stubs for every non‑test Java class in a Spring‑Boot
project, preserving package structure under src/test/java.

Prereqs
-------
pip install openai python-dotenv
Create a .env file (or hard‑code) with OPENAI_API_KEY.
"""

from __future__ import annotations

import os
import sys
from pathlib import Path
from typing import Iterable, List, Optional

from dotenv import load_dotenv
import openai

# ──────────────────────────────────────────────────────────────────────────────
#  CONFIGURATION
# ──────────────────────────────────────────────────────────────────────────────

PROJECT_ROOT = Path(r"C:\UofA\ms-llm-testing\1-ms").resolve()  # ← your root dir
TEST_ROOT    = PROJECT_ROOT / "src" / "test" / "java"

GPT_MODEL   = "gpt-4o-mini"    # or gpt-4o / gpt-4
MAX_TOKENS  = 2048
TEMPERATURE = 0.3

# ──────────────────────────────────────────────────────────────────────────────
#  OPENAI INITIALISATION
# ──────────────────────────────────────────────────────────────────────────────
load_dotenv()
openai.api_key = os.getenv("OPENAI_API_KEY")

if not openai.api_key:
    sys.exit("❌  OPENAI_API_KEY not found (env var or .env file).")

# ──────────────────────────────────────────────────────────────────────────────
#  HELPERS
# ──────────────────────────────────────────────────────────────────────────────
def iter_java_sources(root: Path) -> Iterable[Path]:
    """Yield every *.java file under *root* except anything already in /test/."""
    for path in root.rglob("*.java"):
        if "test" in (p.lower() for p in path.parts):
            continue
        yield path


def class_to_test_filename(java_path: Path) -> str:
    return java_path.stem + "Test.java"


def target_test_path(source_file: Path) -> Path:
    """
    Map a source file like
        .../src/main/java/com/example/Foo.java
    to
        TEST_ROOT/com/example/FooTest.java
    """
    try:
        # drop everything through src/main/java/
        idx = source_file.parts.index("java") + 1  # first segment *after* 'java'
    except ValueError:
        idx = 0
    pkg_parts = source_file.parts[idx:-1]          # package directories
    return TEST_ROOT.joinpath(*pkg_parts, class_to_test_filename(source_file))


SYSTEM_PROMPT = (
    "You are an expert Java developer.  Write production‑ready JUnit 5 tests "
    "for Spring‑Boot classes.  Use Mockito for dependencies and @SpringBootTest "
    "only when necessary.  Do not include back‑ticks or markdown fences."
)
USER_PROMPT_TMPL = (
    "Generate a complete JUnit 5 test class for the Spring‑Boot Java class "
    "below.  Keep the same package declaration, add imports, and include "
    "meaningful test methods with clear Arrange‑Act‑Assert comments.\n\n"
    "{code}"
)


def ask_openai(java_code: str) -> Optional[str]:
    """Return test code or None on error."""
    try:
        completion = openai.chat.completions.create(       # type: ignore[attr-defined]
            model=GPT_MODEL,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user",   "content": USER_PROMPT_TMPL.format(code=java_code)},
            ],
            max_tokens=MAX_TOKENS,
            temperature=TEMPERATURE,
        )
        return completion.choices[0].message.content
    except Exception as exc:
        print(f"❌  OpenAI error → {exc}")
        return None


def clean_backticks(code: str) -> str:
    return code.replace("```", "").replace("`", "")


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"✅  Generated: {path}")


# ──────────────────────────────────────────────────────────────────────────────
#  MAIN
# ──────────────────────────────────────────────────────────────────────────────
def main() -> None:
    print(f"📁  Scanning Java files in: {PROJECT_ROOT}")

    sources: List[Path] = list(iter_java_sources(PROJECT_ROOT))
    if not sources:
        print("❌  No Java classes found.")
        return

    for src in sources:
        java_code = src.read_text(encoding="utf-8")
        test_code_raw = ask_openai(java_code)
        if not test_code_raw:
            continue

        cleaned = clean_backticks(test_code_raw.strip())
        out_path = target_test_path
