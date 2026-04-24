package com.kiteclass.core.module.document;

/**
 * Immutable generation output — raw bytes + MIME + filename hint for Content-Disposition.
 *
 * <p>Record ensures byte array is carried by reference; callers must treat it as immutable even
 * though Java cannot enforce that on arrays.
 */
public record DocumentResponse(byte[] bytes, String mimeType, String filename) {

    public DocumentResponse {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType must not be blank");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename must not be blank");
        }
    }

    public static DocumentResponse of(byte[] bytes, DocumentFormat format, String filename) {
        if (format == null) {
            throw new IllegalArgumentException("format must not be null");
        }
        return new DocumentResponse(bytes, format.mimeType(), filename);
    }
}
