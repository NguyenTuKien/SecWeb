package com.messenger.mini_messenger.security;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        String username,
        UUID sessionKeyId
) {
}
