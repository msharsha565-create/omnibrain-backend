package com.omnibrain.rag;

import com.omnibrain.ai.AiProvider;
import com.omnibrain.ai.AiProviderFactory;
import com.omnibrain.ai.MockAiProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmAnswererTest {

    private static final List<Hit> HITS = List.of(
            new Hit("1", "a.txt", "The reactor was shut down in 1986.", 0.9),
            new Hit("2", "b.txt", "Tides follow the moon.", 0.4));

    private AiProviderFactory factoryReturning(AiProvider provider) {
        AiProviderFactory factory = mock(AiProviderFactory.class);
        when(factory.getProvider()).thenReturn(provider);
        return factory;
    }

    @Test
    void thePromptNumbersThePassagesAndForbidsOutsideKnowledge() {
        String prompt = LlmAnswerer.prompt("When was it shut down?", HITS);
        assertTrue(prompt.contains("[1] The reactor was shut down in 1986."));
        assertTrue(prompt.contains("[2] Tides follow the moon."));
        assertTrue(prompt.contains("ONLY"));
        assertTrue(prompt.contains(LlmAnswerer.NOT_FOUND));
        assertTrue(prompt.contains("When was it shut down?"));
    }

    @Test
    void returnsTheModelsAnswer() {
        List<String> seen = new ArrayList<>();
        AiProvider fake = prompt -> { seen.add(prompt); return "  In 1986 [1].  "; };
        Optional<String> answer = new LlmAnswerer(factoryReturning(fake)).answer("When?", HITS);
        assertEquals(Optional.of("In 1986 [1]."), answer);
        assertTrue(seen.get(0).contains("[1] The reactor"));
    }

    @Test
    void aNotFoundReplyBecomesNoAnswer() {
        AiProvider fake = prompt -> LlmAnswerer.NOT_FOUND;
        assertTrue(new LlmAnswerer(factoryReturning(fake)).answer("Who?", HITS).isEmpty());
        AiProvider blank = prompt -> "   ";
        assertTrue(new LlmAnswerer(factoryReturning(blank)).answer("Who?", HITS).isEmpty());
    }

    @Test
    void refusesToPassOffTheCannedMockProviderAsAnAnswer() {
        var ex = assertThrows(IllegalStateException.class,
                () -> new LlmAnswerer(factoryReturning(new MockAiProvider())).answer("Q?", HITS));
        assertTrue(ex.getMessage().contains("OPENAI_API_KEY"));
    }
}
