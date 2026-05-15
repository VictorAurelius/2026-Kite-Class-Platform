package com.kitehub.email.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EmailType} — verify catalog completeness + template
 * name mapping (Wave 33 Bucket B — GAP-370).
 */
@DisplayName("EmailType Catalog")
class EmailTypeTest {

    @Test
    @DisplayName("Should expose BETA_INVITE + BETA_REQUEST_CONFIRM (GAP-370)")
    void shouldExposeBetaTypes() {
        assertThat(EmailType.BETA_INVITE.getTemplateName()).isEqualTo("beta-invite");
        assertThat(EmailType.BETA_REQUEST_CONFIRM.getTemplateName())
                .isEqualTo("beta-request-confirmation");
    }

    @Test
    @DisplayName("Should cover 13 (Wave 18a) + 2 beta (Wave 33) + INVITE_STAFF (Wave 80 B)")
    void shouldCoverFullCatalog() {
        // 16 = 13 existing (Wave 18a) + 2 beta (Wave 33) + 1 INVITE_STAFF (Wave 80 Bucket B)
        assertThat(EmailType.values()).hasSize(16);

        // Spot-check a representative subset across categories
        assertThat(EmailType.WELCOME.getTemplateName()).isEqualTo("welcome");
        assertThat(EmailType.EMAIL_VERIFICATION.getTemplateName()).isEqualTo("email-verification");
        assertThat(EmailType.SUBSCRIPTION_CREATED.getTemplateName()).isEqualTo("subscription-created");
        assertThat(EmailType.TRIAL_EXPIRED.getTemplateName()).isEqualTo("trial-expired");
        assertThat(EmailType.DATA_DELETED.getTemplateName()).isEqualTo("data-deleted");
    }

    @Test
    @DisplayName("fromTemplateName resolves known names + rejects unknown")
    void fromTemplateNameResolvesAndRejects() {
        assertThat(EmailType.fromTemplateName("beta-invite")).isEqualTo(EmailType.BETA_INVITE);
        assertThat(EmailType.fromTemplateName("beta-request-confirmation"))
                .isEqualTo(EmailType.BETA_REQUEST_CONFIRM);

        assertThatThrownBy(() -> EmailType.fromTemplateName("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown email template");
    }
}
