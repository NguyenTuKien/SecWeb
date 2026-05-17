package com.messenger.mini_messenger.service.impl;

import org.junit.jupiter.api.Test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoServiceImplTest {

    private final CryptoServiceImpl cryptoService = new CryptoServiceImpl();

    @Test
    void encryptsPlaintextWithRsaOaepSha256Successfully() throws Exception {
        // Generate valid RSA KeyPair
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        String plaintext = "Hello Security Team!";
        String encrypted = cryptoService.encryptWithRsaOaepSha256(plaintext, publicKeyBase64);

        assertNotNull(encrypted);
    }

    @Test
    void throwsIllegalArgumentExceptionWhenPublicKeyIsInvalid() {
        String invalidPublicKey = "invalid-key-base64";
        assertThrows(
                IllegalArgumentException.class,
                () -> cryptoService.encryptWithRsaOaepSha256("plaintext", invalidPublicKey)
        );
    }
}
