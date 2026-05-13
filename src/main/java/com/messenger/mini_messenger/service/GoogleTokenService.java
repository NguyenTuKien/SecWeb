package com.messenger.mini_messenger.service;

public interface GoogleTokenService {
    GoogleUserInfo verify(String idToken);

    record GoogleUserInfo(String subject, String email, boolean emailVerified, String name, String picture) {
    }
}
