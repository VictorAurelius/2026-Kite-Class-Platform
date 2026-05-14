package com.kitehub.subscription.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller / service method as an auditable admin action (GAP-521 /
 * OWASP A07). When applied, {@link AdminAuditAspect} captures admin user id,
 * action label, target entity reference, request IP + UA, and an arg-payload
 * snapshot into the {@code admin_audit_log} table.
 *
 * <p>Apply on top-level admin endpoints (approve, reject, suspend, modify-config,
 * etc.), NOT on read-only endpoints (list / view).</p>
 *
 * <p>Usage:
 * <pre>
 * &#64;Auditable(action = "BETA_REQUEST_APPROVE", entityType = "beta_access_request")
 * public BetaAccessRequest approve(&#64;PathVariable UUID id, ...) { ... }
 * </pre>
 *
 * @since 1.0.0 (Wave 72a GAP-521)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Short, stable action code (UPPER_SNAKE_CASE, ≤64 chars). Used for indexed
     * queries on {@code admin_audit_log.action}.
     */
    String action();

    /**
     * Optional entity type label (e.g., "beta_access_request", "instance",
     * "subscription"). Empty when the action does not target a single entity.
     */
    String entityType() default "";

    /**
     * Optional SpEL expression that resolves to the target entity id from the
     * method arguments. Default reads the first argument's {@code toString()}.
     * Use {@code #root.args[0]}, {@code #root.args[0].id}, etc.
     *
     * <p>Kept as a String literal (not full SpEL evaluation) for v1 to avoid
     * adding the SpEL dependency surface; the aspect interprets a small subset:
     * empty → arg0.toString(), or {@code "arg0.id"} → invokes {@code getId()}.</p>
     */
    String entityIdSource() default "arg0";
}
