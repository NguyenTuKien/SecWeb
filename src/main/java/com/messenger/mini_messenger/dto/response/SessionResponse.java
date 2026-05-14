package com.messenger.mini_messenger.dto.response;

import com.messenger.mini_messenger.enums.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID sessionKeyId,
        String sessionPublicKey,
        String deviceInfo,
        SessionStatus status,
        Instant createdAt,
        Instant lastActiveAt,
        Instant expiresAt,
        boolean isCurrent
) {
}
