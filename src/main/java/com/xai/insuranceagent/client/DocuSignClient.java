package com.xai.insuranceagent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for DocuSign electronic signature API (simulation)
 */
@Component
public class DocuSignClient {

    private static final Logger logger = LoggerFactory.getLogger(DocuSignClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${insurance.external-api.docusign.url}")
    private String apiUrl;

    @Value("${insurance.external-api.docusign.api-key}")
    private String apiKey;

    @Value("${insurance.external-api.docusign.enabled}")
    private boolean apiEnabled;

    public DocuSignClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send document for electronic signature asynchronously
     */
    public CompletableFuture<SignatureResponse> sendForSignatureAsync(
            String documentPath, String signerEmail, String signerName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendForSignature(documentPath, signerEmail, signerName);
            } catch (Exception e) {
                logger.error("Error sending for signature: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to send for signature", e);
            }
        });
    }

    /**
     * Send document for electronic signature
     */
    public SignatureResponse sendForSignature(
            String documentPath, String signerEmail, String signerName) throws IOException {
        
        if (!apiEnabled) {
            logger.debug("DocuSign API disabled, using mock response");
            return getMockSignatureResponse(signerEmail, signerName);
        }

        logger.info("Sending document for signature - Signer: {}", signerEmail);

        // Read document and encode to base64
        File documentFile = new File(documentPath);
        byte[] documentBytes = java.nio.file.Files.readAllBytes(documentFile.toPath());
        String base64Document = Base64.getEncoder().encodeToString(documentBytes);

        String requestBody = buildSignatureRequest(base64Document, signerEmail, signerName);

        Request request = new Request.Builder()
                .url(apiUrl + "/envelopes")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warn("DocuSign API returned error: {}, falling back to mock", response.code());
                return getMockSignatureResponse(signerEmail, signerName);
            }

            String responseBody = response.body().string();
            return parseSignatureResponse(responseBody);

        } catch (Exception e) {
            logger.warn("Error calling DocuSign API, using mock response: {}", e.getMessage());
            return getMockSignatureResponse(signerEmail, signerName);
        }
    }

    /**
     * Check signature status
     */
    public SignatureStatus checkSignatureStatus(String envelopeId) throws IOException {
        if (!apiEnabled) {
            return SignatureStatus.PENDING;
        }

        Request request = new Request.Builder()
                .url(apiUrl + "/envelopes/" + envelopeId)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return SignatureStatus.UNKNOWN;
            }

            String responseBody = response.body().string();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            String status = rootNode.path("status").asText();

            return mapDocuSignStatus(status);

        } catch (Exception e) {
            logger.error("Error checking signature status: {}", e.getMessage());
            return SignatureStatus.UNKNOWN;
        }
    }

    /**
     * Build signature request JSON
     */
    private String buildSignatureRequest(String base64Document, String signerEmail, String signerName) 
            throws IOException {
        
        String json = String.format("""
                {
                  "emailSubject": "Please sign this insurance document",
                  "documents": [{
                    "documentBase64": "%s",
                    "name": "Insurance_Policy.pdf",
                    "fileExtension": "pdf",
                    "documentId": "1"
                  }],
                  "recipients": {
                    "signers": [{
                      "email": "%s",
                      "name": "%s",
                      "recipientId": "1",
                      "routingOrder": "1"
                    }]
                  },
                  "status": "sent"
                }
                """, base64Document, signerEmail, signerName);

        return json;
    }

    /**
     * Parse DocuSign API response
     */
    private SignatureResponse parseSignatureResponse(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);

        return SignatureResponse.builder()
                .envelopeId(rootNode.path("envelopeId").asText())
                .status(SignatureStatus.SENT)
                .signingUrl(rootNode.path("url").asText())
                .expiresAt(rootNode.path("expiresDateTime").asText())
                .build();
    }

    /**
     * Mock signature response for testing/fallback
     */
    private SignatureResponse getMockSignatureResponse(String signerEmail, String signerName) {
        String mockEnvelopeId = "ENV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        logger.debug("Generating mock signature response - Envelope: {}", mockEnvelopeId);

        return SignatureResponse.builder()
                .envelopeId(mockEnvelopeId)
                .status(SignatureStatus.SENT)
                .signingUrl("https://demo.docusign.net/signing/" + mockEnvelopeId)
                .expiresAt(java.time.LocalDateTime.now().plusDays(30).toString())
                .recipientEmail(signerEmail)
                .recipientName(signerName)
                .message("Mock signature request sent successfully")
                .build();
    }

    /**
     * Map DocuSign status to internal status
     */
    private SignatureStatus mapDocuSignStatus(String docusignStatus) {
        return switch (docusignStatus.toLowerCase()) {
            case "sent" -> SignatureStatus.SENT;
            case "delivered" -> SignatureStatus.PENDING;
            case "completed" -> SignatureStatus.SIGNED;
            case "declined" -> SignatureStatus.DECLINED;
            case "voided" -> SignatureStatus.EXPIRED;
            default -> SignatureStatus.UNKNOWN;
        };
    }

    /**
     * Signature response model
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SignatureResponse {
        private String envelopeId;
        private SignatureStatus status;
        private String signingUrl;
        private String expiresAt;
        private String recipientEmail;
        private String recipientName;
        private String message;
    }

    public enum SignatureStatus {
        SENT,
        PENDING,
        SIGNED,
        DECLINED,
        EXPIRED,
        UNKNOWN
    }
}

