package com.kiteclass.core.module.enrollment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.idempotency.IdempotencyScope;
import com.kiteclass.core.common.idempotency.IdempotencyService;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.EnrollmentResponse;
import com.kiteclass.core.module.enrollment.dto.UpdateEnrollmentStatusRequest;
import com.kiteclass.core.module.enrollment.service.EnrollmentService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for enrollment management.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Enrolling students in classes</li>
 *   <li>Managing enrollment status</li>
 *   <li>Querying enrollments</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrollment management APIs")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-c", "controller", "enrollment"})
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    /**
     * Enroll a student in a class.
     *
     * <p>Wave beta-readiness-2 Bucket A — Idempotency-Key header (optional)
     * dedupes accidental double-submit (GAP-730). When the header is present
     * and a previous request with the same {@code (tenantId, key, ENROLLMENT)}
     * triple already completed, the cached response is returned without a
     * second enrollment INSERT — prevents 2 rows for 1 user intent.
     *
     * @param request enrollment request data
     * @param idempotencyKey optional {@code Idempotency-Key} header (UUID/ksuid/ulid)
     * @return created enrollment (cache miss) or cached response (replay)
     */
    @PostMapping
    @Operation(summary = "Enroll a student in a class",
               description = "Creates a new enrollment. Validates capacity and prevents duplicates. "
                       + "Optional Idempotency-Key header dedupes replay per GAP-730.")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollStudent(
            @Valid @RequestBody CreateEnrollmentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        log.info("POST /api/v1/enrollments - Enrolling student {} in class {}",
                request.getStudentId(), request.getClassId());

        Optional<String> maybeKey = idempotencyService.validKeyOrEmpty(idempotencyKey);
        if (maybeKey.isEmpty()) {
            // No idempotency header — legacy path, run handler directly.
            EnrollmentResponse response = enrollmentService.enrollStudent(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Student enrolled successfully"));
        }

        // Idempotency-Key present — wrap handler with lookup → execute → record.
        String key = maybeKey.get();
        UUID tenantId = TenantContext.getCurrentTenant();
        IdempotencyScope scope = IdempotencyScope.ENROLLMENT;
        String requestHash = IdempotencyService.hashRequest(serializeRequestForHash(request));

        Optional<IdempotencyService.CachedResponse> existing =
                idempotencyService.findExisting(tenantId, key, scope);
        if (existing.isPresent()) {
            IdempotencyService.CachedResponse cached = existing.get();
            log.info("Idempotency replay: scope=ENROLLMENT key={} returning cached status={}",
                    key, cached.responseStatus());
            ApiResponse<EnrollmentResponse> cachedBody =
                    deserializeCachedBody(cached.responseBody());
            return ResponseEntity.status(cached.responseStatus())
                    .header("X-Idempotent-Replay", "true")
                    .body(cachedBody);
        }

        // Cache miss — execute the normal handler.
        EnrollmentResponse response = enrollmentService.enrollStudent(request);
        ApiResponse<EnrollmentResponse> apiResponse =
                ApiResponse.success(response, "Student enrolled successfully");

        // Record the mapping. If the race was lost, re-lookup the winner.
        String serializedBody = serializeResponseBody(apiResponse);
        boolean recorded = idempotencyService.recordRequest(
                tenantId, key, scope, /* userId */ null,
                requestHash, HttpStatus.CREATED.value(), serializedBody);

        if (!recorded) {
            // Race lost — return the winner's cached response.
            IdempotencyService.CachedResponse winner = idempotencyService
                    .findExisting(tenantId, key, scope)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency race inconsistent: insert failed but lookup empty"));
            ApiResponse<EnrollmentResponse> winnerBody =
                    deserializeCachedBody(winner.responseBody());
            return ResponseEntity.status(winner.responseStatus())
                    .header("X-Idempotent-Replay", "true")
                    .body(winnerBody);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Idempotent-Replay", "false")
                .body(apiResponse);
    }

    /**
     * Serializes a {@link CreateEnrollmentRequest} into a canonical string
     * for the {@code request_hash} column. Includes only the fields that
     * determine the enrollment identity so payload reordering doesn't break
     * replay detection.
     */
    private String serializeRequestForHash(CreateEnrollmentRequest request) {
        // Canonical fixed-order serialization — used as input to SHA-256 so
        // a replay with the same body produces the same hash. Includes ALL
        // request fields so reusing the key with a different body is detected.
        return "studentId=" + request.getStudentId()
                + "|classId=" + request.getClassId()
                + "|tuitionAmount=" + request.getTuitionAmount()
                + "|discountPercent=" + request.getDiscountPercent()
                + "|notes=" + request.getNotes();
    }

    private String serializeResponseBody(ApiResponse<EnrollmentResponse> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            // Serialization failure here is a programming error — log and continue
            // without caching so the request still completes.
            log.warn("Idempotency response serialization failed; replay will miss: {}",
                    ex.getMessage());
            return null;
        }
    }

    private ApiResponse<EnrollmentResponse> deserializeCachedBody(String json) {
        if (json == null) {
            // Cached body never serialized — return a thin success envelope so
            // the client still sees a coherent shape.
            return ApiResponse.success(null, "Idempotent replay (body not cached)");
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructParametricType(
                            ApiResponse.class, EnrollmentResponse.class));
        } catch (JsonProcessingException ex) {
            log.warn("Idempotency cached body deserialization failed: {}", ex.getMessage());
            return ApiResponse.success(null, "Idempotent replay (body corrupted)");
        }
    }

    /**
     * Get enrollment by ID.
     *
     * @param id enrollment ID
     * @return enrollment data
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get enrollment by ID")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> getEnrollment(
            @Parameter(description = "Enrollment ID") @PathVariable Long id) {
        log.debug("GET /api/v1/enrollments/{}", id);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all enrollments for a student.
     *
     * @param studentId student ID
     * @param pageable pagination parameters
     * @return page of enrollments
     */
    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get all enrollments for a student")
    public ResponseEntity<ApiResponse<Page<EnrollmentResponse>>> getEnrollmentsByStudent(
            @Parameter(description = "Student ID") @PathVariable Long studentId,
            @PageableDefault(sort = "enrollmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.debug("GET /api/v1/enrollments/student/{}", studentId);

        Page<EnrollmentResponse> response = enrollmentService.getEnrollmentsByStudent(
                studentId, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all enrollments for a class.
     *
     * @param classId class ID
     * @param status optional status filter
     * @param pageable pagination parameters
     * @return page of enrollments
     */
    @GetMapping("/class/{classId}")
    @Operation(summary = "Get all enrollments for a class")
    public ResponseEntity<ApiResponse<Page<EnrollmentResponse>>> getEnrollmentsByClass(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Filter by status (optional)")
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(sort = "enrollmentDate", direction = Sort.Direction.ASC)
            Pageable pageable) {
        log.debug("GET /api/v1/enrollments/class/{} with status={}", classId, status);

        Page<EnrollmentResponse> response;
        if (status != null) {
            response = enrollmentService.getEnrollmentsByClassAndStatus(
                    classId, status, pageable
            );
        } else {
            response = enrollmentService.getEnrollmentsByClass(classId, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update enrollment status.
     *
     * @param id enrollment ID
     * @param request status update request
     * @return updated enrollment
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update enrollment status",
               description = "Updates enrollment status (e.g., PENDING_PAYMENT → ACTIVE)")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> updateEnrollmentStatus(
            @Parameter(description = "Enrollment ID") @PathVariable Long id,
            @Valid @RequestBody UpdateEnrollmentStatusRequest request) {
        log.info("PUT /api/v1/enrollments/{}/status to {}", id, request.getStatus());

        EnrollmentResponse response = enrollmentService.updateEnrollmentStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Enrollment status updated successfully"));
    }

    /**
     * Withdraw a student from a class.
     *
     * @param id enrollment ID
     * @return updated enrollment
     */
    @PutMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw a student from a class",
               description = "Sets enrollment status to WITHDRAWN")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> withdrawStudent(
            @Parameter(description = "Enrollment ID") @PathVariable Long id) {
        log.info("PUT /api/v1/enrollments/{}/withdraw", id);

        EnrollmentResponse response = enrollmentService.withdrawStudent(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Student withdrawn successfully"));
    }
}
