package com.kiteclass.core.module.document.pdf;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders the Vietnamese tax invoice (hóa đơn GTGT) Thymeleaf template to PDF bytes via
 * OpenHTMLtoPDF, then bundles the result as a {@link DocumentResponse}.
 *
 * <p>Font handling preloads {@code DejaVuSans} + {@code DejaVuSans-Bold} (bundled under
 * {@code resources/fonts/}) so Vietnamese diacritics + Đ/đ render reliably — the default PDFBox
 * font set ships with limited glyph coverage and drops those characters silently.
 *
 * <p>This class is package-private deliberately: the public entry point is {@link PdfGenerator}
 * per ADR-019 Facade+Strategy, and callers should never instantiate the renderer directly.
 */
final class InvoiceRenderer {

    private static final Locale VI_VN = Locale.forLanguageTag("vi-VN");
    private static final DecimalFormat VND_FORMAT;
    private static final String FONT_REGULAR = "/fonts/DejaVuSans.ttf";
    private static final String FONT_BOLD = "/fonts/DejaVuSans-Bold.ttf";

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(VI_VN);
        symbols.setGroupingSeparator('.');
        VND_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    private final TemplateEngine templateEngine;

    InvoiceRenderer() {
        this.templateEngine = buildTemplateEngine();
    }

    DocumentResponse render(DocumentRequest request) {
        Map<String, Object> data = request.data();
        Context ctx = buildContext(data);

        String html = templateEngine.process("pdf/invoice", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> openClasspathFont(FONT_REGULAR), "DejaVuSans");
            builder.useFont(() -> openClasspathFont(FONT_BOLD), "DejaVuSans", 700, null, false);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return DocumentResponse.of(out.toByteArray(), DocumentFormat.PDF, buildFilename(data));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render invoice PDF", ex);
        }
    }

    private Context buildContext(Map<String, Object> data) {
        Context ctx = new Context(VI_VN);
        Map<String, Object> model = new HashMap<>(data);

        // Pre-format monetary values as strings so the Thymeleaf template doesn't need locale logic.
        model.put("subtotalFormatted", formatVnd(data.get("subtotal")));
        model.put("vatAmountFormatted", formatVnd(data.get("vatAmount")));
        model.put("totalFormatted", formatVnd(data.get("total")));
        model.put("vatPercent", formatVatPercent(data.get("vatRate")));
        model.put("itemsView", formatItems(data.get("items")));

        ctx.setVariables(model);
        return ctx;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> formatItems(Object itemsRaw) {
        if (!(itemsRaw instanceof List<?> itemsList)) {
            return List.of();
        }
        return itemsList.stream()
                .filter(Map.class::isInstance)
                .map(o -> (Map<String, Object>) o)
                .map(item -> {
                    Map<String, Object> view = new HashMap<>(item);
                    view.put("unitPriceFormatted", formatVnd(item.get("unitPrice")));
                    view.put("lineTotalFormatted", formatVnd(item.get("lineTotal")));
                    return view;
                })
                .toList();
    }

    private static String formatVnd(Object value) {
        if (value == null) {
            return "0";
        }
        BigDecimal amount = (value instanceof BigDecimal bd) ? bd : new BigDecimal(value.toString());
        return VND_FORMAT.format(amount);
    }

    private static String formatVatPercent(Object rate) {
        if (rate == null) {
            return "0%";
        }
        BigDecimal r = (rate instanceof BigDecimal bd) ? bd : new BigDecimal(rate.toString());
        return r.multiply(new BigDecimal(100)).stripTrailingZeros().toPlainString() + "%";
    }

    private static String buildFilename(Map<String, Object> data) {
        Object invoiceNumber = data.get("invoiceNumber");
        if (invoiceNumber == null) {
            return "invoice.pdf";
        }
        return "invoice-" + invoiceNumber + ".pdf";
    }

    private static InputStream openClasspathFont(String path) {
        InputStream stream = InvoiceRenderer.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException(
                    "Font resource not found on classpath: " + path
                            + ". Expected bundled under kiteclass-core/src/main/resources/fonts/.");
        }
        return stream;
    }

    private static TemplateEngine buildTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
