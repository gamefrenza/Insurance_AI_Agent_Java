# Quick Start Guide

## 🚀 Start in 3 Steps

### Step 1: Prerequisites Check

Ensure you have:
- ✅ Java 17 or higher
- ✅ Maven 3.6 or higher

Check versions:
```bash
java -version
mvn -version
```

### Step 2: Set Environment Variables (Optional)

**Windows PowerShell:**
```powershell
$env:OPENAI_API_KEY="sk-your-api-key-here"
$env:AES_SECRET_KEY="YourSecretKey32CharactersLong!"
```

**Linux/Mac:**
```bash
export OPENAI_API_KEY="sk-your-api-key-here"
export AES_SECRET_KEY="YourSecretKey32CharactersLong!"
```

> **Note**: If you don't set OPENAI_API_KEY, the system will work with fallback responses.

### Step 3: Run the Application

**Option A: Use the startup script**

Windows:
```powershell
.\run.ps1
```

Linux/Mac:
```bash
chmod +x run.sh
./run.sh
```

**Option B: Use Maven directly**
```bash
mvn spring-boot:run
```

The application will start at: **http://localhost:8080/api/v1**

---

## 🧪 Test It Out

### Quick Health Check

**Windows PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/insurance/health"
```

**Linux/Mac:**
```bash
curl http://localhost:8080/api/v1/insurance/health
```

### Send a Test Request

**Windows PowerShell:**
```powershell
$body = @{
    customer = @{
        age = 25
        vehicle = "Tesla Model 3"
        address = "Beijing, China"
        insuranceType = "auto"
        name = "Test User"
        email = "test@example.com"
    }
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/insurance/process" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body | ConvertTo-Json -Depth 10
```

**Linux/Mac:**
```bash
curl -X POST http://localhost:8080/api/v1/insurance/process \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "age": 25,
      "vehicle": "Tesla Model 3",
      "address": "Beijing, China",
      "insuranceType": "auto",
      "name": "Test User",
      "email": "test@example.com"
    }
  }' | jq '.'
```

### Run Automated Tests

**Windows:**
```powershell
.\test-api.ps1
```

**Linux/Mac:**
```bash
chmod +x test-api.sh
./test-api.sh
```

---

## 📋 Example Response

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
    "reasoning": "Application approved based on favorable risk assessment...",
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

---

## 🎯 Supported Insurance Types

| Type | Value | Required Fields | Optional Fields |
|------|-------|----------------|-----------------|
| Auto | `"auto"` | age, address | vehicle |
| Home | `"home"` | age, address | propertyType, propertyValue |
| Life | `"life"` | age, address | occupation, smoker |
| Health | `"health"` | age, address | medicalHistory |

---

## 🔍 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/insurance/process` | Process insurance request |
| GET | `/insurance/health` | Health check |
| GET | `/insurance/session` | Get session info |
| DELETE | `/insurance/session` | Clear session |

---

## ⚙️ Configuration

Edit `src/main/resources/application.yml` to customize:

```yaml
server:
  port: 8080  # Change port

openai:
  api:
    model: gpt-4  # Change AI model
    temperature: 0.7  # Adjust creativity

insurance:
  quoting:
    base-rate:
      auto: 1200.0  # Adjust base rates
```

---

## 📊 Logs

Logs are saved to: `logs/insurance-agent.log`

View logs:
```bash
tail -f logs/insurance-agent.log
```

---

## ❓ Troubleshooting

### Port 8080 already in use
Change port in `application.yml`:
```yaml
server:
  port: 8081
```

### OpenAI API errors
- Check API key is correct
- Verify API has quota
- System will use fallback if API unavailable

### Build errors
```bash
mvn clean install -U
```

### Still having issues?
Check `logs/insurance-agent.log` for detailed error messages.

---

## 📚 Next Steps

1. ✅ Read [README.md](README.md) for complete documentation
2. ✅ Check [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) for architecture details
3. ✅ Modify `application.yml` for your needs
4. ✅ Integrate with your frontend application
5. ✅ Deploy to production (see README.md)

---

## 💡 Tips

- Use the provided `example-request.json` for testing
- Session data persists for 30 minutes
- All sensitive data is encrypted (GDPR compliant)
- Check health endpoint before sending requests
- View logs for debugging

---

**Happy Coding! 🎉**

