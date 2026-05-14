package com.messenger.mini_messenger.service.impl;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.messenger.mini_messenger.dto.request.UpdateMessageRequest;
import com.messenger.mini_messenger.entity.Conversation;
import com.messenger.mini_messenger.entity.ConversationKeyVersion;
import com.messenger.mini_messenger.entity.ConversationMember;
import com.messenger.mini_messenger.entity.Message;
import com.messenger.mini_messenger.entity.User;
import com.messenger.mini_messenger.enums.ConversationMemberStatus;
import com.messenger.mini_messenger.enums.MessageType;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.mapper.MessageMapper;
import com.messenger.mini_messenger.repository.ConversationKeyVersionRepository;
import com.messenger.mini_messenger.repository.ConversationMemberRepository;
import com.messenger.mini_messenger.repository.ConversationRepository;
import com.messenger.mini_messenger.repository.MessageRepository;
import com.messenger.mini_messenger.repository.UserRepository;
import com.messenger.mini_messenger.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository memberRepository;

    @Mock
    private ConversationKeyVersionRepository keyVersionRepository;

    @Mock
    private UserRepository userRepository;

    private MessageServiceImpl messageService;

    MessageServiceImplTest() {
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        MessageMapper messageMapper = new MessageMapper(new com.messenger.mini_messenger.util.JsonUtil(JsonMapper.builder().findAndAddModules().build()));
        messageService = new MessageServiceImpl(
                messageRepository,
                conversationRepository,
                memberRepository,
                keyVersionRepository,
                userRepository,
                messageMapper
        );
    }

    @Test
    void senderCanEditEncryptedMessagePayload() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(senderId, "sender", UUID.randomUUID());
        Message message = message(conversationId, messageId, senderId);
        ConversationKeyVersion keyVersion = keyVersion(conversationId, 2);
        var request = new UpdateMessageRequest("bmV3LWNpcGhlcg==", "bmV3LWl2", "bmV3LWFhZA==", 2, "IMAGE");

        allowParticipant(currentUser, conversationId);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(keyVersionRepository.findByConversationIdAndKeyVersion(conversationId, 2)).thenReturn(Optional.of(keyVersion));

        messageService.updateMessage(currentUser, conversationId, messageId, request);

        assertEquals("bmV3LWNpcGhlcg==", message.getCipherData());
        assertEquals("bmV3LWl2", message.getIv());
        assertEquals("bmV3LWFhZA==", message.getAad());
        assertEquals(MessageType.IMAGE, message.getMessageType());
        assertSame(keyVersion, message.getKeyVersion());
        assertNotNull(message.getEditedAt());
    }

    @Test
    void nonSenderCannotEditMessage() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(currentUserId, "not-sender", UUID.randomUUID());
        Message message = message(conversationId, messageId, senderId);
        var request = new UpdateMessageRequest("bmV3LWNpcGhlcg==", "bmV3LWl2", null, 1, "TEXT");

        allowParticipant(currentUser, conversationId);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> messageService.updateMessage(currentUser, conversationId, messageId, request)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(keyVersionRepository, never()).findByConversationIdAndKeyVersion(any(), any(Integer.class));
    }

    @Test
    void senderCanSoftDeleteOwnMessage() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(senderId, "sender", UUID.randomUUID());
        Message message = message(conversationId, messageId, senderId);

        allowParticipant(currentUser, conversationId);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        var response = messageService.deleteMessage(currentUser, conversationId, messageId);

        assertNotNull(message.getDeletedAt());
        assertEquals("Message deleted successfully", response.message());
    }

    @Test
    void deletedMessageCannotBeEditedAgain() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(senderId, "sender", UUID.randomUUID());
        Message message = message(conversationId, messageId, senderId);
        message.setDeletedAt(Instant.now());
        var request = new UpdateMessageRequest("bmV3LWNpcGhlcg==", "bmV3LWl2", null, 1, "TEXT");

        allowParticipant(currentUser, conversationId);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> messageService.updateMessage(currentUser, conversationId, messageId, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("deleted"));
    }

    @Test
    void rejectsMessageListLimitOutsideAllowedRange() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, "sender", UUID.randomUUID());

        allowParticipant(currentUser, conversationId);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> messageService.getMessages(currentUser, conversationId, 101)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("between 1 and 100"));
    }

    private void allowParticipant(CurrentUser currentUser, UUID conversationId) {
        when(memberRepository.findByConversationIdAndUserIdAndStatus(
                conversationId,
                currentUser.userId(),
                ConversationMemberStatus.ACTIVE
        )).thenReturn(Optional.of(new ConversationMember()));
    }

    private Message message(UUID conversationId, UUID messageId, UUID senderId) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        User sender = new User();
        sender.setId(senderId);
        ConversationKeyVersion keyVersion = keyVersion(conversationId, 1);
        Message message = new Message();
        message.setId(messageId);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setKeyVersion(keyVersion);
        message.setCipherData("b2xkLWNpcGhlcg==");
        message.setIv("b2xkLWl2");
        message.setAad("b2xkLWFhZA==");
        message.setMessageType(MessageType.TEXT);
        message.setClientCreatedAt(Instant.parse("2026-05-13T09:00:00Z"));
        return message;
    }

    private ConversationKeyVersion keyVersion(UUID conversationId, int version) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        ConversationKeyVersion keyVersion = new ConversationKeyVersion();
        keyVersion.setConversation(conversation);
        keyVersion.setKeyVersion(version);
        return keyVersion;
    }
}
