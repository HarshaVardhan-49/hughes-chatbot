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
            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("input", text);
            requestBody.put("model", model);

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody)))
                    .build();

            // Send request and get response
            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response and extract embedding
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode embeddingArray = responseJson
                    .get("data")
                    .get(0)
                    .get("embedding");

            // Convert to List<Float>
            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingArray) {
                embedding.add(value.floatValue());
            }
            return embedding;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get embedding", e);
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