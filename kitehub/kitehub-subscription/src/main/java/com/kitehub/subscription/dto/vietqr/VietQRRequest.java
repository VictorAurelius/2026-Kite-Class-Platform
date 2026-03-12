package com.kitehub.subscription.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for VietQR API.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VietQRRequest {

    /**
     * Bank ID (e.g., "VCB" for Vietcombank, "TCB" for Techcombank).
     */
    private String acqId;

    /**
     * Account number.
     */
    private String accountNo;

    /**
     * Account holder name.
     */
    private String accountName;

    /**
     * Payment amount in VND.
     */
    private Long amount;

    /**
     * Payment description/content.
     */
    private String addInfo;

    /**
     * QR code template (optional).
     * Values: "compact", "print", "qr_only"
     */
    private String template;
}
