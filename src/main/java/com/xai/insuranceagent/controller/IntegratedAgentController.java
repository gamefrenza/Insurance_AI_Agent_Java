package com.xai.insuranceagent.controller;

import com.xai.insuranceagent.model.document.DocumentRequest;
import com.xai.insuranceagent.model.document.DocumentResponse;
import com.xai.insuranceagent.model.quote.QuoteRequest;
import com.xai.insuranceagent.model.quote.QuoteResponse;
import com.xai.insuranceagent.model.underwriting.CustomerRiskProfile;
import com.xai.insuranceagent.model.underwriting.UnderwritingDecision;
import com.xai.insuranceagent.service.EnhancedDocumentFillingService;
import com.xai.insuranceagent.service.EnhancedQuotingService;
import com.xai.insuranceagent.service.EnhancedUnderwritingService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Integrated controller combining all three enhanced services
 */
@RestController
@RequestMapping("/api/v1/insurance")
public class IntegratedAgentController {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedAgentController.class);

    @Autowired
    private EnhancedQuotingService quotingService;

    @Autowired
    private EnhancedUnderwritingService underwritingService;

    @Autowired
    private EnhancedDocumentFillingService documentService;

    /**
     * Complete insurance processing workflow
     */
    @PostMapping("/process")
    public ResponseEntity<ComprehensiveResponse> processComplete(
            @Valid @RequestBody ComprehensiveRequest request) {
        
        logger.info("Processing complete insurance workflow for customer: {}", request.getCustomerId());

        try {
            // Step 1: Generate Quote
            QuoteRequest quoteReq = buildQuoteRequest(request);
            QuoteResponse quote = quotingService.generateQuote(quoteReq);
            logger.info("Quote generated - Premium: ${}", quote.getTotalPremium());

            // Step 2: Underwriting Assessment
            CustomerRiskProfile riskProfile = buildRiskProfile(request);
            UnderwritingDecision decision = underwritingService.assessRisk(riskProfile);
            logger.info("Underwriting completed - Decision: {}, Risk Score: {}", 
                    decision.getDecision(), decision.getRiskScore());

            // Step 3: Document Generation (only if approved or conditional)
            DocumentResponse document = null;
            if ("APPROVED".equals(decision.getDecision()) || "CONDITIONAL_APPROVAL".equals(decision.getDecision())) {
                DocumentRequest docReq = buildDocumentRequest(request, quote, decision);
                document = documentService.fillDocument(docReq);
                logger.info("Document generated - ID: {}", document.getDocumentId());
            }

            // Build comprehensive response
            ComprehensiveResponse response = ComprehensiveResponse.builder()
                    .customerId(request.getCustomerId())
                    .quote(quote)
                    .underwriting(decision)
                    .document(document)
                    .overallStatus(determineOverallStatus(decision))
                    .processedAt(LocalDateTime.now())
                    .message(buildStatusMessage(decision))
                    .build();

            logger.info("Complete workflow processed successfully for customer: {}", request.getCustomerId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in complete workflow: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process complete workflow: " + e.getMessage());
        }
    }

    /**
     * Async complete workflow processing
     */
    @PostMapping("/process-async")
    public CompletableFuture<ResponseEntity<ComprehensiveResponse>> processCompleteAsync(
            @Valid @RequestBody ComprehensiveRequest request) {
        
        logger.info("Starting async complete workflow for customer: {}", request.getCustomerId());

        return CompletableFuture.supplyAsync(() -> {
            // Run quote, underwriting, and document generation in parallel
            CompletableFuture<QuoteResponse> quoteFuture = CompletableFuture.supplyAsync(() -> {
                QuoteRequest quoteReq = buildQuoteRequest(request);
                return quotingService.generateQuote(quoteReq);
            });

            CompletableFuture<UnderwritingDecision> underwritingFuture = CompletableFuture.supplyAsync(() -> {
                CustomerRiskProfile riskProfile = buildRiskProfile(request);
                return underwritingService.assessRisk(riskProfile);
            });

            // Wait for both to complete
            QuoteResponse quote = quoteFuture.join();
            UnderwritingDecision decision = underwritingFuture.join();

            // Generate document if approved
            DocumentResponse document = null;
            if ("APPROVED".equals(decision.getDecision()) || "CONDITIONAL_APPROVAL".equals(decision.getDecision())) {
                DocumentRequest docReq = buildDocumentRequest(request, quote, decision);
                document = documentService.fillDocument(docReq);
            }

            ComprehensiveResponse response = ComprehensiveResponse.builder()
                    .customerId(request.getCustomerId())
                    .quote(quote)
                    .underwriting(decision)
                    .document(document)
                    .overallStatus(determineOverallStatus(decision))
                    .processedAt(LocalDateTime.now())
                    .message("Async workflow completed")
                    .build();

            return ResponseEntity.ok(response);
        });
    }

    // Helper methods

    private QuoteRequest buildQuoteRequest(ComprehensiveRequest req) {
        return QuoteRequest.builder()
                .customerId(req.getCustomerId())
                .age(req.getAge())
                .gender(req.getGender())
                .address(req.getAddress())
                .insuranceType(req.getInsuranceType())
                .vehicleModel(req.getVehicleModel())
                .propertyValue(req.getPropertyValue())
                .isSmoker(req.getIsSmoker())
                .occupation(req.getOccupation())
                .isUrbanArea(isUrban(req.getAddress()))
                .build();
    }

    private CustomerRiskProfile buildRiskProfile(ComprehensiveRequest req) {
        return CustomerRiskProfile.builder()
                .customerId(req.getCustomerId())
                .age(req.getAge())
                .insuranceType(req.getInsuranceType())
                .creditScore(req.getCreditScore())
                .claimsHistory(req.getClaimsHistory())
                .drivingRecord(req.getDrivingRecord())
                .isSmoker(req.getIsSmoker())
                .occupation(req.getOccupation())
                .address(req.getAddress())
                .build();
    }

    private DocumentRequest buildDocumentRequest(ComprehensiveRequest req, 
                                                  QuoteResponse quote, 
                                                  UnderwritingDecision decision) {
        return DocumentRequest.builder()
                .customerId(req.getCustomerId())
                .documentType("policy")
                .insuranceType(req.getInsuranceType())
                .customerData(DocumentRequest.CustomerData.builder()
                        .name(req.getCustomerName())
                        .age(req.getAge())
                        .address(req.getAddress())
                        .email(req.getEmail())
                        .phone(req.getPhone())
                        .occupation(req.getOccupation())
                        .gender(req.getGender())
                        .build())
                .policyData(DocumentRequest.PolicyData.builder()
                        .policyNumber(quote.getQuoteId())
                        .policyType(req.getInsuranceType() + " Insurance Policy")
                        .premiumAmount(quote.getTotalPremium() + (decision.getAdditionalPremium() != null ? decision.getAdditionalPremium() : 0.0))
                        .currency(quote.getCurrency())
                        .effectiveDate(LocalDateTime.now().toString())
                        .expiryDate(LocalDateTime.now().plusYears(1).toString())
                        .coverageAmount(quote.getCoverageAmount())
                        .deductible(String.valueOf(quote.getDeductible()))
                        .build())
                .outputFormat(DocumentRequest.OutputFormat.BOTH)
                .encryptOutput(true)
                .requireSignature(req.getRequireSignature())
                .signerEmail(req.getEmail())
                .signerName(req.getCustomerName())
                .build();
    }

    private boolean isUrban(String address) {
        if (address == null) return false;
        String lower = address.toLowerCase();
        return lower.contains("beijing") || lower.contains("shanghai") ||
               lower.contains("guangzhou") || lower.contains("shenzhen") ||
               lower.contains("city") || lower.contains("urban");
    }

    private String determineOverallStatus(UnderwritingDecision decision) {
        return switch (decision.getDecision()) {
            case "APPROVED" -> "SUCCESS";
            case "CONDITIONAL_APPROVAL" -> "CONDITIONAL";
            case "REJECTED" -> "REJECTED";
            case "MANUAL_REVIEW" -> "PENDING";
            default -> "UNKNOWN";
        };
    }

    private String buildStatusMessage(UnderwritingDecision decision) {
        return switch (decision.getDecision()) {
            case "APPROVED" -> "Insurance application approved! Policy documents are ready.";
            case "CONDITIONAL_APPROVAL" -> "Application conditionally approved with exclusions.";
            case "REJECTED" -> "Application rejected based on underwriting criteria.";
            case "MANUAL_REVIEW" -> "Application requires manual review by underwriting team.";
            default -> "Application processed.";
        };
    }

    /**
     * Comprehensive request model combining all services
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComprehensiveRequest {
        @NotBlank(message = "Customer ID is required")
        private String customerId;

        @NotBlank(message = "Customer name is required")
        private String customerName;

        @NotNull(message = "Age is required")
        @Min(value = 18, message = "Age must be at least 18")
        @Max(value = 100, message = "Age must be at most 100")
        private Integer age;

        @Pattern(regexp = "male|female|other", message = "Gender must be male, female, or other")
        private String gender;

        @NotBlank(message = "Address is required")
        private String address;

        @Email(message = "Valid email is required")
        private String email;

        private String phone;

        @NotBlank(message = "Insurance type is required")
        @Pattern(regexp = "auto|home|life|health", message = "Invalid insurance type")
        private String insuranceType;

        // Quoting fields
        private String vehicleModel;
        private Double propertyValue;
        private Boolean isSmoker;
        private String occupation;

        // Underwriting fields
        @Min(value = 300, message = "Credit score must be at least 300")
        @Max(value = 850, message = "Credit score must be at most 850")
        private Integer creditScore;

        @Min(value = 0, message = "Claims history cannot be negative")
        private Integer claimsHistory;

        private String drivingRecord;

        // Document fields
        @Builder.Default
        private Boolean requireSignature = false;
    }

    /**
     * Comprehensive response model
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComprehensiveResponse {
        private String customerId;
        private QuoteResponse quote;
        private UnderwritingDecision underwriting;
        private DocumentResponse document;
        private String overallStatus;
        private String message;
        private LocalDateTime processedAt;
    }
}

