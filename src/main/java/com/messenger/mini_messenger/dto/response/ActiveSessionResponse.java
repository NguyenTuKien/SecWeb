package com.messenger.mini_messenger.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionResponse(
        UUID sessionKeyId,
        String sessionPublicKey,
        String deviceInfo,
        Instant createdAt,
        Instant lastActiveAt
) {
}
