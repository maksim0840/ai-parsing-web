package io.github.maksim0840.apigateway.dto.api;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
}
