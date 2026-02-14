package com.omnibrain.service;

import com.omnibrain.ai.AiProviderFactory;
import com.omnibrain.entity.AiUsage;
import com.omnibrain.entity.User;
import com.omnibrain.repository.AiUsageRepository;
import com.omnibrain.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AiService {

    private final AiProviderFactory providerFactory;
    private final AiUsageRepository usageRepository;
    private final UserRepository userRepository;

    public AiService(
            AiProviderFactory providerFactory,
            AiUsageRepository usageRepository,
            UserRepository userRepository
    ) {
        this.providerFactory = providerFactory;
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
    }

    public String chat(String prompt) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String userId = "anonymous";

        if (authentication != null && authentication.isAuthenticated()) {
            userId = authentication.getName();
        }

        // 🔹 Get today's usage
        long todayCount =
                usageRepository.countByUserIdAndCreatedAtAfter(
                        userId,
                        LocalDate.now().atStartOfDay()
                );

        // 🔹 Get user plan
        String plan = userRepository.findByUsername(userId)
                .map(User::getPlan)
                .orElse("FREE");

        long limit = switch (plan) {
            case "PRO" -> 100;
            case "ENTERPRISE" -> Long.MAX_VALUE;
            default -> 5;
        };

        if (todayCount >= limit) {
            return "🚫 Daily limit reached for plan: "
                    + plan + ". Upgrade to continue.";
        }

        // 🔹 Call AI provider
        String response =
                providerFactory.getProvider().chat(prompt);

        // 🔹 Save usage
        AiUsage usage = new AiUsage();
        usage.setUserId(userId);
        usage.setPrompt(prompt);
        usage.setProvider("mock");

        usageRepository.save(usage);

        return response;
    }
}
