package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.dto.response.ApiMessageResponse;
import com.messenger.mini_messenger.dto.response.SessionResponse;
import com.messenger.mini_messenger.entity.UserSession;
import com.messenger.mini_messenger.enums.SessionStatus;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.repository.UserSessionRepository;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.SessionService;
import com.messenger.mini_messenger.util.RedisKeyUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository userSessionRepository;
    private final StringRedisTemplate redisTemplate;

    public SessionServiceImpl(UserSessionRepository userSessionRepository, StringRedisTemplate redisTemplate) {
        this.userSessionRepository = userSessionRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<SessionResponse> listMine(CurrentUser currentUser) {
        return userSessionRepository.findByUserId(currentUser.userId())
                .stream()
                .sorted(Comparator.comparing(UserSession::getCreatedAt).reversed())
                .map(session -> toResponse(session, currentUser.sessionKeyId()))
                .toList();
    }

    @Override
    @Transactional
    public ApiMessageResponse revoke(CurrentUser currentUser, UUID sessionKeyId) {
        UserSession session = userSessionRepository.findById(sessionKeyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getUser().getId().equals(currentUser.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot revoke another user's session");
        }
        session.setStatus(SessionStatus.REVOKED);
        redisTemplate.delete(RedisKeyUtil.session(sessionKeyId));
        return new ApiMessageResponse("Session revoked successfully");
    }

    private SessionResponse toResponse(UserSession session, UUID currentSessionKeyId) {
        return new SessionResponse(
                session.getId(),
                session.getSessionPublicKey(),
                session.getDeviceInfo(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getLastActiveAt(),
                session.getExpiresAt(),
                session.getId().equals(currentSessionKeyId)
        );
    }
}
