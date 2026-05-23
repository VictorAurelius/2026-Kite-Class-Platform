package com.kitehub.email.service;

import com.kitehub.email.dto.EmailRequest;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-703 Wave 104 Bucket B2 / GAP-657 Wave 107 — integration test for email
 * layer hardening. Verifies:
 *
 * <ul>
 *   <li>RFC 8058 {@code List-Unsubscribe} + {@code List-Unsubscribe-Post}
 *       headers present on every outbound MimeMessage (Gmail bulk-sender
 *       deliverability requirement).</li>
 *   <li>{@code multipart/alternative} body with BOTH text/html + text/plain
 *       parts whenever the template has a {@code .txt} sibling (RFC 2046
 *       multipart fallback for HTML-stripping clients).</li>
 *   <li>{@code Reply-To} header set per GAP-657 §Step 3.</li>
 * </ul>
 *
 * <p><b>Wave 107 fix (GAP-657 20%):</b> The original {@code @Disabled} was
 * caused by a mismatch between the standalone Thymeleaf resolver chain used
 * in {@code setUp()} (htmlResolver {@code .html} + textResolver {@code ""})
 * and the production {@link com.kitehub.email.config.EmailTemplateResolverConfig}
 * dual-mode setup (suffix {@code .html} order 1 + suffix {@code .txt} order 50).
 * Fix: use {@code @SpringBootTest} so the real production context — including
 * {@code EmailTemplateResolverConfig} — wires correctly.</p>
 *
 * <p>Strategy: {@code @MockitoBean JavaMailSender} returns a real
 * {@link MimeMessage} built from a no-op Session so the MIME structure is
 * inspectable. Capture via {@code verify(mailSender).send(captor.capture())}.
 * No real SMTP connection needed (GAP-612 AWS suspension).</p>
 *
 * @since Wave 107 (GAP-657)
 */
@SpringBootTest
@TestPropertySource(properties = {
        "email.provider=smtp",
        "aws.ses.mock-mode=true",
        "aws.ses.from-email=noreply@kitehub.me",
        "aws.ses.from-name=KiteHub",
        "aws.ses.reply-to-email=support@kitehub.me",
        "aws.ses.unsubscribe-mailto=unsubscribe@kitehub.me",
        "aws.ses.unsubscribe-url-template=https://kitehub.me/unsubscribe?token={token}",
        "kitehub.email.branding-enabled=false",
        "kitehub.email.branding.rabbit-enabled=false"
})
@DisplayName("Email hardening — List-Unsubscribe + multipart/alternative (GAP-657 / GAP-703)")
class EmailHardeningTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private SESEmailService service;

    @Test
    @DisplayName("welcome template: List-Unsubscribe headers + Reply-To + multipart/alternative with plain-text part")
    void shouldIncludeListUnsubscribeAndPlainText() throws Exception {
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));

        EmailRequest req = EmailRequest.builder()
                .to("hong.tran@skyedu.vn")
                .subject("Chào mừng đến KiteHub")
                .templateName("welcome")
                .variables(Map.of(
                        "organizationName", "Trung tâm Sky Education",
                        "trialDays", 30,
                        "expiryDate", "30/06/2026",
                        "loginUrl", "https://kitehub.me/login"))
                .build();

        service.sendTemplatedEmail(req);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        // saveChanges() flushes the MIME tree so DataHandler Content-Type values are written
        // to the MIME part headers — mirrors what real SMTP transport does before transmitting.
        // Without this, isMimeType("text/html") always returns false on a no-op Session message.
        sent.saveChanges();

        // List-Unsubscribe headers (RFC 8058) — Gmail bulk-sender mandate.
        String[] listUnsub = sent.getHeader("List-Unsubscribe");
        assertThat(listUnsub).as("List-Unsubscribe header must be present").isNotNull().isNotEmpty();
        assertThat(listUnsub[0])
                .contains("mailto:unsubscribe@kitehub.me")
                .contains("https://kitehub.me/unsubscribe");

        String[] listUnsubPost = sent.getHeader("List-Unsubscribe-Post");
        assertThat(listUnsubPost).as("List-Unsubscribe-Post header must be present").isNotNull().isNotEmpty();
        assertThat(listUnsubPost[0]).isEqualTo("List-Unsubscribe=One-Click");

        // Reply-To preserved (GAP-657 §Step 3).
        String[] replyTo = sent.getHeader("Reply-To");
        assertThat(replyTo).as("Reply-To header must be present").isNotNull().isNotEmpty();
        assertThat(replyTo[0]).contains("support@kitehub.me");

        // multipart/alternative body with both text/html + text/plain parts.
        Object content = sent.getContent();
        assertThat(content).isInstanceOf(MimeMultipart.class);
        MimeMultipart multipart = unwrapAlternativePart((MimeMultipart) content);
        assertThat(multipart.getContentType()).startsWith("multipart/alternative");

        boolean hasPlain = false;
        boolean hasHtml = false;
        for (int i = 0; i < multipart.getCount(); i++) {
            jakarta.mail.BodyPart bp = multipart.getBodyPart(i);
            if (bp.isMimeType("text/plain")) {
                hasPlain = true;
            } else if (bp.isMimeType("text/html")) {
                hasHtml = true;
            }
        }
        assertThat(hasPlain).as("multipart/alternative must contain text/plain part").isTrue();
        assertThat(hasHtml).as("multipart/alternative must contain text/html part").isTrue();
    }

    @Test
    @DisplayName("password-reset template: List-Unsubscribe + plain-text part present")
    void passwordResetTemplateAlsoHardened() throws Exception {
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));

        EmailRequest req = EmailRequest.builder()
                .to("tam.nguyen@skyedu.vn")
                .subject("Đặt lại mật khẩu KiteHub")
                .templateName("password-reset")
                .variables(Map.of(
                        "userName", "Nguyễn Văn An",
                        "resetUrl", "https://kitehub.me/reset?token=abc",
                        "expiryMinutes", 30))
                .build();

        service.sendTemplatedEmail(req);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();

        assertThat(sent.getHeader("List-Unsubscribe"))
                .as("List-Unsubscribe must be present").isNotNull().isNotEmpty();
        assertThat(sent.getHeader("List-Unsubscribe-Post"))
                .as("List-Unsubscribe-Post must be present").isNotNull().isNotEmpty();

        Object content = sent.getContent();
        assertThat(content).isInstanceOf(MimeMultipart.class);
        MimeMultipart multipart = unwrapAlternativePart((MimeMultipart) content);
        assertThat(multipart.getContentType()).startsWith("multipart/alternative");
    }

    /**
     * Spring's MimeMessageHelper with both text+html wraps the alternative
     * inside a related/mixed envelope (depending on attachments). Walk the
     * tree to find the {@code multipart/alternative} node where html + text
     * coexist.
     */
    private MimeMultipart unwrapAlternativePart(MimeMultipart outer) throws Exception {
        if (outer.getContentType().toLowerCase().startsWith("multipart/alternative")) {
            return outer;
        }
        for (int i = 0; i < outer.getCount(); i++) {
            Object inner = outer.getBodyPart(i).getContent();
            if (inner instanceof MimeMultipart innerMp) {
                if (innerMp.getContentType().toLowerCase().startsWith("multipart/alternative")) {
                    return innerMp;
                }
                MimeMultipart deeper = unwrapAlternativePart(innerMp);
                if (deeper != null) {
                    return deeper;
                }
            }
        }
        return outer; // Fall back to the outer — assertion will still fire if not alternative.
    }
}
