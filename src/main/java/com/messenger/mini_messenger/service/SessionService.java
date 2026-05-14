package com.messenger.mini_messenger.service;

import com.messenger.mini_messenger.dto.response.ApiMessageResponse;
import com.messenger.mini_messenger.dto.response.SessionResponse;
import com.messenger.mini_messenger.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface SessionService {
    List<SessionResponse> listMine(CurrentUser currentUser);

    ApiMessageResponse revoke(CurrentUser currentUser, UUID sessionKeyId);
}
