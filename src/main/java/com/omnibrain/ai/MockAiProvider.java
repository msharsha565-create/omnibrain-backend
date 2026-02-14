package com.omnibrain.ai;

import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String chat(String prompt) {
        return """
                OmniBrain IAM (Demo Mode):

                IAM stands for Identity and Access Management.
                It ensures the right users have the right access
                to the right resources at the right time.
                """;
    }
}
