package com.xai.insuranceagent.service;

import com.xai.insuranceagent.client.GuideWireClient;
import com.xai.insuranceagent.model.quote.QuoteRequest;
import com.xai.insuranceagent.model.quote.QuoteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnhancedQuotingService
 */
@ExtendWith(MockitoExtension.class)
class EnhancedQuotingServiceTest {

    @Mock
    private GuideWireClient guideWireClient;

    @InjectMocks
    private EnhancedQuotingService quotingService;

    @BeforeEach
    void setUp() {
        // Set configuration values via reflection
        ReflectionTestUtils.setField(quotingService, "autoBaseRate", 1000.0);
        ReflectionTestUtils.setField(quotingService, "homeBaseRate", 800.0);
        ReflectionTestUtils.setField(quotingService, "lifeBaseRate", 500.0);
        ReflectionTestUtils.setField(quotingService, "healthBaseRate", 600.0);
        ReflectionTestUtils.setField(quotingService, "useExternalApi", false);
    }

    @Test
    @DisplayName("Should calculate auto insurance quote with young driver surcharge")
    void testAutoInsuranceYoungDriver() {
        // Given
        QuoteRequest request = QuoteRequest.builder()
                .age(22)
                .gender("male")
                .address("Beijing, Chaoyang District")
                .insuranceType("auto")
                .vehicleModel("Honda Civic")
                .isUrbanArea(true)
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getTotalPremium());
        assertTrue(response.getTotalPremium() > 1000.0, "Premium should be higher than base rate");
        assertEquals("USD", response.getCurrency());
        assertEquals("comprehensive", response.getCoverage());
        assertNotNull(response.getQuoteId());
        assertTrue(response.getQuoteId().startsWith("AUT-"));
        
        // Verify breakdown
        QuoteResponse.PremiumBreakdown breakdown = response.getBreakdown();
        assertNotNull(breakdown);
        assertEquals(1000.0, breakdown.getBasePremium());
        assertTrue(breakdown.getAgeFactor() > 0, "Young driver should have age surcharge");
        assertTrue(breakdown.getLocationFactor() > 0, "Urban area should have location surcharge");
    }

    @Test
    @DisplayName("Should calculate health insurance quote with smoker surcharge")
    void testHealthInsuranceSmoker() {
        // Given
        QuoteRequest request = QuoteRequest.builder()
                .age(35)
                .gender("female")
                .address("Shanghai")
                .insuranceType("health")
                .smoker(true)
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalPremium() > 600.0, "Premium should include smoker surcharge");
        assertEquals(500, response.getDeductible());
        
        // Verify breakdown shows smoker risk factor
        QuoteResponse.PremiumBreakdown breakdown = response.getBreakdown();
        assertTrue(breakdown.getRiskFactor() > 0, "Smoker should have risk surcharge");
    }

    @Test
    @DisplayName("Should calculate auto insurance with sports car high risk")
    void testAutoInsuranceSportsCar() {
        // Given
        QuoteRequest request = QuoteRequest.builder()
                .age(30)
                .gender("male")
                .address("New York")
                .insuranceType("auto")
                .vehicleModel("Ferrari F8 Sports Car")
                .isUrbanArea(true)
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalPremium() > 1200.0, "Sports car should have high premium");
        
        QuoteResponse.PremiumBreakdown breakdown = response.getBreakdown();
        assertTrue(breakdown.getRiskFactor() > 0, "Sports car should have high risk factor");
    }

    @Test
    @DisplayName("Should apply optimal age discount")
    void testOptimalAgeDiscount() {
        // Given
        QuoteRequest request = QuoteRequest.builder()
                .age(30)
                .gender("female")
                .address("Rural Area")
                .insuranceType("auto")
                .vehicleModel("Toyota Camry")
                .isUrbanArea(false)
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(request);

        // Then
        assertNotNull(response);
        QuoteResponse.PremiumBreakdown breakdown = response.getBreakdown();
        assertTrue(breakdown.getAgeFactor() < 0, "Age 30 should get discount");
    }

    @Test
    @DisplayName("Should calculate home insurance with high-value property")
    void testHomeInsuranceHighValue() {
        // Given
        QuoteRequest request = QuoteRequest.builder()
                .age(45)
                .address("Los Angeles")
                .insuranceType("home")
                .propertyType("Villa")
                .propertyValue(1500000.0)
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalPremium() > 800.0);
        
        QuoteResponse.PremiumBreakdown breakdown = response.getBreakdown();
        assertTrue(breakdown.getRiskFactor() > 0, "High-value property should have risk surcharge");
    }

    @Test
    @DisplayName("Should calculate life insurance with high-risk occupation")
    void testLifeInsuranceHighRiskJob() {
        // Given
        QuoteRequest request = QuoteRequest.builder()
                .age(40)
                .address("Shanghai")
                .insuranceType("life")
                .occupation("Commercial Pilot")
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(request);

        // Then
        assertNotNull(response);
        QuoteResponse.PremiumBreakdown breakdown = response.getBreakdown();
        assertTrue(breakdown.getRiskFactor() > 0, "Pilot should have high-risk surcharge");
    }

    @Test
    @DisplayName("Should handle async quote generation")
    void testAsyncQuoteGeneration() throws Exception {
        // Given
        QuoteRequest request = QuoteRequest.builder()
                .age(28)
                .address("Beijing")
                .insuranceType("health")
                .smoker(false)
                .build();

        // When
        CompletableFuture<QuoteResponse> futureResponse = quotingService.generateQuoteAsync(request);
        QuoteResponse response = futureResponse.get();

        // Then
        assertNotNull(response);
        assertNotNull(response.getTotalPremium());
        assertTrue(response.getTotalPremium() > 0);
    }

    @Test
    @DisplayName("Should use external API when enabled")
    void testExternalApiIntegration() throws Exception {
        // Given
        ReflectionTestUtils.setField(quotingService, "useExternalApi", true);
        
        QuoteRequest request = QuoteRequest.builder()
                .age(30)
                .insuranceType("auto")
                .address("Test City")
                .build();

        QuoteResponse mockResponse = QuoteResponse.builder()
                .premium(1200.0)
                .totalPremium(1200.0)
                .currency("USD")
                .build();

        when(guideWireClient.getQuoteAsync(any(QuoteRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When
        CompletableFuture<QuoteResponse> futureResponse = quotingService.generateQuoteAsync(request);
        QuoteResponse response = futureResponse.get();

        // Then
        assertNotNull(response);
        verify(guideWireClient, times(1)).getQuoteAsync(any(QuoteRequest.class));
    }

    @Test
    @DisplayName("Should fallback to local calculation when external API fails")
    void testExternalApiFallback() throws Exception {
        // Given
        ReflectionTestUtils.setField(quotingService, "useExternalApi", true);
        
        QuoteRequest request = QuoteRequest.builder()
                .age(30)
                .insuranceType("auto")
                .address("Test City")
                .build();

        when(guideWireClient.getQuoteAsync(any(QuoteRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        // When
        CompletableFuture<QuoteResponse> futureResponse = quotingService.generateQuoteAsync(request);
        QuoteResponse response = futureResponse.get();

        // Then
        assertNotNull(response, "Should fallback to local calculation");
        assertNotNull(response.getTotalPremium());
    }

    @Test
    @DisplayName("Should validate quote ID format")
    void testQuoteIdFormat() {
        // Given
        QuoteRequest autoRequest = QuoteRequest.builder()
                .age(25)
                .insuranceType("auto")
                .address("Test")
                .build();

        QuoteRequest healthRequest = QuoteRequest.builder()
                .age(30)
                .insuranceType("health")
                .address("Test")
                .build();

        // When
        QuoteResponse autoResponse = quotingService.generateDetailedQuote(autoRequest);
        QuoteResponse healthResponse = quotingService.generateDetailedQuote(healthRequest);

        // Then
        assertTrue(autoResponse.getQuoteId().startsWith("AUT-"));
        assertTrue(healthResponse.getQuoteId().startsWith("HLT-"));
    }

    @Test
    @DisplayName("Should calculate correct deductibles")
    void testDeductibleCalculation() {
        // Given - High premium scenario
        QuoteRequest highPremiumRequest = QuoteRequest.builder()
                .age(22)
                .gender("male")
                .address("New York")
                .insuranceType("auto")
                .vehicleModel("Ferrari")
                .isUrbanArea(true)
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(highPremiumRequest);

        // Then
        assertTrue(response.getDeductible() >= 500, "Deductible should be at least $500");
    }

    @Test
    @DisplayName("Should include valid breakdown notes")
    void testBreakdownNotes() {
        // Given
        QuoteRequest request = QuoteRequest.builder()
                .age(22)
                .gender("male")
                .address("Shanghai")
                .insuranceType("auto")
                .vehicleModel("Tesla Model 3")
                .isUrbanArea(true)
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(request);

        // Then
        QuoteResponse.PremiumBreakdown breakdown = response.getBreakdown();
        assertNotNull(breakdown.getCalculationNotes());
        assertFalse(breakdown.getCalculationNotes().isEmpty());
        assertTrue(breakdown.getCalculationNotes().contains("Premium calculated based on"));
    }

    @Test
    @DisplayName("Should handle null optional fields gracefully")
    void testNullOptionalFields() {
        // Given - Minimal request
        QuoteRequest request = QuoteRequest.builder()
                .age(30)
                .address("Test Address")
                .insuranceType("auto")
                .build();

        // When
        QuoteResponse response = quotingService.generateDetailedQuote(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getTotalPremium());
        assertTrue(response.getTotalPremium() > 0);
    }
}

