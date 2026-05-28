package com.kiteclass.core.common.config;

import com.kiteclass.core.common.context.UserContext;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA configuration with auditing support.
 *
 * <p>Enables automatic population of audit fields in {@link com.kiteclass.core.common.entity.BaseEntity}:
 * <ul>
 *   <li>createdAt - automatically set on entity creation</li>
 *   <li>updatedAt - automatically updated on entity modification</li>
 *   <li>createdBy - set via AuditorAware bean (from UserContext)</li>
 *   <li>updatedBy - updated via AuditorAware bean (from UserContext)</li>
 * </ul>
 *
 * <p>User ID is extracted from X-User-Id header (forwarded by Gateway) and stored in
 * UserContext by TenantFilterInterceptor. Returns empty if user context not set
 * (e.g., unauthenticated requests, background jobs).
 *
 * @author KiteClass Team
 * @since 2.2.0
 * @see com.kiteclass.core.common.context.UserContext
 * @see com.kiteclass.core.config.TenantFilterInterceptor
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Provides the current auditor (user) for JPA auditing.
     *
     * <p>Extracts user ID from UserContext (set by TenantFilterInterceptor from X-User-Id header).
     * Returns empty if user context not set (e.g., unauthenticated requests, background jobs).
     *
     * @return AuditorAware that provides current user ID
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.ofNullable(UserContext.getCurrentUser());
    }

    /**
     * Configure EntityManagerFactory to use Spring-managed entity listeners.
     * This ensures EntityPersistenceListener gets Spring dependencies injected.
     */
    @Autowired
    public void configureEntityManagerFactory(EntityManagerFactory emf) {
        if (emf instanceof org.hibernate.engine.spi.SessionFactoryImplementor) {
            System.err.println("=== Configuring Hibernate to use Spring-managed entity listeners ===");
        }
    }
}
