package io.github.maksim0840.internalapi.user.v1.dto;

import java.util.Map;

public record HtmlParserParamsDTO(
    Boolean downloadImages,
    Map<String, String> headers,
    Map<String, String> cookies,
    Map<String, String> proxy,
    String pageComplexity,
    Integer additionalPageLoadTimeoutS
) {
}
