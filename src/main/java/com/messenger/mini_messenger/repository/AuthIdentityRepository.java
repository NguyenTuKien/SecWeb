package com.messenger.mini_messenger.repository;

import com.messenger.mini_messenger.entity.AuthIdentity;
import com.messenger.mini_messenger.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, UUID> {
    Optional<AuthIdentity> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);

    Optional<AuthIdentity> findByUserIdAndProvider(UUID userId, AuthProvider provider);
}
