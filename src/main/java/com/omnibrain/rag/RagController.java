package com.omnibrain.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /* ========= UPLOAD ========= */
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {

        String extractedText;

        // PDF-safe extraction
        if (file.getOriginalFilename() != null &&
            file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {

            extractedText = extractPdfText(file);

        } else {
            extractedText = new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        int chunks = ragService.ingest(
                extractedText,
                file.getOriginalFilename()
        );

        return "Uploaded and indexed " + chunks +
               " chunks from " + file.getOriginalFilename();
    }

    /* ========= ASK (STREAM) ========= */
    @PostMapping(
            value = "/ask/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter ask(@RequestBody AskRequest request) {
        return ragService.streamAnswer(request.getQuestion());
    }

    /* ========= PDF HELPER ========= */
    private String extractPdfText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }
}
