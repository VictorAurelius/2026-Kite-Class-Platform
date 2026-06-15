package com.kitehub.branding.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * GAP-1020 (Part 2) — verifies tier is resolved from {@code instances.tier} server-side and that a
 * client-supplied header NEVER escalates above the authoritative value.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionTierResolver — server-side tier, header not trusted")
class SubscriptionTierResolverTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private SubscriptionTierResolver resolver;

    private final UUID instanceId = UUID.randomUUID();

    @BeforeEach
    void wireEntityManager() {
        // @PersistenceContext field isn't constructor-injected — set it explicitly.
        ReflectionTestUtils.setField(resolver, "entityManager", entityManager);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(eq("id"), eq(instanceId))).thenReturn(query);
    }

    @Test
    @DisplayName("DB tier (FREE) wins over a spoofed ENTERPRISE header")
    void dbTierWinsOverHeader() {
        when(query.getSingleResult()).thenReturn("FREE");

        String tier = resolver.resolveEffectiveTier(instanceId, "ENTERPRISE");

        assertThat(tier).isEqualTo("FREE");
    }

    @Test
    @DisplayName("DB tier normalized to upper-case")
    void dbTierNormalized() {
        when(query.getSingleResult()).thenReturn("premium");

        assertThat(resolver.resolveEffectiveTier(instanceId, "FREE")).isEqualTo("PREMIUM");
    }

    @Test
    @DisplayName("No active subscription row → fall back to gateway-trusted header")
    void fallbackToHeaderWhenNoRow() {
        when(query.getSingleResult()).thenThrow(new NoResultException("none"));

        assertThat(resolver.resolveEffectiveTier(instanceId, "BASIC")).isEqualTo("BASIC");
    }

    @Test
    @DisplayName("DB lookup error → fail-safe, never escalates (falls back to header)")
    void failSafeOnLookupError() {
        when(query.getSingleResult()).thenThrow(new IllegalStateException("db down"));

        assertThat(resolver.resolveEffectiveTier(instanceId, "FREE")).isEqualTo("FREE");
    }

    @Test
    @DisplayName("Null instance + null header → least-privilege FREE")
    void nullInstanceNullHeaderDefaultsFree() {
        assertThat(resolver.resolveEffectiveTier(null, null))
                .isEqualTo(SubscriptionTierResolver.DEFAULT_TIER);
    }

    @Test
    @DisplayName("Null instance → header used (gateway-trusted), normalized")
    void nullInstanceUsesHeader() {
        assertThat(resolver.resolveEffectiveTier(null, "enterprise")).isEqualTo("ENTERPRISE");
    }
}
