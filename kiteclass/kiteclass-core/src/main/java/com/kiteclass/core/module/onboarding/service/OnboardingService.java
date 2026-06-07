package com.kiteclass.core.module.onboarding.service;

import com.kiteclass.core.module.onboarding.dto.SampleDataResponse;

/**
 * Service interface for first-time onboarding helpers.
 *
 * <p>Lets a non-technical Owner seed a believable demo data set so an empty dashboard
 * ("0 students, 0 classes, 0 teachers") becomes a guided, explorable workspace.
 *
 * @author KiteClass Team
 * @since 3.17.0
 */
public interface OnboardingService {

    /**
     * Imports a minimal Vietnamese-edu demo data set for the current tenant.
     *
     * <p>Creates 1 teacher + 1 course + 1 class + 3 students + 3 enrollments, reusing the
     * normal module services (so all validation + tenant scoping applies). The operation is
     * idempotent: if the marker sample course already exists for the tenant, nothing is
     * created and {@link SampleDataResponse#noOp()} is returned.
     *
     * <p>Tenant is resolved from {@code TenantContext.getCurrentTenant()}.
     *
     * @return a {@link SampleDataResponse} describing what was created (or already present)
     */
    SampleDataResponse importSampleData();
}
