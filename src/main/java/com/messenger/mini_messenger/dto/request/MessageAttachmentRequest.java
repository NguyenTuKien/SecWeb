package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.enums.StorageProvider;
import com.messenger.mini_messenger.validation.Base64Value;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageAttachmentRequest(
        @NotNull StorageProvider storageProvider,
        @NotBlank @Size(max = 500) String storageKey,
        @NotBlank @Base64Value String encryptedFileKey,
        @NotBlank @Base64Value String encryptedMetadata
) {
}
