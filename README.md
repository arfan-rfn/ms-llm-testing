
# Spring Boot Auto‑Test Generator

Automatically generates robust, compile‑clean JUnit 5 + Mockito test classes for any Spring Boot micro‑service using the OpenAI API.

---

## 📦 Prerequisites

- **Python 3.10+**  
- **Java 21+** (for your Spring Boot project)  
- **Maven** (to build & run tests)  
- **OpenAI Python SDK** v1+  
- **python‑dotenv**  

---

## 🔧 Installation & Configuration

1. **Clone** or copy this repository into your local machine, and goto "4-auto-test-generator" folder  
2. **Create & activate** a Python virtual environment:
   ```bash
   python3 -m venv .venv
   source .venv/bin/activate
   ```
3. **Install** required packages:
   ```bash
   pip install openai python-dotenv
   ```
4. **Create** a `.env` file at the project root containing:
   ```ini
   OPENAI_API_KEY=sk-…
   ```
5. **Configure** default behavior:
    - **Model**: `gpt-4o-mini`
    - **Temperature**: `0.0` (deterministic)
    - **Integration tests**: off by default; pass `--include-integration` to enable

_All of these can be overridden via CLI flags (see “Usage” below)._

---

## 🚀 Usage

1. **Navigate** to your Spring Boot project root (where `pom.xml` lives) or specify it:
   ```bash
   # Auto‑detect project root
   python springboot_test_generator.py

   # Explicitly point to your service
   python springboot_test_generator.py -d /path/to/my-service
   ```
2. **Optional flags**:
   ```bash
   -m, --model MODEL_NAME         # OpenAI model (default: gpt-4o-mini)
   --temperature FLOAT            # Sampling temperature (default: 0.0)
   --include-integration          # Also generate @SpringBootTest integration tests
   ```
3. **Generated tests** appear under your project’s:
   ```
   src/test/java/…
   ```
   mirroring your package layout.
4. **Build & run** with Maven:
   ```bash
   mvn clean test
   ```

---

## 🔍 How It Works

1. **Discovers** all `.java` files in `src/main/java`.
2. **Parses** each for:
    - Package name
    - Methods returning `double`/`Double`
    - Validation guards (e.g. `if total > MAX`)
    - Auto‑wired repositories
    - Enum filters
3. **Prompts** OpenAI to produce a complete `*Test.java` for each class.
4. **Post‑processes** the LLM output to:
    - Convert `1` → `1.0` for `Double` methods
    - Wrap `.setTotal(x)` in `BigDecimal.valueOf(x)` when needed
    - Clamp large literals under detected guards
    - Stub `when(repo.findAll()).thenReturn(List.of(...))`
    - Auto‑import missing types (e.g. `ResponseEntity`)
    - Simplify or comment out brittle assertions
5. **Writes** the cleaned, compile‑ready tests to `src/test/java/...`.

---

## ⚠️ Troubleshooting

- **Missing API key**  
  Ensure your `.env` contains `OPENAI_API_KEY`.
- **Compilation failures**
    1. Verify your main code compiles: `mvn clean compile`.
    2. Inspect generated tests for missing imports or constructor mismatches.
- **Deep domain logic tests failing**  
  The generator may skip or simplify certain validation tests. You can tweak or disable those filters in the script.

---

## 🗺️ Next Steps

- **Adjust** post‑processing regexes in `springboot_test_generator.py` for your code style.
- **Extend** support for other patterns or edge‑cases in your micro‑services.
- **Enable** full integration tests with `--include-integration`.

Enjoy your auto‑generated, compile‑ready tests! 🚀
```