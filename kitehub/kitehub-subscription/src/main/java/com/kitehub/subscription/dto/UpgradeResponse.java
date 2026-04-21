package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.MigrationPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for {@code POST /api/platform/instances/{id}/upgrade} — 202 Accepted.
 *
 * <p>Client should poll {@code GET /trial-status} using {@link #pollUrl} until
 * {@code migrationPhase == COMPLETED}.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeResponse {

    private UUID instanceId;

    private MigrationPhase migrationPhase;

    private LocalDateTime startedAt;

    /** Per SLA T2P-03 — p95 target. */
    private int estimatedCompletionSeconds;

    /** Absolute path the FE should poll. */
    private String pollUrl;
}
