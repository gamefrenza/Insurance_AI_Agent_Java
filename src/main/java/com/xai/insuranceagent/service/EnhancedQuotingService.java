package com.xai.insuranceagent.service;

import com.xai.insuranceagent.client.GuideWireClient;
import com.xai.insuranceagent.model.Customer;
import com.xai.insuranceagent.model.quote.QuoteRequest;
import com.xai.insuranceagent.model.quote.QuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Quoting Service with detailed rule-based calculations
 * Supports external API integration and async processing
 */
@Service
public class EnhancedQuotingService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedQuotingService.class);

    @Autowired
    private GuideWireClient guideWireClient;

    @Value("${insurance.quoting.base-rate.auto}")
    private double autoBaseRate;

    @Value("${insurance.quoting.base-rate.home}")
    private double homeBaseRate;

    @Value("${insurance.quoting.base-rate.life}")
    private double lifeBaseRate;

    @Value("${insurance.quoting.base-rate.health}")
    private double healthBaseRate;

    @Value("${insurance.quoting.use-external-api}")
    private boolean useExternalApi;

    /**
     * Generate quote with detailed rule-based calculation
     */
    public QuoteResponse generateDetailedQuote(QuoteRequest request) {
        logger.info("Generating detailed quote - Type: {}, Age: {}, Gender: {}", 
                request.getInsuranceType(), request.getAge(), request.getGender());

        try {
            // Get base premium
            double basePremium = getBasePremium(request.getInsuranceType());

            // Calculate adjustments
            QuoteResponse.PremiumBreakdown breakdown = calculatePremiumBreakdown(request, basePremium);

            // Calculate total premium
            double totalPremium = basePremium + breakdown.getTotalAdjustment();
            totalPremium = Math.round(totalPremium * 100.0) / 100.0;

            // Determine deductible
            int deductible = calculateDeductible(request.getInsuranceType(), totalPremium);

            // Generate coverage details
            String coverageDetails = generateCoverageDetails(request);

            // Build response
            QuoteResponse response = QuoteResponse.builder()
                    .premium(totalPremium)
                    .totalPremium(totalPremium)
                    .currency("USD")
                    .coverage(getCoverageType(request.getInsuranceType()))
                    .coverageDetails(coverageDetails)
                    .deductible(deductible)
                    .policyTerm("12 months")
                    .quotedAt(LocalDateTime.now())
                    .validUntil(LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_DATE))
                    .quoteId(generateQuoteId(request.getInsuranceType()))
                    .breakdown(breakdown)
                    .build();

            logger.info("Quote generated - ID: {}, Premium: ${}", response.getQuoteId(), totalPremium);

            return response;

        } catch (Exception e) {
            logger.error("Error generating detailed quote: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate quote", e);
        }
    }

    /**
     * Generate quote asynchronously using external API
     */
    public CompletableFuture<QuoteResponse> generateQuoteAsync(QuoteRequest request) {
        logger.info("Generating quote asynchronously for: {}", request.getInsuranceType());

        if (useExternalApi) {
            return guideWireClient.getQuoteAsync(request)
                    .thenApply(externalQuote -> enhanceExternalQuote(request, externalQuote))
                    .exceptionally(throwable -> {
                        logger.error("External API failed, falling back to local calculation", throwable);
                        return generateDetailedQuote(request);
                    });
        } else {
            return CompletableFuture.supplyAsync(() -> generateDetailedQuote(request));
        }
    }

    /**
     * Calculate premium breakdown with all factors
     */
    private QuoteResponse.PremiumBreakdown calculatePremiumBreakdown(QuoteRequest request, double basePremium) {
        double ageFactor = calculateAgeFactor(request.getAge(), basePremium);
        double genderFactor = calculateGenderFactor(request.getGender(), request.getInsuranceType(), basePremium);
        double locationFactor = calculateLocationFactor(request.getAddress(), request.getIsUrbanArea(), basePremium);
        double riskFactor = calculateRiskFactor(request, basePremium);

        double totalAdjustment = ageFactor + genderFactor + locationFactor + riskFactor;
        
        String notes = buildCalculationNotes(request, ageFactor, genderFactor, locationFactor, riskFactor);

        return QuoteResponse.PremiumBreakdown.builder()
                .basePremium(basePremium)
                .ageFactor(Math.round(ageFactor * 100.0) / 100.0)
                .genderFactor(Math.round(genderFactor * 100.0) / 100.0)
                .locationFactor(Math.round(locationFactor * 100.0) / 100.0)
                .riskFactor(Math.round(riskFactor * 100.0) / 100.0)
                .totalAdjustment(Math.round(totalAdjustment * 100.0) / 100.0)
                .calculationNotes(notes)
                .build();
    }

    /**
     * Calculate age-based premium adjustment
     * Auto insurance: age < 25 +20%, age > 65 +15%
     */
    private double calculateAgeFactor(Integer age, double basePremium) {
        if (age < 25) {
            logger.debug("Age factor: Young driver surcharge (+20%)");
            return basePremium * 0.20;
        } else if (age > 65) {
            logger.debug("Age factor: Senior driver surcharge (+15%)");
            return basePremium * 0.15;
        } else if (age >= 25 && age <= 35) {
            logger.debug("Age factor: Discount for optimal age group (-5%)");
            return basePremium * -0.05;
        }
        return 0.0;
    }

    /**
     * Calculate gender-based premium adjustment
     */
    private double calculateGenderFactor(String gender, String insuranceType, double basePremium) {
        if (gender == null) {
            return 0.0;
        }

        // Auto insurance: male drivers typically pay more
        if ("auto".equals(insuranceType) && "male".equals(gender.toLowerCase())) {
            logger.debug("Gender factor: Male driver surcharge (+8%)");
            return basePremium * 0.08;
        }

        return 0.0;
    }

    /**
     * Calculate location-based premium adjustment
     * City address +10%
     */
    private double calculateLocationFactor(String address, Boolean isUrbanArea, double basePremium) {
        if (isUrbanArea != null && isUrbanArea) {
            logger.debug("Location factor: Urban area surcharge (+10%)");
            return basePremium * 0.10;
        }

        // Check address for major cities
        if (address != null) {
            String lowerAddress = address.toLowerCase();
            if (lowerAddress.contains("beijing") || lowerAddress.contains("shanghai") ||
                lowerAddress.contains("guangzhou") || lowerAddress.contains("shenzhen") ||
                lowerAddress.contains("new york") || lowerAddress.contains("los angeles")) {
                logger.debug("Location factor: Major city surcharge (+10%)");
                return basePremium * 0.10;
            }
        }

        return 0.0;
    }

    /**
     * Calculate risk-based premium adjustment
     * Health insurance: smoker=true +15%
     * Auto insurance: vehicle model factors
     */
    private double calculateRiskFactor(QuoteRequest request, double basePremium) {
        double riskAdjustment = 0.0;

        switch (request.getInsuranceType().toLowerCase()) {
            case "health":
                if (request.getSmoker() != null && request.getSmoker()) {
                    logger.debug("Risk factor: Smoker surcharge (+15%)");
                    riskAdjustment += basePremium * 0.15;
                }
                break;

            case "auto":
                if (request.getVehicleModel() != null) {
                    String vehicle = request.getVehicleModel().toLowerCase();
                    if (vehicle.contains("sports") || vehicle.contains("ferrari") || 
                        vehicle.contains("lamborghini") || vehicle.contains("porsche")) {
                        logger.debug("Risk factor: High-performance vehicle (+25%)");
                        riskAdjustment += basePremium * 0.25;
                    } else if (vehicle.contains("tesla") || vehicle.contains("bmw") || 
                               vehicle.contains("mercedes")) {
                        logger.debug("Risk factor: Luxury vehicle (+12%)");
                        riskAdjustment += basePremium * 0.12;
                    } else if (vehicle.contains("suv") || vehicle.contains("truck")) {
                        logger.debug("Risk factor: Large vehicle (+8%)");
                        riskAdjustment += basePremium * 0.08;
                    }
                }
                break;

            case "home":
                if (request.getPropertyValue() != null && request.getPropertyValue() > 1000000) {
                    logger.debug("Risk factor: High-value property (+18%)");
                    riskAdjustment += basePremium * 0.18;
                }
                break;

            case "life":
                if (request.getOccupation() != null) {
                    String occupation = request.getOccupation().toLowerCase();
                    if (occupation.contains("pilot") || occupation.contains("miner") || 
                        occupation.contains("construction")) {
                        logger.debug("Risk factor: High-risk occupation (+20%)");
                        riskAdjustment += basePremium * 0.20;
                    }
                }
                break;
        }

        return riskAdjustment;
    }

    private double getBasePremium(String insuranceType) {
        return switch (insuranceType.toLowerCase()) {
            case "auto" -> autoBaseRate;
            case "home" -> homeBaseRate;
            case "life" -> lifeBaseRate;
            case "health" -> healthBaseRate;
            default -> 1000.0;
        };
    }

    private int calculateDeductible(String insuranceType, double premium) {
        return switch (insuranceType.toLowerCase()) {
            case "auto" -> premium > 2000 ? 1000 : 500;
            case "home" -> premium > 1500 ? 750 : 500;
            case "health" -> premium > 1000 ? 500 : 250;
            default -> 500;
        };
    }

    private String getCoverageType(String insuranceType) {
        return switch (insuranceType.toLowerCase()) {
            case "auto" -> "comprehensive";
            case "home" -> "full-coverage";
            case "life" -> "term-life";
            case "health" -> "comprehensive";
            default -> "standard";
        };
    }

    private String generateCoverageDetails(QuoteRequest request) {
        return switch (request.getInsuranceType().toLowerCase()) {
            case "auto" -> String.format("Comprehensive auto coverage including liability, collision, and theft protection for %s. Roadside assistance included.",
                    request.getVehicleModel() != null ? request.getVehicleModel() : "your vehicle");
            case "home" -> "Full home protection covering structure, contents, liability, and natural disasters.";
            case "life" -> "Term life insurance with flexible coverage options and beneficiary protection.";
            case "health" -> "Comprehensive health coverage including hospitalization, outpatient care, and preventive services.";
            default -> "Standard insurance coverage with comprehensive protection.";
        };
    }

    private String generateQuoteId(String insuranceType) {
        String prefix = switch (insuranceType.toLowerCase()) {
            case "auto" -> "AUT";
            case "home" -> "HOM";
            case "life" -> "LIF";
            case "health" -> "HLT";
            default -> "INS";
        };
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        return String.format("%s-%s-%s", prefix, timestamp, uniqueId);
    }

    private String buildCalculationNotes(QuoteRequest request, double ageFactor, 
                                         double genderFactor, double locationFactor, double riskFactor) {
        StringBuilder notes = new StringBuilder("Premium calculated based on: ");
        
        if (ageFactor != 0) {
            notes.append("age adjustment ($").append(String.format("%.2f", ageFactor)).append("), ");
        }
        if (genderFactor != 0) {
            notes.append("gender factor ($").append(String.format("%.2f", genderFactor)).append("), ");
        }
        if (locationFactor != 0) {
            notes.append("location surcharge ($").append(String.format("%.2f", locationFactor)).append("), ");
        }
        if (riskFactor != 0) {
            notes.append("risk factors ($").append(String.format("%.2f", riskFactor)).append("), ");
        }
        
        if (notes.toString().endsWith(", ")) {
            notes.setLength(notes.length() - 2);
        }
        
        return notes.toString();
    }

    private QuoteResponse enhanceExternalQuote(QuoteRequest request, QuoteResponse externalQuote) {
        // Enhance external quote with our breakdown
        QuoteResponse.PremiumBreakdown breakdown = calculatePremiumBreakdown(
                request, externalQuote.getPremium());
        
        externalQuote.setBreakdown(breakdown);
        return externalQuote;
    }

    /**
     * Convert Customer to QuoteRequest
     */
    public QuoteRequest convertCustomerToQuoteRequest(Customer customer) {
        return QuoteRequest.builder()
                .age(customer.getAge())
                .gender(customer.getGender())
                .address(customer.getAddress())
                .insuranceType(customer.getInsuranceType())
                .vehicleModel(customer.getVehicle())
                .smoker(customer.getSmoker())
                .medicalHistory(customer.getMedicalHistory())
                .propertyType(customer.getPropertyType())
                .propertyValue(customer.getPropertyValue())
                .occupation(customer.getOccupation())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .isUrbanArea(isUrbanArea(customer.getAddress()))
                .build();
    }

    private boolean isUrbanArea(String address) {
        if (address == null) return false;
        String lower = address.toLowerCase();
        return lower.contains("beijing") || lower.contains("shanghai") ||
               lower.contains("guangzhou") || lower.contains("shenzhen") ||
               lower.contains("new york") || lower.contains("los angeles") ||
               lower.contains("city");
    }
}

