package com.omnibrain.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private static String sentences(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= n; i++) sb.append("This is sentence number ").append(i).append(" in the document. ");
        return sb.toString().trim();
    }

    @Test
    void chunksStayNearTheLimitAndAlignToSentences() {
        List<String> chunks = TextChunker.chunk(sentences(30), 200, 0);
        assertTrue(chunks.size() > 3);
        for (String c : chunks) {
            assertTrue(c.length() <= 200, "too long: " + c.length());
            assertTrue(c.endsWith("document."), "cut mid-sentence: " + c);
            assertTrue(c.startsWith("This is sentence"), "starts mid-sentence: " + c);
        }
    }

    @Test
    void consecutiveChunksOverlapByTheRequestedSentences() {
        List<String> chunks = TextChunker.chunk(sentences(12), 150, 1);
        for (int i = 1; i < chunks.size(); i++) {
            List<String> prev = Text.sentences(chunks.get(i - 1));
            assertEquals(prev.get(prev.size() - 1), Text.sentences(chunks.get(i)).get(0));
        }
    }

    @Test
    void everySentenceSurvivesChunking() {
        String text = sentences(25);
        String all = String.join(" ", TextChunker.chunk(text, 180, 1));
        for (String s : Text.sentences(text)) assertTrue(all.contains(s), "lost: " + s);
    }

    @Test
    void aSentenceLongerThanTheLimitIsSplitOnSpacesNotMidWord() {
        String word = "alpha ";
        String longSentence = (word.repeat(100)).trim() + ".";
        List<String> chunks = TextChunker.chunk(longSentence, 120, 0);
        assertTrue(chunks.size() > 1);
        for (String c : chunks) {
            assertTrue(c.length() <= 120);
            for (String w : c.replace(".", "").split(" ")) assertEquals("alpha", w);
        }
    }

    @Test
    void prefersParagraphBreaksOnceAChunkIsFairlyFull() {
        String para = sentences(4);
        List<String> chunks = TextChunker.chunk(para + "\n\n" + para.replace("number", "item"), 260, 0);
        assertEquals(2, chunks.size());
        assertTrue(chunks.get(1).contains("item"));
    }

    @Test
    void tinyOrEmptyInputProducesNoChunks() {
        assertTrue(TextChunker.chunk("", 200, 1).isEmpty());
        assertTrue(TextChunker.chunk("Hi.", 200, 1).isEmpty());
        assertTrue(TextChunker.chunk(null, 200, 1).isEmpty());
    }

    @Test
    void nonAsciiTextIsPreserved() {
        String text = "Der Käse ist sehr gut und schmeckt nach Heimat. Café au lait ist ein beliebtes Getränk.";
        assertTrue(String.join(" ", TextChunker.chunk(text, 200, 0)).contains("Käse"));
    }
}
