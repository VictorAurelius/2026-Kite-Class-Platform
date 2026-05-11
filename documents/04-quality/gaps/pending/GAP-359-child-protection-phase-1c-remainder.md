# GAP-359: Child Protection Phase 1C — Remainder (retention enforcement, pen test, escalation, full UI, hash-chain cron, 111 webhook)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 LEGAL (sister of GAP-322c Phase 1C v1)
**Domain:** Backend + Frontend + Compliance + Security + Ops
**Detected:** 2026-05-05 (Wave 19 Bucket A v1 closure)
**Affects:** P5 K-12 (Luật Trẻ em Đ.51 follow-through; PDPL Decree 13/2023 Art 16 retention; AC-COMM-006 four-level escalation)

## Context

Wave 19 Bucket A (PR coordinator-merged, salvaged from PC-restart agent kill) shipped Phase 1C v1 — Đ.51 mandatory-reporting banner foundation + ack endpoint + hash-chain audit log table + listener + V54 migration + `IncidentVisibilityScope` enum + minimal banner FE component. Per `gap-done-discipline.md` §3 PARTIAL exit ramp, GAP-322c stays 🟡 PARTIAL and the deferred remainder is captured here so it does not fall off the radar.

## Problem

GAP-322c v1 covered the **trigger + ledger + minimal banner** loop end-to-end. The following Phase 1C scope items remain to satisfy the full criminal-liability + compliance + UX surface:

1. **7-year retention enforcement** — V54 added `visibility_scope` + `child_protection_audit_log` but did NOT add `retention_until` to `incidents`; soft-delete is not yet blocked while window is open.
2. **Pen test execution** — AES-256 + audit chain remain pen-test pending. Phase 1A unit tests cover tampered ciphertext detection in-process; Phase 1C v1 did NOT run a deployed-instance pen test or commit a report.
3. **AC-COMM-006 four-level complaint escalation** — PH → GVCN → BGH → Phòng GD routing depends on GAP-339 (complaint workflow) and is intentionally out-of-scope for Phase 1C v1 to keep blast radius small.
4. **Full UC-INCIDENT-CRITICAL-REPORT page UI** — v1 ships a banner + ack button only. The full page (incident summary, evidence preview gated by RBAC, reference-number form with timestamp picker, prior-ack history, MOLISA 111 link button) is deferred.
5. **Daily hash-chain integrity verification cron** — v1 builds the chain; verification job (recompute genesis-to-tail, alert on break) is not yet wired. Without it, integrity is structural-only — a superuser bypass of the V54 DELETE grant would not surface until manual audit.
6. **MOLISA Tổng đài 111 webhook (Stage 2)** — outbound integration to the official reporting channel + công an coordinator hand-off remains a separate scope item, deferred to Q4 2026 stage per GAP-322c §Stage 2.

## Proposed Fix

### 359.1 — 7-year retention column + soft-delete block (~2 days)
- Migration `V<N+1>__add_incidents_retention_until.sql` adds `retention_until TIMESTAMPTZ NULL` (computed `closed_at + INTERVAL '7 years'`)
- `IncidentService.softDelete(...)` raises `RetentionWindowActive` when `retention_until > now()`
- Lifecycle scheduled job after window: secure-delete + audit-log entry `INCIDENT_RETENTION_EXPIRED_DELETE`
- Backfill: existing closed Incidents without `closed_at` get `retention_until = created_at + 7y` as fail-safe

### 359.2 — Pen test execution + remediation (~5 days, may extend)
- Encryption key rotation IT (re-encrypt with new key against deployed Postgres)
- Tampered ciphertext + IV detection IT against deployed instance (sister of Phase 1A unit test)
- DB direct query without decrypt key cannot read sensitive fields (psql session test)
- Penetration test report committed to `documents/04-quality/audits/security/childprotection-2026-Q3.md`
- Findings tracked as sub-gaps if any

### 359.3 — AC-COMM-006 four-level complaint escalation (~3 days, depends GAP-339)
- Routing pipeline PH → GVCN → BGH → Phòng GD with SLA per level
- Coordinate scope with GAP-339 complaint workflow

### 359.4 — Full UC-INCIDENT-CRITICAL-REPORT page (~3 days)
- Page at `(dashboard)/safeguarding/incidents/[incidentId]/critical-report/page.tsx`
- Sections: incident summary, RBAC-gated evidence preview, reference-number form with timestamp picker, prior-ack history, MOLISA 111 quick-link button
- Replaces the v1 minimal banner+button surface for safeguarding officers

### 359.5 — Daily hash-chain integrity verification cron (~2 days)
- Spring `@Scheduled` job recomputes genesis-to-tail per `(instance_id, entity_type)` chain
- Alerts on break detected (Micrometer counter `child_protection.audit.chain.break`)
- Manual repair runbook + audit-log entry on intentional reset

### 359.6 — MOLISA Tổng đài 111 webhook (Stage 2, deferred to Q4 2026)
- Outbound webhook delivery to MOLISA reporting channel
- Công an coordinator hand-off
- Tracked here for completeness; physical scope shipped via separate Stage 2 wave per GAP-322c §Stage 2

## Acceptance Criteria

- [x] Migration V57 adds `retention_until` column + soft-delete block IT (RetentionWindowActive on premature delete) — Wave 24 Bucket A
- [x] Lifecycle job for secure-delete after retention window + audit-log entry — `RetentionLifecycleService` daily cron 02:00, Wave 24 Bucket A
- [ ] Pen test report shipped at `documents/04-quality/audits/security/childprotection-2026-Q3.md` (sub-task 359.2 — deferred)
- [ ] AC-COMM-006 four-level routing implemented (or coordinated with GAP-339) (sub-task 359.3 — deferred)
- [ ] Full UC-INCIDENT-CRITICAL-REPORT page UI replaces v1 banner-only surface (sub-task 359.4 — deferred)
- [x] Daily hash-chain integrity cron + alert + Micrometer counter — `AuditChainVerificationCron` daily 02:30, Wave 24 Bucket A
- [ ] MOLISA 111 webhook tracked to Stage 2 (Q4 2026) (sub-task 359.6 — deferred)
- [x] Tests: retention-block IT + cron unit (3 new test files: `IncidentServiceTest` retention-block + retention-stamping nested classes; `RetentionLifecycleServiceImplTest`; `AuditChainVerificationCronTest`) — Wave 24 Bucket A. Pen-test IT remains under sub-task 359.2.
- [x] Business docs updated: BR-CHILD-PROTECT-008 (retention) + BR-CHILD-PROTECT-009 (chain-verification cron) — Wave 24 Bucket A. Future BR-CHILD-PROTECT-010..012 (escalation/Stage-2 webhook) remain under sub-tasks 359.3 + 359.6.
- [ ] mvn + pnpm green (Bucket A backend only — pnpm N/A; coordinator verifies post-merge)

## Estimated Effort

~2 weeks excluding Stage 2 webhook:
- 359.1 retention enforcement: ~2 days
- 359.2 pen test + remediation: ~5 days (may extend on findings)
- 359.3 4-level escalation: ~3 days (coordinate GAP-339)
- 359.4 full report page UI: ~3 days
- 359.5 hash-chain verification cron: ~2 days
- 359.6 webhook Stage 2: tracked separately Q4 2026

## Related

- **Parent (PARTIAL):** GAP-322c Phase 1C v1 — banner + audit-log + listener + V54 shipped Wave 19 Bucket A
- **Sister:** GAP-322 Phase 1A (PR #767) + GAP-322b Phase 1B
- **Depends on:** GAP-339 (complaint workflow for §359.3 four-level escalation)
- **Cross-cuts:** GAP-321c (audit log pattern), GAP-184 (retention 7y compliance)
- **Compliance:** Luật Trẻ em 2016 Đ.51 follow-through; PDPL Decree 13/2023 Art 16
- **Stage 2 (Q4 2026):** MOLISA Tổng đài 111 webhook + công an coordinator (separate scope)

## Log

- **2026-05-06** — Wave 24 Bucket A shipped sub-tasks **359.1** (retention 7-year + soft-delete block) and **359.5** (daily hash-chain integrity verification cron). Scope: V57 migration (`incidents.retention_until` + backfill `COALESCE(updated_at, created_at) + 7y` + partial index for cron scan); `Incident` entity + `IncidentService.updateStatus` retention-stamp on CLOSED transition (sticky deadline); `IncidentService.softDelete` raises `RetentionWindowActiveException` HTTP 409 while window active; `RetentionLifecycleService(Impl)` daily cron 02:00 → secure-delete + null-out sensitive fields + audit append `INCIDENT_RETENTION_EXPIRED_DELETE`; `ChildProtectionAuditService.verifyChain(UUID, String)` explicit-instance overload + `findDistinctChains()` repository method; `AuditChainVerificationCron` daily 02:30 → Micrometer counters `child_protection.audit.chain.break{instance,entityType}` + `child_protection.audit.chain.verified{instance,entityType,result}`. Business docs updated: BR-CHILD-PROTECT-008 + BR-CHILD-PROTECT-009 with full 5-attribute frontmatter; rules.md version 0.4 → 0.5. Operational runbook shipped at `documents/05-guides/operations/audit-chain-break-runbook.md`. Tests added: 4 new in `IncidentServiceTest` (retention block + sticky stamp) + `RetentionLifecycleServiceImplTest` (4 tests including per-row isolation) + `AuditChainVerificationCronTest` (4 tests including counter wiring). Status stays 🔵 OPEN — sub-tasks 359.2 (pen test), 359.3 (4-level escalation, depends GAP-339), 359.4 (full report page UI), 359.6 (Tổng đài 111 webhook Stage 2 — Q4 2026) remain. Coordinator updates status when remainder ships.
- **2026-05-05** — Filed by Wave 19 Bucket A v1 closure agent (salvage of PC-restart agent kill). Per `gap-done-discipline.md` §3 PARTIAL exit ramp + `audit-to-gap-pipeline.md` §3 — captures the deferred-remainder scope from GAP-322c so the v1 ship can flip Status to 🟡 PARTIAL without losing the follow-through obligation. K12_ENTERPRISE tier flag REMAINS DISABLED until 322b + 322c FULL (this gap closed) ship + legal counsel sign-off via GAP-156.
