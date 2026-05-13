package com.messenger.mini_messenger.controller;

import com.messenger.mini_messenger.dto.request.CreateConversationRequest;
import com.messenger.mini_messenger.dto.request.StoreConversationKeysRequest;
import com.messenger.mini_messenger.dto.request.UpdateConversationRequest;
import com.messenger.mini_messenger.dto.response.ApiMessageResponse;
import com.messenger.mini_messenger.dto.response.ConversationResponse;
import com.messenger.mini_messenger.dto.response.EncryptedConversationKeyResponse;
import com.messenger.mini_messenger.security.CurrentUser;
import com.messenger.mini_messenger.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationService.create(currentUser, request));
    }

    @GetMapping
    public List<ConversationResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
        return conversationService.list(currentUser);
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse get(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID conversationId) {
        return conversationService.get(currentUser, conversationId);
    }

    @PatchMapping("/{conversationId}")
    public ConversationResponse update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateConversationRequest request
    ) {
        return conversationService.update(currentUser, conversationId, request);
    }

    @DeleteMapping("/{conversationId}")
    public ApiMessageResponse leave(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID conversationId) {
        return conversationService.leave(currentUser, conversationId);
    }

    @PostMapping("/{conversationId}/keys")
    public ResponseEntity<ApiMessageResponse> storeKeys(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID conversationId,
            @Valid @RequestBody StoreConversationKeysRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationService.storeKeys(currentUser, conversationId, request));
    }

    @GetMapping("/{conversationId}/keys/me")
    public EncryptedConversationKeyResponse getMyKey(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID conversationId,
            @RequestParam(name = "keyVersion", required = false) Integer keyVersion
    ) {
        return conversationService.getMyEncryptedKey(currentUser, conversationId, keyVersion);
    }

    @PutMapping("/{conversationId}/keys")
    public ApiMessageResponse rotateKeys(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID conversationId,
            @Valid @RequestBody StoreConversationKeysRequest request
    ) {
        return conversationService.rotateKeys(currentUser, conversationId, request);
    }
}
