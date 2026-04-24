package com.kiteclass.core.module.document;

/**
 * Supported document output formats for Wave 5 (ADR-019).
 *
 * <p>PPT is deferred to Wave 6 per wave-05 decision guide Q6.
 */
public enum DocumentFormat {

    PDF("application/pdf", "pdf"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

    private final String mimeType;
    private final String extension;

    DocumentFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }
}
