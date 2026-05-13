package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.enums.ConversationKeyRecipientType;
import com.messenger.mini_messenger.validation.Base64Value;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EncryptedConversationKeyRequest(
        @NotNull UUID userId,
        @NotNull ConversationKeyRecipientType recipientType,
        @NotNull UUID recipientKeyId,
        @NotBlank @Base64Value String encryptedConversationKey,
        @Min(1) int keyVersion
) {
}
