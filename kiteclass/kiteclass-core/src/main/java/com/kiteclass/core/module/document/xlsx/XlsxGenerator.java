package com.kiteclass.core.module.document.xlsx;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import com.kiteclass.core.module.document.Generator;
import org.springframework.stereotype.Component;

/**
 * Excel Strategy implementation — Wave 5 Sub-PR 5.2 (GAP-047, ADR-019).
 *
 * <p>Auto-discovered by {@link com.kiteclass.core.module.document.DocumentGenerationService} via
 * Spring's {@code List<Generator>} injection. Delegates template-specific workbook assembly to a
 * builder (Facade + Strategy pattern per {@code .claude/rules/design-patterns.md} §2 and §3).
 *
 * <p>Wave 5 whitelist: {@code "attendance"} only (weekly per-class P/A/L/E report). Additional
 * templates (grade report, roster, financial breakdown) arrive in later waves per
 * {@code documents/03-planning/waves/wave-05-document-generation.md} §8 roadmap.
 *
 * <p>Branding (ADR-009) is wired via {@code DocumentBrandingAssembler}: dotted {@code branding.*}
 * keys on {@code request.data()} flow to {@code AttendanceReportBuilder}, which themes the header
 * row using {@code branding.primaryColor} and falls back to neutral defaults if absent. Anchor-image
 * logo injection is tracked as a follow-up enhancement (out of Wave 5 scope).
 */
@Component
public class XlsxGenerator implements Generator {

    private final AttendanceReportBuilder attendanceBuilder;

    public XlsxGenerator() {
        this(new AttendanceReportBuilder());
    }

    XlsxGenerator(AttendanceReportBuilder attendanceBuilder) {
        this.attendanceBuilder = attendanceBuilder;
    }

    @Override
    public DocumentFormat format() {
        return DocumentFormat.XLSX;
    }

    @Override
    public DocumentResponse generate(DocumentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.format() != DocumentFormat.XLSX) {
            throw new IllegalArgumentException(
                    "XlsxGenerator only accepts format=XLSX, got " + request.format());
        }

        String templateId = request.templateId();
        if ("attendance".equals(templateId)) {
            return attendanceBuilder.build(request);
        }

        throw new IllegalArgumentException(
                "Unknown XLSX templateId: '" + templateId + "'. Supported templates: [attendance]");
    }
}
