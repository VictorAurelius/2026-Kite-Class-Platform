package com.kitehub.shared.logging;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field whose value MUST NOT appear in serialized log output.
 *
 * <p>When Jackson serializes a DTO carrying a {@code @Redact} field, it emits
 * {@code "<REDACTED>"} regardless of the underlying value. Combine with structured
 * logging ({@code log.info("event", kv("user", user))}) to suppress accidental PII
 * leaks at the boundary between domain objects and the logging pipeline.</p>
 *
 * <p>For free-form log messages — i.e. when the field's value is interpolated into
 * a {@code String} before logging — the {@link PIIScrubber} regex defence still
 * applies. This annotation is the explicit, type-safe primary defence; the scrubber
 * is the regex-based safety net.</p>
 *
 * <p>Scope: GAP-116. Spec: {@code logs-format-standard.md} §3.2.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@JacksonAnnotationsInside
@JsonSerialize(using = RedactSerializer.class)
public @interface Redact {
}
