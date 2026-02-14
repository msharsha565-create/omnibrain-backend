package com.omnibrain.service;

import com.omnibrain.entity.AiUsage;
import com.omnibrain.entity.User;
import com.omnibrain.repository.AiUsageRepository;
import com.omnibrain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AiService {

    private final AiUsageRepository usageRepository;
    private final UserRepository userRepository;

    public AiService(
            AiUsageRepository usageRepository,
            UserRepository userRepository
    ) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
    }

    public String chat(String prompt) {

        String userId = "anonymous";

        // Auto-create anonymous user if not exists
        User user = userRepository.findByUsername(userId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(userId);
                    newUser.setPassword("dev");
                    newUser.setPlan("FREE");
                    return userRepository.save(newUser);
                });

        // Count today's usage
        long todayCount =
                usageRepository.countByUserIdAndCreatedAtAfter(
                        userId,
                        LocalDate.now().atStartOfDay()
                );

        long limit = switch (user.getPlan()) {
            case "PRO" -> 100;
            case "ENTERPRISE" -> Long.MAX_VALUE;
            default -> 5;
        };

        if (todayCount >= limit) {
            return "🚫 Daily limit reached for plan: "
                    + user.getPlan() + ". Upgrade to continue.";
        }

        // 🔥 Replace this with real OpenAI call later
        String response = "AI Response: " + prompt;

        // Save usage
        AiUsage usage = new AiUsage();
        usage.setUserId(userId);
        usage.setPrompt(prompt);
        usage.setProvider("mock");
        usageRepository.save(usage);

        return response;
    }
}
