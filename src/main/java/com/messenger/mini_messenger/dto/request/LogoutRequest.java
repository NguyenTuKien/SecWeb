package com.messenger.mini_messenger.dto.request;

import java.util.UUID;

public record LogoutRequest(UUID sessionKeyId) {
}
