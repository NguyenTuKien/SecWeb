package com.messenger.mini_messenger.dto.response;

import com.messenger.mini_messenger.enums.StorageProvider;

import java.time.Instant;
import java.util.UUID;

public record MessageAttachmentResponse(
        UUID id,
        StorageProvider storageProvider,
        String storageKey,
        String encryptedFileKey,
        String encryptedMetadata,
        Instant createdAt
) {
}
