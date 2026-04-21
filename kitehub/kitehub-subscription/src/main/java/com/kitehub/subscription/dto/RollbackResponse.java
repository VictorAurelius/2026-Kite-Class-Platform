package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for the admin rollback-migration endpoint.
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackResponse {

    private UUID instanceId;

    private MigrationPhase migrationPhase;

    private LocalDateTime rolledBackAt;

    private InstanceStatus newStatus;

    private LocalDateTime trialExpiresAt;
}
