package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.dto.response.ActiveSessionResponse;
import com.messenger.mini_messenger.dto.response.PublicKeyResponse;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.entity.MasterKey;
import com.messenger.mini_messenger.entity.User;
import com.messenger.mini_messenger.enums.SessionStatus;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.mapper.UserMapper;
import com.messenger.mini_messenger.repository.MasterKeyRepository;
import com.messenger.mini_messenger.repository.UserRepository;
import com.messenger.mini_messenger.repository.UserSessionRepository;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            MasterKeyRepository masterKeyRepository,
            UserSessionRepository userSessionRepository,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.masterKeyRepository = masterKeyRepository;
        this.userSessionRepository = userSessionRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse getMe(CurrentUser currentUser) {
        return userMapper.toResponse(findUser(currentUser.userId()));
    }

    @Override
    public List<UserResponse> search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        return userRepository.findTop20ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public PublicKeyResponse getPublicKey(UUID userId) {
        MasterKey masterKey = masterKeyRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Master public key not found"));
        return new PublicKeyResponse(userId, masterKey.getId(), masterKey.getPublicKey());
    }

    @Override
    public List<ActiveSessionResponse> getActiveSessions(UUID userId) {
        return userSessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE)
                .stream()
                .map(session -> new ActiveSessionResponse(
                        session.getId(),
                        session.getSessionPublicKey(),
                        session.getDeviceInfo(),
                        session.getCreatedAt(),
                        session.getLastActiveAt()
                ))
                .toList();
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
