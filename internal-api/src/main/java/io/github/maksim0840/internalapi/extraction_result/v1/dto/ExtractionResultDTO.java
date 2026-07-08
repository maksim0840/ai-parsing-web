package io.github.maksim0840.internalapi.extraction_result.v1.dto;

import io.github.maksim0840.internalapi.extraction_result.v1.enums.ResultFormat;
import lombok.Builder;

import javax.annotation.Nullable;
import java.time.Instant;

@Builder
@Nullable
public record ExtractionResultDTO(
        String id,
        String url,
        String userId,
        ResultFormat resultFormat,
        String result,
        Instant createdAt
) {
}
