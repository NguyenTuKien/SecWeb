package com.messenger.mini_messenger.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.mini_messenger.dto.redis.RedisSessionValue;
import com.messenger.mini_messenger.dto.request.GoogleLoginRequest;
import com.messenger.mini_messenger.dto.request.LoginRequest;
import com.messenger.mini_messenger.dto.request.LogoutRequest;
import com.messenger.mini_messenger.dto.request.RefreshTokenRequest;
import com.messenger.mini_messenger.dto.request.SignupRequest;
import com.messenger.mini_messenger.dto.response.ApiMessageResponse;
import com.messenger.mini_messenger.dto.response.AuthResponse;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.entity.AuthIdentity;
import com.messenger.mini_messenger.entity.MasterKey;
import com.messenger.mini_messenger.entity.User;
import com.messenger.mini_messenger.entity.UserSession;
import com.messenger.mini_messenger.enums.AuthProvider;
import com.messenger.mini_messenger.enums.SessionStatus;
import com.messenger.mini_messenger.enums.UserRole;
import com.messenger.mini_messenger.enums.UserStatus;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.mapper.MasterKeyMapper;
import com.messenger.mini_messenger.mapper.UserMapper;
import com.messenger.mini_messenger.repository.AuthIdentityRepository;
import com.messenger.mini_messenger.repository.UserRepository;
import com.messenger.mini_messenger.repository.UserSessionRepository;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.AuthService;
import com.messenger.mini_messenger.service.CryptoService;
import com.messenger.mini_messenger.service.GoogleTokenService;
import com.messenger.mini_messenger.service.JwtService;
import com.messenger.mini_messenger.service.TokenService;
import com.messenger.mini_messenger.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final MasterKeyMapper masterKeyMapper;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final CryptoService cryptoService;
    private final GoogleTokenService googleTokenService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long refreshTokenTtlDays;

    public AuthServiceImpl(
            UserRepository userRepository,
            AuthIdentityRepository authIdentityRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            MasterKeyMapper masterKeyMapper,
            UserMapper userMapper,
            JwtService jwtService,
            TokenService tokenService,
            CryptoService cryptoService,
            GoogleTokenService googleTokenService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays
    ) {
        this.userRepository = userRepository;
        this.authIdentityRepository = authIdentityRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.masterKeyMapper = masterKeyMapper;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.cryptoService = cryptoService;
        this.googleTokenService = googleTokenService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Override
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password confirmation does not match");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setDisplayName(request.displayName() == null ? request.username() : request.displayName());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        AuthIdentity identity = new AuthIdentity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.LOCAL);
        identity.setProviderSubject(request.username());
        identity.setProviderEmail(request.email());
        identity.setPasswordHash(passwordEncoder.encode(request.password()));
        identity.setEmailVerified(false);
        user.getIdentities().add(identity);

        MasterKey masterKey = masterKeyMapper.toEntity(request.masterKey());
        masterKey.setUser(user);
        user.setMasterKey(masterKey);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.usernameOrEmail())
                .or(() -> userRepository.findByEmail(request.usernameOrEmail()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        AuthIdentity identity = authIdentityRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Local login is not enabled for this user"));
        if (!passwordEncoder.matches(request.password(), identity.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        identity.setLastLoginAt(Instant.now());
        return createAuthResponse(user, request.sessionPublicKey(), request.deviceInfo());
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenService.GoogleUserInfo googleUser = googleTokenService.verify(request.idToken());
        AuthIdentity identity = authIdentityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, googleUser.subject())
                .orElseGet(() -> createGoogleIdentity(request, googleUser));
        identity.setLastLoginAt(Instant.now());
        return createAuthResponse(identity.getUser(), request.sessionPublicKey(), request.deviceInfo());
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        UserSession session = userSessionRepository.findById(request.sessionKeyId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid session"));
        if (session.getStatus() != SessionStatus.ACTIVE || session.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session is not active");
        }
        if (!tokenService.matchesRefreshToken(request.refreshToken(), session.getRefreshTokenHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        return rotateSessionTokens(session);
    }

    @Override
    @Transactional
    public ApiMessageResponse logout(CurrentUser currentUser, LogoutRequest request) {
        UUID sessionKeyId = request.sessionKeyId() == null ? currentUser.sessionKeyId() : request.sessionKeyId();
        UserSession session = userSessionRepository.findById(sessionKeyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getUser().getId().equals(currentUser.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot revoke another user's session");
        }
        session.setStatus(SessionStatus.REVOKED);
        redisTemplate.delete(RedisKeyUtil.session(sessionKeyId));
        return new ApiMessageResponse("Logged out successfully");
    }

    private AuthIdentity createGoogleIdentity(GoogleLoginRequest request, GoogleTokenService.GoogleUserInfo googleUser) {
        if (request.masterKey() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Master key is required for first Google login");
        }
        User user = userRepository.findByEmail(googleUser.email()).orElseGet(() -> {
            User created = new User();
            created.setUsername(uniqueGoogleUsername(googleUser.email()));
            created.setEmail(googleUser.email());
            created.setDisplayName(googleUser.name());
            created.setAvatarUrl(googleUser.picture());
            created.setRole(UserRole.USER);
            created.setStatus(UserStatus.ACTIVE);
            MasterKey masterKey = masterKeyMapper.toEntity(request.masterKey());
            masterKey.setUser(created);
            created.setMasterKey(masterKey);
            return created;
        });
        AuthIdentity identity = new AuthIdentity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.GOOGLE);
        identity.setProviderSubject(googleUser.subject());
        identity.setProviderEmail(googleUser.email());
        identity.setEmailVerified(googleUser.emailVerified());
        user.getIdentities().add(identity);
        return authIdentityRepository.save(identity);
    }

    private String uniqueGoogleUsername(String email) {
        String base = email == null ? "google_user" : email.substring(0, email.indexOf('@')).replaceAll("[^A-Za-z0-9_]", "_");
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private AuthResponse createAuthResponse(User user, String sessionPublicKey, String deviceInfo) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionPublicKey(sessionPublicKey);
        session.setDeviceInfo(deviceInfo);
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS));
        session.setLastActiveAt(Instant.now());

        String refreshToken = tokenService.generateRefreshToken();
        session.setRefreshTokenHash(tokenService.hashRefreshToken(refreshToken));
        userSessionRepository.save(session);
        storeRedisSession(session);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), session.getId());
        String encryptedRefreshToken = cryptoService.encryptWithRsaOaepSha256(refreshToken, sessionPublicKey);
        return new AuthResponse(
                accessToken,
                encryptedRefreshToken,
                session.getId(),
                userMapper.toResponse(user),
                masterKeyMapper.toResponse(user.getMasterKey())
        );
    }

    private AuthResponse rotateSessionTokens(UserSession session) {
        String refreshToken = tokenService.generateRefreshToken();
        session.setRefreshTokenHash(tokenService.hashRefreshToken(refreshToken));
        session.setLastActiveAt(Instant.now());
        storeRedisSession(session);
        return new AuthResponse(
                jwtService.generateAccessToken(session.getUser().getId(), session.getUser().getUsername(), session.getId()),
                cryptoService.encryptWithRsaOaepSha256(refreshToken, session.getSessionPublicKey()),
                session.getId(),
                userMapper.toResponse(session.getUser()),
                masterKeyMapper.toResponse(session.getUser().getMasterKey())
        );
    }

    private void storeRedisSession(UserSession session) {
        try {
            RedisSessionValue value = new RedisSessionValue(
                    session.getId(),
                    session.getUser().getId(),
                    session.getSessionPublicKey(),
                    session.getUser().getMasterKey().getPublicKey(),
                    session.getRefreshTokenHash(),
                    session.getStatus(),
                    session.getExpiresAt()
            );
            redisTemplate.opsForValue().set(
                    RedisKeyUtil.session(session.getId()),
                    objectMapper.writeValueAsString(value),
                    Duration.between(Instant.now(), session.getExpiresAt())
            );
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot store session in Redis");
        }
    }
}
