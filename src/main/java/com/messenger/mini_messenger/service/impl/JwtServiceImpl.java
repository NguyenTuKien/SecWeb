package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final long accessTokenTtlMinutes;

    public JwtServiceImpl(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    @Override
    public String generateAccessToken(UUID userId, String username, UUID sessionKeyId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("sessionKeyId", sessionKeyId.toString())
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public CurrentUser parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"ACCESS".equals(claims.get("type", String.class))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token type");
            }
            return new CurrentUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get("username", String.class),
                    UUID.fromString(claims.get("sessionKeyId", String.class))
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
    }
}
