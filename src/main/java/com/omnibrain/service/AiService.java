package com.omnibrain.service;

import com.omnibrain.ai.AiProvider;
import com.omnibrain.ai.AiProviderFactory;
import com.omnibrain.entity.AiUsage;
import com.omnibrain.entity.User;
import com.omnibrain.repository.AiUsageRepository;
import com.omnibrain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AiService {

    private final AiUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final AiProviderFactory providerFactory;

    public AiService(
            AiUsageRepository usageRepository,
            UserRepository userRepository,
            AiProviderFactory providerFactory
    ) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
        this.providerFactory = providerFactory;
    }

    public String chat(String username, String prompt) {

        // Plan comes from the registered user; an unknown user (e.g. deleted after login) gets FREE.
        String plan = userRepository.findByUsername(username)
                .map(User::getPlan)
                .orElse("FREE");

        long todayCount = usageRepository.countByUserIdAndCreatedAtAfter(
                username, LocalDate.now().atStartOfDay());

        long limit = switch (plan == null ? "FREE" : plan) {
            case "PRO" -> 100;
            case "ENTERPRISE" -> Long.MAX_VALUE;
            default -> 5;
        };

        if (todayCount >= limit) {
            return "Daily limit reached for plan: " + plan + ". Upgrade to continue.";
        }

        AiProvider provider = providerFactory.getProvider();
        String response = provider.chat(prompt);

        AiUsage usage = new AiUsage();
        usage.setUserId(username);
        usage.setPrompt(prompt);
        usage.setProvider(provider.getClass().getSimpleName().replace("AiProvider", "").toLowerCase());
        usageRepository.save(usage);

        return response;
    }
}
