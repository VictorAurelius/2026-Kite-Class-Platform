package com.kiteclass.core.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the calling teacher's ID from the current HTTP request's
 * {@code X-Teacher-Id} header.
 *
 * <p>Adapter shim so SpEL expressions like
 * {@code @authz.hasAccessToClass(#classId)} can resolve a stable
 * caller identity without each controller threading the header through
 * the principal model. Wave 107 will swap this for a JWT claim
 * extractor (when gateway-issued JWT carries teacher principal).
 *
 * <p>Request-scoped: one instance per request lifecycle — safe to
 * inject into singleton {@link AuthorizationHelper}.
 *
 * @author KiteClass Team — Wave 105 Bucket C
 * @since 2026-05-22
 */
@Slf4j
@Component
@RequestScope
public class TeacherIdHolder {

    public static final String HEADER_NAME = "X-Teacher-Id";

    /**
     * Reads the {@code X-Teacher-Id} request header from the current
     * request context. Returns {@code null} when no request is bound
     * (test context) OR the header is absent OR the value is not a
     * positive long.
     */
    public Long currentTeacherId() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest req = attrs.getRequest();
            String raw = req.getHeader(HEADER_NAME);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            log.debug("Invalid X-Teacher-Id header value: {}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.debug("Failed to read X-Teacher-Id: {}", ex.getMessage());
            return null;
        }
    }
}
