package com.omnibrain.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;

public class OpenAiEmbedder {

    private static final String URL = "https://api.openai.com/v1/embeddings";
    private static final String MODEL = "text-embedding-3-small";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey = System.getenv("OPENAI_API_KEY");

    public float[] embed(String text) {
        try {
            String bodyJson = """
            {
              "model": "%s",
              "input": %s
            }
            """.formatted(MODEL, mapper.writeValueAsString(text));

            Request request = new Request.Builder()
                    .url(URL)
                    .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                JsonNode root = mapper.readTree(response.body().string());
                JsonNode vector = root.at("/data/0/embedding");

                float[] embedding = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    embedding[i] = (float) vector.get(i).asDouble();
                }
                return embedding;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
