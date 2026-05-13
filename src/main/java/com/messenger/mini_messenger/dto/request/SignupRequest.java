package com.messenger.mini_messenger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @Email @Size(max = 255) String email,
        @Size(max = 100) String displayName,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(min = 8, max = 128) String confirmPassword,
        @Valid @NotNull MasterKeyRequest masterKey
) {
}
