# School MIS Integration — Business Rules

**Domain:** KiteClass Core / Integration / MIS Roster Import
**Version:** 1.0 (Phase 1 — GAP-200)
**Updated:** 2026-04-21
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/integration/mis/`
**ADR:** ADR-017

---

## 1. Scope

Phase 1 (this wave) ships the **interface** + pilot adapter skeleton for
importing a roster (students, parents, teachers, classes, enrollments) from an
external school MIS. No live API calls yet — structure only.

Phase 2 (deferred, see ADR-017) will wire live VNEDU API, add SMAS, Base.vn,
OneRoster CSV adapters, a wizard step in onboarding, an orchestration service,
and a conflict-resolution UI.

Out-of-scope for this domain:
- Grades/attendance sync (separate domain, future wave)
- Outbound push to MIS (KiteClass-wins writeback)
- Real-time webhooks (MIS providers do not support)

---

## 2. Provider Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MIS-001 | Supported providers enumerated | `MisProvider` enum values: `VNEDU`, `SMAS`, `BASE_VN`, `MS_SDS`, `GOOGLE_CLASSROOM`, `ONEROSTER_CSV`. Adding a provider = new enum value + new `MisRosterSource` adapter + ADR addendum. |
| BR-MIS-002 | Pilot provider = VNEDU | Phase 1 ships `VneduAdapter` skeleton only. Other adapters stay `@ConditionalOnProperty` gated. |
| BR-MIS-003 | Adapter-per-provider | Each provider has its own `MisRosterSource` implementation. No shared subclass hierarchy — prefer composition + shared helper utilities. Enforces Adapter pattern (`.claude/rules/design-patterns.md` §2). |
| BR-MIS-004 | Vendor types never leak | Adapters translate vendor-specific payloads to neutral `RosterImport` DTO before returning. Core code must not `import` vendor SDK classes. Violates leaky-abstraction rule (§3.10) if breached. |
| BR-MIS-005 | Feature flag default off | `kiteclass.mis.enabled` (env: `MIS_ENABLED`) defaults `false`. When false, all MIS endpoints return `503 MIS_DISABLED`. Phase 2 flips true per tenant once partnership MoU signed. |

---

## 3. Import Lifecycle Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MIS-IMPORT-001 | One-shot import at onboarding | Default import mode is **one-shot** triggered during tenant provisioning wizard. No scheduled polling in Phase 1. |
| BR-MIS-IMPORT-002 | Import status state machine | States: `PENDING` → `RUNNING` → (`COMPLETED` \| `FAILED` \| `PARTIAL`). Managed via `MisImportStatusService` (Phase 2). No direct status setter. |
| BR-MIS-IMPORT-003 | Idempotent re-import | Running import twice with same `providerStudentId` updates (upserts) existing row, does not create duplicate. Uses natural key `(tenant_id, provider, provider_student_id)`. |
| BR-MIS-IMPORT-004 | Max import size per batch | `kiteclass.mis.max-records-per-import` default 10,000 rows. Imports exceeding this split into multiple async jobs. |
| BR-MIS-IMPORT-005 | Cooldown between re-imports | `kiteclass.mis.reimport-cooldown-minutes` default 60. Attempt before cooldown elapses returns `429 MIS_REIMPORT_COOLDOWN`. |
| BR-MIS-IMPORT-006 | Dry-run supported | `POST /api/v1/mis/import?dryRun=true` returns preview (counts + first 10 records per entity type) without persisting. Required for tenant admin to review before commit. |
| BR-MIS-IMPORT-007 | Audit log per import | Every import persists an `MisImportJob` row with started_at, ended_at, provider, initiated_by, record counts, conflicts resolved. 12-month retention (PDPL §7). |

---

## 4. Conflict Resolution Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MIS-CONFLICT-001 | Default strategy `MIS_WINS` | When re-import finds record already in KiteClass with different values, MIS values overwrite. Reflects school staff assumption "MIS is master". |
| BR-MIS-CONFLICT-002 | Strategy configurable per tenant | `kiteclass.mis.conflict-strategy` enum: `MIS_WINS` \| `KITECLASS_WINS` \| `MANUAL_REVIEW`. Stored in `tenant_settings`. |
| BR-MIS-CONFLICT-003 | `MANUAL_REVIEW` creates queue rows | Conflicting records become `MisConflict` entity rows with status `PENDING`, resolved by admin via Phase 2 UI. Import completes with status `PARTIAL`. |
| BR-MIS-CONFLICT-004 | Soft-deleted KiteClass records stay deleted | Re-importing a student that was soft-deleted in KiteClass does NOT resurrect them. Admin must explicitly undelete first. |
| BR-MIS-CONFLICT-005 | Email/phone uniqueness wins over MIS | If MIS provides an email already used by another KiteClass user in the same tenant, record is rejected with reason `DUPLICATE_EMAIL`. Added to import report for manual fix. |

---

## 5. Authentication & Security Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MIS-SEC-001 | Per-tenant credentials | MIS API credentials stored per tenant in `mis_credentials` table, encrypted at rest with tenant-level KEK. Never global. |
| BR-MIS-SEC-002 | Credential rotation mandatory | Partner API keys rotated every 90 days. Scheduled warning 14 days before expiry. |
| BR-MIS-SEC-003 | Outbound calls via Circuit Breaker | Adapter methods wrapped with `@CircuitBreaker(name="mis-{provider}")` per `.claude/rules/design-patterns.md` §3.6. |
| BR-MIS-SEC-004 | No PII in logs | Adapter logging redacts student names, emails, phones. Use provider IDs only. |
| BR-MIS-SEC-005 | TLS 1.2+ required | All adapters reject non-TLS endpoints. Self-signed certs disallowed in production. |

---

## 6. PDPL / Data-Residency Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-MIS-PDPL-001 | DPA required before enable | Tenant must sign Data Processing Agreement before `mis.enabled` flips true. Stored in `tenant_legal_agreements`. |
| BR-MIS-PDPL-002 | Purpose limitation — roster only | Imported data used ONLY for roster materialization. No analytics, no resale, no cross-tenant aggregation. |
| BR-MIS-PDPL-003 | Retention matches source | KiteClass retains imported roster as long as the tenant retains data in source MIS. Tenant admin can trigger roster purge independently. |
| BR-MIS-PDPL-004 | Breach notification 72h | Per PDPL, breach in roster data must be notified to tenant + MPS within 72h. Runbook in `documents/05-guides/incidents/mis-breach.md` (Phase 2). |
| BR-MIS-PDPL-005 | Audit log 12 months | All import jobs + conflict resolutions retained 12 months minimum. |

---

## 7. Config Keys

All keys live in `application.yml` under `kiteclass.mis.*`. Phase 1 defines
the surface; implementation arrives in Phase 2.

| Key | Default | Purpose |
|-----|:-------:|---------|
| `kiteclass.mis.enabled` | `false` | Master feature flag |
| `kiteclass.mis.max-records-per-import` | `10000` | Batch size limit |
| `kiteclass.mis.reimport-cooldown-minutes` | `60` | Throttle re-imports |
| `kiteclass.mis.conflict-strategy` | `MIS_WINS` | Default conflict resolution |
| `kiteclass.mis.vnedu.base-url` | `https://api.vnedu.vn/v1` (placeholder) | VNEDU endpoint |
| `kiteclass.mis.vnedu.timeout-seconds` | `30` | HTTP timeout |
| `kiteclass.mis.dry-run-preview-limit` | `10` | Records returned in dry-run |

---

## 8. References

- Use cases: `use-cases.md`
- API contract: `api-contract.md`
- Catalog: `documents/02-architecture/integrations/school-mis-catalog.md`
- ADR: `documents/02-architecture/adr/ADR-017-mis-sync-strategy.md`
- Gap: `documents/04-quality/gaps/GAP-200-school-mis-integration.md`
- Design patterns: `.claude/rules/design-patterns.md` §2

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — PDPL 2023 cross-system data transfer; integration with school MIS systems may carry student PII.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: MIS partner addition, MoET integration spec update.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
