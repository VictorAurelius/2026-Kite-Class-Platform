package com.kitehub.subscription.seed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SeedProperties")
class SeedPropertiesTest {

    @Test
    @DisplayName("Default mode is 'none' — runner should skip on a fresh boot without env vars")
    void defaultsToNoneMode() {
        SeedProperties p = new SeedProperties();
        assertThat(p.getMode()).isEqualTo("none");
        assertThat(p.isProduction()).isFalse();
    }

    @Test
    @DisplayName("isProduction() is case-insensitive on 'production'")
    void productionPredicateIsCaseInsensitive() {
        SeedProperties p = new SeedProperties();

        p.setMode("production");
        assertThat(p.isProduction()).isTrue();

        p.setMode("PRODUCTION");
        assertThat(p.isProduction()).isTrue();

        p.setMode("Production");
        assertThat(p.isProduction()).isTrue();
    }

    @Test
    @DisplayName("isProduction() is false for any other mode")
    void otherModesAreNotProduction() {
        SeedProperties p = new SeedProperties();

        for (String mode : new String[] {"none", "dev", "staging", "PROD", "prd", "", null}) {
            p.setMode(mode);
            assertThat(p.isProduction())
                    .as("mode=%s should NOT be production", mode)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Defaults: VN-first (VND, vi) + FREE tier + admin@kitehub.vn")
    void defaultsAreVietnamFirst() {
        SeedProperties p = new SeedProperties();
        assertThat(p.getAdminEmail()).isEqualTo("admin@kitehub.vn");
        assertThat(p.getAdminName()).isEqualTo("KiteHub Platform Admin");
        assertThat(p.getDefaultTier()).isEqualTo("FREE");
        assertThat(p.getDefaultCurrency()).isEqualTo("VND");
        assertThat(p.getDefaultLocale()).isEqualTo("vi");
    }
}
