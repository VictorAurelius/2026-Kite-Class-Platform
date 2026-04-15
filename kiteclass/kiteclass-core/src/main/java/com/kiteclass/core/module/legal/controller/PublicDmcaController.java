package com.kiteclass.core.module.legal.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.legal.dto.DmcaTakedownDto;
import com.kiteclass.core.module.legal.dto.DmcaTakedownResponse;
import com.kiteclass.core.module.legal.entity.DmcaTakedownRequest;
import com.kiteclass.core.module.legal.service.DmcaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public DMCA intake endpoint (ADR-012 Track 2).
 *
 * <p>Mounted under {@code /public/**} intentionally — {@code /internal/**} has a different
 * security scope (service-to-service authentication) and would reject unauthenticated browser
 * submissions. Rate-limiting is provided by the existing gateway-level filter; no route-specific
 * filter is configured here.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
@Slf4j
@RestController
@RequestMapping("/public/dmca")
@RequiredArgsConstructor
@Tag(name = "DMCA", description = "Public DMCA takedown intake")
public class PublicDmcaController {

    private final DmcaService dmcaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a DMCA takedown notice",
            description = "Public intake — creates a PENDING takedown for human review.")
    public ApiResponse<DmcaTakedownResponse> submit(@Valid @RequestBody DmcaTakedownDto body) {
        log.info("[dmca] public intake from reporter={}", body.reporterEmail());
        DmcaTakedownRequest entity = DmcaTakedownRequest.builder()
                .reporterEmail(body.reporterEmail())
                .reporterName(body.reporterName())
                .allegedInfringingUrl(body.allegedInfringingUrl())
                .copyrightedWorkDescription(body.copyrightedWorkDescription())
                .build();
        DmcaTakedownRequest saved = dmcaService.receiveTakedown(entity);
        return ApiResponse.success(DmcaTakedownResponse.from(saved),
                "DMCA notice received; our team will review shortly.");
    }
}
