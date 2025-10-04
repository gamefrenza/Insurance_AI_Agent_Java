# Insurance AI Agent

An intelligent insurance agent system built with Java and Spring Boot for automated insurance quoting, underwriting, and document filling.

## Features

- **Automated Quoting**: Generate insurance quotes based on customer data and risk factors
- **AI-Powered Underwriting**: Make intelligent underwriting decisions using OpenAI GPT-4
- **Document Generation**: Automatically generate and fill insurance policy documents
- **Session Management**: Track user context across requests using Spring Session
- **GDPR Compliance**: AES encryption for sensitive customer data with comprehensive logging
- **RESTful API**: Clean, well-documented REST endpoints

## Technology Stack

- **Framework**: Spring Boot 3.1.5
- **Java Version**: 17
- **Dependencies**:
  - spring-boot-starter-web
  - spring-boot-starter-validation
  - spring-session-core
  - okhttp3 (for OpenAI API)
  - jackson-databind (JSON processing)
  - lombok (reduce boilerplate)
  - slf4j (logging)

## Project Structure

```
src/main/java/com/xai/insuranceagent/
├── InsuranceAgentApplication.java     # Main application class
├── config/
│   └── SecurityConfig.java            # Security configuration
├── controller/
│   └── AgentController.java           # REST API controller
├── service/
│   ├── QuotingService.java            # Quote generation
│   ├── UnderwritingService.java       # Underwriting decisions
│   └── DocumentFillingService.java    # Document generation
├── model/
│   ├── Customer.java                  # Customer data model
│   ├── ProcessRequest.java            # API request model
│   ├── ProcessResponse.java           # API response model
│   └── ErrorResponse.java             # Error response model
└── util/
    ├── EncryptionUtil.java            # AES encryption utility
    └── OpenAIClient.java              # OpenAI API client
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- OpenAI API Key (optional, but required for AI features)

## Configuration

### Environment Variables

Set the following environment variables before running:

```bash
# Windows PowerShell
$env:OPENAI_API_KEY="your-openai-api-key-here"
$env:AES_SECRET_KEY="YourSecretKey32CharactersLong!"

# Linux/Mac
export OPENAI_API_KEY="your-openai-api-key-here"
export AES_SECRET_KEY="YourSecretKey32CharactersLong!"
```

### Application Configuration

Edit `src/main/resources/application.yml` to customize:
- Server port (default: 8080)
- Session timeout (default: 30m)
- Insurance base rates
- Risk thresholds
- Logging levels

## Installation & Running

### Method 1: Maven

```bash
# Install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

### Method 2: JAR

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/insurance-agent-1.0.0.jar
```

The application will start at `http://localhost:8080/api/v1`

## API Documentation

### 1. Process Insurance Request

**Endpoint**: `POST /api/v1/insurance/process`

**Description**: Main endpoint for processing insurance requests. Performs quoting, underwriting, and document generation.

**Request Body**:
```json
{
  "customer": {
    "age": 25,
    "vehicle": "Tesla Model 3",
    "address": "Beijing, China",
    "insuranceType": "auto",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+86-1234567890"
  }
}
```

**Response** (200 OK):
```json
{
  "sessionId": "ABC123...",
  "quote": {
    "premiumAmount": 1560.0,
    "currency": "USD",
    "coverageDetails": "Comprehensive auto coverage...",
    "validUntil": "2024-11-03"
  },
  "underwriting": {
    "decision": "APPROVED",
    "riskLevel": "MEDIUM",
    "reasoning": "Application approved...",
    "riskScore": 0.45
  },
  "document": {
    "documentId": "AUT-20241004120000-abc12345",
    "documentUrl": "https://docs.insurance-agent.com/auto/AUT-...",
    "documentType": "Auto Insurance Policy",
    "status": "PENDING_SIGNATURE"
  },
  "status": "SUCCESS",
  "message": "Insurance request processed successfully",
  "timestamp": "2024-10-04T12:00:00"
}
```

### 2. Health Check

**Endpoint**: `GET /api/v1/insurance/health`

**Response**:
```json
{
  "status": "UP",
  "timestamp": "2024-10-04T12:00:00",
  "service": "Insurance AI Agent",
  "version": "1.0.0"
}
```

### 3. Get Session

**Endpoint**: `GET /api/v1/insurance/session`

**Response**:
```json
{
  "sessionId": "ABC123...",
  "creationTime": 1696416000000,
  "lastAccessTime": "2024-10-04T12:00:00",
  "hasCustomerData": true,
  "insuranceType": "auto"
}
```

### 4. Clear Session

**Endpoint**: `DELETE /api/v1/insurance/session`

**Response**:
```json
{
  "message": "Session cleared successfully",
  "sessionId": "ABC123..."
}
```

## Insurance Types

The system supports four insurance types:

1. **auto** - Auto/Vehicle Insurance
   - Required fields: `vehicle`, `age`, `address`
   
2. **home** - Home/Property Insurance
   - Optional fields: `propertyType`, `propertyValue`
   
3. **life** - Life Insurance
   - Optional fields: `occupation`, `smoker`
   
4. **health** - Health Insurance
   - Optional fields: `medicalHistory`

## Example Usage

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/insurance/process \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "age": 30,
      "vehicle": "Honda Civic",
      "address": "Shanghai, China",
      "insuranceType": "auto",
      "name": "Jane Smith",
      "email": "jane@example.com"
    }
  }'
```

### PowerShell Example

```powershell
$body = @{
    customer = @{
        age = 30
        vehicle = "Honda Civic"
        address = "Shanghai, China"
        insuranceType = "auto"
        name = "Jane Smith"
        email = "jane@example.com"
    }
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/insurance/process" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

## Error Handling

The API returns appropriate HTTP status codes:

- **200 OK**: Request processed successfully
- **400 Bad Request**: Validation errors or invalid input
- **500 Internal Server Error**: Server-side errors

**Error Response Format**:
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request data",
  "details": [
    "age: Age must be at least 18",
    "insuranceType: Insurance type must be one of: auto, home, life, health"
  ],
  "timestamp": "2024-10-04T12:00:00"
}
```

## Security & Compliance

### GDPR Compliance

- **Data Encryption**: All sensitive customer data (email, phone, address) is encrypted using AES-256
- **Data Minimization**: Only necessary data is collected and processed
- **Audit Logging**: All operations are logged with SLF4J for audit trails
- **Session Management**: Secure session handling with configurable timeouts

### Logging

Logs are stored in `logs/insurance-agent.log` with:
- Request/response tracking
- Error details
- Performance metrics
- Security events

## Development

### Adding New Features

1. Create service class in `service/` package
2. Inject into `AgentController`
3. Update `ProcessResponse` model if needed
4. Add appropriate logging and error handling

### Testing

```bash
# Run tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Troubleshooting

### Common Issues

1. **OpenAI API Errors**
   - Verify API key is set correctly
   - Check API quota and limits
   - Services fall back to default behavior if AI unavailable

2. **Session Issues**
   - Sessions are in-memory by default
   - Configure external session store for production

3. **Encryption Errors**
   - Ensure AES_SECRET_KEY is exactly 32 characters
   - Check character encoding

## Production Deployment

For production deployment:

1. Use external session store (Redis/Database)
2. Configure proper logging aggregation
3. Set up proper secrets management
4. Enable HTTPS
5. Configure rate limiting
6. Set up monitoring and alerting

## License

MIT License - see LICENSE file

## Support

For issues and questions, please create an issue in the repository.

---

**Built with ❤️ using Java and Spring Boot**

