package com.xai.insuranceagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Main application class for Insurance AI Agent
 * 
 * This application provides:
 * - Insurance quoting
 * - Underwriting decisions
 * - Document generation
 * - AI-powered decision support
 * - GDPR-compliant data handling
 */
@SpringBootApplication
@EnableSpringHttpSession
public class InsuranceAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(InsuranceAgentApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Insurance AI Agent Application...");
        
        SpringApplication.run(InsuranceAgentApplication.class, args);
        
        logger.info("Insurance AI Agent Application started successfully");
        logger.info("API available at: http://localhost:8080/api/v1");
        logger.info("Health check: http://localhost:8080/api/v1/insurance/health");
    }

    /**
     * Configure in-memory session repository
     */
    @Bean
    public SessionRepository<?> sessionRepository() {
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }
}

