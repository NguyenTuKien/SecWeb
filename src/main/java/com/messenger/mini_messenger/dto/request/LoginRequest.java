package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.validation.Base64Value;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 255) String usernameOrEmail,
        @NotBlank @Size(max = 128) String password,
        @NotBlank @Base64Value String sessionPublicKey,
        @Size(max = 500) String deviceInfo
) {
}
