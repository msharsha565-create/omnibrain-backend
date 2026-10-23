package com.omnibrain.rag;

import java.util.List;

/** The result of a question: the answer, whether the documents actually supported one, and sources. */
public record RagAnswer(String answer, boolean answerable, List<Source> sources) {

    public record Source(int n, String source, double score, String text) {}
}
