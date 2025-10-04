package com.xai.insuranceagent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xai.insuranceagent.util.EncryptionUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for external credit scoring API (Experian simulation)
 */
@Component
public class CreditScoreClient {

    private static final Logger logger = LoggerFactory.getLogger(CreditScoreClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Random random;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Value("${insurance.external-api.credit-score.url}")
    private String apiUrl;

    @Value("${insurance.external-api.credit-score.api-key}")
    private String apiKey;

    @Value("${insurance.external-api.credit-score.enabled}")
    private boolean apiEnabled;

    public CreditScoreClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
    }

    /**
     * Asynchronously get credit score
     */
    public CompletableFuture<CreditScoreResponse> getCreditScoreAsync(String customerId, String ssn) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getCreditScore(customerId, ssn);
            } catch (Exception e) {
                logger.error("Error getting credit score: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to get credit score", e);
            }
        });
    }

    /**
     * Synchronously get credit score
     */
    public CreditScoreResponse getCreditScore(String customerId, String ssn) throws IOException {
        if (!apiEnabled) {
            logger.debug("External credit API disabled, using mock response");
            return getMockCreditScore(customerId);
        }

        // Encrypt sensitive data
        String encryptedSsn = encryptionUtil.encrypt(ssn);
        logger.info("Calling credit score API for customer: {}", 
                encryptionUtil.maskSensitiveData(customerId));

        String requestBody = buildCreditScoreRequest(customerId, encryptedSsn);

        Request request = new Request.Builder()
                .url(apiUrl + "/credit-score")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warn("Credit score API returned error: {}, falling back to mock", response.code());
                return getMockCreditScore(customerId);
            }

            String responseBody = response.body().string();
            return parseCreditScoreResponse(responseBody);

        } catch (Exception e) {
            logger.warn("Error calling credit score API, using mock response: {}", e.getMessage());
            return getMockCreditScore(customerId);
        }
    }

    /**
     * Mock credit score for testing/fallback
     */
    private CreditScoreResponse getMockCreditScore(String customerId) {
        logger.debug("Generating mock credit score for customer: {}", customerId);

        // Generate realistic mock data
        int baseScore = 600 + random.nextInt(200); // 600-800
        
        return CreditScoreResponse.builder()
                .customerId(customerId)
                .creditScore(baseScore)
                .scoreRange("300-850")
                .bureau("Experian (Mock)")
                .delinquencies(baseScore < 650 ? random.nextInt(3) : 0)
                .bankruptcies(baseScore < 600 ? random.nextInt(2) : 0)
                .accountsInGoodStanding(random.nextInt(10) + 5)
                .totalDebt(random.nextDouble() * 50000)
                .creditUtilization(random.nextDouble() * 0.8)
                .creditAge(random.nextInt(20) + 3)
                .recentInquiries(random.nextInt(5))
                .riskLevel(determineRiskLevel(baseScore))
                .build();
    }

    private String buildCreditScoreRequest(String customerId, String encryptedSsn) throws IOException {
        return String.format("""
                {
                  "customerId": "%s",
                  "ssn": "%s",
                  "requestType": "full_report"
                }
                """, customerId, encryptedSsn);
    }

    private CreditScoreResponse parseCreditScoreResponse(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        return CreditScoreResponse.builder()
                .customerId(rootNode.path("customerId").asText())
                .creditScore(rootNode.path("creditScore").asInt())
                .scoreRange(rootNode.path("scoreRange").asText())
                .bureau(rootNode.path("bureau").asText())
                .delinquencies(rootNode.path("delinquencies").asInt())
                .bankruptcies(rootNode.path("bankruptcies").asInt())
                .accountsInGoodStanding(rootNode.path("accountsInGoodStanding").asInt())
                .totalDebt(rootNode.path("totalDebt").asDouble())
                .creditUtilization(rootNode.path("creditUtilization").asDouble())
                .creditAge(rootNode.path("creditAge").asInt())
                .recentInquiries(rootNode.path("recentInquiries").asInt())
                .riskLevel(rootNode.path("riskLevel").asText())
                .build();
    }

    private String determineRiskLevel(int creditScore) {
        if (creditScore >= 750) return "LOW";
        if (creditScore >= 650) return "MEDIUM";
        if (creditScore >= 550) return "HIGH";
        return "VERY_HIGH";
    }

    /**
     * Credit score response model
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditScoreResponse {
        private String customerId;
        private Integer creditScore;
        private String scoreRange;
        private String bureau;
        private Integer delinquencies;
        private Integer bankruptcies;
        private Integer accountsInGoodStanding;
        private Double totalDebt;
        private Double creditUtilization;
        private Integer creditAge;
        private Integer recentInquiries;
        private String riskLevel;
    }
}

