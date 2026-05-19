package com.kiteclass.core.module.document.docx;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import com.kiteclass.core.module.document.Generator;
import org.springframework.stereotype.Component;

/**
 * Word Strategy implementation — Wave 5 Sub-PR 5.3 (GAP-047, ADR-019).
 *
 * <p>Auto-discovered by {@link com.kiteclass.core.module.document.DocumentGenerationService} via
 * Spring's {@code List<Generator>} injection. Delegates template-specific document assembly to a
 * builder (Facade + Strategy pattern per {@code .claude/rules/design-patterns.md} §2 and §3).
 *
 * <p>Wave 5 whitelist: {@code "teacher-contract"} only (placeholder wording; legal review deferred
 * per wave plan §3). Additional templates (parent letters, certificates, policies) arrive in later
 * waves per {@code documents/03-planning/waves/wave-05-document-generation.md} §8 roadmap.
 *
 * <p>The generator implements the **Create** pipeline only (of Create / Edit-Fill / Reformat per
 * MiniMax minimax-docx taxonomy — see {@code .claude/skills/document-generation/word/SKILL.md}).
 *
 * <p>Branding (ADR-009) is wired via {@code DocumentBrandingAssembler}: the controller layer
 * lifts the tenant's branding package into dotted {@code branding.*} keys on {@code request.data()}.
 * {@code TeacherContractBuilder} reads {@code branding.primaryColor} for the title paragraph and
 * falls back to neutral black if absent.
 */
@Component
public class DocxGenerator implements Generator {

    private final TeacherContractBuilder contractBuilder;
    private final ThesisReportBuilder thesisBuilder;

    public DocxGenerator() {
        this(new TeacherContractBuilder(), new ThesisReportBuilder());
    }

    DocxGenerator(TeacherContractBuilder contractBuilder, ThesisReportBuilder thesisBuilder) {
        this.contractBuilder = contractBuilder;
        this.thesisBuilder = thesisBuilder;
    }

    @Override
    public DocumentFormat format() {
        return DocumentFormat.DOCX;
    }

    @Override
    public DocumentResponse generate(DocumentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.format() != DocumentFormat.DOCX) {
            throw new IllegalArgumentException(
                    "DocxGenerator only accepts format=DOCX, got " + request.format());
        }

        String templateId = request.templateId();
        if ("teacher-contract".equals(templateId)) {
            return contractBuilder.build(request);
        }
        if ("thesis-report".equals(templateId)) {
            return thesisBuilder.build(request);
        }

        throw new IllegalArgumentException(
                "Unknown DOCX templateId: '" + templateId
                        + "'. Supported templates: [teacher-contract, thesis-report]");
    }
}
