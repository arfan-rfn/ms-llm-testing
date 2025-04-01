# Spring Boot Endpoint Test Generator

This tool generates JUnit tests for Spring Boot endpoints using the DeepSeek API.

## Setup

1. Install the required Python packages:
```bash
pip install -r requirements.txt
```

2. Create a `.env` file in the root directory and add your DeepSeek API key:
```
DEEPSEEK_API_KEY=your_api_key_here
```

## Usage

Run the script to generate tests for all endpoints:
```bash
python generate_tests.py
```

The generated tests will be saved in the `generated_tests` directory. Each test file will be named based on the endpoint path and HTTP method.

## Features

- Generates JUnit tests for all endpoints defined in `endpoints.json`
- Uses MockMvc for testing
- Includes both success and error scenarios
- Follows Spring Boot testing best practices
- Generates appropriate test data
- Saves tests in a structured format