package com.messenger.mini_messenger.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.mini_messenger.dto.request.LoginRequest;
import com.messenger.mini_messenger.dto.request.MasterKeyRequest;
import com.messenger.mini_messenger.dto.request.SignupRequest;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.entity.AuthIdentity;
import com.messenger.mini_messenger.entity.MasterKey;
import com.messenger.mini_messenger.entity.User;
import com.messenger.mini_messenger.enums.AuthProvider;
import com.messenger.mini_messenger.enums.UserRole;
import com.messenger.mini_messenger.enums.UserStatus;
import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.mapper.MasterKeyMapper;
import com.messenger.mini_messenger.mapper.UserMapper;
import com.messenger.mini_messenger.repository.AuthIdentityRepository;
import com.messenger.mini_messenger.repository.UserRepository;
import com.messenger.mini_messenger.repository.UserSessionRepository;
import com.messenger.mini_messenger.service.CryptoService;
import com.messenger.mini_messenger.service.GoogleTokenService;
import com.messenger.mini_messenger.service.JwtService;
import com.messenger.mini_messenger.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthIdentityRepository authIdentityRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MasterKeyMapper masterKeyMapper;
    @Mock private UserMapper userMapper;
    @Mock private JwtService jwtService;
    @Mock private TokenService tokenService;
    @Mock private CryptoService cryptoService;
    @Mock private GoogleTokenService googleTokenService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ObjectMapper objectMapper;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                authIdentityRepository,
                userSessionRepository,
                passwordEncoder,
                masterKeyMapper,
                userMapper,
                jwtService,
                tokenService,
                cryptoService,
                googleTokenService,
                redisTemplate,
                objectMapper,
                30 // refreshTokenTtlDays
        );
    }

    @Test
    void signsUpSuccessfullyWhenInputIsValid() {
        MasterKeyRequest masterKeyRequest = new MasterKeyRequest("pub", "priv", "iv", "salt", Map.of());
        SignupRequest signupRequest = new SignupRequest("username", "email@test.com", "display", "password123", "password123", masterKeyRequest);

        when(userRepository.existsByUsername("username")).thenReturn(false);
        when(userRepository.existsByEmail("email@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        MasterKey masterKey = new MasterKey();
        when(masterKeyMapper.toEntity(masterKeyRequest)).thenReturn(masterKey);

        User savedUser = new User();
        savedUser.setUsername("username");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse userResponse = new UserResponse(UUID.randomUUID(), "username", "email@test.com", "display", null, UserRole.USER, UserStatus.ACTIVE, null);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        UserResponse response = authService.signup(signupRequest);

        assertNotNull(response);
        assertEquals("username", response.username());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void throwsApiExceptionOnSignupWhenPasswordsDoNotMatch() {
        MasterKeyRequest masterKeyRequest = new MasterKeyRequest("pub", "priv", "iv", "salt", Map.of());
        SignupRequest signupRequest = new SignupRequest("username", "email@test.com", "display", "password123", "different", masterKeyRequest);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.signup(signupRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Password confirmation does not match", exception.getMessage());
    }

    @Test
    void throwsApiExceptionOnSignupWhenUsernameExists() {
        MasterKeyRequest masterKeyRequest = new MasterKeyRequest("pub", "priv", "iv", "salt", Map.of());
        SignupRequest signupRequest = new SignupRequest("username", "email@test.com", "display", "password123", "password123", masterKeyRequest);

        when(userRepository.existsByUsername("username")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.signup(signupRequest)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void throwsApiExceptionOnLoginWhenUserNotFound() {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "password", "sessionPub", "device");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void throwsApiExceptionOnLoginWhenPasswordIncorrect() {
        LoginRequest loginRequest = new LoginRequest("username", "wrongpass", "sessionPub", "device");
        
        User user = new User();
        user.setId(UUID.randomUUID());
        
        when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));
        
        AuthIdentity identity = new AuthIdentity();
        identity.setPasswordHash("hashedPassword");
        
        when(authIdentityRepository.findByUserIdAndProvider(user.getId(), AuthProvider.LOCAL))
                .thenReturn(Optional.of(identity));
        when(passwordEncoder.matches("wrongpass", "hashedPassword")).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid credentials", exception.getMessage());
    }
}
