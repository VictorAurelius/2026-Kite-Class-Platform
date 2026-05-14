package com.kitehub.subscription.feedback.service;

import com.kitehub.subscription.feedback.dto.FeedbackSubmissionRequest;
import com.kitehub.subscription.feedback.entity.FeedbackSubmission;
import com.kitehub.subscription.feedback.repository.FeedbackSubmissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain service for feedback submission persistence (GAP-542 Wave 78 Bucket F).
 *
 * <p>Single responsibility: validate-then-persist a {@link FeedbackSubmission}.
 * Honeypot + bean-validation gate at the controller layer; this service trusts
 * the DTO has already been validated.</p>
 *
 * @since Wave 78 — GAP-542
 */
@Service
@Slf4j
public class FeedbackService {

    private final FeedbackSubmissionRepository repository;

    public FeedbackService(FeedbackSubmissionRepository repository) {
        this.repository = repository;
    }

    /**
     * Persist a submitted feedback row. Auto-attach tenant + user context if
     * provided by the controller (JWT-derived).
     */
    @Transactional
    public FeedbackSubmission submit(
            FeedbackSubmissionRequest request,
            String tenantId,
            String userId,
            String clientIp) {

        String trimmedComment = request.comment() == null ? null : request.comment().trim();
        String trimmedEmail = (request.email() == null || request.email().isBlank())
                ? null
                : request.email().trim();
        String trimmedPageUrl = (request.pageUrl() == null || request.pageUrl().isBlank())
                ? null
                : request.pageUrl().trim();
        String category = (request.category() == null || request.category().isBlank())
                ? "GENERAL"
                : request.category().trim();

        FeedbackSubmission entity = FeedbackSubmission.builder()
                .publicId(UUID.randomUUID())
                .rating(request.rating() == null ? null : request.rating().shortValue())
                .comment(trimmedComment)
                .email(trimmedEmail)
                .pageUrl(trimmedPageUrl)
                .category(category)
                .tenantId(tenantId)
                .userId(userId)
                .clientIp(clientIp)
                .status("RECEIVED")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        FeedbackSubmission saved = repository.save(entity);
        log.info("Feedback submitted publicId={} rating={} category={} hasEmail={} authenticated={}",
                saved.getPublicId(),
                saved.getRating(),
                saved.getCategory(),
                trimmedEmail != null,
                userId != null);
        return saved;
    }
}
