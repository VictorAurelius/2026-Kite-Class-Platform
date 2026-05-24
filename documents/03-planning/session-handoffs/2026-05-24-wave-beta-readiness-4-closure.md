---
title: Session handoff — Wave beta-readiness-4 closure
date: 2026-05-24
session_type: wave-closure
wave: beta-readiness-4
audience: dev
---

# Session Handoff — Wave beta-readiness-4 Closure (2026-05-24)

## Wave SHIPPED

5/5 buckets + 3 hotfixes + 1 new META rule = **11 PRs merged**.

| # | PR | Commit | Scope |
|:-:|---|---|---|
| 0 | #1778 | `e9e48c48` | Wave plan (5 buckets parallel design) |
| A | #1779 | `8b0a8d68` | META env-coverage RESEND IaC + CI gate Phase 3 (PARTIAL 90% — live verify gated GAP-612 AWS suspended) |
| A-sync | #1780 | `dca4ea0e` | GAP-508 post-merge Log sync |
| B | #1782 | `5378fca3` | PDPL consent immutable schema + hash chain SHA-256 + analytics SDK lifecycle (PARTIAL — counsel review deferred Phase 2) |
| C | #1783 | `5937ee71` | Pricing PER_HOUR + GAP-292b paired payment recording (DONE) |
| C-hotfix1 | #1784 | `5e3ceebe` | Course entity pricingModel + unitPrice fields |
| D | #1781 | `883f43b8` | Reschedule + email fallback + reason MANDATORY (DONE) |
| E | #1785 | `a60c8f19` | Email tone matrix Thymeleaf + VN sample fixture audit (DONE) |
| META | #1786 | `1be75b6a` | New rule fix-up-ci-selective-rerun v1.0.0 (cancel unrelated CI re-runs) |
| C-hotfix2 | #1787 | `9ce75c17` | ClassMapper @Mapping ignore for 6 reschedule audit cols |
| C-hotfix3 | #1788 | `36c71948` | strict-warnings cleanup (EntityNotFoundException deprecated + unused var) |

## Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| A | GAP-508 META env-coverage RESEND IaC + Phase 3 CI gate | 🟡 PARTIAL 90% | GAP-NEW-resend-live-verify-post-restore (gated GAP-612) |
| B | GAP-353b PDPL consent API + hash chain + analytics handler | 🟡 PARTIAL 85% | GAP-NEW-counsel-pdpl-pre-launch-review (Phase 2 trigger) |
| C | GAP-292 Pricing + GAP-292b paired payment recording | ✅ DONE | GAP-NEW-pricing-data-reclassification (P1 87 existing courses) |
| D | GAP-291 Reschedule + email fallback | ✅ DONE | GAP-NEW-zalo-oa-notification-integration (P2 Wave br-5+) |
| E | META email tone matrix + VN sample audit | ✅ DONE (GAP-736) | — |
| 4 ADRs | ADR-027→ADR-035 (re-numbered chaos resolved) | ✅ ALL SHIPPED | — |

## 3 outside-in audits convergence

3 agents 2026-05-24 (persona simulation + VN edu SaaS benchmark + failure-mode matrix) surfaced 7 P0 + 10 P1 cross-bucket cells. User 3-Q decisions:

1. **PDPL deadline:** Giữ CLAUDE.md 2026-07-01 (không panic mode dù Agent 2 claim Luật 91/2025 effective 2026-01-01)
2. **Pricing taxonomy:** Re-scope `PER_HOUR + MONTHLY + COURSE_PACKAGE + FREE` (Apollo 257-344k/giờ + ILA 195-368k/giờ VN benchmark)
3. **GAP-292b paired:** Bucket C ghép PaymentMethod enum + record-payment endpoint (persona Vy P0-1)

## Meta lessons (force-multiplier insights)

1. **Sonnet 200k autocompact thrash** — Bucket B/C/D first attempt all failed. Opus 1M retry success. Going forward: default Opus cho implementation agents trong repo này (rule auto-load + multi-file reads ~50-100k overhead).
2. **Cross-contamination từ thrashed agent** — Sonnet Agent C earlier wrote files into main checkout before thrash → polluted subsequent agents. Hardened cleanup procedures needed.
3. **ADR numbering chaos** — wave plan specified ADR-027/028/029/030 but reality conflicted (ADR-027 = statuspage-vendor). Lock ADR numbers AT PLAN time via adrs-index.csv reservation.
4. **Migration version reservation** — V67/V67b/V68 reservation locked §3.6 worked cleanly. Apply same pattern for future cross-bucket waves.
5. **New META rule `fix-up-ci-selective-rerun.md`** v1.0.0 — eliminates wait-time waste on fix-up commits. Force-multiplier per `meta-gap-priority.md` §3.
6. **AUDIT_OVERRIDE pattern proven** — GAP-735 deterministic flake admin-merge precedent worked across 4 PRs (B + D + #1784 + #1787 + #1788).
7. **Hotfix iteration count** — Bucket C agent shipped PricingCalculator + tests but FORGOT Course entity fields. Hotfix #1784 caught via IDE diagnostics post-merge. META candidate: `GAP-NEW-classmapper-meta-entity-vs-migration-consistency` mandate per-agent post-implementation entity-vs-migration consistency check.

## 8 follow-up gaps tracked (files pending — track in ROADMAP narrative)

| Gap | Priority | Scope | Trigger |
|---|:-:|---|---|
| GAP-NEW-resend-live-verify-post-restore | P1 | Live verify Resend + DKIM/SPF/DMARC + 5-VN-ISP smoke | GAP-612 AWS restore |
| GAP-NEW-env-coverage-hard-stop-escalation | P2 | WARN → HARD STOP after 30-day stabilization | Post-stabilization |
| GAP-NEW-pricing-data-reclassification | P1 | Per-tenant UI + email Phase 1 BETA Owners | Bucket C post-merge |
| GAP-NEW-zalo-oa-notification-integration | P2 | GAP-063 Phase 2 Zalo OA consumer | Wave br-5+ |
| GAP-NEW-vat-einvoice-misa-integration | P2 | MISA MeInvoice partnership (Thông tư 78) | Wave br-5+ |
| GAP-NEW-counsel-pdpl-pre-launch-review | P1 | Legal counsel formal review | Phase 2 trigger |
| GAP-NEW-resend-vs-ses-phase-2-eval | P2 | AWS SES migration cost + VN region | Phase 2 optimization |
| GAP-NEW-classmapper-meta-entity-vs-migration-consistency | P2 META | Per-agent entity-vs-migration check | Future agent spawns |

## Post-wave audit suite scheduling (per `post-wave-audit-mandate.md` §2.2)

3-day deadline: **2026-05-27**. 4-suite cadence trigger:

- **Security audit** /100 (per `audit-skill-rubric-security-audit.md`) — Bucket B PDPL consent hash chain + audit log immutability + Bucket A IAM wildcard + secrets management
- **Business Logic audit** /100 — Bucket C BR-COURSE-PRICING-001..004 + BR-PAYMENT-METHOD-001..002 + Bucket D Cal.com pattern decision + ADR-027/030/034/035
- **API Contract audit** /100 — Bucket B `/api/v1/consent/v2/*` + Bucket C `/api/v1/invoices/{id}/record-payment` + Bucket D `/api/v1/classes/{id}/reschedule`
- **Ops Readiness audit** /100 — Bucket A env-coverage CI gate WARN-mode + Bucket E VN sample audit WARN-mode + Bucket D Outbox no-op consumer feature flag + post-merge hotfix iteration count

Suite reports → `documents/04-quality/audits/{category}/2026-05-XX-wave-br-4-post-audit.md`.

## Next session pickup

1. Run post-wave audit suite (4 categories) — 4 agents parallel acceptable
2. GAP-735 dedicated test isolation wave (remove AUDIT_OVERRIDE trailer dependency for future code PRs)
3. GAP-612 AWS restoration tracker — unblock live verify cluster (Bucket A live verify + Bucket B counsel review path + Bucket E VN ISP smoke)
4. Wave beta-readiness-5 candidates: GAP-732 + GAP-734 + GAP-NEW-pricing-data-reclassification UI + remaining Phase 1 BETA P0 backlog

## CI metrics (Wave br-4)

- **Total CI minutes saved by new rule:** ~20-30 min (5 unrelated workflows canceled across Bucket E fix-up #1785 + new rule fix-up #1786 + hotfix #1788 first push)
- **Hotfix iteration count:** 3 (Course entity + ClassMapper + strict-warnings) — meta candidate for entity-vs-migration consistency check
- **Sonnet thrash recurrence:** 3/3 buckets (B/C/D) first attempt — Opus 1M retry success on all
- **AUDIT_OVERRIDE usage:** 4 PRs (B/C/D code + Course hotfix + ClassMapper + strict-warnings) — all GAP-735 precedent

---

## Next 5 Waves Queue (drafted for next session pickup)

Per user direction 2026-05-25 — drafted theo thứ tự priority cho session sau spawn agents.

### Wave 1/5 — `wave-audit-1-post-wave-br-4-suite` (MANDATORY, T+0)

**Trigger:** `post-wave-audit-mandate.md` §2.2 — Wave br-4 shipped 5 buckets touching: terraform IaC + Java BE + JPA migrations + email templates + business docs + ADRs → 4 audit categories trigger.

**Scope:** 4 audits parallel (4 agents Sonnet OK — read-only audit not impl):
1. **Security audit** /100 — Bucket B PDPL consent hash chain + audit log immutability + Bucket A IAM wildcard pattern + secrets management baseline
2. **Business Logic audit** /100 — Bucket C BR-COURSE-PRICING-001..004 + BR-PAYMENT-METHOD-001..002 5-attribute check + Bucket D Cal.com pattern + ADRs (027/030/034/035)
3. **API Contract audit** /100 — Bucket B `/api/v1/consent/v2/*` + Bucket C `/api/v1/invoices/{id}/record-payment` + Bucket D `/api/v1/classes/{id}/reschedule` schema check
4. **Ops Readiness audit** /100 — Bucket A env-coverage CI WARN-mode + Bucket E VN sample audit WARN-mode + Bucket D Outbox no-op feature flag + post-merge hotfix iteration count (3 hotfixes = quality signal)

**Output:** 4 audit reports trong `documents/04-quality/audits/{security,business-logic,api-contract,ops-readiness}/2026-05-XX-wave-br-4-post-audit.md`; append `audits-index.csv`; file follow-up gaps cho findings.

**Deadline:** 2026-05-27 (3-day cadence từ Wave br-4 last merge 2026-05-24).

**Effort:** ~2-3h với 4 agents parallel (audit-only, không code change).

---

### Wave 2/5 — `wave-meta-1-test-isolation-gap-735` (META P1 force-multiplier)

**Trigger:** GAP-735 deterministic Testcontainer pollution — currently mọi kiteclass-core code PR cần AUDIT_OVERRIDE trailer + admin merge. Removing this blocker = force-multiplier mọi future code PR.

**Scope (3-bucket parallel):**
- **Bucket A:** `@Transactional@Rollback` annotation cho IT classes share Testcontainer DB (EnrollmentIntegrationTest + InvoiceFlowIntegrationTest + CourseSecurityTest)
- **Bucket B:** Test fixture isolation — per-test tenant context cleanup + DB reset hook
- **Bucket C:** CI workflow update — remove GAP-735 AUDIT_OVERRIDE precedent từ admin-merge-discipline.md exception list

**AC:**
- 6 deterministic test failures (CourseSecurityTest 4× + EnrollmentIT + InvoiceFlowIT) → ALL PASS in full suite
- `./mvnw verify -P strict-warnings` BUILD SUCCESS clean (no flakes)
- Future code PRs merge non-admin (no AUDIT_OVERRIDE trailer needed)
- Remove `Test KiteHub Core Service` từ GAP-735 documented flake list
- Flip GAP-735 OPEN → DONE 100%

**Effort:** ~4-6h Opus 1M (per Sonnet 200k thrash pattern Wave br-4 confirmed).

---

### Wave 3/5 — `wave-beta-readiness-5-beta-signup-unblock` (highest leverage Phase 1 BETA gate)

**Trigger:** Phase 1 BETA exit gate requires 5 beta tenants live. Currently 4 P0 gaps blocking beta signup flow end-to-end → 0 tenants can onboard.

**Scope (4-bucket parallel):**
- **Bucket A: GAP-606** Email template `admin-new-login-alert.html` MISSING kitehub-email → HTTP 500 consumer infinite retry. Add template + smoke test.
- **Bucket B: GAP-608** EC2 IAM role `kitehub-production-ec2-app` thiếu `ses:SendEmail` permission. Terraform IAM update + paired live verify (post-GAP-612 unblock).
- **Bucket C: GAP-610** GET `/api/v1/beta-signup/validate/{token}` returns `TOKEN_NOT_FOUND` cho valid token (RLS suspect). Debug + fix RLS policy.
- **Bucket D: GAP-611** POST `/api/v1/beta-signup` route 404 gateway hoặc security shadow. Gateway route audit + SecurityConfig matcher fix.

**AC:**
- End-to-end beta signup flow: anonymous lands `/request-beta` → submits form → email confirmation arrives → click verify link → 200 OK + tenant provisioned
- 5 beta tenants onboard within 7 days post-merge
- 0 P0 incidents related to signup trong 14 ngày

**Effort:** ~6-8h (4 agents parallel Opus 1M).

**Dependencies:** Bucket B requires GAP-612 AWS restore cho live verify (code path can ship parallel — Bucket A pattern).

---

### Wave 4/5 — `wave-beta-readiness-6-api-contract-drift-trio` (clean disjoint)

**Trigger:** 3 API contract drift gaps blocking consumer FE/external integration trust.

**Scope (3-bucket parallel — clean disjoint Backend):**
- **Bucket A: GAP-231** payment-invoice domain — verify Java `@RequestMapping` URLs match `api-contract.md` Endpoint declarations + DTO schemas match
- **Bucket B: GAP-232** attendance domain — same drift check
- **Bucket C: GAP-233** student-enrollment domain — same drift check

**Pattern:** Use `scripts/check-cross-layer-contract-drift.sh` (existing CI tool per `audit-to-gap-pipeline.md` §2.5) trên 3 domains. Fix drift findings.

**AC:** All 3 domains pass cross-layer-contract-drift CI check; api-contract.md schemas authoritative; FE/external consumers verified.

**Effort:** ~5h (3 agents Sonnet OK — narrow domain scope per bucket).

---

### Wave 5/5 — `wave-beta-readiness-7-document-performance-cluster` (Ops + Performance mix)

**Trigger:** 4 P0 gaps blocking PDF/XLSX/DOCX generation reliability + ops observability.

**Scope (4-bucket parallel):**
- **Bucket A: GAP-215** `BrandingService.getBranding()` not `@Cacheable` → DB hit per document render. Add `@Cacheable` annotation + cache TTL config + invalidation hook.
- **Bucket B: GAP-216** PDF/XLSX/DOCX p95 micro-benchmark + SLO assertion. JMH micro-benchmark + Prometheus histogram + Grafana panel.
- **Bucket C: GAP-217** Alert rules cho `/api/v1/documents/*` endpoints. Prometheus AlertManager rules + escalation per `audit-skill-rubric-ops-readiness-audit.md`.
- **Bucket D: GAP-218** PDF font-missing runbook + container image-build validation. Runbook + Dockerfile assertion (font files exist).

**AC:** Branding cache hit rate >90% / p95 latency targets met / Alerts fire + page on-call / Font validation prevents image build mà thiếu fonts.

**Effort:** ~7h (4 agents — mixed Ops + Java + runbook).

---

## Wave queue summary table

| Order | Wave name | Scope | Effort | Dependency |
|:-:|---|---|:-:|---|
| 1 | `wave-audit-1-post-wave-br-4-suite` | 4-audit suite mandatory cadence | ~3h | None (run first) |
| 2 | `wave-meta-1-test-isolation-gap-735` | Remove AUDIT_OVERRIDE blocker | ~5h | Audit 1 complete (don't compete CI) |
| 3 | `wave-beta-readiness-5-beta-signup-unblock` | 4-gap beta signup E2E | ~7h | GAP-735 ideal (cleaner merges); GAP-612 partial (Bucket B live verify) |
| 4 | `wave-beta-readiness-6-api-contract-drift-trio` | 3-domain contract drift | ~5h | Independent |
| 5 | `wave-beta-readiness-7-document-performance-cluster` | 4-gap PDF/XLSX/cache/alert | ~7h | Independent |

**Total effort:** ~27h across 5 waves. With audit suite first, GAP-735 + GAP-612 unblock in parallel via user-action (AWS support escalation), then 3 product waves parallel-eligible.

**Phase 1 BETA gate ETA:** if all 5 waves ship + GAP-612 restore + 5 beta tenants live + 2 weeks no P0 incidents = Phase 1 → Phase 2 gate clear. Optimistic ~3-4 tuần.

## Next session pickup checklist

1. `/start-session` → load this handoff note
2. Run post-wave audit suite Wave 1/5 immediately (deadline 2026-05-27)
3. After audit suite findings → spawn Wave 2/5 (test isolation) per outside-in optional
4. Parallel: user-action escalate GAP-612 AWS Support / Billing console
5. Pick Wave 3/4/5 per session capacity + GAP-612 unblock state
