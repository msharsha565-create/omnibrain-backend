package com.omnibrain.rag;

import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    // Dummy embedding (replace with OpenAI/Ollama later)
    public float[] embed(String text) {
        float[] v = new float[128];
        int i = 0;
        for (char c : text.toCharArray()) {
            v[i++ % v.length] += c;
        }
        return v;
    }
}
