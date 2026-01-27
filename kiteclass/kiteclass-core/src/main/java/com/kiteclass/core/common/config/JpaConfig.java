package com.kiteclass.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA configuration with auditing support.
 *
 * <p>Enables automatic population of audit fields in {@link com.kiteclass.core.common.entity.BaseEntity}:
 * <ul>
 *   <li>createdAt - automatically set on entity creation</li>
 *   <li>updatedAt - automatically updated on entity modification</li>
 *   <li>createdBy - set via AuditorAware bean</li>
 *   <li>updatedBy - updated via AuditorAware bean</li>
 * </ul>
 *
 * <p>Currently uses a simple implementation that returns null for auditor.
 * This will be replaced with actual user ID from SecurityContext when authentication is integrated.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Provides the current auditor (user) for JPA auditing.
     *
     * <p>TODO: Replace with actual user ID from SecurityContext when authentication is integrated.
     * For now, returns empty Optional to allow auditing to work without authentication.
     *
     * @return AuditorAware that provides current user ID
     */
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            // TODO: Get user ID from SecurityContext
            // Example: return Optional.ofNullable(SecurityContextHolder.getContext())
            //     .map(SecurityContext::getAuthentication)
            //     .filter(Authentication::isAuthenticated)
            //     .map(Authentication::getPrincipal)
            //     .map(principal -> ((UserDetails) principal).getUserId());

            return Optional.empty(); // Return null for now until authentication is integrated
        };
    }
}
