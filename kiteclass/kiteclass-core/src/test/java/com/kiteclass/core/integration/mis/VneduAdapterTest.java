package com.kiteclass.core.integration.mis;

import com.kiteclass.core.integration.mis.adapters.VneduAdapter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for {@link VneduAdapter} — Phase 1 skeleton.
 *
 * <p>These tests do NOT hit any live VNEDU endpoint. They pin the interface
 * shape + explicit "not yet implemented" behavior so that when Phase 2 wires
 * the real HTTP client, the migration is visibly test-driven (these tests
 * will be updated to cover the happy path with MockServer / WireMock).
 */
class VneduAdapterTest {

    private final VneduAdapter adapter = new VneduAdapter();

    @Test
    void provider_identifies_as_vnedu() {
        assertThat(adapter.provider()).isEqualTo(MisProvider.VNEDU);
    }

    @Test
    void ping_returns_not_connected_in_phase_1_skeleton() {
        MisConnectionStatus status = adapter.ping();

        assertThat(status).isNotNull();
        assertThat(status.provider()).isEqualTo(MisProvider.VNEDU);
        assertThat(status.connected()).isFalse();
        assertThat(status.errorMessage())
                .contains("not yet implemented")
                .contains("ADR-017");
        assertThat(status.testedAt()).isNotNull();
    }

    @Test
    void fetchRoster_rejects_blank_academic_year() {
        assertThatThrownBy(() -> adapter.fetchRoster(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("academicYear");

        assertThatThrownBy(() -> adapter.fetchRoster(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("academicYear");
    }

    @Test
    void fetchRoster_throws_integration_exception_in_phase_1_skeleton() {
        // Pin the "fail loud" behavior so nobody accidentally deploys a silent
        // skeleton into production. When Phase 2 wires the real client, this
        // test is replaced with a MockServer-backed happy-path test.
        assertThatThrownBy(() -> adapter.fetchRoster("2025-2026"))
                .isInstanceOf(MisIntegrationException.class)
                .hasMessageContaining("not implemented")
                .satisfies(ex -> assertThat(((MisIntegrationException) ex).provider())
                        .isEqualTo(MisProvider.VNEDU));
    }

    @Test
    void buildEmptyRosterForTests_returns_well_formed_dto() {
        RosterImport roster = adapter.buildEmptyRosterForTests("2025-2026");

        assertThat(roster).isNotNull();
        assertThat(roster.source()).isEqualTo(MisProvider.VNEDU);
        assertThat(roster.academicYear()).isEqualTo("2025-2026");
        assertThat(roster.fetchedAt()).isNotNull();
        // Records compact constructor replaces nulls with empty lists — verify.
        assertThat(roster.students()).isEmpty();
        assertThat(roster.parents()).isEmpty();
        assertThat(roster.teachers()).isEmpty();
        assertThat(roster.classes()).isEmpty();
        assertThat(roster.enrollments()).isEmpty();
    }

    @Test
    void rosterImport_compact_constructor_replaces_nulls_with_empty_lists() {
        RosterImport roster = new RosterImport(
                MisProvider.VNEDU,
                java.time.Instant.now(),
                "2025-2026",
                null, null, null, null, null);

        assertThat(roster.students()).isEmpty();
        assertThat(roster.parents()).isEmpty();
        assertThat(roster.teachers()).isEmpty();
        assertThat(roster.classes()).isEmpty();
        assertThat(roster.enrollments()).isEmpty();
    }

    @Test
    void parent_record_compact_constructor_handles_null_links() {
        RosterImport.ParentRecord parent = new RosterImport.ParentRecord(
                "P001", "Nguyễn Văn A", "a@example.com", "0912345678",
                "FATHER", null);

        assertThat(parent.linkedProviderStudentIds()).isEmpty();
    }

    @Test
    void adapter_implements_mis_roster_source_polymorphically() {
        // Smoke test that confirms future orchestrator code can depend on the
        // interface and get this adapter's behavior through it.
        MisRosterSource source = adapter;
        assertThat(source.provider()).isEqualTo(MisProvider.VNEDU);
        assertThat(source.ping().connected()).isFalse();
    }

    @Test
    void connection_status_helpers_produce_correct_variants() {
        MisConnectionStatus ok = MisConnectionStatus.ok(
                MisProvider.VNEDU, "v1.4", "THPT Demo");
        assertThat(ok.connected()).isTrue();
        assertThat(ok.errorMessage()).isNull();
        assertThat(ok.providerVersion()).isEqualTo("v1.4");
        assertThat(ok.schoolName()).isEqualTo("THPT Demo");

        MisConnectionStatus bad = MisConnectionStatus.failed(
                MisProvider.VNEDU, "Auth failed");
        assertThat(bad.connected()).isFalse();
        assertThat(bad.errorMessage()).isEqualTo("Auth failed");
        assertThat(bad.providerVersion()).isNull();
    }
}
