package com.omnibrain.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAIProvider implements AiProvider {

    private final RestClient restClient;

    public OpenAIProvider(@Value("${openai.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String chat(String prompt) {
String systemPrompt = """
You are an enterprise Identity & Access Management expert.

Specialize in:
- Joiner Mover Leaver lifecycle
- RBAC design
- Access certification
- Zero Trust architecture
- SailPoint
- Okta
- SOX & HIPAA compliance
- IAM interview preparation
""";

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                           Map.of("role", "system", "content",
       "You are an expert Identity and Access Management (IAM) architect. " +
       "You specialize in SailPoint, Okta, Entra ID, ForgeRock, and IAM audit compliance. " +
       "Provide precise, professional, and technically accurate IAM guidance."),
Map.of("role", "user", "content", prompt)
 )
            );

            Map response = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<Map> choices = (List<Map>) response.get("choices");
            Map message = (Map) choices.get(0).get("message");

            return message.get("content").toString();

        } catch (Exception e) {
            return "AI Provider Error: " + e.getMessage();
        }
    }
}
