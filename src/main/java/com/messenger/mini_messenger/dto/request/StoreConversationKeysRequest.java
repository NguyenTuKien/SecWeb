package com.messenger.mini_messenger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StoreConversationKeysRequest(
        @Min(1) int newKeyVersion,
        String reason,
        @NotEmpty List<@Valid EncryptedConversationKeyRequest> encryptedKeys
) {
}
