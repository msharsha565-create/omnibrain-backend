package com.omnibrain.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextTest {

    @Test
    void tokensAreLowerCaseStemmedAndSkipStopwords() {
        assertEquals(List.of("brew", "coffee"), Text.tokens("Brewing the Coffee"));
        assertEquals(Text.tokens("brewed"), Text.tokens("brews"));
        assertTrue(Text.tokens("what is the").isEmpty());
        assertTrue(Text.tokens(null).isEmpty());
    }

    @Test
    void stemmerJoinsCommonInflections() {
        assertEquals(Text.stem("volcano"), Text.stem("volcanoes"));
        assertEquals(Text.stem("erupt"), Text.stem("eruptions"));
        assertEquals("battery", Text.stem("batteries"));
        assertEquals("glass", Text.stem("glass"));
        assertEquals("bus", Text.stem("bus"));
    }

    @Test
    void tokenisationKeepsNonAsciiLetters() {
        assertEquals(List.of("café", "naïve"), Text.tokens("Café naïve"));
    }

    @Test
    void sentencesSplitOnTerminatorsFollowedByACapital() {
        assertEquals(List.of("First one.", "Second one?", "Third!"), Text.sentences("First one. Second one? Third!"));
        assertEquals(1, Text.sentences("Pi is about 3.14 and e is 2.71.").size());
    }

    @Test
    void cleanNormalisesWhitespaceAndDropsControlCharactersButKeepsUnicode() {
        assertEquals("a b\nc", Text.clean("a \u0000 b\r\nc"));
        assertEquals("héllo wörld", Text.clean("héllo   wörld"));
        assertEquals("", Text.clean(null));
    }
}
