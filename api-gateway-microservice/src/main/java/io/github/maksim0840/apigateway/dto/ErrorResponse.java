package io.github.maksim0840.apigateway.dto;

public record ErrorResponse(
        String code,
        String message
) {
}
