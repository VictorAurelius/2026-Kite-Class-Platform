package com.kiteclass.core.module.document.branding;

/**
 * Parses a CSS-style hex color ({@code #RRGGBB} or {@code RRGGBB}) into the two formats the
 * renderers need:
 * <ul>
 *   <li>{@link #toRgbBytes(String)} — 3-byte array for POI {@code XSSFColor}.</li>
 *   <li>{@link #stripHash(String)} — 6-char upper-case hex for POI {@code XWPFRun.setColor}.</li>
 * </ul>
 *
 * <p>All methods return {@code null} when the input is blank or malformed — callers fall back to
 * default styling so a stray tenant config cannot break rendering.
 */
public final class HexColorUtil {

    private HexColorUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static byte[] toRgbBytes(String hex) {
        String stripped = stripHash(hex);
        if (stripped == null) {
            return null;
        }
        try {
            int r = Integer.parseInt(stripped.substring(0, 2), 16);
            int g = Integer.parseInt(stripped.substring(2, 4), 16);
            int b = Integer.parseInt(stripped.substring(4, 6), 16);
            return new byte[]{(byte) r, (byte) g, (byte) b};
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String stripHash(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        String trimmed = hex.trim();
        String value = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
        if (value.length() != 6) {
            return null;
        }
        for (int i = 0; i < 6; i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return null;
            }
        }
        return value.toUpperCase(java.util.Locale.ROOT);
    }
}
