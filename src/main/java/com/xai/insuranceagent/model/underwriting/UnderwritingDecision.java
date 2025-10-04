package com.xai.insuranceagent.model.underwriting;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Underwriting decision result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnderwritingDecision implements Serializable {

    private String decisionId;

    private String customerId;

    @Builder.Default
    private LocalDateTime decisionDate = LocalDateTime.now();

    // Decision outcome
    private String decision; // APPROVE, REJECT, REFER

    // Risk assessment
    private Integer riskScore; // 0-100

    private String riskLevel; // LOW, MEDIUM, HIGH, VERY_HIGH

    // Policy terms
    private String terms;

    private List<String> exclusions;

    private Double extraPremium; // Additional premium due to risk

    private Double premiumMultiplier; // e.g., 1.5 for 50% increase

    // Reasoning
    private String decisionReason;

    private List<String> riskFactors;

    private List<String> positiveFactors;

    // Conditions
    private List<String> conditions; // Conditions for approval

    // Referral information (if decision is REFER)
    private String referralReason;

    private Boolean requiresManualReview;

    // Compliance
    private Boolean complianceCheckPassed;

    private List<String> complianceIssues;

    // ML/Rules engine info
    private String decisionMethod; // RULES_ENGINE, MACHINE_LEARNING, HYBRID

    private Double confidenceScore; // 0.0-1.0
}

