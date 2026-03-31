package com.hughes.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentChunks {
    private String id;
    private String fileName;
    private String chunkText;
    private List<Float> embedding;
}
