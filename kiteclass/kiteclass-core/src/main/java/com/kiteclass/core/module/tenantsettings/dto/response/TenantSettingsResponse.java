package com.kiteclass.core.module.tenantsettings.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for {@link com.kiteclass.core.module.tenantsettings.entity.TenantSettings}.
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSettingsResponse {
    private Long id;
    private String timezone;
    private String locale;
    private String academicYear;
    private String fiscalYear;
    private String schoolType;
    private String address;
    private String phone;
    private String logoUrl;
    private Map<String, Object> themeConfig;
}
