package com.kiteclass.core.module.settings.controller;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.settings.entity.BrandingVersion;
import com.kiteclass.core.module.settings.versioning.BrandingVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Branding version history + manual rollback.
 *
 * <p>GAP-033p (Wave 4). Automated rollback arrives in a later wave.
 *
 * @since Wave 4
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding")
@RequiredArgsConstructor
@Tag(name = "BrandingVersion", description = "Branding version history + rollback")
public class BrandingVersionController {

    private final BrandingVersionService brandingVersionService;

    /**
     * List branding versions for an instance — newest first.
     *
     * <p>{@code instanceId} in the path is the tenant UUID (matches every other
     * {@code /api/v1/...} tenant-scoped endpoint in this service). We also guard
     * with {@link TenantContext} so cross-tenant reads are rejected.
     */
    @GetMapping("/{instanceId}/versions")
    @Operation(summary = "List branding version history (paginated, newest first)")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Page<BrandingVersion>> list(
            @PathVariable UUID instanceId,
            Pageable pageable) {
        assertCurrentTenant(instanceId);
        return ResponseEntity.ok(brandingVersionService.listVersions(instanceId, pageable));
    }

    /**
     * Roll the current branding back to the supplied version number. Creates a
     * new version entry so history remains append-only.
     */
    @PostMapping("/{instanceId}/versions/{versionNumber}/rollback")
    @Operation(summary = "Rollback branding to a specific version (admin only)")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<BrandingVersion> rollback(
            @PathVariable UUID instanceId,
            @PathVariable Integer versionNumber) {
        assertCurrentTenant(instanceId);
        log.info("Rollback branding request: instance={} versionNumber={}", instanceId, versionNumber);
        BrandingVersion result = brandingVersionService.rollback(instanceId, versionNumber);
        return ResponseEntity.ok(result);
    }

    private void assertCurrentTenant(UUID instanceId) {
        UUID current = TenantContext.getCurrentTenant();
        if (current != null && !current.equals(instanceId)) {
            throw new IllegalArgumentException("Instance ID does not match tenant context");
        }
    }
}
