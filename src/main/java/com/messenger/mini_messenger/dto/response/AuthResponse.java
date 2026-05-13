package com.messenger.mini_messenger.dto.response;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String encryptedRefreshToken,
        UUID sessionKeyId,
        UserResponse user,
        MasterKeyResponse masterKey
) {
}
