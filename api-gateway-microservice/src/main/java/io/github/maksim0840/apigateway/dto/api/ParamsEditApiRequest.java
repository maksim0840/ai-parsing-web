package io.github.maksim0840.apigateway.dto.api;

import io.github.maksim0840.internalapi.user.v1.dto.HtmlParserParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.HtmlPreprocessingParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.LLMParamsDTO;

public record ParamsEditApiRequest(
        Long id,
        String name,
        HtmlParserParamsDTO htmlParserParams,
        HtmlPreprocessingParamsDTO htmlPreprocessingParams,
        LLMParamsDTO llmParams
) {
}
