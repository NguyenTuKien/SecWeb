package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.service.CryptoService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class CryptoServiceImpl implements CryptoService {

    @Override
    public String encryptWithRsaOaepSha256(String plaintext, String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot encrypt data with public key", exception);
        }
    }
}
