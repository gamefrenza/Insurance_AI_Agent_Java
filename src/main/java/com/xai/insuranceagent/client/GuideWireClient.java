package com.xai.insuranceagent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xai.insuranceagent.model.quote.QuoteRequest;
import com.xai.insuranceagent.model.quote.QuoteResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for external insurance quoting API (Guidewire simulation)
 */
@Component
public class GuideWireClient {

    private static final Logger logger = LoggerFactory.getLogger(GuideWireClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${insurance.external-api.guidewire.url}")
    private String apiUrl;

    @Value("${insurance.external-api.guidewire.api-key}")
    private String apiKey;

    @Value("${insurance.external-api.guidewire.timeout-seconds}")
    private int timeoutSeconds;

    @Value("${insurance.external-api.guidewire.enabled}")
    private boolean apiEnabled;

    public GuideWireClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Asynchronously get quote from external API
     */
    public CompletableFuture<QuoteResponse> getQuoteAsync(QuoteRequest quoteRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getQuote(quoteRequest);
            } catch (Exception e) {
                logger.error("Error getting quote from Guidewire API: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to get external quote", e);
            }
        });
    }

    /**
     * Synchronously get quote from external API
     */
    public QuoteResponse getQuote(QuoteRequest quoteRequest) throws IOException {
        if (!apiEnabled) {
            logger.debug("External API disabled, using mock response");
            return getMockQuoteResponse(quoteRequest);
        }

        logger.info("Calling Guidewire API for insurance type: {}", quoteRequest.getInsuranceType());

        String requestBody = objectMapper.writeValueAsString(quoteRequest);

        Request request = new Request.Builder()
                .url(apiUrl + "/quotes")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warn("Guidewire API returned error: {}, falling back to mock", response.code());
                return getMockQuoteResponse(quoteRequest);
            }

            String responseBody = response.body().string();
            QuoteResponse quoteResponse = objectMapper.readValue(responseBody, QuoteResponse.class);
            
            logger.info("Successfully received quote from Guidewire API - Premium: ${}", 
                    quoteResponse.getTotalPremium());
            
            return quoteResponse;

        } catch (Exception e) {
            logger.warn("Error calling Guidewire API, using mock response: {}", e.getMessage());
            return getMockQuoteResponse(quoteRequest);
        }
    }

    /**
     * Mock response for testing/fallback
     */
    private QuoteResponse getMockQuoteResponse(QuoteRequest request) {
        logger.debug("Generating mock quote response for: {}", request.getInsuranceType());

        double basePremium = getBasePremiumForType(request.getInsuranceType());
        
        return QuoteResponse.builder()
                .premium(basePremium)
                .totalPremium(basePremium)
                .currency("USD")
                .coverage("comprehensive")
                .coverageDetails("Mock comprehensive coverage from Guidewire API simulation")
                .deductible(500)
                .policyTerm("12 months")
                .quotedAt(LocalDateTime.now())
                .quoteId("GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
    }

    private double getBasePremiumForType(String insuranceType) {
        return switch (insuranceType.toLowerCase()) {
            case "auto" -> 1000.0;
            case "home" -> 800.0;
            case "life" -> 500.0;
            case "health" -> 600.0;
            default -> 1000.0;
        };
    }

    /**
     * Validate external API connectivity
     */
    public boolean testConnection() {
        if (!apiEnabled) {
            logger.info("External API is disabled");
            return false;
        }

        try {
            Request request = new Request.Builder()
                    .url(apiUrl + "/health")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                boolean isSuccess = response.isSuccessful();
                logger.info("Guidewire API health check: {}", isSuccess ? "PASS" : "FAIL");
                return isSuccess;
            }

        } catch (Exception e) {
            logger.warn("Guidewire API health check failed: {}", e.getMessage());
            return false;
        }
    }
}

