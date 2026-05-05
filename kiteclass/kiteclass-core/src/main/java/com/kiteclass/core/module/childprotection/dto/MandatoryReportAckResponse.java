package com.kiteclass.core.module.childprotection.dto;

import java.time.Instant;

/**
 * Response payload for the mandatory-report ack endpoint.
 *
 * @param incidentId      the incident the report applies to
 * @param referenceNumber echoed reference number
 * @param reportedAt      echoed external report timestamp
 * @param auditLogId      surrogate id of the persisted audit log entry
 * @param contentHash     hex-encoded SHA-256 chain hash for the new entry
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
public record MandatoryReportAckResponse(
        Long incidentId,
        String referenceNumber,
        Instant reportedAt,
        Long auditLogId,
        String contentHash) {
}
