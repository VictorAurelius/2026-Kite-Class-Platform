package com.kiteclass.core.module.document.controller;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentGenerationService;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import com.kiteclass.core.module.document.branding.DocumentBrandingAssembler;
import com.kiteclass.core.module.document.dto.DocumentGenerationRequestDto;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.service.BrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sub-PR 5.5 — HTTP surface for Wave 5 document generation (ADR-019).
 *
 * <p>Two endpoints per format:
 * <ul>
 *   <li>{@code POST /api/v1/documents/{format}/preview} — {@code Content-Disposition: inline},
 *       PDF only; xlsx/docx return 400 (browser cannot render them inline).</li>
 *   <li>{@code POST /api/v1/documents/{format}/download} — {@code Content-Disposition: attachment}
 *       for all three formats (pdf/xlsx/docx).</li>
 * </ul>
 *
 * <p>Branding is resolved server-side from the authenticated session via
 * {@link BrandingService#getBranding()}, assembled into the request by
 * {@link DocumentBrandingAssembler}, then dispatched through the
 * {@link DocumentGenerationService} facade.
 *
 * <p>Filename is taken from the generator's {@link DocumentResponse#filename()} and encoded with
 * RFC 5987 UTF-8 so Vietnamese diacritics survive in the Content-Disposition header.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "DocumentGeneration", description = "Generate branded PDF/Excel/Word documents from templates")
public class DocumentGenerationController {

    private final DocumentGenerationService documentGenerationService;
    private final DocumentBrandingAssembler brandingAssembler;
    private final BrandingService brandingService;

    @PostMapping("/{format}/preview")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','TEACHER')")
    @Operation(summary = "Generate a document and return it inline for in-browser preview (PDF only)")
    public ResponseEntity<byte[]> preview(
            @PathVariable String format,
            @Valid @RequestBody DocumentGenerationRequestDto body) {
        DocumentFormat fmt = parseFormat(format);
        if (fmt != DocumentFormat.PDF) {
            throw new IllegalArgumentException(
                    "Preview only supported for PDF; requested format: " + fmt);
        }
        return render(fmt, body, true);
    }

    @PostMapping("/{format}/download")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','TEACHER')")
    @Operation(summary = "Generate a document and return it as a downloadable attachment")
    public ResponseEntity<byte[]> download(
            @PathVariable String format,
            @Valid @RequestBody DocumentGenerationRequestDto body) {
        return render(parseFormat(format), body, false);
    }

    private ResponseEntity<byte[]> render(DocumentFormat format, DocumentGenerationRequestDto body, boolean inline) {
        UUID tenant = TenantContext.getCurrentTenant();
        BrandingResponse branding = brandingService.getBranding();

        DocumentRequest req = DocumentRequest.builder()
                .format(format)
                .templateId(body.templateId())
                .tenantId(tenant.toString())
                .data(body.data() == null ? Map.of() : body.data())
                .build();

        DocumentRequest enriched = brandingAssembler.enrich(req, branding);
        DocumentResponse doc = documentGenerationService.generate(enriched);

        ContentDisposition disposition = ContentDisposition
                .builder(inline ? "inline" : "attachment")
                .filename(doc.filename(), StandardCharsets.UTF_8)
                .build();

        log.info("Generated {} for tenant {} ({} bytes, {})",
                format, tenant, doc.bytes().length, inline ? "inline" : "attachment");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CONTENT_TYPE, doc.mimeType())
                .body(doc.bytes());
    }

    private static DocumentFormat parseFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("format path variable is required");
        }
        try {
            return DocumentFormat.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported format: " + raw + " (supported: pdf, xlsx, docx)");
        }
    }
}
