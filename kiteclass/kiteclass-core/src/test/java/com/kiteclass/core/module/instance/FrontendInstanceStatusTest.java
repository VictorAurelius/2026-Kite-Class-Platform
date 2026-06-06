package com.kiteclass.core.module.instance;

import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import org.junit.jupiter.api.Test;

import static com.kiteclass.core.module.instance.entity.FrontendInstanceStatus.DEPLOYED;
import static com.kiteclass.core.module.instance.entity.FrontendInstanceStatus.FAILED;
import static com.kiteclass.core.module.instance.entity.FrontendInstanceStatus.GENERATING;
import static com.kiteclass.core.module.instance.entity.FrontendInstanceStatus.INITIALIZING;
import static com.kiteclass.core.module.instance.entity.FrontendInstanceStatus.NOT_STARTED;
import static com.kiteclass.core.module.instance.entity.FrontendInstanceStatus.REGENERATING;
import static com.kiteclass.core.module.instance.entity.FrontendInstanceStatus.SUSPENDED;
import static com.kiteclass.core.module.instance.entity.FrontendInstanceStatus.DELETED;
import static org.assertj.core.api.Assertions.assertThat;

class FrontendInstanceStatusTest {

    @Test
    void notStarted_allows_initializing_only() {
        assertThat(NOT_STARTED.canTransitionTo(INITIALIZING)).isTrue();
        assertThat(NOT_STARTED.canTransitionTo(GENERATING)).isFalse();
        assertThat(NOT_STARTED.canTransitionTo(DEPLOYED)).isFalse();
        assertThat(NOT_STARTED.canTransitionTo(FAILED)).isFalse();
    }

    @Test
    void initializing_allows_generating_or_failed() {
        assertThat(INITIALIZING.canTransitionTo(GENERATING)).isTrue();
        assertThat(INITIALIZING.canTransitionTo(FAILED)).isTrue();
        assertThat(INITIALIZING.canTransitionTo(DEPLOYED)).isFalse();
        assertThat(INITIALIZING.canTransitionTo(NOT_STARTED)).isFalse();
    }

    @Test
    void generating_allows_deployed_or_failed() {
        assertThat(GENERATING.canTransitionTo(DEPLOYED)).isTrue();
        assertThat(GENERATING.canTransitionTo(FAILED)).isTrue();
        assertThat(GENERATING.canTransitionTo(REGENERATING)).isFalse();
    }

    @Test
    void deployed_allows_regenerating_or_suspended() {
        assertThat(DEPLOYED.canTransitionTo(REGENERATING)).isTrue();
        assertThat(DEPLOYED.canTransitionTo(SUSPENDED)).isTrue();
        assertThat(DEPLOYED.canTransitionTo(FAILED)).isFalse();
        assertThat(DEPLOYED.canTransitionTo(GENERATING)).isFalse();
        assertThat(DEPLOYED.canTransitionTo(DELETED)).isFalse();
    }

    @Test
    void regenerating_allows_deployed_or_failed() {
        assertThat(REGENERATING.canTransitionTo(DEPLOYED)).isTrue();
        assertThat(REGENERATING.canTransitionTo(FAILED)).isTrue();
        assertThat(REGENERATING.canTransitionTo(INITIALIZING)).isFalse();
    }

    @Test
    void failed_allows_retry_via_initializing_only() {
        assertThat(FAILED.canTransitionTo(INITIALIZING)).isTrue();
        assertThat(FAILED.canTransitionTo(GENERATING)).isFalse();
        assertThat(FAILED.canTransitionTo(DEPLOYED)).isFalse();
    }

    @Test
    void suspended_allows_reactivate_or_delete() {
        // GAP-954 off-boarding: SUSPENDED ⇄ DEPLOYED (reactivate) or → DELETED (soft-delete).
        assertThat(SUSPENDED.canTransitionTo(DEPLOYED)).isTrue();
        assertThat(SUSPENDED.canTransitionTo(DELETED)).isTrue();
        assertThat(SUSPENDED.canTransitionTo(REGENERATING)).isFalse();
        assertThat(SUSPENDED.canTransitionTo(FAILED)).isFalse();
    }

    @Test
    void deleted_is_terminal() {
        // GAP-954: DELETED is one-way terminal — 30d PDPL Art 23 grace then cross-service purge.
        assertThat(DELETED.isTerminal()).isTrue();
        assertThat(DELETED.canTransitionTo(DEPLOYED)).isFalse();
        assertThat(DELETED.canTransitionTo(SUSPENDED)).isFalse();
        assertThat(DELETED.canTransitionTo(NOT_STARTED)).isFalse();
    }

    @Test
    void only_deleted_is_terminal_in_current_machine() {
        // GAP-954: DELETED is the only terminal state; all provisioning + suspend states transition.
        for (FrontendInstanceStatus s : FrontendInstanceStatus.values()) {
            if (s == DELETED) {
                assertThat(s.isTerminal())
                        .as("DELETED must be terminal")
                        .isTrue();
            } else {
                assertThat(s.isTerminal())
                        .as("status %s should not be terminal", s)
                        .isFalse();
            }
        }
    }
}
