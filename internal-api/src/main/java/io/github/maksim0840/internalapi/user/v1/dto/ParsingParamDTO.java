package io.github.maksim0840.internalapi.user.v1.dto;

import java.time.Instant;
import java.util.Map;

public record ParsingParamDTO(
        Long id,
        String name,
        Instant createdAt,
        HtmlParserParamsDTO htmlParserParams,
        HtmlPreprocessingParamsDTO htmlPreprocessingParams,
        LLMParamsDTO llmParams
) {
}
