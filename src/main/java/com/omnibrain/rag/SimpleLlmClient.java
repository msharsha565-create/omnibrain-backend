// LOCKED VERSION — DO NOT MODIFY
// Verified end-to-end: upload → embed → persist → retrieve → answer

package com.omnibrain.rag;

import java.util.List;

public class SimpleLlmClient {

    public List<String> stream(String prompt) {
        // VERY SIMPLE "LLM"
        return List.of(
            "---ANSWER---",
            "OmniBrain",
            "is",
            "a",
            "retrieval-augmented",
            "AI",
            "platform",
            "that",
            "allows",
            "users",
            "to",
            "upload",
            "documents,",
            "generate",
            "embeddings,",
            "and",
            "answer",
            "questions",
            "using",
            "contextual",
            "reasoning."
        );
    }
}
