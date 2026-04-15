package com.kiteclass.core.module.retention;

import lombok.Value;
import org.springframework.stereotype.Service;

/**
 * Reflection-based classifier reading {@link Retention @Retention} annotations on entity
 * classes (ADR-013).
 *
 * <p>Safe default: if no annotation is present, returns
 * {@link RetentionBucket#PURGE_ON_REQUEST} — erring on the side of GDPR compliance over
 * accidental retention. Developers must explicitly annotate classes that need retention.
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.4)
 */
@Service
public class RetentionClassifier {

    private static final String[] NO_FIELDS = new String[0];

    /**
     * Classify a domain entity class by inspecting its {@link Retention @Retention}
     * annotation. Returns the default bucket ({@code PURGE_ON_REQUEST}) when absent.
     */
    public Classification classify(Class<?> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass must not be null");
        }
        Retention annotation = entityClass.getAnnotation(Retention.class);
        if (annotation == null) {
            return new Classification(RetentionBucket.PURGE_ON_REQUEST, NO_FIELDS, false);
        }
        return new Classification(
                annotation.value(),
                annotation.pseudonymizeFields().clone(),
                true);
    }

    /** Result of classifying an entity class. */
    @Value
    public static class Classification {
        RetentionBucket bucket;
        String[] pseudonymizeFields;
        boolean explicit;
    }
}
