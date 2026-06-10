package io.github.maksim0840.internalapi.extraction_result.v1.dto;

import java.time.Instant;
import java.util.Map;

public record ExtractionResultDTO(
        String id,
        String url,
        String userId,
        Map<String, Object> jsonResult,
        Instant createdAt
) {
}
