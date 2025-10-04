package com.xai.insuranceagent.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequest {

    @NotNull(message = "Customer information is required")
    @Valid
    private Customer customer;

    private String sessionId;

    private String additionalContext;
}

