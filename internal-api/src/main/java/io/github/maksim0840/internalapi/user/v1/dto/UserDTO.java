package io.github.maksim0840.internalapi.user.v1.dto;

import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import lombok.Builder;

import javax.annotation.Nullable;
import java.time.Instant;

@Builder
@Nullable
public record UserDTO(
        Long id,
        String name,
        String passwordHash,
        UserRole role,
        Instant createdAt
) {
}
