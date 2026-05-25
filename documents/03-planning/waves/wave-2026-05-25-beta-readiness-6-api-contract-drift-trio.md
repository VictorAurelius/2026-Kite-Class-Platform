---
title: Wave beta-readiness-6 — API contract drift trio (payment-invoice + attendance + student-enrollment)
status: draft
created: 2026-05-25
updated: 2026-05-25
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
**Estimated wall-clock:** ~4-5h (3 agents Sonnet parallel — narrow domain scope per bucket); ~12-15h serial → ~3-4x speedup.

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
| Use Opus 1M cho audit drift checks | Sonnet đủ — `scripts/check-cross-layer-contract-drift.sh` deterministic validator; narrow scope per bucket |
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
| A | GAP-231 payment-invoice | bg-agent Sonnet | ~1.5-2h | ✅ kitehub-subscription/.../Invoice* + Payment* |
| B | GAP-232 attendance | bg-agent Sonnet | ~1.5-2h | ✅ kiteclass-core/.../Attendance* |
| C | GAP-233 student-enrollment | bg-agent Sonnet | ~1.5-2h | ✅ kiteclass-core/.../Student* + Enrollment* |
| Closure | 5-target sync + 3 P0 DONE flip | coordinator inline | ~30 min | After A/B/C |

Disjoint check:
- Bucket A: `kitehub/kitehub-subscription/.../Invoice*Controller.java` + `Payment*Controller.java` + `documents/01-business/payment-invoice/api-contract.md`
- Bucket B: `kiteclass/kiteclass-core/.../Attendance*Controller.java` + `documents/01-business/attendance/api-contract.md`
- Bucket C: `kiteclass/kiteclass-core/.../Student*Controller.java` + `Enrollment*Controller.java` + `documents/01-business/student-enrollment/api-contract.md`
- B + C đều trong kiteclass-core → potential merge conflict trong pom.xml hoặc test fixtures; coordinator merge sequential

---

## 3. Scope

**Stake tier:** MEDIUM → Sonnet 4.6 đủ (narrow domain scope per bucket + deterministic validator).
**Cross-layer?:** NO direct — chỉ BE code change + api-contract.md update; FE consumer paths verified via grep, không sửa FE code wave này.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-231 payment-invoice drift | 🟠 P1 | `kitehub/kitehub-subscription/.../Invoice*Controller.java` + `Payment*Controller.java` + `documents/01-business/payment-invoice/api-contract.md` | parallel batch 1 |
| 2 | **B** | GAP-232 attendance drift | 🟠 P1 | `kiteclass/kiteclass-core/.../Attendance*Controller.java` + `documents/01-business/attendance/api-contract.md` | parallel batch 1 |
| 3 | **C** | GAP-233 student-enrollment drift | 🟠 P1 | `kiteclass/kiteclass-core/.../Student*Controller.java` + `Enrollment*Controller.java` + `documents/01-business/student-enrollment/api-contract.md` | parallel batch 1 |
| 4 | **Closure** | 5-target sync + 3 P0 DONE | 🟠 P1 | After A/B/C verify | sequential |

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
| GAP-231 | Gap file | `bash scripts/query-gaps.sh 231` | OPEN P1 | ✅ exists |
| GAP-232 | Gap file | `bash scripts/query-gaps.sh 232` | OPEN P1 | ✅ exists |
| GAP-233 | Gap file | `bash scripts/query-gaps.sh 233` | OPEN P1 | ✅ exists |
| `scripts/check-cross-layer-contract-drift.sh` | CI validator | `ls scripts/check-cross-layer-contract-drift.sh` | (verify pre-spawn) | ✅ exists |
| `documents/01-business/{payment-invoice,attendance,student-enrollment}/api-contract.md` | Domain contracts | (verify pre-spawn) | (3 files expected) | ✅ expected to exist |

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `bash scripts/check-cross-layer-contract-drift.sh --domain payment-invoice` exit 0 + `./mvnw -pl kitehub-subscription verify -P strict-warnings` | core-ci + kitehub-ci |
| B | `bash scripts/check-cross-layer-contract-drift.sh --domain attendance` exit 0 + `./mvnw -pl kiteclass-core verify -Dtest=Attendance*` | core-ci |
| C | `bash scripts/check-cross-layer-contract-drift.sh --domain student-enrollment` exit 0 + `./mvnw -pl kiteclass-core verify -Dtest=Student*,Enrollment*` | core-ci |
| Closure | All 3 domains drift check PASS | cross-layer-contract-drift |

---

## 6. Agent Spawn Pattern

3 agents parallel batch 1:

```
Bucket A: subagent_type=general-purpose, model=sonnet, isolation=worktree, run_in_background=true
Bucket B: subagent_type=general-purpose, model=sonnet, isolation=worktree, run_in_background=true
Bucket C: subagent_type=general-purpose, model=sonnet, isolation=worktree, run_in_background=true

After A+B+C verify:
  - Coordinator cherry-pick 3 PRs sequential (B + C trong kiteclass-core → potential merge conflict pom.xml)
  - Flip 3 P0 gaps DONE
  - 5-target sync
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

- **2026-05-25 evening (refresh phiên 2026-05-25):** Wave plan refresh cho hand-off phiên sau. Lessons-learned từ Wave br-5 + meta-3/4/5 áp dụng prospectively:
  - **Opus 1M mandatory** per `agent-model-opus-default.md` v1.0.0 — KHÔNG dùng Sonnet (recurrence ≥2 waves Sonnet thrash). Sửa Log cũ "Sonnet OK" → Opus 4.7 1M.
  - **Investigation phase first** per `release-fix-retry-budget.md` §3.5 — mỗi bucket agent PHẢI empirical-read 5+ controllers cùng domain TRƯỚC khi viết api-contract.md (không infer từ stale gap description filed 2026-04-26).
  - **Pre-spawn state-check** per `audit-to-gap-pipeline.md` §2.8 — coordinator next-session re-verify 3 controller paths + 3 domain folder existence trước khi spawn 3 agents. Stale ~1 tháng → likely module structure drift.
  - **Audit-1 findings reference** — Wave 7 audit-1 chấm 42/100 F là outside-in evidence (per `outside-in-coverage-trigger.md` §4 exempt).
  - **User direction 2026-05-25:** "fix hết gaps đã, tránh tốn công số" — wave này align fix-first policy.
  - **Phiên thực thi:** SAU — phiên draft này context 71%, hand-off mandatory.
- **2026-05-25 (status: draft):** Wave plan drafted per session handoff §"Wave 4/5". Counter `beta-readiness-6` = next monotonic. Outside-in audit SKIP per §4 row 4 (internal contract sync). 3 domain disjoint Backend → parallel-eligible. Sonnet OK cho narrow domain scope (⚠️ vacated by 2026-05-25 evening update above — use Opus 4.7 1M). Author: @nguyenvankiet (solo-dev).
