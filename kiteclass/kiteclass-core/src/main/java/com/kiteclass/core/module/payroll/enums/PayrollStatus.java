package com.kiteclass.core.module.payroll.enums;

import lombok.Getter;

/**
 * Lifecycle status of a {@code PayrollPeriod} record.
 *
 * <ul>
 *   <li>{@link #DRAFT}: Calculation engine has produced the gross/net figures but
 *       admin has not approved them. Default state on creation. Phase 1 only
 *       creates DRAFT records — there is no approve/pay UI yet.
 *   <li>{@link #APPROVED}: Admin reviewed + approved. Locked from re-calculation.
 *       Phase 2 (GAP-057b) ships the approve UI + audit trail.
 *   <li>{@link #PAID}: Bank transfer sent. Phase 2 (GAP-057b) ships bank export
 *       + PAID transition.
 * </ul>
 *
 * <p>Allowed transitions: {@code DRAFT → APPROVED → PAID}. Reversal not
 * supported in Phase 1; corrections create a new period (Phase 2 design).
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@Getter
public enum PayrollStatus {

    /**
     * Calculated but not approved. Phase 1 default.
     */
    DRAFT("Bản nháp", "Calculated, awaiting admin approval"),

    /**
     * Admin approved. Phase 2 (GAP-057b).
     */
    APPROVED("Đã duyệt", "Approved by admin, locked"),

    /**
     * Bank transfer sent. Phase 2 (GAP-057b).
     */
    PAID("Đã thanh toán", "Bank transfer completed");

    private final String displayNameVi;
    private final String description;

    PayrollStatus(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }
}
