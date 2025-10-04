package com.xai.insuranceagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xai.insuranceagent.controller.IntegratedAgentController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for complete insurance workflow
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String validApiKey = "test-api-key";

    @BeforeEach
    void setUp() {
        // Test setup
    }

    @Test
    @DisplayName("Should process complete insurance workflow successfully")
    void testCompleteWorkflowSuccess() throws Exception {
        IntegratedAgentController.ComprehensiveRequest request = 
                IntegratedAgentController.ComprehensiveRequest.builder()
                        .customerId("TEST-001")
                        .customerName("John Doe")
                        .age(30)
                        .gender("male")
                        .address("123 Main St, New York")
                        .email("john@example.com")
                        .phone("+1-555-1234")
                        .insuranceType("auto")
                        .vehicleModel("Tesla Model 3")
                        .creditScore(750)
                        .claimsHistory(0)
                        .isSmoker(false)
                        .occupation("Engineer")
                        .requireSignature(false)
                        .build();

        mockMvc.perform(post("/api/v1/insurance/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("TEST-001"))
                .andExpect(jsonPath("$.quote").exists())
                .andExpect(jsonPath("$.quote.totalPremium").isNumber())
                .andExpect(jsonPath("$.underwriting").exists())
                .andExpect(jsonPath("$.underwriting.decision").exists())
                .andExpect(jsonPath("$.overallStatus").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject request with low credit score")
    void testLowCreditScoreRejection() throws Exception {
        IntegratedAgentController.ComprehensiveRequest request = 
                IntegratedAgentController.ComprehensiveRequest.builder()
                        .customerId("TEST-002")
                        .customerName("Jane Smith")
                        .age(35)
                        .gender("female")
                        .address("456 Oak Ave, Boston")
                        .email("jane@example.com")
                        .insuranceType("auto")
                        .creditScore(550)  // Low credit score
                        .claimsHistory(0)
                        .build();

        mockMvc.perform(post("/api/v1/insurance/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.underwriting.decision").value("REJECTED"))
                .andExpect(jsonPath("$.overallStatus").value("REJECTED"))
                .andExpect(jsonPath("$.document").value(nullValue()));
    }

    @Test
    @DisplayName("Should handle async processing")
    void testAsyncProcessing() throws Exception {
        IntegratedAgentController.ComprehensiveRequest request = 
                IntegratedAgentController.ComprehensiveRequest.builder()
                        .customerId("TEST-003")
                        .customerName("Bob Johnson")
                        .age(40)
                        .gender("male")
                        .address("789 Pine Rd, Chicago")
                        .email("bob@example.com")
                        .insuranceType("home")
                        .propertyValue(300000.0)
                        .creditScore(720)
                        .claimsHistory(1)
                        .build();

        mockMvc.perform(post("/api/v1/insurance/process-async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should validate required fields")
    void testValidationErrors() throws Exception {
        IntegratedAgentController.ComprehensiveRequest request = 
                IntegratedAgentController.ComprehensiveRequest.builder()
                        .customerId("")  // Invalid: empty
                        .age(15)  // Invalid: too young
                        .insuranceType("invalid")  // Invalid type
                        .build();

        mockMvc.perform(post("/api/v1/insurance/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return health check status")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/insurance/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    @DisplayName("Should handle high risk customer with conditional approval")
    void testConditionalApproval() throws Exception {
        IntegratedAgentController.ComprehensiveRequest request = 
                IntegratedAgentController.ComprehensiveRequest.builder()
                        .customerId("TEST-004")
                        .customerName("Alice Brown")
                        .age(25)
                        .gender("female")
                        .address("321 Elm St, Los Angeles")
                        .email("alice@example.com")
                        .insuranceType("auto")
                        .vehicleModel("Ferrari F8")  // High risk vehicle
                        .creditScore(700)
                        .claimsHistory(2)  // Some claims history
                        .build();

        mockMvc.perform(post("/api/v1/insurance/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quote.totalPremium").value(greaterThan(2000.0)))
                .andExpect(jsonPath("$.underwriting.riskScore").value(greaterThan(50.0)));
    }

    @Test
    @DisplayName("Should process life insurance for smoker with higher premium")
    void testSmokerLifeInsurance() throws Exception {
        IntegratedAgentController.ComprehensiveRequest request = 
                IntegratedAgentController.ComprehensiveRequest.builder()
                        .customerId("TEST-005")
                        .customerName("Charlie Davis")
                        .age(45)
                        .gender("male")
                        .address("555 Maple Dr, Seattle")
                        .email("charlie@example.com")
                        .insuranceType("life")
                        .isSmoker(true)  // Smoker
                        .occupation("Office Worker")
                        .creditScore(750)
                        .claimsHistory(0)
                        .build();

        mockMvc.perform(post("/api/v1/insurance/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quote.totalPremium").value(greaterThan(1000.0)))
                .andExpect(jsonPath("$.underwriting.riskFactors").isArray())
                .andExpect(jsonPath("$.underwriting.riskFactors[*]", hasItem(containsString("smoker"))));
    }

    @Test
    @DisplayName("Should process urban area with location premium")
    void testUrbanAreaPremium() throws Exception {
        IntegratedAgentController.ComprehensiveRequest request = 
                IntegratedAgentController.ComprehensiveRequest.builder()
                        .customerId("TEST-006")
                        .customerName("David Wilson")
                        .age(32)
                        .gender("male")
                        .address("Beijing, China")  // Urban area
                        .email("david@example.com")
                        .insuranceType("auto")
                        .vehicleModel("Honda Civic")
                        .creditScore(730)
                        .claimsHistory(0)
                        .build();

        mockMvc.perform(post("/api/v1/insurance/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quote.breakdown.locationFactor").exists())
                .andExpect(jsonPath("$.overallStatus").value("SUCCESS"));
    }
}

