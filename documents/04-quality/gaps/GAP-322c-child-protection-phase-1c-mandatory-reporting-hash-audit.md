# GAP-322c: Child Protection Phase 1C — Mandatory reporting Đ.51 + hash-chained audit + 7y retention + pen test

**Status:** 🟡 PARTIAL — Wave 19 Bucket A v1 SHIPPED 2026-05-05; Phase 1C remainder follow-up: [GAP-359](GAP-359-child-protection-phase-1c-remainder.md)
**Priority:** 🔴 P0 LEGAL (criminal liability — sister of GAP-322 Phase 1A + Phase 1B)
**Domain:** Backend + Frontend + Compliance + Security
**Detected:** 2026-05-04 (Wave 18b1 Bucket E closure)
**Affects:** P5 K-12 (Luật Trẻ em Đ.51 mandatory 24h reporting; PDPL Art 16 retention)

## Context

Phase 1A (Wave 18b1 PR #767) shipped Incident entity + AES-256 encryption + safeguarding role. Phase 1B (GAP-322b) adds vetting + MinIO + RBAC. This gap (1C) adds the **legal-mandate observability layer**: mandatory reporting banner Đ.51, non-repudiation audit log, 7-year retention enforcement, and security verification.

## Problem

- **Luật Trẻ em 2016 Đ.51:** schools failing to report suspected abuse to MOLISA Tổng đài 111 + công an địa phương ≤24h face criminal liability
- **PDPL Decree 13/2023 Art 16:** evidence retention for children's data special protection
- **Audit gap:** Phase 1A Incident has no admin-cannot-delete guarantee; admin can hide history
- **Security gap:** AES-256 implementation not yet pen-tested

## Proposed Fix

### 1C.1 — Mandatory reporting Đ.51 banner
- When incident status transitions to `severity=CRITICAL` AND `category IN (ABUSE, GROOMING, CSAM)`, system shows persistent banner
- Banner text: "⚠️ Luật Trẻ em 2016 Đ.51 — Trường có nghĩa vụ báo cáo nghi ngờ xâm hại trẻ em ≤24h cho Tổng đài 111 + công an địa phương"
- Banner stays until safeguarding officer acks "Đã báo cáo" with timestamp + reference number
- Audit entry on banner shown + acked

### 1C.2 — Hash-chained audit log
- New table `child_protection_audit_log` (entity_type, entity_id, action, actor_id, timestamp, prev_hash, content_hash, instance_id)
- Hash chain: `content_hash = SHA-256(prev_hash || action_payload)`
- Append-only — admin cannot delete (DB grant denies DELETE on this table for admin role)
- Daily hash-chain verification cron (alert if break detected)
- Retention 7 years (financial-record class per ND-13/2023)

### 1C.3 — 7-year retention enforcement
- Migration adds `retention_until` column to `incidents` table (computed: closed_at + 7 years; null while open)
- Soft-delete blocked when `retention_until > now()`
- Lifecycle job: after retention window, allow secure delete + audit log entry

### 1C.4 — Security pen test
- Encryption key rotation tested (re-encrypt with new key)
- Tampered ciphertext + IV detection (already in Phase 1A unit tests; this adds IT against deployed instance)
- DB direct query without decrypt key cannot read sensitive fields
- Penetration test report committed to `documents/04-quality/audits/security/childprotection-2026-Q3.md`
- Findings tracked as gaps if any

### 1C.5 — Complaint escalation 4-level (AC-COMM-006)
- Implement complaint routing: PH → GVCN → BGH → Phòng GD
- Dependency on GAP-339 (complaint workflow); coordinate scope

## Acceptance Criteria

### Phase 1C v1 (SHIPPED Wave 19 Bucket A 2026-05-05)

- [x] Mandatory reporting banner triggers on CRITICAL+abuse-category transition (`IncidentBanner.tsx` warning state)
- [x] Banner ack workflow with reference number + timestamp + audit log (`POST /api/v1/incidents/{id}/mandatory-report-ack`; `IncidentReportingController` + `MandatoryReportAckRequest/Response`)
- [x] `child_protection_audit_log` table with hash chain + DELETE denied for app role (V54 migration `REVOKE DELETE`)
- [x] Tests: banner trigger event-fire unit (`IncidentServiceTest`) + audit-log hash-chain unit (`ChildProtectionAuditServiceImplTest`) + endpoint IT (`IncidentReportingControllerIT`)
- [x] Business docs updated: BR-CHILD-PROTECT-005..007 (visibility scope + Đ.51 trigger + hash-chain) + UC-INCIDENT-CRITICAL-REPORT, full 5-attribute frontmatter
- [x] `IncidentVisibilityScope` enum (4 values, default STAFF_ONLY backward compat) — consumed by Wave 19 Bucket D parent-portal conduct facet
- [x] mvn green on `kiteclass-core` (verified salvage-agent run 2026-05-05)

### Phase 1C remainder (deferred — see [GAP-359](GAP-359-child-protection-phase-1c-remainder.md))

- [ ] Daily hash-chain integrity verification cron + alert
- [ ] 7-year retention column + soft-delete block until expiry
- [ ] Pen test report shipped (or feature-flag if pen test unavailable)
- [ ] AC-COMM-006 4-level complaint routing (or coordinate with GAP-339)
- [ ] Full UC-INCIDENT-CRITICAL-REPORT page UI (v1 ships banner + dialog handoff)
- [ ] MOLISA Tổng đài 111 webhook (Stage 2, Q4 2026)

## Estimated Effort

~2-3 weeks:
- 322c.1: Mandatory reporting banner (~3 days)
- 322c.2: Hash-chained audit log (~5 days)
- 322c.3: 7-year retention (~2 days)
- 322c.4: Pen test + remediation (~5 days, may extend if findings)
- 322c.5: 4-level escalation routing (~3 days, coordinate GAP-339)

## Related

- **Sister of:** GAP-322 Phase 1A (PR #767) + GAP-322b Phase 1B
- **Depends on:** GAP-339 (complaint workflow for 4-level escalation)
- **Cross-cuts:** GAP-321c (audit log pattern), GAP-184 (retention 7y compliance)
- **Stage 2 (Q4 2026):** Tổng đài 111 webhook + công an coordinator (separate scope)

## Log

- **2026-05-05** — Phase 1C v1 SHIPPED Wave 19 Bucket A via salvage agent (PC-restart recovery). Salvaged uncommitted worktree (5 modified + 13 new files). Verified via `mvn clean verify -pl kiteclass-core -Dcheckstyle.skip=true` on salvage-agent local environment. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp. Phase 1C remainder follow-up filed as **GAP-359** covering: 7y retention column + soft-delete block, pen test execution + remediation, AC-COMM-006 4-level escalation depending GAP-339, full UC-INCIDENT-CRITICAL-REPORT page UI, daily hash-chain integrity cron, MOLISA 111 webhook Stage 2 Q4 2026. Verification artifact: mvn output (last commit), `child_protection_audit_log` schema verified V54 migration; backward-compat `incidents.visibility_scope DEFAULT 'STAFF_ONLY'` covers existing rows. K12_ENTERPRISE tier flag REMAINS DISABLED until GAP-359 closes + legal counsel sign-off via GAP-156.
- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Per `gap-done-discipline.md` §3 PARTIAL exit ramp. K12_ENTERPRISE tier flag REMAINS DISABLED until 322b + 322c ship + legal counsel sign-off via GAP-156.
