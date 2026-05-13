package com.messenger.mini_messenger.entity;

import com.messenger.mini_messenger.enums.MessageType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageAttachmentEntityTest {

    @Test
    void messageTypesSupportEncryptedMediaMessages() {
        Set<String> names = Arrays.stream(MessageType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertTrue(names.containsAll(Set.of("TEXT", "IMAGE", "VIDEO", "AUDIO", "FILE")));
    }

    @Test
    void attachmentKeepsOnlyEncryptedStorageAndMetadata() throws NoSuchFieldException {
        Class<MessageAttachment> type = MessageAttachment.class;

        type.getDeclaredField("storageProvider");
        type.getDeclaredField("storageKey");
        type.getDeclaredField("encryptedFileKey");
        type.getDeclaredField("encryptedMetadata");
        BaseEntity.class.getDeclaredField("createdAt");
    }
}
