package com.kitehub.branding.util;

/**
 * Utility class for color manipulations.
 * Converts between RGB/HSL and generates color variants.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public class ColorUtils {

    /**
     * Lighten a hex color by percentage (0.0 - 1.0).
     * Adjusts HSL lightness value.
     *
     * @param hex Hex color (e.g., "#2196F3")
     * @param percentage Lightness increase (0.0 - 1.0)
     * @return Lightened hex color
     */
    public static String lighten(String hex, double percentage) {
        int[] rgb = hexToRgb(hex);
        double[] hsl = rgbToHsl(rgb[0], rgb[1], rgb[2]);

        // Increase lightness
        hsl[2] = Math.min(1.0, hsl[2] + (percentage * (1.0 - hsl[2])));

        int[] newRgb = hslToRgb(hsl[0], hsl[1], hsl[2]);
        return rgbToHex(newRgb[0], newRgb[1], newRgb[2]);
    }

    /**
     * Darken a hex color by percentage (0.0 - 1.0).
     * Adjusts HSL lightness value.
     *
     * @param hex Hex color (e.g., "#2196F3")
     * @param percentage Lightness decrease (0.0 - 1.0)
     * @return Darkened hex color
     */
    public static String darken(String hex, double percentage) {
        int[] rgb = hexToRgb(hex);
        double[] hsl = rgbToHsl(rgb[0], rgb[1], rgb[2]);

        // Decrease lightness
        hsl[2] = Math.max(0.0, hsl[2] - (percentage * hsl[2]));

        int[] newRgb = hslToRgb(hsl[0], hsl[1], hsl[2]);
        return rgbToHex(newRgb[0], newRgb[1], newRgb[2]);
    }

    /**
     * Convert hex color to RGB.
     */
    private static int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return new int[]{r, g, b};
    }

    /**
     * Convert RGB to hex color.
     */
    private static String rgbToHex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Convert RGB to HSL.
     * @return [hue (0-360), saturation (0-1), lightness (0-1)]
     */
    private static double[] rgbToHsl(int r, int g, int b) {
        double rNorm = r / 255.0;
        double gNorm = g / 255.0;
        double bNorm = b / 255.0;

        double max = Math.max(rNorm, Math.max(gNorm, bNorm));
        double min = Math.min(rNorm, Math.min(gNorm, bNorm));
        double delta = max - min;

        double h = 0, s, l = (max + min) / 2.0;

        if (delta == 0) {
            h = 0;  // Achromatic (gray)
            s = 0;
        } else {
            s = l > 0.5 ? delta / (2.0 - max - min) : delta / (max + min);

            if (max == rNorm) {
                h = ((gNorm - bNorm) / delta + (gNorm < bNorm ? 6 : 0)) / 6.0;
            } else if (max == gNorm) {
                h = ((bNorm - rNorm) / delta + 2) / 6.0;
            } else {
                h = ((rNorm - gNorm) / delta + 4) / 6.0;
            }
        }

        return new double[]{h * 360, s, l};
    }

    /**
     * Convert HSL to RGB.
     * @param h Hue (0-360)
     * @param s Saturation (0-1)
     * @param l Lightness (0-1)
     * @return [r, g, b] (0-255)
     */
    private static int[] hslToRgb(double h, double s, double l) {
        h = h / 360.0;  // Normalize to 0-1

        double r, g, b;

        if (s == 0) {
            r = g = b = l;  // Achromatic
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRgb(p, q, h + 1.0 / 3.0);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0 / 3.0);
        }

        return new int[]{
                (int) Math.round(r * 255),
                (int) Math.round(g * 255),
                (int) Math.round(b * 255)
        };
    }

    /**
     * Helper for HSL to RGB conversion.
     */
    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }
}
