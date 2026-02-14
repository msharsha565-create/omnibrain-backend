package com.omnibrain.llm;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SimpleLlmClient implements LlmClient {

    @Override
    public Iterable<String> stream(String prompt) {

        // Simulated token stream (replace with OpenAI/Ollama later)
        List<String> tokens = Arrays.asList(
                "OmniBrain", "is", "an", "AI", "knowledge", "platform."
        );

        return tokens;
    }
}
