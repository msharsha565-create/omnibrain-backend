package com.omnibrain.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiProviderFactory {

    private final OpenAIProvider openAIProvider;
    private final MockProvider mockProvider;

    @Value("${ai.provider:mock}")
    private String provider;

    public AiProviderFactory(OpenAIProvider openAIProvider,
                             MockProvider mockProvider) {
        this.openAIProvider = openAIProvider;
        this.mockProvider = mockProvider;
    }

    public AiProvider getProvider() {
        if ("openai".equalsIgnoreCase(provider)) {
            return openAIProvider;
        }
        return mockProvider;
    }
}
