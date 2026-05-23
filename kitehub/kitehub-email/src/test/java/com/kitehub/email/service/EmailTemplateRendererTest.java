package com.kitehub.email.service;

import com.kitehub.email.api.Tone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates {@link EmailTemplateRenderer} renders BOTH HTML and plain-text
 * bodies for the 5 critical templates per GAP-657 §Step 1 (Wave 98 Bucket B1).
 *
 * <p>Uses a minimal Thymeleaf TemplateEngine wiring (HTML + TEXT resolvers)
 * — no Spring context required.</p>
 */
class EmailTemplateRendererTest {

    private EmailTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine engine = new SpringTemplateEngine();

        ClassLoaderTemplateResolver html = new ClassLoaderTemplateResolver();
        html.setPrefix("templates/");
        html.setSuffix(".html");
        html.setTemplateMode(TemplateMode.HTML);
        html.setCharacterEncoding("UTF-8");
        html.setCacheable(false);
        html.setOrder(10);
        html.setCheckExistence(true);
        engine.addTemplateResolver(html);

        ClassLoaderTemplateResolver text = new ClassLoaderTemplateResolver();
        text.setPrefix("templates/");
        text.setSuffix(".txt");
        text.setTemplateMode(TemplateMode.TEXT);
        text.setCharacterEncoding("UTF-8");
        text.setCacheable(false);
        text.setOrder(50);
        text.setCheckExistence(true);
        engine.addTemplateResolver(text);

        renderer = new EmailTemplateRenderer(engine);
    }

    @Test
    void rendersHtmlAndText_forWelcome() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Nguyễn Thị Mai");
        vars.put("loginUrl", "https://kitehub.me/login");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("welcome", vars, Tone.FORMAL_SAFE_DEFAULT);

        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.getHtml()).containsIgnoringCase("KiteHub");
        assertThat(bodies.hasText()).isTrue();
        // GAP-543 Wave 107: welcome.txt switched greeting register to "Em chào anh/chị"
        // (warm-formal, matches P2 Center Owner persona). Per `vn-localization-audit-checklist.md`
        // §2 row "Email body greeting".
        assertThat(bodies.getText()).contains("Em chào anh/chị");
        assertThat(bodies.getText()).contains("Nguyễn Thị Mai");
    }

    @Test
    void rendersHtmlAndText_forBetaInvite() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orgName", "Trung tâm Sky Education");
        vars.put("inviteUrl", "https://kitehub.me/beta/accept");
        vars.put("verificationCode", "382041");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("beta-invite", vars, null);

        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.hasText()).isTrue();
        assertThat(bodies.getText()).contains("382041");
    }

    @Test
    void rendersHtmlAndText_forPasswordReset() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Trần Văn An");
        vars.put("resetUrl", "https://kitehub.me/password-reset?token=abc");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("password-reset", vars, Tone.FORMAL_SAFE_DEFAULT);

        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.hasText()).isTrue();
        assertThat(bodies.getText()).contains("Trần Văn An");
        assertThat(bodies.getText()).contains("đặt lại mật khẩu");
    }

    @Test
    void rendersHtmlAndText_forEmailVerification() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Phạm Văn Bình");
        vars.put("verificationCode", "184205");
        vars.put("verifyUrl", "https://kitehub.me/verify?token=abc");
        vars.put("expiresInMinutes", 15);

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("email-verification", vars, Tone.FORMAL_SAFE_DEFAULT);

        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.hasText()).isTrue();
        // GAP-543 Wave 107: email-verification template uses verifyUrl link
        // (NOT OTP code display) — anti-phishing convention per audit log §3.
        // verificationCode var still passed for backward compat; not rendered.
        assertThat(bodies.getText()).contains("https://kitehub.me/verify");
        assertThat(bodies.getText()).contains("Phạm Văn Bình");
    }

    @Test
    void toneFromRole_resolvesCorrectly() {
        assertThat(Tone.fromRole("PLATFORM_ADMIN")).isEqualTo(Tone.FORMAL_AUTHORITY);
        assertThat(Tone.fromRole("CENTER_OWNER")).isEqualTo(Tone.FORMAL_AUTHORITY);
        assertThat(Tone.fromRole("center_manager")).isEqualTo(Tone.SEMI_FORMAL_PEER);
        assertThat(Tone.fromRole("TEACHER")).isEqualTo(Tone.INFORMAL_FRIEND);
        assertThat(Tone.fromRole(null)).isEqualTo(Tone.FORMAL_SAFE_DEFAULT);
        assertThat(Tone.fromRole("UNKNOWN_ROLE")).isEqualTo(Tone.FORMAL_SAFE_DEFAULT);
        assertThat(Tone.fromRole("")).isEqualTo(Tone.FORMAL_SAFE_DEFAULT);
    }
}
