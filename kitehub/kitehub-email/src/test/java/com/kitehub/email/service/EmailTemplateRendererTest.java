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
 * bodies for the 5 critical templates per GAP-657 §Step 1 (Wave 98 Bucket B1),
 * AND validates per-tone variant dispatch + fallback per GAP-659 §Step 2
 * (Wave 107 final 20%).
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
        assertThat(bodies.getText()).contains("Kính gửi anh/chị");
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
        assertThat(bodies.getText()).contains("184205");
        assertThat(bodies.getText()).contains("Phạm Văn Bình");
    }

    @Test
    void toneFromRole_resolvesCorrectly() {
        assertThat(Tone.fromRole("PLATFORM_ADMIN")).isEqualTo(Tone.FORMAL_AUTHORITY);
        assertThat(Tone.fromRole("CENTER_OWNER")).isEqualTo(Tone.FORMAL_AUTHORITY);
        assertThat(Tone.fromRole("P2_CENTER_OWNER")).isEqualTo(Tone.FORMAL_AUTHORITY);
        assertThat(Tone.fromRole("center_manager")).isEqualTo(Tone.SEMI_FORMAL_PEER);
        assertThat(Tone.fromRole("P3_CENTER_MANAGER")).isEqualTo(Tone.SEMI_FORMAL_PEER);
        assertThat(Tone.fromRole("TEACHER")).isEqualTo(Tone.INFORMAL_FRIEND);
        assertThat(Tone.fromRole("P1_SOLO_TEACHER")).isEqualTo(Tone.INFORMAL_FRIEND);
        assertThat(Tone.fromRole("SOLO_TEACHER")).isEqualTo(Tone.INFORMAL_FRIEND);
        assertThat(Tone.fromRole(null)).isEqualTo(Tone.FORMAL_SAFE_DEFAULT);
        assertThat(Tone.fromRole("UNKNOWN_ROLE")).isEqualTo(Tone.FORMAL_SAFE_DEFAULT);
        assertThat(Tone.fromRole("")).isEqualTo(Tone.FORMAL_SAFE_DEFAULT);
    }

    // -------------------------------------------------------------------------
    // Wave 107 — Per-tone variant dispatch + fallback tests (GAP-659 §Step 2)
    // -------------------------------------------------------------------------

    @Test
    void resolveTemplatePath_formalAuthority_returnsFormalSuffix() {
        String path = renderer.resolveTemplatePath("welcome", Tone.FORMAL_AUTHORITY);
        assertThat(path).isEqualTo("emails/welcome.formal");
    }

    @Test
    void resolveTemplatePath_informalFriend_returnsInformalSuffix() {
        String path = renderer.resolveTemplatePath("welcome", Tone.INFORMAL_FRIEND);
        assertThat(path).isEqualTo("emails/welcome.informal");
    }

    @Test
    void resolveTemplatePath_semiFormalPeer_returnsSemiFormalSuffix() {
        String path = renderer.resolveTemplatePath("welcome", Tone.SEMI_FORMAL_PEER);
        assertThat(path).isEqualTo("emails/welcome.semi-formal");
    }

    @Test
    void resolveTemplatePath_formalSafeDefault_returnsBasePath() {
        String path = renderer.resolveTemplatePath("welcome", Tone.FORMAL_SAFE_DEFAULT);
        assertThat(path).isEqualTo("emails/welcome");
    }

    @Test
    void render_formalAuthority_welcome_usesVariantTemplate() {
        // welcome.formal.html exists → should render variant
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Nguyễn Thị Hằng");
        vars.put("loginUrl", "https://kitehub.me/login");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("welcome", vars, Tone.FORMAL_AUTHORITY);

        assertThat(bodies.getHtml()).isNotBlank();
        // Formal variant uses "Kính gửi chị/anh" salutation
        assertThat(bodies.getHtml()).contains("Kính gửi");
        assertThat(bodies.getHtml()).contains("Nguyễn Thị Hằng");
        // Formal variant uses "Trân trọng kính chào" closing
        assertThat(bodies.getHtml()).contains("Trân trọng kính chào");
    }

    @Test
    void render_informalFriend_welcome_usesVariantTemplate() {
        // welcome.informal.html exists → should render variant
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Trần Thị Linh");
        vars.put("loginUrl", "https://kitehub.me/login");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("welcome", vars, Tone.INFORMAL_FRIEND);

        assertThat(bodies.getHtml()).isNotBlank();
        // Informal variant uses "Chào {Name}" salutation
        assertThat(bodies.getHtml()).contains("Chào");
        assertThat(bodies.getHtml()).contains("Trần Thị Linh");
        // Informal variant uses friendly emoji
        assertThat(bodies.getHtml()).contains("🚀");
    }

    @Test
    void render_semiFormalPeer_welcome_fallsBackToBase() {
        // welcome.semi-formal.html does NOT exist → should fall back to base welcome.html
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Lê Văn Tâm");
        vars.put("loginUrl", "https://kitehub.me/login");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("welcome", vars, Tone.SEMI_FORMAL_PEER);

        // Fallback to base → still renders HTML without throwing
        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.getHtml()).containsIgnoringCase("KiteHub");
    }

    @Test
    void render_formalAuthority_inviteStaff_usesVariantTemplate() {
        // invite-staff.formal.html exists → render variant
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Nguyễn Thị Hằng");
        vars.put("ownerName", "Phạm Thị Mai");
        vars.put("tenantName", "Trung tâm Anh ngữ Sky Education");
        vars.put("role", "QUẢN LÝ");
        vars.put("inviteUrl", "https://kitehub.me/staff/accept-invite?token=abc123");
        vars.put("expiresAt", "Thứ Sáu, 30/05/2026");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("invite-staff", vars, Tone.FORMAL_AUTHORITY);

        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.getHtml()).contains("Kính gửi");
        assertThat(bodies.getHtml()).contains("Nguyễn Thị Hằng");
        assertThat(bodies.getHtml()).contains("Sky Education");
        assertThat(bodies.getHtml()).contains("Trân trọng kính chào");
    }

    @Test
    void render_informalFriend_inviteStaff_usesVariantTemplate() {
        // invite-staff.informal.html exists → render variant
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Trần Thị Linh");
        vars.put("ownerName", "Nguyễn Thị Hằng");
        vars.put("tenantName", "Trung tâm Anh ngữ Sky Education");
        vars.put("role", "GIÁO VIÊN");
        vars.put("inviteUrl", "https://kitehub.me/staff/accept-invite?token=xyz789");
        vars.put("expiresAt", "30/05/2026");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("invite-staff", vars, Tone.INFORMAL_FRIEND);

        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.getHtml()).contains("Chào");
        assertThat(bodies.getHtml()).contains("Trần Thị Linh");
        assertThat(bodies.getHtml()).contains("🎉");
    }

    @Test
    void render_nullTone_fallsBackToFormalSafeDefault() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Nguyễn Văn An");
        vars.put("loginUrl", "https://kitehub.me/login");

        // null tone → FORMAL_SAFE_DEFAULT → base template
        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("welcome", vars, null);

        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.getHtml()).containsIgnoringCase("KiteHub");
    }

    @Test
    void render_unknownTemplate_withVariantTone_fallsBackGracefully() {
        // Template "nonexistent.formal.html" doesn't exist AND base "nonexistent.html" missing
        // → TemplateInputException propagates from base (no silent swallow for base missing)
        Map<String, Object> vars = new HashMap<>();
        org.junit.jupiter.api.Assertions.assertThrows(
                org.thymeleaf.exceptions.TemplateInputException.class,
                () -> renderer.render("nonexistent-template", vars, Tone.FORMAL_AUTHORITY)
        );
    }

    @Test
    void render_rendersHtmlAndText_forInviteStaffBaseline() {
        // invite-staff.txt plain-text sibling should render for baseline tone
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Trần Thị Hồng");
        vars.put("ownerName", "Nguyễn Văn An");
        vars.put("tenantName", "Trung tâm Toán Quang Minh");
        vars.put("role", "Giáo viên");
        vars.put("inviteUrl", "https://kitehub.me/staff/accept-invite?token=test");
        vars.put("expiresAt", "22/05/2026");

        EmailTemplateRenderer.RenderedBodies bodies = renderer.render("invite-staff", vars, Tone.FORMAL_SAFE_DEFAULT);

        assertThat(bodies.getHtml()).isNotBlank();
        assertThat(bodies.hasText()).isTrue();
        assertThat(bodies.getText()).contains("Trần Thị Hồng");
        assertThat(bodies.getText()).contains("Trung tâm Toán Quang Minh");
        assertThat(bodies.getText()).contains("Trân trọng");
    }
}
