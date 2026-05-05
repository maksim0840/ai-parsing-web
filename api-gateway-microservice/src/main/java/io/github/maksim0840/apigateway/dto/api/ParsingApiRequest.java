package io.github.maksim0840.apigateway.dto.api;

import java.util.Map;

public record ParsingApiRequest(
        String url,
        boolean downloadImages,
        Map<String, String> headers,
        Map<String, String> cookies,
        Map<String, String> proxy,
        String pageComplexity,
        int additionalPageLoadTimeoutS
) {
}
