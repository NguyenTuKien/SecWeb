package com.messenger.mini_messenger.dto.request;

import com.messenger.mini_messenger.validation.Base64Value;
import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(
        @Size(max = 2048) @Base64Value String name
) {
}
