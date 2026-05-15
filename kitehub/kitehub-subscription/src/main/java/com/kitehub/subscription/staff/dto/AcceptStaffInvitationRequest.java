package com.kitehub.subscription.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload to accept a staff invitation (Wave 80 Bucket B, GAP-561b).
 *
 * <p>Recipient supplies a new password (validated for complexity per
 * {@code pre-launch-auth-hardening-checklist.md} §2.3) and optionally
 * overrides the full name. Email is taken from the invitation row, not
 * the payload — recipient cannot change it.</p>
 *
 * @since Wave 80 — GAP-561b
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptStaffInvitationRequest {

    @NotBlank(message = "WEAK_PASSWORD")
    @Size(min = 12, max = 256, message = "WEAK_PASSWORD")
    private String password;

    /** Optional — defaults to the invitation's full name when omitted. */
    @Size(max = 255, message = "INVALID_FULL_NAME")
    private String fullName;
}
