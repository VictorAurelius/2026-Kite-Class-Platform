package com.kiteclass.core.module.onboarding.dto;

/**
 * Response DTO for the onboarding sample-data import.
 *
 * <p>Returned by {@code POST /api/v1/onboarding/sample-data}. Reports how many demo
 * entities were created for the current tenant, plus an idempotency flag so the
 * frontend can distinguish a fresh import from a no-op re-import.
 *
 * @param alreadyImported {@code true} if sample data already existed for the tenant
 *                        (nothing was created this call); {@code false} on a fresh import
 * @param teachersCreated number of demo teachers created (1 on fresh import, 0 otherwise)
 * @param coursesCreated  number of demo courses created (1 on fresh import, 0 otherwise)
 * @param classesCreated  number of demo classes created (1 on fresh import, 0 otherwise)
 * @param studentsCreated number of demo students created (3 on fresh import, 0 otherwise)
 * @param enrollmentsCreated number of demo enrollments created (3 on fresh import, 0 otherwise)
 * @author KiteClass Team
 * @since 3.17.0
 */
public record SampleDataResponse(
        boolean alreadyImported,
        int teachersCreated,
        int coursesCreated,
        int classesCreated,
        int studentsCreated,
        int enrollmentsCreated
) {

    /**
     * Builds the response for a successful fresh import (1 teacher + 1 course + 1 class
     * + 3 students + 3 enrollments).
     *
     * @return a fresh-import response
     */
    public static SampleDataResponse freshImport() {
        return new SampleDataResponse(false, 1, 1, 1, 3, 3);
    }

    /**
     * Builds the response for an idempotent no-op (sample data already present).
     *
     * @return an already-imported response with all counts zero
     */
    public static SampleDataResponse noOp() {
        return new SampleDataResponse(true, 0, 0, 0, 0, 0);
    }
}
