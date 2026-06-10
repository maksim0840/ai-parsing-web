package io.github.maksim0840.internalapi.extraction_result.v1.dto;

import lombok.Builder;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

@Builder
@Nullable
public record ExtractionResultDTO(
        String id,
        String url,
        String userId,
        Map<String, Object> jsonResult,
        Instant createdAt
) {
}
