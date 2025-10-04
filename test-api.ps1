# PowerShell script to test Insurance AI Agent API

Write-Host "===== Insurance AI Agent API Test =====" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api/v1"

# Test 1: Health Check
Write-Host "Test 1: Health Check" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/insurance/health" -Method Get
    Write-Host "✓ Health check passed" -ForegroundColor Green
    Write-Host "  Status: $($health.status)"
    Write-Host "  Service: $($health.service)"
    Write-Host ""
} catch {
    Write-Host "✗ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Make sure the application is running on port 8080" -ForegroundColor Yellow
    exit 1
}

# Test 2: Process Auto Insurance Request
Write-Host "Test 2: Process Auto Insurance Request" -ForegroundColor Yellow
$autoRequest = @{
    customer = @{
        age = 25
        vehicle = "Tesla Model 3"
        address = "Beijing, Chaoyang District"
        insuranceType = "auto"
        name = "Zhang Wei"
        email = "zhangwei@example.com"
        phone = "+86-13812345678"
    }
} | ConvertTo-Json -Depth 10

try {
    $autoResponse = Invoke-RestMethod -Uri "$baseUrl/insurance/process" `
        -Method Post `
        -ContentType "application/json" `
        -Body $autoRequest `
        -SessionVariable session

    Write-Host "✓ Auto insurance processed successfully" -ForegroundColor Green
    Write-Host "  Quote: $($autoResponse.quote.premiumAmount) $($autoResponse.quote.currency)"
    Write-Host "  Decision: $($autoResponse.underwriting.decision)"
    Write-Host "  Risk Level: $($autoResponse.underwriting.riskLevel)"
    Write-Host "  Document ID: $($autoResponse.document.documentId)"
    Write-Host ""
} catch {
    Write-Host "✗ Auto insurance request failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 3: Process Life Insurance Request
Write-Host "Test 3: Process Life Insurance Request" -ForegroundColor Yellow
$lifeRequest = @{
    customer = @{
        age = 35
        address = "Shanghai, Pudong District"
        insuranceType = "life"
        name = "Li Ming"
        occupation = "Engineer"
        smoker = $false
        email = "liming@example.com"
    }
} | ConvertTo-Json -Depth 10

try {
    $lifeResponse = Invoke-RestMethod -Uri "$baseUrl/insurance/process" `
        -Method Post `
        -ContentType "application/json" `
        -Body $lifeRequest

    Write-Host "✓ Life insurance processed successfully" -ForegroundColor Green
    Write-Host "  Quote: $($lifeResponse.quote.premiumAmount) $($lifeResponse.quote.currency)"
    Write-Host "  Decision: $($lifeResponse.underwriting.decision)"
    Write-Host "  Risk Score: $($lifeResponse.underwriting.riskScore)"
    Write-Host ""
} catch {
    Write-Host "✗ Life insurance request failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 4: Invalid Request (should fail validation)
Write-Host "Test 4: Invalid Request (Validation Test)" -ForegroundColor Yellow
$invalidRequest = @{
    customer = @{
        age = 15  # Below minimum age
        address = "Test"
        insuranceType = "invalid"
    }
} | ConvertTo-Json -Depth 10

try {
    $invalidResponse = Invoke-RestMethod -Uri "$baseUrl/insurance/process" `
        -Method Post `
        -ContentType "application/json" `
        -Body $invalidRequest
    
    Write-Host "✗ Should have failed validation" -ForegroundColor Red
} catch {
    Write-Host "✓ Validation working correctly (expected error)" -ForegroundColor Green
    Write-Host ""
}

Write-Host "===== All Tests Completed =====" -ForegroundColor Cyan

