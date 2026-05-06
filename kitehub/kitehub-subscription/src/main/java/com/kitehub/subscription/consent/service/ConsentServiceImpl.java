package com.kitehub.subscription.consent.service;

import com.kitehub.subscription.consent.dto.ConsentRequest;
import com.kitehub.subscription.consent.entity.ConsentRecord;
import com.kitehub.subscription.consent.repository.ConsentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link ConsentService}.
 *
 * <p>Idempotency is keyed on {@code visitorId}: if an active (non-revoked,
 * non-expired) record exists we update it in place; otherwise insert new.
 * Re-posting after a revoke creates a NEW record rather than reviving the
 * revoked one — keeping the audit trail honest.</p>
 *
 * <p>Logging follows the contextual-logging guideline from
 * {@code logs-format-standard.md} §3.2: visitor_id is treated as pseudonymous
 * (not PII) but we still avoid string-interpolating raw IPs / user-agents.</p>
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentServiceImpl implements ConsentService {

    private final ConsentRecordRepository repository;

    @Override
    @Transactional
    public ConsentRecord recordConsent(ConsentRequest request) {
        if (request == null || request.getVisitorId() == null) {
            throw new IllegalArgumentException("visitorId is required");
        }

        UUID visitorId = request.getVisitorId();
        Optional<ConsentRecord> existing = repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId);

        OffsetDateTime now = OffsetDateTime.now();
        ConsentRecord target;
        boolean isUpdate = false;

        if (existing.isPresent() && !existing.get().isRevoked()
                && existing.get().getExpiresAt().isAfter(now)) {
            target = existing.get();
            isUpdate = true;
        } else {
            target = new ConsentRecord();
            target.setVisitorId(visitorId);
        }

        // Essential is always true server-side — clients cannot opt out per BR-PDPL-CONSENT-001.
        target.setEssentialConsented(Boolean.TRUE);
        target.setAnalyticsConsented(Boolean.TRUE.equals(request.getAnalyticsConsented()));
        target.setMarketingConsented(Boolean.TRUE.equals(request.getMarketingConsented()));
        target.setUserId(request.getUserId());
        target.setTenantId(request.getTenantId());
        target.setIpAddress(request.getIpAddress());
        target.setUserAgent(request.getUserAgent());
        target.setConsentVersion(request.getConsentVersion() != null ? request.getConsentVersion() : 1);

        if (!isUpdate) {
            target.setCreatedAt(now);
            target.setExpiresAt(now.plusMonths(12));
        }
        // updatedAt set by @PreUpdate / @PrePersist.

        ConsentRecord saved = repository.save(target);
        log.info("Consent {} for visitor={} analytics={} marketing={}",
                isUpdate ? "updated" : "recorded",
                visitorId,
                saved.getAnalyticsConsented(),
                saved.getMarketingConsented());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsentRecord> findLatest(UUID visitorId) {
        if (visitorId == null) {
            throw new IllegalArgumentException("visitorId is required");
        }
        return repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId);
    }

    @Override
    @Transactional
    public ConsentRecord revoke(UUID visitorId) {
        if (visitorId == null) {
            throw new IllegalArgumentException("visitorId is required");
        }
        ConsentRecord record = repository.findFirstByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No consent record found for visitor: " + visitorId));

        if (record.isRevoked()) {
            log.info("Consent already revoked for visitor={} at {}",
                    visitorId, record.getRevokedAt());
            return record;
        }

        record.setRevokedAt(OffsetDateTime.now());
        record.setAnalyticsConsented(Boolean.FALSE);
        record.setMarketingConsented(Boolean.FALSE);
        ConsentRecord saved = repository.save(record);
        log.info("Consent revoked for visitor={}", visitorId);
        return saved;
    }
}
