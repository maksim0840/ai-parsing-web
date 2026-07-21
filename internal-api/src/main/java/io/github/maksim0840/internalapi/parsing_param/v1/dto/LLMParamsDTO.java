package io.github.maksim0840.internalapi.parsing_param.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;

@Builder
@Nullable
public record LLMParamsDTO(
        String modelName,
        String systemMessage,
        String userMessage,
        Double temperature,
        Integer maxOutputTokens
) {
}
