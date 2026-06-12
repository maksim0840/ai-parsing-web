package io.github.maksim0840.internalapi.user.v1.dto;

public record LLMParamsDTO(
        String modelName,
        String systemMessage,
        String userMessage,
        Double temperature,
        Integer maxOutputTokens
) {
}
