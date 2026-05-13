package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.validation.Base64Value;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record MasterKeyRequest(
        @NotBlank @Base64Value String publicKey,
        @NotBlank @Base64Value String encryptedPrivateKey,
        @NotBlank @Base64Value String privateKeyIv,
        @NotBlank @Base64Value String pinSalt,
        @NotNull Map<String, Object> kdfParams
) {
}
