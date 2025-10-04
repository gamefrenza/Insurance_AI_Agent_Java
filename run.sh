#!/bin/bash
# Shell script to run Insurance AI Agent

echo "===== Insurance AI Agent Startup ====="
echo ""

# Check if Java is installed
echo "Checking Java installation..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1)
    echo "✓ Java found: $java_version"
else
    echo "✗ Java not found. Please install Java 17 or higher."
    exit 1
fi
echo ""

# Check if Maven is installed
echo "Checking Maven installation..."
if command -v mvn &> /dev/null; then
    mvn_version=$(mvn -version 2>&1 | head -n 1)
    echo "✓ Maven found: $mvn_version"
else
    echo "✗ Maven not found. Please install Maven 3.6 or higher."
    exit 1
fi
echo ""

# Set environment variables if not already set
if [ -z "$OPENAI_API_KEY" ]; then
    echo "Warning: OPENAI_API_KEY not set. AI features will use fallback behavior."
    echo "To set it, run: export OPENAI_API_KEY='your-api-key-here'"
    echo ""
fi

if [ -z "$AES_SECRET_KEY" ]; then
    echo "Setting default AES_SECRET_KEY..."
    export AES_SECRET_KEY="MySecretKey12345MySecretKey12345"
    echo ""
fi

# Build and run the application
echo "Building and starting application..."
echo ""

mvn spring-boot:run

