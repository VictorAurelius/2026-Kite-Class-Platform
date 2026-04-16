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
}
