package com.xai.insuranceagent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessResponse {

    private String sessionId;
    
    private QuoteResult quote;
    
    private UnderwritingResult underwriting;
    
    private DocumentResult document;
    
    private String status;
    
    private String message;
    
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteResult {
        private Double premiumAmount;
        private String currency;
        private String coverageDetails;
        private String validUntil;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnderwritingResult {
        private String decision; // APPROVED, DECLINED, PENDING
        private String riskLevel; // LOW, MEDIUM, HIGH
        private String reasoning;
        private Double riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResult {
        private String documentId;
        private String documentUrl;
        private String documentType;
        private String status; // GENERATED, PENDING_SIGNATURE, SIGNED
    }
}

