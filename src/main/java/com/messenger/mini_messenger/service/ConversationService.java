package com.messenger.mini_messenger.service;

import com.messenger.mini_messenger.dto.request.CreateConversationRequest;
import com.messenger.mini_messenger.dto.request.StoreConversationKeysRequest;
import com.messenger.mini_messenger.dto.request.UpdateConversationRequest;
import com.messenger.mini_messenger.dto.response.ApiMessageResponse;
import com.messenger.mini_messenger.dto.response.ConversationResponse;
import com.messenger.mini_messenger.dto.response.EncryptedConversationKeyResponse;
import com.messenger.mini_messenger.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface ConversationService {
    ConversationResponse create(CurrentUser currentUser, CreateConversationRequest request);

    List<ConversationResponse> list(CurrentUser currentUser);

    ConversationResponse get(CurrentUser currentUser, UUID conversationId);

    ConversationResponse update(CurrentUser currentUser, UUID conversationId, UpdateConversationRequest request);

    ApiMessageResponse leave(CurrentUser currentUser, UUID conversationId);

    ApiMessageResponse storeKeys(CurrentUser currentUser, UUID conversationId, StoreConversationKeysRequest request);

    EncryptedConversationKeyResponse getMyEncryptedKey(CurrentUser currentUser, UUID conversationId, Integer keyVersion);

    ApiMessageResponse rotateKeys(CurrentUser currentUser, UUID conversationId, StoreConversationKeysRequest request);
}
