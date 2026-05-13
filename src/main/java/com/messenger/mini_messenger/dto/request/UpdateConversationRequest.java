package com.messenger.mini_messenger.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(
        @Size(max = 255) String name
) {
}
