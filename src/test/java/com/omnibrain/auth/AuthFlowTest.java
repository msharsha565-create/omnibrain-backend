package com.omnibrain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibrain.repository.AiUsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AiUsageRepository usage;

    private static String creds(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String unique() {
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void register(String username) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(creds(username, PASSWORD))).andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(creds(username, password))).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String registerAndLogin(String username) throws Exception {
        register(username);
        return login(username, PASSWORD);
    }

    private org.springframework.test.web.servlet.ResultActions chat(String token, String prompt) throws Exception {
        var req = post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"" + prompt + "\"}");
        if (token != null) req = req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void registerValidatesInputAndRejectsDuplicates() throws Exception {
        String name = unique();
        register(name);
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(creds(name, PASSWORD))).andExpect(status().isConflict());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(creds("ab", PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(creds("bad name!", PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(creds(unique(), "short"))).andExpect(status().isBadRequest());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
    }

    @Autowired com.omnibrain.repository.UserRepository users;

    @Test
    void passwordsAreStoredAsBcryptHashesNotPlainText() throws Exception {
        String name = unique();
        register(name);
        String stored = users.findByUsername(name).orElseThrow().getPassword();
        assertNotEquals(PASSWORD, stored);
        assertTrue(stored.startsWith("$2"), "expected a BCrypt hash");
    }

    @Test
    void loginRequiresTheCorrectPassword() throws Exception {
        String name = unique();
        register(name);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(creds(name, "wrong-password!"))).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(creds("nobody-" + unique(), PASSWORD))).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
        assertFalse(login(name, PASSWORD).isBlank());
    }

    @Test
    void theOldLoginWithJustAUsernameParameterIssuesNothing() throws Exception {
        mvc.perform(post("/auth/login").param("username", "anyone"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void protectedEndpointsNeedAValidToken() throws Exception {
        chat(null, "hi").andExpect(status().isUnauthorized());
        chat("garbage", "hi").andExpect(status().isUnauthorized());
        mvc.perform(post("/api/rag/ask/stream").contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"hi\"}")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/rag/upload").file(new MockMultipartFile(
                "file", "a.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgedAndExpiredTokensAreRejected() throws Exception {
        String forged = new JwtUtil("another-secret-another-secret-another-1", 60).generateToken("admin");
        chat(forged, "hi").andExpect(status().isUnauthorized());
        String expired = new JwtUtil("test-secret-test-secret-test-secret-123456", -1).generateToken("admin");
        chat(expired, "hi").andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedChatWorksAndIsRecordedAgainstTheCaller() throws Exception {
        String name = unique();
        String token = registerAndLogin(name);
        chat(token, "what is IAM?").andExpect(status().isOk());
        assertEquals(1, usage.findAll().stream().filter(u -> name.equals(u.getUserId())).count());
        assertEquals("mock", usage.findAll().stream()
                .filter(u -> name.equals(u.getUserId())).findFirst().orElseThrow().getProvider());
    }

    @Test
    void chatValidatesThePrompt() throws Exception {
        String token = registerAndLogin(unique());
        mvc.perform(post("/api/ai/chat").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        chat(token, "x".repeat(4001)).andExpect(status().isBadRequest());
    }

    @Test
    void freePlanIsLimitedToFivePromptsPerDayPerUser() throws Exception {
        String token = registerAndLogin(unique());
        for (int i = 0; i < 5; i++) {
            assertFalse(chat(token, "q" + i).andExpect(status().isOk()).andReturn()
                    .getResponse().getContentAsString().startsWith("Daily limit"));
        }
        assertTrue(chat(token, "one too many").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString().startsWith("Daily limit reached"));
        // another user is unaffected
        assertFalse(chat(registerAndLogin(unique()), "hi").andReturn()
                .getResponse().getContentAsString().startsWith("Daily limit"));
    }

    @Test
    void adminEndpointsAreForConfiguredAdminsOnly() throws Exception {
        String user = registerAndLogin(unique());
        mvc.perform(get("/admin/usage/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/usage/stats").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
        // "admin" is listed in omnibrain.admin-users for the test profile
        register("admin");
        String admin = login("admin", PASSWORD);
        mvc.perform(get("/admin/usage/stats").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void uploadingADocumentWorksWhenAuthenticated() throws Exception {
        String token = registerAndLogin(unique());
        MvcResult result = mvc.perform(multipart("/api/rag/upload")
                .file(new MockMultipartFile("file", "notes.txt", "text/plain",
                        ("OmniBrain stores document chunks with embeddings. ".repeat(10))
                                .getBytes(StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn();
        assertTrue(result.getResponse().getContentAsString().startsWith("Uploaded and indexed"));
    }
}
