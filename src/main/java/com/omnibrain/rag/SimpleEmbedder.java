// PLACEHOLDER embedder: a pseudo-random vector derived from the text's hash. Identical text gets
// an identical vector, but similar text does NOT get similar vectors, so retrieval ranking is
// effectively arbitrary. Replace with a real embedding model before relying on results.

package com.omnibrain.rag;

import java.util.Random;

public class SimpleEmbedder {

    public float[] embed(String text) {
        float[] vec = new float[64];
        Random r = new Random(text.hashCode());
        for (int i = 0; i < vec.length; i++) {
            vec[i] = r.nextFloat();
        }
        return vec;
    }
}
