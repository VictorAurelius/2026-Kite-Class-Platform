package com.kiteclass.core.integration.mis;

import java.time.Instant;

/**
 * Result of a {@link MisRosterSource#ping()} call — used by UC-MIS-01
 * (test connection) and by monitoring endpoints in Phase 2.
 *
 * @param provider        which MIS was probed
 * @param connected       {@code true} when credentials + endpoint validated successfully
 * @param providerVersion upstream version string if the MIS exposes one (may be null)
 * @param schoolName      friendly name retrieved from the MIS (null if anonymised endpoint)
 * @param testedAt        when the probe completed
 * @param errorMessage    populated when {@code connected=false}; i18n handled at controller layer
 * @since 2.20.0
 */
public record MisConnectionStatus(
        MisProvider provider,
        boolean connected,
        String providerVersion,
        String schoolName,
        Instant testedAt,
        String errorMessage
) {

    /** Convenience factory for the happy path. */
    public static MisConnectionStatus ok(
            MisProvider provider,
            String providerVersion,
            String schoolName) {
        return new MisConnectionStatus(
                provider,
                true,
                providerVersion,
                schoolName,
                Instant.now(),
                null);
    }

    /** Convenience factory for the failure path. */
    public static MisConnectionStatus failed(MisProvider provider, String errorMessage) {
        return new MisConnectionStatus(
                provider,
                false,
                null,
                null,
                Instant.now(),
                errorMessage);
    }
}
