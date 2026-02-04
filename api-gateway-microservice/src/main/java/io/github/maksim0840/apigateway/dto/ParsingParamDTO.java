package io.github.maksim0840.apigateway.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ParsingParamDTO(
        Long id,
        Long userId,
        String name,
        String description,
        Instant createdAt
) {
}
