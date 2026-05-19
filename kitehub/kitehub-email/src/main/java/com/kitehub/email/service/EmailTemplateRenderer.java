package com.kitehub.email.service;

import com.kitehub.email.api.Tone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders Thymeleaf email templates to BOTH HTML and plain-text bodies
 * (GAP-657 §Step 1 + GAP-659 §Step 2 — Wave 98 Bucket B1).
 *
 * <p>Each template under {@code src/main/resources/templates/emails/} has a
 * {@code .html} sibling AND a {@code .txt} sibling. Senders MUST emit both
 * bodies (per RFC 2046 multipart/alternative) so HTML-stripping mail clients
 * (Gmail Promotions tab, Outlook plain mode) display readable plain-text
 * fallback. Without this, projected ~20% silent churn for Resend-delivered
 * mail to Gmail per external benchmark.</p>
 *
 * <p><b>Tone resolution (Wave 98 simplification per GAP-659 §Step 4):</b>
 * tone is computed from recipient role via {@link Tone#fromRole(String)} but
 * ALL Wave 98 templates render the {@link Tone#FORMAL_SAFE_DEFAULT} variant
 * (single safe template per type). Wave 99+ will introduce per-tone variant
 * templates (e.g., {@code welcome.formal.html} / {@code welcome.informal.html})
 * and this renderer will dispatch by tone — see {@code TODO Wave 99} marker
 * in {@link #resolveTemplatePath(String, Tone)}.</p>
 *
 * @since Wave 98 Bucket B1 (GAP-657 + GAP-659)
 */
@Slf4j
@Component
public class EmailTemplateRenderer {

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

        String html = templateEngine.process(htmlPath, ctx);
        String text = renderPlainTextSibling(templateName, ctx);

        return new RenderedBodies(html, text);
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
     * Resolve the HTML template path for the given tone.
     *
     * <p>Wave 98 simplification: all tones resolve to the base template
     * ({@code emails/{name}}). Wave 99+ will switch to variant suffix
     * ({@code emails/{name}.formal} / {@code emails/{name}.informal} / etc.).</p>
     */
    private String resolveTemplatePath(String templateName, Tone tone) {
        // TODO Wave 99: per-tone variant templates per GAP-659 §Step 2.
        //   Map tone -> suffix and resolve "emails/{name}.{suffix}" with
        //   fallback to "emails/{name}" when variant is absent.
        return "emails/" + templateName;
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
