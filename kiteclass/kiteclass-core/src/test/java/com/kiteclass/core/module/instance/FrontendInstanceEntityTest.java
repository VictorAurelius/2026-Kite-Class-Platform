package com.kiteclass.core.module.instance;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrontendInstanceEntityTest {

    private FrontendInstance newInstance() {
        return FrontendInstance.builder()
                .tenantSlug("t-1")
                .slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0)
                .brandingVersion(0)
                .build();
    }

    @Test
    void transitionTo_updates_status_and_timestamp() {
        FrontendInstance i = newInstance();

        i.transitionTo(FrontendInstanceStatus.INITIALIZING);

        assertThat(i.getStatus()).isEqualTo(FrontendInstanceStatus.INITIALIZING);
        assertThat(i.getInitializingAt()).isNotNull();
    }

    @Test
    void transitionTo_invalid_throws() {
        FrontendInstance i = newInstance();

        assertThatThrownBy(() -> i.transitionTo(FrontendInstanceStatus.DEPLOYED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    void deploy_bumps_branding_version() {
        FrontendInstance i = newInstance();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.GENERATING);

        i.transitionTo(FrontendInstanceStatus.DEPLOYED);

        assertThat(i.getBrandingVersion()).isEqualTo(1);
        assertThat(i.getDeployedAt()).isNotNull();
    }

    @Test
    void fail_increments_retry_count() {
        FrontendInstance i = newInstance();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);

        i.transitionTo(FrontendInstanceStatus.FAILED);

        assertThat(i.getRetryCount()).isEqualTo(1);
        assertThat(i.getFailedAt()).isNotNull();
    }

    @Test
    void full_happy_path_deploy_then_rebrand() {
        FrontendInstance i = newInstance();

        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.GENERATING);
        i.transitionTo(FrontendInstanceStatus.DEPLOYED);
        i.transitionTo(FrontendInstanceStatus.REGENERATING);
        i.transitionTo(FrontendInstanceStatus.DEPLOYED);

        assertThat(i.getStatus()).isEqualTo(FrontendInstanceStatus.DEPLOYED);
        assertThat(i.getBrandingVersion()).isEqualTo(2);
        assertThat(i.getLastRegenerateAt()).isNotNull();
    }

    @Test
    void retry_clears_previous_failure_reason() {
        FrontendInstance i = newInstance();
        i.transitionTo(FrontendInstanceStatus.INITIALIZING);
        i.transitionTo(FrontendInstanceStatus.FAILED);
        i.setFailureReason("boom");

        i.transitionTo(FrontendInstanceStatus.INITIALIZING);

        assertThat(i.getFailureReason()).isNull();
    }
}
