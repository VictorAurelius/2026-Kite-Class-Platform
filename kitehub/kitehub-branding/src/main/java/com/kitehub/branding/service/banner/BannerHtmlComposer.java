package com.kitehub.branding.service.banner;

import com.kitehub.branding.wizard.dto.BrandColours;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Composes the deterministic TEMPLATE-mode banner HTML (GAP-1135).
 *
 * <p>This is the REAL artifact of the free TEMPLATE path (ADR-037 Amendment) —
 * a 3-layer banner (background gradient + brand text/copy + portrait + theme
 * icon) whose layout mirrors
 * {@code kiteclass-frontend/scripts/compose-sky-demo-banner.mjs}. Crisp
 * Vietnamese diacritics, deterministic, $0. The HTML is then handed to a
 * {@link BannerRenderer} (Playwright sidecar) to rasterise to WebP.</p>
 *
 * <p>All tenant-supplied text is HTML-escaped before interpolation
 * (sanitize-on-write; AI-generated copy must not bypass safety per ADR-037 §4 /
 * GAP-827).</p>
 *
 * @since GAP-1135
 */
@Component
public class BannerHtmlComposer {

    /** OG-ready 1200x630 default (matches compose-sky-demo-banner.mjs output). */
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 630;

    /**
     * Compose the banner HTML for the TEMPLATE path.
     *
     * @param organizationName tenant / centre display name (headline)
     * @param copy             Gemini-generated marketing copy (subtitle source)
     * @param logoUrl          uploaded logo URL (brand mark; nullable)
     * @param portraitUrls     uploaded portrait URLs (GAP-1134 — first is featured; nullable)
     * @param themeIcon        theme/subject icon (emoji or short text; nullable)
     * @param colours          validated brand palette
     * @return composed HTML + dimensions
     */
    public BannerComposition compose(String organizationName, String copy, String logoUrl,
                                     List<String> portraitUrls, String themeIcon,
                                     BrandColours colours) {
        String headline = esc(blankTo(organizationName, "Trung tâm giáo dục"));
        String subtitle = esc(firstSentence(blankTo(copy,
                "Chương trình học chất lượng cao — đội ngũ giảng viên tận tâm.")));
        String icon = esc(blankTo(themeIcon, "📚"));
        String brandInitial = headline.isEmpty() ? "K" : headline.substring(0, 1).toUpperCase();

        String primary = colours.primary();
        String secondary = colours.secondary();
        String accent = colours.accent();
        String neutral = colours.neutral();

        String portrait = portraitUrls != null && !portraitUrls.isEmpty()
                ? portraitUrls.get(0) : null;
        String portraitLayer = portrait != null
                ? "<div class=\"portrait\"><img src=\"" + escAttr(portrait) + "\" alt=\"\"></div>"
                : "<div class=\"portrait portrait--icon\"><span>" + icon + "</span></div>";

        String logoLayer = logoUrl != null && !logoUrl.isBlank()
                ? "<img class=\"logo\" src=\"" + escAttr(logoUrl) + "\" alt=\"\">"
                : "<span class=\"mark\">" + esc(brandInitial) + "</span>";

        String html = """
                <!DOCTYPE html><html lang="vi"><head><meta charset="utf-8">
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600;700;800&display=swap" rel="stylesheet">
                <style>
                *{margin:0;padding:0;box-sizing:border-box}
                html,body{width:%dpx;height:%dpx}
                .banner{width:%dpx;height:%dpx;position:relative;overflow:hidden;
                 font-family:'Be Vietnam Pro',sans-serif;
                 background:linear-gradient(125deg,%s 0%%,%s 60%%,%s 100%%)}
                .glow{position:absolute;right:-120px;bottom:-160px;width:560px;height:560px;
                 background:radial-gradient(circle,%s 0%%,rgba(0,0,0,0) 70%%);opacity:.45}
                .accent{position:absolute;top:-80px;right:280px;width:420px;height:420px;
                 background:radial-gradient(circle,%s 0%%,rgba(0,0,0,0) 68%%);opacity:.35}
                .left{position:absolute;left:72px;top:0;width:640px;height:100%%;
                 display:flex;flex-direction:column;justify-content:center}
                .brand{display:flex;align-items:center;gap:14px;margin-bottom:28px}
                .brand .mark{width:46px;height:46px;border-radius:12px;
                 background:%s;display:flex;align-items:center;justify-content:center;
                 color:#fff;font-weight:800;font-size:24px}
                .brand .logo{height:46px;border-radius:10px;background:#fff;padding:6px}
                .brand .name{color:#fff;font-weight:700;font-size:22px;letter-spacing:.3px}
                h1{color:#fff;font-weight:800;font-size:54px;line-height:1.12;letter-spacing:-.5px;margin-bottom:18px}
                p.sub{color:rgba(255,255,255,.88);font-size:24px;line-height:1.4;max-width:560px}
                .cta{margin-top:34px;display:inline-block;align-self:flex-start;
                 background:%s;color:#0b1220;font-weight:700;font-size:20px;
                 padding:14px 30px;border-radius:999px}
                .portrait{position:absolute;right:60px;bottom:0;width:430px;height:560px;
                 display:flex;align-items:flex-end;justify-content:center}
                .portrait img{height:100%%;object-fit:contain;filter:drop-shadow(0 14px 30px rgba(0,0,0,.35))}
                .portrait--icon span{font-size:220px;opacity:.9}
                .theme-icon{position:absolute;left:72px;bottom:48px;font-size:40px;opacity:.85}
                </style></head>
                <body><div class="banner">
                  <div class="glow"></div><div class="accent"></div>
                  <div class="left">
                    <div class="brand">%s<span class="name">%s</span></div>
                    <h1>%s</h1>
                    <p class="sub">%s</p>
                    <span class="cta">Đăng ký học thử</span>
                  </div>
                  %s
                  <div class="theme-icon">%s</div>
                </div></body></html>
                """.formatted(
                WIDTH, HEIGHT, WIDTH, HEIGHT,
                cssColor(primary), cssColor(secondary), cssColor(neutral),
                cssColor(accent), cssColor(secondary),
                cssColor(accent), cssColor(accent),
                logoLayer, headline, headline, subtitle,
                portraitLayer, icon);

        return new BannerComposition(html, WIDTH, HEIGHT);
    }

    // ---- helpers -------------------------------------------------------------

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String firstSentence(String text) {
        String t = text.trim();
        int dot = t.indexOf('.');
        String s = (dot > 20) ? t.substring(0, dot + 1) : t;
        return s.length() > 160 ? s.substring(0, 157) + "…" : s;
    }

    /** CSS hex passthrough — BrandColours already validates #RRGGBB. */
    private static String cssColor(String hex) {
        return (hex != null && hex.matches("^#[0-9A-Fa-f]{6}$")) ? hex : "#2563EB";
    }

    /** Escape HTML text content. */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Escape an attribute value (URLs): strip quotes/angles to prevent breakout. */
    private static String escAttr(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\"", "%22")
                .replace("'", "%27")
                .replace("<", "%3C")
                .replace(">", "%3E");
    }
}
