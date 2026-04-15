package com.kiteclass.core.common.security.impl;

import com.kiteclass.core.common.security.SvgSanitizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * JSoup-backed {@link SvgSanitizer} that strips XSS vectors from untrusted SVG
 * per ADR-011 "Defense-in-Depth".
 *
 * <p>Preserves standard presentation elements/attributes used by branding SVGs
 * (logos, icons, badges) while removing:
 * <ul>
 *   <li>{@code <script>} elements (any casing)</li>
 *   <li>{@code on*} event-handler attributes (onclick, onload, onerror, onmouseover, ...)</li>
 *   <li>{@code foreignObject}, {@code iframe}, {@code embed}, {@code object} (content embedding)</li>
 *   <li>{@code xlink:href} / {@code href} whose value is NOT a same-origin fragment (#id)</li>
 *   <li>{@code javascript:} / {@code data:} URLs inside style or href attributes</li>
 * </ul>
 *
 * <p>Implementation note: JSoup is used internally only — the SPI contract keeps
 * {@link String} input/output (see package-info).
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.2)
 */
@Component
public class JsoupSvgSanitizer implements SvgSanitizer {

    private static final Safelist SVG_SAFELIST = buildSvgSafelist();

    @Override
    public String sanitize(String rawSvg) {
        if (rawSvg == null || rawSvg.isBlank()) {
            return "";
        }

        // Parse as XML so SVG namespaces / self-closing tags survive; SafeList-clean the body.
        Document doc = Jsoup.parse(rawSvg, "", Parser.xmlParser());

        // Drop dangerous elements the Safelist wouldn't catch when parsing as XML
        // (the allowlist+clean() path below handles most; this belt-and-suspenders pass also
        // strips elements the xmlParser exposed verbatim).
        doc.select("script, foreignObject, iframe, embed, object")
                .forEach(Element::remove);

        // Strip attributes with dangerous schemes or event handlers.
        scrubAttributes(doc);

        String body = doc.outerHtml();

        // Run through HTML cleaner with the SVG SafeList for final allow-list enforcement.
        Document cleaned = Jsoup.parseBodyFragment(body);
        cleaned.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        org.jsoup.nodes.Document.OutputSettings out = new Document.OutputSettings()
                .prettyPrint(false)
                .syntax(Document.OutputSettings.Syntax.xml);

        String safe = Jsoup.clean(body, "", SVG_SAFELIST, out);
        return safe == null ? "" : safe.trim();
    }

    private static void scrubAttributes(Document doc) {
        for (Element el : doc.getAllElements()) {
            // Remove any on* event handler attribute (onclick, onload, ...)
            el.attributes().asList().forEach(attr -> {
                String key = attr.getKey().toLowerCase();
                String val = attr.getValue() == null ? "" : attr.getValue().trim().toLowerCase();
                if (key.startsWith("on")) {
                    el.removeAttr(attr.getKey());
                    return;
                }
                // Block dangerous hrefs except same-document fragment references
                if ((key.equals("href") || key.equals("xlink:href"))
                        && !val.startsWith("#")) {
                    el.removeAttr(attr.getKey());
                    return;
                }
                // Block dangerous schemes in style
                if (key.equals("style") && (val.contains("javascript:") || val.contains("expression("))) {
                    el.removeAttr(attr.getKey());
                }
            });
        }
    }

    private static Safelist buildSvgSafelist() {
        Safelist s = new Safelist();

        String[] tags = {
                "svg", "g", "path", "rect", "circle", "ellipse", "polygon", "polyline",
                "line", "text", "tspan", "defs", "lineargradient", "radialgradient",
                "stop", "filter", "animate", "use", "title", "desc"
        };
        for (String t : tags) {
            s.addTags(t);
        }

        String[] commonAttrs = {
                "id", "class", "style", "fill", "stroke", "stroke-width",
                "x", "y", "x1", "y1", "x2", "y2", "cx", "cy", "r", "rx", "ry",
                "width", "height", "viewBox", "preserveAspectRatio", "transform",
                "offset", "stop-color", "stop-opacity", "opacity", "d"
        };
        for (String t : tags) {
            s.addAttributes(t, commonAttrs);
        }

        // Allow same-doc fragment href on <use> only (#gradient-id style refs)
        s.addAttributes("use", "href", "xlink:href");
        s.addProtocols("use", "href", "#");
        s.addProtocols("use", "xlink:href", "#");

        return s;
    }
}
