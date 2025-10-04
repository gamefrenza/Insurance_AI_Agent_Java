# PowerShell script to test Integrated API

Write-Host "===== Testing Integrated Insurance API =====" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api/v1/insurance"
$apiKey = "your-api-key-here"  # Replace with your actual API key

# Test 1: Health Check
Write-Host "Test 1: Health Check" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
    Write-Host "✓ Health check successful" -ForegroundColor Green
    Write-Host "  Status: $($response.status)" -ForegroundColor White
    Write-Host "  Service: $($response.service)" -ForegroundColor White
    Write-Host ""
} catch {
    Write-Host "✗ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 2: Complete Insurance Workflow (Auto)
Write-Host "Test 2: Complete Auto Insurance Workflow" -ForegroundColor Yellow
$autoRequest = @{
    customerId = "TEST-AUTO-001"
    customerName = "John Doe"
    age = 30
    gender = "male"
    address = "123 Main St, New York"
    email = "john@example.com"
    phone = "+1-555-1234"
    insuranceType = "auto"
    vehicleModel = "Tesla Model 3"
    isSmoker = $false
    occupation = "Engineer"
    creditScore = 750
    claimsHistory = 0
    drivingRecord = "Clean"
    requireSignature = $false
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/process" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{"X-API-Key" = $apiKey} `
        -Body $autoRequest

    Write-Host "✓ Auto insurance processed successfully" -ForegroundColor Green
    Write-Host "  Customer ID: $($response.customerId)" -ForegroundColor White
    Write-Host "  Overall Status: $($response.overallStatus)" -ForegroundColor $(if($response.overallStatus -eq "SUCCESS"){"Green"}else{"Yellow"})
    Write-Host "  Premium: `$$($response.quote.totalPremium)" -ForegroundColor White
    Write-Host "  Underwriting Decision: $($response.underwriting.decision)" -ForegroundColor White
    Write-Host "  Risk Score: $($response.underwriting.riskScore)" -ForegroundColor White
    if ($response.document) {
        Write-Host "  Document ID: $($response.document.documentId)" -ForegroundColor White
        Write-Host "  Document Status: $($response.document.status)" -ForegroundColor White
    }
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 3: Life Insurance for Smoker
Write-Host "Test 3: Life Insurance for Smoker" -ForegroundColor Yellow
$lifeRequest = @{
    customerId = "TEST-LIFE-001"
    customerName = "Jane Smith"
    age = 45
    gender = "female"
    address = "456 Oak Ave, Los Angeles"
    email = "jane@example.com"
    phone = "+1-555-5678"
    insuranceType = "life"
    isSmoker = $true
    occupation = "Marketing Manager"
    creditScore = 720
    claimsHistory = 0
    requireSignature = $false
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/process" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{"X-API-Key" = $apiKey} `
        -Body $lifeRequest

    Write-Host "✓ Life insurance processed" -ForegroundColor Green
    Write-Host "  Premium: `$$($response.quote.totalPremium)" -ForegroundColor White
    Write-Host "  Decision: $($response.underwriting.decision)" -ForegroundColor White
    Write-Host "  Risk Factors: $($response.underwriting.riskFactors -join ', ')" -ForegroundColor White
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 4: High Risk Customer (Low Credit Score)
Write-Host "Test 4: High Risk Customer (Low Credit Score)" -ForegroundColor Yellow
$highRiskRequest = @{
    customerId = "TEST-RISK-001"
    customerName = "Bob Johnson"
    age = 28
    gender = "male"
    address = "789 Pine Rd, Miami"
    email = "bob@example.com"
    phone = "+1-555-9999"
    insuranceType = "auto"
    vehicleModel = "Ferrari F8"
    creditScore = 580
    claimsHistory = 3
    drivingRecord = "Multiple violations"
    requireSignature = $false
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/process" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{"X-API-Key" = $apiKey} `
        -Body $highRiskRequest

    Write-Host "✓ High risk customer processed" -ForegroundColor Yellow
    Write-Host "  Overall Status: $($response.overallStatus)" -ForegroundColor $(if($response.overallStatus -eq "REJECTED"){"Red"}else{"Yellow"})
    Write-Host "  Decision: $($response.underwriting.decision)" -ForegroundColor White
    Write-Host "  Risk Score: $($response.underwriting.riskScore)" -ForegroundColor White
    Write-Host "  Message: $($response.message)" -ForegroundColor White
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 5: Home Insurance
Write-Host "Test 5: Home Insurance" -ForegroundColor Yellow
$homeRequest = @{
    customerId = "TEST-HOME-001"
    customerName = "Alice Brown"
    age = 35
    gender = "female"
    address = "321 Elm St, Chicago"
    email = "alice@example.com"
    phone = "+1-555-2222"
    insuranceType = "home"
    propertyValue = 350000.0
    occupation = "Teacher"
    creditScore = 780
    claimsHistory = 0
    requireSignature = $true
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/process" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{"X-API-Key" = $apiKey} `
        -Body $homeRequest

    Write-Host "✓ Home insurance processed" -ForegroundColor Green
    Write-Host "  Premium: `$$($response.quote.totalPremium)" -ForegroundColor White
    Write-Host "  Decision: $($response.underwriting.decision)" -ForegroundColor White
    if ($response.document) {
        Write-Host "  Document ID: $($response.document.documentId)" -ForegroundColor White
        Write-Host "  Signature Required: Yes" -ForegroundColor Yellow
        if ($response.document.signatureUrl) {
            Write-Host "  Signature URL: $($response.document.signatureUrl)" -ForegroundColor White
        }
    }
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 6: Async Processing
Write-Host "Test 6: Async Processing" -ForegroundColor Yellow
$asyncRequest = @{
    customerId = "TEST-ASYNC-001"
    customerName = "Charlie Davis"
    age = 40
    gender = "male"
    address = "555 Maple Dr, Seattle"
    email = "charlie@example.com"
    insuranceType = "health"
    isSmoker = $false
    creditScore = 740
    claimsHistory = 1
    requireSignature = $false
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/process-async" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{"X-API-Key" = $apiKey} `
        -Body $asyncRequest

    Write-Host "✓ Async processing completed" -ForegroundColor Green
    Write-Host "  Customer ID: $($response.customerId)" -ForegroundColor White
    Write-Host "  Status: $($response.overallStatus)" -ForegroundColor White
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host "===== All Tests Completed =====" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: Make sure the application is running and API key is configured" -ForegroundColor Yellow

