package com.messenger.mini_messenger.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MessageCreatedResponse(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        Instant serverCreatedAt
) {
}
