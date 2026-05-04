package com.kiteclass.core.module.childprotection.enums;

/**
 * Lifecycle status for staff vetting records (LLTP / police-check / interview).
 *
 * <p>State machine (Phase 1B foundation — service-layer enforced):
 * <pre>
 *  PENDING ──submit──▶ SUBMITTED ──interview──▶ INTERVIEW_DONE
 *                                                    │
 *                              ┌─────────────────────┼────────────────────┐
 *                              ▼                                          ▼
 *                          APPROVED ──expiry──▶ EXPIRED              REJECTED (terminal)
 * </pre>
 *
 * <p>Compliance: Decree 56/2017/NĐ-CP §Đ.25 — vetting required for adults
 * working with minors. Staff teachers without status APPROVED are blocked from
 * student PII endpoints (BR-VETTING-003).
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
public enum VettingStatus {

    /** Initial state — record created, no documents submitted yet. */
    PENDING,

    /** HR/admin uploaded LLTP + bằng + CCCD; awaiting interview. */
    SUBMITTED,

    /** Interview conducted; safeguarding officer awaiting decision. */
    INTERVIEW_DONE,

    /** Officer approved — staff member cleared for student-PII access. Terminal except for EXPIRED. */
    APPROVED,

    /** Officer rejected — record kept for audit; staff blocked. Terminal. */
    REJECTED,

    /** APPROVED record passed its expiry date; staff blocked until re-vetting. */
    EXPIRED
}
