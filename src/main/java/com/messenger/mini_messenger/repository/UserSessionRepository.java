package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.UserSession;
import com.messenger.mini_messenger.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserId(UUID userId);

    List<UserSession> findByUserIdAndStatus(UUID userId, SessionStatus status);
}
