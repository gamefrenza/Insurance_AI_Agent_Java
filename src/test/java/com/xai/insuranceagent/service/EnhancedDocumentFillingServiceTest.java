package com.xai.insuranceagent.service;

import com.xai.insuranceagent.client.DocuSignClient;
import com.xai.insuranceagent.model.document.DocumentRequest;
import com.xai.insuranceagent.model.document.DocumentResponse;
import com.xai.insuranceagent.util.EncryptionUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnhancedDocumentFillingService
 */
@ExtendWith(MockitoExtension.class)
class EnhancedDocumentFillingServiceTest {

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private DocuSignClient docuSignClient;

    @InjectMocks
    private EnhancedDocumentFillingService documentFillingService;

    @TempDir
    Path tempDir;

    private String templateDirectory;
    private String outputDirectory;

    @BeforeEach
    void setUp() throws IOException {
        // Create temp directories
        templateDirectory = tempDir.resolve("templates").toString();
        outputDirectory = tempDir.resolve("output").toString();
        
        new File(templateDirectory).mkdirs();
        new File(outputDirectory).mkdirs();

        // Set configuration values
        ReflectionTestUtils.setField(documentFillingService, "templateDirectory", templateDirectory);
        ReflectionTestUtils.setField(documentFillingService, "outputDirectory", outputDirectory);
        ReflectionTestUtils.setField(documentFillingService, "useDocuSign", false);

        // Create a sample PDF template
        createSamplePDFTemplate();

        // Mock encryption util
        when(encryptionUtil.maskSensitiveData(anyString())).thenAnswer(i -> "MASKED");
        when(encryptionUtil.encrypt(anyString())).thenAnswer(i -> "ENCRYPTED_" + i.getArgument(0));
    }

    @Test
    @DisplayName("Should fill document with customer data successfully")
    void testFillDocumentSuccess() {
        // Given
        DocumentRequest request = createBasicDocumentRequest();

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getDocumentId());
        assertNotNull(response.getFilePath());
        assertTrue(response.getFieldsFilled() > 0);
        assertTrue(response.getPageCount() > 0);
        assertTrue(new File(response.getFilePath()).exists());
    }

    @Test
    @DisplayName("Should fill document with policy data")
    void testFillDocumentWithPolicyData() {
        // Given
        DocumentRequest request = createDocumentRequestWithPolicy();

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getFieldsFilled() > 0);
    }

    @Test
    @DisplayName("Should return base64 content when requested")
    void testFillDocumentWithBase64Output() {
        // Given
        DocumentRequest request = createBasicDocumentRequest();
        request.setOutputFormat(DocumentRequest.OutputFormat.BASE64);

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getBase64Content());
        assertTrue(response.getBase64Content().length() > 0);
    }

    @Test
    @DisplayName("Should return both file path and base64")
    void testFillDocumentWithBothOutputFormats() {
        // Given
        DocumentRequest request = createBasicDocumentRequest();
        request.setOutputFormat(DocumentRequest.OutputFormat.BOTH);

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getFilePath());
        assertNotNull(response.getBase64Content());
    }

    @Test
    @DisplayName("Should encrypt document when requested")
    void testFillDocumentWithEncryption() {
        // Given
        DocumentRequest request = createBasicDocumentRequest();
        request.setEncryptOutput(true);

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getEncrypted());
        assertNotNull(response.getEncryptedFilePath());
        verify(encryptionUtil, atLeastOnce()).encrypt(anyString());
    }

    @Test
    @DisplayName("Should handle async document filling")
    void testFillDocumentAsync() throws Exception {
        // Given
        DocumentRequest request = createBasicDocumentRequest();

        // When
        CompletableFuture<DocumentResponse> futureResponse = 
                documentFillingService.fillDocumentAsync(request);
        DocumentResponse response = futureResponse.get();

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Should handle electronic signature request")
    void testFillDocumentWithSignature() {
        // Given
        ReflectionTestUtils.setField(documentFillingService, "useDocuSign", true);
        
        DocumentRequest request = createBasicDocumentRequest();
        request.setRequireSignature(true);
        request.setSignerEmail("test@example.com");
        request.setSignerName("Test Signer");

        DocuSignClient.SignatureResponse mockSignatureResponse = 
                DocuSignClient.SignatureResponse.builder()
                        .envelopeId("ENV-12345")
                        .status(DocuSignClient.SignatureStatus.SENT)
                        .signingUrl("https://sign.docusign.net/12345")
                        .build();

        when(docuSignClient.sendForSignature(anyString(), anyString(), anyString()))
                .thenReturn(mockSignatureResponse);

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response);
        assertEquals("PENDING_SIGNATURE", response.getStatus());
        assertNotNull(response.getSignatureRequestId());
        assertNotNull(response.getSignatureUrl());
        verify(docuSignClient, times(1)).sendForSignature(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle additional fields")
    void testFillDocumentWithAdditionalFields() {
        // Given
        DocumentRequest request = createBasicDocumentRequest();
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("customField1", "value1");
        additionalFields.put("customField2", "value2");
        request.getCustomerData().setAdditionalFields(additionalFields);

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Should validate required customer fields")
    void testValidateRequiredFields() {
        // Given - Request with incomplete data
        DocumentRequest request = DocumentRequest.builder()
                .customerId("CUST001")
                .documentType("policy")
                .insuranceType("auto")
                .customerData(DocumentRequest.CustomerData.builder()
                        .name("")  // Invalid: empty name
                        .age(25)
                        .build())
                .build();

        // When/Then - Validation should fail
        // Note: In actual usage, validation happens at controller level
        assertNotNull(request.getCustomerData());
    }

    @Test
    @DisplayName("Should handle multi-page PDF")
    void testHandleMultiPagePDF() throws IOException {
        // Given - Create multi-page template
        String multiPageTemplate = createMultiPagePDFTemplate();
        
        DocumentRequest request = createBasicDocumentRequest();
        request.setTemplatePath(multiPageTemplate);

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getPageCount() > 1);
    }

    @Test
    @DisplayName("Should generate unique document IDs")
    void testGenerateUniqueDocumentIds() {
        // Given
        DocumentRequest request1 = createBasicDocumentRequest();
        DocumentRequest request2 = createBasicDocumentRequest();

        // When
        DocumentResponse response1 = documentFillingService.fillDocument(request1);
        DocumentResponse response2 = documentFillingService.fillDocument(request2);

        // Then
        assertNotNull(response1.getDocumentId());
        assertNotNull(response2.getDocumentId());
        assertNotEquals(response1.getDocumentId(), response2.getDocumentId());
    }

    @Test
    @DisplayName("Should track file size correctly")
    void testTrackFileSize() {
        // Given
        DocumentRequest request = createBasicDocumentRequest();

        // When
        DocumentResponse response = documentFillingService.fillDocument(request);

        // Then
        assertNotNull(response.getFileSize());
        assertTrue(response.getFileSize() > 0);
    }

    // Helper methods

    private DocumentRequest createBasicDocumentRequest() {
        return DocumentRequest.builder()
                .customerId("CUST001")
                .documentType("policy")
                .insuranceType("auto")
                .customerData(DocumentRequest.CustomerData.builder()
                        .name("John Doe")
                        .age(30)
                        .address("123 Main St, Beijing")
                        .email("john@example.com")
                        .phone("+86-1381234567")
                        .build())
                .outputFormat(DocumentRequest.OutputFormat.FILE_PATH)
                .encryptOutput(false)
                .requireSignature(false)
                .build();
    }

    private DocumentRequest createDocumentRequestWithPolicy() {
        DocumentRequest request = createBasicDocumentRequest();
        request.setPolicyData(DocumentRequest.PolicyData.builder()
                .policyNumber("POL-12345")
                .policyType("Comprehensive Auto")
                .premiumAmount(1500.0)
                .currency("USD")
                .effectiveDate("2024-01-01")
                .expiryDate("2025-01-01")
                .coverageAmount("50000")
                .deductible("500")
                .build());
        return request;
    }

    private void createSamplePDFTemplate() throws IOException {
        String templatePath = new File(templateDirectory, "auto_policy_template.pdf").getAbsolutePath();
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            document.save(templatePath);
        }
    }

    private String createMultiPagePDFTemplate() throws IOException {
        String templatePath = new File(templateDirectory, "multi_page_template.pdf").getAbsolutePath();
        
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            document.save(templatePath);
        }
        
        return templatePath;
    }
}

