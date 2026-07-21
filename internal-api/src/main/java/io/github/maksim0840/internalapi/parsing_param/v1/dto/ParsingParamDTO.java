package io.github.maksim0840.internalapi.parsing_param.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.time.Instant;

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
