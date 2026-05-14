package com.messenger.mini_messenger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 255) String email,
        @Size(max = 100) String displayName,
        @Size(max = 2048) String avatarUrl,
        @Size(min = 8, max = 128) String currentPassword,
        @Size(min = 8, max = 128) String newPassword,
        @Valid MasterKeyRequest masterKey
) {
}
