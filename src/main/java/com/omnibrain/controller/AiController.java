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

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = aiService.chat(request.getPrompt());
        return new ChatResponse(reply);
    }
}
