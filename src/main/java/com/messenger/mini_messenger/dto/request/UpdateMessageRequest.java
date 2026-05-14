package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.validation.Base64Value;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMessageRequest(
        @NotBlank @Base64Value String cipherData,
        @NotBlank @Base64Value String iv,
        @Size(max = 512) @Base64Value String aad,
        @Min(1) int keyVersion,
        @NotBlank String messageType
) {
}
