package com.kitehub.subscription.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Cursor (keyset) pagination envelope — Wave 85 Bucket D D-AC1.
 *
 * <p>Avoids {@code OFFSET N} cliff when paginating datasets &gt;1M rows.
 * The {@code nextCursor} is an opaque base64-encoded UUID of the last row
 * in the current page; clients pass it back unchanged.</p>
 *
 * <p>Order convention: {@code id ASC} (stable keyset). Combine with size 50-200.</p>
 *
 * @param <T> response DTO type
 */
public final class CursorPage<T> {

    private final List<T> content;
    private final int size;
    private final String nextCursor;
    private final boolean hasNext;

    public CursorPage(List<T> content, int size, String nextCursor, boolean hasNext) {
        this.content = content;
        this.size = size;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public List<T> getContent() {
        return content;
    }

    public int getSize() {
        return size;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    /** Encode a UUID into an opaque base64 cursor token. */
    public static String encodeCursor(UUID id) {
        if (id == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode a base64 cursor token back to a UUID.
     *
     * @param cursor opaque token from prior page response (nullable for first page)
     * @return decoded UUID or {@code null} if cursor is null/blank
     * @throws IllegalArgumentException if cursor is malformed
     */
    public static UUID decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            return UUID.fromString(new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cursor token", e);
        }
    }
}
