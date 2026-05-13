package com.messenger.mini_messenger.dto.response;

import com.messenger.mini_messenger.enums.ConversationKeyRecipientType;

import java.util.UUID;

public record EncryptedConversationKeyResponse(
        ConversationKeyRecipientType recipientType,
        UUID recipientKeyId,
        String encryptedConversationKey,
        int keyVersion
) {
}
