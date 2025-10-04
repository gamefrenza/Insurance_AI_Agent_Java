package com.xai.insuranceagent.model.quote;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Quote response model containing premium and coverage details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteResponse implements Serializable {

    private Double premium;

    private Double totalPremium;

    private String currency;

    private String coverage;

    private String coverageDetails;

    private Integer deductible;

    private String policyTerm;

    @Builder.Default
    private LocalDateTime quotedAt = LocalDateTime.now();

    private String validUntil;

    private String quoteId;

    // Breakdown of premium calculation
    private PremiumBreakdown breakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PremiumBreakdown {
        private Double basePremium;
        private Double ageFactor;
        private Double genderFactor;
        private Double locationFactor;
        private Double riskFactor;
        private Double totalAdjustment;
        private String calculationNotes;
    }
}

