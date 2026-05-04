package com.kitehub.platform.domain.enums;

/**
 * Operating-model discriminator for KiteClass tenants.
 *
 * <p>KiteClass supports two fundamentally different operating models:
 * <ul>
 *   <li>{@link #CENTER} — private trung tâm (legacy default, per-day
 *       attendance, single-subject grading).</li>
 *   <li>{@link #K12_SCHOOL} — trường công lập per TT 22/2021 + TT 32/2018
 *       (GDPT 2018): per-period attendance, multi-subject gradebook,
 *       Tổ trưởng approval chain, MOET học bạ format.</li>
 * </ul>
 *
 * <p>Phase 1A (Wave 18b1, GAP-323) introduces the discriminator column. The
 * service layer in kiteclass-core gates which attendance / grading model
 * applies. CHECK constraint pairing this enum with the K-12 write paths will
 * land in Phase 1B (GAP-323b).
 *
 * @since GAP-323 Phase 1A (Wave 18b1)
 */
public enum VerticalType {

    /**
     * Private trung tâm — legacy default. Per-day attendance, single-subject
     * grading. Existing tenants are migrated to this value automatically.
     */
    CENTER,

    /**
     * K-12 trường công lập per TT 22/2021/TT-BGDĐT + TT 32/2018/TT-BGDĐT
     * (chương trình GDPT 2018). Per-period attendance, 12-15 môn gradebook,
     * Tổ trưởng approval chain, MOET học bạ.
     */
    K12_SCHOOL
}
