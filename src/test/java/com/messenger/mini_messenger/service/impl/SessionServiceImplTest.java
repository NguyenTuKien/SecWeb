package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.entity.User;
import com.messenger.mini_messenger.entity.UserSession;
import com.messenger.mini_messenger.enums.SessionStatus;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.util.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private com.messenger.mini_messenger.repository.UserSessionRepository userSessionRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void listsCurrentUsersSessionsAndMarksCurrentSession() {
        UUID userId = UUID.randomUUID();
        UUID currentSessionId = UUID.randomUUID();
        UserSession current = session(userId, currentSessionId, SessionStatus.ACTIVE);
        UserSession revoked = session(userId, UUID.randomUUID(), SessionStatus.REVOKED);
        SessionServiceImpl service = new SessionServiceImpl(userSessionRepository, redisTemplate);

        when(userSessionRepository.findByUserId(userId)).thenReturn(List.of(current, revoked));

        var sessions = service.listMine(new CurrentUser(userId, "user", currentSessionId));

        assertEquals(2, sessions.size());
        assertTrue(sessions.getFirst().isCurrent());
        assertEquals(SessionStatus.ACTIVE, sessions.getFirst().status());
        assertEquals(SessionStatus.REVOKED, sessions.get(1).status());
    }

    @Test
    void revokesOwnSessionAndDeletesRedisCache() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UserSession session = session(userId, sessionId, SessionStatus.ACTIVE);
        SessionServiceImpl service = new SessionServiceImpl(userSessionRepository, redisTemplate);

        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        var response = service.revoke(new CurrentUser(userId, "user", UUID.randomUUID()), sessionId);

        assertEquals(SessionStatus.REVOKED, session.getStatus());
        assertEquals("Session revoked successfully", response.message());
        verify(redisTemplate).delete(RedisKeyUtil.session(sessionId));
    }

    @Test
    void rejectsRevokingAnotherUsersSession() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UserSession session = session(ownerId, sessionId, SessionStatus.ACTIVE);
        SessionServiceImpl service = new SessionServiceImpl(userSessionRepository, redisTemplate);

        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.revoke(new CurrentUser(requesterId, "requester", UUID.randomUUID()), sessionId)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    private UserSession session(UUID userId, UUID sessionId, SessionStatus status) {
        User user = new User();
        user.setId(userId);
        UserSession session = new UserSession();
        session.setId(sessionId);
        session.setUser(user);
        session.setSessionPublicKey("c2Vzc2lvbi1wdWJsaWM=");
        session.setDeviceInfo("Chrome");
        session.setRefreshTokenHash("$hash");
        session.setStatus(status);
        session.setCreatedAt(Instant.parse("2026-05-14T01:00:00Z"));
        session.setLastActiveAt(Instant.parse("2026-05-14T02:00:00Z"));
        session.setExpiresAt(Instant.parse("2026-06-14T01:00:00Z"));
        return session;
    }
}
