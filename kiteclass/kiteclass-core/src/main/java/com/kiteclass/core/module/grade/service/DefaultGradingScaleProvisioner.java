package com.kiteclass.core.module.grade.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.grade.entity.GradingScale;
import com.kiteclass.core.module.grade.repository.GradingScaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * GAP-1002: seeds the 8 default grading scales for a freshly-provisioned tenant.
 *
 * <p>{@code GradingScale} extends {@code BaseEntity} (instance_id NOT NULL +
 * Hibernate tenantFilter + Postgres RLS), so the legacy {@code instance_id IS NULL}
 * "system default" fallback can never return rows at request time. Scales MUST be
 * seeded per-tenant. V88 (GAP-998) backfilled existing tenants; this provisioner
 * closes the gap for tenants created AFTER V88 — without it, calculate/finalize
 * returns 404 on a brand-new tenant.</p>
 *
 * <p>Band values mirror V88 (BR-GRD-005 letter bands + BR-GRD-006 GPA). Idempotent:
 * skips seeding if the tenant already has any grading scale.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultGradingScaleProvisioner {

    private final GradingScaleRepository gradingScaleRepository;

    /** scale_name, letter, min, max, gpa, is_passing — mirrors V88 CROSS JOIN VALUES. */
    private static final Object[][] DEFAULT_BANDS = {
            {"A+", "95.00", "100.00", "4.00", true},
            {"A",  "90.00", "94.99",  "4.00", true},
            {"B+", "85.00", "89.99",  "3.30", true},
            {"B",  "80.00", "84.99",  "3.00", true},
            {"C+", "75.00", "79.99",  "2.30", true},
            {"C",  "70.00", "74.99",  "2.00", true},
            {"D",  "60.00", "69.99",  "1.00", true},
            {"F",  "0.00",  "59.99",  "0.00", false},
    };

    /**
     * Seed default grading scales for the current tenant if it has none.
     * Relies on {@link TenantContext} being set to the new tenant (saga consumer
     * sets it per GAP-1047) so RLS + tenantFilter admit the inserts.
     *
     * @param instanceId the tenant/instance UUID to seed for
     */
    @Transactional
    public void seedDefaults(UUID instanceId) {
        UUID tenant = instanceId != null ? instanceId : TenantContext.getCurrentTenant();
        if (tenant == null) {
            log.warn("[provisioning] grading-scale seed skipped — no tenant context");
            return;
        }

        List<GradingScale> existing =
                gradingScaleRepository.findByInstanceIdAndDeletedFalseOrderByMinScoreDesc(tenant);
        if (!existing.isEmpty()) {
            log.info("[provisioning] grading scales already present for tenant={} (count={}); skip seed",
                    tenant, existing.size());
            return;
        }

        for (Object[] band : DEFAULT_BANDS) {
            GradingScale scale = GradingScale.builder()
                    .scaleName("Default")
                    .letterGrade((String) band[0])
                    .minScore(new BigDecimal((String) band[1]))
                    .maxScore(new BigDecimal((String) band[2]))
                    .gpaValue(new BigDecimal((String) band[3]))
                    .isPassing((Boolean) band[4])
                    .isDefault(true)
                    .build();
            scale.setInstanceId(tenant);
            gradingScaleRepository.save(scale);
        }

        log.info("[provisioning] seeded {} default grading scales for tenant={}",
                DEFAULT_BANDS.length, tenant);
    }
}
