package com.omnibrain.controller;

import com.omnibrain.repository.AiUsageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AiUsageRepository usageRepository;

    public AdminController(AiUsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    @GetMapping("/usage/stats")
    public Map<String, Object> stats() {

        long totalPrompts = usageRepository.count();
        long totalUsers = usageRepository.findAll()
                .stream()
                .map(u -> u.getUserId())
                .distinct()
                .count();

        return Map.of(
                "total_prompts", totalPrompts,
                "total_active_users", totalUsers
        );
    }
}
