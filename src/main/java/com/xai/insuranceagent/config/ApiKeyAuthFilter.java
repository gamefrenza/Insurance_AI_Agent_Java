package com.xai.insuranceagent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * API Key authentication filter
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${insurance.security.api-key}")
    private String validApiKey;

    @Value("${insurance.security.api-key-enabled:true}")
    private boolean apiKeyEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {

        // Skip authentication for public endpoints
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if API key authentication is disabled
        if (!apiKeyEnabled) {
            // Auto-authenticate for development
            authenticateRequest("development-mode");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API key from header
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("Missing API key for request: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"API Key required\", \"message\": \"Please provide X-API-Key header\"}");
            return;
        }

        // Validate API key
        if (!validApiKey.equals(apiKey)) {
            logger.warn("Invalid API key attempt for request: {}", path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Invalid API Key\", \"message\": \"The provided API key is invalid\"}");
            return;
        }

        // API key is valid - authenticate request
        authenticateRequest(apiKey);
        logger.debug("API key authentication successful for request: {}", path);

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(String apiKey) {
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                apiKey, 
                null, 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER"))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/health") || 
               path.contains("/actuator") ||
               path.contains("/swagger-ui") ||
               path.contains("/v3/api-docs");
    }
}

