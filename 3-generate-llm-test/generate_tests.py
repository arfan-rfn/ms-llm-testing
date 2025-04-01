import json
import os
from dotenv import load_dotenv
import requests
from typing import Dict, List

# Load environment variables
load_dotenv()

class TestGenerator:
    def __init__(self):
        self.api_key = os.getenv('DEEPSEEK_API_KEY')
        if not self.api_key:
            raise ValueError("DEEPSEEK_API_KEY not found in environment variables")

        self.api_url = "https://api.deepseek.com/v1/chat/completions"
        self.headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

    def load_endpoints(self, file_path: str) -> List[Dict]:
        with open(file_path, 'r') as f:
            return json.load(f)

    def generate_test_prompt(self, endpoint: Dict) -> str:
        path = endpoint['path']
        method = endpoint['method']
        parameters = endpoint['parameters']
        response_type = endpoint['response']['type']

        prompt = f"""Generate a JUnit test for the following Spring Boot endpoint:
Path: {path}
Method: {method}
Parameters: {json.dumps(parameters, indent=2)}
Response Type: {response_type}

Please generate a complete JUnit test that:
1. Tests both successful and error scenarios
2. Uses appropriate test data
3. Follows Spring Boot testing best practices
4. Includes proper assertions
5. Uses MockMvc for testing

Generate only the test code without any explanations."""

        return prompt

    def generate_test(self, endpoint: Dict) -> str:
        prompt = self.generate_test_prompt(endpoint)

        payload = {
            "model": "deepseek-coder",
            "messages": [
                {"role": "system", "content": "You are a Java testing expert who generates high-quality JUnit tests for Spring Boot applications."},
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.7,
            "max_tokens": 2000
        }

        try:
            response = requests.post(self.api_url, headers=self.headers, json=payload)
            response.raise_for_status()
            return response.json()['choices'][0]['message']['content']
        except Exception as e:
            print(f"Error generating test for {endpoint['path']}: {str(e)}")
            return None

    def save_test(self, test_code: str, endpoint: Dict):
        if not test_code:
            return

        # Create test file name from endpoint path
        test_name = endpoint['path'].replace('/', '_').replace('{', '').replace('}', '')
        test_name = f"{test_name}_{endpoint['method'].lower()}_test.java"

        # Create test directory if it doesn't exist
        os.makedirs('generated_tests', exist_ok=True)

        # Save the test file
        file_path = os.path.join('generated_tests', test_name)
        with open(file_path, 'w') as f:
            f.write(test_code)
        print(f"Generated test saved to: {file_path}")

def main():
    generator = TestGenerator()
    endpoints = generator.load_endpoints('../2-static-analysis/endpoints.json')

    for endpoint in endpoints:
        print(f"Generating test for {endpoint['path']}...")
        test_code = generator.generate_test(endpoint)
        generator.save_test(test_code, endpoint)

if __name__ == "__main__":
    main()