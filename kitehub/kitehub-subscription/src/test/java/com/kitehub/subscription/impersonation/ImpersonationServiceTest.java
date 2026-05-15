package com.kitehub.subscription.impersonation;

import com.kitehub.subscription.impersonation.dto.ImpersonationEndResponse;
import com.kitehub.subscription.impersonation.dto.ImpersonationStartResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.JwtKeyService;
import com.kitehub.platform.domain.entity.Instance;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImpersonationService} (GAP-040 Wave 79 F-bis).
 *
 * <p>Covers GAP-040 ACs:
 * <ul>
 *   <li>30-second TTL enforced on issued JWT exp claim</li>
 *   <li>Audit-log row INSERTed BEFORE token returned (transaction binding)</li>
 *   <li>Manual exit marks row MANUAL_EXIT</li>
 *   <li>Stale active session for same admin auto-closed before new one starts</li>
 *   <li>{@link ImpersonationService#expireStaleSessions} sweep marks AUTO_TIMEOUT</li>
 * </ul></p>
 */
@DisplayName("ImpersonationService")
class ImpersonationServiceTest {

    private ImpersonationAuditRepository auditRepository;
    private InstanceRepository instanceRepository;
    private JwtKeyService jwtKeyService;
    private ImpersonationService service;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        auditRepository = mock(ImpersonationAuditRepository.class);
        instanceRepository = mock(InstanceRepository.class);
        jwtKeyService = mock(JwtKeyService.class);

        // Deterministic test key — 32 bytes for HS256.
        byte[] keyBytes = "test-secret-key-test-secret-key-".getBytes(StandardCharsets.UTF_8);
        signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        when(jwtKeyService.signingKey()).thenReturn(signingKey);

        service = new ImpersonationService(auditRepository, instanceRepository, jwtKeyService);
        ReflectionTestUtils.setField(service, "issuer", "kitehub-subscription-test");

        when(auditRepository.save(any())).thenAnswer(inv -> {
            ImpersonationAuditEntry e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(1001L);
            }
            return e;
        });
    }

    @Test
    @DisplayName("startImpersonation: persists audit row + mints JWT with 30s TTL + tenant/admin claims")
    void start_happy_path() {
        UUID adminId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instance tenant = mock(Instance.class);
        when(tenant.getId()).thenReturn(tenantId);
        when(instanceRepository.findBySubdomainAndDeletedFalse("acme")).thenReturn(Optional.of(tenant));
        when(auditRepository.findActiveSession(adminId)).thenReturn(Optional.empty());

        ImpersonationStartResponse resp = service.startImpersonation(adminId, "acme", "10.0.0.1", "Mozilla/5.0");

        // Audit row saved with expected fields
        ArgumentCaptor<ImpersonationAuditEntry> captor = ArgumentCaptor.forClass(ImpersonationAuditEntry.class);
        verify(auditRepository).save(captor.capture());
        ImpersonationAuditEntry saved = captor.getValue();
        assertThat(saved.getAdminUserId()).isEqualTo(adminId);
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getTenantSlug()).isEqualTo("acme");
        assertThat(saved.getEndedAt()).isNull(); // active
        assertThat(saved.getRequestIp()).isEqualTo("10.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");

        // Response carries the audit-row id + tenant info
        assertThat(resp.sessionId()).isEqualTo(1001L);
        assertThat(resp.tenantId()).isEqualTo(tenantId);
        assertThat(resp.tenantSlug()).isEqualTo("acme");

        // JWT parses + has expected claims + 30s TTL window
        assertThat(resp.impersonationToken()).isNotBlank();
        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(resp.impersonationToken()).getPayload();
        assertThat(claims.get("type", String.class)).isEqualTo("impersonation");
        assertThat(claims.get("tenant_id", String.class)).isEqualTo(tenantId.toString());
        assertThat(claims.get("tenant_slug", String.class)).isEqualTo("acme");
        assertThat(claims.get("impersonated_by", String.class)).isEqualTo(adminId.toString());

        long ttlMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttlMs).isEqualTo(ImpersonationService.SESSION_TTL.toMillis());
        assertThat(Duration.ofMillis(ttlMs)).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("startImpersonation: rejects when tenant slug not found (404 path)")
    void start_tenant_not_found() {
        UUID adminId = UUID.randomUUID();
        when(instanceRepository.findBySubdomainAndDeletedFalse("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startImpersonation(adminId, "ghost", "1.1.1.1", "ua"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ghost");
        verify(auditRepository, never()).save(any());
    }

    @Test
    @DisplayName("startImpersonation: auto-closes stale active session for same admin before starting new one")
    void start_closes_stale_session() {
        UUID adminId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instance tenant = mock(Instance.class);
        when(tenant.getId()).thenReturn(tenantId);
        when(instanceRepository.findBySubdomainAndDeletedFalse("acme")).thenReturn(Optional.of(tenant));

        ImpersonationAuditEntry stale = ImpersonationAuditEntry.builder()
                .id(999L)
                .adminUserId(adminId)
                .tenantId(UUID.randomUUID())
                .tenantSlug("previous")
                .startedAt(OffsetDateTime.now().minusMinutes(5))
                .build();
        when(auditRepository.findActiveSession(adminId)).thenReturn(Optional.of(stale));

        service.startImpersonation(adminId, "acme", "1.1.1.1", "ua");

        // Save called twice — once for the stale-close, once for the new session
        verify(auditRepository, times(2)).save(any());
        assertThat(stale.getEndedAt()).isNotNull();
        assertThat(stale.getEndedReason()).isEqualTo(ImpersonationAuditEntry.EndedReason.AUTO_TIMEOUT);
    }

    @Test
    @DisplayName("endImpersonation: marks active row MANUAL_EXIT")
    void end_manual_exit() {
        UUID adminId = UUID.randomUUID();
        ImpersonationAuditEntry active = ImpersonationAuditEntry.builder()
                .id(42L)
                .adminUserId(adminId)
                .tenantId(UUID.randomUUID())
                .tenantSlug("acme")
                .startedAt(OffsetDateTime.now().minusSeconds(5))
                .build();
        when(auditRepository.findActiveSession(adminId)).thenReturn(Optional.of(active));

        ImpersonationEndResponse resp = service.endImpersonation(adminId);

        assertThat(resp.sessionId()).isEqualTo(42L);
        assertThat(resp.endedReason()).isEqualTo(ImpersonationAuditEntry.EndedReason.MANUAL_EXIT);
        assertThat(active.getEndedAt()).isNotNull();
        assertThat(active.getEndedReason()).isEqualTo(ImpersonationAuditEntry.EndedReason.MANUAL_EXIT);
        verify(auditRepository).save(active);
    }

    @Test
    @DisplayName("endImpersonation: 404 when no active session")
    void end_no_active_session() {
        UUID adminId = UUID.randomUUID();
        when(auditRepository.findActiveSession(adminId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.endImpersonation(adminId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No active impersonation session");
    }

    @Test
    @DisplayName("expireStaleSessions: marks rows past TTL as AUTO_TIMEOUT in saveAll batch")
    void expire_stale_sessions_sweep() {
        ImpersonationAuditEntry expired1 = ImpersonationAuditEntry.builder()
                .id(1L).adminUserId(UUID.randomUUID()).tenantId(UUID.randomUUID()).tenantSlug("a")
                .startedAt(OffsetDateTime.now().minusMinutes(1)).build();
        ImpersonationAuditEntry expired2 = ImpersonationAuditEntry.builder()
                .id(2L).adminUserId(UUID.randomUUID()).tenantId(UUID.randomUUID()).tenantSlug("b")
                .startedAt(OffsetDateTime.now().minusMinutes(2)).build();
        when(auditRepository.findExpiredActiveSessions(any())).thenReturn(List.of(expired1, expired2));

        service.expireStaleSessions();

        assertThat(expired1.getEndedReason()).isEqualTo(ImpersonationAuditEntry.EndedReason.AUTO_TIMEOUT);
        assertThat(expired1.getEndedAt()).isNotNull();
        assertThat(expired2.getEndedReason()).isEqualTo(ImpersonationAuditEntry.EndedReason.AUTO_TIMEOUT);
        verify(auditRepository).saveAll(any());
    }

    @Test
    @DisplayName("expireStaleSessions: no-op when no expired sessions")
    void expire_stale_sessions_no_op() {
        when(auditRepository.findExpiredActiveSessions(any())).thenReturn(List.of());

        service.expireStaleSessions();

        verify(auditRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("startImpersonation: rejects blank tenant slug + null admin id")
    void start_input_validation() {
        assertThatThrownBy(() -> service.startImpersonation(null, "acme", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.startImpersonation(UUID.randomUUID(), "", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.startImpersonation(UUID.randomUUID(), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
