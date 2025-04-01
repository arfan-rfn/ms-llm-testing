# Spring Boot Static Analysis Tool

This tool performs static analysis on Spring Boot applications to generate comprehensive API documentation and endpoint information.

## Features

1. Endpoint Analysis
   - Automatically discovers all REST endpoints in a Spring Boot application
   - Extracts HTTP methods, paths, parameters, and response types
   - Generates example request/response structures
   - Outputs results in a structured JSON format

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Building the Tool

```bash
mvn clean package
```

This will create a JAR file in the `target` directory.

## Usage

Run the tool by providing the path to your Spring Boot application's source code:

```bash
java -jar target/static-analysis-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/spring/boot/app
```

The tool will analyze the application and generate an `endpoints.json` file containing detailed information about all endpoints.

## Output Format

The generated `endpoints.json` file contains an array of endpoint information in the following format:

```json
[
  {
    "path": "/api/orders",
    "method": "POST",
    "description": "Create a new order",
    "parameters": [
      {
        "name": "order",
        "type": "Order",
        "location": "body",
        "required": true,
        "description": "Order details"
      }
    ],
    "response": {
      "type": "Order",
      "description": "Created order"
    }
  }
]
```

## Future Enhancements

1. Call Tree Analysis
   - Generate detailed call trees for each endpoint
   - Track method calls, conditionals, and external service calls
   - Support for generating combinatorial test cases