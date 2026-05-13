package com.messenger.mini_messenger.service;

public interface CryptoService {
    String encryptWithRsaOaepSha256(String plaintext, String publicKeyBase64);
}
