package com.kiteclass.core.module.settings.versioning;

import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.entity.BrandingVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Branding version history + rollback (GAP-033p).
 *
 * <p>MVP scope (Wave 4):
 * <ul>
 *   <li>Snapshot current branding on every update.</li>
 *   <li>List versions (paginated, newest-first).</li>
 *   <li>Manual rollback to a specific version.</li>
 * </ul>
 *
 * <p>Out of scope: automated rollback triggers, A/B branding tests, diff viewer.
 *
 * @since Wave 4 (GAP-033p)
 */
public interface BrandingVersionService {

    /**
     * Record the current state of {@code branding} as a new version and mark it
     * active. The previously active version (if any) is set to {@code active = false}.
     *
     * @param branding   current in-memory Branding row (already saved)
     * @param rollbackOf when non-null, indicates this snapshot was produced by
     *                   rolling back to the supplied version ID; used for audit.
     * @return persisted version
     */
    BrandingVersion snapshot(Branding branding, Long rollbackOf);

    /**
     * Paginated version history for a tenant, newest first.
     */
    Page<BrandingVersion> listVersions(UUID instanceId, Pageable pageable);

    /**
     * Restore the Branding table to the state of the supplied version number
     * for the current tenant. Creates a new version row to preserve the audit
     * trail (never mutates history in-place).
     *
     * @param instanceId    tenant UUID
     * @param versionNumber version to restore
     * @return the newly-created version entry (the restored active state)
     * @throws IllegalArgumentException when the version doesn't exist for the tenant
     */
    BrandingVersion rollback(UUID instanceId, Integer versionNumber);
}
