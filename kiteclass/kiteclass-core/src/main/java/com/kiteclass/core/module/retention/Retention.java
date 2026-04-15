package com.kiteclass.core.module.retention;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Class-level retention classification marker read by {@link RetentionClassifier}.
 *
 * <p>Runtime retention so reflection-based lookup works from
 * {@link RetentionClassifier#classify(Class)}.
 *
 * <p>Usage:
 * <pre>{@code
 *   @Retention(value = RetentionBucket.PURGE_ON_REQUEST)
 *   public class BrandingResource extends BaseEntity { ... }
 *
 *   @Retention(value = RetentionBucket.RETAIN_WITH_PSEUDO,
 *              pseudonymizeFields = {"email", "phone"})
 *   public class Invoice extends BaseEntity { ... }
 * }</pre>
 *
 * <p><b>Note:</b> this annotation intentionally shadows {@code java.lang.annotation.Retention}
 * when imported into domain entities — callers should use the fully-qualified form there to
 * avoid collision (see {@code BrandingResource}, {@code AuditLog}, {@code OutboxEvent}).
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.4, ADR-013)
 */
@Target(ElementType.TYPE)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Retention {

    /** Retention bucket applied to all rows of the annotated entity class. */
    RetentionBucket value();

    /**
     * Column / field names that must be pseudonymized (HMAC-hashed) when the bucket is
     * {@link RetentionBucket#RETAIN_WITH_PSEUDO}. Ignored for other buckets.
     */
    String[] pseudonymizeFields() default {};
}
