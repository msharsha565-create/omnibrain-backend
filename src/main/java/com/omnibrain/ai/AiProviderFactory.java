package com.omnibrain.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {

    private final OpenAiProvider openAiProvider;
    private final MockAiProvider mockAiProvider;

    @Value("${OPENAI_API_KEY:}")
    private String apiKey;

    public AiProviderFactory(
            OpenAiProvider openAiProvider,
            MockAiProvider mockAiProvider
    ) {
        this.openAiProvider = openAiProvider;
        this.mockAiProvider = mockAiProvider;
    }

    public AiProvider getProvider() {
        if (apiKey != null && !apiKey.isBlank()) {
            return openAiProvider;
        }
        return mockAiProvider;
    }
}
