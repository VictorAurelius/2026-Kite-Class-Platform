package com.kiteclass.core.module.clazz;

/**
 * Reason categories for class reschedule operations.
 *
 * <p>Enumerates the 6 most common reschedule reasons surfaced by VN edu market
 * research (Wave beta-readiness-4 Bucket D — GAP-291). Mandatory dropdown on FE
 * reschedule modal; analytics aggregation per category in post-Phase-1.5 dashboards.
 *
 * <p>Per cross-bucket LOCKED decision §3.6, this enum is stored as VARCHAR(64)
 * in the {@code classes.reschedule_reason_category} column (no Postgres ENUM
 * type — keeps migration backward-compatible).
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
public enum RescheduleReasonCategory {

    /** Giáo viên ốm/bận đột xuất */
    GV_OM_BAN_DOT_XUAT("Giáo viên ốm/bận đột xuất"),

    /** Phòng học không khả dụng (sửa chữa, sự cố cơ sở vật chất) */
    PHONG_HOC_KHONG_KHA_DUNG("Phòng học không khả dụng"),

    /** Mất điện / mất Internet */
    MAT_DIEN_INTERNET("Mất điện / mất Internet"),

    /** Lễ Tết / nghỉ chính thức theo lịch nhà nước */
    LE_TET_NGHI_CHINH_THUC("Lễ Tết / nghỉ chính thức"),

    /** Học sinh xin nghỉ tập thể (sự kiện trường, dịch bệnh, v.v.) */
    HOC_SINH_XIN_NGHI_TAP_THE("Học sinh xin nghỉ tập thể"),

    /** Lý do khác (yêu cầu nhập reason_notes) */
    LY_DO_KHAC("Lý do khác");

    private final String displayNameVi;

    RescheduleReasonCategory(String displayNameVi) {
        this.displayNameVi = displayNameVi;
    }

    /**
     * Returns the Vietnamese display name for this reason category.
     *
     * @return display name in Vietnamese (used by FE dropdown labels and email templates)
     */
    public String getDisplayNameVi() {
        return displayNameVi;
    }
}
