package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.ConversationKeyBackup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationKeyBackupRepository extends JpaRepository<ConversationKeyBackup, UUID> {
    Optional<ConversationKeyBackup> findByConversationIdAndUserIdAndKeyVersionKeyVersion(
            UUID conversationId,
            UUID userId,
            int keyVersion
    );
}
