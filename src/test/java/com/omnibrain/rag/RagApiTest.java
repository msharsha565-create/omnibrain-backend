package com.omnibrain.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RagApiTest {

    static final String DOC = RagServiceTest.DOC;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token() throws Exception {
        String name = "u" + UUID.randomUUID().toString().substring(0, 8);
        String creds = "{\"username\":\"" + name + "\",\"password\":\"correct-horse-battery\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds)).andExpect(status().isCreated());
        MvcResult r = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds)).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    private org.springframework.test.web.servlet.ResultActions upload(String token, String name, byte[] content) throws Exception {
        return mvc.perform(multipart("/api/rag/upload").file(file(name, content)).header("Authorization", "Bearer " + token));
    }

    private JsonNode ask(String token, String question) throws Exception {
        MvcResult r = mvc.perform(post("/api/rag/ask").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("question", question))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    @Test
    void uploadThenAskReturnsACitedAnswerWithSources() throws Exception {
        String t = token();
        upload(t, "coffee.txt", DOC.getBytes(StandardCharsets.UTF_8)).andExpect(status().isOk());
        JsonNode a = ask(t, "What water temperature is best for pour-over coffee?");
        assertTrue(a.get("answerable").asBoolean());
        assertTrue(a.get("answer").asText().contains("ninety-three"));
        assertEquals("coffee.txt", a.get("sources").get(0).get("source").asText());
    }

    @Test
    void questionsOutsideTheDocumentsAreAnsweredWithNotFound() throws Exception {
        String t = token();
        upload(t, "coffee.txt", DOC.getBytes(StandardCharsets.UTF_8)).andExpect(status().isOk());
        JsonNode a = ask(t, "How many legs does a spider have?");
        assertFalse(a.get("answerable").asBoolean());
        assertEquals(RagService.NOT_FOUND, a.get("answer").asText());
    }

    @Test
    void documentsAreIsolatedPerUser() throws Exception {
        String alice = token(), bob = token();
        upload(alice, "alice.txt", DOC.getBytes(StandardCharsets.UTF_8)).andExpect(status().isOk());
        JsonNode asBob = ask(bob, "What water temperature is best for pour-over coffee?");
        assertFalse(asBob.get("answerable").asBoolean());
        assertEquals(RagService.NO_DOCUMENTS, asBob.get("answer").asText());
        mvc.perform(get("/api/rag/documents").header("Authorization", "Bearer " + bob)).andExpect(status().isOk());
        MvcResult list = mvc.perform(get("/api/rag/documents").header("Authorization", "Bearer " + alice)).andReturn();
        assertEquals("alice.txt", json.readTree(list.getResponse().getContentAsString()).get(0).get("source").asText());
        // Bob deleting "alice.txt" removes nothing of Alice's
        mvc.perform(delete("/api/rag/documents").param("source", "alice.txt").header("Authorization", "Bearer " + bob))
                .andExpect(status().isOk());
        assertTrue(ask(alice, "What water temperature is best for pour-over coffee?").get("answerable").asBoolean());
    }

    @Test
    void listAndDeleteDocuments() throws Exception {
        String t = token();
        upload(t, "coffee.txt", DOC.getBytes(StandardCharsets.UTF_8)).andExpect(status().isOk());
        MvcResult del = mvc.perform(delete("/api/rag/documents").param("source", "coffee.txt")
                .header("Authorization", "Bearer " + t)).andExpect(status().isOk()).andReturn();
        assertTrue(json.readTree(del.getResponse().getContentAsString()).get("deletedChunks").asLong() >= 1);
        MvcResult list = mvc.perform(get("/api/rag/documents").header("Authorization", "Bearer " + t)).andReturn();
        assertEquals(0, json.readTree(list.getResponse().getContentAsString()).size());
    }

    @Test
    void rejectsUnsupportedBinaryAndEmptyUploads() throws Exception {
        String t = token();
        upload(t, "malware.exe", "x".getBytes()).andExpect(status().isUnsupportedMediaType());
        upload(t, "noextension", "x".getBytes()).andExpect(status().isUnsupportedMediaType());
        upload(t, "fake.txt", new byte[]{'a', 0, 'b'}).andExpect(status().isBadRequest());
        upload(t, "blank.txt", "   \n  ".getBytes()).andExpect(status().isUnprocessableEntity());
        upload(t, "broken.pdf", "this is not a pdf".getBytes()).andExpect(status().isBadRequest());
    }

    @Test
    void pathComponentsInTheUploadNameAreStripped() throws Exception {
        String t = token();
        upload(t, "../../etc/notes.txt", DOC.getBytes(StandardCharsets.UTF_8)).andExpect(status().isOk());
        upload(t, "C:\\Users\\me\\win.txt", DOC.getBytes(StandardCharsets.UTF_8)).andExpect(status().isOk());
        MvcResult list = mvc.perform(get("/api/rag/documents").header("Authorization", "Bearer " + t)).andReturn();
        String body = list.getResponse().getContentAsString();
        assertTrue(body.contains("\"notes.txt\"") && body.contains("\"win.txt\""), body);
        assertFalse(body.contains(".."));
    }

    @Test
    void validatesTheQuestion() throws Exception {
        String t = token();
        mvc.perform(post("/api/rag/ask").header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"  \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/rag/ask").header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"" + "x".repeat(1001) + "\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/rag/ask").header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    void theStreamingEndpointSendsTheAnswerAndSources() throws Exception {
        String t = token();
        upload(t, "coffee.txt", DOC.getBytes(StandardCharsets.UTF_8)).andExpect(status().isOk());
        MvcResult started = mvc.perform(post("/api/rag/ask/stream").header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"What water temperature is best for pour-over coffee?\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        started.getAsyncResult(5000);
        String body = mvc.perform(asyncDispatch(started)).andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("ninety-three"), body);
        assertTrue(body.contains("Sources:") && body.contains("coffee.txt"), body);
    }
}
