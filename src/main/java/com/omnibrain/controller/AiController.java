package com.omnibrain.controller;

import com.omnibrain.service.AiService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    static final int MAX_PROMPT_CHARS = 4000;

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/chat")
    public String test() {
        return "Backend is alive. Use POST to chat.";
    }

    @PostMapping("/chat")
    public String chat(Authentication authentication, @RequestBody Map<String, String> body) {
        String prompt = body == null ? null : body.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        if (prompt.length() > MAX_PROMPT_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "prompt is longer than " + MAX_PROMPT_CHARS + " characters");
        }
        return aiService.chat(authentication.getName(), prompt);
    }
}
