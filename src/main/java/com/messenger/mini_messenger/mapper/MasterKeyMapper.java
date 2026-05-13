package com.messenger.mini_messenger.mapper;

import com.messenger.mini_messenger.dto.request.MasterKeyRequest;
import com.messenger.mini_messenger.dto.response.MasterKeyResponse;
import com.messenger.mini_messenger.entity.MasterKey;
import com.messenger.mini_messenger.util.JsonUtil;
import org.springframework.stereotype.Component;

@Component
public class MasterKeyMapper {

    private final JsonUtil jsonUtil;

    public MasterKeyMapper(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    public MasterKey toEntity(MasterKeyRequest request) {
        MasterKey masterKey = new MasterKey();
        masterKey.setPublicKey(request.publicKey());
        masterKey.setEncryptedPrivateKey(request.encryptedPrivateKey());
        masterKey.setPrivateKeyIv(request.privateKeyIv());
        masterKey.setPinSalt(request.pinSalt());
        masterKey.setKdfParams(jsonUtil.toJson(request.kdfParams()));
        return masterKey;
    }

    public MasterKeyResponse toResponse(MasterKey masterKey) {
        return new MasterKeyResponse(
                masterKey.getId(),
                masterKey.getPublicKey(),
                masterKey.getEncryptedPrivateKey(),
                masterKey.getPrivateKeyIv(),
                masterKey.getPinSalt(),
                jsonUtil.toMap(masterKey.getKdfParams()),
                masterKey.getStatus(),
                masterKey.getCreatedAt()
        );
    }
}
