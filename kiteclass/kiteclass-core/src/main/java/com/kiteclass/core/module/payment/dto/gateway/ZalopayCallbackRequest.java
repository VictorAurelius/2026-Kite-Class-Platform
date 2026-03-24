package com.kiteclass.core.module.payment.dto.gateway;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Typed DTO for ZaloPay payment webhook callback.
 *
 * <p>Maps known ZaloPay callback fields while preserving any additional
 * parameters via {@link #extraParams}. Provides {@link #toMap()} for
 * backward-compatible usage with {@code PaymentService}.
 *
 * @since 1.1.0
 */
@Data
public class ZalopayCallbackRequest {

    private String data;
    private String mac;
    private String type;

    @JsonProperty("app_id")
    private String appId;

    @JsonProperty("app_trans_id")
    private String appTransId;

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
        putIfNotNull(map, "data", data);
        putIfNotNull(map, "mac", mac);
        putIfNotNull(map, "type", type);
        putIfNotNull(map, "app_id", appId);
        putIfNotNull(map, "app_trans_id", appTransId);
        return map;
    }

    private static void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
