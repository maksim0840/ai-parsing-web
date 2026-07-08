package io.github.maksim0840.apigateway.dto.api;

import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;

import java.time.Instant;
import java.util.Map;

public record ResultResponse(
        String id,
        String url,
        ResultFormat resultFormat,
        String result,
        Instant createdAt
) {
}
