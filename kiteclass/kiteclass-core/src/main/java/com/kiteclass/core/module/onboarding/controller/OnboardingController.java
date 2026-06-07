package com.kiteclass.core.module.onboarding.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.onboarding.dto.SampleDataResponse;
import com.kiteclass.core.module.onboarding.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for first-time onboarding helpers.
 *
 * <p>Provides:
 * <ul>
 *   <li>POST /api/v1/onboarding/sample-data — seed a minimal demo data set</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 3.17.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "First-time onboarding helper APIs")
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Seeds a minimal Vietnamese-edu demo data set (1 teacher + 1 course + 1 class
     * + 3 students + 3 enrollments) for the current tenant.
     *
     * <p>Restricted to Owner / Admin / Principal. Idempotent — re-calling after a successful
     * import returns {@code alreadyImported=true} with zero counts.
     *
     * @return ApiResponse wrapping the {@link SampleDataResponse} with HTTP 201
     */
    @PostMapping("/sample-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','PRINCIPAL')")
    @Operation(
            summary = "Import sample data",
            description = "Seeds a believable demo set (1 teacher, 1 class, 3 students) for the current tenant. Idempotent."
    )
    public ApiResponse<SampleDataResponse> importSampleData() {
        log.info("REST request to import onboarding sample data");
        SampleDataResponse response = onboardingService.importSampleData();
        String message = response.alreadyImported()
                ? "Dữ liệu mẫu đã tồn tại"
                : "Đã tạo dữ liệu mẫu: 1 lớp, 3 học sinh, 1 giáo viên";
        return ApiResponse.success(response, message);
    }
}
