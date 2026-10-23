package com.omnibrain.rag;

import com.omnibrain.GovernanceEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RagPiecesTest {

    @Test
    void vectorsRoundTripThroughTheirStringForm() {
        float[] v = {0.5f, -1.25f, 3f, 0f};
        assertArrayEquals(v, VectorUtils.deserialize(VectorUtils.serialize(v)));
    }

    @Test
    void placeholderEmbedderIsDeterministicButNotSemantic() {
        SimpleEmbedder e = new SimpleEmbedder();
        assertArrayEquals(e.embed("hello world"), e.embed("hello world"));
        assertEquals(64, e.embed("anything").length);
        // Documents the known limitation: near-identical text does not get a near-identical vector.
        assertFalse(java.util.Arrays.equals(e.embed("hello world"), e.embed("hello world!")));
    }

    @Test
    void governanceEngineIsAKeywordFilter() {
        assertFalse(GovernanceEngine.allow("my PII is here"));
        assertTrue(GovernanceEngine.allow("what is IAM?"));
    }
}
