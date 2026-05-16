package com.messenger.mini_messenger.entity;

import com.messenger.mini_messenger.enums.StorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "message_attachments")
public class MessageAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", length = 30, nullable = false)
    private StorageProvider storageProvider;

    @Column(name = "storage_key", length = 500, nullable = false)
    private String storageKey;

    @Column(name = "encrypted_file_key", columnDefinition = "text", nullable = false)
    private String encryptedFileKey;

    @Column(name = "encrypted_metadata", columnDefinition = "text", nullable = false)
    private String encryptedMetadata;
}
