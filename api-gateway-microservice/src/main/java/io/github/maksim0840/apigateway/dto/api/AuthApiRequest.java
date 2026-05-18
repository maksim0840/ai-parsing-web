package io.github.maksim0840.apigateway.dto.api;

public record AuthApiRequest(
        String username,
        String password
) {
}
