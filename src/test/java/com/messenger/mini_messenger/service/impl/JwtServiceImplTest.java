package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.security.CurrentUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private static final String SECRET = "my-very-long-super-secret-key-that-is-at-least-256-bits-long";
    private static final long TTL_MINUTES = 15;

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(SECRET, TTL_MINUTES);
    }

    @Test
    void generatesAndParsesValidAccessTokenSuccessfully() {
        UUID userId = UUID.randomUUID();
        String username = "testuser";
        UUID sessionKeyId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, username, sessionKeyId);
        assertNotNull(token);

        CurrentUser user = jwtService.parseAccessToken(token);
        assertNotNull(user);
        assertEquals(userId, user.userId());
        assertEquals(username, user.username());
        assertEquals(sessionKeyId, user.sessionKeyId());
    }

    @Test
    void throwsApiExceptionWhenParsingTokenWithInvalidType() {
        // Generate a token with a different type than "ACCESS"
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("username", "testuser")
                .claim("sessionKeyId", UUID.randomUUID().toString())
                .claim("type", "REFRESH") // Invalid type
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ApiException exception = assertThrows(
                ApiException.class,
                () -> jwtService.parseAccessToken(token)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid token type", exception.getMessage());
    }

    @Test
    void throwsApiExceptionWhenParsingMalformedOrExpiredToken() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> jwtService.parseAccessToken("malformed.token.value")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid access token", exception.getMessage());
    }
}
