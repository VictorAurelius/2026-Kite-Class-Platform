package com.kitehub.email.template;

import com.kitehub.email.dto.TenantBranding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thymeleaf rendering tests for the {@code admin-new-login-alert} email template
 * introduced in Wave 91 Bucket C (GAP-606). Verifies producer-side variables
 * ({@code ip}, {@code userAgent}, {@code loginAt}, {@code supportUrl} —
 * see {@code EmailServiceClient#sendAdminNewLoginAlert}) substitute correctly
 * and the security-alert UX elements (Vietnamese narrative, alert badge,
 * action box, OWASP A07 disclaimer) render without {@code TemplateInputException}.
 *
 * <p>Closes GAP-606 root cause: template MISSING in source → kitehub-email
 * HTTP 500 → RMQ consumer infinite retry (~864K wasted messages over 24h
 * before Wave 91 cutover).</p>
 */
@DisplayName("admin-new-login-alert email template rendering")
class AdminNewLoginAlertTemplateTest {

    private final TemplateEngine engine = buildEngine();

    private static TemplateEngine buildEngine() {
        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.refresh();
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(ctx);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine eng = new SpringTemplateEngine();
        eng.setTemplateResolver(resolver);
        return eng;
    }

    private Context buildContext(Map<String, Object> vars) {
        Context ctx = new Context();
        ctx.setVariables(vars);
        ctx.setVariable("branding", TenantBranding.defaultBranding());
        return ctx;
    }

    @Test
    @DisplayName("renders all producer variables (ip, userAgent, loginAt, supportUrl) without TemplateInputException")
    void adminNewLoginAlertRendersAllVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("ip", "203.0.113.7");
        vars.put("userAgent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        vars.put("loginAt", "2026-05-18T09:30:00");
        vars.put("supportUrl", "https://kitehub.me/support");

        String html = engine.process("emails/admin-new-login-alert", buildContext(vars));

        // Producer variables substituted (closes GAP-606 root cause)
        assertThat(html).contains("203.0.113.7");
        assertThat(html).contains("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
        assertThat(html).contains("2026-05-18T09:30:00");
        assertThat(html).contains("https://kitehub.me/support");
    }

    @Test
    @DisplayName("renders Vietnamese narrative + alert UX elements (badge, action box, OWASP disclaimer)")
    void adminNewLoginAlertRendersVietnameseAlertElements() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("ip", "10.0.0.1");
        vars.put("userAgent", "curl/7.88");
        vars.put("loginAt", "2026-05-18 09:30:00");
        vars.put("supportUrl", "https://kitehub.me/support");

        String html = engine.process("emails/admin-new-login-alert", buildContext(vars));

        // Vietnamese narrative per dev-readable-doc-language.md
        assertThat(html).contains("Đăng nhập mới vào tài khoản Admin");
        assertThat(html).contains("Xin chào Quản trị viên");
        assertThat(html).contains("Chi tiết phiên đăng nhập");

        // Alert UX
        assertThat(html).contains("Security Alert");
        assertThat(html).contains("Liên hệ hỗ trợ ngay"); // CTA

        // OWASP A07 disclaimer reference
        assertThat(html).contains("OWASP A07");

        // Default branding fallback applied
        assertThat(html).containsIgnoringCase("KiteHub");
    }

    @Test
    @DisplayName("falls back gracefully when nullable variables (ip, userAgent) absent")
    void adminNewLoginAlertFallsBackOnMissingVariables() {
        Map<String, Object> vars = new HashMap<>();
        // Intentionally omit ip + userAgent — producer passes "unknown" string,
        // but template MUST also self-defend via Thymeleaf Elvis (?:) when var absent
        vars.put("loginAt", "2026-05-18 09:30:00");
        vars.put("supportUrl", "https://kitehub.me/support");

        // Should NOT throw TemplateInputException
        String html = engine.process("emails/admin-new-login-alert", buildContext(vars));

        // Elvis fallback rendered in cells where var absent
        assertThat(html).contains("unknown");
        assertThat(html).contains("2026-05-18 09:30:00");
    }

    @Test
    @DisplayName("supportUrl falls back to kitehub.me/support when absent")
    void adminNewLoginAlertSupportUrlFallback() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("ip", "203.0.113.7");
        vars.put("userAgent", "Mozilla/5.0");
        vars.put("loginAt", "2026-05-18 09:30:00");
        // Intentionally omit supportUrl

        String html = engine.process("emails/admin-new-login-alert", buildContext(vars));

        assertThat(html).contains("https://kitehub.me/support");
    }
}
