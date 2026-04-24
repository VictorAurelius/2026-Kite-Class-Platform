package com.kiteclass.core.module.document.pdf;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import com.kiteclass.core.module.document.Generator;
import org.springframework.stereotype.Component;

/**
 * PDF Strategy implementation — Wave 5 Sub-PR 5.1 (GAP-047, ADR-019).
 *
 * <p>Auto-discovered by {@link com.kiteclass.core.module.document.DocumentGenerationService}
 * via Spring's {@code List<Generator>} injection. Delegates template-specific HTML composition
 * to an internal renderer (Facade + Strategy pattern per {@code .claude/rules/design-patterns.md}
 * §2 and §3).
 *
 * <p>Wave 5 ships exactly one template: {@code "invoice"} (Vietnamese tax invoice, hóa đơn GTGT).
 * Additional templates (certificate, transcript, report) arrive in later waves per
 * {@code documents/03-planning/waves/wave-05-document-generation.md} §8 roadmap.
 *
 * <p>TODO (Sub-PR 5.5): wire Branding Package API (ADR-009) through the request data map so
 * tenant primary color and logo URL drive the invoice header. For now, renderer reads optional
 * {@code branding.primaryColor} / {@code branding.logoUrl} keys from {@code request.data()} and
 * falls back to neutral defaults.
 */
@Component
public class PdfGenerator implements Generator {

    private final InvoiceRenderer invoiceRenderer;

    public PdfGenerator() {
        this(new InvoiceRenderer());
    }

    PdfGenerator(InvoiceRenderer invoiceRenderer) {
        this.invoiceRenderer = invoiceRenderer;
    }

    @Override
    public DocumentFormat format() {
        return DocumentFormat.PDF;
    }

    @Override
    public DocumentResponse generate(DocumentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.format() != DocumentFormat.PDF) {
            throw new IllegalArgumentException(
                    "PdfGenerator only accepts format=PDF, got " + request.format());
        }

        String templateId = request.templateId();
        if ("invoice".equals(templateId)) {
            return invoiceRenderer.render(request);
        }

        // Fail-fast on unknown template (per BR-DOC-PDF-001 and design-patterns.md §1.1 YAGNI).
        throw new IllegalArgumentException(
                "Unknown PDF templateId: '" + templateId + "'. Supported templates: [invoice]");
    }
}
