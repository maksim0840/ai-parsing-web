package io.github.maksim0840.apigateway.security;

public record JwtPrincipal(
        Long userId,
        String name
) {
}
