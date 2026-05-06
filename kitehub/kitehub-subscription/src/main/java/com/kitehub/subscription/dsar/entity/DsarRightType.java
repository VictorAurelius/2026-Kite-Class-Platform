package com.kitehub.subscription.dsar.entity;

/**
 * PDPL 2023 Article 14 — six data-subject rights.
 *
 * <p>Per `documents/01-business/kitehub/marketing/rules.md` BR-PDPL-DSAR-001.</p>
 *
 * <ul>
 *   <li>{@link #ACCESS} — quyền truy cập (view what data is held)</li>
 *   <li>{@link #RECTIFICATION} — quyền chỉnh sửa (correct inaccurate data)</li>
 *   <li>{@link #ERASURE} — quyền xoá (right to be forgotten)</li>
 *   <li>{@link #PORTABILITY} — quyền chuyển dữ liệu (machine-readable export)</li>
 *   <li>{@link #RESTRICT} — quyền hạn chế xử lý (suspend processing)</li>
 *   <li>{@link #OBJECT} — quyền phản đối xử lý (object to specific use)</li>
 * </ul>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
public enum DsarRightType {
    ACCESS,
    RECTIFICATION,
    ERASURE,
    PORTABILITY,
    RESTRICT,
    OBJECT
}
