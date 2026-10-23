package com.omnibrain.rag;

/** A retrieved chunk and its cosine similarity to the question. */
public record Hit(String id, String source, String text, double score) {}
