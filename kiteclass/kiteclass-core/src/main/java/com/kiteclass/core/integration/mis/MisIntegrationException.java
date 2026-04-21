package com.kiteclass.core.integration.mis;

/**
 * Raised by {@link MisRosterSource} implementations when the upstream MIS call
 * fails in a way the adapter cannot recover from (HTTP 5xx, timeout, malformed
 * payload). Caller is expected to translate this into the appropriate
 * {@code ApiResponse} error code (e.g. {@code MIS_ADAPTER_UNREACHABLE} or
 * {@code MIS_IMPORT_TIMEOUT}) before returning to the FE.
 *
 * <p>Per BR-MIS-SEC-004, messages must not contain PII. Adapters should log
 * the vendor-specific detail separately (with redaction) and surface a
 * generic cause here.
 *
 * @since 2.20.0
 */
public class MisIntegrationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final MisProvider provider;

    public MisIntegrationException(MisProvider provider, String message) {
        super(message);
        this.provider = provider;
    }

    public MisIntegrationException(MisProvider provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public MisProvider provider() {
        return provider;
    }
}
