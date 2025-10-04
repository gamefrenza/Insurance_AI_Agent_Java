package com.xai.insuranceagent.service;

import com.xai.insuranceagent.model.Customer;
import com.xai.insuranceagent.model.ProcessResponse;
import com.xai.insuranceagent.util.OpenAIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for underwriting decisions
 */
@Service
public class UnderwritingService {

    private static final Logger logger = LoggerFactory.getLogger(UnderwritingService.class);

    @Autowired
    private OpenAIClient openAIClient;

    @Value("${insurance.underwriting.age.min}")
    private int minAge;

    @Value("${insurance.underwriting.age.max}")
    private int maxAge;

    @Value("${insurance.underwriting.risk-factors.high-risk-threshold}")
    private double highRiskThreshold;

    /**
     * Performs underwriting evaluation
     */
    public ProcessResponse.UnderwritingResult performUnderwriting(Customer customer) {
        logger.info("Performing underwriting for customer - Age: {}, Type: {}", 
                customer.getAge(), customer.getInsuranceType());

        try {
            // Calculate risk score
            double riskScore = calculateRiskScore(customer);
            
            // Determine risk level
            String riskLevel = determineRiskLevel(riskScore);
            
            // Make underwriting decision
            String decision = makeDecision(customer, riskScore);
            
            // Get AI reasoning
            String reasoning = generateReasoning(customer, riskScore, decision);

            logger.info("Underwriting completed - Decision: {}, Risk Level: {}, Score: {}", 
                    decision, riskLevel, riskScore);

            return ProcessResponse.UnderwritingResult.builder()
                    .decision(decision)
                    .riskLevel(riskLevel)
                    .reasoning(reasoning)
                    .riskScore(Math.round(riskScore * 100.0) / 100.0)
                    .build();

        } catch (Exception e) {
            logger.error("Error in underwriting process: {}", e.getMessage(), e);
            throw new RuntimeException("Underwriting failed", e);
        }
    }

    private double calculateRiskScore(Customer customer) {
        double score = 0.0;
        
        // Age risk
        if (customer.getAge() < minAge || customer.getAge() > maxAge) {
            score += 0.8;
        } else if (customer.getAge() < 25 || customer.getAge() > 65) {
            score += 0.3;
        } else {
            score += 0.1;
        }
        
        // Insurance type specific risk
        score += calculateTypeSpecificRisk(customer);
        
        // Normalize score to 0-1 range
        return Math.min(score, 1.0);
    }

    private double calculateTypeSpecificRisk(Customer customer) {
        double risk = 0.0;
        
        switch (customer.getInsuranceType().toLowerCase()) {
            case "auto":
                if (customer.getVehicle() != null) {
                    String vehicle = customer.getVehicle().toLowerCase();
                    if (vehicle.contains("sports") || vehicle.contains("racing")) {
                        risk += 0.4;
                    } else if (vehicle.contains("tesla") || vehicle.contains("electric")) {
                        risk += 0.2;
                    } else {
                        risk += 0.1;
                    }
                }
                break;
                
            case "life":
                if (customer.getSmoker() != null && customer.getSmoker()) {
                    risk += 0.5;
                }
                if (customer.getOccupation() != null) {
                    String occupation = customer.getOccupation().toLowerCase();
                    if (occupation.contains("pilot") || occupation.contains("miner")) {
                        risk += 0.3;
                    }
                }
                break;
                
            case "health":
                if (customer.getMedicalHistory() != null && 
                    !customer.getMedicalHistory().equalsIgnoreCase("none")) {
                    risk += 0.3;
                }
                break;
                
            case "home":
                if (customer.getPropertyValue() != null && customer.getPropertyValue() > 1000000) {
                    risk += 0.2;
                }
                break;
        }
        
        return risk;
    }

    private String determineRiskLevel(double riskScore) {
        if (riskScore >= highRiskThreshold) {
            return "HIGH";
        } else if (riskScore >= 0.4) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String makeDecision(Customer customer, double riskScore) {
        // Age validation
        if (customer.getAge() < minAge || customer.getAge() > maxAge) {
            return "DECLINED";
        }
        
        // Risk-based decision
        if (riskScore >= 0.9) {
            return "DECLINED";
        } else if (riskScore >= highRiskThreshold) {
            return "PENDING";
        } else {
            return "APPROVED";
        }
    }

    private String generateReasoning(Customer customer, double riskScore, String decision) {
        String systemPrompt = """
                You are an insurance underwriting expert. Provide a brief, professional 
                explanation for the underwriting decision in 2-3 sentences. 
                Be factual and reference specific risk factors.
                """;

        String userPrompt = String.format("""
                Explain the underwriting decision:
                Decision: %s
                Risk Score: %.2f
                Customer Age: %d
                Insurance Type: %s
                Additional Context: %s
                """,
                decision,
                riskScore,
                customer.getAge(),
                customer.getInsuranceType(),
                getContextInfo(customer));

        try {
            return openAIClient.chat(systemPrompt, userPrompt);
        } catch (Exception e) {
            logger.warn("Failed to get AI reasoning, using default", e);
            return getDefaultReasoning(decision, riskScore);
        }
    }

    private String getContextInfo(Customer customer) {
        StringBuilder context = new StringBuilder();
        if (customer.getVehicle() != null) {
            context.append("Vehicle: ").append(customer.getVehicle()).append(". ");
        }
        if (customer.getSmoker() != null) {
            context.append("Smoker: ").append(customer.getSmoker()).append(". ");
        }
        if (customer.getOccupation() != null) {
            context.append("Occupation: ").append(customer.getOccupation()).append(". ");
        }
        return context.toString();
    }

    private String getDefaultReasoning(String decision, double riskScore) {
        return switch (decision) {
            case "APPROVED" -> String.format("Application approved based on favorable risk assessment (score: %.2f). Customer meets all underwriting criteria.", riskScore);
            case "DECLINED" -> String.format("Application declined due to high risk factors (score: %.2f) or age restrictions.", riskScore);
            case "PENDING" -> String.format("Application requires additional review due to moderate risk factors (score: %.2f).", riskScore);
            default -> "Standard underwriting evaluation completed.";
        };
    }
}

