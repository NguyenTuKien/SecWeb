package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.ConversationMember;
import com.messenger.mini_messenger.enums.ConversationMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {
    Optional<ConversationMember> findByConversationIdAndUserIdAndStatus(
            UUID conversationId,
            UUID userId,
            ConversationMemberStatus status
    );
}
