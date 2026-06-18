package io.github.maksim0840.internalapi.user.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

@Builder
@Nullable
public record ParsingParamDTO(
        Long id,
        Long userId,
        String name,
        Instant createdAt,
        HtmlParserParamsDTO htmlParserParams,
        HtmlPreprocessingParamsDTO htmlPreprocessingParams,
        LLMParamsDTO llmParams
) {
}
