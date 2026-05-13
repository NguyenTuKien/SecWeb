package com.messenger.mini_messenger.dto.response;

import com.messenger.mini_messenger.enums.MasterKeyStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MasterKeyResponse(
        UUID id,
        String publicKey,
        String encryptedPrivateKey,
        String privateKeyIv,
        String pinSalt,
        Map<String, Object> kdfParams,
        MasterKeyStatus status,
        Instant createdAt
) {
}
