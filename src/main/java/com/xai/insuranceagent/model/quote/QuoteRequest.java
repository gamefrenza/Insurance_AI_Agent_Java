package com.xai.insuranceagent.model.quote;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Quote request model for insurance quoting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequest implements Serializable {

    @NotNull(message = "Customer age is required")
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 100, message = "Age must be at most 100")
    private Integer age;

    @Pattern(regexp = "male|female|other", message = "Gender must be one of: male, female, other")
    private String gender;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Insurance type is required")
    @Pattern(regexp = "auto|home|life|health", message = "Insurance type must be one of: auto, home, life, health")
    private String insuranceType;

    // Auto insurance specific fields
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleMake;

    // Health insurance specific fields
    private Boolean smoker;
    private String medicalHistory;

    // Home insurance specific fields
    private String propertyType;
    private Double propertyValue;

    // Life insurance specific fields
    private String occupation;

    // Additional fields
    private String name;
    private String email;
    private String phone;

    // City flag for location-based pricing
    @Builder.Default
    private Boolean isUrbanArea = false;
}

