package com.omnibrain.rag;

import com.omnibrain.ai.AiProvider;
import com.omnibrain.ai.AiProviderFactory;
import com.omnibrain.ai.MockAiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Asks a chat model to answer strictly from the retrieved passages. Enabled with
 * {@code RAG_ANSWERER=llm}; requires a real provider ({@code OPENAI_API_KEY}, optionally
 * {@code OPENAI_BASE_URL} / {@code OPENAI_MODEL} for any OpenAI-compatible API).
 */
@Component
@ConditionalOnProperty(name = "omnibrain.rag.answerer", havingValue = "llm")
public class LlmAnswerer implements AnswerGenerator {

    static final String NOT_FOUND = "NOT_FOUND";

    private final AiProviderFactory providers;

    public LlmAnswerer(AiProviderFactory providers) {
        this.providers = providers;
    }

    static String prompt(String question, List<Hit> hits) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            context.append('[').append(i + 1).append("] ").append(hits.get(i).text()).append("\n\n");
        }
        return """
                Answer the question using ONLY the numbered passages below. Cite the passages you use
                as [1], [2]. If the passages do not contain the answer, reply with exactly %s and
                nothing else. Do not use outside knowledge.

                Passages:
                %s
                Question: %s
                """.formatted(NOT_FOUND, context, question);
    }

    @Override
    public Optional<String> answer(String question, List<Hit> hits) {
        AiProvider provider = providers.getProvider();
        if (provider instanceof MockAiProvider) {
            throw new IllegalStateException("RAG_ANSWERER=llm needs OPENAI_API_KEY (no real model is configured)");
        }
        String reply = provider.chat(prompt(question, hits)).trim();
        if (reply.isEmpty() || reply.contains(NOT_FOUND)) return Optional.empty();
        return Optional.of(reply);
    }
}
