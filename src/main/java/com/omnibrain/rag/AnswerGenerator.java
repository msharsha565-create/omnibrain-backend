package com.omnibrain.rag;

import java.util.List;
import java.util.Optional;

/** Produces an answer grounded in the retrieved hits, or nothing if they do not contain one. */
public interface AnswerGenerator {

    /** Citations use the 1-based position of the hit in {@code hits}, as in "[2]". */
    Optional<String> answer(String question, List<Hit> hits);
}
