package com.omnibrain.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Answers by quoting the most relevant sentences from the retrieved passages, with citations.
 * It cannot invent anything: a passage is used only if it contains enough of the question's
 * content words, and only its matching sentences are quoted; otherwise the answer is "not found". No LLM or API key is needed.
 */
@Component
@ConditionalOnProperty(name = "omnibrain.rag.answerer", havingValue = "extractive", matchIfMissing = true)
public class ExtractiveAnswerer implements AnswerGenerator {

    static final int MAX_SENTENCES = 2;

    /** Question content words a passage must contain: at least half, and never fewer than one. */
    static int required(int questionTerms) {
        return Math.max(1, (int) Math.ceil(questionTerms * 0.5));
    }

    private record Candidate(int hit, int order, String sentence, double score) {}

    @Override
    public Optional<String> answer(String question, List<Hit> hits) {
        Set<String> terms = new HashSet<>(Text.tokens(question));
        if (terms.isEmpty()) return Optional.empty();
        int need = required(terms.size());

        List<Candidate> candidates = new ArrayList<>();
        for (int h = 0; h < hits.size(); h++) {
            // Answerability is judged on the whole passage: the question's words may be spread over
            // several of its sentences (a single shared common word is not enough, so we need `need`).
            if (matched(terms, Text.tokens(hits.get(h).text())) < need) continue;

            List<String> sentences = Text.sentences(hits.get(h).text());
            for (int s = 0; s < sentences.size(); s++) {
                int matched = matched(terms, Text.tokens(sentences.get(s)));
                if (matched >= 1) {
                    // Tie-break toward higher-ranked passages.
                    candidates.add(new Candidate(h, s, sentences.get(s), matched + 0.1 * hits.get(h).score()));
                }
            }
        }
        if (candidates.isEmpty()) return Optional.empty();

        List<Candidate> best = candidates.stream()
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .limit(MAX_SENTENCES)
                .sorted(Comparator.comparingInt(Candidate::hit).thenComparingInt(Candidate::order))
                .toList();
        StringBuilder out = new StringBuilder();
        for (Candidate c : best) {
            if (out.length() > 0) out.append(' ');
            out.append(c.sentence()).append(" [").append(c.hit() + 1).append(']');
        }
        return Optional.of(out.toString());
    }

    private static int matched(Set<String> terms, List<String> tokens) {
        Set<String> present = new HashSet<>(tokens);
        int n = 0;
        for (String t : terms) if (present.contains(t)) n++;
        return n;
    }
}
