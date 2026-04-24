package com.kiteclass.core.module.document;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable generation request — what format, which template, for which tenant, with what data.
 *
 * <p>Validation fails fast in {@link Builder#build()}; generators receive a known-good request.
 * Invariants enforced:
 * <ul>
 *   <li>{@code format} non-null</li>
 *   <li>{@code templateId} non-blank</li>
 *   <li>{@code tenantId} non-blank</li>
 *   <li>{@code data} defaults to empty map when null</li>
 * </ul>
 */
public final class DocumentRequest {

    private final DocumentFormat format;
    private final String templateId;
    private final String tenantId;
    private final Map<String, Object> data;

    private DocumentRequest(Builder b) {
        this.format = b.format;
        this.templateId = b.templateId;
        this.tenantId = b.tenantId;
        this.data = b.data == null ? Map.of() : Collections.unmodifiableMap(b.data);
    }

    public DocumentFormat format() {
        return format;
    }

    public String templateId() {
        return templateId;
    }

    public String tenantId() {
        return tenantId;
    }

    public Map<String, Object> data() {
        return data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DocumentFormat format;
        private String templateId;
        private String tenantId;
        private Map<String, Object> data;

        public Builder format(DocumentFormat format) {
            this.format = format;
            return this;
        }

        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public DocumentRequest build() {
            if (format == null) {
                throw new IllegalArgumentException("format must not be null");
            }
            if (templateId == null || templateId.isBlank()) {
                throw new IllegalArgumentException("templateId must not be blank");
            }
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId must not be blank");
            }
            return new DocumentRequest(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentRequest that)) {
            return false;
        }
        return format == that.format
                && Objects.equals(templateId, that.templateId)
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, templateId, tenantId, data);
    }
}
