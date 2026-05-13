package com.messenger.mini_messenger.dto.redis;

import java.time.Instant;
import java.util.UUID;

public record RedisConversationKeyValue(
        UUID sessionKeyId,
        UUID userId,
        UUID conversationId,
        int keyVersion,
        String encryptedConversationKey,
        Instant expiresAt
) {
}
