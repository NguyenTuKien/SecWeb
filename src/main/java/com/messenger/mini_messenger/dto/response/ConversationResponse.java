package com.messenger.mini_messenger.dto.response;

import com.messenger.mini_messenger.enums.ConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        ConversationType type,
        String name,
        int currentKeyVersion,
        Instant lastMessageAt,
        List<UserResponse> participants,
        Instant createdAt,
        Instant updatedAt
) {
}
