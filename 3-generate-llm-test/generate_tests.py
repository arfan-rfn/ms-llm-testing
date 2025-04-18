import json
import os
import re
from pathlib import Path
from typing import Dict, List
import requests
from dotenv import load_dotenv

load_dotenv()

class CompatibleTestGenerator:
    def __init__(self):
        self.api_key = os.getenv('DEEPSEEK_API_KEY')
        self.api_url = "https://api.deepseek.com/v1/chat/completions"
        self.headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        self.max_retries = 3

    def load_endpoints(self, file_path: str) -> List[Dict]:
        with open(file_path, 'r') as f:
            return json.load(f)

    def generate_test_class_name(self, path: str, method: str) -> str:
        clean_path = re.sub(r'[^a-zA-Z0-9]', '', path.replace('/', '_'))
        return f"Api{clean_path.capitalize()}{method.capitalize()}Test"

    def build_prompt(self, endpoint: Dict, previous_attempt: str = None) -> str:
        base_prompt = f"""
Generate a JUnit test class for this Spring Boot endpoint that strictly follows these requirements:

=== ABSOLUTE REQUIREMENTS ===
1. Order objects MUST ONLY be created with no-arg constructor + setters:
   CORRECT:
   Order order = new Order();
   order.setId(1L);
   order.setCustomerName("test");

   WRONG (NEVER USE THESE):
   - Order order = new Order(1L, "test");  // BANNED
   - Order order = Order.builder().id(1L).build();  // BANNED
   - Any other constructor or builder pattern  // BANNED

2. Service layer mocking MUST ONLY use these exact methods:
   - Optional<Order> findOrderById(long id)
   - List<Order> findOrdersByCustomer(String name)
   - Order saveOrder(Order order)
   - double getTotalRevenue()

=== TEST STRUCTURE ===
1. Use @SpringBootTest with MockMvc
2. Include:
   - @Autowired MockMvc
   - @MockBean OrderService
3. Test both success and error cases
4. Use proper assertions (status(), jsonPath())

=== ENDPOINT DETAILS ===
Path: {endpoint['path']}
Method: {endpoint['method']}
Parameters: {json.dumps(endpoint.get('parameters', []), indent=2)}
Response: {json.dumps(endpoint['response'], indent=2)}
"""

        if previous_attempt:
            base_prompt += f"""

=== PREVIOUS ATTEMPT ERRORS ===
Your previous attempt contained these errors:
{previous_attempt}

Please carefully fix these issues in your new attempt.
"""

        base_prompt += """

=== EXAMPLE TEST ===
@SpringBootTest
@AutoConfigureMockMvc
public class ExampleTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private OrderService orderService;

    @Test
    void shouldReturnOrder() throws Exception {
        // CORRECT Order creation
        Order order = new Order();
        order.setId(1L);
        order.setCustomerName("test");
        order.setAmount(100.0);

        when(orderService.findOrderById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }
}

=== OUTPUT REQUIREMENTS ===
Return ONLY the complete Java code with:
1. Package declaration: package com.example.orderservice;
2. All required imports
3. Full test class implementation
4. ABSOLUTELY NO constructor usage other than new Order()
5. NO explanations or comments outside the code
"""

        return base_prompt

    def generate_test_case(self, endpoint: Dict, previous_attempt: str = None) -> str:
        prompt = self.build_prompt(endpoint, previous_attempt)
        payload = {
            "model": "deepseek-coder",
            "messages": [{
                "role": "system",
                "content": "You are a Java testing expert that strictly follows requirements."
            }, {
                "role": "user",
                "content": prompt
            }],
            "temperature": 0.1,  # Very low for maximum consistency
            "max_tokens": 3000
        }

        response = requests.post(self.api_url, headers=self.headers, json=payload)
        response.raise_for_status()
        return response.json()['choices'][0]['message']['content']

    def validate_test_case(self, test_code: str) -> Dict:
        results = {
            "is_valid": True,
            "errors": [],
            "problematic_lines": []
        }

        # Required patterns
        requirements = [
            (r"@SpringBootTest", "Missing @SpringBootTest annotation"),
            (r"@Autowired\s+private\s+MockMvc", "Missing MockMvc autowired field"),
            (r"@MockBean\s+private\s+OrderService", "Missing OrderService mock"),
            (r"Order\s+\w+\s*=\s*new\s+Order\(\s*\)", "Order not created with no-arg constructor"),
            (r"\.set[A-Z]\w*\(", "Missing property setters"),
            (r"when\(orderService\.(findOrderById|findOrdersByCustomer|saveOrder|getTotalRevenue)\(", "Incorrect service method mocking"),
        ]

        # Forbidden patterns
        prohibitions = [
            (r"new\s+Order\s*\([^)]+\)", "Used Order constructor with arguments"),
            (r"Order\s*\([^)]*\)", "Used Order constructor (any form)"),
            (r"CustomerNotFoundException", "Used forbidden exception"),
            (r"orderService\.(getOrderById|getOrdersByCustomerName|calculateTotalRevenue)", "Used wrong service method names"),
        ]

        # Check requirements
        for pattern, message in requirements:
            if not re.search(pattern, test_code):
                results["is_valid"] = False
                results["errors"].append(f"Missing: {message}")

        # Check prohibitions
        for pattern, message in prohibitions:
            matches = re.finditer(pattern, test_code)
            for match in matches:
                results["is_valid"] = False
                results["errors"].append(f"Prohibited: {message}")
                results["problematic_lines"].append(match.group(0))

        # Remove duplicate errors
        results["errors"] = list(dict.fromkeys(results["errors"]))
        results["problematic_lines"] = list(dict.fromkeys(results["problematic_lines"])[:3])

        return results

    def fix_common_issues(self, test_code: str) -> str:
        """Attempt to automatically fix common issues"""
        # Fix constructor usage
        test_code = re.sub(
            r"new\s+Order\s*\([^)]*\)",
            "new Order()",
            test_code
        )

        # Ensure proper service method names
        test_code = re.sub(
            r"orderService\.getOrderById\(",
            "orderService.findOrderById(",
            test_code
        )

        return test_code

    def save_test_file(self, test_code: str, endpoint: Dict):
        class_name = self.generate_test_class_name(endpoint['path'], endpoint['method'])
        package_path = "src/test/java/com/example/orderservice"
        test_dir = Path(package_path)
        test_dir.mkdir(parents=True, exist_ok=True)

        file_path = test_dir / f"{class_name}.java"

        # Ensure package declaration exists
        if not test_code.strip().startswith("package"):
            test_code = f"package com.example.orderservice;\n\n{test_code}"

        with open(file_path, 'w') as f:
            f.write(test_code)
        print(f"✅ Successfully saved valid test: {file_path}")

    def process_endpoint(self, endpoint: Dict):
        print(f"\n🔧 Processing {endpoint['method']} {endpoint['path']}")

        test_code = None
        validation_results = None

        for attempt in range(self.max_retries):
            try:
                print(f"Attempt {attempt + 1}/{self.max_retries}")

                previous_errors = "\n".join(validation_results["errors"]) if validation_results else None
                test_code = self.generate_test_case(endpoint, previous_errors)

                # Apply automatic fixes
                test_code = self.fix_common_issues(test_code)

                validation_results = self.validate_test_case(test_code)

                if validation_results["is_valid"]:
                    self.save_test_file(test_code, endpoint)
                    return
                else:
                    print("❌ Validation failed:")
                    for error in validation_results["errors"]:
                        print(f" - {error}")
                    if validation_results["problematic_lines"]:
                        print("\nProblematic code:")
                        for line in validation_results["problematic_lines"]:
                            print(f" - {line}")

            except Exception as e:
                print(f"⚠️ Attempt {attempt + 1} failed: {str(e)}")
                if attempt == self.max_retries - 1:
                    raise

        # If all retries failed
        print(f"❌ Failed to generate valid test after {self.max_retries} attempts")

        # Save the best attempt for debugging
        debug_dir = Path("debug_failed_tests")
        debug_dir.mkdir(exist_ok=True)
        debug_file = debug_dir / f"failed_{self.generate_test_class_name(endpoint['path'], endpoint['method'])}.java"

        with open(debug_file, 'w') as f:
            f.write(test_code)
        print(f"💾 Saved failed attempt to: {debug_file}")

    def cleanup_old_tests(self):
        test_dir = Path("src/test/java/com/example/orderservice")
        for file in test_dir.glob("Api*.java"):
            try:
                file.unlink()
                print(f"🗑️ Deleted old test: {file}")
            except Exception as e:
                print(f"⚠️ Error deleting {file}: {str(e)}")

    def process_endpoints(self, endpoints_file: str):
        self.cleanup_old_tests()
        endpoints = self.load_endpoints(endpoints_file)

        for endpoint in endpoints:
            try:
                self.process_endpoint(endpoint)
            except Exception as e:
                print(f"⛔ Critical error processing {endpoint['path']}: {str(e)}")
                continue

if __name__ == "__main__":
    generator = CompatibleTestGenerator()
    generator.process_endpoints("../2-static-analysis/endpoints.json")