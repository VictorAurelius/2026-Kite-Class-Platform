package com.kitehub.email.api;

import com.kitehub.email.config.SESConfig;
import com.kitehub.email.service.SESEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test for the {@link NotificationChannel} interface — verifies every
 * implementation honors the same calling conventions (BR-NOTIF-001).
 *
 * <p>In Phase 1 only {@link SESEmailService} implements the interface; future
 * implementations (Zalo/SMS/Push from GAP-063b) MUST extend this test to confirm
 * they share the same contract surface.</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@ExtendWith(MockitoExtension.class)
class NotificationChannelContractTest {

    @Mock
    private TemplateEngine templateEngine;

    private NotificationChannel channel;

    @BeforeEach
    void setUp() {
        SESConfig.SESProperties props = new SESConfig.SESProperties();
        props.setMockMode(true);
        props.setFromEmail("noreply@kitehub.test");
        props.setFromName("KiteHub Test");
        SESEmailService impl = new SESEmailService(props, null, null, templateEngine, null,
                new com.kitehub.email.service.EmailTemplateRenderer(templateEngine));
        // Force mock provider to avoid real SES / SMTP wiring in this unit test.
        ReflectionTestUtils.setField(impl, "emailProvider", "mock");
        ReflectionTestUtils.setField(impl, "brandingEnabled", false);
        channel = impl;
    }

    @Test
    void channelName_isNonBlankAndMatchesEnumConvention() {
        // Channel name must align with NotificationChannelType enum (UPPER_SNAKE).
        assertThat(channel.channelName()).isNotBlank();
        assertThat(channel.channelName()).isEqualTo(channel.channelName().toUpperCase());
    }

    @Test
    void send_returnsNonNullResultEvenForMockMode() {
        NotificationSendResult result = channel.send(
                "test@example.com",
                "<p>Hello</p>",
                NotificationContext.builder().subject("Test").build()
        );

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isIn(
                NotificationSendResult.Status.SENT,
                NotificationSendResult.Status.MOCK
        );
        assertThat(result.getSentAt()).isNotNull();
        assertThat(result.getChannel()).isEqualTo(channel.channelName());
    }

    @Test
    void send_withNullContext_doesNotThrow() {
        // Per interface javadoc: implementations MUST tolerate null context.
        NotificationSendResult result = channel.send(
                "test@example.com",
                "<p>Body</p>",
                null
        );

        assertThat(result).isNotNull();
    }

    @Test
    void send_withBlankRecipient_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                channel.send("", "body", NotificationContext.builder().subject("s").build())
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void send_withNullRecipient_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                channel.send(null, "body", NotificationContext.builder().subject("s").build())
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
