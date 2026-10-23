package com.omnibrain.rag;

import java.util.ArrayList;
import java.util.List;

/** Turns text into an L2-normalised vector. Implementations must be deterministic per model. */
public interface Embedder {

    /** Identifies the model; stored with each vector so vectors from different models are never compared. */
    String name();

    float[] embed(String text);

    default List<float[]> embedAll(List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        for (String t : texts) out.add(embed(t));
        return out;
    }

    static float[] normalize(float[] v) {
        double sum = 0;
        for (float x : v) sum += (double) x * x;
        if (sum == 0) return v;
        float inv = (float) (1.0 / Math.sqrt(sum));
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] * inv;
        return out;
    }

    static double cosine(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
