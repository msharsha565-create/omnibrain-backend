package com.omnibrain.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Retrieval-augmented question answering over a user's own uploads:
 * clean and chunk the text, embed and store the chunks, retrieve the best ones for a question,
 * and answer only from them.
 */
@Service
public class RagService {

    static final String NO_DOCUMENTS = "You have not uploaded any documents yet.";
    static final String NOT_FOUND = "I couldn't find that in your documents.";
    static final String REINDEX = "Your documents were indexed with a different embedding model. Please upload them again.";

    private final EmbeddingRepository repo;
    private final Embedder embedder;
    private final Retriever retriever;
    private final AnswerGenerator generator;
    private final int topK;
    private final double minScore;
    private final int chunkChars;
    private final ExecutorService streams = Executors.newVirtualThreadPerTaskExecutor();

    public RagService(
            EmbeddingRepository repo,
            Embedder embedder,
            Retriever retriever,
            AnswerGenerator generator,
            @Value("${omnibrain.rag.top-k:3}") int topK,
            @Value("${omnibrain.rag.min-score:0.05}") double minScore,
            @Value("${omnibrain.rag.chunk-chars:700}") int chunkChars
    ) {
        this.repo = repo;
        this.embedder = embedder;
        this.retriever = retriever;
        this.generator = generator;
        this.topK = topK;
        this.minScore = minScore;
        this.chunkChars = chunkChars;
    }

    /** Indexes a document for {@code owner}, replacing any earlier upload with the same name. */
    @Transactional
    public int ingest(String owner, String filename, String text) {
        List<String> chunks = TextChunker.chunk(text, chunkChars, 1);
        if (chunks.isEmpty()) return 0;
        List<float[]> vectors = embedder.embedAll(chunks); // embed first: a failure leaves the old copy intact
        repo.deleteByOwnerAndSource(owner, filename);
        List<EmbeddingEntity> rows = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            rows.add(new EmbeddingEntity(UUID.randomUUID().toString(), vectors.get(i), chunks.get(i),
                    filename, owner, embedder.name()));
        }
        repo.saveAll(rows);
        return rows.size();
    }

    public RagAnswer ask(String owner, String question) {
        if (repo.countByOwner(owner) == 0) return new RagAnswer(NO_DOCUMENTS, false, List.of());

        List<Hit> hits = retriever.search(owner, question, topK);
        if (hits.isEmpty()) return new RagAnswer(REINDEX, false, List.of());

        List<RagAnswer.Source> sources = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            Hit h = hits.get(i);
            sources.add(new RagAnswer.Source(i + 1, h.source(), Math.round(h.score() * 1000) / 1000.0, h.text()));
        }
        if (hits.get(0).score() < minScore) return new RagAnswer(NOT_FOUND, false, sources);

        return generator.answer(question, hits)
                .map(a -> new RagAnswer(a, true, sources))
                .orElseGet(() -> new RagAnswer(NOT_FOUND, false, sources));
    }

    public SseEmitter streamAnswer(String owner, String question) {
        SseEmitter emitter = new SseEmitter(120_000L);
        streams.submit(() -> {
            try {
                RagAnswer result = ask(owner, question);
                emitter.send(result.answer());
                StringBuilder sources = new StringBuilder("\n\nSources:\n");
                for (RagAnswer.Source s : result.sources()) {
                    sources.append('[').append(s.n()).append("] ").append(s.source())
                            .append(" (score ").append(String.format("%.2f", s.score())).append(") ")
                            .append(s.text()).append("\n\n");
                }
                if (!result.sources().isEmpty()) emitter.send(sources.toString());
                emitter.complete();
            } catch (IOException e) {
                emitter.complete(); // client went away
            } catch (RuntimeException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    public List<EmbeddingRepository.DocumentSummary> documents(String owner) {
        return repo.summarize(owner);
    }

    @Transactional
    public long delete(String owner, String source) {
        return repo.deleteByOwnerAndSource(owner, source);
    }
}
