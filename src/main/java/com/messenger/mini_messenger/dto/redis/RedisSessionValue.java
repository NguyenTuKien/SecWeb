package com.messenger.mini_messenger.dto.redis;

import com.messenger.mini_messenger.enums.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record RedisSessionValue(
        UUID sessionKeyId,
        UUID userId,
        String sessionPublicKey,
        String masterPublicKey,
        String refreshTokenHash,
        SessionStatus status,
        Instant expiresAt
) {
}
