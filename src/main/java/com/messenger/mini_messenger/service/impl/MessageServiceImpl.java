package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.dto.request.SendMessageRequest;
import com.messenger.mini_messenger.dto.response.MessageCreatedResponse;
import com.messenger.mini_messenger.dto.response.MessageResponse;
import com.messenger.mini_messenger.entity.Conversation;
import com.messenger.mini_messenger.entity.ConversationKeyVersion;
import com.messenger.mini_messenger.entity.Message;
import com.messenger.mini_messenger.entity.User;
import com.messenger.mini_messenger.enums.ConversationMemberStatus;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.mapper.MessageMapper;
import com.messenger.mini_messenger.repository.ConversationKeyVersionRepository;
import com.messenger.mini_messenger.repository.ConversationMemberRepository;
import com.messenger.mini_messenger.repository.ConversationRepository;
import com.messenger.mini_messenger.repository.MessageRepository;
import com.messenger.mini_messenger.repository.UserRepository;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final ConversationKeyVersionRepository keyVersionRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    public MessageServiceImpl(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            ConversationKeyVersionRepository keyVersionRepository,
            UserRepository userRepository,
            MessageMapper messageMapper
    ) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.keyVersionRepository = keyVersionRepository;
        this.userRepository = userRepository;
        this.messageMapper = messageMapper;
    }

    @Override
    public List<MessageResponse> getMessages(CurrentUser currentUser, UUID conversationId, int limit) {
        requireParticipant(currentUser, conversationId);
        List<Message> messages = messageRepository.findByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(conversationId);
        return messages.stream()
                .limit(Math.min(Math.max(limit, 1), 100))
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .map(messageMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MessageCreatedResponse sendMessage(CurrentUser currentUser, UUID conversationId, SendMessageRequest request) {
        requireParticipant(currentUser, conversationId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
        User sender = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        ConversationKeyVersion keyVersion = keyVersionRepository.findByConversationIdAndKeyVersion(conversationId, request.keyVersion())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Conversation key version not found"));

        Message message = messageMapper.toEntity(request);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setKeyVersion(keyVersion);
        messageRepository.save(message);

        conversation.setLastMessageAt(Instant.now());
        return new MessageCreatedResponse(message.getId(), conversationId, sender.getId(), message.getCreatedAt());
    }

    private void requireParticipant(CurrentUser currentUser, UUID conversationId) {
        memberRepository.findByConversationIdAndUserIdAndStatus(
                        conversationId,
                        currentUser.userId(),
                        ConversationMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this conversation"));
    }
}
