# PowerShell script to test Enhanced Document Filling Service

Write-Host "===== Enhanced Document Filling Service Test =====" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api/v1"

# Test 1: Fill Auto Policy Document
Write-Host "Test 1: Fill Auto Policy Document" -ForegroundColor Yellow
$autoRequest = @{
    customerId = "CUST-001"
    documentType = "policy"
    insuranceType = "auto"
    customerData = @{
        name = "Zhang Wei"
        age = 30
        address = "123 Chaoyang Road, Beijing"
        email = "zhangwei@example.com"
        phone = "+86-13812345678"
        occupation = "Engineer"
        gender = "male"
        dateOfBirth = "1994-05-15"
    }
    policyData = @{
        policyNumber = "POL-2024-AUTO-001"
        policyType = "Comprehensive Auto Insurance"
        premiumAmount = 1560.0
        currency = "USD"
        effectiveDate = "2024-10-01"
        expiryDate = "2025-10-01"
        coverageAmount = "50000"
        deductible = "500"
    }
    outputFormat = "FILE_PATH"
    encryptOutput = $true
    requireSignature = $false
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/document/fill" `
        -Method Post `
        -ContentType "application/json" `
        -Body $autoRequest

    Write-Host "✓ Document filled successfully" -ForegroundColor Green
    Write-Host "  Document ID: $($response.documentId)"
    Write-Host "  Status: $($response.status)" -ForegroundColor Green
    Write-Host "  File Path: $($response.filePath)"
    Write-Host "  Fields Filled: $($response.fieldsFilled)"
    Write-Host "  Page Count: $($response.pageCount)"
    Write-Host "  File Size: $($response.fileSize) bytes"
    Write-Host "  Encrypted: $($response.encrypted)"
    if ($response.encryptedFilePath) {
        Write-Host "  Encrypted File: $($response.encryptedFilePath)"
    }
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 2: Fill Document with Base64 Output
Write-Host "Test 2: Fill Document with Base64 Output" -ForegroundColor Yellow
$base64Request = @{
    customerId = "CUST-002"
    documentType = "application"
    insuranceType = "home"
    customerData = @{
        name = "Li Ming"
        age = 35
        address = "456 Pudong Ave, Shanghai"
        email = "liming@example.com"
        phone = "+86-13987654321"
    }
    outputFormat = "BASE64"
    encryptOutput = $false
    requireSignature = $false
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/document/fill" `
        -Method Post `
        -ContentType "application/json" `
        -Body $base64Request

    Write-Host "✓ Document filled with Base64 output" -ForegroundColor Green
    Write-Host "  Document ID: $($response.documentId)"
    Write-Host "  Status: $($response.status)"
    Write-Host "  Base64 Length: $($response.base64Content.Length) chars"
    Write-Host "  Fields Filled: $($response.fieldsFilled)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 3: Fill Document with Both Output Formats
Write-Host "Test 3: Fill Document with Both Outputs" -ForegroundColor Yellow
$bothRequest = @{
    customerId = "CUST-003"
    documentType = "quote"
    insuranceType = "health"
    customerData = @{
        name = "Wang Fang"
        age = 40
        address = "789 Tianhe Road, Guangzhou"
        email = "wangfang@example.com"
        phone = "+86-13698745632"
    }
    policyData = @{
        policyNumber = "QUOTE-2024-HLT-001"
        premiumAmount = 850.0
        currency = "USD"
    }
    outputFormat = "BOTH"
    encryptOutput = $true
    requireSignature = $false
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/document/fill" `
        -Method Post `
        -ContentType "application/json" `
        -Body $bothRequest

    Write-Host "✓ Document filled with both outputs" -ForegroundColor Green
    Write-Host "  Document ID: $($response.documentId)"
    Write-Host "  File Path: $($response.filePath)"
    Write-Host "  Base64 Available: $(if($response.base64Content){'Yes'}else{'No'})"
    Write-Host "  Encrypted: $($response.encrypted)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 4: Fill Document with Electronic Signature (Mock)
Write-Host "Test 4: Fill Document with Electronic Signature" -ForegroundColor Yellow
$signatureRequest = @{
    customerId = "CUST-004"
    documentType = "policy"
    insuranceType = "life"
    customerData = @{
        name = "Chen Yu"
        age = 45
        address = "321 Nanshan Blvd, Shenzhen"
        email = "chenyu@example.com"
        phone = "+86-13512348765"
    }
    policyData = @{
        policyNumber = "POL-2024-LIFE-001"
        policyType = "Term Life Insurance"
        premiumAmount = 2000.0
        currency = "USD"
    }
    outputFormat = "FILE_PATH"
    encryptOutput = $false
    requireSignature = $true
    signerEmail = "chenyu@example.com"
    signerName = "Chen Yu"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/document/fill" `
        -Method Post `
        -ContentType "application/json" `
        -Body $signatureRequest

    Write-Host "✓ Document filled and sent for signature" -ForegroundColor Green
    Write-Host "  Document ID: $($response.documentId)"
    Write-Host "  Status: $($response.status)" -ForegroundColor Yellow
    Write-Host "  Signature Status: $($response.signatureStatus)"
    if ($response.signatureRequestId) {
        Write-Host "  Signature Request ID: $($response.signatureRequestId)"
        Write-Host "  Signing URL: $($response.signatureUrl)"
    }
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 5: Async Document Filling
Write-Host "Test 5: Async Document Filling" -ForegroundColor Yellow
$asyncRequest = @{
    customerId = "CUST-005"
    documentType = "claim"
    insuranceType = "auto"
    customerData = @{
        name = "Liu Wei"
        age = 33
        address = "555 Wangjing Street, Beijing"
        email = "liuwei@example.com"
        phone = "+86-13823456789"
    }
    outputFormat = "FILE_PATH"
    encryptOutput = $false
    requireSignature = $false
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/insurance/document/fill-async" `
        -Method Post `
        -ContentType "application/json" `
        -Body $asyncRequest

    Write-Host "✓ Async document filled successfully" -ForegroundColor Green
    Write-Host "  Document ID: $($response.documentId)"
    Write-Host "  Status: $($response.status)"
    Write-Host "  Fields Filled: $($response.fieldsFilled)"
    Write-Host ""
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host "===== All Tests Completed =====" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: If template files are not found, please create them in the ./templates directory" -ForegroundColor Yellow

