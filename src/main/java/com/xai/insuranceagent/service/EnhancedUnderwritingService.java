package com.xai.insuranceagent.service;

import com.xai.insuranceagent.client.CreditScoreClient;
import com.xai.insuranceagent.model.underwriting.CustomerRiskProfile;
import com.xai.insuranceagent.model.underwriting.UnderwritingDecision;
import com.xai.insuranceagent.util.EncryptionUtil;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Underwriting Service with Drools Rules Engine
 * Provides automated risk assessment and underwriting decisions
 */
@Service
public class EnhancedUnderwritingService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedUnderwritingService.class);

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private CreditScoreClient creditScoreClient;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired(required = false)
    private MLUnderwritingService mlUnderwritingService;

    @Value("${insurance.underwriting.use-ml}")
    private boolean useML;

    @Value("${insurance.underwriting.use-external-credit-check}")
    private boolean useExternalCreditCheck;

    /**
     * Perform underwriting assessment synchronously
     */
    public UnderwritingDecision performUnderwriting(CustomerRiskProfile riskProfile) {
        logger.info("Starting underwriting for customer: {}, Type: {}", 
                riskProfile.getCustomerId(), riskProfile.getInsuranceType());

        try {
            // Log sensitive data access for compliance
            logComplianceEvent(riskProfile);

            // Enrich profile with external data if needed
            if (useExternalCreditCheck && !riskProfile.getExternalCreditCheckCompleted()) {
                enrichWithCreditScore(riskProfile);
            }

            // Initialize decision object
            UnderwritingDecision decision = initializeDecision(riskProfile);

            // Apply rules engine
            applyRulesEngine(riskProfile, decision);

            // If rules didn't make a decision, use ML or standard assessment
            if (decision.getDecision() == null) {
                if (useML && mlUnderwritingService != null) {
                    decision = mlUnderwritingService.assessRiskWithML(riskProfile, decision);
                    decision.setDecisionMethod("MACHINE_LEARNING");
                } else {
                    performStandardAssessment(riskProfile, decision);
                    decision.setDecisionMethod("STANDARD_ASSESSMENT");
                }
            } else {
                decision.setDecisionMethod("RULES_ENGINE");
            }

            // Calculate final risk score if not set
            if (decision.getRiskScore() == null) {
                decision.setRiskScore(calculateRiskScore(riskProfile));
            }

            // Compliance check
            performComplianceCheck(decision);

            // Finalize decision
            finalizeDecision(decision);

            logger.info("Underwriting completed - Decision: {}, Risk Level: {}, Score: {}", 
                    decision.getDecision(), decision.getRiskLevel(), decision.getRiskScore());

            return decision;

        } catch (Exception e) {
            logger.error("Error in underwriting process: {}", e.getMessage(), e);
            throw new RuntimeException("Underwriting failed", e);
        }
    }

    /**
     * Perform underwriting assessment asynchronously
     */
    public CompletableFuture<UnderwritingDecision> performUnderwritingAsync(CustomerRiskProfile riskProfile) {
        logger.info("Starting async underwriting for customer: {}", riskProfile.getCustomerId());
        
        return CompletableFuture.supplyAsync(() -> performUnderwriting(riskProfile))
                .exceptionally(throwable -> {
                    logger.error("Async underwriting failed: {}", throwable.getMessage());
                    return createErrorDecision(riskProfile, throwable.getMessage());
                });
    }

    /**
     * Apply Drools rules engine
     */
    private void applyRulesEngine(CustomerRiskProfile riskProfile, UnderwritingDecision decision) {
        logger.debug("Applying Drools rules engine");
        
        KieSession kieSession = kieContainer.newKieSession();
        
        try {
            // Set global logger for rules
            kieSession.setGlobal("logger", logger);
            
            // Insert facts
            kieSession.insert(riskProfile);
            kieSession.insert(decision);
            
            // Fire all rules
            int rulesFired = kieSession.fireAllRules();
            logger.info("Drools rules fired: {}", rulesFired);
            
        } finally {
            kieSession.dispose();
        }
    }

    /**
     * Enrich profile with external credit score
     */
    private void enrichWithCreditScore(CustomerRiskProfile riskProfile) {
        try {
            logger.debug("Fetching external credit score");
            
            // In production, SSN would come from secure storage
            String mockSsn = "XXX-XX-" + riskProfile.getCustomerId().substring(0, 4);
            
            CreditScoreClient.CreditScoreResponse creditScore = 
                    creditScoreClient.getCreditScore(riskProfile.getCustomerId(), mockSsn);
            
            riskProfile.setCreditScore(creditScore.getCreditScore());
            riskProfile.setExternalCreditCheckCompleted(true);
            
            logger.info("Credit score enriched: {}", creditScore.getCreditScore());
            
        } catch (Exception e) {
            logger.warn("Failed to fetch external credit score: {}", e.getMessage());
            // Continue with provided credit score
        }
    }

    /**
     * Standard risk assessment (fallback)
     */
    private void performStandardAssessment(CustomerRiskProfile riskProfile, UnderwritingDecision decision) {
        logger.debug("Performing standard risk assessment");
        
        int riskScore = calculateRiskScore(riskProfile);
        
        if (riskScore >= 80) {
            decision.setDecision("REJECT");
            decision.setDecisionReason("High risk score from standard assessment");
            decision.setRiskLevel("VERY_HIGH");
        } else if (riskScore >= 60) {
            decision.setDecision("REFER");
            decision.setReferralReason("Moderate risk requires manual review");
            decision.setRequiresManualReview(true);
            decision.setRiskLevel("HIGH");
        } else {
            decision.setDecision("APPROVE");
            decision.setDecisionReason("Standard approval based on risk assessment");
            decision.setRiskLevel(riskScore >= 40 ? "MEDIUM" : "LOW");
            decision.setPremiumMultiplier(1.0 + (riskScore * 0.01)); // 1% per risk point
        }
        
        decision.setRiskScore(riskScore);
    }

    /**
     * Calculate numerical risk score (0-100)
     */
    private int calculateRiskScore(CustomerRiskProfile riskProfile) {
        int score = 0;
        
        // Credit score factor (0-40 points)
        if (riskProfile.getCreditScore() != null) {
            if (riskProfile.getCreditScore() < 600) {
                score += 40;
            } else if (riskProfile.getCreditScore() < 650) {
                score += 30;
            } else if (riskProfile.getCreditScore() < 700) {
                score += 20;
            } else if (riskProfile.getCreditScore() < 750) {
                score += 10;
            }
            // Excellent credit (750+) adds 0 points
        }
        
        // Claims history factor (0-25 points)
        if (riskProfile.getClaimsInLast3Years() != null) {
            score += Math.min(riskProfile.getClaimsInLast3Years() * 8, 25);
        }
        
        // Insurance type specific factors (0-20 points)
        score += calculateTypeSpecificRisk(riskProfile);
        
        // Previous issues (0-15 points)
        if (Boolean.TRUE.equals(riskProfile.getPreviousCancellation())) {
            score += 10;
        }
        if (Boolean.TRUE.equals(riskProfile.getPreviousDenial())) {
            score += 15;
        }
        
        return Math.min(score, 100);
    }

    /**
     * Calculate insurance type specific risk
     */
    private int calculateTypeSpecificRisk(CustomerRiskProfile riskProfile) {
        int risk = 0;
        
        switch (riskProfile.getInsuranceType().toLowerCase()) {
            case "auto":
                if (Boolean.TRUE.equals(riskProfile.getDui())) {
                    risk += 20;
                }
                if (riskProfile.getDrivingViolations() != null) {
                    risk += Math.min(riskProfile.getDrivingViolations() * 5, 15);
                }
                if (riskProfile.getAtFaultAccidents() != null) {
                    risk += Math.min(riskProfile.getAtFaultAccidents() * 7, 20);
                }
                break;
                
            case "health":
            case "life":
                if (Boolean.TRUE.equals(riskProfile.getSmoker())) {
                    risk += 15;
                }
                if (riskProfile.getMedicalConditions() != null) {
                    risk += Math.min(riskProfile.getMedicalConditions().size() * 5, 20);
                }
                break;
                
            case "home":
                if (Boolean.TRUE.equals(riskProfile.getInFloodZone())) {
                    risk += 10;
                }
                if (riskProfile.getPropertyAge() != null && riskProfile.getPropertyAge() > 50) {
                    risk += 10;
                }
                if (Boolean.FALSE.equals(riskProfile.getHasSecuritySystem())) {
                    risk += 5;
                }
                break;
        }
        
        return Math.min(risk, 20);
    }

    /**
     * Initialize decision object
     */
    private UnderwritingDecision initializeDecision(CustomerRiskProfile riskProfile) {
        return UnderwritingDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .customerId(riskProfile.getCustomerId())
                .decisionDate(LocalDateTime.now())
                .riskFactors(new ArrayList<>())
                .positiveFactors(new ArrayList<>())
                .exclusions(new ArrayList<>())
                .conditions(new ArrayList<>())
                .complianceIssues(new ArrayList<>())
                .build();
    }

    /**
     * Perform compliance check
     */
    private void performComplianceCheck(UnderwritingDecision decision) {
        boolean compliancePassed = true;
        
        // Check for required fields
        if (decision.getDecision() == null) {
            decision.getComplianceIssues().add("Decision is null");
            compliancePassed = false;
        }
        
        if (decision.getRiskScore() == null || decision.getRiskScore() < 0 || decision.getRiskScore() > 100) {
            decision.getComplianceIssues().add("Invalid risk score");
            compliancePassed = false;
        }
        
        // Log compliance check
        logger.info("Compliance check: {}", compliancePassed ? "PASSED" : "FAILED");
        decision.setComplianceCheckPassed(compliancePassed);
    }

    /**
     * Finalize decision with additional details
     */
    private void finalizeDecision(UnderwritingDecision decision) {
        // Set default terms if not set
        if (decision.getTerms() == null) {
            decision.setTerms(generateDefaultTerms(decision));
        }
        
        // Set confidence score
        if (decision.getConfidenceScore() == null) {
            decision.setConfidenceScore(calculateConfidenceScore(decision));
        }
    }

    /**
     * Generate default policy terms
     */
    private String generateDefaultTerms(UnderwritingDecision decision) {
        if ("REJECT".equals(decision.getDecision())) {
            return "Application rejected";
        } else if ("REFER".equals(decision.getDecision())) {
            return "Pending manual review";
        } else {
            StringBuilder terms = new StringBuilder("Standard policy terms");
            if (decision.getExclusions() != null && !decision.getExclusions().isEmpty()) {
                terms.append(" with ").append(decision.getExclusions().size()).append(" exclusion(s)");
            }
            if (decision.getConditions() != null && !decision.getConditions().isEmpty()) {
                terms.append(", ").append(decision.getConditions().size()).append(" condition(s) apply");
            }
            return terms.toString();
        }
    }

    /**
     * Calculate confidence score for the decision
     */
    private double calculateConfidenceScore(UnderwritingDecision decision) {
        if ("RULES_ENGINE".equals(decision.getDecisionMethod())) {
            return 0.95; // High confidence for rules-based decisions
        } else if ("MACHINE_LEARNING".equals(decision.getDecisionMethod())) {
            return 0.85; // Good confidence for ML
        } else {
            return 0.75; // Moderate confidence for standard assessment
        }
    }

    /**
     * Create error decision for failures
     */
    private UnderwritingDecision createErrorDecision(CustomerRiskProfile riskProfile, String errorMessage) {
        return UnderwritingDecision.builder()
                .decisionId(UUID.randomUUID().toString())
                .customerId(riskProfile.getCustomerId())
                .decisionDate(LocalDateTime.now())
                .decision("REFER")
                .referralReason("System error: " + errorMessage)
                .requiresManualReview(true)
                .riskLevel("UNKNOWN")
                .decisionMethod("ERROR_FALLBACK")
                .complianceCheckPassed(false)
                .build();
    }

    /**
     * Log compliance event for audit trail
     */
    private void logComplianceEvent(CustomerRiskProfile riskProfile) {
        logger.info("COMPLIANCE LOG - Underwriting started for customer: {}, Type: {}, Credit Score: {}", 
                encryptionUtil.maskSensitiveData(riskProfile.getCustomerId()),
                riskProfile.getInsuranceType(),
                riskProfile.getCreditScore());
    }
}

