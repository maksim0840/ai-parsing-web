package io.github.maksim0840.apigateway.dto.api;

public record RegisterResponse(
        Long userId,
        String username,
        String message
) {
}
