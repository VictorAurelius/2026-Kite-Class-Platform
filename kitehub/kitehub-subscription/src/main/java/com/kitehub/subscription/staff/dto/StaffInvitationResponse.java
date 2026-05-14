package com.kitehub.subscription.staff.dto;

import com.kitehub.subscription.staff.entity.StaffInvitation;
import com.kitehub.subscription.staff.entity.StaffInvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read model for staff invitation.
 *
 * <p>Schema source-of-truth: {@code documents/01-business/roles/api-contract.md}.
 * Never exposes {@code tokenHash} — that's server-internal.</p>
 *
 * @since Wave 79 — GAP-561
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffInvitationResponse {

    private UUID id;
    private UUID tenantId;
    private String email;
    private String fullName;
    private String role; // always "STAFF"
    private StaffInvitationStatus status;
    private UUID invitedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime acceptedAt;

    public static StaffInvitationResponse from(StaffInvitation inv) {
        return StaffInvitationResponse.builder()
                .id(inv.getId())
                .tenantId(inv.getTenantId())
                .email(inv.getEmail())
                .fullName(inv.getFullName())
                .role("STAFF")
                .status(inv.getStatus())
                .invitedBy(inv.getInvitedBy())
                .createdAt(inv.getCreatedAt())
                .expiresAt(inv.getExpiresAt())
                .acceptedAt(inv.getAcceptedAt())
                .build();
    }
}
