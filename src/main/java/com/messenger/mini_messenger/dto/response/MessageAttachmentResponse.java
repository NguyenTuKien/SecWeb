package com.messenger.mini_messenger.dto.response;

import com.messenger.mini_messenger.enums.StorageProvider;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageAttachmentResponse(
        UUID id,
        StorageProvider storageProvider,
        String storageKey,
        String encryptedFileKey,
        Map<String, Object> encryptedMetadata,
        Instant createdAt
) {
}
