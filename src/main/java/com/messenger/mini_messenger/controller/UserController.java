package com.messenger.mini_messenger.controller;

import com.messenger.mini_messenger.dto.response.ActiveSessionResponse;
import com.messenger.mini_messenger.dto.response.PublicKeyResponse;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return userService.getMe(currentUser);
    }

    @GetMapping("/search")
    public List<UserResponse> search(@RequestParam(name = "q", defaultValue = "") String keyword) {
        return userService.search(keyword);
    }

    @GetMapping("/{userId}/public-key")
    public PublicKeyResponse publicKey(@PathVariable UUID userId) {
        return userService.getPublicKey(userId);
    }

    @GetMapping("/{userId}/sessions")
    public List<ActiveSessionResponse> activeSessions(@PathVariable UUID userId) {
        return userService.getActiveSessions(userId);
    }
}
