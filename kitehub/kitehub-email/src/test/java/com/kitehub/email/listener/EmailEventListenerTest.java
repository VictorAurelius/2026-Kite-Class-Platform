package com.kitehub.email.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.service.EmailIdempotencyGuard;
import com.kitehub.email.service.SESEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for {@link EmailEventListener} (GAP-787 — staff-invite email delivery).
 *
 * <p>Exercises BOTH producer wire-shapes that publish to {@code email.send}:</p>
 * <ol>
 *   <li>fast-path JSON object ({@code EmailServiceClient.publishToQueue} via convertAndSend(EmailEvent))</li>
 *   <li>outbox JSON-string-wrapping-JSON ({@code SubscriptionOutboxDispatcher} via convertAndSend(payloadString))</li>
 * </ol>
 * plus malformed-payload + missing-field guard paths.
 */
@DisplayName("EmailEventListener — consume email.send → SES dispatch (GAP-787)")
class EmailEventListenerTest {

    private SESEmailService sesEmailService;
    private EmailEventListener listener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        sesEmailService = mock(SESEmailService.class);
        when(sesEmailService.sendTemplatedEmail(org.mockito.ArgumentMatchers.any()))
                .thenReturn(EmailResponse.builder().status("SENT").messageId("mid-1").build());
        // Real guard (60-min TTL) — exercises the actual dedup path, not a mock.
        listener = new EmailEventListener(
                sesEmailService, objectMapper, new EmailIdempotencyGuard(60, 50000));
    }

    @Test
    @DisplayName("fast-path JSON object → renders invite-staff template + dispatches")
    void consumesFastPathJsonObject() {
        String body = """
                {"instanceId":null,"to":"staff.test1@test.vn","subject":"Bạn được mời tham gia Trung tâm Sky Education trên KiteHub",
                 "templateName":"invite-staff","emailType":"invite-staff",
                 "variables":{"recipientName":"Trần Thị Hồng","ownerName":"Nguyễn Văn An",
                 "tenantName":"Trung tâm Anh ngữ Sky Education","role":"STAFF",
                 "inviteUrl":"https://sky-edu-test.kitehub.me/staff/accept-invite?token=RAW-abc","expiresAt":"Thứ Hai, 22/05/2026"}}
                """;

        listener.onEmailEvent(body);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(sesEmailService).sendTemplatedEmail(captor.capture());
        EmailRequest req = captor.getValue();
        assertThat(req.getTo()).isEqualTo("staff.test1@test.vn");
        assertThat(req.getTemplateName()).isEqualTo("invite-staff");
        assertThat(req.getVariables()).containsEntry("recipientName", "Trần Thị Hồng");
        assertThat(req.getVariables()).containsEntry("role", "STAFF");
        assertThat(req.getVariables().get("inviteUrl").toString())
                .contains("/staff/accept-invite?token=RAW-abc");
    }

    @Test
    @DisplayName("outbox JSON-string-wrapping-JSON → unwrapped + dispatched")
    void consumesOutboxDoubleEncodedPayload() throws Exception {
        String inner = """
                {"to":"staff.test2@test.vn","subject":"S","templateName":"invite-staff",
                 "emailType":"invite-staff","variables":{"role":"STAFF"}}
                """;
        // Simulate Jackson2JsonMessageConverter serializing a String payload → JSON string literal.
        String doubleEncoded = objectMapper.writeValueAsString(inner);
        assertThat(doubleEncoded).startsWith("\"");

        listener.onEmailEvent(doubleEncoded);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(sesEmailService).sendTemplatedEmail(captor.capture());
        assertThat(captor.getValue().getTo()).isEqualTo("staff.test2@test.vn");
        assertThat(captor.getValue().getTemplateName()).isEqualTo("invite-staff");
    }

    @Test
    @DisplayName("malformed payload → dropped (no dispatch, no throw)")
    void dropsMalformedPayload() {
        listener.onEmailEvent("not-json-at-all-{{{");
        verify(sesEmailService, never()).sendTemplatedEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("missing recipient → skipped (no dispatch)")
    void skipsMissingRecipient() {
        listener.onEmailEvent("{\"templateName\":\"invite-staff\",\"subject\":\"S\"}");
        verify(sesEmailService, never()).sendTemplatedEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("missing templateName → skipped (no dispatch)")
    void skipsMissingTemplate() {
        listener.onEmailEvent("{\"to\":\"x@test.vn\",\"subject\":\"S\"}");
        verify(sesEmailService, never()).sendTemplatedEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("GAP-580 — redelivery of identical event (explicit idempotencyKey) → single send")
    void idempotentRedeliverySameExplicitKey() {
        String body = """
                {"to":"vy@prospect.vn","subject":"Chào mừng","templateName":"welcome",
                 "emailType":"welcome","idempotencyKey":"welcome:inst-1:vy@prospect.vn:2026-06-02",
                 "variables":{"recipientName":"Vy"}}
                """;

        // First delivery → sends. Redelivery (broker at-least-once) → suppressed.
        listener.onEmailEvent(body);
        listener.onEmailEvent(body);

        // Exactly one provider send despite two consumes.
        verify(sesEmailService, times(1)).sendTemplatedEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("GAP-580 — redelivery of identical event (no explicit key, derived hash) → single send")
    void idempotentRedeliveryDerivedKey() {
        String body = """
                {"to":"hang@owner.vn","subject":"Đăng ký thành công","templateName":"signup",
                 "emailType":"signup","variables":{"recipientName":"Hằng","orgName":"Trung tâm Sao Mai"}}
                """;

        listener.onEmailEvent(body);
        listener.onEmailEvent(body);

        verify(sesEmailService, times(1)).sendTemplatedEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("GAP-580 — distinct recipients (same template) → both sent (no false dedup)")
    void distinctRecipientsBothSent() {
        String body1 = "{\"to\":\"a@test.vn\",\"subject\":\"S\",\"templateName\":\"welcome\",\"emailType\":\"welcome\"}";
        String body2 = "{\"to\":\"b@test.vn\",\"subject\":\"S\",\"templateName\":\"welcome\",\"emailType\":\"welcome\"}";

        listener.onEmailEvent(body1);
        listener.onEmailEvent(body2);

        verify(sesEmailService, times(2)).sendTemplatedEmail(org.mockito.ArgumentMatchers.any());
    }
}
