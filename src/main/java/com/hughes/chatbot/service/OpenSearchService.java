package com.hughes.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hughes.chatbot.model.DocumentChunks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenSearchService {

    @Value("${opensearch.endpoint}")
    private String endpoint;

    @Value("${opensearch.index}")
    private String index;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void storeChunk(DocumentChunks chunk) {
        try {
            float[] embeddingArray = new float[chunk.getEmbedding().size()];
            for (int i = 0; i < chunk.getEmbedding().size(); i++) {
                embeddingArray[i] = chunk.getEmbedding().get(i);
            }

            Map<String, Object> document = new HashMap<>();
            document.put("id", chunk.getId());
            document.put("fileName", chunk.getFileName());
            document.put("chunkText", chunk.getChunkText());
            document.put("embedding", embeddingArray);

            String jsonBody = objectMapper.writeValueAsString(document);

            String safeId = chunk.getId()
                    .replace("/", "_")
                    .replace(".", "_");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/" + index + "/_doc/" + safeId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("Stored: {}", chunk.getId());
            } else {
                log.error("Failed to store: {} Status: {} Response: {}",
                        chunk.getId(), response.statusCode(), response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to store chunk: " + chunk.getId(), e);
        }
    }

    public void storeAllChunks(List<DocumentChunks> chunks) {
        log.info("Storing {} chunks into OpenSearch...", chunks.size());
        for (DocumentChunks chunk : chunks) {
            storeChunk(chunk);
        }
        log.info("All chunks stored successfully!");
    }

    public boolean indexHasData() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/" + index + "/_count"))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode json = objectMapper.readTree(response.body());
            int count = json.get("count").asInt();
            log.info("Current document count in OpenSearch: {}", count);
            return count > 0;
        } catch (Exception e) {
            log.error("Failed to check index data: {}", e.getMessage());
            return false;
        }
    }

    public List<String> searchSimilarChunks(List<Float> questionVector, int k) {
        try {
            float[] vectorArray = new float[questionVector.size()];
            for (int i = 0; i < questionVector.size(); i++) {
                vectorArray[i] = questionVector.get(i);
            }

            Map<String, Object> knnField = new HashMap<>();
            knnField.put("vector", vectorArray);
            knnField.put("k", k);

            Map<String, Object> embeddingKnn = new HashMap<>();
            embeddingKnn.put("embedding", knnField);

            Map<String, Object> knnQuery = new HashMap<>();
            knnQuery.put("knn", embeddingKnn);

            Map<String, Object> searchBody = new HashMap<>();
            searchBody.put("size", k);
            searchBody.put("query", knnQuery);

            String queryJson = objectMapper.writeValueAsString(searchBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/" + index + "/_search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(queryJson))
                    .build();

            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode hits = json.get("hits").get("hits");

            List<String> chunks = new ArrayList<>();
            for (JsonNode hit : hits) {
                String chunkText = hit.get("_source").get("chunkText").asText();
                double score = hit.get("_score").asDouble();
                chunks.add("Score: " + score + "\n" + chunkText);
            }
            return chunks;

        } catch (Exception e) {
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }
}

//OpenAI  = internet service, costs money
//converts text → vector
//generates GPT answers
//nothing stored here
//
//        OpenSearch = local storage on your Mac
//runs inside Docker
//stores all 115 chunks + vectors permanently
//free to use