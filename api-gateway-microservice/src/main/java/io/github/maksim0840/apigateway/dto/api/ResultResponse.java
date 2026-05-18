package io.github.maksim0840.apigateway.dto.api;

import java.time.Instant;
import java.util.Map;

public record ResultResponse(
        String id,
        String url,
        Map<String, Object> jsonResult,
        Instant createdAt
) {
}
