package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.validation.Base64Value;
import com.messenger.mini_messenger.enums.MessageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record SendMessageRequest(
        @Size(max = 100) String clientMessageId,
        @NotBlank @Base64Value String cipherData,
        @NotBlank @Base64Value String iv,
        @Size(max = 512) @Base64Value String aad,
        @Min(1) int keyVersion,
        @NotNull MessageType messageType,
        @NotNull Instant clientCreatedAt,
        @Size(max = 20) List<@Valid MessageAttachmentRequest> attachments
) {
}
