package com.kitehub.email.service;

import com.kitehub.email.api.NotificationContext;
import com.kitehub.email.api.NotificationSendResult;
import com.kitehub.email.zalo.ZaloMessage;
import com.kitehub.email.zalo.ZaloOAClient;
import com.kitehub.email.zalo.ZaloOAConfig;
import com.kitehub.email.zalo.ZaloOAMockClient;
import com.kitehub.email.zalo.ZaloSendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ZaloNotificationChannel} — verifies the ZALO adapter bridges the
 * platform {@code NotificationChannel} seam down to the {@link ZaloOAClient} mock without
 * any real network call (GAP-063 Phase 1, Wave local-doable-11 Bucket B).
 *
 * <p>Mirrors the project's Mockito + AssertJ unit-test conventions
 * ({@code OwnerNotificationDispatcherTest}). The happy-path test wires the REAL
 * {@link ZaloOAMockClient} so the deterministic mock dispatch path is exercised end-to-end;
 * the template-mapping + skip tests use a Mockito-mocked {@link ZaloOAClient} so we can assert
 * the {@link ZaloMessage} built by the adapter and the no-client fallback. None of the tests
 * touch a live Zalo OA endpoint.</p>
 *
 * @since Wave local-doable-11 Bucket B (GAP-063 Phase 1 scaffold)
 */
@ExtendWith(MockitoExtension.class)
class ZaloNotificationChannelTest {

    @Mock
    private ObjectProvider<ZaloOAClient> clientProvider;

    private final ZaloOAConfig.ZaloProperties properties = new ZaloOAConfig.ZaloProperties();

    private ZaloNotificationChannel newChannel() {
        return new ZaloNotificationChannel(clientProvider, properties);
    }

    @Test
    @DisplayName("channelName() == ZALO (matches NotificationChannelType.ZALO)")
    void channelNameIsZalo() {
        assertThat(newChannel().channelName()).isEqualTo("ZALO");
        assertThat(ZaloNotificationChannel.CHANNEL_NAME).isEqualTo("ZALO");
    }

    @Test
    @DisplayName("Mock-mode dispatch returns MOCK status + deterministic fake id, no network")
    void mockDispatchReturnsSuccessWithFakeId() {
        // Real mock client = exercises the actual deterministic mock path (no HTTP).
        when(clientProvider.getIfAvailable()).thenReturn(new ZaloOAMockClient());
        NotificationContext ctx = NotificationContext.builder()
                .subject("Thanh toán đã được xác nhận")
                .locale("vi")
                .build();

        NotificationSendResult result = newChannel().send("zalo-user-1", "Xin chào", ctx);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationSendResult.Status.MOCK);
        assertThat(result.getProviderMessageId()).isEqualTo("mock-zalo-1");
        assertThat(result.getChannel()).isEqualTo("ZALO");
        assertThat(result.getSentAt()).isNotNull();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("ctx.templateName resolves through zalo.zns-template-ids into the ZaloMessage")
    void resolvesZnsTemplateIdMapping() {
        ZaloOAClient mockClient = mock(ZaloOAClient.class);
        when(mockClient.sendMessage(anyString(), any()))
                .thenReturn(ZaloSendResult.builder()
                        .providerMessageId("mock-zalo-42")
                        .status(ZaloSendResult.Status.MOCK)
                        .sentAt(Instant.now())
                        .build());
        when(clientProvider.getIfAvailable()).thenReturn(mockClient);
        properties.setZnsTemplateIds(Map.of("payment-confirmed", "ZNS_TPL_123"));

        NotificationContext ctx = NotificationContext.builder()
                .templateName("payment-confirmed")
                .locale("vi")
                .build();

        NotificationSendResult result = newChannel().send("zalo-user-9", "body", ctx);

        ArgumentCaptor<ZaloMessage> captor = ArgumentCaptor.forClass(ZaloMessage.class);
        verify(mockClient).sendMessage(eq("zalo-user-9"), captor.capture());
        ZaloMessage sent = captor.getValue();
        assertThat(sent.getTemplateId()).isEqualTo("ZNS_TPL_123");
        assertThat(sent.getBody()).isEqualTo("body");
        assertThat(sent.getLocale()).isEqualTo("vi");
        assertThat(result.getStatus()).isEqualTo(NotificationSendResult.Status.MOCK);
        assertThat(result.getProviderMessageId()).isEqualTo("mock-zalo-42");
    }

    @Test
    @DisplayName("No ZaloOAClient available → SKIPPED_DISABLED_IN_PHASE_1, no throw")
    void skipsWhenNoClientAvailable() {
        when(clientProvider.getIfAvailable()).thenReturn(null);

        NotificationSendResult result = newChannel().send("zalo-user-1", "hi", null);

        assertThat(result.getStatus())
                .isEqualTo(NotificationSendResult.Status.SKIPPED_DISABLED_IN_PHASE_1);
        assertThat(result.getChannel()).isEqualTo("ZALO");
        assertThat(result.getProviderMessageId()).isNull();
    }

    @Test
    @DisplayName("Blank/null recipient is rejected before any client interaction")
    void rejectsBlankRecipient() {
        ZaloNotificationChannel channel = newChannel();

        assertThatThrownBy(() -> channel.send(null, "hi", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> channel.send("   ", "hi", null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(clientProvider);
    }
}
