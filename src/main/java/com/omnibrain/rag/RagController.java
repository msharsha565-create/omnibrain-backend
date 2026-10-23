package com.omnibrain.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    static final int MAX_QUESTION_CHARS = 1000;
    static final int MAX_FILENAME_CHARS = 120;
    private static final Set<String> TYPES = Set.of("pdf", "txt", "md");

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
        String name = safeName(file.getOriginalFilename());
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!TYPES.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only .pdf, .txt and .md files are supported");
        }
        byte[] bytes = file.getBytes();
        String text;
        if (ext.equals("pdf")) {
            text = extractPdf(bytes);
        } else {
            for (byte b : bytes) {
                if (b == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File does not look like text");
            }
            text = new String(bytes, StandardCharsets.UTF_8);
        }
        int chunks;
        try {
            chunks = ragService.ingest(auth.getName(), name, text);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
        if (chunks == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No readable text found in " + name);
        }
        return "Uploaded and indexed " + chunks + " chunks from " + name;
    }

    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RagAnswer askJson(Authentication auth, @RequestBody AskRequest request) {
        try {
            return ragService.ask(auth.getName(), validQuestion(request));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    @PostMapping(value = "/ask/stream", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(Authentication auth, @RequestBody AskRequest request) {
        return ragService.streamAnswer(auth.getName(), validQuestion(request));
    }

    @GetMapping("/documents")
    public List<Map<String, Object>> documents(Authentication auth) {
        return ragService.documents(auth.getName()).stream()
                .map(d -> Map.<String, Object>of("source", d.getSource(), "chunks", d.getChunks()))
                .toList();
    }

    @DeleteMapping("/documents")
    public Map<String, Long> delete(Authentication auth, @RequestParam("source") String source) {
        return Map.of("deletedChunks", ragService.delete(auth.getName(), source));
    }

    private static String validQuestion(AskRequest request) {
        String q = request == null ? null : request.getQuestion();
        if (q == null || q.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        if (q.length() > MAX_QUESTION_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is longer than " + MAX_QUESTION_CHARS + " characters");
        }
        return q.trim();
    }

    /** Strips any directory part (including Windows separators) and bounds the length. */
    static String safeName(String original) {
        if (original == null || original.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file name is required");
        }
        String base = Paths.get(original.replace('\\', '/')).getFileName().toString().replaceAll("[\\p{Cntrl}]", "");
        if (base.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file name is required");
        return base.length() > MAX_FILENAME_CHARS ? base.substring(base.length() - MAX_FILENAME_CHARS) : base;
    }

    private static String extractPdf(byte[] bytes) {
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the PDF");
        }
    }
}
