/**
 * PDF generation — Wave 5 Sub-PR 5.1.
 *
 * <p>Contains the {@link com.kiteclass.core.module.document.Generator} implementation for
 * {@link com.kiteclass.core.module.document.DocumentFormat#PDF} plus the Thymeleaf-based
 * {@link com.kiteclass.core.module.document.pdf.InvoiceRenderer} helper that lays out the
 * Vietnamese tax invoice template (hóa đơn GTGT).
 *
 * <p>Rendering pipeline: Thymeleaf (HTML + inline CSS subset) → OpenHTMLtoPDF → PDFBox 2.0.
 * Fonts (DejaVu Sans Regular + Bold) are loaded from {@code resources/fonts/} for
 * Vietnamese diacritic coverage — see {@code .claude/skills/document-generation/pdf/SKILL.md}
 * gotchas §3 and {@code documents/01-business/kiteclass/document-generation/rules.md}
 * BR-DOC-PDF-002.
 */
package com.kiteclass.core.module.document.pdf;
