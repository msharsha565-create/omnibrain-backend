package com.omnibrain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MockMvc never performs the servlet container's error-page dispatch, so it cannot show how errors
 * look on a real server. These tests run against an embedded Tomcat and check that error statuses
 * are not rewritten to 401 by the security chain.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RealServerErrorsTest {

    @LocalServerPort int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String loginAsNewUser() throws Exception {
        String name = "u" + UUID.randomUUID().toString().substring(0, 8);
        String creds = "{\"username\":\"" + name + "\",\"password\":\"correct-horse-battery\"}";
        assertEquals(201, send("POST", "/auth/register", null, creds).statusCode());
        return json.readTree(send("POST", "/auth/login", null, creds).body()).get("token").asText();
    }

    @Test
    void anOrdinaryUserOnTheAdminAreaGets403NotUnauthorized401() throws Exception {
        assertEquals(403, send("GET", "/admin/usage/stats", loginAsNewUser(), null).statusCode());
    }

    @Test
    void anonymousCallersStillGet401() throws Exception {
        assertEquals(401, send("GET", "/admin/usage/stats", null, null).statusCode());
        assertEquals(401, send("POST", "/api/ai/chat", null, "{\"prompt\":\"hi\"}").statusCode());
    }

    @Test
    void validationErrorsKeepTheirOwnStatusForAuthenticatedUsers() throws Exception {
        String token = loginAsNewUser();
        assertEquals(400, send("POST", "/api/ai/chat", token, "{}").statusCode());
        assertEquals(400, send("POST", "/api/rag/ask", token, "{\"question\":\"  \"}").statusCode());
    }
}
