#!/bin/bash

# Check if path argument is provided
if [ $# -eq 0 ]; then
    echo "Error: Please provide the path to the Spring Boot application"
    echo "Usage: ./run.sh <path-to-spring-boot-app>"
    echo "Example: ./run.sh ../1-ms/src/main/java"
    exit 1
fi

# Build the project
mvn clean package -DskipTests

# Run the static analysis application with the provided path
java -jar target/static-analysis-1.0-SNAPSHOT-jar-with-dependencies.jar "$1"
