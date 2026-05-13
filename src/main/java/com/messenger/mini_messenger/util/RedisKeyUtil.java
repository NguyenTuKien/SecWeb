package com.messenger.mini_messenger.util;

import java.util.UUID;

public final class RedisKeyUtil {

    private RedisKeyUtil() {
    }

    public static String session(UUID sessionKeyId) {
        return "session:%s".formatted(sessionKeyId);
    }

    public static String conversationKey(UUID sessionKeyId, UUID conversationId, int keyVersion) {
        return "convkey:%s:%s:%d".formatted(sessionKeyId, conversationId, keyVersion);
    }
}
