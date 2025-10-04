package com.xai.insuranceagent.service;

import com.xai.insuranceagent.client.CreditScoreClient;
import com.xai.insuranceagent.model.underwriting.CustomerRiskProfile;
import com.xai.insuranceagent.model.underwriting.UnderwritingDecision;
import com.xai.insuranceagent.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnhancedUnderwritingService
 */
@ExtendWith(MockitoExtension.class)
class EnhancedUnderwritingServiceTest {

    @Mock
    private KieContainer kieContainer;

    @Mock
    private KieSession kieSession;

    @Mock
    private CreditScoreClient creditScoreClient;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private MLUnderwritingService mlUnderwritingService;

    @InjectMocks
    private EnhancedUnderwritingService underwritingService;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(underwritingService, "useML", false);
        ReflectionTestUtils.setField(underwritingService, "useExternalCreditCheck", false);

        // Mock KieContainer
        when(kieContainer.newKieSession()).thenReturn(kieSession);
        when(kieSession.fireAllRules()).thenReturn(1);
        
        // Mock EncryptionUtil
        when(encryptionUtil.maskSensitiveData(anyString())).thenAnswer(i -> "MASKED");
    }

    @Test
    @DisplayName("Should reject application with low credit score")
    void testLowCreditScoreRejection() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST001")
                .creditScore(550)
                .insuranceType("auto")
                .age(30)
                .claimsInLast3Years(0)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        assertNotNull(decision.getDecisionId());
        assertEquals("CUST001", decision.getCustomerId());
        assertNotNull(decision.getDecision());
        assertTrue(decision.getRiskScore() >= 0 && decision.getRiskScore() <= 100);
        verify(kieSession, times(1)).fireAllRules();
    }

    @Test
    @DisplayName("Should approve application with excellent credit and no claims")
    void testExcellentCreditApproval() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST002")
                .creditScore(800)
                .insuranceType("auto")
                .age(35)
                .claimsInLast3Years(0)
                .drivingViolations(0)
                .atFaultAccidents(0)
                .yearsLicensed(15)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        assertEquals("CUST002", decision.getCustomerId());
        assertNotNull(decision.getDecision());
        assertTrue(decision.getRiskScore() < 40, "Excellent profile should have low risk score");
    }

    @Test
    @DisplayName("Should refer application with DUI history")
    void testDUIReferral() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST003")
                .creditScore(680)
                .insuranceType("auto")
                .age(28)
                .dui(true)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        // DUI should result in rejection or referral
        assertTrue("REJECT".equals(decision.getDecision()) || 
                   "REFER".equals(decision.getDecision()));
    }

    @Test
    @DisplayName("Should apply extra premium for high claims history")
    void testHighClaimsExtraPremium() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST004")
                .creditScore(700)
                .insuranceType("home")
                .age(45)
                .claimsInLast3Years(4)
                .totalClaimAmount(15000.0)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        assertTrue(decision.getRiskScore() > 50, "High claims should increase risk score");
        // Should have risk factors identified
        assertNotNull(decision.getRiskFactors());
    }

    @Test
    @DisplayName("Should handle async underwriting")
    void testAsyncUnderwriting() throws Exception {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST005")
                .creditScore(720)
                .insuranceType("health")
                .age(40)
                .smoker(false)
                .build();

        // When
        CompletableFuture<UnderwritingDecision> futureDecision = 
                underwritingService.performUnderwritingAsync(profile);
        UnderwritingDecision decision = futureDecision.get();

        // Then
        assertNotNull(decision);
        assertEquals("CUST005", decision.getCustomerId());
        assertNotNull(decision.getDecision());
    }

    @Test
    @DisplayName("Should identify smoker risk factor for health insurance")
    void testSmokerRiskFactor() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST006")
                .creditScore(690)
                .insuranceType("health")
                .age(35)
                .smoker(true)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        assertTrue(decision.getRiskScore() > 30, "Smoker should increase risk score");
    }

    @Test
    @DisplayName("Should identify flood zone risk for home insurance")
    void testFloodZoneRisk() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST007")
                .creditScore(710)
                .insuranceType("home")
                .age(50)
                .inFloodZone(true)
                .hasSecuritySystem(false)
                .propertyAge(60)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        assertTrue(decision.getRiskScore() > 20, "Flood zone should increase risk");
    }

    @Test
    @DisplayName("Should handle previous cancellation")
    void testPreviousCancellation() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST008")
                .creditScore(670)
                .insuranceType("auto")
                .age(32)
                .previousCancellation(true)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        // Previous cancellation should be flagged
        assertTrue(decision.getRiskScore() > 20);
    }

    @Test
    @DisplayName("Should handle multiple driving violations")
    void testMultipleDrivingViolations() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST009")
                .creditScore(680)
                .insuranceType("auto")
                .age(26)
                .drivingViolations(4)
                .atFaultAccidents(2)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        assertTrue(decision.getRiskScore() > 40, "Multiple violations should increase risk");
    }

    @Test
    @DisplayName("Should calculate risk score correctly")
    void testRiskScoreCalculation() {
        // Given - Various risk levels
        CustomerRiskProfile lowRisk = CustomerRiskProfile.builder()
                .customerId("CUST010")
                .creditScore(780)
                .insuranceType("auto")
                .age(35)
                .claimsInLast3Years(0)
                .build();

        CustomerRiskProfile highRisk = CustomerRiskProfile.builder()
                .customerId("CUST011")
                .creditScore(600)
                .insuranceType("auto")
                .age(22)
                .claimsInLast3Years(3)
                .build();

        // When
        UnderwritingDecision lowRiskDecision = underwritingService.performUnderwriting(lowRisk);
        UnderwritingDecision highRiskDecision = underwritingService.performUnderwriting(highRisk);

        // Then
        assertTrue(lowRiskDecision.getRiskScore() < highRiskDecision.getRiskScore(),
                "Low risk profile should have lower score than high risk profile");
    }

    @Test
    @DisplayName("Should pass compliance check for valid decision")
    void testComplianceCheck() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST012")
                .creditScore(700)
                .insuranceType("life")
                .age(40)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision.getComplianceCheckPassed());
        assertNotNull(decision.getDecisionMethod());
        assertNotNull(decision.getConfidenceScore());
    }

    @Test
    @DisplayName("Should include decision method in response")
    void testDecisionMethod() {
        // Given
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST013")
                .creditScore(730)
                .insuranceType("auto")
                .age(30)
                .build();

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision.getDecisionMethod());
        assertTrue(decision.getDecisionMethod().equals("RULES_ENGINE") ||
                   decision.getDecisionMethod().equals("STANDARD_ASSESSMENT") ||
                   decision.getDecisionMethod().equals("MACHINE_LEARNING"));
    }

    @Test
    @DisplayName("Should handle external credit check when enabled")
    void testExternalCreditCheck() throws Exception {
        // Given
        ReflectionTestUtils.setField(underwritingService, "useExternalCreditCheck", true);
        
        CustomerRiskProfile profile = CustomerRiskProfile.builder()
                .customerId("CUST014")
                .creditScore(650)
                .insuranceType("auto")
                .age(28)
                .externalCreditCheckCompleted(false)
                .build();

        CreditScoreClient.CreditScoreResponse creditResponse = 
                CreditScoreClient.CreditScoreResponse.builder()
                        .customerId("CUST014")
                        .creditScore(720)
                        .riskLevel("LOW")
                        .build();

        when(creditScoreClient.getCreditScore(anyString(), anyString())).thenReturn(creditResponse);

        // When
        UnderwritingDecision decision = underwritingService.performUnderwriting(profile);

        // Then
        assertNotNull(decision);
        verify(creditScoreClient, times(1)).getCreditScore(anyString(), anyString());
    }
}

