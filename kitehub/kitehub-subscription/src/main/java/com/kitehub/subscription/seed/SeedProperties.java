package com.kitehub.subscription.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for production data seeding.
 *
 * <p>Activated via the {@code kite.seed.mode} property (or {@code KITE_SEED_MODE}
 * env var). Only the {@code production} mode triggers {@link ProductionSeedRunner}
 * to populate the platform admin user + system config rows on first deploy.</p>
 *
 * <p>Configuration keys (per `documents/04-quality/gaps/GAP-376-production-data-seed.md`):</p>
 * <ul>
 *   <li>{@code kite.seed.mode} — {@code production}, {@code dev}, {@code none} (default {@code none})</li>
 *   <li>{@code kite.seed.admin-email} — default {@code admin@kitehub.vn}</li>
 *   <li>{@code kite.seed.admin-password} — REQUIRED when mode=production. Source from
 *       {@code SEED_ADMIN_PASSWORD} secret manager env var (per GAP-379).</li>
 *   <li>{@code kite.seed.admin-name} — display name (default {@code KiteHub Platform Admin})</li>
 *   <li>{@code kite.seed.default-tier} — system_config default tier (default {@code FREE})</li>
 *   <li>{@code kite.seed.default-currency} — system_config currency (default {@code VND})</li>
 *   <li>{@code kite.seed.default-locale} — system_config locale (default {@code vi})</li>
 * </ul>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kite.seed")
public class SeedProperties {

    /** Seed mode: {@code production}, {@code dev}, or {@code none}. Default {@code none}. */
    private String mode = "none";

    /** Admin user email. Default {@code admin@kitehub.vn}. */
    private String adminEmail = "admin@kitehub.vn";

    /**
     * Admin user plaintext password. REQUIRED when mode=production.
     * Loaded from {@code SEED_ADMIN_PASSWORD} env var (secrets manager — GAP-379).
     * The runner BCrypts before persisting; never stored plaintext.
     */
    private String adminPassword;

    /** Admin display name. Default {@code KiteHub Platform Admin}. */
    private String adminName = "KiteHub Platform Admin";

    /** Default tier for new instances. */
    private String defaultTier = "FREE";

    /** Default currency. */
    private String defaultCurrency = "VND";

    /** Default locale. */
    private String defaultLocale = "vi";

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getDefaultTier() { return defaultTier; }
    public void setDefaultTier(String defaultTier) { this.defaultTier = defaultTier; }

    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }

    public String getDefaultLocale() { return defaultLocale; }
    public void setDefaultLocale(String defaultLocale) { this.defaultLocale = defaultLocale; }

    /** True when mode is exactly {@code production}. */
    public boolean isProduction() {
        return "production".equalsIgnoreCase(mode);
    }
}
