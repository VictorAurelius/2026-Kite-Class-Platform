package com.kiteclass.core.module.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public claim payload — the invitee posts this against the public claim
 * endpoint to set their initial password and complete profile.
 *
 * <p>User creation lives in the gateway service, not in kiteclass-core. This
 * endpoint marks the invitation ACCEPTED and returns the credential payload
 * the gateway needs to provision the User row + role binding.
 *
 * @param fullName Staff member's display name in Vietnamese narrative
 *                 (e.g., {@code Trần Thị Hồng}); supports VN diacritics per
 *                 {@code vn-localization-audit-checklist.md} §5
 * @param password Initial password — strength validated at gateway layer
 *                 (≥8 chars + complexity)
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
public record AcceptStaffInviteRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank
        @Size(min = 8, max = 128)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least 1 letter + 1 digit")
        String password
) {}
