package com.kitehub.gateway.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Minimal branding payload used by the gateway's branded error pages (GAP-032).
 *
 * @since Wave 4 (GAP-032)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayBranding implements Serializable {

    private static final long serialVersionUID = 1L;

    private String displayName;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;

    public static GatewayBranding defaults() {
        return GatewayBranding.builder()
                .displayName("KiteHub")
                .logoUrl("")
                .primaryColor("#3B82F6")
                .secondaryColor("#8B5CF6")
                .build();
    }
}
