package com.omnibrain.rag;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.omnibrain.rag.RetrievalQualityTest.OFF_TOPIC;
import static com.omnibrain.rag.RetrievalQualityTest.PASSAGES;
import static com.omnibrain.rag.RetrievalQualityTest.QUESTIONS;
import static org.junit.jupiter.api.Assertions.*;

class ExtractiveAnswererTest {

    private final HashingEmbedder embedder = new HashingEmbedder();
    private final ExtractiveAnswerer answerer = new ExtractiveAnswerer();

    /** Top-3 passages for a question, as the retriever would return them. */
    private List<Hit> top3(String question) {
        float[] q = embedder.embed(question);
        List<Hit> all = new ArrayList<>();
        for (int i = 0; i < PASSAGES.length; i++) {
            all.add(new Hit("p" + i, "doc.txt", PASSAGES[i], Embedder.cosine(embedder.embed(PASSAGES[i]), q)));
        }
        all.sort((a, b) -> Double.compare(b.score(), a.score()));
        return all.subList(0, 3);
    }

    @Test
    void directQuestionsGetAGroundedCitedAnswer() {
        int answered = 0;
        for (int q = 0; q < 13; q++) { // 0-12 are the direct (non-paraphrase) questions
            String question = (String) QUESTIONS[q][0];
            int expected = (Integer) QUESTIONS[q][1];
            List<Hit> hits = top3(question);
            Optional<String> answer = answerer.answer(question, hits);
            assertTrue(answer.isPresent(), "no answer for: " + question);
            answered++;
            // every citation points at the passage that holds the answer
            int cited = Integer.parseInt(answer.get().replaceAll(".*\\[(\\d)\\]$", "$1"));
            assertEquals("p" + expected, hits.get(cited - 1).id(), "wrong citation for: " + question);
            // grounded: every quoted sentence appears verbatim in a retrieved passage
            for (String part : answer.get().split("\\s*\\[\\d\\]\\s*")) {
                if (part.isBlank()) continue;
                assertTrue(hits.stream().anyMatch(h -> h.text().contains(part.trim())), "invented text: " + part);
            }
        }
        assertEquals(13, answered);
    }

    @Test
    void offTopicQuestionsAreNotAnswered() {
        for (String question : OFF_TOPIC) {
            assertTrue(answerer.answer(question, top3(question)).isEmpty(), "answered off-topic: " + question);
        }
    }

    @Test
    void aQuestionWithOnlyStopwordsGetsNoAnswer() {
        assertTrue(answerer.answer("what is the", top3("what is the")).isEmpty());
    }

    @Test
    void requiresAtLeastHalfTheContentWords() {
        assertEquals(1, ExtractiveAnswerer.required(1));
        assertEquals(2, ExtractiveAnswerer.required(3));
        assertEquals(3, ExtractiveAnswerer.required(6));
    }

    @Test
    void quotesAtMostTwoSentences() {
        String text = "Solar panels make power. Solar panels use silicon. Solar panels need sunlight. Solar panels last decades.";
        String answer = answerer.answer("solar panels", List.of(new Hit("x", "d", text, 0.5))).orElseThrow();
        assertTrue(answer.split("\\[1\\]", -1).length - 1 <= ExtractiveAnswerer.MAX_SENTENCES);
    }
}
