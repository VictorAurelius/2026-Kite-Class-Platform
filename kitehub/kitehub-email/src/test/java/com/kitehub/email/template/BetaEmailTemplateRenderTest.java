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
 * Thymeleaf rendering tests for the beta email templates introduced in Wave 33
 * Bucket B (GAP-370) — verifies variables substitute correctly and required
 * elements (CTA, disclaimer, branding fields) are present.
 */
@DisplayName("Beta email template rendering")
class BetaEmailTemplateRenderTest {

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
    @DisplayName("beta-invite renders orgName, claimCode, inviteUrl, expiryDate + CTA + disclaimer (GAP-388 388-B)")
    void betaInviteRenders() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orgName", "Trung tâm Anh ngữ Sao Mai");
        vars.put("claimCode", "428193");
        // inviteUrl now points to the signup page (no UUID in href per GAP-388 388-B).
        vars.put("inviteUrl", "https://kitehub.vn/auth/beta-signup");
        // GAP-797: canonical expiry var is `expiresAt` (matches sender + .txt sibling);
        // beta-invite.html previously read `expiryDate` → silent fallback drift.
        vars.put("expiresAt", "2026-05-31");

        String html = engine.process("emails/beta-invite", buildContext(vars));

        // Variables substituted
        assertThat(html).contains("Trung tâm Anh ngữ Sao Mai");
        assertThat(html).contains("428193"); // GAP-388 388-B: claim code, NOT raw UUID
        assertThat(html).contains("https://kitehub.vn/auth/beta-signup");
        assertThat(html).contains("2026-05-31");

        // Required UX elements
        assertThat(html).contains("Mở trang đăng ký"); // CTA — links to signup, not direct token URL
        assertThat(html).containsIgnoringCase("beta"); // beta disclaimer
        assertThat(html).contains("v1 pending counsel review"); // CLAUDE.md decision context disclaimer
        // GAP-388 388-B regression guard: raw UUID-shaped tokens MUST NOT appear in the body.
        assertThat(html).doesNotContain("abc-123-def-456");
    }

    @Test
    @DisplayName("beta-request-confirmation renders orgName, expectedReviewTime + uses default branding")
    void betaRequestConfirmationRenders() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orgName", "Trường Mầm non Hoa Hồng");
        vars.put("expectedReviewTime", "3-5 ngày làm việc");
        vars.put("persona", "Mầm non / mẫu giáo");
        vars.put("requestedAt", "2026-05-07 10:30");

        String html = engine.process("emails/beta-request-confirmation", buildContext(vars));

        assertThat(html).contains("Trường Mầm non Hoa Hồng");
        assertThat(html).contains("3-5 ngày làm việc");
        assertThat(html).contains("Đã nhận yêu cầu beta");
        assertThat(html).contains("v1 pending counsel review");
        // Default branding fallback applied
        assertThat(html).containsIgnoringCase("KiteClass");
    }

    @Test
    @DisplayName("beta-request-confirmation falls back to default review time when variable absent")
    void betaRequestConfirmationFallsBackOnReviewTime() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orgName", "Org Test");
        // Intentionally omit expectedReviewTime — template should fall back

        String html = engine.process("emails/beta-request-confirmation", buildContext(vars));

        assertThat(html).contains("3-5 ngày làm việc"); // default fallback in template
    }
}
