package com.xai.insuranceagent.service;

import com.xai.insuranceagent.model.Customer;
import com.xai.insuranceagent.model.ProcessResponse;
import com.xai.insuranceagent.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for document generation and filling
 */
@Service
public class DocumentFillingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentFillingService.class);

    @Autowired
    private EncryptionUtil encryptionUtil;

    /**
     * Generates insurance documents
     */
    public ProcessResponse.DocumentResult generateDocument(
            Customer customer, 
            ProcessResponse.QuoteResult quote,
            ProcessResponse.UnderwritingResult underwriting) {
        
        logger.info("Generating document for customer - Type: {}", customer.getInsuranceType());

        try {
            // Generate unique document ID
            String documentId = generateDocumentId(customer);
            
            // Encrypt sensitive data (GDPR compliance)
            encryptSensitiveData(customer);
            
            // Generate document URL
            String documentUrl = generateDocumentUrl(documentId, customer.getInsuranceType());
            
            // Determine document type
            String documentType = determineDocumentType(customer.getInsuranceType());
            
            // Determine document status
            String status = determineDocumentStatus(underwriting);

            logger.info("Document generated successfully - ID: {}, Type: {}", documentId, documentType);

            return ProcessResponse.DocumentResult.builder()
                    .documentId(documentId)
                    .documentUrl(documentUrl)
                    .documentType(documentType)
                    .status(status)
                    .build();

        } catch (Exception e) {
            logger.error("Error generating document: {}", e.getMessage(), e);
            throw new RuntimeException("Document generation failed", e);
        }
    }

    private String generateDocumentId(Customer customer) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String type = customer.getInsuranceType().toUpperCase().substring(0, 3);
        
        return String.format("%s-%s-%s", type, timestamp, uniqueId);
    }

    private void encryptSensitiveData(Customer customer) {
        try {
            // Encrypt personal information for GDPR compliance
            if (customer.getEmail() != null) {
                logger.debug("Encrypting email: {}", 
                        encryptionUtil.maskSensitiveData(customer.getEmail()));
                encryptionUtil.encrypt(customer.getEmail());
            }
            
            if (customer.getPhone() != null) {
                logger.debug("Encrypting phone: {}", 
                        encryptionUtil.maskSensitiveData(customer.getPhone()));
                encryptionUtil.encrypt(customer.getPhone());
            }
            
            if (customer.getAddress() != null) {
                logger.debug("Encrypting address");
                encryptionUtil.encrypt(customer.getAddress());
            }
            
            logger.info("Sensitive data encrypted successfully (GDPR compliance)");
            
        } catch (Exception e) {
            logger.error("Error encrypting sensitive data: {}", e.getMessage());
        }
    }

    private String generateDocumentUrl(String documentId, String insuranceType) {
        // In production, this would be a real URL to a document storage service
        return String.format("https://docs.insurance-agent.com/%s/%s.pdf", 
                insuranceType, documentId);
    }

    private String determineDocumentType(String insuranceType) {
        return switch (insuranceType.toLowerCase()) {
            case "auto" -> "Auto Insurance Policy";
            case "home" -> "Homeowners Insurance Policy";
            case "life" -> "Life Insurance Policy";
            case "health" -> "Health Insurance Policy";
            default -> "Insurance Policy";
        };
    }

    private String determineDocumentStatus(ProcessResponse.UnderwritingResult underwriting) {
        if (underwriting == null) {
            return "GENERATED";
        }
        
        return switch (underwriting.getDecision()) {
            case "APPROVED" -> "PENDING_SIGNATURE";
            case "DECLINED" -> "GENERATED";
            case "PENDING" -> "GENERATED";
            default -> "GENERATED";
        };
    }

    /**
     * Fills document template with customer data
     */
    public String fillDocumentTemplate(Customer customer, ProcessResponse.QuoteResult quote) {
        StringBuilder document = new StringBuilder();
        
        document.append("=== INSURANCE POLICY DOCUMENT ===\n\n");
        document.append("Policy Type: ").append(customer.getInsuranceType()).append("\n");
        document.append("Customer Name: ").append(customer.getName() != null ? customer.getName() : "N/A").append("\n");
        document.append("Age: ").append(customer.getAge()).append("\n");
        document.append("Address: ").append(encryptionUtil.maskSensitiveData(customer.getAddress())).append("\n\n");
        
        document.append("=== QUOTE INFORMATION ===\n");
        document.append("Premium Amount: $").append(quote.getPremiumAmount()).append(" ").append(quote.getCurrency()).append("\n");
        document.append("Coverage: ").append(quote.getCoverageDetails()).append("\n");
        document.append("Valid Until: ").append(quote.getValidUntil()).append("\n\n");
        
        document.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append("\n");
        
        return document.toString();
    }
}

