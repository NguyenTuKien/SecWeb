package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findTop30ByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID conversationId);

    List<Message> findByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID conversationId);
}
