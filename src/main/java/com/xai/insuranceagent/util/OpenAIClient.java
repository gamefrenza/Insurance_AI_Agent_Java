package com.xai.insuranceagent.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI GPT-4 API Client for AI-powered decision support
 */
@Component
public class OpenAIClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    @Value("${openai.api.max-tokens}")
    private int maxTokens;

    @Value("${openai.api.temperature}")
    private double temperature;

    public OpenAIClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends a prompt to OpenAI GPT-4 and returns the response
     */
    public String chat(String systemPrompt, String userPrompt) {
        try {
            String requestBody = buildRequestBody(systemPrompt, userPrompt);
            
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            logger.debug("Sending request to OpenAI API");
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("OpenAI API request failed with code: {}", response.code());
                    throw new IOException("OpenAI API request failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }

        } catch (Exception e) {
            logger.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get AI response", e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) throws IOException {
        String jsonTemplate = """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "%s"
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "max_tokens": %d,
                  "temperature": %.1f
                }
                """;

        return String.format(jsonTemplate, 
                model,
                systemPrompt.replace("\"", "\\\"").replace("\n", "\\n"),
                userPrompt.replace("\"", "\\\"").replace("\n", "\\n"),
                maxTokens,
                temperature);
    }

    private String parseResponse(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode choicesNode = rootNode.path("choices");
        
        if (choicesNode.isArray() && choicesNode.size() > 0) {
            JsonNode messageNode = choicesNode.get(0).path("message");
            return messageNode.path("content").asText();
        }
        
        logger.warn("Unexpected response format from OpenAI API");
        return "No response from AI";
    }
}

