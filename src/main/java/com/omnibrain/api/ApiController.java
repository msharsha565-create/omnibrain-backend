package com.omnibrain.backend.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "service", "omnibrain-backend",
            "status", "running",
            "version", "0.1"
        );
    }
}
