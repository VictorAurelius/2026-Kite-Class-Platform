package com.kiteclass.core.module.document;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable generation output — raw bytes + MIME + filename hint for Content-Disposition.
 *
 * <p>Record ensures byte array is carried by reference; callers must treat it as immutable even
 * though Java cannot enforce that on arrays. {@code equals}/{@code hashCode}/{@code toString} are
 * overridden so the {@code bytes} component compares by content instead of reference (the record
 * auto-generated versions delegate to {@link Objects} which uses reference equality for arrays).
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentResponse that)) {
            return false;
        }
        return Arrays.equals(bytes, that.bytes)
                && Objects.equals(mimeType, that.mimeType)
                && Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(bytes), mimeType, filename);
    }

    @Override
    public String toString() {
        return "DocumentResponse[bytes=" + bytes.length + " byte(s)"
                + ", mimeType=" + mimeType
                + ", filename=" + filename + "]";
    }
}
