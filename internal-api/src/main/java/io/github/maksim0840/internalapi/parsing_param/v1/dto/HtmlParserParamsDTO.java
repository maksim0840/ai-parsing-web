package io.github.maksim0840.internalapi.parsing_param.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;

@Builder
@Nullable
public record HtmlParserParamsDTO(
    Boolean downloadImages,
    Map<String, String> headers,
    Map<String, String> cookies,
    Map<String, String> proxy,
    String pageComplexity,
    Integer additionalPageLoadTimeoutS
) {
}
