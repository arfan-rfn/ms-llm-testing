import requests
import json
import os
import re
from typing import Dict, Optional

class SpringBootTestGenerator:
    def __init__(self, deepseek_api_key: str, springboot_project_path: str):
        self.deepseek_api_url = "https://api.deepseek.com/v1/chat/completions"
        self.api_key = deepseek_api_key
        self.project_path = springboot_project_path
        self.headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}"
        }

    def get_java_test_template(self, component_type: str, class_name: str, package_path: str) -> str:
        """Return a basic test template with proper structure"""
        template = f"""package {package_path};

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class {class_name}Test {{
    // Add your test cases here

    @Test
    public void testExample() {{
        // Test implementation
        assertTrue(true);
    }}
}}"""
        return template

    def analyze_project_structure(self) -> Dict:
        """Find all Spring components"""
        components = {
            "controllers": [],
            "services": [],
            "repositories": [],
            "models": []
        }

        java_path = os.path.join(self.project_path, "src", "main", "java")

        for root, dirs, files in os.walk(java_path):
            for file in files:
                if file.endswith(".java"):
                    file_path = os.path.join(root, file)
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                        if "@RestController" in content or "@Controller" in content:
                            components["controllers"].append(file_path)
                        elif "@Service" in content:
                            components["services"].append(file_path)
                        elif "@Repository" in content or "extends JpaRepository" in content:
                            components["repositories"].append(file_path)
                        elif "@Entity" in content or "@Data" in content:
                            components["models"].append(file_path)

        return components

    def generate_test_methods(self, component_type: str, file_path: str) -> Optional[str]:
        """Generate only test methods for the template"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                code_content = f.read()

            prompt = f"""
            Generate 3-5 JUnit 5 test methods for this Spring Boot {component_type}.
            Include:
            - Proper test method signatures
            - Assertions
            - Mocking where needed
            - Edge case testing

            Return ONLY the test methods (no class definition, no package declaration).
            Each test method should be properly annotated with @Test.

            Here is the class to test:
            {code_content}
            """

            payload = {
                "model": "deepseek-chat",
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.3,
                "max_tokens": 2000
            }

            response = requests.post(self.deepseek_api_url, headers=self.headers, json=payload)
            response.raise_for_status()
            return response.json()["choices"][0]["message"]["content"]

        except Exception as e:
            print(f"[ERROR] Failed to generate test methods: {str(e)}")
            return None

    def save_test_file(self, original_path: str, test_content: str) -> bool:
        """Save test file with validation"""
        test_path = original_path.replace("src/main/java", "src/test/java")
        test_filename = os.path.basename(test_path).replace(".java", "Test.java")
        test_path = os.path.join(os.path.dirname(test_path), test_filename)

        try:
            os.makedirs(os.path.dirname(test_path), exist_ok=True)
            with open(test_path, 'w', encoding='utf-8') as f:
                f.write(test_content)
            print(f"[SUCCESS] Created test: {test_path}")
            return True
        except Exception as e:
            print(f"[ERROR] Failed to save test: {str(e)}")
            return False

    def generate_all_tests(self) -> None:
        """Main test generation workflow"""
        print("\n=== Starting Test Generation ===")
        components = self.analyze_project_structure()

        for component_type, files in components.items():
            print(f"\nGenerating {len(files)} {component_type} tests...")
            for file in files:
                print(f"\nProcessing: {file}")

                # Get package and class name
                with open(file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    package_match = re.search(r'package\s+([\w\.]+);', content)
                    class_match = re.search(r'class\s+(\w+)', content)

                    if not package_match or not class_match:
                        print("[SKIPPED] Could not determine package or class name")
                        continue

                    package_path = package_match.group(1)
                    class_name = class_match.group(1)

                # Generate base template
                template = self.get_java_test_template(component_type, class_name, package_path)

                # Generate test methods
                test_methods = self.generate_test_methods(component_type, file)
                if not test_methods:
                    print("[WARNING] Using template with default test method")
                    test_methods = """
    @Test
    public void testExample() {
        // Default test implementation
        assertTrue(true);
    }"""

                # Insert test methods into template
                final_content = template.replace("// Add your test cases here", test_methods.strip())

                # Save the file
                self.save_test_file(file, final_content)

        print("\n=== Test Generation Complete ===")

if __name__ == "__main__":
    # Configuration
    DEEPSEEK_API_KEY = "sk-44423bad90f84b7f90a855bad04e6b17"
    SPRINGBOOT_PROJECT_PATH = r"C:\UofA\ms-llm-testing\1-ms"

    # Clean up old test files
    test_files = [
        r"src\test\java\com\example\orderservice\controller\OrderControllerTest.java",
        r"src\test\java\com\example\orderservice\service\OrderServiceTest.java"
    ]

    for test_file in test_files:
        full_path = os.path.join(SPRINGBOOT_PROJECT_PATH, test_file)
        if os.path.exists(full_path):
            os.remove(full_path)
            print(f"Removed old test file: {test_file}")

    # Run generator
    generator = SpringBootTestGenerator(DEEPSEEK_API_KEY, SPRINGBOOT_PROJECT_PATH)
    generator.generate_all_tests()