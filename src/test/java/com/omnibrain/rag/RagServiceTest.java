package com.omnibrain.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RagServiceTest {

    static final String DOC = """
            Pour-over coffee is brewed by pouring hot water in slow circles over ground beans in a paper filter.
            A water temperature near ninety-three degrees Celsius extracts flavour without bitterness.

            Photosynthesis lets plants turn sunlight, water and carbon dioxide into glucose and oxygen.
            The reaction takes place in chloroplasts, which contain the green pigment chlorophyll.
            """;

    @Autowired RagService rag;
    @Autowired EmbeddingRepository repo;
    @Autowired Embedder embedder;

    private static String user() {
        return "u" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void answersFromTheUsersOwnDocumentWithCitationsAndSources() {
        String alice = user();
        assertTrue(rag.ingest(alice, "notes.txt", DOC) >= 1);

        RagAnswer a = rag.ask(alice, "What water temperature is best for pour-over coffee?");
        assertTrue(a.answerable());
        assertTrue(a.answer().contains("ninety-three degrees"), a.answer());
        assertTrue(a.answer().matches(".*\\[\\d\\]$"), a.answer());
        assertEquals("notes.txt", a.sources().get(0).source());
        assertEquals(1, a.sources().get(0).n());
    }

    @Test
    void sayssoWhenTheDocumentsDoNotContainTheAnswer() {
        String alice = user();
        rag.ingest(alice, "notes.txt", DOC);
        RagAnswer a = rag.ask(alice, "Who won the football world cup in 2010?");
        assertFalse(a.answerable());
        assertEquals(RagService.NOT_FOUND, a.answer());
    }

    @Test
    void aUserWithNoDocumentsIsToldSo() {
        RagAnswer a = rag.ask(user(), "anything about coffee?");
        assertFalse(a.answerable());
        assertEquals(RagService.NO_DOCUMENTS, a.answer());
        assertTrue(a.sources().isEmpty());
    }

    @Test
    void usersCannotRetrieveEachOthersDocuments() {
        String alice = user(), bob = user();
        rag.ingest(alice, "secret.txt", DOC);

        RagAnswer asBob = rag.ask(bob, "What water temperature is best for pour-over coffee?");
        assertFalse(asBob.answerable());
        assertEquals(RagService.NO_DOCUMENTS, asBob.answer());
        assertTrue(rag.documents(bob).isEmpty());

        rag.ingest(bob, "mine.txt", "Marathon training builds weekly mileage gradually with one long run each week.");
        RagAnswer bobAskingAboutCoffee = rag.ask(bob, "What water temperature is best for pour-over coffee?");
        assertFalse(bobAskingAboutCoffee.answerable());
        assertTrue(bobAskingAboutCoffee.sources().stream().noneMatch(s -> s.source().equals("secret.txt")));
    }

    @Test
    void reuploadingTheSameFileReplacesItInsteadOfDuplicating() {
        String alice = user();
        int first = rag.ingest(alice, "notes.txt", DOC);
        int second = rag.ingest(alice, "notes.txt", DOC);
        assertEquals(first, second);
        assertEquals(first, repo.countByOwner(alice));
    }

    @Test
    void documentsCanBeListedAndDeleted() {
        String alice = user();
        int chunks = rag.ingest(alice, "notes.txt", DOC);
        var docs = rag.documents(alice);
        assertEquals(1, docs.size());
        assertEquals("notes.txt", docs.get(0).getSource());
        assertEquals(chunks, docs.get(0).getChunks());

        assertEquals(chunks, rag.delete(alice, "notes.txt"));
        assertTrue(rag.documents(alice).isEmpty());
        assertEquals(0, rag.delete(alice, "notes.txt"));
    }

    @Test
    void vectorsFromADifferentEmbeddingModelAreNotComparedAndTheUserIsToldToReupload() {
        String alice = user();
        repo.save(new EmbeddingEntity(UUID.randomUUID().toString(), new float[]{1f, 0f}, "old chunk text here",
                "old.txt", alice, "some-other-model"));
        RagAnswer a = rag.ask(alice, "anything at all?");
        assertFalse(a.answerable());
        assertEquals(RagService.REINDEX, a.answer());
    }

    @Test
    void rowsStoredByOlderVersionsWithoutAnOwnerAreNeverReturned() {
        repo.save(new EmbeddingEntity(UUID.randomUUID().toString(), embedder.embed("legacy coffee passage"),
                "legacy coffee passage about pour-over", "legacy.txt", null, embedder.name()));
        String alice = user();
        rag.ingest(alice, "notes.txt", DOC);
        RagAnswer a = rag.ask(alice, "pour-over coffee");
        assertTrue(a.sources().stream().noneMatch(s -> s.source().equals("legacy.txt")));
    }

    @Test
    void anEmptyDocumentIndexesNothing() {
        assertEquals(0, rag.ingest(user(), "empty.txt", "   \n\n  "));
    }

    @Test
    void retrievalRanksTheRelevantPassageFirstAmongSeveralDocuments() {
        String alice = user();
        rag.ingest(alice, "coffee.txt", DOC.split("\n\n")[0]);
        rag.ingest(alice, "plants.txt", DOC.split("\n\n")[1]);
        rag.ingest(alice, "bikes.txt", "A derailleur moves the chain between gears on a bicycle when the rider shifts.");
        List<Hit> hits = new Retriever(repo, embedder).search(alice, "where does photosynthesis happen in plants", 3);
        assertEquals("plants.txt", hits.get(0).source());
        assertTrue(hits.get(0).score() > hits.get(1).score());
    }
}
