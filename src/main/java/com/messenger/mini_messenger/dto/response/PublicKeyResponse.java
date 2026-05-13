package com.messenger.mini_messenger.dto.response;

import java.util.UUID;

public record PublicKeyResponse(
        UUID userId,
        UUID masterKeyId,
        String publicKey
) {
}
