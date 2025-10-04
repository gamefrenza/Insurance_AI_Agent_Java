package com.xai.insuranceagent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    
    private String error;
    
    private String message;
    
    private List<String> details;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    private String path;
}

