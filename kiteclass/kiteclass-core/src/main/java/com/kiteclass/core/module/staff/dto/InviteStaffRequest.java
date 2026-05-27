package com.kiteclass.core.module.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Owner-side payload for issuing a staff invitation.
 *
 * @param email Staff member's email — becomes their login identifier
 * @param role  Role to provision on acceptance — one of {@code STAFF},
 *              {@code TEACHER}, {@code MANAGER}. Owner role excluded
 *              (single-owner-per-tenant invariant).
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
public record InviteStaffRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^(STAFF|TEACHER|MANAGER)$",
                message = "role must be one of STAFF, TEACHER, MANAGER") String role
) {}
