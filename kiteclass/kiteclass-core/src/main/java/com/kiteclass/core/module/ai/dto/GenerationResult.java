package com.kiteclass.core.module.ai.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Output of {@code AIClient.generate()}.
 *
 * <p>Either carries an image reference (URL or bytes) on happy path, or
 * {@code templateFallback=true} when the resilience fallback fired.
 *
 * @since 3.18.0 (Wave 3 Sub-PR 3.2)
 */
@Value
@Builder
public class GenerationResult {

    /** Remote URL (e.g. MinIO object URL). Null when only bytes available. */
    String imageUrl;

    /** Raw image bytes when the provider returns inline data. Null when URL-only. */
    byte[] imageBytes;

    /** PNG / JPEG / WEBP mime type. */
    String mimeType;

    /** True when returned by resilience fallback — caller must use template path. */
    boolean templateFallback;

    public static GenerationResult templateFallback() {
        return GenerationResult.builder().templateFallback(true).build();
    }
}
