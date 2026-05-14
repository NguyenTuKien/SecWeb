package com.messenger.mini_messenger.service;

import com.messenger.mini_messenger.dto.request.SendMessageRequest;
import com.messenger.mini_messenger.dto.request.UpdateMessageRequest;
import com.messenger.mini_messenger.dto.response.ApiMessageResponse;
import com.messenger.mini_messenger.dto.response.MessageCreatedResponse;
import com.messenger.mini_messenger.dto.response.MessageResponse;
import com.messenger.mini_messenger.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    List<MessageResponse> getMessages(CurrentUser currentUser, UUID conversationId, int limit);

    MessageCreatedResponse sendMessage(CurrentUser currentUser, UUID conversationId, SendMessageRequest request);

    MessageResponse updateMessage(CurrentUser currentUser, UUID conversationId, UUID messageId, UpdateMessageRequest request);

    ApiMessageResponse deleteMessage(CurrentUser currentUser, UUID conversationId, UUID messageId);
}
