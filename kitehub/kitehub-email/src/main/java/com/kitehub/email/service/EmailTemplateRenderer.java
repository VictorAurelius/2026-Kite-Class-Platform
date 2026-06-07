package com.kitehub.email.service;

import com.kitehub.email.api.Tone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders Thymeleaf email templates to BOTH HTML and plain-text bodies
 * (GAP-657 §Step 1 + GAP-659 §Step 2 — Wave 98 Bucket B1 / Wave 107 final 20%).
 *
 * <p>Each template under {@code src/main/resources/templates/emails/} has a
 * {@code .html} sibling AND a {@code .txt} sibling. Senders MUST emit both
 * bodies (per RFC 2046 multipart/alternative) so HTML-stripping mail clients
 * (Gmail Promotions tab, Outlook plain mode) display readable plain-text
 * fallback. Without this, projected ~20% silent churn for Resend-delivered
 * mail to Gmail per external benchmark.</p>
 *
 * <p><b>Tone resolution (Wave 107 final — per GAP-659 §Step 2):</b>
 * {@link #resolveTemplatePath(String, Tone)} now dispatches to per-tone variant
 * templates ({@code emails/{name}.formal.html} / {@code emails/{name}.informal.html}
 * / {@code emails/{name}.semi-formal.html}) with automatic fallback to the base
 * template ({@code emails/{name}}) when the variant is absent. This enables
 * progressive roll-out: templates opt-in to per-tone treatment by adding variant
 * siblings; remaining templates continue using the FORMAL_SAFE_DEFAULT base.</p>
 *
 * <p>Tone suffix map (Wave 107):</p>
 * <ul>
 *   <li>{@link Tone#FORMAL_AUTHORITY} → {@code .formal}</li>
 *   <li>{@link Tone#SEMI_FORMAL_PEER} → {@code .semi-formal}</li>
 *   <li>{@link Tone#INFORMAL_FRIEND} → {@code .informal}</li>
 *   <li>{@link Tone#FORMAL_SAFE_DEFAULT} → base template (no suffix)</li>
 * </ul>
 *
 * @since Wave 98 Bucket B1 (GAP-657 + GAP-659); extended Wave 107 (GAP-659 final 20%)
 */
@Slf4j
@Component
public class EmailTemplateRenderer {

    /**
     * Map from {@link Tone} to the variant filename suffix used when selecting
     * a per-tone template (e.g. {@code .formal} → {@code welcome.formal.html}).
     * {@link Tone#FORMAL_SAFE_DEFAULT} has no suffix — it uses the base template.
     */
    private static final Map<Tone, String> TONE_SUFFIX;

    static {
        Map<Tone, String> m = new EnumMap<>(Tone.class);
        m.put(Tone.FORMAL_AUTHORITY, ".formal");
        m.put(Tone.SEMI_FORMAL_PEER, ".semi-formal");
        m.put(Tone.INFORMAL_FRIEND, ".informal");
        // FORMAL_SAFE_DEFAULT → base template (no suffix); not inserted → absent key = base
        TONE_SUFFIX = m;
    }

    private final TemplateEngine templateEngine;

    public EmailTemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Render both HTML and plain-text bodies for the given template + variables.
     *
     * <p>If the {@code .txt} sibling is missing, returns an empty plain-text body
     * + logs a warning. The HTML body is still rendered so the send path can
     * fall back to HTML-only (GAP-657 transitional period when not all 20+
     * templates have plain-text siblings yet).</p>
     *
     * @param templateName template filename stem (e.g. {@code welcome},
     *                     {@code beta-invite}) — without extension
     * @param variables    Thymeleaf context variables (may be null)
     * @param tone         recipient tone (Wave 98 = always
     *                     {@link Tone#FORMAL_SAFE_DEFAULT} effectively);
     *                     accepts null = defaults to safe
     * @return rendered bodies bundle (HTML always present; text may be empty)
     */
    public RenderedBodies render(String templateName, Map<String, Object> variables, Tone tone) {
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("templateName must not be null or blank");
        }

        Tone effectiveTone = tone != null ? tone : Tone.FORMAL_SAFE_DEFAULT;
        String htmlPath = resolveTemplatePath(templateName, effectiveTone);

        Context ctx = buildContext(variables, effectiveTone);

        String html = renderHtmlWithFallback(templateName, htmlPath, ctx);
        String text = renderPlainTextSibling(templateName, ctx);

        return new RenderedBodies(html, text);
    }

    /**
     * Render HTML body, falling back to the base (no-suffix) template when the
     * per-tone variant path does not exist. This implements the progressive opt-in
     * model: variant templates are optional; absence silently falls back to base.
     *
     * <p>If the BASE template itself is missing (GAP-1053), this method degrades
     * gracefully — it logs at WARN and returns a minimal inline fallback body
     * rather than letting {@link TemplateInputException} escape. An unhandled
     * exception here would crash the RabbitMQ consumer ({@code EmailEventListener})
     * and poison the queue: a missing template is a deploy-time bug that retrying
     * cannot fix, so DLQ-looping is pointless. The WARN log keeps the missing-template
     * bug visible (NOT silently swallowed) so it can still be detected + fixed.</p>
     */
    private String renderHtmlWithFallback(String templateName, String htmlPath, Context ctx) {
        String basePath = "emails/" + templateName;
        if (htmlPath.equals(basePath)) {
            // No variant → render base directly (common path)
            return processBaseOrMinimalFallback(templateName, basePath, ctx);
        }
        try {
            return templateEngine.process(htmlPath, ctx);
        } catch (TemplateInputException ex) {
            log.debug("Per-tone variant '{}' not found; falling back to base template '{}'",
                    htmlPath, basePath);
            return processBaseOrMinimalFallback(templateName, basePath, ctx);
        }
    }

    /**
     * Render the base template, degrading to a minimal inline fallback body when
     * the base template resource is genuinely missing (GAP-1053). A missing base
     * template MUST NOT escape as an unhandled exception that crashes the email
     * consumer — instead WARN-log (so the bug stays visible) + emit a minimal,
     * clearly-marked HTML body so the message can still be delivered.
     */
    private String processBaseOrMinimalFallback(String templateName, String basePath, Context ctx) {
        try {
            return templateEngine.process(basePath, ctx);
        } catch (TemplateInputException ex) {
            log.warn("Email base template '{}' is missing — emitting minimal fallback body. "
                    + "Fix: add src/main/resources/templates/{}.html", basePath, basePath);
            return minimalFallbackHtml(templateName);
        }
    }

    /**
     * Minimal, generic HTML body used when an email template is missing entirely.
     * Intentionally generic (no template-specific copy) — its only job is to keep
     * the send path alive instead of crashing the consumer. The HTML comment marks
     * the degradation so it is identifiable in delivered mail / test assertions.
     */
    private String minimalFallbackHtml(String templateName) {
        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"></head><body>"
                + "<!-- minimal fallback: template '" + templateName + "' missing (GAP-1053) -->"
                + "<p>Xin chào,</p>"
                + "<p>Bạn vừa nhận được một thông báo từ KiteClass. "
                + "Vui lòng đăng nhập vào hệ thống để xem chi tiết.</p>"
                + "<p>Trân trọng,<br/>Đội ngũ KiteClass</p>"
                + "</body></html>";
    }

    /**
     * Render plain-text sibling. Returns empty string if the {@code .txt}
     * resource is missing (transitional — Wave 98 ships 5 critical templates
     * with siblings; remaining ~15 templates fall back to empty until follow-up).
     */
    private String renderPlainTextSibling(String templateName, Context ctx) {
        // Thymeleaf has multiple resolvers; the TEXT resolver picks up `.txt`
        // resources. We invoke by full filename including extension so the
        // configured TemplateResolver chain picks the correct one.
        String textTemplate = "emails/" + templateName + ".txt";
        try {
            return templateEngine.process(textTemplate, ctx);
        } catch (TemplateInputException ex) {
            log.debug("Plain-text sibling missing for template '{}' (HTML-only send will be used)",
                    templateName);
            return "";
        } catch (Exception ex) {
            log.warn("Failed to render plain-text sibling for '{}': {}",
                    templateName, ex.getMessage());
            return "";
        }
    }

    /**
     * Resolve the HTML template path for the given tone (Wave 107 — GAP-659 §Step 2).
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>If tone has a suffix (see {@link #TONE_SUFFIX}), try
     *       {@code emails/{name}{suffix}} (e.g. {@code emails/welcome.formal}).</li>
     *   <li>If the variant template does not exist
     *       ({@link TemplateInputException} / {@link IllegalArgumentException}),
     *       fall back to the base template {@code emails/{name}}.</li>
     *   <li>{@link Tone#FORMAL_SAFE_DEFAULT} always resolves to base (no suffix).</li>
     * </ol>
     *
     * <p>This allows incremental opt-in: templates ship variant siblings progressively.
     * Templates that have not yet received per-tone variants continue using the
     * safe-default base without any change at the call-site.</p>
     *
     * @param templateName template stem (e.g. {@code welcome}, {@code invite-staff})
     * @param tone         effective tone (never null at call-site)
     * @return Thymeleaf template path to pass to {@link TemplateEngine#process}
     */
    String resolveTemplatePath(String templateName, Tone tone) {
        String suffix = TONE_SUFFIX.get(tone);
        if (suffix == null) {
            // FORMAL_SAFE_DEFAULT or any unregistered tone → base template (no variant suffix)
            return "emails/" + templateName;
        }
        // Return the per-tone variant path. If the variant template resource does not
        // exist on the classpath, renderHtmlWithFallback() will catch the resulting
        // TemplateInputException and transparently fall back to the base template.
        return "emails/" + templateName + suffix;
    }

    private Context buildContext(Map<String, Object> variables, Tone tone) {
        Context ctx = new Context();
        Map<String, Object> merged = variables != null ? new HashMap<>(variables) : new HashMap<>();
        merged.putIfAbsent("tone", tone.name());
        ctx.setVariables(merged);
        return ctx;
    }

    /**
     * Bundle returned by {@link #render(String, Map, Tone)}.
     */
    public static final class RenderedBodies {
        private final String html;
        private final String text;

        public RenderedBodies(String html, String text) {
            this.html = html;
            this.text = text;
        }

        public String getHtml() {
            return html;
        }

        public String getText() {
            return text;
        }

        public boolean hasText() {
            return text != null && !text.isBlank();
        }
    }
}
