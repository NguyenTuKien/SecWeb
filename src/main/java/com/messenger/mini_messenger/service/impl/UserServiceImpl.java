package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.dto.request.MasterKeyRequest;
import com.messenger.mini_messenger.dto.request.UpdateUserRequest;
import com.messenger.mini_messenger.dto.response.ActiveSessionResponse;
import com.messenger.mini_messenger.dto.response.PublicKeyResponse;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.entity.AuthIdentity;
import com.messenger.mini_messenger.entity.MasterKey;
import com.messenger.mini_messenger.entity.User;
import com.messenger.mini_messenger.enums.AuthProvider;
import com.messenger.mini_messenger.enums.SessionStatus;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.mapper.MasterKeyMapper;
import com.messenger.mini_messenger.mapper.UserMapper;
import com.messenger.mini_messenger.repository.AuthIdentityRepository;
import com.messenger.mini_messenger.repository.MasterKeyRepository;
import com.messenger.mini_messenger.repository.UserRepository;
import com.messenger.mini_messenger.repository.UserSessionRepository;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final MasterKeyMapper masterKeyMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            MasterKeyRepository masterKeyRepository,
            UserSessionRepository userSessionRepository,
            AuthIdentityRepository authIdentityRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            MasterKeyMapper masterKeyMapper
    ) {
        this.userRepository = userRepository;
        this.masterKeyRepository = masterKeyRepository;
        this.userSessionRepository = userSessionRepository;
        this.authIdentityRepository = authIdentityRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.masterKeyMapper = masterKeyMapper;
    }

    @Override
    public UserResponse getMe(CurrentUser currentUser) {
        return userMapper.toResponse(findUser(currentUser.userId()));
    }

    @Override
    @Transactional
    public UserResponse updateMe(CurrentUser currentUser, UpdateUserRequest request) {
        User user = findUser(currentUser.userId());
        updateProfile(user, request);
        updatePasswordIfRequested(user, request);
        updateMasterKeyIfRequested(user, request.masterKey());
        return userMapper.toResponse(user);
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

    private void updateProfile(User user, UpdateUserRequest request) {
        if (request.email() != null) {
            String email = normalize(request.email());
            if (email == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Email cannot be blank");
            }
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
            }
            user.setEmail(email);
            authIdentityRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL)
                    .ifPresent(identity -> identity.setProviderEmail(email));
        }
        if (request.displayName() != null) {
            user.setDisplayName(normalize(request.displayName()));
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(normalize(request.avatarUrl()));
        }
    }

    private void updatePasswordIfRequested(User user, UpdateUserRequest request) {
        boolean wantsPasswordChange = request.currentPassword() != null || request.newPassword() != null;
        if (!wantsPasswordChange) {
            return;
        }
        if (request.currentPassword() == null || request.newPassword() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password and new password are required");
        }
        AuthIdentity identity = authIdentityRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Local login is not enabled for this user"));
        if (!passwordEncoder.matches(request.currentPassword(), identity.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        identity.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private void updateMasterKeyIfRequested(User user, MasterKeyRequest request) {
        if (request == null) {
            return;
        }
        MasterKey updated = masterKeyMapper.toEntity(request);
        MasterKey masterKey = user.getMasterKey();
        masterKey.setPublicKey(updated.getPublicKey());
        masterKey.setEncryptedPrivateKey(updated.getEncryptedPrivateKey());
        masterKey.setPrivateKeyIv(updated.getPrivateKeyIv());
        masterKey.setPinSalt(updated.getPinSalt());
        masterKey.setKdfParams(updated.getKdfParams());
        masterKey.setRotatedAt(Instant.now());
    }

    private String normalize(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
