package com.omnibrain.controller;

import com.omnibrain.model.ChatRequest;
import com.omnibrain.model.ChatResponse;
import com.omnibrain.service.AiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }
@GetMapping("/chat")
public String test() {
    return "Backend is alive. Use POST to chat.";
}

    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> body) {
        return aiService.chat(body.get("prompt"));
    }
}
