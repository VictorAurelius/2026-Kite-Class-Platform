package com.kiteclass.core.module.document;

/**
 * Strategy — one implementation per {@link DocumentFormat}. Concrete implementations (PdfGenerator,
 * XlsxGenerator, DocxGenerator) land in Wave 5 Sub-PRs 5.1–5.3 and are auto-discovered by
 * {@link DocumentGenerationService} via Spring bean injection of {@code List<Generator>}.
 */
public interface Generator {

    /** The format this generator produces. Used by the facade to route requests. */
    DocumentFormat format();

    /**
     * Generate a document for the given request.
     *
     * <p>Implementations must apply tenant branding via the Branding Package API (ADR-009) and
     * fail-fast on unknown template IDs.
     */
    DocumentResponse generate(DocumentRequest request);
}
