package com.omnibrain.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real semantic embeddings from any OpenAI-compatible {@code /embeddings} endpoint (OpenAI,
 * Gemini's compatibility endpoint, Ollama, vLLM). Enabled with {@code EMBEDDINGS_PROVIDER=openai}.
 * Unlike the hashing embedder it understands paraphrase.
 */
@Component
@ConditionalOnProperty(name = "omnibrain.embeddings.provider", havingValue = "openai")
public class OpenAiCompatEmbedder implements Embedder {

    private static final int BATCH = 64;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String url;
    private final String model;
    private final String apiKey;

    public OpenAiCompatEmbedder(
            @Value("${omnibrain.embeddings.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${omnibrain.embeddings.model:text-embedding-3-small}") String model,
            @Value("${omnibrain.embeddings.api-key:}") String apiKey
    ) {
        this.url = baseUrl.replaceAll("/+$", "") + "/embeddings";
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return "openai:" + model;
    }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += BATCH) {
            out.addAll(request(texts.subList(i, Math.min(texts.size(), i + BATCH))));
        }
        return out;
    }

    private List<float[]> request(List<String> batch) {
        try {
            String body = json.writeValueAsString(Map.of("model", model, "input", batch));
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (!apiKey.isBlank()) req.header("Authorization", "Bearer " + apiKey);
            HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                String snippet = resp.body().length() > 300 ? resp.body().substring(0, 300) : resp.body();
                throw new IllegalStateException("Embeddings request failed: HTTP " + resp.statusCode() + " " + snippet);
            }
            JsonNode data = json.readTree(resp.body()).path("data");
            if (!data.isArray() || data.size() != batch.size()) {
                throw new IllegalStateException("Embeddings response did not contain one vector per input");
            }
            List<float[]> vectors = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode values = item.path("embedding");
                float[] v = new float[values.size()];
                for (int k = 0; k < v.length; k++) v[k] = (float) values.get(k).asDouble();
                vectors.add(Embedder.normalize(v));
            }
            return vectors;
        } catch (IOException e) {
            throw new IllegalStateException("Embeddings request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embeddings request interrupted", e);
        }
    }
}
