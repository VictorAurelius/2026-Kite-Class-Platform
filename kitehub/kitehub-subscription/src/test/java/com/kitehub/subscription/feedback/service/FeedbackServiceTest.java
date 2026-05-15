package com.kitehub.subscription.feedback.service;

import com.kitehub.subscription.feedback.config.FeedbackConfig;
import com.kitehub.subscription.feedback.dto.FeedbackSubmissionRequest;
import com.kitehub.subscription.feedback.entity.FeedbackSubmission;
import com.kitehub.subscription.feedback.repository.FeedbackSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FeedbackService (GAP-542 Wave 78 Bucket F).
 *
 * Coverage:
 *  - Persistence happy path with all fields
 *  - Default category = GENERAL when null
 *  - Trim whitespace on comment + email + pageUrl
 *  - Anonymous submit (tenantId/userId null) works
 *
 * @since Wave 78 Bucket F
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackSubmissionRepository repository;

    private FeedbackService service;

    @BeforeEach
    void setUp() {
        // Wave 79 Bucket A GAP-555 — FeedbackConfig wires kitehub.feedback.* keys.
        // Use real config with defaults so tests exercise the same whitelist as prod.
        FeedbackConfig config = new FeedbackConfig(1, 5, 5, 2000, 10, 30,
                "BUG,USABILITY,FEATURE_REQUEST,GENERAL");
        service = new FeedbackService(repository, config);
    }

    @Test
    void shouldPersistSubmissionWithAllFields() {
        FeedbackSubmissionRequest request = new FeedbackSubmissionRequest(
                4,
                "Onboarding rất rõ ràng",
                "user@kitehub.me",
                "https://kitehub.me/dashboard",
                "USABILITY",
                ""
        );
        when(repository.save(any(FeedbackSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackSubmission saved = service.submit(request, "tenant-1", "user-42", "203.0.113.5");

        ArgumentCaptor<FeedbackSubmission> captor = ArgumentCaptor.forClass(FeedbackSubmission.class);
        verify(repository).save(captor.capture());
        FeedbackSubmission captured = captor.getValue();

        assertThat(captured.getRating()).isEqualTo((short) 4);
        assertThat(captured.getComment()).isEqualTo("Onboarding rất rõ ràng");
        assertThat(captured.getEmail()).isEqualTo("user@kitehub.me");
        assertThat(captured.getPageUrl()).isEqualTo("https://kitehub.me/dashboard");
        assertThat(captured.getCategory()).isEqualTo("USABILITY");
        assertThat(captured.getTenantId()).isEqualTo("tenant-1");
        assertThat(captured.getUserId()).isEqualTo("user-42");
        assertThat(captured.getClientIp()).isEqualTo("203.0.113.5");
        assertThat(captured.getStatus()).isEqualTo("RECEIVED");
        assertThat(captured.getPublicId()).isNotNull();
        assertThat(saved).isSameAs(captured);
    }

    @Test
    void shouldDefaultCategoryToGeneralWhenNull() {
        FeedbackSubmissionRequest request = new FeedbackSubmissionRequest(
                3,
                "Khá tốt",
                null,
                null,
                null,
                ""
        );
        when(repository.save(any(FeedbackSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.submit(request, null, null, "127.0.0.1");

        ArgumentCaptor<FeedbackSubmission> captor = ArgumentCaptor.forClass(FeedbackSubmission.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("GENERAL");
    }

    @Test
    void shouldAcceptAnonymousSubmission() {
        FeedbackSubmissionRequest request = new FeedbackSubmissionRequest(
                5,
                "Sản phẩm tuyệt vời",
                null,
                null,
                "GENERAL",
                ""
        );
        when(repository.save(any(FeedbackSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.submit(request, null, null, "203.0.113.7");

        ArgumentCaptor<FeedbackSubmission> captor = ArgumentCaptor.forClass(FeedbackSubmission.class);
        verify(repository).save(captor.capture());
        FeedbackSubmission captured = captor.getValue();
        assertThat(captured.getTenantId()).isNull();
        assertThat(captured.getUserId()).isNull();
        assertThat(captured.getEmail()).isNull();
    }

    @Test
    void shouldTrimWhitespaceInComment() {
        FeedbackSubmissionRequest request = new FeedbackSubmissionRequest(
                4,
                "  có khoảng trắng  ",
                "  user@example.com  ",
                null,
                "BUG",
                ""
        );
        when(repository.save(any(FeedbackSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.submit(request, null, null, null);

        ArgumentCaptor<FeedbackSubmission> captor = ArgumentCaptor.forClass(FeedbackSubmission.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getComment()).isEqualTo("có khoảng trắng");
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
    }
}
