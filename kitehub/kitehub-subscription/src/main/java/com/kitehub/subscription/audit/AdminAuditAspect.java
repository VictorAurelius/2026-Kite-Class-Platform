package com.kitehub.subscription.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AOP aspect that persists an {@link AdminAuditLog} row around every method
 * annotated with {@link Auditable} (GAP-521 / OWASP A07).
 *
 * <p>Captures:
 * <ul>
 *   <li>{@code admin_user_id} from {@link SecurityContextHolder}</li>
 *   <li>{@code action} + {@code target_entity_type} from {@link Auditable}</li>
 *   <li>{@code target_entity_id} via best-effort SpEL-lite (default arg0)</li>
 *   <li>{@code request_ip} + {@code user_agent} from the current servlet request</li>
 *   <li>{@code payload_json} — redacted snapshot of method args (passwords,
 *       tokens, secret-shaped keys are masked)</li>
 *   <li>{@code success} / {@code error_message} based on method outcome</li>
 * </ul>
 *
 * <p>Failure to write the audit row MUST NOT prevent the underlying action
 * from completing — admin work is more important than the audit-of-audit
 * loop. Log a WARN and continue.</p>
 *
 * @since 1.0.0 (Wave 72a GAP-521)
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuditAspect {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Argument names/types whose values must NEVER be serialized into payloadJson. */
    private static final String[] SENSITIVE_NAMES = {
        "password", "passwd", "secret", "token", "apiKey", "api_key",
        "authorization", "refreshToken", "accessToken", "jwt"
    };

    private final AdminAuditLogRepository repository;

    @Around("@annotation(com.kitehub.subscription.audit.Auditable)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Auditable annotation = method.getAnnotation(Auditable.class);

        AdminAuditLog.AdminAuditLogBuilder logBuilder = AdminAuditLog.builder()
            .action(annotation.action())
            .targetEntityType(annotation.entityType().isBlank() ? null : annotation.entityType())
            .targetEntityId(resolveEntityId(annotation, pjp.getArgs(), annotation.entityIdSource()))
            // Wave 92 Bucket A — GAP-521 Phase 2 enrichment.
            .targetResourceType(annotation.resourceType().isBlank() ? null : annotation.resourceType())
            .targetResourceId(annotation.resourceIdSource().isBlank()
                ? null
                : resolveEntityId(annotation, pjp.getArgs(), annotation.resourceIdSource()))
            .createdAt(LocalDateTime.now());

        populateAdmin(logBuilder);
        populateRequest(logBuilder);
        logBuilder.payloadJson(buildRedactedPayloadJson(sig.getParameterNames(), pjp.getArgs()));

        Object result;
        try {
            result = pjp.proceed();
            logBuilder.success(true);
        } catch (Throwable ex) {
            logBuilder.success(false);
            logBuilder.errorMessage(truncate(ex.getClass().getSimpleName() + ": " + ex.getMessage(), 1024));
            persist(logBuilder.build());
            throw ex;
        }

        persist(logBuilder.build());
        return result;
    }

    private void persist(AdminAuditLog entry) {
        try {
            repository.save(entry);
        } catch (Exception ex) {
            // Audit write must NEVER fail the action it audits.
            log.warn("Failed to persist admin_audit_log row (action={}, target={}/{}): {}",
                entry.getAction(), entry.getTargetEntityType(), entry.getTargetEntityId(),
                ex.getMessage());
        }
    }

    private void populateAdmin(AdminAuditLog.AdminAuditLogBuilder builder) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            // Should not happen on @PreAuthorize('hasRole') endpoints, but guard anyway.
            return;
        }
        try {
            builder.adminUserId(UUID.fromString(auth.getName()));
        } catch (IllegalArgumentException ex) {
            log.warn("Authentication principal '{}' is not a UUID — admin audit row will lack adminUserId",
                auth.getName());
        }
    }

    private void populateRequest(AdminAuditLog.AdminAuditLogBuilder builder) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            return;
        }
        HttpServletRequest req = sra.getRequest();
        builder.requestIp(extractClientIp(req));
        builder.userAgent(truncate(req.getHeader("User-Agent"), 512));
        // Wave 92 Bucket A — GAP-521 Phase 2: capture X-Request-Id / traceparent
        // for correlation với access logs + APM traces. Prefer explicit
        // X-Request-Id; fallback traceparent (OTel header).
        String requestId = req.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = req.getHeader("traceparent");
        }
        if (requestId != null && !requestId.isBlank()) {
            builder.requestId(truncate(requestId, 64));
        }
    }

    private String extractClientIp(HttpServletRequest req) {
        // Trust X-Forwarded-For only if behind the gateway; first IP in the list is client.
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) {
                return truncate(first, 64);
            }
        }
        return truncate(req.getRemoteAddr(), 64);
    }

    /**
     * Resolve a target id from method args per a SpEL-lite source string.
     * Supports a minimal subset:
     * <ul>
     *   <li>{@code "arg0"} (default) — first arg's {@code toString()}</li>
     *   <li>{@code "argN"} — N-th arg's {@code toString()}</li>
     *   <li>{@code "argN.id"} — invokes {@code getId()} on the N-th arg</li>
     * </ul>
     *
     * <p>Wave 92 Bucket A — signature accepts explicit {@code source} so the
     * same parser can resolve both {@link Auditable#entityIdSource()} and
     * {@link Auditable#resourceIdSource()}.</p>
     */
    @SuppressWarnings("squid:S3011") // reflective access to getId is intentional
    private String resolveEntityId(Auditable annotation, Object[] args, String source) {
        if (source == null || source.isBlank() || args.length == 0) {
            return null;
        }
        try {
            // Parse argN[.id]
            String[] parts = source.split("\\.");
            String argPart = parts[0];
            if (!argPart.startsWith("arg")) return null;
            int index = Integer.parseInt(argPart.substring(3));
            if (index >= args.length || args[index] == null) return null;
            Object value = args[index];
            if (parts.length == 1) {
                return truncate(value.toString(), 128);
            }
            // Look up getId()
            if ("id".equals(parts[1])) {
                Method getId = value.getClass().getMethod("getId");
                Object id = getId.invoke(value);
                return id != null ? truncate(id.toString(), 128) : null;
            }
        } catch (Exception ex) {
            log.debug("Could not resolve audit entityId from source='{}': {}", source, ex.getMessage());
        }
        return null;
    }

    private String buildRedactedPayloadJson(String[] paramNames, Object[] args) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = paramNames != null && i < paramNames.length ? paramNames[i] : ("arg" + i);
            if (isSensitive(name)) {
                payload.put(name, "<redacted>");
            } else if (args[i] == null) {
                payload.put(name, null);
            } else {
                payload.put(name, redactValue(args[i]));
            }
        }
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize admin audit payload — falling back to toString summary: {}",
                ex.getMessage());
            return "{\"_serialization_error\":\"" + ex.getClass().getSimpleName() + "\"}";
        }
    }

    /**
     * Redact sensitive sub-fields of complex arg values. For simple types (UUID,
     * String, Number, Boolean), return as-is. For others, attempt a best-effort
     * conversion via Jackson — Jackson will use the type's serializer, which is
     * fine for DTO records.
     */
    @SuppressWarnings("unchecked")
    private Object redactValue(Object value) {
        if (value instanceof CharSequence || value instanceof Number
            || value instanceof Boolean || value instanceof UUID) {
            return value;
        }
        // Convert to a tree, then walk to scrub any field named password/secret/etc.
        try {
            Map<String, Object> tree = MAPPER.convertValue(value, Map.class);
            return scrubMap(tree);
        } catch (Exception ex) {
            return value.getClass().getSimpleName() + "@" + System.identityHashCode(value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> scrubMap(Map<String, Object> input) {
        if (input == null) return null;
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> e : input.entrySet()) {
            if (isSensitive(e.getKey())) {
                out.put(e.getKey(), "<redacted>");
            } else if (e.getValue() instanceof Map<?, ?> nested) {
                out.put(e.getKey(), scrubMap((Map<String, Object>) nested));
            } else {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private boolean isSensitive(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        for (String s : SENSITIVE_NAMES) {
            if (lower.contains(s.toLowerCase())) return true;
        }
        return false;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
