#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
source venv/bin/activate

# Install requirements in the virtual environment
echo "Installing requirements..."
./venv/bin/pip install -r requirements.txt

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found"
    echo "Please create a .env file with your DeepSeek API key"
    echo "Example: DEEPSEEK_API_KEY=your_api_key_here"
    exit 1
fi

# Run the Python script
echo "Generating tests..."
./venv/bin/python generate_tests.py

# Deactivate virtual environment when done
deactivate
