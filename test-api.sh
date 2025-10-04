#!/bin/bash
# Shell script to test Insurance AI Agent API

echo "===== Insurance AI Agent API Test ====="
echo ""

BASE_URL="http://localhost:8080/api/v1"

# Test 1: Health Check
echo "Test 1: Health Check"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/insurance/health")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo "✓ Health check passed"
    echo "$body" | jq '.'
else
    echo "✗ Health check failed"
    echo "  Make sure the application is running on port 8080"
    exit 1
fi
echo ""

# Test 2: Process Auto Insurance Request
echo "Test 2: Process Auto Insurance Request"
curl -s -X POST "$BASE_URL/insurance/process" \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "age": 25,
      "vehicle": "Tesla Model 3",
      "address": "Beijing, Chaoyang District",
      "insuranceType": "auto",
      "name": "Zhang Wei",
      "email": "zhangwei@example.com",
      "phone": "+86-13812345678"
    }
  }' | jq '.'
echo ""

# Test 3: Process Life Insurance Request
echo "Test 3: Process Life Insurance Request"
curl -s -X POST "$BASE_URL/insurance/process" \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "age": 35,
      "address": "Shanghai, Pudong District",
      "insuranceType": "life",
      "name": "Li Ming",
      "occupation": "Engineer",
      "smoker": false,
      "email": "liming@example.com"
    }
  }' | jq '.'
echo ""

echo "===== All Tests Completed ====="

