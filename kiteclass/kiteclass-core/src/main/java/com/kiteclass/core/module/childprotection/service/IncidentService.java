package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IncidentService — Phase 1A CRUD for child-protection tickets with
 * encryption roundtrip via {@code AesGcmAttributeConverter}.
 *
 * <p>Phase 1A scope:
 * <ul>
 *   <li>{@link #create(String, String, String, IncidentSeverity, IncidentCategory, Long, Long)}
 *       — create a new incident; encrypted fields persisted via converter</li>
 *   <li>{@link #findById(Long)} — read-by-id; decryption transparent</li>
 *   <li>{@link #findAll(IncidentSeverity, IncidentCategory, IncidentStatus, Pageable)}
 *       — paged read-only listing</li>
 *   <li>{@link #updateStatus(Long, IncidentStatus)} — minimal lifecycle progression</li>
 *   <li>{@link #softDelete(Long)} — soft-delete (restoration left to admin)</li>
 * </ul>
 *
 * <p>Phase 1B / 1C deferred (GAP-322b/c):
 * <ul>
 *   <li>RBAC gate on decryption — only safeguarding officer + Hiệu trưởng +
 *       counselor may read decrypted description/evidence</li>
 *   <li>Mandatory-reporting auto-suggest banner (Đ.51) — service-level emit</li>
 *   <li>Hash-chained non-repudiation audit log on every CRUD operation</li>
 *   <li>State-machine enforcement on status transitions</li>
 *   <li>7-year retention enforcement (anti-delete on CLOSED + age &lt; 7y)</li>
 * </ul>
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IncidentService {

    private static final int MAX_TITLE_LENGTH = 200;

    private final IncidentRepository incidentRepository;

    /**
     * Create a new incident. Encrypted fields ({@code description},
     * {@code evidencePaths}) are persisted via
     * {@code AesGcmAttributeConverter} — caller passes plaintext.
     *
     * @param title         non-sensitive title (≤200 chars, plaintext, indexed)
     * @param description   sensitive narrative (encrypted at rest); may be null
     * @param evidencePaths newline-separated MinIO object keys (encrypted); may be null
     * @param severity      non-null
     * @param category      non-null
     * @param reporterUserId non-null (FK to users)
     * @param subjectStudentId optional (FK to students)
     * @return persisted incident with id populated; status defaults to REPORTED
     * @throws ValidationException if title null/blank/too long, severity null,
     *                             category null, or reporterUserId null
     */
    public Incident create(
            String title,
            String description,
            String evidencePaths,
            IncidentSeverity severity,
            IncidentCategory category,
            Long reporterUserId,
            Long subjectStudentId
    ) {
        validateTitle(title);
        if (severity == null) {
            throw new ValidationException("Severity is required");
        }
        if (category == null) {
            throw new ValidationException("Category is required");
        }
        if (reporterUserId == null) {
            throw new ValidationException("Reporter user id is required");
        }

        Incident incident = Incident.builder()
                .title(title)
                .description(description)
                .evidencePaths(evidencePaths)
                .severity(severity)
                .category(category)
                .status(IncidentStatus.REPORTED)
                .reporterUserId(reporterUserId)
                .subjectStudentId(subjectStudentId)
                .build();

        Incident saved = incidentRepository.save(incident);
        log.info("Created incident id={} severity={} category={} (reporter={})",
                saved.getId(), severity, category, reporterUserId);
        return saved;
    }

    /**
     * Look up incident by id (excludes soft-deleted).
     *
     * @throws EntityNotFoundException if not found or deleted
     */
    @Transactional(readOnly = true)
    public Incident findById(Long id) {
        return incidentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Incident not found: id=" + id));
    }

    /**
     * Paged list of incidents with optional severity / category / status
     * filters. Phase 1A read-only — no decryption-RBAC gating yet.
     */
    @Transactional(readOnly = true)
    public Page<Incident> findAll(
            IncidentSeverity severity,
            IncidentCategory category,
            IncidentStatus status,
            Pageable pageable
    ) {
        return incidentRepository.findByFilters(severity, category, status, pageable);
    }

    /**
     * Advance lifecycle status. Phase 1A allows any non-null transition;
     * Phase 1B locks the state machine.
     *
     * @throws EntityNotFoundException if not found
     * @throws ValidationException     if newStatus null
     */
    public Incident updateStatus(Long id, IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new ValidationException("Status is required");
        }
        Incident incident = findById(id);
        IncidentStatus previous = incident.getStatus();
        incident.setStatus(newStatus);
        Incident saved = incidentRepository.save(incident);
        log.info("Incident id={} status {} → {}", id, previous, newStatus);
        return saved;
    }

    /**
     * Assign safeguarding officer to incident. Phase 1A skeletal — Phase 1B
     * gates decryption to assigned officer + Hiệu trưởng + counselor.
     */
    public Incident assignOfficer(Long id, Long officerUserId) {
        if (officerUserId == null) {
            throw new ValidationException("Officer user id is required");
        }
        Incident incident = findById(id);
        incident.setAssignedOfficerUserId(officerUserId);
        Incident saved = incidentRepository.save(incident);
        log.info("Incident id={} assigned officer userId={}", id, officerUserId);
        return saved;
    }

    /**
     * Soft-delete the incident. Phase 1A allows; Phase 1B (GAP-322c) enforces
     * 7-year retention on CLOSED incidents.
     *
     * @throws EntityNotFoundException if not found or already deleted
     */
    public void softDelete(Long id) {
        Incident incident = findById(id);
        incident.markAsDeleted();
        incidentRepository.save(incident);
        log.info("Incident id={} soft-deleted", id);
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Title is required");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException(
                    "Title too long (max " + MAX_TITLE_LENGTH + " chars, got " + title.length() + ")");
        }
    }
}
