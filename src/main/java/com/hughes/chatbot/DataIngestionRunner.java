package com.hughes.chatbot;

import com.hughes.chatbot.model.DocumentChunks;
import com.hughes.chatbot.service.ChunkingService;
import com.hughes.chatbot.service.OpenSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataIngestionRunner implements CommandLineRunner {

    private final ChunkingService chunkingService;
    private final OpenSearchService openSearchService;

    @Override
    public void run(String... args) throws Exception {
        if (openSearchService.indexHasData()) {
            log.info("OpenSearch already has data — skipping ingestion.");
            log.info("Knowledge base is ready.");
            return;
        }

        log.info("Starting data ingestion...");

        List<DocumentChunks> chunks = chunkingService.processAllDocuments();

        log.info("=================================");
        log.info("Total chunks created: {}", chunks.size());
        log.info("=================================");

        openSearchService.storeAllChunks(chunks);

        log.info("=================================");
        log.info("Data ingestion complete!");
        log.info("Knowledge base is ready.");
        log.info("=================================");
    }
}

//
//        ## What this does
//
//`CommandLineRunner` means — run this code automatically when Spring Boot starts up. So when we hit Run, it will:
//        ```
//Spring Boot starts
//      ↓
//              DataIngestionRunner.run() called automatically
//      ↓
//Calls processAllDocuments()
//      ↓
//Prints results in console