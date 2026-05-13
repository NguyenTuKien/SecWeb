package com.messenger.mini_messenger.service;

import com.messenger.mini_messenger.dto.request.GoogleLoginRequest;
import com.messenger.mini_messenger.dto.request.LoginRequest;
import com.messenger.mini_messenger.dto.request.LogoutRequest;
import com.messenger.mini_messenger.dto.request.RefreshTokenRequest;
import com.messenger.mini_messenger.dto.request.SignupRequest;
import com.messenger.mini_messenger.dto.response.ApiMessageResponse;
import com.messenger.mini_messenger.dto.response.AuthResponse;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.security.CurrentUser;

public interface AuthService {
    UserResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    ApiMessageResponse logout(CurrentUser currentUser, LogoutRequest request);
}
