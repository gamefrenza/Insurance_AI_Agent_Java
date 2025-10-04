package com.xai.insuranceagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer implements Serializable {

    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 100, message = "Age must be at most 100")
    private Integer age;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "Insurance type is required")
    @Pattern(regexp = "auto|home|life|health", message = "Insurance type must be one of: auto, home, life, health")
    @JsonProperty("insuranceType")
    private String insuranceType;

    // Additional fields for auto insurance
    private String vehicle;

    // Additional fields for home insurance
    private String propertyType;
    private Double propertyValue;

    // Additional fields for life insurance
    private String occupation;
    private Boolean smoker;

    // Additional fields for health insurance
    private String medicalHistory;

    // Common fields
    private String name;
    private String email;
    private String phone;
}

