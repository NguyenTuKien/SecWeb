package com.messenger.mini_messenger.dto.websocket;

import com.messenger.mini_messenger.dto.request.MessageAttachmentRequest;
import com.messenger.mini_messenger.enums.MessageType;
import com.messenger.mini_messenger.validation.Base64Value;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RealtimeMessageRequest(
        @NotNull UUID conversationId,
        @NotNull UUID senderId,
        @Size(max = 100) String clientMessageId,
        @NotBlank @Base64Value String cipherData,
        @NotBlank @Base64Value String iv,
        @Size(max = 512) @Base64Value String aad,
        @Min(1) Integer keyVersion,
        MessageType messageType,
        Instant clientCreatedAt,
        @Size(max = 20) List<@Valid MessageAttachmentRequest> attachments
) {
}
