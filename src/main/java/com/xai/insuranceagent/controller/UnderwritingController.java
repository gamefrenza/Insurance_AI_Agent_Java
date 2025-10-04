package com.xai.insuranceagent.controller;

import com.xai.insuranceagent.model.ErrorResponse;
import com.xai.insuranceagent.model.underwriting.CustomerRiskProfile;
import com.xai.insuranceagent.model.underwriting.UnderwritingDecision;
import com.xai.insuranceagent.service.EnhancedUnderwritingService;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for underwriting operations
 */
@RestController
@RequestMapping("/insurance/underwriting")
public class UnderwritingController {

    private static final Logger logger = LoggerFactory.getLogger(UnderwritingController.class);

    @Autowired
    private EnhancedUnderwritingService underwritingService;

    /**
     * Perform underwriting assessment synchronously
     */
    @PostMapping("/assess")
    public ResponseEntity<UnderwritingDecision> assessRisk(
            @Valid @RequestBody CustomerRiskProfile riskProfile) {
        
        logger.info("Received underwriting request - Customer: {}, Type: {}", 
                riskProfile.getCustomerId(), riskProfile.getInsuranceType());

        try {
            UnderwritingDecision decision = underwritingService.performUnderwriting(riskProfile);
            
            logger.info("Underwriting completed - Decision: {}, Risk Score: {}", 
                    decision.getDecision(), decision.getRiskScore());
            
            return ResponseEntity.ok(decision);

        } catch (Exception e) {
            logger.error("Error in underwriting assessment: {}", e.getMessage(), e);
            throw new RuntimeException("Underwriting assessment failed: " + e.getMessage());
        }
    }

    /**
     * Perform underwriting assessment asynchronously
     */
    @PostMapping("/assess-async")
    public CompletableFuture<ResponseEntity<UnderwritingDecision>> assessRiskAsync(
            @Valid @RequestBody CustomerRiskProfile riskProfile) {
        
        logger.info("Received async underwriting request - Customer: {}, Type: {}", 
                riskProfile.getCustomerId(), riskProfile.getInsuranceType());

        return underwritingService.performUnderwritingAsync(riskProfile)
                .thenApply(decision -> {
                    logger.info("Async underwriting completed - Decision: {}", decision.getDecision());
                    return ResponseEntity.ok(decision);
                })
                .exceptionally(throwable -> {
                    logger.error("Error in async underwriting: {}", throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
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
                .message("Invalid risk profile data")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();

        logger.warn("Underwriting validation error: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Exception handler for general errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        logger.error("Underwriting error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}

