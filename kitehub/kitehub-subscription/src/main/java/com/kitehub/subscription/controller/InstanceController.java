package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.UpdateInstanceRequest;
import com.kitehub.subscription.service.InstanceService;
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

    /**
     * Create a new trial instance.
     *
     * @param request create instance request
     * @return created instance response
     */
    @PostMapping
    public ResponseEntity<InstanceResponse> createInstance(@Valid @RequestBody CreateInstanceRequest request) {
        InstanceResponse response = instanceService.createTrialInstance(request);
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
}
