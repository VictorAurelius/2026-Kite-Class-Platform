package com.kitehub.subscription.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from VietQR API.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VietQRResponse {

    /**
     * Response code (00 = success).
     */
    private String code;

    /**
     * Response description.
     */
    @JsonProperty("desc")
    private String description;

    /**
     * Response data containing QR code info.
     */
    private VietQRData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VietQRData {

        /**
         * Bank account number.
         */
        @JsonProperty("acpId")
        private String accountNumber;

        /**
         * Account holder name.
         */
        @JsonProperty("accountName")
        private String accountName;

        /**
         * QR code content (base64 or URL).
         */
        @JsonProperty("qrCode")
        private String qrCode;

        /**
         * QR data URL (image URL).
         */
        @JsonProperty("qrDataURL")
        private String qrDataUrl;
    }
}
