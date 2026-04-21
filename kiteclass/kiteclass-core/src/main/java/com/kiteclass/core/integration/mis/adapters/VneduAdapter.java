package com.kiteclass.core.integration.mis.adapters;

import com.kiteclass.core.integration.mis.MisConnectionStatus;
import com.kiteclass.core.integration.mis.MisIntegrationException;
import com.kiteclass.core.integration.mis.MisProvider;
import com.kiteclass.core.integration.mis.MisRosterSource;
import com.kiteclass.core.integration.mis.RosterImport;

import java.time.Instant;
import java.util.List;

/**
 * VNEDU (vnedu.vn) pilot adapter — Phase 1 SKELETON.
 *
 * <p>Intentionally does NOT call the VNEDU partner API yet. The live
 * implementation lands in Phase 2 once the partner MoU is signed and
 * sandbox credentials are available (see ADR-017 §Implementation Notes).
 *
 * <p>What the skeleton DOES do:
 * <ul>
 *   <li>Locks the {@link MisRosterSource} contract shape so downstream
 *       orchestration code in Phase 2 can compile against a real type.</li>
 *   <li>Provides a deterministic {@link #ping()} result for unit tests that
 *       exercise the controller happy path without a live backend.</li>
 *   <li>Throws {@link UnsupportedOperationException} in
 *       {@link #fetchRoster(String)} so accidentally wiring this in production
 *       fails loud rather than silently returning empty rosters.</li>
 * </ul>
 *
 * <p>Phase 2 TODO checklist (tracked in GAP-200 Phase 2 successor):
 * <ul>
 *   <li>Inject {@code VneduClient} (HTTP) + {@code MisCredentialsService}.</li>
 *   <li>Implement {@link #ping()} as authenticated GET {@code /me}.</li>
 *   <li>Implement {@link #fetchRoster} with paginated GET {@code /students}
 *       + {@code /teachers} + {@code /classes} endpoints.</li>
 *   <li>Wrap with {@code @CircuitBreaker}, {@code @Bulkhead}, {@code @Retry}
 *       (BR-MIS-SEC-003).</li>
 *   <li>Redact PII in log statements (BR-MIS-SEC-004).</li>
 *   <li>Add Spring {@code @Component} + {@code @ConditionalOnProperty} gating.</li>
 * </ul>
 *
 * @since 2.20.0 (Phase 1, 2026-04-21)
 */
public class VneduAdapter implements MisRosterSource {

    /**
     * Placeholder identifier used by {@link #ping()} until the real VNEDU
     * API schema is wired. Phase 2 replaces this with the value returned
     * by {@code GET /v1/partner/version}.
     */
    static final String PLACEHOLDER_PROVIDER_VERSION = "skeleton-0.1";

    @Override
    public MisProvider provider() {
        return MisProvider.VNEDU;
    }

    @Override
    public MisConnectionStatus ping() {
        // TODO(Phase 2): replace with authenticated GET /me against VNEDU partner API.
        // For Phase 1 the skeleton always returns "not connected" so code paths
        // that accidentally reach production light up red dashboards rather than
        // claiming a fake-healthy connection.
        return MisConnectionStatus.failed(
                provider(),
                "VNEDU adapter not yet implemented — Phase 2 (see ADR-017)");
    }

    @Override
    public RosterImport fetchRoster(String academicYear) {
        if (academicYear == null || academicYear.isBlank()) {
            throw new IllegalArgumentException("academicYear must not be null or blank");
        }
        // TODO(Phase 2): implement paginated fetch of students/teachers/classes/enrollments.
        // Until then we throw so nobody can misconfigure the orchestrator to ship
        // empty rosters into production silently.
        throw new MisIntegrationException(
                provider(),
                "VNEDU roster fetch not implemented (Phase 1 skeleton). "
                        + "See ADR-017 §Implementation Notes.");
    }

    /**
     * Test-only helper — builds an empty {@link RosterImport} shaped correctly
     * for the given academic year. Useful for Phase 2 orchestrator tests that
     * need a non-null payload without hitting the real VNEDU API.
     *
     * <p>Not part of the public interface contract and therefore not routed
     * through {@link MisRosterSource}.
     *
     * @param academicYear required — propagated to the returned DTO
     * @return empty roster tagged with this adapter's provider + current time
     */
    public RosterImport buildEmptyRosterForTests(String academicYear) {
        if (academicYear == null || academicYear.isBlank()) {
            throw new IllegalArgumentException("academicYear must not be null or blank");
        }
        return new RosterImport(
                provider(),
                Instant.now(),
                academicYear,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
