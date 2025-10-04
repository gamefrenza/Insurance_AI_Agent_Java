package com.xai.insuranceagent.service;

import com.xai.insuranceagent.client.DocuSignClient;
import com.xai.insuranceagent.model.document.DocumentRequest;
import com.xai.insuranceagent.model.document.DocumentResponse;
import com.xai.insuranceagent.util.EncryptionUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Document Filling Service with PDF processing and electronic signature
 */
@Service
public class EnhancedDocumentFillingService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDocumentFillingService.class);

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private DocuSignClient docuSignClient;

    @Value("${insurance.document.output-directory}")
    private String outputDirectory;

    @Value("${insurance.document.template-directory}")
    private String templateDirectory;

    @Value("${insurance.document.use-docusign}")
    private boolean useDocuSign;

    /**
     * Fill document with customer and policy data
     */
    public DocumentResponse fillDocument(DocumentRequest request) {
        logger.info("Filling document - Type: {}, Customer: {}", 
                request.getDocumentType(), request.getCustomerId());

        try {
            // Create output directory if not exists
            ensureOutputDirectory();

            // Get template path
            String templatePath = getTemplatePath(request);

            // Generate output file path
            String outputPath = generateOutputPath(request);

            // Fill PDF document
            int fieldsFilled = fillPDFDocument(templatePath, outputPath, request);

            // Get file size
            File outputFile = new File(outputPath);
            long fileSize = outputFile.length();

            // Encrypt if required
            String encryptedPath = null;
            if (request.getEncryptOutput()) {
                encryptedPath = encryptDocument(outputPath);
            }

            // Count pages
            int pageCount = countPDFPages(outputPath);

            // Prepare base response
            DocumentResponse.DocumentResponseBuilder responseBuilder = DocumentResponse.builder()
                    .documentId(generateDocumentId(request))
                    .customerId(request.getCustomerId())
                    .documentType(request.getDocumentType())
                    .fieldsFilled(fieldsFilled)
                    .pageCount(pageCount)
                    .fileSize(fileSize)
                    .fileName(new File(outputPath).getName())
                    .encrypted(request.getEncryptOutput())
                    .status("SUCCESS");

            // Add output based on format
            if (request.getOutputFormat() == DocumentRequest.OutputFormat.FILE_PATH ||
                request.getOutputFormat() == DocumentRequest.OutputFormat.BOTH) {
                responseBuilder.filePath(outputPath);
                if (encryptedPath != null) {
                    responseBuilder.encryptedFilePath(encryptedPath);
                }
            }

            if (request.getOutputFormat() == DocumentRequest.OutputFormat.BASE64 ||
                request.getOutputFormat() == DocumentRequest.OutputFormat.BOTH) {
                String base64 = encodeToBase64(outputPath);
                responseBuilder.base64Content(base64);
            }

            // Handle electronic signature
            if (request.getRequireSignature() && useDocuSign) {
                handleElectronicSignature(request, outputPath, responseBuilder);
            } else if (request.getRequireSignature()) {
                responseBuilder.signatureStatus(DocumentResponse.SignatureStatus.NOT_REQUIRED)
                        .message("Document filled successfully. Electronic signature disabled.");
            }

            DocumentResponse response = responseBuilder.build();

            logger.info("Document filled successfully - ID: {}, Fields: {}, Pages: {}", 
                    response.getDocumentId(), fieldsFilled, pageCount);

            return response;

        } catch (Exception e) {
            logger.error("Error filling document: {}", e.getMessage(), e);
            return DocumentResponse.builder()
                    .customerId(request.getCustomerId())
                    .status("FAILED")
                    .message("Document filling failed: " + e.getMessage())
                    .errors(Arrays.asList(e.getMessage()))
                    .build();
        }
    }

    /**
     * Fill document asynchronously
     */
    public CompletableFuture<DocumentResponse> fillDocumentAsync(DocumentRequest request) {
        logger.info("Starting async document filling for customer: {}", request.getCustomerId());
        
        return CompletableFuture.supplyAsync(() -> fillDocument(request))
                .exceptionally(throwable -> {
                    logger.error("Async document filling failed: {}", throwable.getMessage());
                    return DocumentResponse.builder()
                            .customerId(request.getCustomerId())
                            .status("FAILED")
                            .message("Async document filling failed: " + throwable.getMessage())
                            .build();
                });
    }

    /**
     * Fill PDF document with data
     */
    private int fillPDFDocument(String templatePath, String outputPath, DocumentRequest request) 
            throws IOException {
        
        int fieldsFilled = 0;
        
        try (PDDocument document = PDDocument.load(new File(templatePath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            
            if (acroForm != null) {
                // Fill form fields
                fieldsFilled = fillFormFields(acroForm, request);
            } else {
                // No form fields, add text to pages
                logger.warn("No form fields found in template, adding text directly");
                fieldsFilled = addTextToPages(document, request);
            }
            
            // Flatten the form (make fields non-editable)
            if (acroForm != null) {
                acroForm.flatten();
            }
            
            // Save filled document
            document.save(outputPath);
            
            logger.debug("PDF document filled and saved to: {}", outputPath);
        }
        
        return fieldsFilled;
    }

    /**
     * Fill PDF form fields
     */
    private int fillFormFields(PDAcroForm acroForm, DocumentRequest request) throws IOException {
        int fieldsFilled = 0;
        Map<String, String> fieldMappings = buildFieldMappings(request);
        
        for (PDField field : acroForm.getFields()) {
            String fieldName = field.getFullyQualifiedName();
            String value = fieldMappings.get(fieldName.toLowerCase());
            
            if (value != null) {
                field.setValue(value);
                fieldsFilled++;
                logger.debug("Filled field: {} = {}", fieldName, 
                        encryptionUtil.maskSensitiveData(value));
            }
        }
        
        return fieldsFilled;
    }

    /**
     * Add text directly to PDF pages (for templates without form fields)
     */
    private int addTextToPages(PDDocument document, DocumentRequest request) throws IOException {
        int fieldsAdded = 0;
        
        // Add customer info to first page
        PDPage firstPage = document.getPage(0);
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, firstPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(50, 700);
            
            // Add customer data
            DocumentRequest.CustomerData customer = request.getCustomerData();
            contentStream.showText("Customer Name: " + customer.getName());
            contentStream.newLineAtOffset(0, -20);
            fieldsAdded++;
            
            contentStream.showText("Age: " + customer.getAge());
            contentStream.newLineAtOffset(0, -20);
            fieldsAdded++;
            
            contentStream.showText("Address: " + customer.getAddress());
            contentStream.newLineAtOffset(0, -20);
            fieldsAdded++;
            
            if (customer.getEmail() != null) {
                contentStream.showText("Email: " + customer.getEmail());
                contentStream.newLineAtOffset(0, -20);
                fieldsAdded++;
            }
            
            // Add policy data if available
            if (request.getPolicyData() != null) {
                DocumentRequest.PolicyData policy = request.getPolicyData();
                contentStream.newLineAtOffset(0, -20);
                
                if (policy.getPolicyNumber() != null) {
                    contentStream.showText("Policy Number: " + policy.getPolicyNumber());
                    contentStream.newLineAtOffset(0, -20);
                    fieldsAdded++;
                }
                
                if (policy.getPremiumAmount() != null) {
                    contentStream.showText("Premium: $" + policy.getPremiumAmount());
                    fieldsAdded++;
                }
            }
            
            contentStream.endText();
        }
        
        return fieldsAdded;
    }

    /**
     * Build field mappings from request data
     */
    private Map<String, String> buildFieldMappings(DocumentRequest request) {
        Map<String, String> mappings = new HashMap<>();
        
        DocumentRequest.CustomerData customer = request.getCustomerData();
        
        // Standard customer field mappings
        mappings.put("name", customer.getName());
        mappings.put("customername", customer.getName());
        mappings.put("fullname", customer.getName());
        
        mappings.put("age", String.valueOf(customer.getAge()));
        
        mappings.put("address", customer.getAddress());
        mappings.put("customeraddress", customer.getAddress());
        
        if (customer.getEmail() != null) {
            mappings.put("email", customer.getEmail());
            mappings.put("customeremail", customer.getEmail());
        }
        
        if (customer.getPhone() != null) {
            mappings.put("phone", customer.getPhone());
            mappings.put("telephone", customer.getPhone());
        }
        
        if (customer.getOccupation() != null) {
            mappings.put("occupation", customer.getOccupation());
        }
        
        if (customer.getGender() != null) {
            mappings.put("gender", customer.getGender());
        }
        
        if (customer.getDateOfBirth() != null) {
            mappings.put("dob", customer.getDateOfBirth());
            mappings.put("dateofbirth", customer.getDateOfBirth());
        }
        
        // Policy data mappings
        if (request.getPolicyData() != null) {
            DocumentRequest.PolicyData policy = request.getPolicyData();
            
            if (policy.getPolicyNumber() != null) {
                mappings.put("policynumber", policy.getPolicyNumber());
                mappings.put("policyno", policy.getPolicyNumber());
            }
            
            if (policy.getPolicyType() != null) {
                mappings.put("policytype", policy.getPolicyType());
            }
            
            if (policy.getPremiumAmount() != null) {
                mappings.put("premium", String.valueOf(policy.getPremiumAmount()));
                mappings.put("premiumamount", String.valueOf(policy.getPremiumAmount()));
            }
            
            if (policy.getEffectiveDate() != null) {
                mappings.put("effectivedate", policy.getEffectiveDate());
                mappings.put("startdate", policy.getEffectiveDate());
            }
            
            if (policy.getExpiryDate() != null) {
                mappings.put("expirydate", policy.getExpiryDate());
                mappings.put("enddate", policy.getExpiryDate());
            }
            
            if (policy.getCoverageAmount() != null) {
                mappings.put("coverage", policy.getCoverageAmount());
                mappings.put("coverageamount", policy.getCoverageAmount());
            }
        }
        
        // Add additional fields
        if (customer.getAdditionalFields() != null) {
            customer.getAdditionalFields().forEach((key, value) -> 
                    mappings.put(key.toLowerCase(), value));
        }
        
        if (request.getPolicyData() != null && 
            request.getPolicyData().getAdditionalFields() != null) {
            request.getPolicyData().getAdditionalFields().forEach((key, value) -> 
                    mappings.put(key.toLowerCase(), value));
        }
        
        return mappings;
    }

    /**
     * Handle electronic signature
     */
    private void handleElectronicSignature(DocumentRequest request, String documentPath,
                                           DocumentResponse.DocumentResponseBuilder responseBuilder) {
        try {
            String signerEmail = request.getSignerEmail() != null ? 
                    request.getSignerEmail() : request.getCustomerData().getEmail();
            String signerName = request.getSignerName() != null ? 
                    request.getSignerName() : request.getCustomerData().getName();

            DocuSignClient.SignatureResponse signatureResponse = 
                    docuSignClient.sendForSignature(documentPath, signerEmail, signerName);

            responseBuilder
                    .signatureStatus(mapSignatureStatus(signatureResponse.getStatus()))
                    .signatureRequestId(signatureResponse.getEnvelopeId())
                    .signatureUrl(signatureResponse.getSigningUrl())
                    .status("PENDING_SIGNATURE")
                    .message("Document filled and sent for signature");

            logger.info("Document sent for signature - Envelope: {}", 
                    signatureResponse.getEnvelopeId());

        } catch (Exception e) {
            logger.error("Error sending for signature: {}", e.getMessage(), e);
            responseBuilder
                    .signatureStatus(DocumentResponse.SignatureStatus.PENDING)
                    .message("Document filled successfully, but signature request failed: " + e.getMessage());
        }
    }

    /**
     * Encrypt document file
     */
    private String encryptDocument(String filePath) throws IOException {
        logger.debug("Encrypting document: {}", filePath);
        
        // Read file content
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        
        // Encrypt content
        String base64Content = Base64.getEncoder().encodeToString(fileContent);
        String encryptedContent = encryptionUtil.encrypt(base64Content);
        
        // Save encrypted file
        String encryptedPath = filePath + ".encrypted";
        Files.writeString(Paths.get(encryptedPath), encryptedContent);
        
        logger.info("Document encrypted and saved to: {}", encryptedPath);
        
        return encryptedPath;
    }

    /**
     * Encode file to Base64
     */
    private String encodeToBase64(String filePath) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        return Base64.getEncoder().encodeToString(fileContent);
    }

    /**
     * Count PDF pages
     */
    private int countPDFPages(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.getNumberOfPages();
        }
    }

    /**
     * Get template path
     */
    private String getTemplatePath(DocumentRequest request) {
        if (request.getTemplatePath() != null) {
            return request.getTemplatePath();
        }
        
        // Build default template path
        String templateName = String.format("%s_%s_template.pdf", 
                request.getInsuranceType(), request.getDocumentType());
        
        return Paths.get(templateDirectory, templateName).toString();
    }

    /**
     * Generate output file path
     */
    private String generateOutputPath(DocumentRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%s_%s_%s_%s.pdf", 
                request.getInsuranceType(),
                request.getDocumentType(),
                request.getCustomerId(),
                timestamp);
        
        return Paths.get(outputDirectory, fileName).toString();
    }

    /**
     * Generate document ID
     */
    private String generateDocumentId(DocumentRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String prefix = request.getDocumentType().substring(0, 3).toUpperCase();
        
        return String.format("%s-%s-%s", prefix, timestamp, uniqueId);
    }

    /**
     * Ensure output directory exists
     */
    private void ensureOutputDirectory() throws IOException {
        Path outputPath = Paths.get(outputDirectory);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            logger.info("Created output directory: {}", outputDirectory);
        }
    }

    /**
     * Map signature status
     */
    private DocumentResponse.SignatureStatus mapSignatureStatus(DocuSignClient.SignatureStatus status) {
        return switch (status) {
            case SENT -> DocumentResponse.SignatureStatus.SENT;
            case PENDING -> DocumentResponse.SignatureStatus.PENDING;
            case SIGNED -> DocumentResponse.SignatureStatus.SIGNED;
            case DECLINED -> DocumentResponse.SignatureStatus.DECLINED;
            case EXPIRED -> DocumentResponse.SignatureStatus.EXPIRED;
            default -> DocumentResponse.SignatureStatus.PENDING;
        };
    }
}

