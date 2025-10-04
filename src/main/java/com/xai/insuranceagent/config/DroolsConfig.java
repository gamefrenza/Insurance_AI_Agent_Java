package com.xai.insuranceagent.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drools Rules Engine Configuration
 */
@Configuration
public class DroolsConfig {

    private static final Logger logger = LoggerFactory.getLogger(DroolsConfig.class);
    private static final String RULES_PATH = "underwriting-rules.drl";

    @Bean
    public KieContainer kieContainer() {
        logger.info("Initializing Drools KieContainer...");
        
        KieServices kieServices = KieServices.Factory.get();
        
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH));
        
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        
        if (kieBuilder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            logger.error("Drools rules compilation errors:");
            kieBuilder.getResults().getMessages().forEach(
                message -> logger.error(message.toString())
            );
            throw new RuntimeException("Drools rules compilation failed");
        }
        
        KieModule kieModule = kieBuilder.getKieModule();
        KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        
        logger.info("Drools KieContainer initialized successfully");
        return kieContainer;
    }
}

