---
title: Phase 1 BETA Closure Roadmap V2 Amendment — state-checked scope correction
date: 2026-05-24
phase: phase-1-beta
wave: beta-readiness-1..7 (planning)
supersedes: phase-1-beta-closure-roadmap-2026-05-24.md (V1)
audience: dev
audits: [persona-review]
gaps_referenced:
  - GAP-291 # TS-3 session reschedule (confirmed BLOCKING)
  - GAP-286 # Mobile OTP/Zalo signup
  - GAP-216 # PDF benchmark
  - GAP-217 # Alert rules
  - GAP-231 # API contract drift
  - GAP-232
  - GAP-233
---

# Phase 1 BETA Closure Roadmap V2 Amendment

**Mục đích:** Sync V1 roadmap với 3 audit V2 state-checked findings (per `audit-to-gap-pipeline.md` §2.8). V1 roadmap merged via PR #1758 với 17 + 38 + 8 findings từ V1 audit — V2 reveal ~30-60% V1 findings là false-positive (stale state, gap CSV drift vs code state).

V1 roadmap **CANCEL**; V2 amendment supersedes. Wave 108-N scope REVISED.

---

## §1 V2 audit summary

| Audit | V1 findings | V2 verified | V2 false-positive eliminated |
|---|---|---|---|
| Persona walkthrough | 15 NEW gap candidates + 5 FM | **8 actionable** (5 VERIFIED-MISSING + 3 PARTIAL) | ~60% (onboarding wizard / testimonials / USD pricing / Excel import — all already exist) |
| VN edu SaaS benchmark | 5 TS missing + 3 diff + 8 defer | **1 BLOCKING TS-3** + 2 PARTIAL (TS-1/TS-4) + 2 V1-WRONG (TS-2/TS-5 actually EXIST) | 40% (TS-2 onboarding ✅ EXIST 11 files; TS-5 parent dashboard ✅ EXIST 10 routes) |
| Failure-mode matrix | 17 items (5 P0-CRITICAL + 3 P1 + 8 AWS-gated + 2 vendor) | **2 V1-CORRECTIONS** (A1 PaymentController FIXED Wave 105 + B1 VND false-positive Lucide icon) + **2 SEVERITY ELEVATIONS** (A2 XSS 7 dangerouslySetInnerHTML; A4 enrollment NO capacity model entirely) | ~10% (most P0 valid but 2 SHIFTED scope) |

**Key meta-lesson:** Gap CSV status OPEN ≠ code state MISSING. Wave 92/98/102/105 shipped major implementations without closing gap CSV status. `audit-to-gap-pipeline.md` §2.8 fix-time state-check mandate is correct + critical.

---

## §2 Confirmed Phase 1 BETA P0 blockers (verified)

After V2 state-check on all 3 audits, **8 verified P0 blockers** for Phase 1 BETA gate:

| ID | Finding | Source | Code state | Wave beta-readiness-N target |
|---|---|---|---|---|
| 1 | **XSS 7 dangerouslySetInnerHTML kitehub-frontend without DOMPurify** (SVG template highest risk) | FM V2 A2 elevated | ✅ VERIFIED-MISSING sanitize | beta-readiness-1 |
| 2 | **Enrollment capacity model entirely absent** (no max_capacity/maxStudents/enrolledCount anywhere) | FM V2 A4 elevated | ✅ VERIFIED-MISSING — greenfield | beta-readiness-1 |
| 3 | **Idempotency POST kiteclass narrow scope** (signup + enrollment + beta-request; payment already has PaymentIdempotencyService) | FM V2 A3 | ⚠️ VERIFIED-PARTIAL (payment only) | beta-readiness-1 |
| 4 | **Per-resource authz A01 OWASP audit** (5 @PreAuthorize hits parent module — partial coverage; cross-tenant/cross-user IT coverage incomplete) | FM V2 A5 | ⚠️ VERIFIED-PARTIAL | beta-readiness-1 |
| 5 | **TS-3 Session reschedule/cancel** (no reschedule/cancel API; P3 Manager daily ops blocker) | Benchmark V2 TS-3 | ✅ VERIFIED-MISSING (GAP-291) | beta-readiness-2 |
| 6 | **OnboardingWizard no persona picker** (all P1 Solo land on P2 Owner wizard — wrong persona flow) | Persona V2 GAP-NEW-A | ✅ VERIFIED-MISSING (wizard exists 11 files but persona-fork missing) | beta-readiness-2 |
| 7 | **Batch grade entry missing** (no batchGrade/bulkGrade endpoint; per-student grade entry too slow for daily teacher use) | Persona V2 GAP-NEW-C | ✅ VERIFIED-MISSING | beta-readiness-2 |
| 8 | **ZaloPay gateway stub** (UnsupportedOperationException across all 4 methods — revenue critical) | Persona V2 GAP-NEW-B | ⚠️ VERIFIED-PARTIAL stub | beta-readiness-2 |

**P1 PDPL deadline 2026-07-01:**
| 9 | **Student data self-export** (PDPL Art 11 right-to-export — deadline 2026-07-01) | Persona V2 GAP-NEW-D | ✅ VERIFIED-MISSING | beta-readiness-3 (priority before deadline) |

**AWS-gated blockers (defer beta-readiness-5 post-GAP-612):** GAP-231/232/233 API contracts + GAP-216 PDF + GAP-217 alerts + GAP-286 mobile OTP (verified gap files exist OPEN/PARTIAL).

---

## §3 Revised Wave 108-N scope

### Wave beta-readiness-1 (REVISED — security + greenfield)

**4 bucket** (V1 had 5; PaymentController DROPPED):

| Bucket | Scope | Verified state | Estimated |
|---|---|---|---|
| A | XSS sanitize 7 `dangerouslySetInnerHTML` + DOMPurify wrap | VERIFIED 7 sites need fix | ~2h |
| B | Enrollment capacity model (greenfield: schema + maxStudents column + check + tests) | VERIFIED entirely absent | ~3h |
| C | Idempotency POST narrow (signup + enrollment + beta-request controllers — pattern from PaymentIdempotencyService) | VERIFIED partial | ~3h |
| D | Per-resource authz A01 audit + cross-tenant cross-user IT tests | VERIFIED partial | ~2h |

**Estimated:** 1-2 phiên (~10h)
**Blocking:** PHẢI ship trước beta invite

### Wave beta-readiness-2 (REVISED — feature gaps)

**4 bucket** (V1 had 4 different scope; revised after V2 state-check):

| Bucket | Scope | Source |
|---|---|---|
| A | TS-3 Session reschedule/cancel API + UI (GAP-291) | Benchmark V2 BLOCKING |
| B | OnboardingWizard persona picker (extend 11 existing files với persona-fork branching) | Persona V2 GAP-NEW-A |
| C | Batch grade entry endpoint + UI | Persona V2 GAP-NEW-C |
| D | ZaloPay gateway full implementation (replace stub 4 methods) | Persona V2 GAP-NEW-B revenue critical |

**Estimated:** 2-3 phiên (~10-12h)

### Wave beta-readiness-3 (REVISED — PDPL + trust gate)

**3 bucket** (V1 had 4; FM-4 trust signals/landing dropped — already exist):

| Bucket | Scope | Priority |
|---|---|---|
| A | Student data self-export endpoint (PDPL Art 11) | P1 — **DEADLINE 2026-07-01** |
| B | Refund/cancellation invoice variant (pairs với GAP-297 invoice 70% complete) | P1 |
| C | Tết holiday preset + Mon-Sat schedule template (verify existing schedule scope first per §2.8) | P1 |

**Estimated:** 1-2 phiên (~6-8h)

### Wave beta-readiness-4 (REVISED — UX polish + GAP-726)

**2 bucket** (V1 had 4; onboarding/parent dashboard/Excel wizard dropped — all already exist):

| Bucket | Scope | Source |
|---|---|---|
| A | KC `/branding/wizard` blank fix (Wave 107 RST B2) | GAP-726 |
| B | Teacher attendance export + schedule publish to parents + notification inbox (Persona V2 PARTIAL items GAP-NEW-F/G/H) | Persona V2 |

**Estimated:** 1 phiên (~4h)

### Wave beta-readiness-5 (AWS-gated — unchanged)

Gated GAP-612 restore. Scope per V1 §2 still valid:
- Wave 107 3 gap flip DONE (GAP-543/657/659)
- Wave 105 live verify cluster
- API contract drift (GAP-231/232/233)
- 8 P0 AWS-gated cluster
- Wave 105 post-merge audit suite refresh

### Wave beta-readiness-6 (RST remaining)

V1 scope confirmed valid (16 RST flows: B-CRUD + B-vận-hành + C + D3+D4).

### Wave beta-readiness-7 (Phase 1 BETA closure)

V1 scope confirmed valid (Quality audit ≥80 + 5 beta tenant + 2-tuần monitor + Phase 2 decision).

---

## §4 Realistic timeline (REVISED — shrunk vs V1)

V1 estimate 3-4 tuần calendar. V2 reduces ~30% scope (false-positives dropped):

| Sub-wave | Sessions | Calendar |
|---|---|---|
| beta-readiness-1 (security + greenfield enrollment) | 1-2 | Week 1 |
| beta-readiness-2 (TS-3 + persona picker + batch grade + ZaloPay) | 2-3 | Week 1-2 |
| beta-readiness-3 (PDPL export + refund + schedule) | 1-2 | Week 2 (PDPL deadline 2026-07-01) |
| beta-readiness-4 (GAP-726 + 3 PARTIAL items) | 1 | Week 2 |
| beta-readiness-5 (AWS-gated) | 2-3 | Week 3 (gated AWS restore) |
| beta-readiness-6 (RST 16 flows) | 1-2 | Week 3-4 |
| beta-readiness-7 (Phase 1 closure) | 1 | Week 4 |

**Total revised:** 2.5-3 tuần calendar (vs V1 3-4 tuần).

---

## §5 META lesson + rule extension recommendation

**Pattern surfaced:** Outside-in audit agents shipped findings WITHOUT applying `audit-to-gap-pipeline.md` §2.8 state-check. 3/3 audits had 10-60% false-positive rate.

**Root cause:** Audit agent prompts (V1) said "read gap-status.csv + cross-reference" but did NOT mandate empirical grep verify before claim. V2 spawn explicitly mandated §2.8 + provided per-claim state-check command examples — V2 results 80%+ true-positive.

**Recommend rule extension:**

1. File META gap **GAP-734**: outside-in audit agent prompt template MUST include §2.8 state-check mandate per claim (apply to `quality/persona-based-business-review/SKILL.md` + `quality/simulation-gap-finder/SKILL.md` + audit-spawn templates)
2. Or extend `outside-in-coverage-trigger.md` §3 Bước 3 audit spawn with explicit "§2.8 state-check per claim mandatory" wording
3. Add CI detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days) — scan new audit reports for §2.8 evidence markers

V2 audits demonstrate methodology works — drift between gap CSV status + actual code state is REAL pattern across 188 active gaps. Continuous state-check needed for Phase 1 closure execution.

---

## §6 Open items

- [ ] File META GAP-734 (audit state-check mandate extension)
- [ ] Draft Wave beta-readiness-1 plan PR (next concrete action)
- [ ] Confirm 4 V2 audit findings táng score Phase 1 BETA gate from V1 "CANNOT clear" to V2 "8 verified P0 + 1 PDPL deadline"
- [ ] Re-validate Wave 105 post-merge audit deadline 2026-05-25 (overlaps với beta-readiness-5)
- [ ] Defer Wave 106 full RST plan officially (superseded by beta-readiness-6 chunked)

---

## §7 Cross-link

- 3 V1 audits shipped 2026-05-24 (PR #1758) — historical baseline
- 3 V2 audits shipped this PR — supersedes V1:
  - `2026-05-24-outside-in-phase-1-closure-persona-walkthrough-v2-state-checked.md`
  - `2026-05-24-outside-in-phase-1-closure-vn-edu-saas-benchmark-v2-state-checked.md`
  - `2026-05-24-outside-in-phase-1-closure-failure-mode-matrix-v2-state-checked.md`
- Rules:
  - `.claude/rules/audit-to-gap-pipeline.md` §2.8 (state-check mandate vindicated)
  - `.claude/rules/outside-in-coverage-trigger.md` §3 (audit spawn template extension recommend)
  - `.claude/rules/wave-tag-numbering-convention.md` (Wave 108 = beta-readiness-1)
- V1 roadmap PR #1758 merged (historical) — this V2 amendment supersedes

---

## §8 Log

- **2026-05-24 (V2 amendment):** Sinh ra từ 3 audit V2 state-checked findings post user mandate "re-spawn 3 audit với §2.8 state-check mandate". V1 false-positive elimination ~30-60% across 3 audits. Phase 1 BETA gate revised: 8 verified P0 + 1 PDPL deadline (vs V1 "17+38+8 findings, CANNOT clear"). Wave beta-readiness-1 scope reduced 5→4 bucket (drop PaymentController already fixed). Wave beta-readiness-2 scope re-targeted to verified BLOCKING (TS-3 + persona picker + batch grade + ZaloPay) vs V1 generic table-stakes. Timeline shrinks ~30% (~2.5-3 tuần vs ~3-4 tuần). META lesson: gap CSV status drift vs code state pervasive in 188-row backlog; §2.8 state-check mandate VINDICATED + extension recommended via GAP-734.
