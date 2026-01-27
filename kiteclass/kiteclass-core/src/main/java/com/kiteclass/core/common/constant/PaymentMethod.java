package com.kiteclass.core.common.constant;

import lombok.Getter;

/**
 * Payment method values.
 *
 * <p>Supported payment methods with availability flags:
 * <ul>
 *   <li>CASH: Cash payment</li>
 *   <li>BANK_TRANSFER: Bank transfer</li>
 *   <li>MOMO: MoMo e-wallet</li>
 *   <li>VNPAY: VNPay QR payment</li>
 *   <li>ZALOPAY: ZaloPay e-wallet</li>
 *   <li>CREDIT_CARD: Credit card (not available in VN)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public enum PaymentMethod {

    CASH("Tiền mặt", "cash", true),
    BANK_TRANSFER("Chuyển khoản", "bank", true),
    MOMO("Ví MoMo", "momo", true),
    VNPAY("VNPay QR", "vnpay", true),
    ZALOPAY("ZaloPay", "zalopay", true),
    CREDIT_CARD("Thẻ tín dụng", "card", false);  // Not available in VN

    private final String displayNameVi;
    private final String code;
    private final boolean availableInVN;

    PaymentMethod(String displayNameVi, String code, boolean availableInVN) {
        this.displayNameVi = displayNameVi;
        this.code = code;
        this.availableInVN = availableInVN;
    }
}
