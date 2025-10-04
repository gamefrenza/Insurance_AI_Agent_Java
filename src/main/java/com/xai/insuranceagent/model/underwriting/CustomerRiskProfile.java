package com.xai.insuranceagent.model.underwriting;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Customer risk profile for underwriting assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRiskProfile implements Serializable {

    @NotNull(message = "Customer ID is required")
    private String customerId;

    // Credit information
    @NotNull(message = "Credit score is required")
    @Min(value = 300, message = "Credit score must be at least 300")
    @Max(value = 850, message = "Credit score must be at most 850")
    private Integer creditScore;

    // Claims history
    @Builder.Default
    private Integer claimsInLast3Years = 0;

    @Builder.Default
    private Double totalClaimAmount = 0.0;

    private LocalDate lastClaimDate;

    // Driving record (for auto insurance)
    private Integer drivingViolations;
    
    private Integer atFaultAccidents;
    
    private Boolean dui; // Driving Under Influence

    private Integer yearsLicensed;

    // Insurance history
    private Integer yearsWithPreviousInsurer;
    
    private Boolean previousCancellation;
    
    private Boolean previousDenial;

    // Financial information
    private Double annualIncome;
    
    private String employmentStatus;

    // Property information (for home insurance)
    private Integer propertyAge;
    
    private Boolean hasSecuritySystem;
    
    private Boolean inFloodZone;

    // Health information (for health/life insurance)
    private Boolean smoker;
    
    private List<String> medicalConditions;
    
    private String occupation;

    // General risk factors
    @NotBlank(message = "Insurance type is required")
    @Pattern(regexp = "auto|home|life|health", message = "Insurance type must be one of: auto, home, life, health")
    private String insuranceType;

    private Integer age;

    private String address;

    // External data flags
    @Builder.Default
    private Boolean externalCreditCheckCompleted = false;

    @Builder.Default
    private Boolean drivingRecordCheckCompleted = false;
}

