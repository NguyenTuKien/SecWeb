package com.messenger.mini_messenger.dto.websocket;

public record RealtimeErrorEvent(
        String type,
        String message
) {
    public static RealtimeErrorEvent error(String message) {
        return new RealtimeErrorEvent("error", message);
    }
}
