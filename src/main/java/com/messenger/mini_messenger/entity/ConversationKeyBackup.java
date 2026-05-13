package com.messenger.mini_messenger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "conversation_key_backups",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversation_master_key_backup",
                columnNames = {"conversation_id", "key_version_id", "user_id", "master_key_id"}
        )
)
public class ConversationKeyBackup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "key_version_id", nullable = false)
    private ConversationKeyVersion keyVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_key_id", nullable = false)
    private MasterKey masterKey;

    @Column(name = "encrypted_conversation_key", columnDefinition = "text", nullable = false)
    private String encryptedConversationKey;
}
