package com.kiteclass.core.module.childprotection.enums;

/**
 * Incident category — used by safeguarding officer for classification +
 * statistics, and (Phase 1B) to drive mandatory-reporting routing.
 *
 * <p>Values map to Luật Trẻ em 2016 Đ.4 definitions of "xâm hại" (abuse) and
 * Decree 56/2017 Art 13 categories of "hành vi gây nguy hại cho trẻ em".
 *
 * <p>Phase 1A persists; reporting + classification UI are deferred to Phase 1B.
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
public enum IncidentCategory {

    /** Bắt nạt / bullying among peers (Đ.6 quyền được bảo vệ). */
    BULLYING,

    /** Physical, emotional, or sexual abuse (Đ.4 §6 xâm hại trẻ em). */
    ABUSE,

    /** Online grooming / luring of minors (Đ.54 môi trường mạng). */
    GROOMING,

    /**
     * Child sexual abuse material — strictest handling: Tổng đài 111 + công an
     * mandatory ≤24h per Đ.51 + Bộ luật Hình sự Đ.147.
     */
    CSAM,

    /** Other category not covered above. Triage via safeguarding officer. */
    OTHER
}
