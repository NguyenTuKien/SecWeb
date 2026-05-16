package com.messenger.mini_messenger.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.messenger.mini_messenger.dto.request.CreateConversationRequest;
import com.messenger.mini_messenger.dto.request.EncryptedConversationKeyRequest;
import com.messenger.mini_messenger.dto.request.MessageAttachmentRequest;
import com.messenger.mini_messenger.dto.request.SendMessageRequest;
import com.messenger.mini_messenger.enums.ConversationKeyRecipientType;
import com.messenger.mini_messenger.enums.ConversationType;
import com.messenger.mini_messenger.enums.MessageType;
import com.messenger.mini_messenger.enums.StorageProvider;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SendMessageRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsPlaintextContentFieldDuringDeserialization() {
        var mapper = JsonMapper.builder()
                .findAndAddModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build();

        String payload = """
                {
                  "cipherData": "base64-cipher",
                  "iv": "base64-iv",
                  "keyVersion": 1,
                  "messageType": "TEXT",
                  "clientCreatedAt": "2026-05-13T09:00:00Z",
                  "content": "server must never accept plaintext"
                }
                """;

        assertThrows(UnrecognizedPropertyException.class, () -> mapper.readValue(payload, SendMessageRequest.class));
    }

    @Test
    void validatesRequiredEncryptedMessageFields() throws JsonProcessingException {
        var request = new SendMessageRequest(
                "client-message-1",
                "Y2lwaGVy",
                "aXY=",
                "YWFk",
                1,
                MessageType.TEXT,
                Instant.parse("2026-05-13T09:00:00Z"),
                null
        );

        assertEquals(0, validator.validate(request).size());
    }

    @Test
    void deserializesClientMessageIdAndAadAsEncryptedTransportMetadata() throws JsonProcessingException {
        var mapper = JsonMapper.builder()
                .findAndAddModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build();

        String payload = """
                {
                  "clientMessageId": "client-message-1",
                  "cipherData": "Y2lwaGVy",
                  "iv": "aXY=",
                  "aad": "YWFk",
                  "keyVersion": 1,
                  "messageType": "TEXT",
                  "clientCreatedAt": "2026-05-13T09:00:00Z"
                }
                """;

        SendMessageRequest request = mapper.readValue(payload, SendMessageRequest.class);

        assertEquals("client-message-1", request.clientMessageId());
        assertEquals("YWFk", request.aad());
    }

    @Test
    void rejectsPlaintextConversationName() {
        var request = new CreateConversationRequest(
                ConversationType.GROUP,
                "Project planning room",
                List.of(UUID.randomUUID()),
                List.of(new EncryptedConversationKeyRequest(
                        UUID.randomUUID(),
                        ConversationKeyRecipientType.MASTER,
                        UUID.randomUUID(),
                        "ZW5jcnlwdGVkLWtleQ==",
                        1
                ))
        );

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void rejectsPlaintextAttachmentMetadata() {
        var request = new SendMessageRequest(
                "client-message-1",
                "Y2lwaGVy",
                "aXY=",
                "YWFk",
                1,
                MessageType.FILE,
                Instant.parse("2026-05-13T09:00:00Z"),
                List.of(new MessageAttachmentRequest(
                        StorageProvider.LOCAL,
                        "object-key",
                        "ZmlsZS1rZXk=",
                        "{\"filename\":\"secret-plan.pdf\"}"
                ))
        );

        assertEquals(1, validator.validate(request).size());
    }
}
