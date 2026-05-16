package com.messenger.mini_messenger.dto.websocket;

import com.messenger.mini_messenger.dto.response.MessageResponse;

public record RealtimeMessageEvent(
        String type,
        MessageResponse message
) {
    public static RealtimeMessageEvent created(MessageResponse message) {
        return new RealtimeMessageEvent("message.created", message);
    }
}
