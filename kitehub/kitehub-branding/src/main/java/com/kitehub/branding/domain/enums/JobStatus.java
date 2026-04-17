package com.kitehub.branding.domain.enums;

/**
 * Branding job status.
 *
 * @since 1.0
 */
public enum JobStatus {
    /**
     * Job queued, waiting to be processed.
     */
    QUEUED,

    /**
     * Job is currently being processed.
     */
    PROCESSING,

    /**
     * Job completed successfully.
     */
    COMPLETED,

    /**
     * Job failed (will retry if retry count < max).
     */
    FAILED,

    /**
     * Job cancelled by user.
     */
    CANCELLED
}
