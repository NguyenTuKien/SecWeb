package com.messenger.mini_messenger.mapper;

import com.messenger.mini_messenger.dto.request.MessageAttachmentRequest;
import com.messenger.mini_messenger.dto.request.SendMessageRequest;
import com.messenger.mini_messenger.dto.request.UpdateMessageRequest;
import com.messenger.mini_messenger.dto.response.MessageAttachmentResponse;
import com.messenger.mini_messenger.dto.response.MessageResponse;
import com.messenger.mini_messenger.entity.ConversationKeyVersion;
import com.messenger.mini_messenger.entity.Message;
import com.messenger.mini_messenger.entity.MessageAttachment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageMapper {

    public Message toEntity(SendMessageRequest request) {
        Message message = new Message();
        message.setClientMessageId(request.clientMessageId());
        message.setCipherData(request.cipherData());
        message.setIv(request.iv());
        message.setAad(request.aad());
        message.setMessageType(request.messageType());
        message.setClientCreatedAt(request.clientCreatedAt());
        if (request.attachments() != null) {
            request.attachments().forEach(attachmentRequest -> {
                MessageAttachment attachment = toEntity(attachmentRequest);
                attachment.setMessage(message);
                message.getAttachments().add(attachment);
            });
        }
        return message;
    }

    public void applyEncryptedUpdate(Message message, UpdateMessageRequest request, ConversationKeyVersion keyVersion) {
        message.setCipherData(request.cipherData());
        message.setIv(request.iv());
        message.setAad(request.aad());
        message.setKeyVersion(keyVersion);
        message.setMessageType(request.messageType());
    }

    public MessageAttachment toEntity(MessageAttachmentRequest request) {
        MessageAttachment attachment = new MessageAttachment();
        attachment.setStorageProvider(request.storageProvider());
        attachment.setStorageKey(request.storageKey());
        attachment.setEncryptedFileKey(request.encryptedFileKey());
        attachment.setEncryptedMetadata(request.encryptedMetadata());
        return attachment;
    }

    public MessageResponse toResponse(Message message) {
        List<MessageAttachmentResponse> attachments = message.getAttachments().stream()
                .map(this::toResponse)
                .toList();
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getKeyVersion().getKeyVersion(),
                message.getClientMessageId(),
                message.getCipherData(),
                message.getIv(),
                message.getAad(),
                message.getMessageType(),
                message.getClientCreatedAt(),
                message.getCreatedAt(),
                message.getEditedAt(),
                attachments
        );
    }

    private MessageAttachmentResponse toResponse(MessageAttachment attachment) {
        return new MessageAttachmentResponse(
                attachment.getId(),
                attachment.getStorageProvider(),
                attachment.getStorageKey(),
                attachment.getEncryptedFileKey(),
                attachment.getEncryptedMetadata(),
                attachment.getCreatedAt()
        );
    }
}
