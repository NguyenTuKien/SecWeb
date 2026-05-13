package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.ConversationKeyVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationKeyVersionRepository extends JpaRepository<ConversationKeyVersion, UUID> {
    Optional<ConversationKeyVersion> findByConversationIdAndKeyVersion(UUID conversationId, int keyVersion);
}
