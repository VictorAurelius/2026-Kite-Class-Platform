package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.parent.dto.ParentInternalResponse;
import com.kiteclass.core.module.parent.service.ParentService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service endpoint consumed by the Gateway to enrich JWT claims
 * (namely {@code linked_student_ids}) and the login response profile for
 * parent accounts.
 *
 * <p>Protected by {@link com.kiteclass.core.config.InternalRequestFilter}'s
 * HMAC verification. Hidden from public Swagger per convention.
 *
 * @since 2.14.0
 */
@Slf4j
@RestController
@RequestMapping("/internal/parents")
@RequiredArgsConstructor
@Hidden
@Tag(name = "Internal Parent API", description = "Service-to-service parent profile lookup")
public class InternalParentController {

    private final ParentService parentService;

    @GetMapping("/{id}")
    @Operation(summary = "Get parent profile with linked student ids (Internal)")
    public ResponseEntity<ApiResponse<ParentInternalResponse>> getParent(@PathVariable Long id) {
        log.debug("Internal API: get parent profile, id={}", id);
        ParentInternalResponse response = parentService.getInternalParentView(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
