package com.messenger.mini_messenger.dto.response;

import com.messenger.mini_messenger.enums.UserRole;
import com.messenger.mini_messenger.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        String displayName,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
