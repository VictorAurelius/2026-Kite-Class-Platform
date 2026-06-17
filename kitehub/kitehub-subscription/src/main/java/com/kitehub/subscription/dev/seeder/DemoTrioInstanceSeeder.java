package com.kitehub.subscription.dev.seeder;

import com.kitehub.subscription.repository.InstanceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Idempotently seeds the demo-trio kitehub {@code instances} rows when the
 * {@code dev} profile is active (GAP-1180).
 *
 * <p><b>Why this exists:</b> {@code PublicTenantController.resolveBySubdomain}
 * (Wave tenant-domain-1 / GAP-813) resolves a subdomain slug to a tenant UUID by
 * reading the kitehub {@code instances} table via
 * {@link InstanceRepository#findBySubdomainAndDeletedFalse(String)}. The FE
 * middleware (GAP-811) calls that endpoint to route Host→tenant. Without a kitehub
 * {@code instances} row, {@code by-subdomain/co-ha-toan} returns 404 and the
 * landing-100 subdomain walk is blocked at step 1.</p>
 *
 * <p>{@code BrandingDataSeeder} (kiteclass-core, {@code dev} profile) seeds only the
 * <i>kiteclass-side</i> rows ({@code FrontendInstance} + {@code Branding} +
 * {@code LandingPage}) for the demo-trio. It never creates the kitehub
 * {@code instances} row that the public resolve endpoint reads — a cross-service
 * seed-coverage gap. This seeder closes that gap on the kitehub side.</p>
 *
 * <p><b>UUID parity (canonical):</b> the {@code instances.id} for each demo tenant
 * MUST equal the {@code instance_id} that {@code BrandingDataSeeder} stamps on the
 * kiteclass branding/landing rows (shared-DB + RLS model per ADR-023). The public
 * resolve returns this UUID and the FE fetches landing keyed by it. The canonical
 * scheme is {@code a1100000-…-0001} (co-ha-toan) / {@code b1100000-…-0002}
 * (thay-nhi-hoa) — matching {@code BrandingDataSeeder.HA_TENANT_ID} /
 * {@code NHI_TENANT_ID}. {@code seed-landing-content.sql} was reconciled to this
 * scheme in the same PR (was outlier {@code ad0fa96e…} / {@code 0abe093c…}).</p>
 *
 * <p>Idempotent: skips a tenant when a non-deleted row already exists for its
 * subdomain, so it is safe to re-run on every boot.</p>
 *
 * <p>A native {@code INSERT} is used (not {@code repository.save}) because the
 * {@code @GeneratedValue(strategy = UUID)} generator overwrites any pre-assigned
 * id on both {@code persist} and {@code merge}; a native insert is the only
 * reliable way to pin the canonical UUID. The trade-off is an explicit column
 * list — kept in sync with the {@code Instance} entity NOT NULL fields.</p>
 *
 * <p>Tracking: GAP-1180.</p>
 */
@Component
// Demo-trio seed runs on `dev` (every local boot) OR the `demo-seed` profile —
// activate `demo-seed` on any environment (incl production:
// SPRING_PROFILES_ACTIVE=prod,demo-seed) to reproduce the demo identically.
// Idempotent upsert → safe to re-run. Order on prod: this (instances) BEFORE
// kiteclass-core BrandingDataSeeder + DemoAcademicSeeder (which need the rows).
@Profile({"dev", "demo-seed"})
@RequiredArgsConstructor
@Slf4j
public class DemoTrioInstanceSeeder {

    /** Cô Nguyễn Thị Hà — gói FREE, Toán tiểu học. Matches BrandingDataSeeder.HA_TENANT_ID. */
    static final String HA_INSTANCE_ID = "a1100000-0000-4000-a000-000000000001";
    static final String HA_SUBDOMAIN = "co-ha-toan";
    static final String HA_ORG_NAME = "Lớp Toán cô Nguyễn Thị Hà";
    static final String HA_OWNER_ID = "a1100000-0000-4000-a000-0000000000a1";
    static final String HA_CONTACT_EMAIL = "co-ha-toan@demo.kite.local";
    static final String HA_TIER = "FREE";

    /** Thầy Nguyễn Đình Nhì — gói trả phí (PREMIUM), Hóa THCS. Matches BrandingDataSeeder.NHI_TENANT_ID. */
    static final String NHI_INSTANCE_ID = "b1100000-0000-4000-a000-000000000002";
    static final String NHI_SUBDOMAIN = "thay-nhi-hoa";
    static final String NHI_ORG_NAME = "Hóa học THCS thầy Nguyễn Đình Nhì";
    static final String NHI_OWNER_ID = "b1100000-0000-4000-a000-0000000000b2";
    static final String NHI_CONTACT_EMAIL = "thay-nhi-hoa@demo.kite.local";
    // PricingTier has no literal "PAID"; the paid tier for the demo is PREMIUM,
    // matching seed-landing-content.sql §3 ("gói PREMIUM" for thầy Nhì).
    static final String NHI_TIER = "PREMIUM";

    private final InstanceRepository instanceRepository;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /** Immutable spec for one demo-trio kitehub {@code instances} row seed. */
    private record TrioInstanceSpec(String id, String subdomain, String organizationName,
                                    String ownerId, String contactEmail, String tier) {
    }

    /** Triggered after the Spring context is fully initialized. */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        List<TrioInstanceSpec> specs = List.of(
                new TrioInstanceSpec(HA_INSTANCE_ID, HA_SUBDOMAIN, HA_ORG_NAME,
                        HA_OWNER_ID, HA_CONTACT_EMAIL, HA_TIER),
                new TrioInstanceSpec(NHI_INSTANCE_ID, NHI_SUBDOMAIN, NHI_ORG_NAME,
                        NHI_OWNER_ID, NHI_CONTACT_EMAIL, NHI_TIER));
        for (TrioInstanceSpec spec : specs) {
            seedInstance(spec);
        }
    }

    /**
     * Seeds one demo-trio kitehub {@code instances} row. Idempotent by subdomain.
     * Each tenant runs in its own transaction so one failure doesn't abort the rest.
     */
    private void seedInstance(TrioInstanceSpec spec) {
        try {
            if (instanceRepository.existsBySubdomainAndDeletedFalse(spec.subdomain())) {
                log.info("Demo-trio instances row already present (subdomain={}). Skipping.",
                        spec.subdomain());
                return;
            }
            transactionTemplate.executeWithoutResult(status -> entityManager.createNativeQuery(
                            "INSERT INTO instances "
                                    + "(id, subdomain, slug, organization_name, owner_id, contact_email, "
                                    + " tier, status, database_url, database_username, database_password, "
                                    + " email_notifications, trial_reminders, migration_phase, vertical_type, "
                                    + " domain_status, deleted, created_at, updated_at) "
                                    + "VALUES "
                                    + "(CAST(:id AS uuid), :subdomain, :slug, :orgName, CAST(:ownerId AS uuid), "
                                    + " :contactEmail, :tier, 'ACTIVE', 'pending', 'pending', 'pending', "
                                    + " true, true, 'NONE', 'CENTER', 'NONE', false, now(), now())")
                    .setParameter("id", spec.id())
                    .setParameter("subdomain", spec.subdomain())
                    .setParameter("slug", spec.subdomain())
                    .setParameter("orgName", spec.organizationName())
                    .setParameter("ownerId", spec.ownerId())
                    .setParameter("contactEmail", spec.contactEmail())
                    .setParameter("tier", spec.tier())
                    .executeUpdate());
            log.info("Seeded demo-trio instances row (subdomain={}, id={}, tier={}).",
                    spec.subdomain(), spec.id(), spec.tier());
        } catch (Exception ex) {
            // Best-effort dev seed — never block boot if one tenant insert fails.
            log.warn("Failed to seed demo-trio instances row (subdomain={}): {}",
                    spec.subdomain(), ex.getMessage());
        }
    }
}
