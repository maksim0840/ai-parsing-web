package io.github.maksim0840.apigateway.dto.api;

import io.github.maksim0840.internalapi.parsing_param.v1.dto.HtmlParserParamsDTO;
import io.github.maksim0840.internalapi.parsing_param.v1.dto.HtmlPreprocessingParamsDTO;
import io.github.maksim0840.internalapi.parsing_param.v1.dto.LLMParamsDTO;

public record ParamsCreateApiRequest(
        String name,
        HtmlParserParamsDTO htmlParserParams,
        HtmlPreprocessingParamsDTO htmlPreprocessingParams,
        LLMParamsDTO llmParams
) {
}
