package io.github.maksim0840.apigateway.dto.api;

public record LLMApiRequest(
        String modelName,
        String systemMessage,
        String userMessage,
        double temperature,
        int maxOutputTokens
) {
}
