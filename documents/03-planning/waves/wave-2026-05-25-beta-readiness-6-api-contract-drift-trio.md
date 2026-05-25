---
title: Wave beta-readiness-6 — API contract drift trio (payment-invoice + attendance + student-enrollment)
status: draft
created: 2026-05-25
updated: 2026-05-26
wave: 6
tag_primary: beta-readiness
tags_secondary: [api-contract, contract-drift, gap-231, gap-232, gap-233]
counter: 6
date_launch: 2026-05-25
waves: [beta-readiness-6]
gaps: [GAP-231, GAP-232, GAP-233]
---

# Wave beta-readiness-6 — API contract drift trio (3 domain disjoint)

**Goal:** Đồng bộ Java `@RequestMapping` URLs + DTO schemas với `api-contract.md` declarations cho 3 domains (payment-invoice + attendance + student-enrollment). FE/external consumer trust restored.
**Trigger:** Session handoff `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 4/5" — 3 API contract drift gaps blocking consumer integration. Clean disjoint domain split (3 backend services, separate codebases).
**Estimated wall-clock:** ~4-5h (3 agents Opus 4.7 1M parallel — narrow domain scope per bucket); ~12-15h serial → ~3-4x speedup.

---

## 1. Brainstorm

**Q1 (alignment — inside-out 4-bucket):**

- **Inside-out từ session handoff** §"Wave 4/5": 3 P0 drift gap (GAP-231 payment-invoice + GAP-232 attendance + GAP-233 student-enrollment)
- **Inside-out từ queue file:** verify 3 gaps trong Phase 1 BETA scope
- **Inside-out từ audit:** Wave 98 API contract audit 76/100 FAIL surfaced cross-layer drift pattern (cross-link)
- **Outside-in NEW:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 (internal scope — contract sync = ops, không user-facing change)

Persona phục vụ: FE/external API consumers + future-Claude session pickup khi consume those endpoints. Domain: 3 disjoint BE services.

**Q2 (trade-offs):**

| Rejected option | Reason |
|---|---|
| Single agent serial 3 domains | 3 disjoint scope → parallel 3x speedup |
| Sonnet 4.6 cho narrow domain scope | Rejected per `agent-model-opus-default.md` v1.0.0 — recurrence ≥2 waves Sonnet thrash (br-4 + br-8 Đợt 1); Opus 4.7 1M mandatory mọi agent spawn |
| Skip integration test verification | Per `audit-skill-rubric-api-contract-audit.md` integration test = mandatory check; without IT verify, drift catches at runtime |
| Defer Wave audit-2 sau audit suite findings | Audit suite Wave audit-1 chạy parallel sẽ surface findings overlap nếu drift detected — ship code fix Wave beta-readiness-6 independent |

**Q3 (risks):**

| Risk | Recovery |
|---|---|
| Drift fix breaks existing FE consumer | Verify FE consumer paths via grep before code change; ship contract update first → code update second |
| Cross-layer contract drift detector false-positive | Agent reads `scripts/check-cross-layer-contract-drift.sh` output + manual verify |
| Missing integration test causes future drift | Agent adds IT verify per `audit-skill-rubric-api-contract-audit.md` mandate |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-231 payment-invoice | bg-agent Opus 1M | ~1.5-2h | ✅ kiteclass-core/.../module/{payment,invoice}/{Payment,Invoice,Refund,InstallmentPlan}Controller (5 ctrl, 32 endpoints) |
| B | GAP-232 attendance | bg-agent Opus 1M | ~1.5-2h | ✅ kiteclass-core/.../module/attendance/{Attendance,AttendanceClassBatch,AttendancePeriod}Controller + parent facet (4 ctrl, 9 endpoints) |
| C | GAP-233 student-enrollment | bg-agent Opus 1M | ~1.5-2h | ✅ kiteclass-core/.../module/{enrollment,student}/{Enrollment,Student}*Controller (5 ctrl, 6 endpoints) |
| Closure | 5-target sync + 3 P0 DONE flip | coordinator inline | ~30 min | After A/B/C |

Disjoint check (verified 2026-05-26 pre-spawn state-check):
- Bucket A: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/controller/Payment{,Webhook}Controller.java` + `module/invoice/controller/{Invoice,Refund,InstallmentPlan}Controller.java` + `documents/01-business/kiteclass/payment-invoice/api-contract.md` (PARTIAL drift — ~22-25/32 endpoints documented)
- Bucket B: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/controller/*Controller.java` + parent facet `module/parent/controller/ParentAttendanceFacetController.java` + `documents/01-business/kiteclass/attendance/api-contract.md`
- Bucket C: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/enrollment/controller/EnrollmentController.java` + `module/student/controller/{Student,InternalStudent,StudentPortal}Controller.java` + bulkimport + `documents/01-business/kiteclass/student-enrollment/api-contract.md`
- **CORRECTED 2026-05-26:** ALL 3 buckets touch `kiteclass-core` (Bucket A previously mis-scoped to `kitehub-subscription`). Merge conflict probability HIGH on `kiteclass-core/pom.xml` + test fixtures → coordinator merge MUST be sequential A → B → C.

---

## 3. Scope

**Stake tier:** MEDIUM → Opus 4.7 1M per `agent-model-opus-default.md` v1.0.0 (Sonnet rejected — recurrence ≥2 waves thrash).
**Cross-layer?:** NO direct — chỉ BE code change + api-contract.md update; FE consumer paths verified via grep, không sửa FE code wave này.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-231 payment-invoice drift (P0) | 🔴 P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{payment,invoice}/controller/*Controller.java` + `documents/01-business/kiteclass/payment-invoice/api-contract.md` | parallel batch 1 |
| 2 | **B** | GAP-232 attendance drift (P0) | 🔴 P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/controller/*Controller.java` + `module/parent/controller/ParentAttendanceFacetController.java` + `documents/01-business/kiteclass/attendance/api-contract.md` | parallel batch 1 |
| 3 | **C** | GAP-233 student-enrollment drift (P0) | 🔴 P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{enrollment,student}/controller/*Controller.java` + bulkimport + `documents/01-business/kiteclass/student-enrollment/api-contract.md` | parallel batch 1 |
| 4 | **Closure** | 5-target sync + 3 P0 DONE | 🔴 P0 | After A/B/C verify | sequential |

### Per-bucket pattern

1. Run `bash scripts/check-cross-layer-contract-drift.sh` cho domain target
2. Reconcile drift: edit api-contract.md OR Controller signature (chọn source-of-truth per gap analysis)
3. Add integration test verify schema (DTO field check, error code check, response shape check)
4. Verify per `audit-skill-rubric-api-contract-audit.md` 7-check matrix per endpoint
5. Acceptance: `scripts/check-cross-layer-contract-drift.sh` exit 0 cho domain; per-endpoint IT exists

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| GAP-231 | Gap file | `grep -E "^GAP-231," documents/04-quality/gaps/gap-status.csv` | OPEN P0 Backend phase-1-beta (verified 2026-05-26) | ✅ exists |
| GAP-232 | Gap file | `grep -E "^GAP-232," documents/04-quality/gaps/gap-status.csv` | OPEN P0 Backend phase-1-beta (verified 2026-05-26) | ✅ exists |
| GAP-233 | Gap file | `grep -E "^GAP-233," documents/04-quality/gaps/gap-status.csv` | OPEN P0 Backend phase-1-beta (verified 2026-05-26) | ✅ exists |
| `scripts/check-cross-layer-contract-drift.sh` | CI validator | `ls scripts/check-cross-layer-contract-drift.sh` | exists (no `--domain` filter; `--report-only` categorizes 86 repo-wide drifts) | ✅ exists |
| `documents/01-business/kiteclass/payment-invoice/api-contract.md` | Bucket A domain contract | `wc -l documents/01-business/kiteclass/payment-invoice/api-contract.md` | 189 lines (PARTIAL drift per GAP-231 — ~22-25/32 endpoints documented) | ✅ verified 2026-05-26 |
| `documents/01-business/kiteclass/attendance/api-contract.md` | Bucket B domain contract | `wc -l documents/01-business/kiteclass/attendance/api-contract.md` | 113 lines (PARTIAL drift per GAP-232 — all 9 endpoints stubbed, quality moderate) | ✅ verified 2026-05-26 |
| `documents/01-business/kiteclass/student-enrollment/api-contract.md` | Bucket C domain contract | `wc -l documents/01-business/kiteclass/student-enrollment/api-contract.md` | 91 lines (PARTIAL drift per GAP-233 — student CRUD + 6 enrollment endpoints) | ✅ verified 2026-05-26 |
| Bucket A controllers | 5 Java files | `find kiteclass/kiteclass-core -name "*Controller.java" \| grep -iE "payment\|invoice\|refund\|installment"` | `Payment{,Webhook}Controller` + `Invoice{,Refund,InstallmentPlan}Controller` (5 ctrl, 32 endpoints) | ✅ verified 2026-05-26 |
| Bucket B controllers | 4 Java files | `find kiteclass/kiteclass-core -name "*Controller.java" \| grep -iE "attendance"` | `Attendance{,ClassBatch,Period}Controller` + `ParentAttendanceFacetController` (4 ctrl, 9 endpoints) | ✅ verified 2026-05-26 |
| Bucket C controllers | 5 Java files | `find kiteclass/kiteclass-core -name "*Controller.java" \| grep -iE "student\|enrollment"` | `EnrollmentController` + `Student{,Internal,Portal}Controller` + `BulkImportController` (5 ctrl, 6+ endpoints) | ✅ verified 2026-05-26 |

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `bash scripts/check-cross-layer-contract-drift.sh --report-only \| grep -iE "payment\|invoice\|refund\|installment"` (no `--domain` flag — manual filter) + `cd kiteclass/kiteclass-core && ./mvnw verify -Dtest=Payment*,Invoice*,Refund*,InstallmentPlan*` | core-ci + cross-layer-contract-drift |
| B | `bash scripts/check-cross-layer-contract-drift.sh --report-only \| grep -i attendance` + `cd kiteclass/kiteclass-core && ./mvnw verify -Dtest=Attendance*` | core-ci + cross-layer-contract-drift |
| C | `bash scripts/check-cross-layer-contract-drift.sh --report-only \| grep -iE "student\|enrollment"` + `cd kiteclass/kiteclass-core && ./mvnw verify -Dtest=Student*,Enrollment*` | core-ci + cross-layer-contract-drift |
| Closure | All 3 domain drift entries absent from `--report-only` output | cross-layer-contract-drift |

---

## 6. Agent Spawn Pattern

3 agents parallel batch 1 (per `agent-model-opus-default.md` v1.0.0 + `agent-background-spawn-default.md` v1.0.1):

```
Bucket A: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket B: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket C: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true

After A+B+C verify:
  - Coordinator merge 3 PRs SEQUENTIAL A → B → C (ALL 3 in kiteclass-core → high pom.xml + test fixture merge conflict probability)
  - Each agent prompt MUST include investigation-first mandate per `release-fix-retry-budget.md` §3.5:
    "TRƯỚC khi viết api-contract.md, đọc empirically 5+ controllers cùng domain + verify @RequestMapping URLs +
     DTO field names + error annotation. KHÔNG infer từ stale gap description filed 2026-04-26."
  - Flip 3 P0 gaps DONE (CSV first per gap-architecture-v2.md, then markdown checkbox sync)
  - 5-target sync per post-merge-sync-completeness.md (gap-status.csv + ROADMAP §🚀 + wave-history.jsonl + MEMORY.md + session-handoff)
```

---

## 7. Closure Protocol

1. All 3 buckets SHIPPED + drift check exit 0 cho 3 domains
2. 3 P0 gaps flipped (GAP-231/232/233) per `gap-done-discipline.md`
3. Integration tests verify per-endpoint schema match
4. 5-target sync + handoff
5. Worktree cleanup

---

## 8. Log

- **2026-05-26 (state-check patch):** Coordinator next-session pre-spawn state-check per `audit-to-gap-pipeline.md` §2.8 + `release-fix-retry-budget.md` §3.5 surfaced 3 scope errors trong plan §2/§3/§5:
  - **Bucket A module mis-scoped:** Plan said `kitehub/kitehub-subscription/.../Invoice*+Payment*Controller`; actual = `kiteclass/kiteclass-core/.../module/{payment,invoice}/{Payment,Invoice,Refund,InstallmentPlan}Controller` (5 ctrl, 32 endpoints). ALL 3 buckets in `kiteclass-core` (not 2 as plan said) → merge conflict probability HIGH, sequential merge mandatory.
  - **3 api-contract.md paths missing `kiteclass/` prefix:** Plan referenced top-level `documents/01-business/{payment-invoice,attendance,student-enrollment}/`; actual 01-business uses 2-level nesting `documents/01-business/{kitehub,kiteclass}/<domain>/`. All 3 contracts EXIST under correct paths (PARTIAL drift not total per gap problem statements).
  - **Drift validator interface:** No `--domain` flag exists; `--report-only` shows all 86 repo-wide drifts categorized. Verification commands updated §5 to use `grep` filter on report output.
  - **§3 Priority elevated P1 → P0:** CSV row reflects P0 (Business-Logic per `meta-gap-priority.md` §3 "wrong API contract = wrong product"); plan §3 incorrectly showed P1.
  - **§6 model=sonnet → model=opus:** Sync with 2026-05-25 evening Log statement; previously inconsistent (Log said Opus, code block said Sonnet).
  - Counterfactual: spawning without these patches → 3 agents go to wrong module paths Bucket A (404 file read) + wrong contract paths all 3 buckets → likely autocompact thrash + retry cycles. State-check eliminated ~30-60 min preventable round-trips.
- **2026-05-25 evening (refresh phiên 2026-05-25):** Wave plan refresh cho hand-off phiên sau. Lessons-learned từ Wave br-5 + meta-3/4/5 áp dụng prospectively:
  - **Opus 1M mandatory** per `agent-model-opus-default.md` v1.0.0 — KHÔNG dùng Sonnet (recurrence ≥2 waves Sonnet thrash). Sửa Log cũ "Sonnet OK" → Opus 4.7 1M.
  - **Investigation phase first** per `release-fix-retry-budget.md` §3.5 — mỗi bucket agent PHẢI empirical-read 5+ controllers cùng domain TRƯỚC khi viết api-contract.md (không infer từ stale gap description filed 2026-04-26).
  - **Pre-spawn state-check** per `audit-to-gap-pipeline.md` §2.8 — coordinator next-session re-verify 3 controller paths + 3 domain folder existence trước khi spawn 3 agents. Stale ~1 tháng → likely module structure drift.
  - **Audit-1 findings reference** — Wave 7 audit-1 chấm 42/100 F là outside-in evidence (per `outside-in-coverage-trigger.md` §4 exempt).
  - **User direction 2026-05-25:** "fix hết gaps đã, tránh tốn công số" — wave này align fix-first policy.
  - **Phiên thực thi:** SAU — phiên draft này context 71%, hand-off mandatory.
- **2026-05-25 (status: draft):** Wave plan drafted per session handoff §"Wave 4/5". Counter `beta-readiness-6` = next monotonic. Outside-in audit SKIP per §4 row 4 (internal contract sync). 3 domain disjoint Backend → parallel-eligible. Sonnet OK cho narrow domain scope (⚠️ vacated by 2026-05-25 evening update above — use Opus 4.7 1M). Author: @nguyenvankiet (solo-dev).
