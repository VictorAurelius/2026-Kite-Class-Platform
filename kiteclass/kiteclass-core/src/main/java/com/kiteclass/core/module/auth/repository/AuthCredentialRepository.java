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
}
