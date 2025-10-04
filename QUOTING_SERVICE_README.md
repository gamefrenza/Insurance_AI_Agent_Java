# Enhanced Quoting Service Documentation

## Overview

The Enhanced Quoting Service is a sophisticated insurance premium calculation engine that provides detailed, rule-based quoting with support for external API integration and asynchronous processing.

## Features

### ✅ Rule-Based Calculation
- **Age Factors**: Young driver surcharge (+20% for age < 25), Senior surcharge (+15% for age > 65), Optimal age discount (-5% for age 25-35)
- **Gender Factors**: Male driver surcharge (+8% for auto insurance)
- **Location Factors**: Urban area surcharge (+10% for major cities)
- **Risk Factors**: Vehicle type, smoking status, occupation, property value

### ✅ Insurance Types Supported
1. **Auto Insurance**
   - Base rate: $1,200
   - Factors: Age, gender, location, vehicle model
   - Sports cars: +25%, Luxury vehicles: +12%, SUV/Truck: +8%

2. **Health Insurance**
   - Base rate: $600
   - Factors: Age, smoking status
   - Smoker surcharge: +15%

3. **Home Insurance**
   - Base rate: $800
   - Factors: Location, property value
   - High-value property (>$1M): +18%

4. **Life Insurance**
   - Base rate: $500
   - Factors: Age, occupation
   - High-risk occupation (pilot, miner): +20%

### ✅ External API Integration
- **GuideWire Client**: Simulated external insurance quoting API
- **Async Support**: CompletableFuture for non-blocking operations
- **Fallback Mechanism**: Local calculation if external API fails

### ✅ Detailed Response
- Premium breakdown with all factors
- Coverage details
- Deductible calculation
- Quote ID generation
- Validity period

## API Endpoints

### 1. Generate Quote (Synchronous)

**Endpoint**: `POST /api/v1/insurance/quote/generate`

**Request Body**:
```json
{
  "age": 25,
  "gender": "male",
  "address": "Beijing, Chaoyang District",
  "insuranceType": "auto",
  "vehicleModel": "Tesla Model 3",
  "vehicleYear": 2023,
  "vehicleMake": "Tesla",
  "name": "Zhang Wei",
  "email": "zhangwei@example.com",
  "phone": "+86-13812345678",
  "isUrbanArea": true
}
```

**Response** (200 OK):
```json
{
  "premium": 1560.0,
  "totalPremium": 1560.0,
  "currency": "USD",
  "coverage": "comprehensive",
  "coverageDetails": "Comprehensive auto coverage including liability, collision, and theft protection for Tesla Model 3. Roadside assistance included.",
  "deductible": 1000,
  "policyTerm": "12 months",
  "quotedAt": "2024-10-04T12:00:00",
  "validUntil": "2024-11-03",
  "quoteId": "AUT-20241004120000-ABC123",
  "breakdown": {
    "basePremium": 1200.0,
    "ageFactor": 240.0,
    "genderFactor": 96.0,
    "locationFactor": 120.0,
    "riskFactor": 144.0,
    "totalAdjustment": 600.0,
    "calculationNotes": "Premium calculated based on: age adjustment ($240.00), gender factor ($96.00), location surcharge ($120.00), risk factors ($144.00)"
  }
}
```

### 2. Generate Quote (Asynchronous)

**Endpoint**: `POST /api/v1/insurance/quote/generate-async`

**Request Body**: Same as synchronous endpoint

**Response**: Same as synchronous endpoint (processed asynchronously)

## Request Model (QuoteRequest)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| age | Integer | Yes | Customer age (18-100) |
| gender | String | No | "male", "female", or "other" |
| address | String | Yes | Customer address |
| insuranceType | String | Yes | "auto", "home", "life", or "health" |
| vehicleModel | String | No | Vehicle model (auto insurance) |
| vehicleYear | Integer | No | Vehicle year |
| vehicleMake | String | No | Vehicle manufacturer |
| smoker | Boolean | No | Smoking status (health insurance) |
| medicalHistory | String | No | Medical history |
| propertyType | String | No | Property type (home insurance) |
| propertyValue | Double | No | Property value |
| occupation | String | No | Occupation (life insurance) |
| name | String | No | Customer name |
| email | String | No | Customer email |
| phone | String | No | Customer phone |
| isUrbanArea | Boolean | No | Urban area flag |

## Response Model (QuoteResponse)

| Field | Type | Description |
|-------|------|-------------|
| premium | Double | Premium amount |
| totalPremium | Double | Total premium amount |
| currency | String | Currency code (USD) |
| coverage | String | Coverage type |
| coverageDetails | String | Detailed coverage description |
| deductible | Integer | Deductible amount |
| policyTerm | String | Policy term |
| quotedAt | DateTime | Quote generation timestamp |
| validUntil | String | Quote validity date |
| quoteId | String | Unique quote identifier |
| breakdown | PremiumBreakdown | Detailed breakdown |

### PremiumBreakdown

| Field | Type | Description |
|-------|------|-------------|
| basePremium | Double | Base premium before adjustments |
| ageFactor | Double | Age-based adjustment |
| genderFactor | Double | Gender-based adjustment |
| locationFactor | Double | Location-based adjustment |
| riskFactor | Double | Risk-based adjustment |
| totalAdjustment | Double | Sum of all adjustments |
| calculationNotes | String | Explanation of calculations |

## Premium Calculation Rules

### Age-Based Adjustments

```
Age < 25:        +20% (Young driver surcharge)
Age 25-35:       -5%  (Optimal age discount)
Age 36-65:       0%   (Standard rate)
Age > 65:        +15% (Senior surcharge)
```

### Gender-Based Adjustments

```
Auto Insurance:
  Male:          +8%
  Female:        0%
  Other:         0%
```

### Location-Based Adjustments

```
Major Cities:    +10%
- Beijing, Shanghai, Guangzhou, Shenzhen
- New York, Los Angeles
- Any address with "city" or isUrbanArea=true

Rural Areas:     0%
```

### Risk Factors

#### Auto Insurance
```
Sports Cars:     +25% (Ferrari, Lamborghini, Porsche, "sports")
Luxury Cars:     +12% (Tesla, BMW, Mercedes)
Large Vehicles:  +8%  (SUV, Truck)
Standard:        0%
```

#### Health Insurance
```
Smoker:          +15%
Non-smoker:      0%
```

#### Home Insurance
```
Property > $1M:  +18%
Property < $1M:  0%
```

#### Life Insurance
```
High-risk Job:   +20% (Pilot, Miner, Construction)
Standard Job:    0%
```

## Deductible Calculation

```
Auto Insurance:
  Premium > $2,000:  $1,000
  Premium ≤ $2,000:  $500

Home Insurance:
  Premium > $1,500:  $750
  Premium ≤ $1,500:  $500

Health Insurance:
  Premium > $1,000:  $500
  Premium ≤ $1,000:  $250

Default:             $500
```

## External API Configuration

Edit `application.yml`:

```yaml
insurance:
  quoting:
    use-external-api: false  # Set to true to enable

  external-api:
    guidewire:
      url: https://api.guidewire.example.com
      api-key: your-api-key-here
      timeout-seconds: 30
      enabled: false
```

Environment variables:
```bash
export GUIDEWIRE_API_URL="https://api.guidewire.example.com"
export GUIDEWIRE_API_KEY="your-api-key-here"
```

## Testing

### Run Unit Tests

```bash
mvn test -Dtest=EnhancedQuotingServiceTest
```

### Run Integration Tests

```powershell
# Windows
.\test-quote-api.ps1

# Linux/Mac
chmod +x test-quote-api.sh
./test-quote-api.sh
```

### Example Test Cases

1. **Young Male Driver in City** (Multiple surcharges)
2. **Smoker Health Insurance** (Risk factor)
3. **Sports Car** (High risk vehicle)
4. **Optimal Age Discount** (Age 25-35)
5. **High-Risk Occupation** (Pilot, miner)
6. **High-Value Property** (>$1M)

## Code Examples

### Using EnhancedQuotingService

```java
@Autowired
private EnhancedQuotingService quotingService;

// Synchronous quote
QuoteRequest request = QuoteRequest.builder()
    .age(25)
    .gender("male")
    .address("Beijing")
    .insuranceType("auto")
    .vehicleModel("Tesla Model 3")
    .isUrbanArea(true)
    .build();

QuoteResponse response = quotingService.generateDetailedQuote(request);

// Asynchronous quote
CompletableFuture<QuoteResponse> futureResponse = 
    quotingService.generateQuoteAsync(request);
    
QuoteResponse response = futureResponse.get();
```

### Using REST API (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/insurance/quote/generate \
  -H "Content-Type: application/json" \
  -d '{
    "age": 25,
    "gender": "male",
    "address": "Beijing",
    "insuranceType": "auto",
    "vehicleModel": "Tesla Model 3",
    "isUrbanArea": true
  }'
```

### Using REST API (PowerShell)

```powershell
$body = @{
    age = 25
    gender = "male"
    address = "Beijing"
    insuranceType = "auto"
    vehicleModel = "Tesla Model 3"
    isUrbanArea = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/insurance/quote/generate" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

## Architecture

```
QuoteController
    ↓
EnhancedQuotingService
    ↓
    ├── Local Calculation (Rule-based)
    │   ├── Base Premium
    │   ├── Age Factor
    │   ├── Gender Factor
    │   ├── Location Factor
    │   └── Risk Factor
    │
    └── External API (Optional)
        └── GuideWireClient
            ├── Async Processing
            └── Fallback to Local
```

## Error Handling

### Validation Errors (400)
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid quote request data",
  "details": [
    "age: Age must be at least 18",
    "insuranceType: Insurance type must be one of: auto, home, life, health"
  ],
  "timestamp": "2024-10-04T12:00:00"
}
```

### Server Errors (500)
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to generate quote: ...",
  "timestamp": "2024-10-04T12:00:00"
}
```

## Performance

- **Synchronous**: ~50-100ms
- **Asynchronous**: Non-blocking
- **External API**: 30s timeout with fallback

## Best Practices

1. ✅ Use async endpoint for non-critical operations
2. ✅ Always validate input data
3. ✅ Handle external API failures gracefully
4. ✅ Log all quote generations for audit
5. ✅ Cache frequently requested quotes
6. ✅ Monitor premium calculation accuracy

## Troubleshooting

### High Premiums
- Check breakdown for surcharge reasons
- Verify input data is correct
- Review calculation notes

### External API Errors
- Check API credentials
- Verify network connectivity
- Review fallback behavior

### Validation Errors
- Ensure required fields are provided
- Check field format and ranges
- Review error details in response

## Future Enhancements

- [ ] Machine learning-based risk scoring
- [ ] Multi-currency support
- [ ] Quote comparison engine
- [ ] Real-time market data integration
- [ ] Advanced discount rules
- [ ] Customer loyalty factors

---

**For more information, see the main [README.md](README.md)**

