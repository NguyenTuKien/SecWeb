package com.messenger.mini_messenger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.mini_messenger.dto.request.LoginRequest;
import com.messenger.mini_messenger.dto.request.MasterKeyRequest;
import com.messenger.mini_messenger.dto.request.SignupRequest;
import com.messenger.mini_messenger.dto.response.AuthResponse;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.enums.UserRole;
import com.messenger.mini_messenger.enums.UserStatus;
import com.messenger.mini_messenger.security.JwtAuthenticationFilter;
import com.messenger.mini_messenger.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signupsSuccessfullyAndReturnsCreatedStatus() throws Exception {
        MasterKeyRequest masterKeyRequest = new MasterKeyRequest("publicKeyBase64EncodedString=", "encryptedPrivateKey=", "privateKeyIv=", "pinSalt=", Map.of());
        SignupRequest signupRequest = new SignupRequest("username", "email@test.com", "display", "password128", "password128", masterKeyRequest);

        UserResponse userResponse = new UserResponse(UUID.randomUUID(), "email@test.com", "username", "display", null, UserRole.USER, UserStatus.ACTIVE, null, null);
        when(authService.signup(any(SignupRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("username"))
                .andExpect(jsonPath("$.email").value("email@test.com"));
    }

    @Test
    void loginsSuccessfullyAndReturnsAuthResponse() throws Exception {
        LoginRequest loginRequest = new LoginRequest("username", "password128", "sessionPublicKey=", "deviceInfo");
        AuthResponse authResponse = new AuthResponse("accessToken", "encryptedRefreshToken", UUID.randomUUID(), null, null);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.encryptedRefreshToken").value("encryptedRefreshToken"));
    }
}
