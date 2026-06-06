package com.kiteclass.core.module.tenantsettings.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Per-tenant (trường học) settings — exactly ONE row per instance.
 *
 * <p>Holds the configuration that was previously scattered across {@code Instance}
 * (organizationName / contactEmail) or hard-coded global ({@code system_config.locale}):
 * timezone, locale, Năm học (academic year), fiscal year, school type, address, phone,
 * logo URL, and a free-form theme config blob.
 *
 * <p>Multi-tenant isolation: {@code instance_id} (from {@link BaseEntity}) is the tenant
 * scope AND the 1:1 key — enforced by a unique index + RLS tenant_isolation policy (V90).
 * The {@code academicYear} field is auto-filled at first read / provision via
 * {@link com.kiteclass.core.module.tenantsettings.util.AcademicYearCalculator}.
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@Entity
@Table(
        name = "tenant_settings",
        indexes = {
                @Index(name = "uk_tenant_settings_instance_id", columnList = "instance_id", unique = true),
                @Index(name = "idx_tenant_settings_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSettings extends BaseEntity {

    /** Default IANA timezone for VN tenants. */
    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    /** Default locale (Vietnamese). */
    public static final String DEFAULT_LOCALE = "vi";

    /**
     * Tenant timezone (IANA identifier, e.g. {@code Asia/Ho_Chi_Minh}).
     */
    @NotBlank(message = "Timezone is required")
    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = DEFAULT_TIMEZONE;

    /**
     * Tenant locale / display language (e.g. {@code vi}, {@code en}).
     */
    @NotBlank(message = "Locale is required")
    @Column(name = "locale", nullable = false, length = 10)
    @Builder.Default
    private String locale = DEFAULT_LOCALE;

    /**
     * Năm học — current academic year label (e.g. {@code "2026-2027"}).
     * Auto-filled at provision via VN K-12 Sep→May convention.
     */
    @NotBlank(message = "Academic year is required")
    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    /**
     * Fiscal year for finance / invoicing (e.g. {@code "2026"}). Optional.
     */
    @Column(name = "fiscal_year", length = 20)
    private String fiscalYear;

    /**
     * Type of education organization.
     */
    @NotNull(message = "School type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "school_type", nullable = false, length = 20)
    @Builder.Default
    private SchoolType schoolType = SchoolType.CENTER;

    /**
     * Postal / physical address. Optional.
     */
    @Column(name = "address", length = 500)
    private String address;

    /**
     * Contact phone number. Optional.
     */
    @Column(name = "phone", length = 30)
    private String phone;

    /**
     * Logo URL (resolved object-storage / CDN link). Optional.
     */
    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    /**
     * Free-form theme configuration (color tokens, font, layout flags).
     *
     * <p>Stored as Postgres {@code jsonb} (mirrors {@code UserPreferences.notificationPreferences}
     * precedent). Nullable — tenants without custom theming fall back to platform default.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_config", columnDefinition = "jsonb")
    private Map<String, Object> themeConfig;
}
