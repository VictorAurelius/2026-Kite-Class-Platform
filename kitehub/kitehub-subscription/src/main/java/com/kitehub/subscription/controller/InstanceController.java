package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.RegisterInstanceRequest;
import com.kitehub.subscription.dto.RegisterInstanceResponse;
import com.kitehub.subscription.dto.TrialStatusResponse;
import com.kitehub.subscription.dto.UpdateInstanceRequest;
import com.kitehub.subscription.service.InstanceService;
import com.kitehub.subscription.service.TrialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for instance management.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/platform/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;
    private final TrialService trialService;

    /**
     * Create a new trial instance.
     *
     * @param request create instance request
     * @return created instance response
     */
    /**
     * List all instances.
     *
     * @return list of all instance responses
     */
    @GetMapping
    public ResponseEntity<List<InstanceResponse>> listInstances() {
        List<InstanceResponse> responses = instanceService.listAllInstances();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<InstanceResponse> createInstance(@Valid @RequestBody CreateInstanceRequest request) {
        InstanceResponse response = instanceService.createTrialInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Register a new trial instance (self-service registration).
     * Creates owner and instance in one step.
     *
     * @param request registration request
     * @return registration response with user info and tokens
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterInstanceResponse> registerInstance(@Valid @RequestBody RegisterInstanceRequest request) {
        RegisterInstanceResponse response = instanceService.registerInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get instance by ID.
     *
     * @param id instance UUID
     * @return instance response
     */
    @GetMapping("/{id}")
    public ResponseEntity<InstanceResponse> getInstanceById(@PathVariable UUID id) {
        InstanceResponse response = instanceService.getInstanceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get instance by subdomain.
     *
     * @param subdomain subdomain
     * @return instance response
     */
    @GetMapping("/subdomain/{subdomain}")
    public ResponseEntity<InstanceResponse> getInstanceBySubdomain(@PathVariable String subdomain) {
        InstanceResponse response = instanceService.getInstanceBySubdomain(subdomain);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all instances for owner.
     *
     * @param ownerId owner UUID
     * @return list of instance responses
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<InstanceResponse>> getInstancesByOwner(@PathVariable UUID ownerId) {
        List<InstanceResponse> responses = instanceService.getInstancesByOwner(ownerId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Update instance.
     *
     * @param id instance UUID
     * @param request update request
     * @return updated instance response
     */
    @PutMapping("/{id}")
    public ResponseEntity<InstanceResponse> updateInstancePut(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInstanceRequest request
    ) {
        InstanceResponse response = instanceService.updateInstance(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InstanceResponse> updateInstance(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInstanceRequest request
    ) {
        InstanceResponse response = instanceService.updateInstance(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete instance (soft delete).
     *
     * @param id instance UUID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable UUID id) {
        instanceService.deleteInstance(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get trial status for instance.
     *
     * @param id instance UUID
     * @return trial status response
     */
    @GetMapping("/{id}/trial-status")
    public ResponseEntity<TrialStatusResponse> getTrialStatus(@PathVariable UUID id) {
        TrialStatusResponse response = trialService.getTrialStatus(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Extend trial period (admin only).
     *
     * @param id instance UUID
     * @param days number of days to extend
     * @return no content
     */
    @PostMapping("/{id}/extend-trial")
    public ResponseEntity<Void> extendTrial(
        @PathVariable UUID id,
        @RequestParam int days
    ) {
        trialService.extendTrial(id, days);
        return ResponseEntity.noContent().build();
    }
}
