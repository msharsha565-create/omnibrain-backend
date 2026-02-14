package com.omnibrain.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String root() {
        return "OmniBrain Backend Running 🚀";
    }

    @GetMapping("/api/health")
    public String health() {
        return "Healthy ✅";
    }
}
