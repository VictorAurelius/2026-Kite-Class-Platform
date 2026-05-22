package com.kitehub.email.service;

import com.kitehub.email.config.SESConfig;
import com.kitehub.email.dto.EmailRequest;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-703 Wave 104 Bucket B2 regression coverage for email-layer hardening:
 *
 * <ul>
 *   <li>RFC 8058 {@code List-Unsubscribe} + {@code List-Unsubscribe-Post}
 *       headers applied on every outbound MimeMessage (Gmail bulk-sender
 *       deliverability requirement).</li>
 *   <li>{@code multipart/alternative} body with BOTH text/html + text/plain
 *       parts whenever the template has a {@code .txt} sibling
 *       (RFC 2046 multipart fallback for HTML-stripping clients).</li>
 * </ul>
 *
 * <p>Wave 103 verify (2026-05-22) found live emails missing both headers AND
 * lacking the plain-text part — Content-Type was {@code multipart/mixed} not
 * {@code multipart/alternative} and zero {@code List-Unsubscribe*} headers.
 * This test locks the corrected behaviour.</p>
 *
 * <p>Strategy: mock {@link JavaMailSender#createMimeMessage()} to return a real
 * {@link MimeMessage} built from a no-op Session; capture the message via
 * {@code verify(...).send(captor.capture())} after exercising
 * {@link SESEmailService#sendTemplatedEmail}; introspect headers + body.</p>
 */
@DisplayName("Email hardening — List-Unsubscribe + multipart/alternative (GAP-703)")
class EmailHardeningTest {

    private SESConfig.SESProperties sesProperties;
    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;
    private SESEmailService service;

    @BeforeEach
    void setUp() {
        // Real Thymeleaf engine pointing at packaged email templates so the
        // .html + .txt siblings render exactly as production does.
        ClassLoaderTemplateResolver htmlResolver = new ClassLoaderTemplateResolver();
        htmlResolver.setPrefix("templates/");
        htmlResolver.setSuffix(".html");
        htmlResolver.setTemplateMode(TemplateMode.HTML);
        htmlResolver.setCharacterEncoding("UTF-8");
        htmlResolver.setCheckExistence(true);

        ClassLoaderTemplateResolver textResolver = new ClassLoaderTemplateResolver();
        textResolver.setPrefix("templates/");
        textResolver.setSuffix("");
        textResolver.setTemplateMode(TemplateMode.TEXT);
        textResolver.setCharacterEncoding("UTF-8");
        textResolver.setCheckExistence(true);

        templateEngine = new TemplateEngine();
        templateEngine.addTemplateResolver(htmlResolver);
        templateEngine.addTemplateResolver(textResolver);

        sesProperties = new SESConfig.SESProperties();
        sesProperties.setFromEmail("noreply@kitehub.me");
        sesProperties.setFromName("KiteHub");
        sesProperties.setReplyToEmail("support@kitehub.me");
        // Defaults from SESConfig — explicit here to insulate from future config drift.
        sesProperties.setUnsubscribeMailto("unsubscribe@kitehub.me");
        sesProperties.setUnsubscribeUrlTemplate("https://kitehub.me/unsubscribe?token={token}");

        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));

        service = new SESEmailService(sesProperties, /* ses */ null, mailSender, templateEngine,
                /* branding */ null, new EmailTemplateRenderer(templateEngine));
        ReflectionTestUtils.setField(service, "emailProvider", "smtp");
        ReflectionTestUtils.setField(service, "brandingEnabled", false);
    }

    @Test
    @DisplayName("shouldIncludeListUnsubscribeAndPlainText — welcome template emits both headers + multipart/alternative")
    void shouldIncludeListUnsubscribeAndPlainText() throws Exception {
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

        // List-Unsubscribe headers (RFC 8058) — Gmail bulk-sender mandate.
        String[] listUnsub = sent.getHeader("List-Unsubscribe");
        assertThat(listUnsub).isNotNull().isNotEmpty();
        assertThat(listUnsub[0])
                .contains("mailto:unsubscribe@kitehub.me")
                .contains("https://kitehub.me/unsubscribe");

        String[] listUnsubPost = sent.getHeader("List-Unsubscribe-Post");
        assertThat(listUnsubPost).isNotNull().isNotEmpty();
        assertThat(listUnsubPost[0]).isEqualTo("List-Unsubscribe=One-Click");

        // Reply-To preserved (GAP-657 §Step 3).
        String[] replyTo = sent.getHeader("Reply-To");
        assertThat(replyTo).isNotNull().isNotEmpty();
        assertThat(replyTo[0]).contains("support@kitehub.me");

        // multipart/alternative body with both text/html + text/plain parts.
        Object content = sent.getContent();
        assertThat(content).isInstanceOf(MimeMultipart.class);
        MimeMultipart multipart = unwrapAlternativePart((MimeMultipart) content);
        assertThat(multipart.getContentType()).startsWith("multipart/alternative");

        boolean hasPlain = false;
        boolean hasHtml = false;
        for (int i = 0; i < multipart.getCount(); i++) {
            String ct = multipart.getBodyPart(i).getContentType();
            if (ct.toLowerCase().startsWith("text/plain")) {
                hasPlain = true;
            } else if (ct.toLowerCase().startsWith("text/html")) {
                hasHtml = true;
            }
        }
        assertThat(hasPlain).as("multipart/alternative should contain text/plain part").isTrue();
        assertThat(hasHtml).as("multipart/alternative should contain text/html part").isTrue();
    }

    @Test
    @DisplayName("password-reset template (5-type sample) also includes both headers + plain-text part")
    void passwordResetTemplateAlsoHardened() throws Exception {
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

        assertThat(sent.getHeader("List-Unsubscribe")).isNotNull().isNotEmpty();
        assertThat(sent.getHeader("List-Unsubscribe-Post")).isNotNull().isNotEmpty();
        assertThat(((MimeMultipart) unwrapAlternativePart((MimeMultipart) sent.getContent()))
                .getContentType()).startsWith("multipart/alternative");
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
