package com.omnibrain.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the embeddings client against a real local HTTP server that speaks the OpenAI wire format. */
class OpenAiCompatEmbedderTest {

    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final List<String> auth = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String forcedBody = null;
    private String baseUrl;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            JsonNode req = json.readTree(exchange.getRequestBody().readAllBytes());
            requests.add(req);
            auth.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            String body = forcedBody;
            if (body == null) {
                var data = json.createArrayNode();
                for (JsonNode ignored : req.get("input")) {
                    var item = json.createObjectNode();
                    var emb = item.putArray("embedding");
                    emb.add(3.0).add(4.0).add(0.0); // length 5: must come back normalised to 0.6, 0.8, 0
                    data.add(item);
                }
                body = json.createObjectNode().set("data", data).toString();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsTheOpenAiRequestShapeAndNormalisesVectors() {
        var embedder = new OpenAiCompatEmbedder(baseUrl, "my-model", "secret-key");
        float[] v = embedder.embed("hello");
        assertArrayEquals(new float[]{0.6f, 0.8f, 0f}, v, 1e-6f);
        assertEquals("my-model", requests.get(0).get("model").asText());
        assertEquals("hello", requests.get(0).get("input").get(0).asText());
        assertEquals("Bearer secret-key", auth.get(0));
        assertEquals("openai:my-model", embedder.name());
    }

    @Test
    void omitsTheAuthorizationHeaderWhenThereIsNoKey() {
        new OpenAiCompatEmbedder(baseUrl, "m", "").embed("x");
        assertEquals("null", auth.get(0));
    }

    @Test
    void batchesLargeInputsIntoMultipleRequests() {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < 150; i++) texts.add("text " + i);
        List<float[]> vectors = new OpenAiCompatEmbedder(baseUrl, "m", "k").embedAll(texts);
        assertEquals(150, vectors.size());
        assertEquals(3, requests.size()); // 64 + 64 + 22
        assertEquals(22, requests.get(2).get("input").size());
    }

    @Test
    void anErrorStatusSurfacesTheProvidersMessage() {
        status = 429;
        forcedBody = "{\"error\":\"quota exceeded\"}";
        var ex = assertThrows(IllegalStateException.class, () -> new OpenAiCompatEmbedder(baseUrl, "m", "k").embed("x"));
        assertTrue(ex.getMessage().contains("429") && ex.getMessage().contains("quota exceeded"), ex.getMessage());
    }

    @Test
    void aResponseWithTheWrongNumberOfVectorsIsRejected() {
        forcedBody = "{\"data\":[]}";
        assertThrows(IllegalStateException.class, () -> new OpenAiCompatEmbedder(baseUrl, "m", "k").embed("x"));
    }
}
