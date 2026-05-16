package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.ConversationMember;
import com.messenger.mini_messenger.enums.ConversationMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {
    Optional<ConversationMember> findByConversationIdAndUserIdAndStatus(
            UUID conversationId,
            UUID userId,
            ConversationMemberStatus status
    );

    @Query("""
            select member.user.id
            from ConversationMember member
            where member.conversation.id = :conversationId
              and member.status = :status
            """)
    List<UUID> findUserIdsByConversationIdAndStatus(
            @Param("conversationId") UUID conversationId,
            @Param("status") ConversationMemberStatus status
    );
}
