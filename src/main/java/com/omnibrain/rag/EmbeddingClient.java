package com.omnibrain.rag;

import org.springframework.stereotype.Component;

@Component
public class EmbeddingClient {
    public float[] embed(String text) {
        return new float[1536];
    }
}
