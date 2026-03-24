package com.kiteclass.core.module.payment.dto.gateway;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Typed DTO for MoMo payment webhook callback.
 *
 * <p>Maps known MoMo callback fields while preserving any additional
 * parameters via {@link #extraParams}. Provides {@link #toMap()} for
 * backward-compatible usage with {@code PaymentService}.
 *
 * @since 1.1.0
 */
@Data
public class MomoCallbackRequest {

    private String partnerCode;
    private String orderId;
    private String requestId;
    private String amount;
    private String orderInfo;
    private String orderType;
    private String transId;
    private String resultCode;
    private String message;
    private String payType;
    private String responseTime;
    private String extraData;
    private String signature;

    private final Map<String, String> extraParams = new HashMap<>();

    @JsonAnySetter
    public void setExtraParam(String key, String value) {
        extraParams.put(key, value);
    }

    /**
     * Converts this DTO to a flat Map for backward compatibility.
     *
     * @return all fields as a Map
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>(extraParams);
        putIfNotNull(map, "partnerCode", partnerCode);
        putIfNotNull(map, "orderId", orderId);
        putIfNotNull(map, "requestId", requestId);
        putIfNotNull(map, "amount", amount);
        putIfNotNull(map, "orderInfo", orderInfo);
        putIfNotNull(map, "orderType", orderType);
        putIfNotNull(map, "transId", transId);
        putIfNotNull(map, "resultCode", resultCode);
        putIfNotNull(map, "message", message);
        putIfNotNull(map, "payType", payType);
        putIfNotNull(map, "responseTime", responseTime);
        putIfNotNull(map, "extraData", extraData);
        putIfNotNull(map, "signature", signature);
        return map;
    }

    private static void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
