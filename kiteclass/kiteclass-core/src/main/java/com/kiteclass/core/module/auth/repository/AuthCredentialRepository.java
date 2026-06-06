package com.kiteclass.core.module.auth.repository;

import com.kiteclass.core.module.auth.entity.AuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Pre-auth credential lookup for KC-native login (Wave auth-1).
 *
 * <p>Lookup by globally-unique email — runs BEFORE any tenant context is set,
 * so {@link AuthCredential} deliberately carries no tenant {@code @Filter}.
 */
@Repository
public interface AuthCredentialRepository extends JpaRepository<AuthCredential, Long> {

    Optional<AuthCredential> findByEmailIgnoreCase(String email);

    /**
     * Lookup by owning domain entity (entityType + entityId) — used to disable a
     * credential when its parent/teacher entity is deactivated/soft-deleted
     * (Wave auth-2, GAP-1013b). Backed by index {@code ix_auth_credentials_entity}.
     */
    Optional<AuthCredential> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
