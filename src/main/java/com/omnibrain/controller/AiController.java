package com.omnibrain.controller;

import com.omnibrain.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;   // 👈 YOU NEED THIS

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // Test endpoint
    @GetMapping("/chat")
    public String test() {
        return "Backend is alive. Use POST to chat.";
    }

    // Actual AI endpoint
    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> body) {
        return aiService.chat(body.get("prompt"));
    }
}
