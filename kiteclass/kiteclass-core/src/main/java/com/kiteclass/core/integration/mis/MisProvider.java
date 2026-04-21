package com.kiteclass.core.integration.mis;

/**
 * Supported School Management Information System providers.
 *
 * <p>Adding a new provider requires:
 * <ol>
 *   <li>New enum value here</li>
 *   <li>New {@link MisRosterSource} adapter in the {@code adapters} subpackage</li>
 *   <li>Catalog entry in {@code documents/02-architecture/integrations/school-mis-catalog.md}</li>
 *   <li>ADR addendum under ADR-017 (or new ADR superseding it)</li>
 * </ol>
 *
 * <p>Per BR-MIS-001.
 *
 * @since 2.20.0
 */
public enum MisProvider {

    /** Viettel / Ministry of Education MIS — widest VN K-12 reach. Phase 1 pilot. */
    VNEDU,

    /** Viettel Business SMAS — overlaps VNEDU partner channel. Phase 2. */
    SMAS,

    /** Base Enterprise SMS (private K-12 SaaS). Phase 2. */
    BASE_VN,

    /** Microsoft School Data Sync (international private schools on M365). Phase 2. */
    MS_SDS,

    /** Google Classroom + Admin Directory. Phase 2. */
    GOOGLE_CLASSROOM,

    /** IMS OneRoster 1.2 CSV / REST — vendor-neutral standard. Phase 2. */
    ONEROSTER_CSV
}
