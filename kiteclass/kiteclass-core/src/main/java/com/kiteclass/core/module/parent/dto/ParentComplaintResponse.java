package com.kiteclass.core.module.parent.dto;

import com.kiteclass.core.module.parent.entity.ParentComplaint;

import java.time.Instant;

/**
 * Response payload for {@code POST /api/v1/parent/complaints}.
 *
 * <p>Returns the persisted id + timestamp so the FE can render a
 * "submitted, ticket #N" confirmation. Wave 19 GAP-321c Phase 1C v1.
 *
 * @since 2.19.0
 */
public record ParentComplaintResponse(
        Long id,
        Long studentId,
        String status,
        Instant createdAt) {

    public static ParentComplaintResponse from(ParentComplaint c) {
        return new ParentComplaintResponse(
                c.getId(),
                c.getStudentId(),
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getCreatedAt());
    }
}
