package com.kiteclass.core.module.auth.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.auth.dto.LoginRequest;
import com.kiteclass.core.module.auth.dto.LoginResponse;
import com.kiteclass.core.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KC-native login (Wave auth-1, Option B — GAP-725) for tenant-scoped roles
 * PARENT/TEACHER/STUDENT that KH subscription does not issue tokens for.
 *
 * <p>Path is {@code /api/v1/tenant-auth/**} (NOT {@code /api/v1/auth/**}, which the
 * gateway routes to KH subscription for OWNER/STAFF). Public — no JWT required; the
 * gateway forwards this route to core without the auth filter (Bucket C).
 */
@RestController
@RequestMapping("/api/v1/tenant-auth")
@RequiredArgsConstructor
@Tag(name = "Tenant Auth", description = "KC-native login for PARENT/TEACHER/STUDENT (Wave auth-1)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Email + password login for tenant-scoped roles",
            description = "Returns an HS512 access token carrying role + tenantId + referenceId claims. "
                    + "401 INVALID_CREDENTIALS on any failure (uniform, no user-enumeration).")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }
}
