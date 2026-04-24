package com.kiteclass.core.module.document;

import java.util.Map;

/**
 * Shared test helpers for Sub-PRs 5.1–5.3 generator tests.
 *
 * <p>Keep minimal — sub-PRs extend with format-specific fixtures (PDF text extraction, XLSX sheet
 * parsing, DOCX paragraph inspection). Do NOT add Spring context loading here — generator unit
 * tests should stay fast and pure.
 */
public abstract class DocumentGenerationTestBase {

    protected static final String SAMPLE_TENANT_ID = "tenant-test-001";

    protected DocumentRequest sampleRequest(DocumentFormat format, String templateId) {
        return DocumentRequest.builder()
                .format(format)
                .templateId(templateId)
                .tenantId(SAMPLE_TENANT_ID)
                .data(Map.of())
                .build();
    }

    protected DocumentRequest sampleRequest(DocumentFormat format, String templateId, Map<String, Object> data) {
        return DocumentRequest.builder()
                .format(format)
                .templateId(templateId)
                .tenantId(SAMPLE_TENANT_ID)
                .data(data)
                .build();
    }
}
