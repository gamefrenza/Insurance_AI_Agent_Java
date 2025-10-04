package com.xai.insuranceagent.model.document;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Document filling response model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentResponse implements Serializable {

    private String documentId;

    private String customerId;

    private String documentType;

    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();

    // Output data
    private String filePath;

    private String base64Content;

    private String encryptedFilePath;

    // Document metadata
    private Integer pageCount;

    private Integer fieldsFilled;

    private Long fileSize; // in bytes

    private String fileName;

    // Electronic signature
    private SignatureStatus signatureStatus;

    private String signatureRequestId;

    private String signatureUrl;

    // Status
    private String status; // SUCCESS, PENDING_SIGNATURE, FAILED

    private String message;

    private List<String> errors;

    // Compliance
    @Builder.Default
    private Boolean encrypted = false;

    @Builder.Default
    private Boolean complianceCheckPassed = true;

    public enum SignatureStatus {
        NOT_REQUIRED,
        PENDING,
        SENT,
        SIGNED,
        DECLINED,
        EXPIRED
    }
}

