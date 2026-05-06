package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import com.kiteclass.core.module.childprotection.repository.VettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Phase 1B foundation impl of {@link VettingService}.
 *
 * <p>Encapsulates:
 * <ul>
 *   <li>CRUD over {@link Vetting} with AES-256 roundtrip on sensitive fields
 *       (BR-VETTING-002 — handled transparently by
 *       {@code AesGcmAttributeConverter}).</li>
 *   <li>State-machine guard (BR-VETTING-001) — illegal transitions rejected
 *       with {@code ValidationException("VETTING_INVALID_TRANSITION")}.</li>
 *   <li>Soft-delete (BR-VETTING-005) — preserves the row for audit.</li>
 * </ul>
 *
 * <p>RBAC (BR-VETTING-003) is enforced one layer up by
 * {@code VettingController} based on the X-User-Roles header forwarded by
 * the Gateway. Service callers are assumed authorized.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VettingServiceImpl implements VettingService {

    /**
     * Allowed transitions per BR-VETTING-001. Source enum → set of allowed
     * targets. Any (source, target) pair not present here is rejected.
     */
    private static final Map<VettingStatus, Set<VettingStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(VettingStatus.class);
        ALLOWED_TRANSITIONS.put(VettingStatus.PENDING, Set.of(VettingStatus.SUBMITTED));
        ALLOWED_TRANSITIONS.put(VettingStatus.SUBMITTED, Set.of(VettingStatus.INTERVIEW_DONE));
        ALLOWED_TRANSITIONS.put(VettingStatus.INTERVIEW_DONE,
                Set.of(VettingStatus.APPROVED, VettingStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(VettingStatus.APPROVED, Set.of(VettingStatus.EXPIRED));
        // REJECTED + EXPIRED are terminal — no outgoing edges.
        ALLOWED_TRANSITIONS.put(VettingStatus.REJECTED, Set.of());
        ALLOWED_TRANSITIONS.put(VettingStatus.EXPIRED, Set.of());
    }

    private final VettingRepository vettingRepository;

    @Override
    public Vetting create(Long teacherId, String lltpNumber, String policeCheckDetails, Instant expiresAt) {
        if (teacherId == null) {
            throw new ValidationException("VETTING_TEACHER_ID_REQUIRED", new Object[0]);
        }
        Vetting vetting = Vetting.builder()
                .teacherId(teacherId)
                .status(VettingStatus.PENDING)
                .lltpNumber(lltpNumber)
                .policeCheckDetails(policeCheckDetails)
                .expiresAt(expiresAt)
                .build();
        Vetting saved = vettingRepository.save(vetting);
        log.info("Created vetting id={} teacherId={} status=PENDING", saved.getId(), teacherId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Vetting findById(Long id) {
        return vettingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("VETTING_NOT_FOUND", (Object) id));
    }

    @Override
    @Transactional(readOnly = true)
    public Vetting findLatestForTeacher(Long teacherId) {
        if (teacherId == null) {
            throw new ValidationException("VETTING_TEACHER_ID_REQUIRED", new Object[0]);
        }
        return vettingRepository.findFirstByTeacherIdAndDeletedFalseOrderByIdDesc(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("VETTING_NOT_FOUND_FOR_TEACHER", (Object) teacherId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Vetting> findAll(VettingStatus status, Pageable pageable) {
        return vettingRepository.findByFilters(status, pageable);
    }

    @Override
    public Vetting transition(Long id, VettingStatus target, Long decidedByUserId) {
        if (target == null) {
            throw new ValidationException("VETTING_TARGET_STATUS_REQUIRED", new Object[0]);
        }
        Vetting vetting = findById(id);
        VettingStatus current = vetting.getStatus();
        Set<VettingStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            log.warn("Rejected illegal vetting transition id={} {} → {}", id, current, target);
            throw new ValidationException("VETTING_INVALID_TRANSITION", current, target);
        }

        Instant now = Instant.now();
        switch (target) {
            case SUBMITTED -> vetting.setSubmittedAt(now);
            case INTERVIEW_DONE -> vetting.setInterviewedAt(now);
            case APPROVED, REJECTED -> {
                vetting.setDecidedAt(now);
                vetting.setDecidedByUserId(decidedByUserId);
            }
            case EXPIRED -> {
                // No timestamp update — expiresAt was set at APPROVE time.
            }
            default -> {
                // PENDING is never a target — fall-through guarded by ALLOWED_TRANSITIONS.
            }
        }
        vetting.setStatus(target);
        Vetting saved = vettingRepository.save(vetting);
        log.info("Vetting id={} transitioned {} → {} by user={}", id, current, target, decidedByUserId);
        return saved;
    }

    @Override
    public void softDelete(Long id) {
        Vetting vetting = findById(id);
        vetting.markAsDeleted();
        vettingRepository.save(vetting);
        log.info("Vetting id={} soft-deleted", id);
    }
}
