package com.messenger.mini_messenger.service;

public interface TokenService {
    String generateRefreshToken();

    String hashRefreshToken(String refreshToken);

    boolean matchesRefreshToken(String refreshToken, String expectedHash);
}
