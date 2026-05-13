package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {
}
