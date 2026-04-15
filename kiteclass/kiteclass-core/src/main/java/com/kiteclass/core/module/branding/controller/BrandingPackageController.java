package com.kiteclass.core.module.branding.controller;

import com.kiteclass.core.module.branding.dto.BrandingPackage;
import com.kiteclass.core.module.branding.service.BrandingPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single composite endpoint per ADR-009 — returns theme + assets + metadata so FE
 * makes 1 round-trip instead of 10. ETag enables 304 Not Modified for repeat visits.
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4, ADR-009)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding")
@RequiredArgsConstructor
@Tag(name = "BrandingPackage", description = "Composite branding package API")
public class BrandingPackageController {

    private final BrandingPackageService packageService;

    @GetMapping("/{instanceId}/package")
    @Operation(summary = "Get composite branding package",
            description = "Returns theme + assets + metadata with ETag for 304 revalidation")
    public ResponseEntity<BrandingPackage> get(
            @PathVariable Long instanceId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        BrandingPackage pkg = packageService.getByInstanceId(instanceId);
        String etag = buildEtag(pkg);
        if (etag.equals(ifNoneMatch)) {
            log.debug("[branding-package] 304 instance={} etag={}", instanceId, etag);
            return ResponseEntity.status(304).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(pkg);
    }

    static String buildEtag(BrandingPackage pkg) {
        return "W/\"v" + pkg.brandingVersion() + "-" + Integer.toHexString(pkg.hashCode()) + "\"";
    }
}
