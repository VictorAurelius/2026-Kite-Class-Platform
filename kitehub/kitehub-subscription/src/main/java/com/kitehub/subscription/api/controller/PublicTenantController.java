package com.kitehub.subscription.api.controller;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.api.dto.TenantResolveDto;
import com.kitehub.subscription.module.tenant.service.TenantLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Anonymous public tenant-resolve endpoint
 * (Wave tenant-domain-1 Bucket B — GAP-813).
 *
 * <p>FE middleware (Bucket C — GAP-811) calls this to resolve a request
 * {@code Host} header subdomain to a tenant UUID + status BEFORE routing.
 * No JWT required; rate-limited at the gateway (30 req/min/IP) to defend
 * against enumeration scans.</p>
 *
 * <p>Contract: {@code documents/01-business/kitehub/marketing/api-contract.md}
 * §9. Mapping rules:
 * <ul>
 *   <li>200 + DTO when row found AND status == {@link InstanceStatus#ACTIVE}</li>
 *   <li>400 {@code INVALID_SLUG_FORMAT} when slug fails regex validation</li>
 *   <li>404 {@code TENANT_NOT_FOUND} when no matching row</li>
 *   <li>410 {@code TENANT_SUSPENDED} / {@code TENANT_DELETED} when row exists
 *       but status != ACTIVE</li>
 * </ul></p>
 *
 * <p>Sensitive fields (database URL/credentials, contact email, subscription
 * dates, owner UUID) are deliberately NOT exposed by the DTO projection —
 * see {@link TenantLookupService#toDto(Instance)}.</p>
 *
 * @author KiteHub Team
 * @since Wave tenant-domain-1 Bucket B (GAP-813)
 */
@RestController
@RequestMapping("/api/v1/public/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Tenant Resolve",
        description = "Anonymous tenant lookup by subdomain slug (Wave tenant-domain-1).")
public class PublicTenantController {

    /**
     * Subdomain slug regex per
     * {@code documents/01-business/kitehub/marketing/api-contract.md} §9.1.1:
     * lowercase letters/digits with optional internal hyphens; single-char OK;
     * no leading or trailing hyphen; total length 1-50.
     */
    static final Pattern SLUG_PATTERN = Pattern.compile(
            "^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$");

    /**
     * Hard upper bound on slug length per contract §9.1.1.
     */
    static final int MAX_SLUG_LENGTH = 50;

    private final TenantLookupService tenantLookupService;

    /**
     * Resolve a subdomain slug to a tenant UUID + display name + status.
     *
     * @param slug lowercase-kebab subdomain (e.g. {@code sky})
     * @return 200 + {@link TenantResolveDto} on ACTIVE match; 400 / 404 / 410
     *         per contract §9.1.4
     */
    @Operation(summary = "Resolve tenant by subdomain slug",
            description = "Public endpoint. FE middleware uses this to route Host header "
                    + "subdomain → tenant UUID before requesting tenant-scoped APIs. "
                    + "Rate-limited 30 req/min/IP at the gateway.")
    @GetMapping("/by-subdomain/{slug}")
    public ResponseEntity<?> resolveBySubdomain(@PathVariable("slug") String slug) {
        // Inline slug validation — explicit shape per contract; avoids relying
        // on @Validated annotation processor wiring just for one path var.
        if (slug == null || slug.isEmpty() || slug.length() > MAX_SLUG_LENGTH
                || !SLUG_PATTERN.matcher(slug).matches()) {
            log.debug("Public tenant resolve: invalid slug format '{}'", slug);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "INVALID_SLUG_FORMAT",
                            "message", "Subdomain must match ^[a-z0-9][a-z0-9-]*[a-z0-9]$ "
                                    + "(or single char), length 1-50."
                    ));
        }

        Optional<Instance> match = tenantLookupService.findBySubdomain(slug);

        if (match.isEmpty()) {
            log.debug("Public tenant resolve: slug '{}' not found", slug);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "TENANT_NOT_FOUND",
                            "message", "Tenant '" + slug + "' was not found."
                    ));
        }

        Instance instance = match.get();
        InstanceStatus status = instance.getStatus();

        if (status == InstanceStatus.ACTIVE || status == InstanceStatus.TRIAL) {
            // TRIAL surfaces as ACTIVE to FE — tenant is reachable; trial-vs-paid
            // distinction lives behind authenticated endpoints, not in the
            // public resolve. We rewrite status to ACTIVE so FE doesn't have to
            // learn 2 "OK" states. See contract §9.1.3.
            TenantResolveDto dto = tenantLookupService.toDto(instance);
            // Force status field to ACTIVE for the public projection regardless
            // of internal trial vs active state.
            return ResponseEntity.ok(TenantResolveDto.builder()
                    .id(dto.getId())
                    .subdomain(dto.getSubdomain())
                    .name(dto.getName())
                    .status(InstanceStatus.ACTIVE.name())
                    .build());
        }

        // Suspended / deleted / purged / pending → 410 GONE per contract.
        String errorCode = switch (status) {
            case SUSPENDED -> "TENANT_SUSPENDED";
            case DELETED, PURGED -> "TENANT_DELETED";
            case PENDING -> "TENANT_NOT_FOUND";
            default -> "TENANT_NOT_FOUND";
        };

        if ("TENANT_NOT_FOUND".equals(errorCode)) {
            // Defensive — PENDING rows are pre-provisioning, behave like missing.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", errorCode,
                            "message", "Tenant '" + slug + "' was not found."
                    ));
        }

        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of(
                        "error", errorCode,
                        "message", "Tenant '" + slug + "' is " + status.name().toLowerCase() + ".",
                        "status", status.name()
                ));
    }
}
