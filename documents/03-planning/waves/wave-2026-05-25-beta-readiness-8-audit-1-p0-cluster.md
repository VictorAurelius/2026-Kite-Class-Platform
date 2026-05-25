---
title: Wave beta-readiness-8 — Audit-1 P0 cluster (Security IDOR + 4 Business Logic + 1 API contract)
status: draft
created: 2026-05-25
updated: 2026-05-25
wave: 8
tag_primary: beta-readiness
tags_secondary: [audit-fix, security, business-logic, phase-1-beta-gate]
counter: 8
date_launch: 2026-05-25
waves: [beta-readiness-8]
gaps: [GAP-737, GAP-738, GAP-739, GAP-740, GAP-741]
---

# Wave beta-readiness-8 — Audit-1 P0 cluster

**Goal:** Fix 5 P0 finding từ Wave audit-1 (Security 1 + Business Logic 4) để Wave audit-2 re-run đạt Phase 1 BETA gate ≥80/100 cả 4 axis (Security + Business Logic + API Contract + Ops).
**Trigger:** Wave audit-1 (2026-05-25) ship 4 audit suite với 1 P0 Security IDOR + 4 P0 Business Logic. Per `audit-to-gap-pipeline.md` §3 fix priority + `post-wave-audit-mandate.md` cadence — P0 findings phải có wave plan ≤3 ngày.
**Estimated wall-clock:** ~5-6h Opus 1M coordinator + 5 bg-agent Sonnet parallel; ~20h serial → ~3-4x speedup.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment — inside-out 4-bucket per `inside-out-completeness-trigger.md` §3):**

- **Inside-out từ ROADMAP §🚀 Next Action:** Wave audit-1 findings (4 audit reports shipped PR #1792) + 4 wave plan draft (PR #1791) chưa cover audit-1 P0 ← gap chính
- **Inside-out từ queue file `documents/03-planning/inside-out-queue.md`:** empty (chưa có item liên quan audit-1)
- **Inside-out từ audit:** Wave audit-1 2026-05-25 (chính source cho wave này — 4 audit reports với 5 P0 + 5+ P1)
- **Outside-in NEW:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 (Wave 100 3-agent persona/benchmark/failure-mode audit 2026-05-19 + Wave audit-1 2026-05-25 đều cover surface ≤30 ngày) + row 2 (bug fix cụ thể, audit chính danh root cause)

Persona phục vụ: Phase 1 BETA gate (5 beta tenant onboard); thesis defense readiness (compliance + business correctness audit-ready); developer (Living Docs verification chain integrity).

**Q2 (trade-offs — alternatives rejected):**

| Rejected option | Reason |
|---|---|
| Extend beta-readiness-5/6/7 hiện có với audit-1 fix items | Phá disjoint scope của 4 wave đã draft; tăng bucket count per wave risk rate-limit |
| File audit-1 fix vào meta-1 (META wave) | Scope mismatch — audit-1 5 P0 là code fix + business docs, không phải META governance |
| Bỏ qua audit-1 findings, ship beta-readiness-5/6/7 trước | Vi phạm `post-wave-audit-mandate.md` cadence ≤3 ngày + P0 backlog cộng dồn |
| File new tag (vd `audit-fix-1`) | Phá monotonic counter beta-readiness; tag taxonomy thêm overhead không cần |

**Q3 (risks):**

| Risk | Recovery |
|---|---|
| Bucket B 3-layer docs scope lớn (9 file × 3 domain) | Coordinator inline Bucket B nếu agent slow; split thành 3 sub-bucket per domain |
| PaymentMethod refactor (Bucket C) touch nhiều file cross-service | IT test cover trước refactor; staged rollout (canonical declare → import migration → remove duplicate) |
| Course.pricingModel migration default change (Bucket D) | DB migration backward compat (existing rows keep value, only NEW rows default change); test với existing data |
| Cross-bucket conflict (Bucket B 3-layer docs + Bucket C PaymentMethod + Bucket D Course.pricingModel cùng touch payment-record domain) | Bucket B docs first → Bucket C+D fix với reference; sequential merge nếu cần |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-737 Security IDOR ImmutableConsentController | bg-agent Sonnet | ~1.5h | ✅ kitehub-platform consent controller |
| B | GAP-738 3-layer business docs cho 3 new domains | bg-agent (or coordinator inline) | ~3-4h | ⚠️ touch 9 file mới trong `documents/01-business/kiteclass/` |
| C | GAP-739 PaymentMethod enum DUPLICATE + drift | bg-agent Sonnet | ~2-3h | ⚠️ cross-service refactor (kiteclass-core + kitehub-subscription) |
| D | GAP-740 Course.pricingModel default fix | bg-agent Sonnet | ~1h | ✅ kiteclass-core Course entity + migration |
| E | GAP-741 PricingModel.java javadoc ADR-027 → ADR-035 | bg-agent Sonnet | ~30 min | ✅ kiteclass-core javadoc only |

Disjoint check:
- Bucket A: kitehub-platform `ImmutableConsentController.java` + IT test
- Bucket B: `documents/01-business/kiteclass/{reschedule,course-pricing,payment-record}/*.md` (9 file mới)
- Bucket C: cross-service PaymentMethod enum refactor (Java + TypeScript + api-contract.md)
- Bucket D: `Course.java` entity + migration V*.sql + IT test
- Bucket E: `PricingModel.java` javadoc only (file overlap với Bucket D nếu cùng file — coordinator merge sequential nếu cần)

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM → model: Opus 4.7 (1M) coordinator + Bucket B coordinator inline (docs scope lớn); bg-agent A/C/D/E Sonnet 4.6.
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO — backend + business docs scope only; no FE cross-layer (Bucket C TypeScript update là consumer-side import refactor, không phải new API contract).

**Outside-in audit:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 (Wave 100 + Wave audit-1 đã cover surface ≤30 ngày) + row 2 (bug fix cụ thể).

**Post-wave audit (per `post-wave-audit-mandate.md` §2.4):** Domain key `beta-readiness` — post-wave audit suite cần re-run sau merge (Security + Business Logic + API Contract) ≤3 ngày để verify P0 closed.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-737 | 🔴 P0 | `kitehub-platform/src/main/java/.../consent/ImmutableConsentController.java` + IT test | Đợt 1 |
| 2 | **D** | GAP-740 | 🔴 P0 | `kiteclass-core/src/main/java/.../course/Course.java` + migration | Đợt 1 |
| 3 | **E** | GAP-741 | 🔴 P0 | `kiteclass-core/src/main/java/.../course/PricingModel.java` (javadoc only) | Đợt 1 |
| 4 | **B** | GAP-738 | 🔴 P0 | `documents/01-business/kiteclass/{reschedule,course-pricing,payment-record}/{rules,use-cases,api-contract}.md` (9 file) | Đợt 2 (coordinator inline) |
| 5 | **C** | GAP-739 | 🔴 P0 | `kiteclass-core/src/main/java/.../payment/PaymentMethod.java` + cross-service refactor | Đợt 2 |

### Bucket A — Security IDOR fix (GAP-737)

- Files: `kitehub-platform/src/main/java/.../consent/ImmutableConsentController.java` + bean `authz.canAccessConsent()` + IT test
- Tests: cross-user IDOR → 403; admin override → 200
- Acceptance: GAP-737 OPEN → DONE; Security audit re-run P0-1 closed → Cat3 +6
- Pattern reference: Wave 105 PR #1727 Bucket E Security P0 OWASP A01

### Bucket B — 3-layer business docs (GAP-738) — coordinator inline

- Files: 9 file mới trong `documents/01-business/kiteclass/{reschedule,course-pricing,payment-record}/{rules,use-cases,api-contract}.md`
- Pattern: BR-DOMAIN-NNN business rules + UC-DOMAIN-NNN use cases + endpoint declarations
- Acceptance: 9 files + `check-3-layer-completeness.sh` PASS + Business Logic Cat 1 score +X

### Bucket C — PaymentMethod enum dedup (GAP-739)

- Files: `kiteclass-core/src/main/java/.../payment/PaymentMethod.java` (canonical) + remove duplicate trong kitehub-subscription (or vice versa) + FE TypeScript union update + api-contract.md
- Tests: IT test all PaymentMethod values produce valid payment record
- Acceptance: GAP-739 DONE + `check-cross-layer-contract-drift.sh` PASS

### Bucket D — Course.pricingModel default (GAP-740)

- Files: `Course.java` entity + new migration `V*.sql` ALTER DEFAULT + IT test
- Tests: new Course no pricingModel specified → assert PER_HOUR
- Acceptance: GAP-740 DONE; Business Logic audit P0-3 closed

### Bucket E — PricingModel.java javadoc (GAP-741)

- Files: `PricingModel.java` javadoc only (line-level edit)
- Tests: none (docs-only sub-bucket)
- Acceptance: 0 reference ADR-027 trong PricingModel.java; javadoc cite ADR-035

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `ImmutableConsentController` | Java class | `grep -rn "ImmutableConsentController" kitehub-platform/src/main/java/` | (verify) | ✅ exists (Wave br-4 ship) |
| `PaymentMethod` (enum) | Java enum | `grep -rn "enum PaymentMethod" kiteclass kitehub` | (verify cross-service) | ⚠️ duplicate exists (audit P0-2) |
| `Course.pricingModel` | Java field | `grep -rn "pricingModel" kiteclass-core/src/main/java/` | (verify default value) | ✅ exists (Wave br-4 hotfix #1784) |
| `PricingModel.java` | Java enum | `ls kiteclass-core/src/main/java/.../course/PricingModel.java` | (verify ADR-027 javadoc) | ✅ exists |
| `ADR-035` | ADR doc | `ls documents/02-architecture/adr/ADR-035*.md` | (verify canonical pricing decision) | ✅ exists |
| `documents/01-business/kiteclass/reschedule/` | Business docs folder | `ls documents/01-business/kiteclass/reschedule/ 2>/dev/null` | 0 file | 🆕 to-be-created (Bucket B) |
| `documents/01-business/kiteclass/course-pricing/` | Business docs folder | `ls documents/01-business/kiteclass/course-pricing/ 2>/dev/null` | 0 file | 🆕 to-be-created (Bucket B) |
| `documents/01-business/kiteclass/payment-record/` | Business docs folder | `ls documents/01-business/kiteclass/payment-record/ 2>/dev/null` | 0 file | 🆕 to-be-created (Bucket B) |
| `authz.canAccessConsent(consentId)` | Java bean method | `grep -rn "canAccessConsent" kitehub-platform/src/main/java/` | 0 match (expected) | 🆕 to-be-created (Bucket A) |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-platform verify -P strict-warnings` | core-ci + script-quality |
| B | `bash scripts/check-3-layer-completeness.sh` PASS | three-layer-completeness CI |
| C | `cd kiteclass && ./mvnw -pl kiteclass-core verify` + `bash scripts/check-cross-layer-contract-drift.sh` | cross-layer-contract-drift CI |
| D | `cd kiteclass && ./mvnw -pl kiteclass-core verify` (IT test new) | core-ci |
| E | `cd kiteclass && ./mvnw -pl kiteclass-core compile` (docs-only sub-bucket, no test) | strict-warnings |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

- Bucket A/D/E spawned bg-agent Sonnet 4.6 với `run_in_background: true` + worktree isolation (RELATIVE paths)
- Bucket C bg-agent Sonnet (cross-service refactor risk, but well-scoped)
- Bucket B coordinator inline Opus 4.7 1M (docs scope lớn 9 file, narrative consistency cần coordination)
- Coordinator merges sequentially sau all background completions

Stagger spawn 3-1-1 (per Wave 102.7.4 rate-limit lesson):

```
Đợt 1 (3 bg-agent parallel):
  - Bucket A: GAP-737 Security IDOR
  - Bucket D: GAP-740 Course.pricingModel default
  - Bucket E: GAP-741 PricingModel.java javadoc

Đợt 2 (1 bg-agent + coordinator inline, sau Đợt 1 land):
  - Bucket C: GAP-739 PaymentMethod enum dedup (bg-agent)
  - Bucket B: GAP-738 3-layer business docs (coordinator inline)
```

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `wave-closure-scope-completeness.md`:

1. Mỗi bucket PR cập nhật gap file Log + status flip (per `gap-architecture-v2.md` CSV canonical)
2. Sync `documents/04-quality/gaps/gap-status.csv` (5 dòng status flip): GAP-737/738/739/740/741 OPEN → DONE 100%
3. ROADMAP §🎯 entry mới format: "Wave beta-readiness-8 SHIPPED — Audit-1 P0 cluster 5 buckets"
4. Wave plan frontmatter `status: draft → complete` flip
5. `wave-history.jsonl` append entry tag_primary=beta-readiness, counter=8
6. Scope-completeness reconciliation table per `wave-closure-scope-completeness.md` §3 (5 P0 items)
7. `bash scripts/prune-merged-worktrees.sh --yes` cleanup
8. Session handoff doc
9. **Post-wave audit re-run mandate** per `post-wave-audit-mandate.md` ≤3 ngày — Security + Business Logic + API Contract verify P0 closed → score recovery target ≥80
10. Closure PR commit trailer: `AUDIT_DEFER_DOMAIN_MILESTONE: beta-readiness` (per §2.4 domain registry)

---

## 8. Log

- **2026-05-25** (draft): Plan created post-Wave-audit-1 (4 audit reports SHIPPED). 5 P0 findings (Security 1 + Business Logic 4) clustered. Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 (Wave 100 + audit-1 cover ≤30 ngày). META domain `beta-readiness` — post-wave audit re-run mandate ≤3 ngày sau closure. Paired same-PR với plan edits: beta-readiness-7 thêm Bucket E (GAP-742 Outbox DLQ) + meta-1 thêm Bucket D (GAP-743 Entity-Mapper CI gate) — phương án C Hybrid per user direction 2026-05-25.
