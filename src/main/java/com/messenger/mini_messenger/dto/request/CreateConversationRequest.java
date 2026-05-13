package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.enums.ConversationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateConversationRequest(
        @NotNull ConversationType type,
        @Size(max = 255) String name,
        @NotEmpty List<@NotNull UUID> participantIds,
        @NotEmpty List<@Valid EncryptedConversationKeyRequest> encryptedKeys
) {
}
