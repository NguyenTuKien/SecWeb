package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    @Query("""
            select distinct c
            from Conversation c
            join c.members m
            where m.user.id = :userId
              and m.status = com.messenger.mini_messenger.enums.ConversationMemberStatus.ACTIVE
            order by c.lastMessageAt desc nulls last, c.createdAt desc
            """)
    List<Conversation> findActiveConversationsForUser(UUID userId);
}
