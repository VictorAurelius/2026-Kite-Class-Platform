# Multi-tenancy — Business Rules

**Domain:** kiteclass / multi-tenancy
**Layer:** Layer 1 (rules.md) of 3-layer business docs per CLAUDE.md §Business Logic Documents
**Owner:** Backend (security-critical) + Compliance
**Created:** 2026-05-11 (GAP-466 / Wave 56 Phase 3)

---

## BR-MULTITENANT-001: Multi-tenant data isolation (defense-in-depth)

- **Value:** Every tenant-scoped table (51 in `kiteclass-core`, 12 in `kitehub-subscription`)
  MUST enforce isolation at two layers:
  1. **Code layer** — Hibernate `@FilterDef("tenantFilter")` on `BaseEntity.instanceId`,
     activated per HTTP request by `TenantFilterInterceptor` from the `X-Tenant-Id` header.
  2. **Database layer** — Postgres Row-Level Security (`ENABLE ROW LEVEL SECURITY` +
     `FORCE ROW LEVEL SECURITY` on `kiteclass-core` tables; `ENABLE` only on
     `kitehub-subscription` tables until that service gains per-request tenant context)
     with policy
     `tenant_isolation USING (instance_id = current_setting('app.current_tenant_id', true)::uuid)`.
     `TenantAwareDataSourceInterceptor` issues `SET LOCAL app.current_tenant_id = ?`
     at every `@Transactional` boundary.

- **Source:**
  - **VN law / regulation:** PDPL 2023 (Personal Data Protection Law, effective 2026-07-01)
    Art 23 — controllers MUST keep personal data segregated per the data subject's consent
    scope; cross-tenant access without lawful basis is unlawful processing.
  - **Industry standard:** AWS Well-Architected SaaS Lens — "Pool" multi-tenant model
    recommends RLS as defense-in-depth (https://docs.aws.amazon.com/wellarchitected/latest/saas-lens/).
  - **Internal incident-prevention:** GAP-466 §Threat-model documents the cost of a
    cross-tenant leak (legal liability + reputational damage + PDPL violation).

- **Rationale:** Why two layers, not just one?
  - Layer 1 alone is bypassable: custom JPQL `@Query`, native SQL via `entityManager.createNativeQuery`,
    projection DTOs, and `JdbcTemplate` direct queries do not run through the Hibernate filter.
    A single developer-error forgetting `WHERE instance_id = ?` = silent leak.
  - Layer 2 alone is sufficient for security but Layer 1 keeps Hibernate's query plan tidy
    (filter applied at query-build time, not just at DB execution), and provides earlier
    feedback to developers (the query never even leaves the JVM).
  - **Why FORCE on kc-core but not kh-subscription?** kc-core has a per-request
    `TenantContext` populated from `X-Tenant-Id`; kh-subscription is a control-plane service
    that does not currently carry per-request tenant identity. `FORCE` would default-deny
    every kh-subscription query and break the service. Tracked as future-tightening when
    kh-subscription gains tenant-aware request flow.

- **Reviewer:** @nguyenvankiet (acting Security + Compliance Owner, solo-dev, 2026-05-11).
  Formal external review queued — to be scheduled with engaged legal counsel during
  Phase 1 BETA → Phase 2 transition (per CLAUDE.md current-phase context). Per
  `.claude/rules/business-logic-review.md` §2.3 solo-dev exemption clause: role is
  explicitly stated + follow-up obligation attached.

- **Compliance check:** **Compliant**
  - **PDPL 2023 Art 23 (data segregation):** Layer 2 RLS makes cross-tenant reads
    structurally impossible at the database layer even when the application-layer guard
    fails. Evidence: `RLSEnforcementIT` + `TenantIsolationIT`.
  - **Luật An ninh mạng 2018 + Nghị định 53/2022/NĐ-CP (data localisation):** N/A — this
    rule covers segregation across tenants, not storage location. Both layers run inside
    `kite-postgres` in `ap-southeast-1` per `documents/02-architecture/deployment-strategy.md`.
  - **Luật Bảo vệ Quyền lợi Người tiêu dùng 2023:** N/A — no direct consumer-rights
    trigger (no pricing/refund/marketing claim).

- **Review cadence:** **Annual + event-driven.**
  - **Next review:** 2027-05-11.
  - **Event triggers:**
    - Any new tenant-scoped table → update both V58/V34 migration pattern + sister migration
    - Any new Java entity extending `BaseEntity` → CI verifies the table appears in
      `tenant_tables` array of V58 (no migration drift)
    - PDPL implementing-decree amendment → re-verify §Compliance text
    - Cross-tenant incident → P0 retro per `documents/05-guides/operations/runbooks/rls-policy-violation.md`
    - kh-subscription gains per-request `TenantContext` → upgrade V34 to FORCE

- **Code references:**
  - `kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql`
  - `kitehub/kitehub-subscription/src/main/resources/db/migration/V34__enable_rls_tenant_scoped_tables.sql`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/datasource/TenantAwareDataSourceInterceptor.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/entity/BaseEntity.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/config/TenantFilterInterceptor.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/context/TenantContext.java`

- **A/B test:** N/A (security mandate, not a tunable parameter).

---

## Related

- Architecture: [`documents/02-architecture/kiteclass-architecture.md`](../../../02-architecture/kiteclass-architecture.md) §Multi-Tenant Isolation
- Runbook: [`documents/05-guides/operations/runbooks/rls-policy-violation.md`](../../../05-guides/operations/runbooks/rls-policy-violation.md)
- Gap: [GAP-466](../../../04-quality/gaps/GAP-466-multi-tenant-postgres-rls-defense-in-depth.md)
- Wave: [Wave 56](../../../03-planning/waves/wave-2026-05-11-56-rls-hardening.md)
- Rule governance: `.claude/rules/business-logic-review.md` §2 (5-attribute schema)

---

## Log

- **2026-05-11**: BR-MULTITENANT-001 created as Phase 3 of GAP-466 / Wave 56. First business rule codifying the dual-layer isolation guarantee that has existed at code level since Wave 18 but lacked an explicit reviewable business rule. Compliance check anchors PDPL 2023 Art 23 obligation; review cadence pegged to annual + event-driven with explicit triggers.
