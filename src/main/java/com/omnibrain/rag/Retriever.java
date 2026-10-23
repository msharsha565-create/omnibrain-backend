package com.omnibrain.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Finds the chunks most similar to a question among one user's documents. It scans that user's
 * chunks and keeps the top k, which is fine for thousands of chunks; a vector index would be
 * the next step beyond that.
 */
@Component
public class Retriever {

    private final EmbeddingRepository repo;
    private final Embedder embedder;

    public Retriever(EmbeddingRepository repo, Embedder embedder) {
        this.repo = repo;
        this.embedder = embedder;
    }

    public List<Hit> search(String owner, String question, int k) {
        float[] q = embedder.embed(question);
        PriorityQueue<Hit> top = new PriorityQueue<>(Comparator.comparingDouble(Hit::score));
        for (EmbeddingEntity e : repo.findByOwnerAndEmbedder(owner, embedder.name())) {
            double score = Embedder.cosine(e.getVector(), q);
            if (top.size() < k) {
                top.add(new Hit(e.getId(), e.getSource(), e.getText(), score));
            } else if (score > top.peek().score()) {
                top.poll();
                top.add(new Hit(e.getId(), e.getSource(), e.getText(), score));
            }
        }
        List<Hit> hits = new ArrayList<>(top);
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        return hits;
    }
}
