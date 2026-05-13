package com.messenger.mini_messenger.mapper;

import com.messenger.mini_messenger.dto.response.ConversationResponse;
import com.messenger.mini_messenger.dto.response.UserResponse;
import com.messenger.mini_messenger.entity.Conversation;
import com.messenger.mini_messenger.enums.ConversationMemberStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversationMapper {

    private final UserMapper userMapper;

    public ConversationMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public ConversationResponse toResponse(Conversation conversation) {
        List<UserResponse> participants = conversation.getMembers().stream()
                .filter(member -> member.getStatus() == ConversationMemberStatus.ACTIVE)
                .map(member -> userMapper.toResponse(member.getUser()))
                .toList();
        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getCurrentKeyVersion(),
                conversation.getLastMessageAt(),
                participants,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
