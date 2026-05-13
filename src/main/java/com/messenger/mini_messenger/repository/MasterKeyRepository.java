package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.MasterKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MasterKeyRepository extends JpaRepository<MasterKey, UUID> {
    Optional<MasterKey> findByUserId(UUID userId);
}
