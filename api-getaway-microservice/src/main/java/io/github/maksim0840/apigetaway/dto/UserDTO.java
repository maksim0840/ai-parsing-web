package io.github.maksim0840.apigetaway.dto;

import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UserDTO(
        Long id,
        String name,
        String passwordHash,
        UserRole role,
        Instant createdAt
) {
}
