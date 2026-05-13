package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.validation.Base64Value;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank String idToken,
        @NotBlank @Base64Value String sessionPublicKey,
        @Size(max = 500) String deviceInfo,
        @Valid MasterKeyRequest masterKey
) {
}
