package com.kitehub.subscription.audit.login;

import com.kitehub.platform.domain.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoginAuditService} new-fingerprint detection +
 * cooldown logic (GAP-517 / Wave 72b Bucket C).
 *
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoginAuditService — new-fingerprint admin alert (GAP-517)")
class LoginAuditServiceTest {

    @Mock LoginAuditLogRepository repository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock HttpServletRequest request;

    LoginAuditService service;
    User adminUser;
    User regularUser;

    @BeforeEach
    void setUp() {
        service = new LoginAuditService(repository, eventPublisher);

        adminUser = User.builder()
            .id(UUID.randomUUID())
            .email("admin@kitehub.me")
            .name("Platform Admin")
            .passwordHash("x")
            .role("PLATFORM_ADMIN")
            .build();

        regularUser = User.builder()
            .id(UUID.randomUUID())
            .email("user@kitehub.me")
            .name("Tenant User")
            .passwordHash("x")
            .role("OWNER")
            .build();

        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Test");
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        // Echo the saved entity back with a stable id so cooldown filter logic works.
        when(repository.save(any(LoginAuditLog.class))).thenAnswer(inv -> {
            LoginAuditLog row = inv.getArgument(0);
            if (row.getId() == null) row.setId(42L);
            return row;
        });
    }

    @Test
    @DisplayName("Known fingerprint within cooldown → NO event emitted")
    void knownFingerprintWithinCooldown_noEvent() {
        // Pretend repository already has a recent matching row for this fingerprint
        when(repository.findRecentByUserAndFingerprint(eq(adminUser.getId()),
                anyString(), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(LoginAuditLog.builder().id(99L).build()));

        service.recordLogin(adminUser, request);

        verify(eventPublisher, never()).publishEvent(any(AdminLoginNewFingerprintEvent.class));
    }

    @Test
    @DisplayName("New fingerprint for non-PLATFORM_ADMIN → NO event emitted")
    void newFingerprintNonAdmin_noEvent() {
        when(repository.findRecentByUserAndFingerprint(any(), anyString(), any(), any(Pageable.class)))
            .thenReturn(List.of());

        service.recordLogin(regularUser, request);

        verify(eventPublisher, never()).publishEvent(any(AdminLoginNewFingerprintEvent.class));
        // But the audit row IS still persisted (non-admin auditing).
        verify(repository, times(1)).save(any(LoginAuditLog.class));
    }

    @Test
    @DisplayName("New fingerprint for PLATFORM_ADMIN → event emitted + alert_sent=true")
    void newFingerprintAdmin_eventEmittedAndFlagged() {
        when(repository.findRecentByUserAndFingerprint(any(), anyString(), any(), any(Pageable.class)))
            .thenReturn(List.of());

        service.recordLogin(adminUser, request);

        ArgumentCaptor<AdminLoginNewFingerprintEvent> evt =
            ArgumentCaptor.forClass(AdminLoginNewFingerprintEvent.class);
        verify(eventPublisher, times(1)).publishEvent(evt.capture());
        assertThat(evt.getValue().getUserId()).isEqualTo(adminUser.getId());
        assertThat(evt.getValue().getEmail()).isEqualTo("admin@kitehub.me");
        assertThat(evt.getValue().getIp()).isEqualTo("203.0.113.5");

        // Two saves: initial row + flagged update with alert_sent=true.
        ArgumentCaptor<LoginAuditLog> rowCap = ArgumentCaptor.forClass(LoginAuditLog.class);
        verify(repository, times(2)).save(rowCap.capture());
        LoginAuditLog finalRow = rowCap.getAllValues().get(rowCap.getAllValues().size() - 1);
        assertThat(finalRow.isAlertSent()).isTrue();
        assertThat(finalRow.getAlertSentAt()).isNotNull();
    }

    @Test
    @DisplayName("24h cooldown: same fingerprint twice for admin → only first emits event")
    void cooldownEnforced_secondCallSuppressed() {
        // First call: no prior row → event fires
        when(repository.findRecentByUserAndFingerprint(any(), anyString(), any(), any(Pageable.class)))
            .thenReturn(List.of());
        service.recordLogin(adminUser, request);
        verify(eventPublisher, times(1)).publishEvent(any(AdminLoginNewFingerprintEvent.class));

        // Second call: simulate the just-saved row being within cooldown
        // (the filter excludes the same id, but we mock a DIFFERENT prior id)
        when(repository.findRecentByUserAndFingerprint(any(), anyString(), any(), any(Pageable.class)))
            .thenReturn(List.of(LoginAuditLog.builder().id(7L).build()));
        service.recordLogin(adminUser, request);

        // Still only 1 event emitted total — second call suppressed by cooldown
        verify(eventPublisher, times(1)).publishEvent(any(AdminLoginNewFingerprintEvent.class));
    }

    @Test
    @DisplayName("Null HttpServletRequest → audit still attempted, no NPE")
    void nullRequest_noNpe() {
        when(repository.findRecentByUserAndFingerprint(any(), anyString(), any(), any(Pageable.class)))
            .thenReturn(List.of());

        // Should not throw
        service.recordLogin(adminUser, null);

        // Event still fires with null ip/ua
        verify(eventPublisher, times(1)).publishEvent(any(AdminLoginNewFingerprintEvent.class));
    }

    @Test
    @DisplayName("GAP-707: multi-row cooldown hit → no 'unique result' WARN + cooldown still enforced")
    void multiRowCooldownHit_pageBoundedQuery_noWarn() {
        // Scenario: admin user has multiple prior rows for same fingerprint within
        // cooldown window (which is the COMMON case — every successful login adds a row).
        // Pre-fix: repository returned Optional but @Query had no LIMIT → Spring emitted
        // "Query did not return a unique result" WARN per login.
        // Post-fix: query returns List bounded by Pageable to size 1 → no WARN, just
        // the most recent prior row.
        LoginAuditLog mostRecentPrior = LoginAuditLog.builder().id(7L).build();
        when(repository.findRecentByUserAndFingerprint(
                any(), anyString(), any(), any(Pageable.class)))
            .thenReturn(List.of(mostRecentPrior));

        service.recordLogin(adminUser, request);

        // Cooldown logic still works deterministically: prior row found → no event emitted
        verify(eventPublisher, never()).publishEvent(any(AdminLoginNewFingerprintEvent.class));

        // Verify repository called with PageRequest.of(0, 1) — query bound to single result
        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(1)).findRecentByUserAndFingerprint(
            eq(adminUser.getId()), anyString(), any(LocalDateTime.class), pageableCap.capture());
        Pageable used = pageableCap.getValue();
        assertThat(used.getPageSize()).isEqualTo(1);
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used).isEqualTo(PageRequest.of(0, 1));
    }

    @Test
    @DisplayName("Fingerprint hash is deterministic + 64 hex chars")
    void fingerprintHashFormat() {
        String h1 = LoginAuditService.computeFingerprint("203.0.113.5", "Mozilla/5.0");
        String h2 = LoginAuditService.computeFingerprint("203.0.113.5", "Mozilla/5.0");
        String h3 = LoginAuditService.computeFingerprint("203.0.113.6", "Mozilla/5.0");

        assertThat(h1).isEqualTo(h2);
        assertThat(h1).isNotEqualTo(h3);
        assertThat(h1).hasSize(64).matches("[0-9a-f]+");
    }
}
