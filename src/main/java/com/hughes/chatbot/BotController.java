package com.hughes.chatbot;

import com.hughes.chatbot.service.BotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;

    @GetMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(
            @RequestParam String question) {

        log.info("Question received: {}", question);

        if (question == null || question.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Question cannot be empty");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String answer = botService.askQuestion(question.trim());
            Map<String, String> response = new HashMap<>();
            response.put("question", question.trim());
            response.put("answer", answer);
            log.info("Answer returned successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing question: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Something went wrong. Please try again.");
            error.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
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