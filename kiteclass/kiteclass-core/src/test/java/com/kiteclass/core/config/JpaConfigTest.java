package com.kiteclass.core.config;

import com.kiteclass.core.common.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JpaConfig}.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
class JpaConfigTest {

    @Test
    void auditorProvider_shouldReturnAuditorAware() {
        // Given
        JpaConfig config = new JpaConfig();

        // When
        AuditorAware<Long> auditorAware = config.auditorProvider();

        // Then
        assertThat(auditorAware).isNotNull();
    }

    @Test
    void auditorProvider_shouldReturnEmptyOptionalWhenNoAuthenticationContext() {
        // Given
        JpaConfig config = new JpaConfig();
        AuditorAware<Long> auditorAware = config.auditorProvider();

        // When
        Optional<Long> currentAuditor = auditorAware.getCurrentAuditor();

        // Then
        // Should return empty until authentication is integrated
        assertThat(currentAuditor).isEmpty();
    }
}
