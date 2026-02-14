package com.omnibrain.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {

    private final OpenAiProvider openAiProvider;
    private final MockAiProvider mockAiProvider;
    private final String apiKey;

    public AiProviderFactory(
            OpenAiProvider openAiProvider,
            MockAiProvider mockAiProvider,
            @Value("${OPENAI_API_KEY:}") String apiKey
    ) {
        this.openAiProvider = openAiProvider;
        this.mockAiProvider = mockAiProvider;
        this.apiKey = apiKey;
    }

    public AiProvider getProvider() {

        if (apiKey == null || apiKey.isBlank()) {
            return mockAiProvider;
        }

        return openAiProvider;
    }
}
