package com.kitehub.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/platform/admin/instances/{id}/rollback-migration}.
 *
 * <p>Admin-only — used by ops when a payment reversal is confirmed out-of-band.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackRequest {

    @NotBlank(message = "reason is required")
    @Size(max = 500)
    private String reason;

    @Size(max = 128)
    private String referenceId;
}
