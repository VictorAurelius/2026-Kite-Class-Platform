package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.subscription.config.TrialToPaidConfig;
import com.kitehub.subscription.exception.MigrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MigrationStateMachine")
class MigrationStateMachineTest {

    private TrialToPaidConfig config;
    private MigrationStateMachine sm;
    private Instance instance;

    @BeforeEach
    void setUp() {
        config = new TrialToPaidConfig();
        sm = new MigrationStateMachine(config);
        instance = new Instance();
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setMigrationPhase(MigrationPhase.NONE);
    }

    @Nested
    @DisplayName("transitionPhase()")
    class TransitionPhase {
        @Test
        void valid_transition_updates_phase_and_started_at_for_INITIATED() {
            LocalDateTime now = LocalDateTime.now();
            sm.transitionPhase(instance, MigrationPhase.INITIATED, now);
            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.INITIATED);
            assertThat(instance.getMigrationStartedAt()).isEqualTo(now);
        }

        @Test
        void invalid_transition_throws_INVALID_PHASE_TRANSITION() {
            instance.setMigrationPhase(MigrationPhase.NONE);
            assertThatThrownBy(() -> sm.transitionPhase(instance, MigrationPhase.COMPLETED, LocalDateTime.now()))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("Illegal transition");
        }

        @Test
        void null_current_phase_treated_as_NONE() {
            instance.setMigrationPhase(null);
            sm.transitionPhase(instance, MigrationPhase.INITIATED, LocalDateTime.now());
            assertThat(instance.getMigrationPhase()).isEqualTo(MigrationPhase.INITIATED);
        }
    }

    @Nested
    @DisplayName("assertCanStartMigration()")
    class AssertCanStart {
        @Test
        void NONE_passes() {
            instance.setMigrationPhase(MigrationPhase.NONE);
            sm.assertCanStartMigration(instance);
        }

        @Test
        void MIGRATION_FAILED_throws_FAILED_LOCKED() {
            instance.setMigrationPhase(MigrationPhase.MIGRATION_FAILED);
            assertThatThrownBy(() -> sm.assertCanStartMigration(instance))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("MIGRATION_FAILED");
        }

        @Test
        void in_flight_phase_throws_IN_FLIGHT() {
            instance.setMigrationPhase(MigrationPhase.PAYMENT_PENDING);
            assertThatThrownBy(() -> sm.assertCanStartMigration(instance))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("Another migration already in flight");
        }
    }

    @Nested
    @DisplayName("assertWithinRescueWindowOrStillTrial()")
    class AssertRescueWindow {
        @Test
        void TRIAL_within_window_passes() {
            instance.setStatus(InstanceStatus.TRIAL);
            instance.setTrialExpiresAt(LocalDateTime.now().plusDays(1));
            sm.assertWithinRescueWindowOrStillTrial(instance);
        }

        @Test
        void non_TRIAL_status_throws() {
            instance.setStatus(InstanceStatus.ACTIVE);
            assertThatThrownBy(() -> sm.assertWithinRescueWindowOrStillTrial(instance))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("not on TRIAL");
        }

        @Test
        void expired_beyond_rescue_window_throws() {
            instance.setStatus(InstanceStatus.TRIAL);
            instance.setTrialExpiresAt(LocalDateTime.now()
                .minusHours(config.getRescueWindowHours() + 1));
            assertThatThrownBy(() -> sm.assertWithinRescueWindowOrStillTrial(instance))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("rescue window");
        }
    }

    @Nested
    @DisplayName("isWithinReversalWindow()")
    class ReversalWindow {
        @Test
        void no_completion_timestamp_returns_false() {
            instance.setMigrationCompletedAt(null);
            assertThat(sm.isWithinReversalWindow(instance)).isFalse();
        }

        @Test
        void within_window_returns_true() {
            instance.setMigrationCompletedAt(LocalDateTime.now().minusHours(1));
            assertThat(sm.isWithinReversalWindow(instance)).isTrue();
        }

        @Test
        void beyond_window_returns_false() {
            instance.setMigrationCompletedAt(LocalDateTime.now()
                .minusHours(config.getReversalWindowHours() + 1));
            assertThat(sm.isWithinReversalWindow(instance)).isFalse();
        }
    }
}
