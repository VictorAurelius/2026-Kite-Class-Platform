package com.kitehub.admin.controller;

import com.kitehub.admin.dto.InstanceSummary;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin instances v1 REST API — exposes instance listing + detail endpoints at the canonical
 * {@code /api/v1/admin/instances} path expected by frontend + integration consumers.
 *
 * <p>Fixes Wave 90 walkthrough sub-finding (404 at {@code /api/v1/admin/instances}): legacy
 * {@link AdminController} mounts at {@code /api/platform/admin} prefix; this v1 controller
 * provides the canonical path. Both prefixes coexist in Phase 1 BETA — legacy path
 * deprecation deferred to Phase 1.5+ when frontend consolidation complete.</p>
 *
 * <p>Per Wave 92 Bucket D, this controller is a thin read-only stub: paginated list + detail
 * GET endpoints only. Mutation operations (suspend/activate) remain on legacy
 * {@link AdminController} until v1 mutation scope plan (defer follow-up gap).</p>
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/instances")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin v1 - Instances", description = "Admin instances listing + detail (Wave 92 Bucket D — Wave 90 404 fix)")
public class AdminInstancesController {

    private final InstanceRepository instanceRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Default page size for list endpoint (mirrors {@link AdminController#DEFAULT_PAGE_SIZE}).
     */
    static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Hard cap for page size to prevent unbounded scans (mirrors {@link AdminController#MAX_PAGE_SIZE}).
     */
    static final int MAX_PAGE_SIZE = 100;

    /**
     * Get paginated list of instances with summary information.
     *
     * @param pageable pagination + sort (default size 20, max 100)
     * @return page of instance summaries
     */
    @GetMapping
    @Operation(summary = "List instances", description = "Paginated list of instances with summary info")
    public ResponseEntity<Page<InstanceSummary>> listInstances(
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        Pageable safe = clampPageable(pageable);
        log.info("Admin v1 list instances page={} size={}", safe.getPageNumber(), safe.getPageSize());

        Page<Instance> page = instanceRepository.findAll(safe);
        Page<InstanceSummary> summaries = page.map(this::convertToSummary);

        return ResponseEntity.ok(summaries);
    }

    /**
     * Get instance detail by ID.
     *
     * @param id instance ID
     * @return instance summary
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get instance detail", description = "Get full instance summary by ID")
    public ResponseEntity<InstanceSummary> getInstance(@PathVariable UUID id) {
        log.info("Admin v1 get instance detail: {}", id);

        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + id));

        return ResponseEntity.ok(convertToSummary(instance));
    }

    /**
     * Enforce page size ceiling — same pattern as legacy {@link AdminController#clampPageable}.
     */
    static Pageable clampPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE);
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }

    /**
     * Convert Instance entity to InstanceSummary DTO. Mirrors {@link AdminController}'s convert logic
     * to maintain response shape consistency across legacy + v1 endpoints.
     */
    private InstanceSummary convertToSummary(Instance instance) {
        Subscription subscription = subscriptionRepository.findActiveByInstanceId(instance.getId())
                .orElse(null);

        return InstanceSummary.builder()
                .id(instance.getId())
                .organizationName(instance.getOrganizationName())
                .subdomain(instance.getSubdomain())
                .status(instance.getStatus().name())
                .tier(subscription != null ? subscription.getTier().name() : "TRIAL")
                .ownerEmail(null)
                .ownerPhone(null)
                .trialEndDate(instance.getTrialExpiresAt())
                .subscriptionEndDate(subscription != null ? subscription.getExpiresAt() : null)
                .databaseUrl(instance.getDatabaseUrl())
                .totalUsers(0L)
                .totalStudents(0L)
                .totalCourses(0L)
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }
}
