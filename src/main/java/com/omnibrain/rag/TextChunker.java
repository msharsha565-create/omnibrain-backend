package com.omnibrain.rag;

import java.util.ArrayList;
import java.util.List;

/** Splits a document into overlapping, sentence-aligned chunks. */
public final class TextChunker {

    private TextChunker() {}

    static final int MIN_CHUNK_CHARS = 20;

    /**
     * @param maxChars        soft upper bound for a chunk (a single longer sentence is split on spaces)
     * @param overlapSentences number of trailing sentences repeated at the start of the next chunk
     */
    public static List<String> chunk(String text, int maxChars, int overlapSentences) {
        List<String> chunks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentLen = 0;

        for (String paragraph : Text.clean(text).split("\n\\s*\n")) {
            for (String sentence : Text.sentences(paragraph)) {
                for (String piece : splitLong(sentence, maxChars)) {
                    if (currentLen + piece.length() + 1 > maxChars && !current.isEmpty()) {
                        emit(chunks, current);
                        current = tail(current, overlapSentences);
                        currentLen = length(current);
                    }
                    current.add(piece);
                    currentLen += piece.length() + 1;
                }
            }
            // Prefer ending a chunk at a paragraph break once it is reasonably full.
            if (currentLen >= maxChars * 0.6) {
                emit(chunks, current);
                current = new ArrayList<>();
                currentLen = 0;
            }
        }
        emit(chunks, current);
        return chunks;
    }

    private static void emit(List<String> chunks, List<String> sentences) {
        String chunk = String.join(" ", sentences).trim();
        if (chunk.length() >= MIN_CHUNK_CHARS && (chunks.isEmpty() || !chunks.get(chunks.size() - 1).equals(chunk))) {
            chunks.add(chunk);
        }
    }

    private static List<String> tail(List<String> sentences, int n) {
        if (n <= 0 || sentences.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(sentences.subList(Math.max(0, sentences.size() - n), sentences.size()));
    }

    private static int length(List<String> sentences) {
        return sentences.stream().mapToInt(s -> s.length() + 1).sum();
    }

    private static List<String> splitLong(String sentence, int maxChars) {
        if (sentence.length() <= maxChars) return List.of(sentence);
        List<String> pieces = new ArrayList<>();
        String rest = sentence;
        while (rest.length() > maxChars) {
            int cut = rest.lastIndexOf(' ', maxChars);
            if (cut < maxChars / 2) cut = maxChars;
            pieces.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        if (!rest.isEmpty()) pieces.add(rest);
        return pieces;
    }
}
