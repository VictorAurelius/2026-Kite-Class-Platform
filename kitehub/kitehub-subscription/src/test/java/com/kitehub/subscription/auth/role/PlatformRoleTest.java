package com.kitehub.subscription.auth.role;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PlatformRole} canonical enum + alias resolver.
 *
 * <p>Wave 79 GAP-562 — verifies backward-compat aliases (PLATFORM_ADMIN,
 * ADMIN) map correctly to canonical OWNER during 30-day window.</p>
 *
 * @since Wave 79
 */
class PlatformRoleTest {

    @Test
    void resolvesCanonicalOwnerString() {
        assertThat(PlatformRole.fromStoredValue("OWNER")).isEqualTo(PlatformRole.OWNER);
        assertThat(PlatformRole.fromStoredValue("owner")).isEqualTo(PlatformRole.OWNER);
        assertThat(PlatformRole.fromStoredValue(" OWNER ")).isEqualTo(PlatformRole.OWNER);
    }

    @Test
    void resolvesLegacyPlatformAdminAliasToOwner() {
        // 30-day backward-compat window cutoff 2026-06-14 (Wave 81 cleanup).
        assertThat(PlatformRole.fromStoredValue("PLATFORM_ADMIN"))
                .isEqualTo(PlatformRole.OWNER);
    }

    @Test
    void resolvesLegacyAdminAliasToOwner() {
        assertThat(PlatformRole.fromStoredValue("ADMIN")).isEqualTo(PlatformRole.OWNER);
    }

    @Test
    void resolvesStaff() {
        assertThat(PlatformRole.fromStoredValue("STAFF")).isEqualTo(PlatformRole.STAFF);
        assertThat(PlatformRole.fromStoredValue("staff")).isEqualTo(PlatformRole.STAFF);
    }

    @Test
    void unknownValueReturnsNull() {
        assertThat(PlatformRole.fromStoredValue("UNKNOWN_ROLE")).isNull();
        assertThat(PlatformRole.fromStoredValue("")).isNull();
        assertThat(PlatformRole.fromStoredValue(null)).isNull();
    }

    @Test
    void authorityStringFollowsSpringConvention() {
        assertThat(PlatformRole.OWNER.authority()).isEqualTo("ROLE_OWNER");
        assertThat(PlatformRole.STAFF.authority()).isEqualTo("ROLE_STAFF");
    }

    @Test
    void isOwnerAndIsStaffAreMutuallyExclusive() {
        assertThat(PlatformRole.OWNER.isOwner()).isTrue();
        assertThat(PlatformRole.OWNER.isStaff()).isFalse();
        assertThat(PlatformRole.STAFF.isStaff()).isTrue();
        assertThat(PlatformRole.STAFF.isOwner()).isFalse();
    }
}
