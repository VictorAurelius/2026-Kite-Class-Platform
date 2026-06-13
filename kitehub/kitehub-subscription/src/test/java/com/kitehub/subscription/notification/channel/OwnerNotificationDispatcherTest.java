package com.kitehub.subscription.notification.channel;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.billing.dto.ReceiptResponse;
import com.kitehub.subscription.notification.enums.NotificationChannelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OwnerNotificationDispatcher} — multi-channel fan-out (GAP-1265).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerNotificationDispatcher Unit Tests")
class OwnerNotificationDispatcherTest {

    @Mock private NotificationChannel emailChannel;
    @Mock private NotificationChannel inAppChannel;

    private OwnerNotificationDispatcher dispatcher() {
        when(emailChannel.type()).thenReturn(NotificationChannelType.EMAIL);
        when(inAppChannel.type()).thenReturn(NotificationChannelType.IN_APP);
        lenient().when(emailChannel.deliver(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        lenient().when(inAppChannel.deliver(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        return new OwnerNotificationDispatcher(List.of(emailChannel, inAppChannel));
    }

    @Test
    @DisplayName("GAP-1265: notifyOwner fans out to both EMAIL and IN_APP channels")
    void notifyOwner_fansOutToAllChannels() {
        OwnerNotificationDispatcher dispatcher = dispatcher();
        OwnerNotification n = OwnerNotification.builder()
            .instanceId(UUID.randomUUID())
            .notificationType("payment-confirmed")
            .recipientEmail("owner@example.com")
            .emailTemplate("payment-confirmed")
            .title("t").body("b")
            .build();

        dispatcher.notifyOwner(n);

        verify(emailChannel).deliver(n);
        verify(inAppChannel).deliver(n);
    }

    @Test
    @DisplayName("GAP-1257-BE: sendPaymentConfirmed builds a payment-confirmed notification with the receipt")
    void sendPaymentConfirmed_buildsNotification() {
        OwnerNotificationDispatcher dispatcher = dispatcher();
        Instance instance = new Instance();
        instance.setId(UUID.randomUUID());
        instance.setContactEmail("owner@example.com");
        instance.setOrganizationName("Trung tâm Demo");
        ReceiptResponse receipt = ReceiptResponse.builder()
            .receiptNumber("BN-2026-AAAA")
            .amountVnd(500_000L)
            .tier("BASIC")
            .build();

        dispatcher.sendPaymentConfirmed(instance, receipt);

        ArgumentCaptor<OwnerNotification> captor = ArgumentCaptor.forClass(OwnerNotification.class);
        verify(emailChannel).deliver(captor.capture());
        OwnerNotification sent = captor.getValue();
        assertThat(sent.getNotificationType()).isEqualTo("payment-confirmed");
        assertThat(sent.getRecipientEmail()).isEqualTo("owner@example.com");
        assertThat(sent.getEmailTemplate()).isEqualTo("payment-confirmed");
        assertThat(sent.getEmailVariables()).containsEntry("receiptNumber", "BN-2026-AAAA");
    }

    @Test
    @DisplayName("GAP-1263-BE: sendWinBack builds a winback-reactivate notification with CTA")
    void sendWinBack_buildsNotification() {
        OwnerNotificationDispatcher dispatcher = dispatcher();
        Instance instance = new Instance();
        instance.setId(UUID.randomUUID());
        instance.setContactEmail("owner@example.com");
        instance.setOrganizationName("Trung tâm Demo");

        dispatcher.sendWinBack(instance, true);

        ArgumentCaptor<OwnerNotification> captor = ArgumentCaptor.forClass(OwnerNotification.class);
        verify(inAppChannel).deliver(captor.capture());
        OwnerNotification sent = captor.getValue();
        assertThat(sent.getNotificationType()).isEqualTo("winback-reactivate");
        assertThat(sent.getActionUrl()).contains("reactivate");
        assertThat(sent.getEmailVariables()).containsKey("reactivateUrl");
    }
}
