package com.kitehub.subscription.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response shape for a successful accept-invitation call (Wave 80 Bucket B,
 * GAP-561b).
 *
 * <p>Returns the newly-created user shell so the FE can immediately redirect
 * to the login page (or, future Wave 81+, hand back a session token if the
 * accept flow auto-logs-in).</p>
 *
 * @since Wave 80 — GAP-561b
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptStaffInvitationResponse {

    private UUID userId;
    private UUID tenantId;
    private String email;
    private String fullName;
    private String role;
}
