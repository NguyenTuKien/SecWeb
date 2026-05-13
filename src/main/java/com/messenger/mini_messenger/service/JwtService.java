package com.messenger.mini_messenger.service;

import com.messenger.mini_messenger.security.CurrentUser;

import java.util.UUID;

public interface JwtService {
    String generateAccessToken(UUID userId, String username, UUID sessionKeyId);

    CurrentUser parseAccessToken(String token);
}
