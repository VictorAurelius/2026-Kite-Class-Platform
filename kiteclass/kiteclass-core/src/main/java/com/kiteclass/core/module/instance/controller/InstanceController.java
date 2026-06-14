package com.kiteclass.core.module.instance.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.instance.dto.InitiateInstanceRequest;
import com.kiteclass.core.module.instance.dto.InstanceResponse;
import com.kiteclass.core.module.instance.dto.MarkBrandingCompletedRequest;
import com.kiteclass.core.module.instance.dto.MarkFailedRequest;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for frontend instance lifecycle (GAP-009, Wave 3 Sub-PR 3.4).
 *
 * <p>Every write endpoint delegates to {@link InstanceLifecycleService}, which enforces
 * the State Pattern transitions and emits outbox events per ADR-007. Controllers MUST
 * NOT set status fields directly.
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/instances")
@RequiredArgsConstructor
@Tag(name = "FrontendInstance", description = "Frontend instance provisioning lifecycle APIs")
public class InstanceController {

    /**
     * GAP-1359: hard cap on the unfiltered list response. {@code FrontendInstance} is a
     * platform-level table that grows with the number of provisioned instances; without a
     * bound the {@code status == null} branch materialised + serialised the whole table in a
     * single response. The list shape is retained (FE consumers expect a JSON array) — this is
     * the "hard cap + documented exemption" path of GAP-1359's AC. A full {@code Pageable}
     * envelope is deferred to avoid breaking the array contract.
     */
    static final int INSTANCE_LIST_MAX = 500;

    private final InstanceLifecycleService lifecycle;
    private final FrontendInstanceRepository repository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Initiate provisioning", description = "NOT_STARTED → INITIALIZING")
    public ApiResponse<InstanceResponse> initiate(@Valid @RequestBody InitiateInstanceRequest request) {
        log.info("REST initiate tenant={} slug={}", request.tenantId(), request.slug());
        FrontendInstance i = lifecycle.initiate(request.tenantId(), request.slug());
        return ApiResponse.success(InstanceResponse.from(i), "Instance provisioning initiated");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get instance by id")
    public ApiResponse<InstanceResponse> get(@PathVariable Long id) {
        FrontendInstance i = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FrontendInstance not found: id=" + id));
        return ApiResponse.success(InstanceResponse.from(i));
    }

    @GetMapping
    @Operation(summary = "List instances (optionally filtered by status)")
    public ApiResponse<List<InstanceResponse>> list(
            @RequestParam(required = false) FrontendInstanceStatus status) {
        List<FrontendInstance> all = (status == null)
                ? repository.findAll(PageRequest.of(0, INSTANCE_LIST_MAX,
                        Sort.by(Sort.Direction.DESC, "id"))).getContent()
                : repository.findByStatusAndDeletedFalse(status);
        return ApiResponse.success(all.stream().map(InstanceResponse::from).toList());
    }

    @PostMapping("/{id}/infrastructure-ready")
    @Operation(summary = "Mark infrastructure ready", description = "INITIALIZING → GENERATING")
    public ApiResponse<InstanceResponse> markInfrastructureReady(@PathVariable Long id) {
        FrontendInstance i = lifecycle.markInfrastructureReady(id);
        return ApiResponse.success(InstanceResponse.from(i));
    }

    @PostMapping("/{id}/branding-completed")
    @Operation(summary = "Mark branding completed",
            description = "GENERATING|REGENERATING → DEPLOYED (brandingVersion++)")
    public ApiResponse<InstanceResponse> markBrandingCompleted(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MarkBrandingCompletedRequest request) {
        String url = request != null ? request.frontendUrl() : null;
        FrontendInstance i = lifecycle.markBrandingCompleted(id, url);
        return ApiResponse.success(InstanceResponse.from(i));
    }

    @PostMapping("/{id}/rebrand")
    @Operation(summary = "Trigger rebrand", description = "DEPLOYED → REGENERATING")
    public ApiResponse<InstanceResponse> rebrand(@PathVariable Long id) {
        FrontendInstance i = lifecycle.rebrand(id);
        return ApiResponse.success(InstanceResponse.from(i));
    }

    @PostMapping("/{id}/failed")
    @Operation(summary = "Mark failed", description = "* → FAILED (retryCount++)")
    public ApiResponse<InstanceResponse> markFailed(
            @PathVariable Long id,
            @Valid @RequestBody MarkFailedRequest request) {
        FrontendInstance i = lifecycle.markFailed(id, request.reason());
        return ApiResponse.success(InstanceResponse.from(i));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry failed instance",
            description = "FAILED → INITIALIZING (blocks after MAX_RETRIES)")
    public ApiResponse<InstanceResponse> retry(@PathVariable Long id) {
        FrontendInstance i = lifecycle.retry(id);
        return ApiResponse.success(InstanceResponse.from(i));
    }
}
