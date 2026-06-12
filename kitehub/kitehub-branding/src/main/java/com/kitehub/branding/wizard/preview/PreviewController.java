package com.kitehub.branding.wizard.preview;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.BrandColours;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * Iframe-safe HTML preview for the wizard preview step.
 *
 * <p>Replaces Wave 32 v1 inline {@code data:} URI hack (sub-GAP-272j).
 * Returns a fully-rendered standalone HTML document so FE can embed via
 * {@code <iframe sandbox="allow-same-origin">}.</p>
 *
 * <p>Headers per api-contract:
 * {@code X-Frame-Options: SAMEORIGIN},
 * {@code Content-Security-Policy: ... frame-ancestors 'self'},
 * {@code Cache-Control: private, max-age=60}.</p>
 *
 * <p>v0 implementation: HTML composed inline (no Thymeleaf dependency).
 * Wave 35+ may swap in a real template engine once branding theme
 * persistence (Bucket A's V29 + sibling theme service) is wired.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding/jobs")
@RequiredArgsConstructor
@Tag(name = "Branding Wizard — Preview",
        description = "Iframe-safe HTML preview (sub-GAP-272j)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
        extraTags = {"slo", "tier-b", "controller", "branding-preview"})
public class PreviewController {

    private static final String CSP_VALUE =
            "default-src 'self'; img-src 'self' https://cdn.kitehub.me data:; "
                    + "style-src 'self' 'unsafe-inline'; frame-ancestors 'self'";

    private final BrandingJobRepository jobRepository;
    private final BrandColoursDeriver coloursDeriver;

    @GetMapping(value = "/{jobId}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPreview(@PathVariable UUID jobId) {
        Optional<BrandingJob> jobOpt = jobRepository.findById(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/html; charset=utf-8"));
        headers.set("X-Frame-Options", "SAMEORIGIN");
        headers.set("Content-Security-Policy", CSP_VALUE);
        headers.setCacheControl("private, max-age=60");

        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .headers(headers)
                    .body(renderNotFound(jobId));
        }

        BrandingJob job = jobOpt.get();
        BrandColours colours = coloursDeriver.derive(job);
        String html = renderPreview(job, colours);
        log.debug("Rendered preview for job {} ({} bytes)", jobId, html.length());
        return ResponseEntity.ok().headers(headers).body(html);
    }

    private String renderNotFound(UUID jobId) {
        return """
                <!doctype html>
                <html lang="vi">
                <head><meta charset="utf-8"><title>Preview not found</title></head>
                <body style="font-family:sans-serif;padding:24px">
                <h1>Preview unavailable</h1>
                <p>Job <code>%s</code> not found.</p>
                </body></html>
                """.formatted(escape(jobId.toString()));
    }

    private String renderPreview(BrandingJob job, BrandColours colours) {
        String orgName = escape(job.getOrganizationName() == null ? "Trường ABC" : job.getOrganizationName());
        return """
                <!doctype html>
                <html lang="vi">
                <head>
                <meta charset="utf-8">
                <title>%s — Brand Preview</title>
                <style>
                  :root {
                    --primary: %s;
                    --secondary: %s;
                    --accent: %s;
                    --neutral: %s;
                    --background: %s;
                  }
                  * { box-sizing: border-box; }
                  body {
                    margin: 0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    background: var(--background);
                    color: var(--neutral);
                  }
                  .hero {
                    background: linear-gradient(135deg, var(--primary), var(--secondary));
                    color: var(--background);
                    padding: 48px 32px;
                  }
                  .hero h1 { margin: 0 0 12px; font-size: 28px; }
                  .hero p { margin: 0; opacity: 0.9; }
                  .card {
                    background: var(--background);
                    border: 1px solid rgba(0,0,0,0.08);
                    border-radius: 8px;
                    padding: 20px;
                    margin: 24px;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.06);
                  }
                  .badge {
                    display: inline-block;
                    background: var(--accent);
                    color: var(--background);
                    padding: 4px 10px;
                    border-radius: 12px;
                    font-size: 12px;
                    font-weight: 600;
                  }
                  .btn {
                    display: inline-block;
                    background: var(--primary);
                    color: var(--background);
                    padding: 10px 18px;
                    border-radius: 6px;
                    text-decoration: none;
                    margin-top: 12px;
                  }
                </style>
                </head>
                <body>
                  <header class="hero" data-testid="preview-hero">
                    <span class="badge">Preview</span>
                    <h1>%s</h1>
                    <p>Brand identity preview rendered từ job %s</p>
                  </header>
                  <section class="card" data-testid="preview-card">
                    <h2 style="color: var(--primary);">Welcome to your branded instance</h2>
                    <p>Đây là preview tạm thời để xem brand colors trên một sample card.
                    Sau khi approve, theme sẽ apply cho toàn bộ instance.</p>
                    <a class="btn" href="#">Primary action</a>
                  </section>
                </body>
                </html>
                """.formatted(
                        orgName,
                        escape(colours.primary()),
                        escape(colours.secondary()),
                        escape(colours.accent()),
                        escape(colours.neutral()),
                        escape(colours.background()),
                        orgName,
                        escape(job.getId().toString())
        );
    }

    private static String escape(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
