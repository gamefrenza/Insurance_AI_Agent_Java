# Enhanced Underwriting Service Documentation

## Overview

The Enhanced Underwriting Service is a sophisticated risk assessment and decision-making engine that uses **Drools Rules Engine**, optional **Machine Learning (Weka)**, and external API integration to automate insurance underwriting decisions.

## Features

### ✅ Drools Rules Engine
- **10+ Pre-configured Rules**: Credit score, DUI, claims history, driving violations, etc.
- **Real-time Rule Evaluation**: Fast and accurate decision making
- **Transparent Logic**: Rules are defined in human-readable DRL format
- **Easy to Extend**: Add new rules without code changes

### ✅ Machine Learning (Optional)
- **Weka Decision Tree (J48)**: Trainable ML model for risk assessment
- **Confidence Scoring**: ML predictions include confidence levels
- **Fallback Support**: Gracefully handles ML failures

### ✅ External API Integration
- **Credit Score API**: Experian simulation for real-time credit checks
- **Async Processing**: Non-blocking external API calls
- **Auto-Fallback**: Uses mock data if API unavailable

### ✅ Comprehensive Risk Assessment
- **Multi-factor Analysis**: Credit, claims, driving record, health, property
- **Risk Scoring (0-100)**: Numerical risk assessment
- **Risk Levels**: LOW, MEDIUM, HIGH, VERY_HIGH
- **Premium Adjustments**: Automatic multiplier calculation

### ✅ Compliance & Security
- **Audit Logging**: All decisions logged with SLF4J
- **Data Encryption**: Sensitive data encrypted via javax.crypto
- **GDPR Compliant**: Masked logging and secure data handling

---

## Architecture

```
UnderwritingController
    ↓
EnhancedUnderwritingService
    ↓
    ├──> Drools Rules Engine (Primary)
    ├──> ML Service (Optional)
    ├──> Credit Score API (External)
    └──> Standard Assessment (Fallback)
```

---

## API Endpoints

### 1. Assess Risk (Synchronous)

**Endpoint**: `POST /api/v1/insurance/underwriting/assess`

**Request Body**:
```json
{
  "customerId": "CUST-2024-001",
  "creditScore": 720,
  "claimsInLast3Years": 1,
  "totalClaimAmount": 2500.0,
  "lastClaimDate": "2023-06-15",
  "drivingViolations": 0,
  "atFaultAccidents": 0,
  "dui": false,
  "yearsLicensed": 10,
  "yearsWithPreviousInsurer": 5,
  "previousCancellation": false,
  "previousDenial": false,
  "annualIncome": 75000.0,
  "employmentStatus": "Full-time",
  "insuranceType": "auto",
  "age": 32,
  "address": "Shanghai, Pudong District"
}
```

**Response** (200 OK):
```json
{
  "decisionId": "dec-abc123",
  "customerId": "CUST-2024-001",
  "decisionDate": "2024-10-04T12:00:00",
  "decision": "APPROVE",
  "riskScore": 35,
  "riskLevel": "LOW",
  "terms": "Standard policy terms",
  "exclusions": [],
  "extraPremium": null,
  "premiumMultiplier": 1.0,
  "decisionReason": "Standard approval based on risk assessment",
  "riskFactors": ["Minor claim history"],
  "positiveFactors": ["Good credit score", "Long insurance history"],
  "conditions": [],
  "referralReason": null,
  "requiresManualReview": false,
  "complianceCheckPassed": true,
  "complianceIssues": [],
  "decisionMethod": "RULES_ENGINE",
  "confidenceScore": 0.95
}
```

### 2. Assess Risk (Asynchronous)

**Endpoint**: `POST /api/v1/insurance/underwriting/assess-async`

**Request Body**: Same as synchronous endpoint

**Response**: Same structure, processed asynchronously

---

## Drools Rules

### Rule 1: Low Credit Score - Reject
```
IF credit_score < 600
THEN decision = REJECT, risk_level = VERY_HIGH, risk_score = 95
```

### Rule 2: DUI History - Reject (Auto Insurance)
```
IF insurance_type = auto AND dui = true
THEN decision = REJECT, risk_level = VERY_HIGH, risk_score = 98
```

### Rule 3: Previous Cancellation - Refer
```
IF previous_cancellation = true
THEN decision = REFER, requires_manual_review = true
```

### Rule 4: Excellent Credit - Approve with Discount
```
IF credit_score >= 750 AND claims = 0
THEN decision = APPROVE, risk_level = LOW, risk_score = 15, premium_multiplier = 0.9
```

### Rule 5: High Claims History - Extra Premium
```
IF claims >= 3 AND credit_score >= 600
THEN decision = APPROVE, risk_level = HIGH, risk_score = 75, premium_multiplier = 1.5, extra_premium = $500
```

### Rule 6: Multiple Driving Violations - High Risk
```
IF insurance_type = auto AND violations >= 3 AND credit_score >= 600
THEN decision = APPROVE, risk_level = HIGH, risk_score = 80, premium_multiplier = 1.4
CONDITION: Must complete defensive driving course
```

### Rule 7: Property in Flood Zone - Exclusion
```
IF insurance_type = home AND in_flood_zone = true
THEN exclusions.add("Flood damage not covered"), condition.add("Separate flood insurance required")
```

### Rule 8: Smoker - Extra Premium (Health/Life)
```
IF insurance_type IN (health, life) AND smoker = true
THEN premium_multiplier *= 1.3, condition.add("Annual health screening required")
```

### Rule 9: Young Driver Clean Record - Approve
```
IF insurance_type = auto AND age < 25 AND violations = 0 AND accidents = 0 AND credit_score >= 650
THEN decision = APPROVE, risk_level = MEDIUM, risk_score = 45, premium_multiplier = 1.2
```

### Rule 10: Default Approval - Good Standing
```
IF credit_score >= 650 AND credit_score < 750
THEN decision = APPROVE, risk_level = MEDIUM, risk_score = 40, premium_multiplier = 1.0
```

---

## Risk Score Calculation

### Formula
```
Risk Score = Credit_Factor + Claims_Factor + Type_Specific_Factor + Previous_Issues_Factor

Range: 0-100
```

### Credit Score Factor (0-40 points)
```
< 600:   40 points
600-649: 30 points
650-699: 20 points
700-749: 10 points
>= 750:  0 points (excellent)
```

### Claims History Factor (0-25 points)
```
claims_count * 8, max 25 points
```

### Type-Specific Risk Factor (0-20 points)

#### Auto Insurance
- DUI: +20 points
- Driving violations: violations * 5, max 15
- At-fault accidents: accidents * 7, max 20

#### Health/Life Insurance
- Smoker: +15 points
- Medical conditions: conditions_count * 5, max 20

#### Home Insurance
- Flood zone: +10 points
- Property age > 50: +10 points
- No security system: +5 points

### Previous Issues Factor (0-15 points)
- Previous cancellation: +10 points
- Previous denial: +15 points

---

## Decision Outcomes

### APPROVE
- Risk score < 60
- Standard or adjusted premium
- May include conditions or exclusions
- Premium multiplier: 0.9 - 1.5

### REJECT
- Risk score >= 80
- High-risk factors (DUI, low credit, etc.)
- Cannot be overridden by rules engine

### REFER
- Risk score 60-79
- Requires manual review
- Uncertain risk factors
- Previous issues flagged

---

## Machine Learning Mode

### Enable ML
```yaml
insurance:
  underwriting:
    use-ml: true
```

### ML Model: Weka J48 Decision Tree

**Training Features**:
- Credit score
- Claims count
- Age
- Years licensed
- Insurance type

**Output Classes**:
- APPROVE
- REJECT
- REFER

**Confidence Threshold**: 0.85

**Training Data**: Sample data included, replace with production data

---

## External API Integration

### Credit Score API (Experian)

**Enable**:
```yaml
insurance:
  underwriting:
    use-external-credit-check: true
  external-api:
    credit-score:
      url: https://api.experian.com
      api-key: your-api-key
      enabled: true
```

**Response**:
```json
{
  "customerId": "CUST001",
  "creditScore": 720,
  "scoreRange": "300-850",
  "bureau": "Experian",
  "delinquencies": 0,
  "bankruptcies": 0,
  "accountsInGoodStanding": 8,
  "totalDebt": 25000.0,
  "creditUtilization": 0.3,
  "creditAge": 12,
  "recentInquiries": 2,
  "riskLevel": "LOW"
}
```

---

## Data Models

### CustomerRiskProfile

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| customerId | String | Yes | Unique customer identifier |
| creditScore | Integer | Yes | 300-850 |
| claimsInLast3Years | Integer | No | Number of claims |
| totalClaimAmount | Double | No | Total claim amount |
| drivingViolations | Integer | No | Traffic violations count |
| atFaultAccidents | Integer | No | At-fault accidents count |
| dui | Boolean | No | DUI on record |
| yearsLicensed | Integer | No | Years of driving experience |
| previousCancellation | Boolean | No | Previous policy cancellation |
| previousDenial | Boolean | No | Previous application denial |
| smoker | Boolean | No | Smoking status |
| inFloodZone | Boolean | No | Property in flood zone |
| insuranceType | String | Yes | auto, home, life, health |
| age | Integer | No | Customer age |

### UnderwritingDecision

| Field | Type | Description |
|-------|------|-------------|
| decisionId | String | Unique decision identifier |
| customerId | String | Customer reference |
| decision | String | APPROVE, REJECT, REFER |
| riskScore | Integer | 0-100 numerical risk |
| riskLevel | String | LOW, MEDIUM, HIGH, VERY_HIGH |
| premiumMultiplier | Double | Premium adjustment factor |
| extraPremium | Double | Additional premium amount |
| exclusions | List<String> | Policy exclusions |
| conditions | List<String> | Approval conditions |
| decisionMethod | String | RULES_ENGINE, ML, STANDARD |
| confidenceScore | Double | Decision confidence 0.0-1.0 |

---

## Testing

### Run Unit Tests
```bash
mvn test -Dtest=EnhancedUnderwritingServiceTest
```

### Run API Tests
```powershell
# Windows
.\test-underwriting-api.ps1

# Linux/Mac
chmod +x test-underwriting-api.sh
./test-underwriting-api.sh
```

### Test Cases Included
1. ✅ Excellent credit - approval
2. ✅ Low credit score - rejection
3. ✅ DUI history - rejection
4. ✅ High claims - extra premium
5. ✅ Smoker - risk factor
6. ✅ Flood zone - exclusion
7. ✅ Async assessment
8. ✅ Multiple violations - high risk
9. ✅ Previous cancellation - referral
10. ✅ Young driver clean record

---

## Usage Examples

### Example 1: Basic Underwriting

```java
@Autowired
private EnhancedUnderwritingService underwritingService;

CustomerRiskProfile profile = CustomerRiskProfile.builder()
    .customerId("CUST001")
    .creditScore(720)
    .insuranceType("auto")
    .age(30)
    .claimsInLast3Years(0)
    .build();

UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

System.out.println("Decision: " + decision.getDecision());
System.out.println("Risk Score: " + decision.getRiskScore());
```

### Example 2: Async Underwriting

```java
CompletableFuture<UnderwritingDecision> futureDecision = 
    underwritingService.performUnderwritingAsync(profile);

futureDecision.thenAccept(decision -> {
    System.out.println("Decision: " + decision.getDecision());
});
```

### Example 3: cURL

```bash
curl -X POST http://localhost:8080/api/v1/insurance/underwriting/assess \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "creditScore": 720,
    "insuranceType": "auto",
    "age": 30,
    "claimsInLast3Years": 0
  }'
```

---

## Configuration

### application.yml

```yaml
insurance:
  underwriting:
    use-ml: false  # Enable ML mode
    use-external-credit-check: false  # Enable external API
    age:
      min: 18
      max: 75
    risk-factors:
      high-risk-threshold: 0.7

  external-api:
    credit-score:
      url: https://api.experian.com
      api-key: ${CREDIT_SCORE_API_KEY}
      enabled: false
```

### Environment Variables

```bash
# Credit Score API
export CREDIT_SCORE_API_URL="https://api.experian.com"
export CREDIT_SCORE_API_KEY="your-api-key"
```

---

## Compliance & Security

### Audit Logging
```
INFO - COMPLIANCE LOG - Underwriting started for customer: MASKED, Type: auto, Credit Score: 720
```

### Data Encryption
- SSN encrypted before API calls
- Sensitive fields masked in logs
- AES-256 encryption via EncryptionUtil

### GDPR Compliance
- Right to explanation: Decision reasoning provided
- Data minimization: Only necessary fields collected
- Audit trail: All decisions logged

---

## Performance

| Operation | Response Time | Throughput |
|-----------|---------------|------------|
| Rules Engine | 50-100ms | High |
| With ML | 100-200ms | Medium |
| With External API | 200-500ms | Medium |
| Async Processing | Non-blocking | High |

---

## Troubleshooting

### Issue: Rules not firing

**Solution**: Check Drools logs, verify rule syntax in `underwriting-rules.drl`

### Issue: ML initialization failed

**Solution**: Ensure Weka dependency is present, check training data

### Issue: External API timeout

**Solution**: Check network connectivity, API credentials, system falls back to mock data

### Issue: High rejection rate

**Solution**: Review rule thresholds, adjust credit score or risk factor limits

---

## Adding Custom Rules

Edit `src/main/resources/underwriting-rules.drl`:

```drl
rule "Custom Rule Name"
    when
        $profile : CustomerRiskProfile(/* conditions */)
        $decision : UnderwritingDecision(decision == null)
    then
        logger.info("Rule triggered: Custom Rule");
        $decision.setDecision("APPROVE");
        // ... set other fields
        update($decision);
end
```

Restart application to load new rules.

---

## Future Enhancements

- [ ] Real-time fraud detection
- [ ] Deep learning risk models
- [ ] Multi-bureau credit checks
- [ ] Automated appeals process
- [ ] Dynamic rule learning
- [ ] A/B testing framework

---

**For more information, see [README.md](README.md) and [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)**

