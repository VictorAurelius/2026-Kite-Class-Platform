package com.kiteclass.core.module.payment.record.entity;

/**
 * Manual payment methods recorded by teachers/admins at trung tâm dạy thêm.
 *
 * <p>Distinct from {@code com.kiteclass.core.module.payment.enums.PaymentMethod} which is gateway-oriented
 * (VNPAY, ZaloPay, etc.). This enum covers offline-first and hybrid payment channels used when
 * recording received tuition fees manually.
 *
 * <p>VN edu market norm: phụ huynh nộp tiền mặt hoặc chuyển khoản (Vietcombank/Techcombank/MB).
 * QR-based VietQR is increasingly common. MoMo covers digital-wallet parents.
 */
public enum PaymentRecordMethod {

    /** Tiền mặt — most common for smaller TT; teacher/receptionist collects at center. */
    CASH,

    /** Chuyển khoản ngân hàng — Vietcombank, Techcombank, MB, ACB, etc. */
    BANK_TRANSFER,

    /** VietQR scan-to-pay — bank-agnostic QR code; increasingly standard in VN edu market. */
    VIETQR,

    /** Ví MoMo — digital wallet common among younger parents. */
    MOMO
}
