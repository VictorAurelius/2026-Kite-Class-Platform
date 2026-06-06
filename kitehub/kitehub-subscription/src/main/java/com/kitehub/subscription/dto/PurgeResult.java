package com.kitehub.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Result of a purge operation on an instance.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurgeResult {
    private UUID instanceId;
    private String subdomain;
    private PurgeStatus status;
    private boolean databaseDropped;
    private int backupFilesDeleted;
    private int emailLogsDeleted;
    private boolean brandingCleanupPublished;
    private String errorMessage;
    private LocalDateTime purgedAt;

    /** GAP-954: count of tenant MinIO/S3 objects (logos + branding assets) purged by prefix. */
    private int s3ObjectsDeleted;

    /** GAP-954: whether the tenant DNS / custom-domain record was cleared. */
    private boolean dnsRecordCleared;

    /** GAP-954: whether the TENANT_DELETED audit row was written (PDPL Art 23). */
    private boolean tenantDeletedAuditWritten;
}
