# Business Logic Audit — Wave 34 AI Branding Backend

**Date:** 2026-05-07
**Auditor:** Background agent ad3b6e89 (Sonnet, Explore subagent)
**Scope:** Wave 34 (Bucket 0/A/B/C/D — PRs #905-911) — `documents/01-business/kitehub/ai-branding/{rules,api-contract,use-cases}.md` + Java code

---

## Score: 78/100 — C+

| Category | Status | Notes |
|----------|--------|-------|
| Code ↔ rules.md sync | OK | InstanceLifecycleService matches BR-LIFE state machine; quality aggregator matches BR-QUALITY scaffold |
| 5-attribute compliance (`business-logic-review.md` v1.0.0) | **PARTIAL** | BR-LIFE-001..006 + BR-QUALITY-001 thiếu Reviewer + Compliance + Cadence blocks |
| Business correctness | Mostly OK | Tier quotas (FREE 3/PRO 10/PREMIUM 30/ENT -1) match audit rule §2.5 |
| Approval flow | DEFERRED | RebrandApprovalService exists Wave 3, NO controller endpoints — TBD per api-contract.md L527-535 |

---

## 5-Attribute Compliance Table (Wave 34 BRs)

| BR ID | Source | Rationale | Reviewer | Compliance | Cadence | ✓/✗ |
|-------|--------|-----------|----------|-----------|---------|:---:|
| BR-LIFE-001..006 | ai-branding-guidelines.md §6 | State machine NOT_STARTED→...→DEPLOYED⇄REGENERATING; FAILED retry | ✗ missing | ✗ N/A not stated | ✗ missing | ⚠️ |
| BR-QUALITY-001 | guidelines.md §5 | Pass threshold ≥70 trước DEPLOY | ✗ missing | ✗ N/A not stated | ✗ missing | ✗ |
| BR-WIZARD-001..006 | Wave 34 design-patterns.md | Audience/tone/tier-gating | ✓ | ✓ N/A pure domain | ✓ Q3 2026 | ✅ |
| BR-APRV-001..006 | GAP-070 (Wave 3) | Two-person rule + 24h TTL + optimistic lock | ✓ (Wave 3) | ✓ | ✓ | ⚠️ deferred to controller PR |

---

## Top 5 Findings

| # | Sev | Issue | File |
|---|:---:|-------|------|
| 1 | 🔴 P0 | `quality-gate.pass-threshold=70` HARDCODED `THRESHOLD = 70` | `kitehub-branding/.../wizard/quality/QualityScoreAggregator.java:33` — violates 12-factor; blocks A/B testing post-deploy |
| 2 | 🟠 P1 | BR-LIFE-001..006 + BR-QUALITY-001 thiếu 5-attribute compliance blocks per `business-logic-review.md` v1.0.0 | `documents/01-business/kitehub/ai-branding/rules.md` §Lifecycle (L72-81) + §Quality Gate (L90) |
| 3 | 🟠 P1 | Approval flow API endpoints chưa ship (POST /rebrand-approval, /approve, /reject) | `api-contract.md` L527-535 marks TBD |
| 4 | 🟡 P2 | QualityScoreAggregator sub-scores deterministic (hash-based v0); rules.md acknowledges nhưng thiếu formal deferral notice + GAP-226/227/228 link | `rules.md` §Quality Gate L90 |
| 5 | 🟢 P3 | Missing test cho regenerate-quota midnight boundary edge case | `RegenerateQuotaServiceTest.java` |

---

## Gap Recommendations

- **NEW GAP P0**: Externalize `quality-gate.pass-threshold` qua `application.yml` + `@Value` inject
- **NEW GAP P1**: Append 5-attribute compliance blocks cho BR-LIFE + BR-QUALITY trong `rules.md`
- **TRACKED**: Approval controller (Wave 35 candidate)
- Sub-score scaffolding: doc-only update linking GAP-226/227/228

---

## 1-line summary

Wave 34 ship 78/100 business logic: core lifecycle/quota/quality-gate functional; threshold hardcoded (12-factor violation); BR-LIFE/QUALITY thiếu 5-attribute review block; approval API deferred Wave 35.
