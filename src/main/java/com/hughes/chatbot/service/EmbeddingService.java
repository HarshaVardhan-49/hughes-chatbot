package com.hughes.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.embedding.model}")
    private String model;

    @Value("${openai.embedding.url}")
    private String embeddingUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Float> getEmbedding(String text) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("input", text);
            requestBody.put("model", model);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode responseJson = objectMapper.readTree(response.body());

            // Handle OpenAI API errors
            if (responseJson.has("error")) {
                String errorMsg = responseJson.get("error").get("message").asText();
                throw new RuntimeException("OpenAI API error: " + errorMsg);
            }

            JsonNode dataNode = responseJson.get("data");
            if (dataNode == null || dataNode.isEmpty()) {
                throw new RuntimeException("OpenAI returned empty embedding response");
            }

            JsonNode embeddingArray = dataNode.get(0).get("embedding");
            if (embeddingArray == null) {
                throw new RuntimeException("No embedding found in OpenAI response");
            }

            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingArray) {
                embedding.add(value.floatValue());
            }
            return embedding;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get embedding: " + e.getMessage(), e);
        }
    }
}

//## What this does in plain English
//
//One method — `getEmbedding(text)`:
//        ```
//Takes any text string
//      ↓
//Builds a request to OpenAI API
//        (exactly like the curl command you ran on Day 2!)
//      ↓
//Sends it with your API key in the header
//      ↓
//Gets back the 1536 numbers
//      ↓
//Returns them as List<Float>