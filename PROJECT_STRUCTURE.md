# Project Structure Overview

## 📁 Complete File Structure

```
Insurance_AI_Agent_Java/
│
├── src/
│   └── main/
│       ├── java/com/xai/insuranceagent/
│       │   ├── InsuranceAgentApplication.java    # Main Spring Boot application
│       │   │
│       │   ├── config/
│       │   │   └── SecurityConfig.java           # CORS and security configuration
│       │   │
│       │   ├── controller/
│       │   │   └── AgentController.java          # REST API endpoints
│       │   │
│       │   ├── service/
│       │   │   ├── QuotingService.java           # Quote generation logic
│       │   │   ├── UnderwritingService.java      # Underwriting decisions
│       │   │   └── DocumentFillingService.java   # Document generation
│       │   │
│       │   ├── model/
│       │   │   ├── Customer.java                 # Customer data model
│       │   │   ├── ProcessRequest.java           # API request model
│       │   │   ├── ProcessResponse.java          # API response model
│       │   │   └── ErrorResponse.java            # Error response model
│       │   │
│       │   └── util/
│       │       ├── EncryptionUtil.java           # AES encryption (GDPR)
│       │       └── OpenAIClient.java             # OpenAI API integration
│       │
│       └── resources/
│           └── application.yml                    # Application configuration
│
├── pom.xml                                        # Maven dependencies
├── README.md                                      # Complete documentation
├── PROJECT_STRUCTURE.md                           # This file
├── .gitignore                                     # Git ignore rules
│
├── example-request.json                           # Sample API request
├── run.ps1                                        # Windows startup script
├── run.sh                                         # Linux/Mac startup script
├── test-api.ps1                                   # Windows API test script
└── test-api.sh                                    # Linux/Mac API test script
```

## 🎯 Component Descriptions

### Core Application

**InsuranceAgentApplication.java**
- Main entry point
- Configures Spring Boot
- Sets up session management
- Initializes application context

### Controllers

**AgentController.java**
- REST API endpoints
- Request validation
- Session management
- Error handling
- Endpoints:
  - `POST /insurance/process` - Main processing endpoint
  - `GET /insurance/health` - Health check
  - `GET /insurance/session` - Get session info
  - `DELETE /insurance/session` - Clear session

### Services (Business Logic)

**QuotingService.java**
- Calculates insurance premiums
- Applies risk factors
- Uses AI for coverage details
- Supports: auto, home, life, health insurance

**UnderwritingService.java**
- Risk assessment
- Decision making (APPROVED/DECLINED/PENDING)
- Risk score calculation
- AI-powered reasoning

**DocumentFillingService.java**
- Document generation
- Data encryption (GDPR)
- Document ID generation
- Document template filling

### Models (Data Structures)

**Customer.java**
- Customer information
- Validation rules
- Support for all insurance types

**ProcessRequest.java**
- API request wrapper
- Contains Customer object
- Session and context info

**ProcessResponse.java**
- API response wrapper
- Contains Quote, Underwriting, Document results
- Status and timestamp

**ErrorResponse.java**
- Standardized error format
- Validation error details
- HTTP status codes

### Utilities

**EncryptionUtil.java**
- AES-256 encryption
- Data masking for logs
- GDPR compliance

**OpenAIClient.java**
- OpenAI API integration
- GPT-4 chat completions
- Error handling with fallbacks

### Configuration

**SecurityConfig.java**
- CORS configuration
- Security settings

**application.yml**
- Server configuration
- OpenAI settings
- Encryption keys
- Business rules
- Logging configuration

## 🔄 Request Flow

```
1. Client sends POST /insurance/process
   ↓
2. AgentController receives request
   ↓
3. Validates Customer data
   ↓
4. Stores in Session
   ↓
5. QuotingService generates quote
   ↓
6. UnderwritingService makes decision
   ↓
7. DocumentFillingService generates document
   ↓
8. Response sent back to client
```

## 📊 Data Flow

```
Customer Data
    ↓
Validation (Jakarta Validation)
    ↓
Encryption (GDPR)
    ↓
Quoting (Base Rate + Risk Factors)
    ↓
AI Analysis (OpenAI GPT-4)
    ↓
Underwriting Decision
    ↓
Document Generation
    ↓
Response to Client
```

## 🛠️ Technology Stack

### Core
- Java 17
- Spring Boot 3.1.5
- Maven

### Libraries
- **Web**: spring-boot-starter-web
- **Validation**: jakarta.validation
- **Session**: spring-session-core
- **HTTP Client**: OkHttp3
- **JSON**: Jackson
- **Utilities**: Lombok
- **Logging**: SLF4J

### Features
- RESTful API
- AI Integration (OpenAI GPT-4)
- Data Encryption (AES-256)
- Session Management
- Comprehensive Logging
- Error Handling

## 🚀 Quick Start

### Windows
```powershell
.\run.ps1
```

### Linux/Mac
```bash
chmod +x run.sh
./run.sh
```

### Manual
```bash
mvn spring-boot:run
```

## 🧪 Testing

### Windows
```powershell
.\test-api.ps1
```

### Linux/Mac
```bash
chmod +x test-api.sh
./test-api.sh
```

## 📝 Key Features Implementation

### 1. Modular Design ✅
- Separate services for quoting, underwriting, and document filling
- Dependency injection via Spring

### 2. AI Integration ✅
- OpenAI GPT-4 API calls via OkHttp
- Fallback mechanisms
- Context-aware prompts

### 3. Session Management ✅
- Spring Session for context storage
- 30-minute timeout
- In-memory storage (configurable)

### 4. GDPR Compliance ✅
- AES-256 encryption
- Data masking in logs
- Comprehensive audit trails

### 5. Error Handling ✅
- Validation errors (HTTP 400)
- Server errors (HTTP 500)
- Detailed error messages

### 6. Logging ✅
- SLF4J with Logback
- File and console output
- Configurable levels

## 🔐 Security Features

1. **Data Encryption**: AES-256 for sensitive data
2. **Session Security**: Secure session management
3. **CORS**: Configurable CORS policies
4. **Validation**: Input validation at all levels
5. **Logging**: Security event logging

## 📚 Documentation

- **README.md**: Complete user guide and API documentation
- **PROJECT_STRUCTURE.md**: This file - architecture overview
- **Comments**: Inline documentation in all Java files

## 🎓 Learning Resources

### Spring Boot
- Controllers handle HTTP requests
- Services contain business logic
- Models define data structures
- Repositories (not used here) would handle data persistence

### Design Patterns
- **Dependency Injection**: Spring manages object lifecycle
- **Service Layer**: Business logic separation
- **DTO Pattern**: Request/Response models
- **Builder Pattern**: Lombok @Builder

### Best Practices
- Validation at controller level
- Logging at all levels
- Exception handling with @ExceptionHandler
- Configuration externalization

---

**For detailed API documentation, see README.md**

