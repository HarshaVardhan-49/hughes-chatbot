package com.hughes.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BotService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final EmbeddingService embeddingService;
    private final OpenSearchService openSearchService;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String askQuestion(String question) {
        try {
            // Step 1 - embed the question
            List<Float> questionVector = embeddingService.getEmbedding(question);

            // Step 2 - search OpenSearch for top 5 chunks
            List<String> relevantChunks = openSearchService
                    .searchSimilarChunks(questionVector, 5);

            // Step 3 - build context from chunks
            StringBuilder context = new StringBuilder();
            for (String chunk : relevantChunks) {
                context.append(chunk).append("\n\n");
            }

            // Step 4 - build prompt for GPT
            String prompt = "You are an AI assistant for Hughes Network Systems " +
                    "field technicians. Answer the technician's question using " +
                    "only the context provided below. If the answer is not in " +
                    "the context, say: 'I don't have information on that. " +
                    "Please contact Hughes support at 1-866-347-3292.'\n\n" +
                    "CONTEXT:\n" + context +
                    "\nTECHNICIAN QUESTION: " + question +
                    "\n\nANSWER:";

            // Step 5 - call GPT
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("max_tokens", 500);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.set("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

            // Step 6 - extract answer from GPT response
            JsonNode json = objectMapper.readTree(response.body());
            return json.get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get answer", e);
        }
    }
}
//
//## What this does — plain English
//```
//askQuestion("signal is weak")
//      ↓
//EmbeddingService → vector
//      ↓
//OpenSearchService → top 5 chunks
//      ↓
//Build prompt:
//        "You are Hughes assistant...
//Here are relevant docs: [chunks]
//Question: signal is weak
//Answer:"
//        ↓
//Send to GPT-3.5-turbo
//      ↓
//GPT reads docs + writes answer
//      ↓
//Return answer string