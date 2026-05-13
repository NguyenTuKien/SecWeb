package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.service.TokenService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenServiceImpl implements TokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash refresh token", exception);
        }
    }

    @Override
    public boolean matchesRefreshToken(String refreshToken, String expectedHash) {
        return MessageDigest.isEqual(hashRefreshToken(refreshToken).getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
