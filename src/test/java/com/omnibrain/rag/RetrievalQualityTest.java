package com.omnibrain.rag;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Measures retrieval on a small hand-written corpus: 15 passages on unrelated topics and 15
 * questions, each with one correct passage. Some questions are paraphrases with little word
 * overlap on purpose: a lexical embedder is expected to miss those, and the test records it.
 */
class RetrievalQualityTest {

    static final String[] PASSAGES = {
        /* 0 */ "Pour-over coffee is brewed by pouring hot water in slow circles over ground beans in a paper filter. A water temperature near ninety-three degrees Celsius extracts flavour without bitterness.",
        /* 1 */ "Photosynthesis lets plants turn sunlight, water and carbon dioxide into glucose and oxygen. The reaction takes place in chloroplasts, which contain the green pigment chlorophyll.",
        /* 2 */ "The TCP handshake opens a connection in three steps: the client sends SYN, the server answers with SYN-ACK, and the client confirms with ACK before any data flows.",
        /* 3 */ "Sourdough bread rises without commercial yeast. A starter of flour and water cultivates wild yeast and lactic acid bacteria, which give the loaf its tang and open crumb.",
        /* 4 */ "Solar panels convert sunlight into electricity using photovoltaic cells made from silicon. An inverter then changes the direct current into alternating current for household use.",
        /* 5 */ "Vaccines train the immune system by exposing it to a harmless piece or weakened form of a pathogen. The body then remembers the threat and responds faster on real infection.",
        /* 6 */ "In chess, controlling the centre early gives pieces more room. Openings such as the Italian Game and the Queen's Gambit develop the knights and bishops toward the middle squares.",
        /* 7 */ "A Kubernetes pod is the smallest deployable unit and wraps one or more containers that share network and storage. Controllers such as Deployments keep the desired number of pods running.",
        /* 8 */ "Marathon training builds weekly mileage gradually, with one long run each week. Tapering the final two weeks lets muscles recover before race day.",
        /* 9 */ "Volcanoes form where molten rock called magma rises through the crust. Eruptions release lava, ash and gas, and the cooled material builds the mountain layer by layer.",
        /* 10 */ "Compound interest earns interest on both the original deposit and the interest already added. Over many years the balance grows faster than with simple interest.",
        /* 11 */ "A JSON Web Token carries signed claims such as the user and an expiry time. The server verifies the signature on each request instead of storing a session.",
        /* 12 */ "Recycling plastic starts with sorting by resin type. The plastic is shredded, washed, melted and formed into pellets that manufacturers use to make new products.",
        /* 13 */ "Ocean tides are caused mainly by the moon's gravity pulling on the water. Most coasts see two high tides and two low tides each day.",
        /* 14 */ "Passwords should be stored as salted hashes made with a slow algorithm such as bcrypt or Argon2, so a stolen database does not reveal the original passwords."
    };

    /** question, index of the correct passage */
    static final Object[][] QUESTIONS = {
        {"What water temperature is best for pour-over coffee?", 0},
        {"Where in the plant cell does photosynthesis happen?", 1},
        {"What are the three steps of the TCP handshake?", 2},
        {"Why does sourdough bread have a tangy taste?", 3},
        {"How does an inverter help a solar panel system?", 4},
        {"How do vaccines help the body fight infection?", 5},
        {"Which chess openings develop the knights and bishops?", 6},
        {"What does a Kubernetes pod contain?", 7},
        {"How long should a marathon taper last?", 8},
        {"What does a volcano erupt?", 9},
        {"How is compound interest different from simple interest?", 10},
        {"What does the server check on each request with a JWT?", 11},
        {"How is plastic recycled into pellets?", 12},
        // Paraphrases with little vocabulary overlap: expected to be hard for a lexical embedder.
        {"Why do the seas rise and fall twice daily?", 13},
        {"How should a website keep user logins safe if its database leaks?", 14}
    };

    static final String[] OFF_TOPIC = {
        "Who won the football world cup in 2010?",
        "What is the capital city of Australia?",
        "How many legs does a spider have?",
        "Translate good morning into Spanish."
    };

    /** The previous implementation: a pseudo-random vector seeded by the text's hash. */
    static class RandomBaseline implements Embedder {
        public String name() { return "random-baseline"; }
        public float[] embed(String text) {
            float[] v = new float[64];
            Random r = new Random(text.hashCode());
            for (int i = 0; i < v.length; i++) v[i] = r.nextFloat();
            return v;
        }
    }

    record Result(double precisionAt1, double precisionAt3, double mrr, List<Integer> misses) {}

    static Result evaluate(Embedder e) {
        List<float[]> docs = e.embedAll(List.of(PASSAGES));
        int top1 = 0, top3 = 0;
        double rr = 0;
        List<Integer> misses = new ArrayList<>();
        for (int q = 0; q < QUESTIONS.length; q++) {
            float[] qv = e.embed((String) QUESTIONS[q][0]);
            int expected = (Integer) QUESTIONS[q][1];
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < docs.size(); i++) order.add(i);
            order.sort((a, b) -> Double.compare(Embedder.cosine(docs.get(b), qv), Embedder.cosine(docs.get(a), qv)));
            int rank = order.indexOf(expected) + 1;
            if (rank == 1) top1++; else misses.add(q);
            if (rank <= 3) top3++;
            rr += 1.0 / rank;
        }
        int n = QUESTIONS.length;
        return new Result((double) top1 / n, (double) top3 / n, rr / n, misses);
    }

    static double bestScore(Embedder e, String question) {
        float[] qv = e.embed(question);
        return e.embedAll(List.of(PASSAGES)).stream().mapToDouble(d -> Embedder.cosine(d, qv)).max().orElse(0);
    }

    @Test
    void hashingEmbedderRetrievesFarBetterThanTheRandomBaseline() {
        Result hashing = evaluate(new HashingEmbedder());
        Result random = evaluate(new RandomBaseline());
        System.out.printf("RETRIEVAL hashing: P@1=%.2f P@3=%.2f MRR=%.2f misses=%s%n",
                hashing.precisionAt1(), hashing.precisionAt3(), hashing.mrr(), hashing.misses());
        System.out.printf("RETRIEVAL random : P@1=%.2f P@3=%.2f MRR=%.2f%n",
                random.precisionAt1(), random.precisionAt3(), random.mrr());
        assertTrue(hashing.precisionAt1() >= 0.9, "P@1 was " + hashing.precisionAt1());
        assertTrue(random.precisionAt1() <= 0.35, "baseline P@1 was " + random.precisionAt1());
    }

    @Test
    void cosineAloneIsNotASafeAnswerabilityGate() {
        // A single shared common word ("many") gives an off-topic question a non-trivial score, which is why
        // answerability is decided by ExtractiveAnswerer's content-word check and not by a score threshold.
        HashingEmbedder e = new HashingEmbedder();
        double maxOffTopic = 0;
        for (String q : OFF_TOPIC) maxOffTopic = Math.max(maxOffTopic, bestScore(e, q));
        System.out.printf("RETRIEVAL off-topic best cosine=%.3f%n", maxOffTopic);
        assertTrue(maxOffTopic > 0, "expected spurious overlap to be visible in this corpus");
    }
}
