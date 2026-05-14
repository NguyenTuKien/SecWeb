package com.messenger.mini_messenger.dto.response;

import com.messenger.mini_messenger.enums.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        int keyVersion,
        String clientMessageId,
        String cipherData,
        String iv,
        String aad,
        MessageType messageType,
        Instant clientCreatedAt,
        Instant serverCreatedAt,
        Instant editedAt,
        List<MessageAttachmentResponse> attachments
) {
}
