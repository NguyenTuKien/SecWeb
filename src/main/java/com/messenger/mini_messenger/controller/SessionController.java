package com.messenger.mini_messenger.controller;

import com.messenger.mini_messenger.dto.response.ApiMessageResponse;
import com.messenger.mini_messenger.dto.response.SessionResponse;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.SessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/me")
    public List<SessionResponse> listMine(@AuthenticationPrincipal CurrentUser currentUser) {
        return sessionService.listMine(currentUser);
    }

    @DeleteMapping("/{sessionKeyId}")
    public ApiMessageResponse revoke(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID sessionKeyId
    ) {
        return sessionService.revoke(currentUser, sessionKeyId);
    }
}
