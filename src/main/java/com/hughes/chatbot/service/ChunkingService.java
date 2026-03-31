package com.hughes.chatbot.service;

import com.hughes.chatbot.model.DocumentChunks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final S3Service s3Service;
    private final EmbeddingService embeddingService;

    public List<DocumentChunks> processAllDocuments() {
        List<DocumentChunks> allChunks = new ArrayList<>();

        // Step 1 - get all doc names from S3
        List<String> documents = s3Service.listDocuments();
        log.info("Found {} documents in S3", documents.size());

        // Step 2 - process each doc
        for (String docKey : documents) {
            log.info("Processing: {}", docKey);

            // Step 3 - download the doc
            String content = s3Service.downloadDocument(docKey);

            // Step 4 - split into chunks by blank line
            List<String> chunks = splitIntoChunks(content);
            log.info("Split into {} chunks", chunks.size());

            // Step 5 - for each chunk get embedding
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);

                if (chunkText.trim().isEmpty()) continue;

                // Call OpenAI to get vector
                List<Float> embedding = embeddingService.getEmbedding(chunkText);

                // Build DocumentChunk object
                DocumentChunks chunk = DocumentChunks.builder()
                        .id(docKey + "_chunk_" + i)
                        .fileName(docKey)
                        .chunkText(chunkText)
                        .embedding(embedding)
                        .build();

                allChunks.add(chunk);
                log.info("Chunk {}: got embedding of size {}",
                        i, embedding.size());
            }
        }

        log.info("Total chunks processed: {}", allChunks.size());
        return allChunks;
    }

    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\n\n");
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                chunks.add(paragraph.trim());
            }
        }
        return chunks;
    }
}


//        ## What this does in plain English
//
//Two methods:
//
//        **`processAllDocuments()`** — the main method that ties everything together:
//        ```
//List all docs from S3 (using S3Service)
//      ↓
//For each doc → download it (using S3Service)
//      ↓
//Split into chunks (using splitIntoChunks)
//      ↓
//For each chunk → get embedding (using EmbeddingService)
//      ↓
//Build a DocumentChunk object
//      ↓
//Add to list → return all chunks
//```
//
//        **`splitIntoChunks()`** — splits text by blank lines:
//        ```
//        "OVERVIEW:\nThis guide...\n\nSTEP 1:\nDo this..."
//        ↓
//        ["OVERVIEW:\nThis guide...", "STEP 1:\nDo this..."]