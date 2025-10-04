package com.xai.insuranceagent.service;

import com.xai.insuranceagent.model.Customer;
import com.xai.insuranceagent.model.ProcessResponse;
import com.xai.insuranceagent.util.OpenAIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating insurance quotes
 */
@Service
public class QuotingService {

    private static final Logger logger = LoggerFactory.getLogger(QuotingService.class);

    @Autowired
    private OpenAIClient openAIClient;

    @Value("${insurance.quoting.base-rate.auto}")
    private double autoBaseRate;

    @Value("${insurance.quoting.base-rate.home}")
    private double homeBaseRate;

    @Value("${insurance.quoting.base-rate.life}")
    private double lifeBaseRate;

    @Value("${insurance.quoting.base-rate.health}")
    private double healthBaseRate;

    private final Map<String, Double> baseRates = new HashMap<>();

    /**
     * Generates a quote for the customer
     */
    public ProcessResponse.QuoteResult generateQuote(Customer customer) {
        logger.info("Generating quote for customer - Type: {}, Age: {}", 
                customer.getInsuranceType(), customer.getAge());

        try {
            // Calculate base premium
            double basePremium = getBaseRate(customer.getInsuranceType());
            
            // Apply risk factors
            double adjustedPremium = applyRiskFactors(customer, basePremium);
            
            // Get AI insights for coverage details
            String coverageDetails = generateCoverageDetails(customer, adjustedPremium);
            
            // Calculate validity period
            String validUntil = LocalDate.now().plusDays(30)
                    .format(DateTimeFormatter.ISO_DATE);

            logger.info("Quote generated successfully - Premium: ${}", adjustedPremium);

            return ProcessResponse.QuoteResult.builder()
                    .premiumAmount(Math.round(adjustedPremium * 100.0) / 100.0)
                    .currency("USD")
                    .coverageDetails(coverageDetails)
                    .validUntil(validUntil)
                    .build();

        } catch (Exception e) {
            logger.error("Error generating quote: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate quote", e);
        }
    }

    private double getBaseRate(String insuranceType) {
        return switch (insuranceType.toLowerCase()) {
            case "auto" -> autoBaseRate;
            case "home" -> homeBaseRate;
            case "life" -> lifeBaseRate;
            case "health" -> healthBaseRate;
            default -> 1000.0;
        };
    }

    private double applyRiskFactors(Customer customer, double basePremium) {
        double multiplier = 1.0;
        
        // Age factor
        if (customer.getAge() < 25) {
            multiplier += 0.3;
        } else if (customer.getAge() > 65) {
            multiplier += 0.2;
        }
        
        // Insurance type specific factors
        if ("auto".equals(customer.getInsuranceType()) && customer.getVehicle() != null) {
            if (customer.getVehicle().toLowerCase().contains("tesla") || 
                customer.getVehicle().toLowerCase().contains("sports")) {
                multiplier += 0.15;
            }
        }
        
        if ("life".equals(customer.getInsuranceType()) && customer.getSmoker() != null) {
            if (customer.getSmoker()) {
                multiplier += 0.5;
            }
        }
        
        return basePremium * multiplier;
    }

    private String generateCoverageDetails(Customer customer, double premium) {
        String systemPrompt = """
                You are an insurance expert. Generate a concise coverage details summary 
                for an insurance quote. Include key coverage points in 2-3 sentences.
                Be specific and professional.
                """;

        String userPrompt = String.format("""
                Generate coverage details for:
                Insurance Type: %s
                Customer Age: %d
                Premium Amount: $%.2f
                Additional Info: %s
                """,
                customer.getInsuranceType(),
                customer.getAge(),
                premium,
                getAdditionalInfo(customer));

        try {
            return openAIClient.chat(systemPrompt, userPrompt);
        } catch (Exception e) {
            logger.warn("Failed to get AI coverage details, using default", e);
            return getDefaultCoverage(customer.getInsuranceType());
        }
    }

    private String getAdditionalInfo(Customer customer) {
        return switch (customer.getInsuranceType().toLowerCase()) {
            case "auto" -> "Vehicle: " + customer.getVehicle();
            case "home" -> "Property Type: " + customer.getPropertyType();
            case "life" -> "Occupation: " + customer.getOccupation();
            case "health" -> "Medical History: " + customer.getMedicalHistory();
            default -> "Standard coverage";
        };
    }

    private String getDefaultCoverage(String insuranceType) {
        return switch (insuranceType.toLowerCase()) {
            case "auto" -> "Comprehensive auto coverage including liability, collision, and comprehensive protection.";
            case "home" -> "Full home protection covering structure, contents, and liability.";
            case "life" -> "Term life insurance with flexible coverage options and beneficiary protection.";
            case "health" -> "Comprehensive health coverage including hospitalization and outpatient care.";
            default -> "Standard insurance coverage with comprehensive protection.";
        };
    }
}

