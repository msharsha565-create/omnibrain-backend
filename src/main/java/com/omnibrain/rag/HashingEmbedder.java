package com.omnibrain.rag;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature-hashing embedder: words and word pairs are hashed into a fixed-size signed vector
 * (sub-linear term frequency, L2-normalised). Deterministic, needs no model or API key.
 *
 * <p>It measures vocabulary overlap, so it ranks passages that share words with the question.
 * It does not understand synonyms or paraphrase; use an OpenAI-compatible embedding model
 * ({@code EMBEDDINGS_PROVIDER=openai}) when that matters.
 */
@Component
@ConditionalOnProperty(name = "omnibrain.embeddings.provider", havingValue = "hashing", matchIfMissing = true)
public class HashingEmbedder implements Embedder {

    static final int DIMENSIONS = 2048;
    private static final double BIGRAM_WEIGHT = 0.5;

    @Override
    public String name() {
        return "hashing-v1-" + DIMENSIONS;
    }

    @Override
    public float[] embed(String text) {
        List<String> tokens = Text.tokens(text);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            counts.merge(tokens.get(i), 1, Integer::sum);
            if (i + 1 < tokens.size()) counts.merge(tokens.get(i) + "_" + tokens.get(i + 1), 1, Integer::sum);
        }
        float[] v = new float[DIMENSIONS];
        for (var e : counts.entrySet()) {
            int h = fnv1a(e.getKey());
            int index = (h & 0x7fffffff) % DIMENSIONS;
            double sign = ((h >>> 16) & 1) == 0 ? 1.0 : -1.0;
            double weight = e.getKey().indexOf('_') >= 0 ? BIGRAM_WEIGHT : 1.0;
            v[index] += (float) (sign * weight * (1.0 + Math.log(e.getValue())));
        }
        return Embedder.normalize(v);
    }

    /** 32-bit FNV-1a: stable across JVMs and versions, unlike an unspecified hash. */
    static int fnv1a(String s) {
        int h = 0x811c9dc5;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xff);
            h *= 0x01000193;
        }
        return h;
    }
}
