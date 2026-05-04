package com.kiteclass.core.module.childprotection.dto;

import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;

import java.time.Instant;

/**
 * REST DTO for {@link Vetting} reads (GET / POST / PATCH responses).
 *
 * <p>Fields are returned only to safeguarding-officer callers (RBAC enforced
 * by {@code VettingController}). Encrypted fields are decrypted on read by
 * {@code AesGcmAttributeConverter}.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
public record VettingResponse(
        Long id,
        Long teacherId,
        VettingStatus status,
        String lltpNumber,
        String policeCheckDetails,
        Instant submittedAt,
        Instant interviewedAt,
        Instant decidedAt,
        Instant expiresAt,
        Long decidedByUserId,
        Instant createdAt,
        Instant updatedAt
) {

    public static VettingResponse from(Vetting v) {
        return new VettingResponse(
                v.getId(),
                v.getTeacherId(),
                v.getStatus(),
                v.getLltpNumber(),
                v.getPoliceCheckDetails(),
                v.getSubmittedAt(),
                v.getInterviewedAt(),
                v.getDecidedAt(),
                v.getExpiresAt(),
                v.getDecidedByUserId(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }
}
