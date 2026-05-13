package com.messenger.mini_messenger.service.impl;

import com.messenger.mini_messenger.exception.ApiException;
import com.messenger.mini_messenger.service.GoogleTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GoogleTokenServiceImpl implements GoogleTokenService {

    private final JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
    private final Set<String> allowedAudiences;

    public GoogleTokenServiceImpl(@Value("${app.google.allowed-audiences:}") String allowedAudiences) {
        this.allowedAudiences = Arrays.stream(allowedAudiences.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        if (allowedAudiences.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Google allowed audiences are not configured");
        }
        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            if (!allowedAudiences.contains(jwt.getAudience().getFirst())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Google token audience is not allowed");
            }
            String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
            if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Google token issuer is invalid");
            }
            return new GoogleUserInfo(
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaim("email_verified")),
                    jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("picture")
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google id token");
        }
    }
}
