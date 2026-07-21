package io.github.maksim0840.apigateway.dto.api;

public record AuthApiLoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
