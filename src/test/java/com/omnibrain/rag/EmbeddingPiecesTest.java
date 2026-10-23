package com.omnibrain.rag;

import com.omnibrain.GovernanceEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingPiecesTest {

    private final HashingEmbedder e = new HashingEmbedder();

    @Test
    void vectorsAreUnitLengthAndDeterministic() {
        float[] v = e.embed("Solar panels convert sunlight into electricity");
        double norm = 0;
        for (float x : v) norm += x * x;
        assertEquals(1.0, Math.sqrt(norm), 1e-5);
        assertArrayEquals(v, e.embed("Solar panels convert sunlight into electricity"));
        assertEquals(HashingEmbedder.DIMENSIONS, v.length);
    }

    @Test
    void emptyTextEmbedsToTheZeroVectorWithoutError() {
        for (float x : e.embed("the of and")) assertEquals(0f, x);
        assertEquals(0.0, Embedder.cosine(e.embed(""), e.embed("anything")));
    }

    @Test
    void relatedTextScoresHigherThanUnrelatedText() {
        float[] q = e.embed("how do solar panels make electricity");
        double related = Embedder.cosine(q, e.embed("Solar panels turn sunlight into electricity."));
        double unrelated = Embedder.cosine(q, e.embed("Marathon runners taper before race day."));
        assertTrue(related > unrelated + 0.2, related + " vs " + unrelated);
    }

    @Test
    void wordOrderMattersSlightlyThroughBigrams() {
        float[] a = e.embed("machine learning model");
        assertTrue(Embedder.cosine(a, e.embed("machine learning model")) > Embedder.cosine(a, e.embed("model learning machine")));
    }

    @Test
    void fnvHashIsStableAcrossRuns() {
        // 32-bit FNV-1a reference values
        assertEquals(0x811c9dc5, HashingEmbedder.fnv1a(""));
        assertEquals(0xe40c292c, HashingEmbedder.fnv1a("a"));
        assertEquals(0xbf9cf968, HashingEmbedder.fnv1a("foobar"));
    }

    @Test
    void embedderNameEncodesTheDimension() {
        assertEquals("hashing-v1-2048", e.name());
    }

    @Test
    void cosineHandlesMismatchedLengths() {
        assertEquals(0.0, Embedder.cosine(new float[]{1, 0}, new float[]{1, 0, 0}));
    }

    @Test
    void sparseVectorsRoundTripAndAreCompact() {
        float[] v = e.embed("Solar panels convert sunlight into electricity");
        String stored = VectorUtils.serialize(v);
        assertTrue(stored.startsWith("s2048:"));
        assertTrue(stored.length() < 2000, "sparse form should be small, was " + stored.length());
        assertArrayEquals(v, VectorUtils.deserialize(stored));
    }

    @Test
    void denseAndLegacyVectorsStillRoundTrip() {
        float[] dense = {0.5f, -1.25f, 3f, 0.75f};
        assertArrayEquals(dense, VectorUtils.deserialize(VectorUtils.serialize(dense)));
        assertArrayEquals(new float[]{1.5f, 2.5f}, VectorUtils.deserialize("1.5,2.5")); // pre-sparse format
        assertEquals(0, VectorUtils.deserialize("s5:").length == 5 ? 0 : 1);
    }

    @Test
    void governanceEngineIsOnlyAKeywordFilter() {
        assertFalse(GovernanceEngine.allow("my PII is here"));
        assertTrue(GovernanceEngine.allow("what is IAM?"));
    }
}
