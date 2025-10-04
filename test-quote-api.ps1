# PowerShell script to test Enhanced Quoting Service API

Write-Host "===== Enhanced Quoting Service Test =====" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api/v1"

# Test 1: Auto Insurance Quote - Young Male Driver in City
Write-Host "Test 1: Auto Insurance - Young Male Driver in City" -ForegroundColor Yellow
$autoRequest = @{
    age = 24
    gender = "male"
    address = "Beijing, Chaoyang District"
    insuranceType = "auto"
    vehicleModel = "Tesla Model 3"
    vehicleYear = 2023
    name = "Zhang Wei"
    email = "zhangwei@example.com"
    isUrbanArea = $true
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/quote/generate" `
        -Method Post `
        -ContentType "application/json" `
        -Body $autoRequest

    Write-Host "✓ Quote Generated Successfully" -ForegroundColor Green
    Write-Host "  Quote ID: $($response.quoteId)"
    Write-Host "  Base Premium: `$$($response.breakdown.basePremium)"
    Write-Host "  Age Factor: `$$($response.breakdown.ageFactor)"
    Write-Host "  Gender Factor: `$$($response.breakdown.genderFactor)"
    Write-Host "  Location Factor: `$$($response.breakdown.locationFactor)"
    Write-Host "  Risk Factor: `$$($response.breakdown.riskFactor)"
    Write-Host "  Total Premium: `$$($response.totalPremium)" -ForegroundColor Cyan
    Write-Host "  Deductible: `$$($response.deductible)"
    Write-Host "  Coverage: $($response.coverage)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 2: Health Insurance - Smoker
Write-Host "Test 2: Health Insurance - Smoker" -ForegroundColor Yellow
$healthRequest = @{
    age = 35
    gender = "female"
    address = "Shanghai, Pudong District"
    insuranceType = "health"
    smoker = $true
    name = "Li Ming"
    email = "liming@example.com"
    isUrbanArea = $true
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/quote/generate" `
        -Method Post `
        -ContentType "application/json" `
        -Body $healthRequest

    Write-Host "✓ Quote Generated Successfully" -ForegroundColor Green
    Write-Host "  Quote ID: $($response.quoteId)"
    Write-Host "  Base Premium: `$$($response.breakdown.basePremium)"
    Write-Host "  Risk Factor (Smoker): `$$($response.breakdown.riskFactor)" -ForegroundColor Yellow
    Write-Host "  Total Premium: `$$($response.totalPremium)" -ForegroundColor Cyan
    Write-Host "  Notes: $($response.breakdown.calculationNotes)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 3: Auto Insurance - Sports Car
Write-Host "Test 3: Auto Insurance - Sports Car (High Risk)" -ForegroundColor Yellow
$sportsCarRequest = @{
    age = 28
    gender = "male"
    address = "Los Angeles, California"
    insuranceType = "auto"
    vehicleModel = "Ferrari F8 Sports Car"
    vehicleYear = 2024
    name = "John Doe"
    email = "john@example.com"
    isUrbanArea = $true
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/quote/generate" `
        -Method Post `
        -ContentType "application/json" `
        -Body $sportsCarRequest

    Write-Host "✓ Quote Generated Successfully" -ForegroundColor Green
    Write-Host "  Quote ID: $($response.quoteId)"
    Write-Host "  Base Premium: `$$($response.breakdown.basePremium)"
    Write-Host "  Risk Factor (Sports Car): `$$($response.breakdown.riskFactor)" -ForegroundColor Red
    Write-Host "  Total Premium: `$$($response.totalPremium)" -ForegroundColor Cyan
    Write-Host "  Deductible: `$$($response.deductible)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 4: Async Quote Generation
Write-Host "Test 4: Async Quote Generation" -ForegroundColor Yellow
$asyncRequest = @{
    age = 30
    gender = "female"
    address = "Guangzhou"
    insuranceType = "home"
    propertyType = "Apartment"
    propertyValue = 800000
    name = "Wang Fang"
    isUrbanArea = $true
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/quote/generate-async" `
        -Method Post `
        -ContentType "application/json" `
        -Body $asyncRequest

    Write-Host "✓ Async Quote Generated Successfully" -ForegroundColor Green
    Write-Host "  Quote ID: $($response.quoteId)"
    Write-Host "  Total Premium: `$$($response.totalPremium)" -ForegroundColor Cyan
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 5: Life Insurance - High Risk Occupation
Write-Host "Test 5: Life Insurance - High Risk Occupation" -ForegroundColor Yellow
$lifeRequest = @{
    age = 40
    gender = "male"
    address = "Shenzhen"
    insuranceType = "life"
    occupation = "Commercial Pilot"
    name = "Chen Yu"
    isUrbanArea = $true
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/quote/generate" `
        -Method Post `
        -ContentType "application/json" `
        -Body $lifeRequest

    Write-Host "✓ Quote Generated Successfully" -ForegroundColor Green
    Write-Host "  Quote ID: $($response.quoteId)"
    Write-Host "  Risk Factor (Pilot): `$$($response.breakdown.riskFactor)" -ForegroundColor Yellow
    Write-Host "  Total Premium: `$$($response.totalPremium)" -ForegroundColor Cyan
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 6: Optimal Age Discount
Write-Host "Test 6: Auto Insurance - Optimal Age Discount" -ForegroundColor Yellow
$optimalAgeRequest = @{
    age = 30
    gender = "female"
    address = "Rural Area, Hebei"
    insuranceType = "auto"
    vehicleModel = "Toyota Camry"
    isUrbanArea = $false
    name = "Liu Mei"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/quote/generate" `
        -Method Post `
        -ContentType "application/json" `
        -Body $optimalAgeRequest

    Write-Host "✓ Quote Generated Successfully" -ForegroundColor Green
    Write-Host "  Quote ID: $($response.quoteId)"
    Write-Host "  Age Factor (Discount): `$$($response.breakdown.ageFactor)" -ForegroundColor Green
    Write-Host "  Location Factor: `$$($response.breakdown.locationFactor)"
    Write-Host "  Total Premium: `$$($response.totalPremium)" -ForegroundColor Cyan
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host "===== All Tests Completed =====" -ForegroundColor Cyan

