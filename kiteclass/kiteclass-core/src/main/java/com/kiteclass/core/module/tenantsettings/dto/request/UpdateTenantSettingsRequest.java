package com.kiteclass.core.module.tenantsettings.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating tenant settings (PUT — upsert with provided-field-wins).
 *
 * <p>All fields optional; null fields keep the existing value (PATCH-style merge so
 * partial updates from the settings UI don't clobber unrelated fields).
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTenantSettingsRequest {

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    @Size(max = 10, message = "Locale must not exceed 10 characters")
    private String locale;

    @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Academic year must be in 'YYYY-YYYY' format")
    @Size(max = 20, message = "Academic year must not exceed 20 characters")
    private String academicYear;

    @Size(max = 20, message = "Fiscal year must not exceed 20 characters")
    private String fiscalYear;

    @Pattern(regexp = "^(CENTER|K12|UNIVERSITY|OTHER)$",
            message = "School type must be CENTER, K12, UNIVERSITY, or OTHER")
    private String schoolType;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 1000, message = "Logo URL must not exceed 1000 characters")
    private String logoUrl;

    private Map<String, Object> themeConfig;
}
