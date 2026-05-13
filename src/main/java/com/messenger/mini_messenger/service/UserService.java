package com.messenger.mini_messenger.service;

import com.messenger.mini_messenger.dto.response.ActiveSessionResponse;
import com.messenger.mini_messenger.dto.response.PublicKeyResponse;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse getMe(CurrentUser currentUser);

    List<UserResponse> search(String keyword);

    PublicKeyResponse getPublicKey(UUID userId);

    List<ActiveSessionResponse> getActiveSessions(UUID userId);
}
