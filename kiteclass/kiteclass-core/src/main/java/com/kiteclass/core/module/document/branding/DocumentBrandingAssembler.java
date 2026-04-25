package com.kiteclass.core.module.document.branding;

import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Sub-PR 5.5 — assembles per-tenant branding into a {@link DocumentRequest} before it hits the
 * format-specific {@link com.kiteclass.core.module.document.Generator}.
 *
 * <p>Key names stay as the generators already expect (<code>branding.primaryColor</code>,
 * <code>branding.logoUrl</code>, ...), so this class only needs to populate the data map — no
 * generator change is required.
 *
 * <p>Precedence: caller-provided keys win. Callers can override resolver values for
 * testing/preview scenarios without forking the generator path.
 */
@Component
public class DocumentBrandingAssembler {

    public DocumentRequest enrich(DocumentRequest request, BrandingResponse branding) {
        if (branding == null) {
            return request;
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        putIfPresent(merged, "branding.primaryColor", branding.getPrimaryColor());
        putIfPresent(merged, "branding.secondaryColor", branding.getSecondaryColor());
        putIfPresent(merged, "branding.accentColor", branding.getAccentColor());
        putIfPresent(merged, "branding.logoUrl", branding.getLogoUrl());
        putIfPresent(merged, "branding.displayName", branding.getDisplayName());

        if (merged.isEmpty() && request.data().isEmpty()) {
            return request;
        }
        if (merged.isEmpty()) {
            return request;
        }

        // Caller-provided data wins (put AFTER branding values).
        merged.putAll(request.data());

        return DocumentRequest.builder()
                .format(request.format())
                .templateId(request.templateId())
                .tenantId(request.tenantId())
                .data(merged)
                .build();
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            return;
        }
        target.put(key, value);
    }
}
