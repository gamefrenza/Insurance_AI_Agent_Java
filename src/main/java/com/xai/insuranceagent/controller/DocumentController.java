package com.xai.insuranceagent.controller;

import com.xai.insuranceagent.model.ErrorResponse;
import com.xai.insuranceagent.model.document.DocumentRequest;
import com.xai.insuranceagent.model.document.DocumentResponse;
import com.xai.insuranceagent.service.EnhancedDocumentFillingService;
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
 * REST Controller for document filling operations
 */
@RestController
@RequestMapping("/insurance/document")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private EnhancedDocumentFillingService documentFillingService;

    /**
     * Fill document synchronously
     */
    @PostMapping("/fill")
    public ResponseEntity<DocumentResponse> fillDocument(
            @Valid @RequestBody DocumentRequest request) {
        
        logger.info("Received document fill request - Type: {}, Customer: {}", 
                request.getDocumentType(), request.getCustomerId());

        try {
            DocumentResponse response = documentFillingService.fillDocument(request);
            
            if ("SUCCESS".equals(response.getStatus()) || 
                "PENDING_SIGNATURE".equals(response.getStatus())) {
                logger.info("Document filled successfully - ID: {}", response.getDocumentId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Document filling failed: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("Error filling document: {}", e.getMessage(), e);
            throw new RuntimeException("Document filling failed: " + e.getMessage());
        }
    }

    /**
     * Fill document asynchronously
     */
    @PostMapping("/fill-async")
    public CompletableFuture<ResponseEntity<DocumentResponse>> fillDocumentAsync(
            @Valid @RequestBody DocumentRequest request) {
        
        logger.info("Received async document fill request - Type: {}, Customer: {}", 
                request.getDocumentType(), request.getCustomerId());

        return documentFillingService.fillDocumentAsync(request)
                .thenApply(response -> {
                    if ("SUCCESS".equals(response.getStatus()) || 
                        "PENDING_SIGNATURE".equals(response.getStatus())) {
                        logger.info("Async document filled - ID: {}", response.getDocumentId());
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Error in async document filling: {}", throwable.getMessage());
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
                .message("Invalid document request data")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();

        logger.warn("Document request validation error: {}", errors);

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

        logger.error("Document filling error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}

