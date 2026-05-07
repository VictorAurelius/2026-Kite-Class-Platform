package com.kitehub.branding.wizard.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Bean wiring for Wave 34 wizard endpoints. Provides a UTC {@link Clock}
 * (overridable in tests via {@code @MockitoBean Clock}) and enables
 * {@code @Scheduled} for the SSE deploy-stream poller + heartbeat.
 */
@Configuration
@EnableScheduling
public class WizardConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
