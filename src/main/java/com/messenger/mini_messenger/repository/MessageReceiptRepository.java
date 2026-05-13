package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.MessageReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, UUID> {
}
