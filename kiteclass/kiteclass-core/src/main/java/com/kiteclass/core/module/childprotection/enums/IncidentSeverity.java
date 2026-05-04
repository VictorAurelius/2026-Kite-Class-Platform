package com.kiteclass.core.module.childprotection.enums;

/**
 * Incident severity classification — drives mandatory-reporting + escalation
 * routing per Luật Trẻ em 2016 Đ.51 (mandatory reporting ≤24h for serious
 * cases) and Decree 56/2017/NĐ-CP.
 *
 * <p>Phase 1A persists the value; auto-suggest banner ("Đ.51 — báo cáo Tổng đài
 * 111 + công an địa phương ≤24h") for {@link #CRITICAL} + abuse categories is
 * deferred to GAP-322c (Wave 18b3).
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
public enum IncidentSeverity {

    /** Minor concern, school-internal handling sufficient. */
    LOW,

    /** Pattern of behavior; requires homeroom + parent involvement. */
    MEDIUM,

    /** Significant harm or risk; safeguarding officer escalation required. */
    HIGH,

    /**
     * Suspected abuse / grooming / CSAM. Triggers Đ.51 mandatory-reporting
     * banner in Phase 1B; safeguarding officer + Hiệu trưởng + counselor
     * are the only roles that may decrypt the incident record.
     */
    CRITICAL
}
