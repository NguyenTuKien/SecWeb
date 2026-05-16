package com.messenger.mini_messenger.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.mini_messenger.dto.response.MessageResponse;
import com.messenger.mini_messenger.dto.websocket.RealtimeErrorEvent;
import com.messenger.mini_messenger.dto.websocket.RealtimeMessageEvent;
import com.messenger.mini_messenger.dto.websocket.RealtimeMessageRequest;
import com.messenger.mini_messenger.enums.ConversationMemberStatus;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.repository.ConversationMemberRepository;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.MessageService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeMessageWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final MessageService messageService;
    private final ConversationMemberRepository memberRepository;
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    public RealtimeMessageWebSocketHandler(
            ObjectMapper objectMapper,
            Validator validator,
            MessageService messageService,
            ConversationMemberRepository memberRepository
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.messageService = messageService;
        this.memberRepository = memberRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        CurrentUser currentUser = currentUser(session);
        sessionsByUserId.computeIfAbsent(currentUser.userId(), ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        try {
            RealtimeMessageRequest request = objectMapper.readValue(textMessage.getPayload(), RealtimeMessageRequest.class);
            validate(request);
            MessageResponse savedMessage = messageService.sendRealtimeMessage(currentUser(session), request);
            broadcastToParticipants(savedMessage);
        } catch (ApiException exception) {
            sendError(session, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            sendError(session, exception.getMessage());
        } catch (Exception exception) {
            sendError(session, "Invalid realtime message payload");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Object attribute = session.getAttributes().get(JwtHandshakeInterceptor.CURRENT_USER_ATTRIBUTE);
        if (attribute instanceof CurrentUser currentUser) {
            Set<WebSocketSession> sessions = sessionsByUserId.get(currentUser.userId());
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionsByUserId.remove(currentUser.userId());
                }
            }
        }
    }

    private void validate(RealtimeMessageRequest request) {
        Set<ConstraintViolation<RealtimeMessageRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getPropertyPath() + " " + violations.iterator().next().getMessage());
        }
    }

    private void broadcastToParticipants(MessageResponse message) throws IOException {
        String payload = objectMapper.writeValueAsString(RealtimeMessageEvent.created(message));
        memberRepository.findUserIdsByConversationIdAndStatus(message.conversationId(), ConversationMemberStatus.ACTIVE)
                .forEach(userId -> sendToUser(userId, payload));
    }

    private void sendToUser(UUID userId, String payload) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.removeIf(session -> !session.isOpen());
        sessions.forEach(session -> send(session, payload));
    }

    private void send(WebSocketSession session, String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException exception) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ignored) {
            }
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(RealtimeErrorEvent.error(message))));
        }
    }

    private CurrentUser currentUser(WebSocketSession session) {
        return (CurrentUser) session.getAttributes().get(JwtHandshakeInterceptor.CURRENT_USER_ATTRIBUTE);
    }
}
