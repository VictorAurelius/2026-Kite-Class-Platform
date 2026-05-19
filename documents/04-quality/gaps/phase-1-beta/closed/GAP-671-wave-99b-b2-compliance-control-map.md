# GAP-671: Wave 99B B2 — Compliance × Code Map + SLO Registry + NFR + Risk Register

**Status:** 🟢 DONE 2026-05-19 — `documents/02-architecture/compliance-control-map.md` created với 4 sections per arc42 §10 + AWS WA + outside-in Failure-Mode agent Top 2 finding; 19 compliance rows mapped (PDPL Art 7 + Luật ANM 3 + ISO27001 5 + Payment 4 N/A); 11-service SLO registry; 35+ NFR rows consolidated; 5-row Risk Register top entries; Tech Lead Persona 4 self-test PASS (5 applicable rules surfaced ≤5 min for sample billing PR)
**Priority:** 🔴 P0
**Domain:** Architecture / Documentation / Compliance
**Phase:** phase-1-beta
**Wave:** 99b
**Created:** 2026-05-19
**Closed:** 2026-05-19
**Sister gaps:** GAP-668 (B6 foundation — archive sweep), GAP-669 (B0 frontmatter + Mermaid audit), GAP-670 (B1 service catalog + auth flow), GAP-672 (B3 database map), GAP-673 (B4 C4 diagram), GAP-674 (B5 README rewrite)

---

## Problem

Wave 99B Bucket B2 plan (`documents/03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md` §3 B2) mandate: create NEW file `documents/02-architecture/compliance-control-map.md` per arc42 §10 + AWS Well-Architected + outside-in Failure-Mode agent Top 2 finding.

Pre-Wave-99B state: compliance enforcement scattered across multiple files (BRD `compliance-scope.md` skeleton — TODO-heavy; per-domain `rules.md` — no cross-cut view; `output-review-mandate.md` §3 matrix — rolls up after-fact audit results, no pre-PR lookup); SLO numbers scattered across `nfr-catalog.md` (skeleton) + scattered audit reports + ADR-028; Risk Register exists implicitly trong 3 threat-models + ops audit P0 carry-forward but no single rollup page.

**Impact on personas:**
- **Tech Lead Persona 4 (review billing PR):** without compliance map, must check 5+ files (`compliance-scope.md` + per-domain `rules.md` + audit reports + threat-models + NFR catalog) to verify PR satisfies regulatory obligation → ≥30 min friction per review → likely skip checks → compliance drift risk.
- **SRE Persona 3 (verify SLO):** without SLO registry, scattered audit reports inconsistent (Wave 84 baseline 78/100 vs Wave 92 baseline 75/100 — different scope, different metric); no per-service rollup → on-call response slower.
- **Auditor (quarterly):** GAP-156 quarterly audit refresh requires consolidated risk register; missing risk register = audit produces stale numbers.

## Root Cause

Pre-Wave-99B arch folder evolved organically. Compliance + SLO + Risk artifacts dispersed across:
- BRD docs (`00-brd/compliance-scope.md`, `nfr-catalog.md`) — high-level scope, skeleton TODO
- Per-domain rules.md (`01-business/*/rules.md`) — domain-specific, no cross-cut
- Audit reports (`04-quality/audits/`) — point-in-time, no rollup
- Threat models (`02-architecture/threat-models/`) — per-feature, no aggregate risk register
- ADR rationale (`02-architecture/adr/`) — risk acceptance per decision, no cross-decision summary

Wave 99B outside-in audit Failure-Mode agent Top 2 finding: "Tech Lead Persona 4 self-test 30 min → 5 min target requires single-page Compliance × Code map".

## Proposed Fix

Create `documents/02-architecture/compliance-control-map.md` với 4 sections per task spec:

### Section 1 — Compliance Control Table
Map regulatory article → obligation → enforcing code path → test evidence:
- PDPL 2023 Art 8/9/11/14/15/20/23 (7 rows)
- Luật An ninh mạng 24/2018/QH14 + NĐ-53 (3 rows)
- ISO 27001 baseline informal (5 rows: A.9 access + A.10 crypto + A.12 ops + A.14 SDLC + A.16 incident)
- Payment compliance Phase 1.5+ (4 N/A rows: PCI DSS + VN e-invoice TCT + AML/KYC)

### Section 2 — SLO Registry
Per-service uptime + P99 latency + error rate consolidated:
- 11 services (10 active + 1 library N/A)
- 5 platform-wide composite SLO (API availability + signup funnel + AI SLA + email delivery + RLS cross-tenant zero-leak)
- TBD count documented (measurement instrumentation deferred GAP-135 Phase 2 RUM trigger)

### Section 3 — NFR + Quality Attribute Registry per arc42 §10
Consolidated từ `nfr-catalog.md` skeleton:
- Performance (11 rows: API latency + bundle + Web Vitals + DB)
- Availability + Reliability (10 rows: uptime per tier + RTO/RPO per scenario)
- Scalability (5 rows: per-tenant + platform-total)
- Security (8 rows: pen-test + CVE SLA + lockout + encryption + secrets rotation)
- Accessibility (5 rows: WCAG AA)
- Maintainability (5 rows: coverage + CI duration + PR cycle)
- Observability (6 rows: structured logs + tracing + metrics + alert MTTR)

### Section 4 — Risk Register
Top 5 known risks Phase 1 BETA scope:
- R1: Cross-tenant data leak via RLS misconfiguration ✅ MITIGATED Wave 85
- R2: Auth bypass via JWT forgery / replay ⚠️ PARTIAL (KMS migration P1 carry)
- R3: Bulk CSV import malicious payload ⚠️ PARTIAL (ClamAV planned Phase 2)
- R4: Production restore drill never executed ❌ BLOCKED GAP-257 + GAP-612 AWS
- R5: Phase 1.5+ scale jump no proven scale path ⚠️ ACCEPTED RISK per ADR-028

## Acceptance Criteria

- [x] NEW file `documents/02-architecture/compliance-control-map.md` exists với frontmatter (audience: dev + last-reviewed: 2026-05-19 + status: living)
- [x] Section 1 Compliance Control Table: 19 rows mapped với regulatory article → obligation → enforcing code path → test evidence → status
- [x] Section 2 SLO Registry: 11-service per-service table + 5 platform-wide composite SLO; TBD count documented
- [x] Section 3 NFR Registry: 35+ rows consolidated từ `nfr-catalog.md` skeleton + audit baselines
- [x] Section 4 Risk Register: Top 5 risks với likelihood + impact + mitigation status + owner + link to threat models
- [x] Cross-references: ADR-013 retention via `data-retention-policy.md` + ADR-025 region pin + ADR-028 ECS Fargate + GAP-156 quarterly audit + 3 threat-models + `nfr-catalog.md` + `compliance-scope.md`
- [x] Tech Lead Persona 4 self-test PASS: sample billing PR ("POST /api/subscription/upgrade") — 5 applicable rules surfaced trong ≤5 min via single-page lookup
- [x] Section §6 Open follow-ups: 8 next-refresh triggers documented (counsel sign-off Phase 3, RUM baseline, ECS migration plan, etc.)
- [x] Section §7 Maintenance: quarterly refresh cadence per `post-wave-audit-mandate.md` §2.4 meta-governance domain

## Verification

- [x] File created: `documents/02-architecture/compliance-control-map.md` (~330 lines)
- [x] Counts verified:
  - Compliance table: **19 rows** (PDPL 7 + Luật ANM 3 + ISO27001 5 + Payment 4)
  - TBD count compliance: **0** (all rows have explicit Status verdict)
  - SLO registry per-service: **11 services**
  - SLO TBD count: **17** (P99 latency + cache hit ratio measurement deferred)
  - NFR rows: **35+ across 7 categories**
  - Risk Register top entries: **5**
- [x] Cross-link sanity: verified `nfr-catalog.md` + `compliance-scope.md` + 3 threat-model file paths resolve; ADR-013/025/028 IDs match `documents/02-architecture/adr/` actual files; GAP-156/257/612/637 references match `gap-status.csv` rows
- [x] Tech Lead self-test (§5): sample PR walkthrough surfaces 5 rules trong ≤5 min target

## Log

- **2026-05-19** — Wave 99B B2 SHIPPED. File `compliance-control-map.md` created với 4 sections + Tech Lead self-test PASS. Tier1 audit IDs N/A (compliance map is living doc, not date-stamped audit artifact). Sister gaps GAP-668 + GAP-669 already DONE; B3/B4/B5 follow Wave 2-3 spawn order per plan §3.
