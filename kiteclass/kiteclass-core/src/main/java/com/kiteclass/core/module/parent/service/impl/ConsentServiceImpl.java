package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentalConsent;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ConsentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed {@link ConsentService} reading + writing the JSONB
 * {@code parental_consent} column on {@code parent_student_links}.
 *
 * <p>Read path is the hot path (every facet API hits it) so we keep it
 * to a single repository call. No caching layer in v1 — Phase 1B audit
 * row already adds 1 write per facet read; consent gate adds 1 more
 * read. Redis caching is a Wave 20+ optimization tracked in the
 * GAP-321c follow-up.
 *
 * <p>Write path runs in the surrounding transaction; flushes the
 * updated consent map back to the JSONB column in one round-trip.
 *
 * @author KiteClass Team
 * @since 2.19.0 (Wave 19 — GAP-321c Phase 1C v1)
 */
@Slf4j
@Service
public class ConsentServiceImpl implements ConsentService {

    private final ParentStudentLinkRepository linkRepository;

    /**
     * BR-PARENT-PORTAL-015 — current required policy version. Default
     * {@code 1} matches the V56 migration's seeded version. Admin tooling
     * (BR-PARENT-PORTAL-016) bumps this value via configuration; existing
     * parental-consent records below it are required to re-consent before
     * facet APIs return data (per BR-PARENT-PORTAL-015).
     */
    private final int requiredVersion;

    public ConsentServiceImpl(
            ParentStudentLinkRepository linkRepository,
            @Value("${kite.parent.consent.required-version:1}") int requiredVersion) {
        this.linkRepository = linkRepository;
        this.requiredVersion = Math.max(1, requiredVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkConsent(Long parentId, Long childId, String field) {
        if (parentId == null || childId == null || field == null || field.isBlank()) {
            return false;
        }
        Optional<ParentStudentLink> linkOpt = findActiveLink(parentId, childId);
        if (linkOpt.isEmpty()) {
            return false;
        }
        ParentalConsent consent = linkOpt.get().getParentalConsent();
        if (consent == null) {
            return false;
        }
        return consent.hasFieldConsent(field);
    }

    @Override
    @Transactional(readOnly = true)
    public ParentalConsent getConsent(Long parentId, Long childId) {
        if (parentId == null || childId == null) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }
        return findActiveLink(parentId, childId)
                .map(ParentStudentLink::getParentalConsent)
                .orElseGet(ParentalConsent::defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public int getConsentVersion(Long parentId, Long childId) {
        if (parentId == null || childId == null) {
            return 1;
        }
        return findActiveLink(parentId, childId)
                .map(ParentStudentLink::getParentalConsent)
                .map(ParentalConsent::version)
                .orElse(1);
    }

    @Override
    @Transactional
    public ParentalConsent bumpConsent(Long parentId,
                                       Long childId,
                                       Map<String, Boolean> updates) {
        if (parentId == null || childId == null || updates == null) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }
        ParentStudentLink link = findActiveLink(parentId, childId)
                .orElseThrow(() -> new BusinessException(
                        "PARENT_CONSENT_LINK_NOT_FOUND", HttpStatus.NOT_FOUND));

        ParentalConsent existing = link.getParentalConsent();
        if (existing == null) {
            existing = ParentalConsent.defaultValue();
        }

        // Merge: copy existing flags, then overlay incoming updates so the
        // PUT can be sparse (only the toggled keys).
        Map<String, Boolean> merged = new HashMap<>();
        if (existing.fields() != null) {
            merged.putAll(existing.fields());
        }
        merged.putAll(updates);

        ParentalConsent next = new ParentalConsent(
                merged,
                existing.version() + 1,
                Instant.now());
        link.setParentalConsent(next);
        // JPA dirty checking flushes on transaction commit; explicit
        // save not required (link is managed).
        log.info("Parent {} bumped consent for child {} → version {}",
                parentId, childId, next.version());
        return next;
    }

    @Override
    public int getRequiredVersion() {
        return requiredVersion;
    }

    @Override
    @Transactional
    public int bulkBumpVersion(UUID instanceId, int newVersion, String reason) {
        if (instanceId == null) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }
        if (newVersion < 1) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }
        int updated = linkRepository.bulkBumpConsentVersion(instanceId, newVersion);
        log.info("Admin bulk-bumped {} parental-consent records in tenant {} to version {} (reason: {})",
                updated, instanceId, newVersion, reason);
        return updated;
    }

    /**
     * Single canonical lookup — every public method funnels through here so
     * the JPQL stays in one place (and the test mock has a single seam).
     */
    private Optional<ParentStudentLink> findActiveLink(Long parentId, Long childId) {
        // ParentStudentLinkRepository#findByParentIdWithStudent is the only
        // existing finder that returns the entity (vs boolean exists). Filter
        // to the requested child in-process — typical parent has ≤5 children.
        List<ParentStudentLink> links =
                linkRepository.findByParentIdWithStudent(parentId);
        return links.stream()
                .filter(l -> l.getStudent() != null
                        && childId.equals(l.getStudent().getId()))
                .findFirst();
    }
}
