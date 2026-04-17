package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Type of invoice line items.
 *
 * <p>Defines categories of charges on an invoice:
 * <ul>
 *   <li>TUITION: Course tuition fees</li>
 *   <li>MATERIALS: Course materials, textbooks</li>
 *   <li>REGISTRATION_FEE: One-time registration fee</li>
 *   <li>EXAM_FEE: Examination fees</li>
 *   <li>OTHER: Miscellaneous charges</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Getter
public enum InvoiceItemType {

    TUITION("Học phí"),
    MATERIALS("Tài liệu học tập"),
    REGISTRATION_FEE("Phí đăng ký"),
    EXAM_FEE("Phí thi"),
    OTHER("Khác");

    private final String displayNameVi;

    InvoiceItemType(String displayNameVi) {
        this.displayNameVi = displayNameVi;
    }
}
