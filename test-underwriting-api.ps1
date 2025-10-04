# PowerShell script to test Enhanced Underwriting Service

Write-Host "===== Enhanced Underwriting Service Test =====" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api/v1"

# Test 1: Excellent Credit - Should Approve
Write-Host "Test 1: Excellent Credit Profile - Should Approve" -ForegroundColor Yellow
$excellentRequest = @{
    customerId = "CUST-001"
    creditScore = 800
    claimsInLast3Years = 0
    drivingViolations = 0
    atFaultAccidents = 0
    dui = $false
    yearsLicensed = 15
    yearsWithPreviousInsurer = 5
    previousCancellation = $false
    previousDenial = $false
    insuranceType = "auto"
    age = 35
    address = "Beijing, Haidian District"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $excellentRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision ID: $($response.decisionId)"
    Write-Host "  Decision: $($response.decision)" -ForegroundColor $(if($response.decision -eq "APPROVE"){"Green"}else{"Red"})
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Risk Level: $($response.riskLevel)"
    Write-Host "  Decision Method: $($response.decisionMethod)"
    Write-Host "  Confidence: $($response.confidenceScore)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 2: Low Credit Score - Should Reject
Write-Host "Test 2: Low Credit Score - Should Reject" -ForegroundColor Yellow
$lowCreditRequest = @{
    customerId = "CUST-002"
    creditScore = 550
    claimsInLast3Years = 2
    insuranceType = "auto"
    age = 28
    address = "Shanghai"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $lowCreditRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)" -ForegroundColor $(if($response.decision -eq "REJECT"){"Red"}else{"Yellow"})
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Decision Reason: $($response.decisionReason)"
    Write-Host "  Risk Factors: $($response.riskFactors -join ', ')"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 3: DUI History - Should Reject
Write-Host "Test 3: DUI History - Should Reject" -ForegroundColor Yellow
$duiRequest = @{
    customerId = "CUST-003"
    creditScore = 680
    insuranceType = "auto"
    age = 30
    dui = $true
    drivingViolations = 1
    atFaultAccidents = 1
    address = "Guangzhou"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $duiRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)" -ForegroundColor Red
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Decision Reason: $($response.decisionReason)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 4: High Claims History - Should Approve with Extra Premium
Write-Host "Test 4: High Claims History - Extra Premium" -ForegroundColor Yellow
$highClaimsRequest = @{
    customerId = "CUST-004"
    creditScore = 700
    claimsInLast3Years = 4
    totalClaimAmount = 12000.0
    insuranceType = "home"
    age = 45
    hasSecuritySystem = $true
    inFloodZone = $false
    address = "Shenzhen"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $highClaimsRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)" -ForegroundColor Yellow
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Premium Multiplier: $($response.premiumMultiplier)"
    Write-Host "  Extra Premium: `$$($response.extraPremium)"
    Write-Host "  Conditions: $($response.conditions -join ', ')"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 5: Smoker Health Insurance - Risk Factor
Write-Host "Test 5: Smoker Health Insurance - Risk Factor" -ForegroundColor Yellow
$smokerRequest = @{
    customerId = "CUST-005"
    creditScore = 710
    insuranceType = "health"
    age = 40
    smoker = $true
    medicalConditions = @("Hypertension")
    address = "Beijing"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $smokerRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)"
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Premium Multiplier: $($response.premiumMultiplier)"
    Write-Host "  Risk Factors: $($response.riskFactors -join ', ')"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 6: Property in Flood Zone - Exclusion
Write-Host "Test 6: Property in Flood Zone - Exclusion" -ForegroundColor Yellow
$floodZoneRequest = @{
    customerId = "CUST-006"
    creditScore = 730
    insuranceType = "home"
    age = 50
    inFloodZone = $true
    hasSecuritySystem = $true
    propertyAge = 25
    address = "Houston, Texas"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $floodZoneRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)"
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Exclusions: $($response.exclusions -join ', ')" -ForegroundColor Yellow
    Write-Host "  Conditions: $($response.conditions -join ', ')"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 7: Async Assessment
Write-Host "Test 7: Async Underwriting Assessment" -ForegroundColor Yellow
$asyncRequest = @{
    customerId = "CUST-007"
    creditScore = 690
    claimsInLast3Years = 1
    insuranceType = "life"
    age = 38
    occupation = "Engineer"
    smoker = $false
    address = "Hangzhou"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess-async" `
        -Method Post `
        -ContentType "application/json" `
        -Body $asyncRequest

    Write-Host "✓ Async Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)"
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Terms: $($response.terms)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 8: Multiple Driving Violations
Write-Host "Test 8: Multiple Driving Violations - High Risk" -ForegroundColor Yellow
$violationsRequest = @{
    customerId = "CUST-008"
    creditScore = 670
    insuranceType = "auto"
    age = 26
    drivingViolations = 4
    atFaultAccidents = 2
    yearsLicensed = 8
    dui = $false
    address = "Los Angeles"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $violationsRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)"
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Risk Level: $($response.riskLevel)"
    Write-Host "  Premium Multiplier: $($response.premiumMultiplier)"
    Write-Host "  Conditions: $($response.conditions -join ', ')"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 9: Previous Cancellation - Referral
Write-Host "Test 9: Previous Cancellation - Should Refer" -ForegroundColor Yellow
$cancellationRequest = @{
    customerId = "CUST-009"
    creditScore = 680
    insuranceType = "auto"
    age = 35
    previousCancellation = $true
    claimsInLast3Years = 0
    address = "Chicago"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $cancellationRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)" -ForegroundColor Yellow
    Write-Host "  Referral Reason: $($response.referralReason)"
    Write-Host "  Manual Review Required: $($response.requiresManualReview)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 10: Young Driver Clean Record
Write-Host "Test 10: Young Driver with Clean Record" -ForegroundColor Yellow
$youngDriverRequest = @{
    customerId = "CUST-010"
    creditScore = 690
    insuranceType = "auto"
    age = 22
    drivingViolations = 0
    atFaultAccidents = 0
    dui = $false
    yearsLicensed = 4
    address = "Seattle"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/underwriting/assess" `
        -Method Post `
        -ContentType "application/json" `
        -Body $youngDriverRequest

    Write-Host "✓ Assessment Completed" -ForegroundColor Green
    Write-Host "  Decision: $($response.decision)"
    Write-Host "  Risk Score: $($response.riskScore)"
    Write-Host "  Risk Level: $($response.riskLevel)"
    Write-Host "  Positive Factors: $($response.positiveFactors -join ', ')"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host "===== All Tests Completed =====" -ForegroundColor Cyan

