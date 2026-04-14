package com.kiteclass.core.module.academicyear.entity;

/**
 * Status of an academic year.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>UPCOMING: Created, not yet started (before startDate)</li>
 *   <li>CURRENT: Active academic year (startDate ≤ today ≤ endDate)</li>
 *   <li>COMPLETED: Past endDate, grades finalized</li>
 * </ul>
 *
 * <p>Only 1 CURRENT year per tenant at a time (enforced at service level).
 *
 * @since 3.15.0 (GAP-053, ADR-002)
 */
public enum AcademicYearStatus {
    UPCOMING,
    CURRENT,
    COMPLETED
}
