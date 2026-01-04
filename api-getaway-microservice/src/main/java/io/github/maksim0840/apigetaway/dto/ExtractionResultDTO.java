package io.github.maksim0840.apigetaway.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ExtractionResultDTO(
        String id,
        String url,
        String userId,
        Map<String, Object> jsonResult,
        Instant createdAt
) {
}
