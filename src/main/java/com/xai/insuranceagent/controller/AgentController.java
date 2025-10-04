package com.xai.insuranceagent.controller;

import com.xai.insuranceagent.model.Customer;
import com.xai.insuranceagent.model.ErrorResponse;
import com.xai.insuranceagent.model.ProcessRequest;
import com.xai.insuranceagent.model.ProcessResponse;
import com.xai.insuranceagent.service.DocumentFillingService;
import com.xai.insuranceagent.service.QuotingService;
import com.xai.insuranceagent.service.UnderwritingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main controller for Insurance Agent operations
 */
@RestController
@RequestMapping("/insurance")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private QuotingService quotingService;

    @Autowired
    private UnderwritingService underwritingService;

    @Autowired
    private DocumentFillingService documentFillingService;

    /**
     * Main endpoint for processing insurance requests
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessResponse> processInsuranceRequest(
            @Valid @RequestBody ProcessRequest request,
            HttpSession session) {

        String sessionId = session.getId();
        logger.info("Processing insurance request - Session: {}, Type: {}", 
                sessionId, request.getCustomer().getInsuranceType());

        try {
            Customer customer = request.getCustomer();
            
            // Store customer data in session for context management
            session.setAttribute("customer", customer);
            session.setAttribute("lastAccessTime", LocalDateTime.now());

            // Step 1: Generate Quote
            logger.debug("Step 1: Generating quote");
            ProcessResponse.QuoteResult quote = quotingService.generateQuote(customer);

            // Step 2: Perform Underwriting
            logger.debug("Step 2: Performing underwriting");
            ProcessResponse.UnderwritingResult underwriting = 
                    underwritingService.performUnderwriting(customer);

            // Step 3: Generate Documents
            logger.debug("Step 3: Generating documents");
            ProcessResponse.DocumentResult document = 
                    documentFillingService.generateDocument(customer, quote, underwriting);

            // Build response
            ProcessResponse response = ProcessResponse.builder()
                    .sessionId(sessionId)
                    .quote(quote)
                    .underwriting(underwriting)
                    .document(document)
                    .status("SUCCESS")
                    .message("Insurance request processed successfully")
                    .timestamp(LocalDateTime.now())
                    .build();

            logger.info("Insurance request processed successfully - Session: {}, Decision: {}", 
                    sessionId, underwriting.getDecision());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing insurance request: {}", e.getMessage(), e);
            
            ProcessResponse errorResponse = ProcessResponse.builder()
                    .sessionId(sessionId)
                    .status("ERROR")
                    .message("Failed to process insurance request: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Insurance AI Agent");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get session context
     */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getSession(HttpSession session) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", session.getId());
        sessionData.put("creationTime", session.getCreationTime());
        sessionData.put("lastAccessTime", session.getAttribute("lastAccessTime"));
        
        Customer customer = (Customer) session.getAttribute("customer");
        if (customer != null) {
            sessionData.put("hasCustomerData", true);
            sessionData.put("insuranceType", customer.getInsuranceType());
        } else {
            sessionData.put("hasCustomerData", false);
        }
        
        return ResponseEntity.ok(sessionData);
    }

    /**
     * Clear session context
     */
    @DeleteMapping("/session")
    public ResponseEntity<Map<String, String>> clearSession(HttpSession session) {
        String sessionId = session.getId();
        session.invalidate();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session cleared successfully");
        response.put("sessionId", sessionId);
        
        logger.info("Session cleared: {}", sessionId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        List<String> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request data")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();

        logger.warn("Validation error: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Global exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}

