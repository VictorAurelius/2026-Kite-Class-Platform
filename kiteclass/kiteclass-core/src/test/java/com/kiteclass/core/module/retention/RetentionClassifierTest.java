package com.kiteclass.core.module.retention;

import com.kiteclass.core.common.audit.AuditLog;
import com.kiteclass.core.common.outbox.OutboxEvent;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetentionClassifierTest {

    private final RetentionClassifier classifier = new RetentionClassifier();

    @Test
    void annotated_entity_returns_its_bucket() {
        RetentionClassifier.Classification c = classifier.classify(BrandingResource.class);

        assertThat(c.getBucket()).isEqualTo(RetentionBucket.PURGE_ON_REQUEST);
        assertThat(c.isExplicit()).isTrue();
        assertThat(c.getPseudonymizeFields()).isEmpty();
    }

    @Test
    void audit_log_is_retain_with_pseudo_and_carries_pseudonymize_fields() {
        RetentionClassifier.Classification c = classifier.classify(AuditLog.class);

        assertThat(c.getBucket()).isEqualTo(RetentionBucket.RETAIN_WITH_PSEUDO);
        assertThat(c.isExplicit()).isTrue();
        assertThat(c.getPseudonymizeFields()).containsExactly("actor_user_id");
    }

    @Test
    void outbox_event_is_purge_delayed() {
        RetentionClassifier.Classification c = classifier.classify(OutboxEvent.class);

        assertThat(c.getBucket()).isEqualTo(RetentionBucket.PURGE_DELAYED);
        assertThat(c.isExplicit()).isTrue();
    }

    @Test
    void unannotated_class_defaults_to_purge_on_request() {
        RetentionClassifier.Classification c = classifier.classify(Unannotated.class);

        assertThat(c.getBucket()).isEqualTo(RetentionBucket.PURGE_ON_REQUEST);
        assertThat(c.isExplicit()).isFalse();
        assertThat(c.getPseudonymizeFields()).isEmpty();
    }

    @Test
    void null_class_rejected() {
        assertThatThrownBy(() -> classifier.classify(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    /** Local unannotated entity to verify the safe default branch. */
    private static final class Unannotated {
    }
}
