package com.hughes.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotService {

    private static final String FALLBACK_MESSAGE =
            "I don't have information on that. " +
                    "Please contact Hughes support at 1-866-347-3292.";

    private static final double CONFIDENCE_THRESHOLD = 0.40;

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

            // Step 3 - check if results exist
            if (relevantChunks == null || relevantChunks.isEmpty()) {
                log.info("No chunks found for question: {}", question);
                return FALLBACK_MESSAGE;
            }

            // Step 4 - check confidence threshold
            double topScore = extractScore(relevantChunks.get(0));
            log.info("Top similarity score: {}", topScore);

            if (topScore < CONFIDENCE_THRESHOLD) {
                log.info("Score {} below threshold {} — returning fallback",
                        topScore, CONFIDENCE_THRESHOLD);
                return FALLBACK_MESSAGE;
            }

            // Step 5 - build clean context (strip score lines)
            StringBuilder context = new StringBuilder();
            for (String chunk : relevantChunks) {
                String cleanChunk = chunk.contains("\n")
                        ? chunk.substring(chunk.indexOf("\n") + 1)
                        : chunk;
                context.append(cleanChunk.trim()).append("\n\n");
            }

            // Step 6 - build GPT prompt
            String prompt = "You are an AI assistant for Hughes Network Systems " +
                    "field technicians. The technician may use typos or informal " +
                    "phrasing — interpret their intent and answer clearly and concisely " +
                    "using only the context provided below. " +
                    "Format your answer in plain sentences, not raw bullet points from docs. " +
                    "If the answer is not in the context, respond with: '" + FALLBACK_MESSAGE + "'\n\n" +
                    "CONTEXT:\n" + context +
                    "\nTECHNICIAN QUESTION: " + question +
                    "\n\nANSWER:";

            // Step 7 - call GPT
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

            // Step 8 - parse GPT response
            JsonNode json = objectMapper.readTree(response.body());

            if (json.has("error")) {
                log.error("GPT error: {}", json.get("error").get("message").asText());
                return FALLBACK_MESSAGE;
            }

            JsonNode choices = json.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("GPT returned no choices");
                return FALLBACK_MESSAGE;
            }

            return choices.get(0).get("message").get("content").asText().trim();

        } catch (Exception e) {
            log.error("Error in askQuestion: {}", e.getMessage());
            throw new RuntimeException("Failed to get answer: " + e.getMessage(), e);
        }
    }

    private double extractScore(String chunkWithScore) {
        try {
            String scoreLine = chunkWithScore.split("\n")[0];
            return Double.parseDouble(scoreLine.replace("Score: ", "").trim());
        } catch (Exception e) {
            log.warn("Could not parse score, defaulting to 0.0");
            return 0.0;
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