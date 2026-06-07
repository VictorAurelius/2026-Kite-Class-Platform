# GAP-1002: grading_scales NULL-default fallback unreachable (tenantFilter+RLS) + new-tenant provisioning seed thiếu

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend (architecture — KC-6)
**Found:** 2026-06-05 (Wave flow-kc6 G1 walk — V88 design discovery)
**Affects:** `GradingScale` entity + `GradingScaleRepository.findDefaultGradingScales` + tenant provisioning

## Problem

`GradingScale extends BaseEntity` → `instance_id NOT NULL` + Hibernate `@Filter tenantFilter (instance_id=:tenantId)` + Postgres RLS `tenant_isolation` (FORCE). Code `findDefaultGradingScales() WHERE instance_id IS NULL` (fallback khi tenant không có scale riêng) **KHÔNG BAO GIỜ trả rows** ở request time: NULL ≠ tenantId (filter) + NULL ≠ current_tenant_id (RLS) → default-scale path chết by design. Đồng thời instance_id NOT NULL → không thể seed NULL default.

**Hệ quả:** scales PHẢI seed per-tenant (instance_id = tenant). V88 (GAP-998) backfill 8 scale × tenant-có-classes hiện tại. NHƯNG:
1. **New tenant** (provisioned sau V88) KHÔNG có scale → calculate/finalize → 404. Cần provisioning hook seed default scales khi tạo tenant.
2. **NULL-default fallback** trong code là dead code (gây hiểu nhầm "có default sẵn"). Hoặc bỏ, hoặc make GradingScale reference-data (remove @Filter + native query bypass) để default thực sự shared.

## Proposed Fix

(defer — architecture decision Phase 1.5+). Options: (a) tenant-provisioning hook seed 8 default scales per new tenant (low-risk, matches V88 backfill); (b) refactor GradingScale thành reference-data (instance_id nullable + `findDefaultGradingScales` nativeQuery bypass tenantFilter/RLS + RLS policy allow NULL-instance reference rows) — system default shared, per-tenant override optional.

## Acceptance Criteria
- [ ] New tenant tự có 8 default grading scales (provisioning hook) HOẶC reference-data refactor
- [ ] calculate/finalize works cho tenant mới không cần manual seed

## Related
- V88 (GAP-998) backfill existing tenants — this gap covers new-tenant + design
- Discovered in: Wave flow-kc6 G1 walk 2026-06-05 (V88 instance_id NOT NULL failure)

## Log

- **2026-06-05 (Wave flow-kc6):** Filed — V88 per-tenant backfill unblocks existing tenants; provisioning hook + reference-data refactor = follow-up (Phase 1.5+ architecture).
- **2026-06-07 (Wave g2-blockers-1 Bucket E, inline):** Chọn Option (a) low-risk provisioning hook. Thêm `DefaultGradingScaleProvisioner.seedDefaults()` (grade module) seed 8 default bands (mirror V88: A+/A/B+/B/C+/C/D/F, BR-GRD-005/006) per-tenant, idempotent (skip nếu tenant đã có scale). Wired vào `TenantProvisioningSaga.provisionInfrastructure()` — best-effort (failure không fail saga; relies on TenantContext set per GAP-1047 cho RLS). New tenant giờ tự có default scales → calculate/finalize không 404. AC#1+#2 met (Option a). **Status 🟡 PARTIAL ~85%** — code fix + compile PASS; **residual:** (a) IT (new-tenant provision → 8 scales seeded + calculate works); (b) Option (b) reference-data refactor (system-default shared) vẫn defer Phase 1.5+ architecture; (c) dead-code `findDefaultGradingScales() WHERE IS NULL` chưa remove (Phase 1.5 cleanup); (d) G3 re-walk pending coordinator.
