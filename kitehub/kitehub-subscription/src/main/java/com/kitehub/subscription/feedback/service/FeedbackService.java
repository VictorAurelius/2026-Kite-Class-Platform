package com.kitehub.subscription.feedback.service;

import com.kitehub.subscription.feedback.config.FeedbackConfig;
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
    private final FeedbackConfig config;

    public FeedbackService(FeedbackSubmissionRepository repository, FeedbackConfig config) {
        this.repository = repository;
        this.config = config;
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

        // GAP-555: defense-in-depth — enforce config-key whitelist on top of
        // controller-layer Bean Validation @Pattern (rules.md kitehub.feedback.categories).
        if (!isAllowedCategory(category)) {
            throw new IllegalArgumentException(
                    "Category not in kitehub.feedback.categories whitelist: " + category);
        }
        // GAP-555: rating + comment-length runtime checks mirror @Min/@Max/@Size
        // defaults; rules.md is the source-of-truth for these bounds, so any
        // production override of the config key flows through here without
        // requiring a redeploy of the DTO validator class-literals.
        Integer rating = request.rating();
        if (rating != null
                && (rating < config.getRatingRangeMin() || rating > config.getRatingRangeMax())) {
            throw new IllegalArgumentException(
                    "Rating outside kitehub.feedback.rating-range bounds: " + rating);
        }
        if (trimmedComment != null
                && (trimmedComment.length() < config.getCommentMinChars()
                        || trimmedComment.length() > config.getCommentMaxChars())) {
            throw new IllegalArgumentException(
                    "Comment length outside kitehub.feedback.comment-*-chars bounds: "
                            + trimmedComment.length());
        }

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

    private boolean isAllowedCategory(String category) {
        for (String allowed : config.getCategories().split(",")) {
            if (allowed.trim().equals(category)) {
                return true;
            }
        }
        return false;
    }
}
