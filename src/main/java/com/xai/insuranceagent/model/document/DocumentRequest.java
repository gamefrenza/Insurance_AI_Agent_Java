package com.xai.insuranceagent.model.document;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Document filling request model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest implements Serializable {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "policy|claim|application|quote", 
             message = "Document type must be one of: policy, claim, application, quote")
    private String documentType;

    @NotBlank(message = "Insurance type is required")
    @Pattern(regexp = "auto|home|life|health", 
             message = "Insurance type must be one of: auto, home, life, health")
    private String insuranceType;

    // Customer data for filling
    @NotNull(message = "Customer data is required")
    private CustomerData customerData;

    // Policy data
    private PolicyData policyData;

    // Template configuration
    private String templatePath;

    // Electronic signature request
    @Builder.Default
    private Boolean requireSignature = false;

    private String signerEmail;

    private String signerName;

    // Output format
    @Builder.Default
    private OutputFormat outputFormat = OutputFormat.FILE_PATH;

    @Builder.Default
    private Boolean encryptOutput = true;

    public enum OutputFormat {
        FILE_PATH,
        BASE64,
        BOTH
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerData {
        @NotBlank(message = "Customer name is required")
        private String name;

        @NotNull(message = "Age is required")
        @Min(value = 18, message = "Age must be at least 18")
        private Integer age;

        @NotBlank(message = "Address is required")
        private String address;

        @Email(message = "Valid email is required")
        private String email;

        @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Valid phone number is required")
        private String phone;

        private String occupation;
        private String gender;
        private String dateOfBirth;
        private String nationalId;

        // Additional fields for dynamic filling
        private Map<String, String> additionalFields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyData {
        private String policyNumber;
        private String policyType;
        private Double premiumAmount;
        private String currency;
        private String effectiveDate;
        private String expiryDate;
        private String coverageAmount;
        private String deductible;
        private String terms;

        // Additional policy fields
        private Map<String, String> additionalFields;
    }
}

