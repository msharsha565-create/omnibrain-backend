// PLACEHOLDER "LLM": ignores the prompt and streams one fixed sentence. Replace with a real
// model client (for example the OpenAI provider in com.omnibrain.ai) to answer from the context.

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
