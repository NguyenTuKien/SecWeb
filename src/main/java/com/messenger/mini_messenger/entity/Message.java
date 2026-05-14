package com.messenger.mini_messenger.entity;

import com.messenger.mini_messenger.enums.MessageType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "messages",
        uniqueConstraints = @UniqueConstraint(name = "uk_sender_client_message", columnNames = {"sender_id", "client_message_id"})
)
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "key_version_id", nullable = false)
    private ConversationKeyVersion keyVersion;

    @Column(name = "client_message_id", length = 100)
    private String clientMessageId;

    @Column(name = "cipher_data", columnDefinition = "text", nullable = false)
    private String cipherData;

    @Column(name = "iv", length = 128, nullable = false)
    private String iv;

    @Column(name = "aad", length = 512)
    private String aad;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20, nullable = false)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "client_created_at", nullable = false)
    private Instant clientCreatedAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageAttachment> attachments = new ArrayList<>();
}
