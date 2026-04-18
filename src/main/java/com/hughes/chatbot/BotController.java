package com.hughes.chatbot.controller;

import com.hughes.chatbot.exception.ApiException;
import com.hughes.chatbot.model.DocumentChunks;
import com.hughes.chatbot.service.BotService;
import com.hughes.chatbot.service.ChunkingService;
import com.hughes.chatbot.service.OpenSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bot")
@CrossOrigin(origins = "*")
public class BotController {

    private final BotService botService;
    private final ChunkingService chunkingService;
    private final OpenSearchService openSearchService;

    public BotController(BotService botService,
                         ChunkingService chunkingService,
                         OpenSearchService openSearchService) {
        this.botService = botService;
        this.chunkingService = chunkingService;
        this.openSearchService = openSearchService;
    }

    // ── 1. MAIN CHAT ENDPOINT ──────────────────────────────────────────
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, String> body) {

        String question = body.get("question");

        if (question == null || question.isBlank()) {
            throw new ApiException("Question cannot be blank", 400);
        }
        if (question.length() > 500) {
            throw new ApiException("Question too long. Max 500 characters.", 400);
        }

        String answer = botService.askQuestion(question);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("question", question);
        response.put("answer", answer);

        return ResponseEntity.ok(response);
    }

    // ── 2. HEALTH CHECK ────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            int chunkCount = openSearchService.getDocumentCount();
            response.put("status", "UP");
            response.put("chunks", chunkCount);
            response.put("version", "1.0.0");
        } catch (Exception e) {
            response.put("status", "DEGRADED");
            response.put("error", "OpenSearch unreachable: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // Wipes index, re-ingests all docs from S3, re-embeds, re-stores — no server restart needed
    @PostMapping("/admin/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {

        try {
            openSearchService.deleteIndex();

            // processAllDocuments() downloads + chunks + embeds — returns the list
            // storeAllChunks() takes that list and pushes everything into OpenSearch
            List<DocumentChunks> chunks = chunkingService.processAllDocuments();
            openSearchService.storeAllChunks(chunks);

            int newCount = openSearchService.getDocumentCount();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "reindexed");
            response.put("chunks", newCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            throw new ApiException("Reindex failed: " + e.getMessage(), 500);
        }
    }
}

//
//        ## What this does — plain English
//```
//GET /bot/ask?question=what do I do when signal is weak
//        ↓
//BotController receives the question
//        ↓
//Validates it's not empty
//        ↓
//Calls BotService.askQuestion()
//        ↓
//Returns JSON:
//        {
//        "question": "what do I do when signal is weak",
//        "answer": "Check cables first, then re-align..."
//        }