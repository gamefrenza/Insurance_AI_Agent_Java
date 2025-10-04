# Enhanced Document Filling Service Documentation

## Overview

The Enhanced Document Filling Service automates insurance document generation using **Apache PDFBox** for PDF processing and **DocuSign API** for electronic signatures. It supports template-based filling, encryption, and multiple output formats.

## Features

### ✅ PDF Processing
- **Apache PDFBox Integration**: Parse and fill PDF forms
- **Multi-page Support**: Handle documents with any number of pages
- **Dynamic Field Mapping**: Automatic field detection and filling
- **Form Flattening**: Convert filled forms to non-editable format

### ✅ Electronic Signature
- **DocuSign Integration**: Send documents for e-signature
- **Async Processing**: Non-blocking signature requests
- **Status Tracking**: Monitor signature status
- **Mock Support**: Fallback for testing without API

### ✅ Output Formats
- **File Path**: Save to local filesystem
- **Base64**: Return encoded document
- **Both**: Get both file path and base64
- **Encrypted**: AES-256 encrypted storage

### ✅ Security & Compliance
- **AES Encryption**: Protect sensitive documents
- **Field Validation**: Hibernate Validator
- **Audit Logging**: Track all operations
- **GDPR Compliant**: Secure data handling

---

## Architecture

```
DocumentController
    ↓
EnhancedDocumentFillingService
    ↓
    ├──> PDF Processing (Apache PDFBox)
    ├──> Field Mapping & Filling
    ├──> Encryption (AES)
    └──> Electronic Signature (DocuSign)
```

---

## API Endpoints

### 1. Fill Document (Synchronous)

**Endpoint**: `POST /api/v1/insurance/document/fill`

**Request Body**:
```json
{
  "customerId": "CUST-2024-001",
  "documentType": "policy",
  "insuranceType": "auto",
  "customerData": {
    "name": "Zhang Wei",
    "age": 30,
    "address": "123 Chaoyang Road, Beijing",
    "email": "zhangwei@example.com",
    "phone": "+86-13812345678",
    "occupation": "Engineer",
    "gender": "male",
    "dateOfBirth": "1994-05-15",
    "additionalFields": {
      "driverLicense": "B1234567890"
    }
  },
  "policyData": {
    "policyNumber": "POL-2024-AUTO-001",
    "policyType": "Comprehensive Auto Insurance",
    "premiumAmount": 1560.0,
    "currency": "USD",
    "effectiveDate": "2024-10-01",
    "expiryDate": "2025-10-01",
    "coverageAmount": "50000",
    "deductible": "500"
  },
  "outputFormat": "BOTH",
  "encryptOutput": true,
  "requireSignature": true,
  "signerEmail": "zhangwei@example.com",
  "signerName": "Zhang Wei"
}
```

**Response** (200 OK):
```json
{
  "documentId": "POL-20241004120000-ABC123",
  "customerId": "CUST-2024-001",
  "documentType": "policy",
  "generatedAt": "2024-10-04T12:00:00",
  "filePath": "./output/documents/auto_policy_CUST-2024-001_20241004_120000.pdf",
  "base64Content": "JVBERi0xLjQKJeLjz9MK...",
  "encryptedFilePath": "./output/documents/auto_policy_CUST-2024-001_20241004_120000.pdf.encrypted",
  "pageCount": 3,
  "fieldsFilled": 15,
  "fileSize": 45820,
  "fileName": "auto_policy_CUST-2024-001_20241004_120000.pdf",
  "signatureStatus": "SENT",
  "signatureRequestId": "ENV-12345ABC",
  "signatureUrl": "https://demo.docusign.net/signing/ENV-12345ABC",
  "status": "PENDING_SIGNATURE",
  "message": "Document filled and sent for signature",
  "encrypted": true,
  "complianceCheckPassed": true
}
```

### 2. Fill Document (Asynchronous)

**Endpoint**: `POST /api/v1/insurance/document/fill-async`

**Request Body**: Same as synchronous endpoint

**Response**: Same structure, processed asynchronously

---

## Request Model

### DocumentRequest

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| customerId | String | Yes | Customer identifier |
| documentType | String | Yes | policy, claim, application, quote |
| insuranceType | String | Yes | auto, home, life, health |
| customerData | CustomerData | Yes | Customer information |
| policyData | PolicyData | No | Policy information |
| templatePath | String | No | Custom template path |
| outputFormat | Enum | No | FILE_PATH, BASE64, BOTH (default: FILE_PATH) |
| encryptOutput | Boolean | No | Encrypt output file (default: true) |
| requireSignature | Boolean | No | Request e-signature (default: false) |
| signerEmail | String | No | Signer's email |
| signerName | String | No | Signer's name |

### CustomerData

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Full name |
| age | Integer | Yes | Age (18-100) |
| address | String | Yes | Full address |
| email | String | No | Email address |
| phone | String | No | Phone number |
| occupation | String | No | Occupation |
| gender | String | No | Gender |
| dateOfBirth | String | No | Date of birth |
| nationalId | String | No | National ID |
| additionalFields | Map | No | Custom fields |

### PolicyData

| Field | Type | Description |
|-------|------|-------------|
| policyNumber | String | Policy number |
| policyType | String | Type of policy |
| premiumAmount | Double | Premium amount |
| currency | String | Currency code |
| effectiveDate | String | Policy start date |
| expiryDate | String | Policy end date |
| coverageAmount | String | Coverage amount |
| deductible | String | Deductible amount |
| terms | String | Policy terms |
| additionalFields | Map | Custom policy fields |

---

## Response Model

### DocumentResponse

| Field | Type | Description |
|-------|------|-------------|
| documentId | String | Unique document ID |
| customerId | String | Customer reference |
| documentType | String | Document type |
| generatedAt | DateTime | Generation timestamp |
| filePath | String | Path to saved file |
| base64Content | String | Base64 encoded content |
| encryptedFilePath | String | Path to encrypted file |
| pageCount | Integer | Number of pages |
| fieldsFilled | Integer | Number of fields filled |
| fileSize | Long | File size in bytes |
| fileName | String | Generated filename |
| signatureStatus | Enum | Signature status |
| signatureRequestId | String | DocuSign envelope ID |
| signatureUrl | String | Signing URL |
| status | String | SUCCESS, PENDING_SIGNATURE, FAILED |
| message | String | Status message |
| errors | List | Error messages |
| encrypted | Boolean | Encryption status |
| complianceCheckPassed | Boolean | Compliance status |

---

## PDF Field Mapping

### Standard Field Names

The service automatically maps common field names:

| Data Field | PDF Field Names (case-insensitive) |
|------------|-------------------------------------|
| Customer Name | name, customername, fullname |
| Age | age |
| Address | address, customeraddress |
| Email | email, customeremail |
| Phone | phone, telephone |
| Occupation | occupation |
| Gender | gender |
| Date of Birth | dob, dateofbirth |
| Policy Number | policynumber, policyno |
| Policy Type | policytype |
| Premium | premium, premiumamount |
| Effective Date | effectivedate, startdate |
| Expiry Date | expirydate, enddate |
| Coverage | coverage, coverageamount |

### Custom Fields

Use `additionalFields` in `CustomerData` or `PolicyData` to add custom mappings:

```json
{
  "customerData": {
    "name": "John Doe",
    "additionalFields": {
      "driverLicense": "DL123456",
      "vehicleVIN": "1HGBH41JXMN109186"
    }
  }
}
```

---

## Output Formats

### FILE_PATH
Returns the path to the saved PDF file.

```json
{
  "filePath": "./output/documents/auto_policy_CUST001_20241004.pdf"
}
```

### BASE64
Returns the PDF content as base64 encoded string.

```json
{
  "base64Content": "JVBERi0xLjQKJeLjz9MK..."
}
```

### BOTH
Returns both file path and base64 content.

```json
{
  "filePath": "./output/documents/auto_policy_CUST001_20241004.pdf",
  "base64Content": "JVBERi0xLjQKJeLjz9MK..."
}
```

---

## Electronic Signature

### Enable DocuSign

```yaml
insurance:
  document:
    use-docusign: true
  external-api:
    docusign:
      url: https://demo.docusign.net/restapi
      api-key: your-api-key
      enabled: true
```

### Request with Signature

```json
{
  "requireSignature": true,
  "signerEmail": "customer@example.com",
  "signerName": "Customer Name"
}
```

### Response

```json
{
  "status": "PENDING_SIGNATURE",
  "signatureStatus": "SENT",
  "signatureRequestId": "ENV-12345ABC",
  "signatureUrl": "https://demo.docusign.net/signing/ENV-12345ABC"
}
```

### Signature Status

- **NOT_REQUIRED**: Signature not requested
- **PENDING**: Request being processed
- **SENT**: Email sent to signer
- **SIGNED**: Document signed
- **DECLINED**: Signer declined
- **EXPIRED**: Request expired

---

## File Encryption

### Enable Encryption

```json
{
  "encryptOutput": true
}
```

### Encrypted Output

- Original file: `document.pdf`
- Encrypted file: `document.pdf.encrypted`
- Algorithm: AES-256
- Content: Base64(AES(PDF bytes))

### Decrypt File

```java
@Autowired
private EncryptionUtil encryptionUtil;

// Read encrypted content
String encryptedContent = Files.readString(Path.of("document.pdf.encrypted"));

// Decrypt
String decryptedBase64 = encryptionUtil.decrypt(encryptedContent);
byte[] pdfBytes = Base64.getDecoder().decode(decryptedBase64);

// Save decrypted PDF
Files.write(Path.of("document.pdf"), pdfBytes);
```

---

## Configuration

### application.yml

```yaml
insurance:
  document:
    template-directory: ./templates  # PDF templates location
    output-directory: ./output/documents  # Output files location
    use-docusign: false  # Enable electronic signature

  external-api:
    docusign:
      url: https://demo.docusign.net/restapi
      api-key: ${DOCUSIGN_API_KEY}
      enabled: false
```

### Environment Variables

```bash
# DocuSign API
export DOCUSIGN_API_URL="https://demo.docusign.net/restapi"
export DOCUSIGN_API_KEY="your-api-key"

# Directory paths
export TEMPLATE_DIR="./templates"
export OUTPUT_DIR="./output/documents"
```

---

## Template Setup

### 1. Create Template Directory

```bash
mkdir -p ./templates
```

### 2. Add PDF Templates

Place PDF templates in the template directory with naming convention:

```
{insuranceType}_{documentType}_template.pdf

Examples:
- auto_policy_template.pdf
- home_application_template.pdf
- life_quote_template.pdf
- health_claim_template.pdf
```

### 3. Template Fields

For best results, create PDF forms with field names matching the standard mappings:

- Adobe Acrobat: Form > Add Text Field
- Field names (case-insensitive): name, age, address, email, etc.
- Field types: Text, Number, Date

---

## Usage Examples

### Example 1: Fill Policy Document

```powershell
$body = @{
    customerId = "CUST001"
    documentType = "policy"
    insuranceType = "auto"
    customerData = @{
        name = "John Doe"
        age = 30
        address = "123 Main St"
        email = "john@example.com"
    }
    policyData = @{
        policyNumber = "POL-001"
        premiumAmount = 1500.0
    }
    outputFormat = "FILE_PATH"
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/insurance/document/fill" `
    -Method Post -ContentType "application/json" -Body $body
```

### Example 2: Async with Signature

```java
@Autowired
private EnhancedDocumentFillingService documentService;

DocumentRequest request = DocumentRequest.builder()
    .customerId("CUST001")
    .documentType("policy")
    .insuranceType("auto")
    .customerData(/* ... */)
    .requireSignature(true)
    .signerEmail("customer@example.com")
    .build();

CompletableFuture<DocumentResponse> future = 
    documentService.fillDocumentAsync(request);

future.thenAccept(response -> {
    System.out.println("Document ID: " + response.getDocumentId());
    System.out.println("Signing URL: " + response.getSignatureUrl());
});
```

---

## Testing

### Run Unit Tests

```bash
mvn test -Dtest=EnhancedDocumentFillingServiceTest
```

### Run API Tests

```powershell
# Windows
.\test-document-api.ps1
```

### Test Cases

1. ✅ Fill document with customer data
2. ✅ Fill with policy data
3. ✅ Base64 output
4. ✅ Both output formats
5. ✅ File encryption
6. ✅ Async processing
7. ✅ Electronic signature
8. ✅ Additional fields
9. ✅ Multi-page PDF
10. ✅ Field validation

---

## Performance

| Operation | Latency | Description |
|-----------|---------|-------------|
| Fill simple PDF | 50-100ms | 1-2 pages, 10 fields |
| Fill complex PDF | 100-200ms | 5+ pages, 50+ fields |
| Encryption | +20-50ms | AES encryption overhead |
| DocuSign API | +200-500ms | Network call |
| Async processing | Non-blocking | Background execution |

---

## Troubleshooting

### Issue: Template not found

**Solution**: Ensure template file exists in template directory with correct naming:
```
{insuranceType}_{documentType}_template.pdf
```

### Issue: Fields not filled

**Solution**: 
1. Check PDF has form fields (not just text)
2. Verify field names match mappings (case-insensitive)
3. Use Adobe Acrobat to check field names

### Issue: Encryption fails

**Solution**: 
1. Verify AES_SECRET_KEY is 32 characters
2. Check EncryptionUtil is properly configured

### Issue: DocuSign API error

**Solution**:
1. Verify API credentials
2. Check network connectivity
3. System falls back to mock if API disabled

---

## Best Practices

1. **Template Design**: Create well-structured PDF forms with clear field names
2. **Field Naming**: Use standard names for automatic mapping
3. **Error Handling**: Always check response status
4. **Encryption**: Enable for sensitive documents
5. **Async Processing**: Use for non-urgent documents
6. **Logging**: Monitor document generation logs
7. **Storage**: Clean up old documents regularly

---

## Future Enhancements

- [ ] Support for DOCX templates
- [ ] Image insertion (logos, signatures)
- [ ] Barcode/QR code generation
- [ ] Multi-language support
- [ ] Batch document processing
- [ ] Cloud storage integration (S3, Azure)
- [ ] Advanced signature workflows

---

**For more information, see [README.md](README.md) and [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)**

