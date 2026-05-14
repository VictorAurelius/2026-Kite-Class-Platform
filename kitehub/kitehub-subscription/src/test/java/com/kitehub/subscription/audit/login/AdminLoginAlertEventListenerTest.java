package com.kitehub.subscription.audit.login;

import com.kitehub.subscription.client.EmailServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AdminLoginAlertEventListener} (GAP-517 / Wave 72b Bucket C).
 *
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminLoginAlertEventListener — alert dispatch (GAP-517)")
class AdminLoginAlertEventListenerTest {

    @Mock EmailServiceClient emailServiceClient;

    @InjectMocks AdminLoginAlertEventListener listener;

    @Test
    @DisplayName("Event consumed → EmailServiceClient called with template + params")
    void eventConsumed_emailDispatched() {
        AdminLoginNewFingerprintEvent event = new AdminLoginNewFingerprintEvent(
            42L,
            UUID.randomUUID(),
            "admin@kitehub.me",
            "203.0.113.5",
            "Mozilla/5.0 Test"
        );

        listener.onAdminNewLoginFingerprint(event);

        verify(emailServiceClient, times(1)).sendAdminNewLoginAlert(
            eq("admin@kitehub.me"),
            eq("203.0.113.5"),
            eq("Mozilla/5.0 Test"),
            any(LocalDateTime.class));
    }

    @Test
    @DisplayName("EmailServiceClient throws → listener swallows (login never blocked)")
    void emailFailureSwallowed() {
        doThrow(new RuntimeException("Resend API 500"))
            .when(emailServiceClient)
            .sendAdminNewLoginAlert(any(), any(), any(), any());

        AdminLoginNewFingerprintEvent event = new AdminLoginNewFingerprintEvent(
            42L, UUID.randomUUID(), "admin@kitehub.me", "203.0.113.5", "Mozilla/5.0"
        );

        // Should NOT throw
        listener.onAdminNewLoginFingerprint(event);

        verify(emailServiceClient, times(1)).sendAdminNewLoginAlert(any(), any(), any(), any());
    }
}
