package com.kitehub.gateway.controller;

import com.kitehub.gateway.client.GatewayBranding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders branded HTML error pages using simple {@code {{token}}} substitution.
 *
 * <p>We avoid a full template engine (Thymeleaf, Freemarker) to keep the
 * gateway image small and sidestep the reactive-vs-servlet engine split.
 * Templates live at {@code classpath:/templates/errors/*.html}.
 *
 * @since Wave 4 (GAP-032)
 */
@Slf4j
@Component
public class ErrorPageRenderer {

    private static final String FALLBACK_503 = "<html><body><h1>503 Service Unavailable</h1></body></html>";

    public String render(String templateName, GatewayBranding branding,
                         String title, String message, String service) {
        String raw = loadTemplate(templateName);
        return applyTokens(raw, branding, title, message, service);
    }

    private String loadTemplate(String name) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/errors/" + name);
            try (var is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            log.error("Failed to load error template {}: {}", name, ex.getMessage());
            return FALLBACK_503;
        }
    }

    private String applyTokens(String raw, GatewayBranding branding,
                               String title, String message, String service) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("displayName", safe(branding.getDisplayName(), "KiteHub"));
        tokens.put("primaryColor", safe(branding.getPrimaryColor(), "#3B82F6"));
        tokens.put("secondaryColor", safe(branding.getSecondaryColor(), "#8B5CF6"));
        tokens.put("title", safe(title, ""));
        tokens.put("message", safe(message, ""));
        tokens.put("service", safe(service, ""));
        tokens.put("timestamp", Instant.now().toString());

        String logoUrl = branding.getLogoUrl();
        tokens.put("logoBlock",
                (logoUrl == null || logoUrl.isBlank())
                        ? ""
                        : "<img class=\"logo\" src=\"" + escape(logoUrl) + "\" alt=\"Logo\"/>");

        String out = raw;
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    /** Conservative HTML-attribute escape for untrusted URL content. */
    private static String escape(String raw) {
        return raw.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
