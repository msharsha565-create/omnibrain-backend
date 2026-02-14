package com.omnibrain.llm;

public interface LlmClient {
    Iterable<String> stream(String prompt);
}
