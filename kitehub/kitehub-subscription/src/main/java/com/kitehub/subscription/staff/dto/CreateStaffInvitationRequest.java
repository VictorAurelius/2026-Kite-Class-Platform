package com.kitehub.subscription.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to issue a new staff invitation.
 *
 * <p>Schema source-of-truth:
 * {@code documents/01-business/roles/api-contract.md} §POST /api/v1/staff-invitations.</p>
 *
 * @since Wave 79 — GAP-561
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffInvitationRequest {

    @NotBlank(message = "INVALID_EMAIL")
    @Email(message = "INVALID_EMAIL")
    @Size(max = 255, message = "INVALID_EMAIL")
    private String email;

    @NotBlank(message = "INVALID_FULL_NAME")
    @Size(min = 2, max = 255, message = "INVALID_FULL_NAME")
    private String fullName;
}
