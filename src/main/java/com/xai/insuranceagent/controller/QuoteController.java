package com.xai.insuranceagent.controller;

import com.xai.insuranceagent.model.ErrorResponse;
import com.xai.insuranceagent.model.quote.QuoteRequest;
import com.xai.insuranceagent.model.quote.QuoteResponse;
import com.xai.insuranceagent.service.EnhancedQuotingService;
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
 * REST Controller for enhanced quoting functionality
 */
@RestController
@RequestMapping("/insurance/quote")
public class QuoteController {

    private static final Logger logger = LoggerFactory.getLogger(QuoteController.class);

    @Autowired
    private EnhancedQuotingService quotingService;

    /**
     * Generate a detailed insurance quote synchronously
     */
    @PostMapping("/generate")
    public ResponseEntity<QuoteResponse> generateQuote(@Valid @RequestBody QuoteRequest request) {
        logger.info("Received quote request - Type: {}, Age: {}", 
                request.getInsuranceType(), request.getAge());

        try {
            QuoteResponse response = quotingService.generateDetailedQuote(request);
            logger.info("Quote generated successfully - ID: {}, Premium: ${}", 
                    response.getQuoteId(), response.getTotalPremium());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating quote: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate quote: " + e.getMessage());
        }
    }

    /**
     * Generate a detailed insurance quote asynchronously
     */
    @PostMapping("/generate-async")
    public CompletableFuture<ResponseEntity<QuoteResponse>> generateQuoteAsync(
            @Valid @RequestBody QuoteRequest request) {
        
        logger.info("Received async quote request - Type: {}, Age: {}", 
                request.getInsuranceType(), request.getAge());

        return quotingService.generateQuoteAsync(request)
                .thenApply(response -> {
                    logger.info("Async quote generated - ID: {}, Premium: ${}", 
                            response.getQuoteId(), response.getTotalPremium());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    logger.error("Error in async quote generation: {}", throwable.getMessage());
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
                .message("Invalid quote request data")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();

        logger.warn("Quote validation error: {}", errors);

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

        logger.error("Quote generation error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}

