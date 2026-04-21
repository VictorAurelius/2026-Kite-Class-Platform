package com.kiteclass.core.integration.mis;

/**
 * Adapter contract for every School MIS / SMS provider KiteClass integrates
 * with. Each implementation wraps one vendor (VNEDU, SMAS, Base.vn, …) and
 * translates vendor-specific payloads into the neutral {@link RosterImport}
 * DTO.
 *
 * <p>Design decisions:
 * <ul>
 *   <li><strong>Adapter pattern</strong> ({@code .claude/rules/design-patterns.md} §2) —
 *       one class per provider; no shared base class.</li>
 *   <li><strong>Return types are DTOs, not entities</strong> — the import
 *       orchestration service (Phase 2) maps DTOs → JPA entities + applies the
 *       configured conflict strategy (BR-MIS-CONFLICT-001..005).</li>
 *   <li><strong>No {@code throws} of vendor exceptions</strong> — implementations
 *       translate vendor failures into {@link MisIntegrationException}.</li>
 *   <li><strong>Circuit breaker wrapping lives in the implementation</strong>
 *       (BR-MIS-SEC-003), not here — keeps the interface free of resilience
 *       concerns so tests can mock without plumbing.</li>
 * </ul>
 *
 * <p>Phase 1 defines the interface + one skeleton implementation
 * ({@code VneduAdapter}). Phase 2 ships live HTTP calls, persistence,
 * orchestration, and UI.
 *
 * @since 2.20.0
 */
public interface MisRosterSource {

    /**
     * Identifies which vendor this adapter speaks to. Used by the import
     * orchestrator to route based on the tenant's configured provider.
     *
     * @return the provider enum value; never null
     */
    MisProvider provider();

    /**
     * Lightweight connectivity / credentials probe. Called by UC-MIS-01
     * before persisting credentials. Implementations should make the cheapest
     * authenticated call available (e.g. GET /me) and NOT fetch a full roster.
     *
     * <p>Implementations must never throw — translate all failures into a
     * {@link MisConnectionStatus#failed} result.
     *
     * @return connection status (never null)
     */
    MisConnectionStatus ping();

    /**
     * Fetches the full roster for a given academic year. Used by UC-MIS-02
     * (dry-run) and UC-MIS-03 (commit). Implementations may paginate
     * internally but MUST return a complete in-memory DTO — streaming is
     * Phase 2 concern.
     *
     * @param academicYear e.g. {@code "2025-2026"}; must match the tenant's
     *                     active academic year. Required (non-null, non-blank).
     * @return roster DTO (never null; may contain empty lists if academic year
     *     has no data yet)
     * @throws MisIntegrationException when upstream fetch fails; the exception
     *     message is safe for logs but should be translated before exposing to
     *     end users (BR-MIS-SEC-004).
     * @throws IllegalArgumentException if {@code academicYear} is null or blank
     */
    RosterImport fetchRoster(String academicYear);
}
