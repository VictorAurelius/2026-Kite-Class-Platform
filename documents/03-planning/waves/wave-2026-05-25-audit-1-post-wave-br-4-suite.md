---
title: Wave audit-1 — Post-wave audit suite cho Wave beta-readiness-4
status: complete
created: 2026-05-25
updated: 2026-05-25
closed_at: 2026-05-25
wave: 1
tag_primary: audit
tags_secondary: [post-wave, security, business-logic, api-contract, ops-readiness, beta-readiness-4]
counter: 1
date_launch: 2026-05-25
waves: [audit-1]
gaps: []
---

# Wave audit-1 — Post-wave audit suite cho Wave beta-readiness-4

**Goal:** Hoàn thành 4-suite audit cadence (Security + Business Logic + API Contract + Ops Readiness) cho Wave beta-readiness-4 trước deadline 2026-05-27 per `post-wave-audit-mandate.md` §2.2 3-day cadence; refresh `output-review-mandate.md` §3 matrix scores; file follow-up gaps per `audit-to-gap-pipeline.md` Step 3.
**Trigger:** `post-wave-audit-mandate.md` §2.2 mandate — Wave br-4 last merge 2026-05-24 (PR #1789 closure). 3-day deadline = 2026-05-27. Today 2026-05-25 = T-2.
**Estimated wall-clock:** ~2.5-3h với 4 agents parallel Sonnet (audit read-only, không code change); ~10-12h serial → ~4x speedup.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment — inside-out 4-bucket per `inside-out-completeness-trigger.md` §3):**

- **Inside-out from session handoff** `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 1/5": 4-audit suite scope đã document — Security (Bucket B PDPL consent hash chain + Bucket A IAM wildcard) + Business Logic (Bucket C pricing + payment) + API Contract (B/C/D endpoints) + Ops Readiness (env-coverage CI gate + VN sample audit + hotfix iteration count)
- **Inside-out from queue file** `documents/03-planning/inside-out-queue.md`: không có item audit-specific (queue file scope = Phase 1 BETA infra/feature)
- **Inside-out from audit:** N/A — đây CHÍNH LÀ wave audit
- **Outside-in NEW:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 — wave 100% internal scope (audit refresh, ops). Cũng không user-facing change.

Persona phục vụ: Author (solo-dev) + future-Claude session pickup + Phase 1 BETA gate decision (≥80 quality target). Domain: 4 audit categories cho Wave br-4 scope.

**Q2 (trade-offs — alternatives rejected):**

| Rejected option | Reason |
|---|---|
| Skip audit suite, jump Wave 3/5 beta-signup-unblock | Vi phạm `post-wave-audit-mandate.md` §2.2 cadence + audit-gate hook BLOCK any code PR post-deadline |
| Run 4 audits serial (1 agent sequential) | 4x slower vs parallel; audit read-only = ideal cho wave-pack pattern |
| Use Opus 1M cho audit agents | Cost-overkill — audit-skill-rubric-*.md template-driven, Sonnet đủ; handoff notes "4 agents Sonnet OK" |
| Run thêm Performance + UI audit | Wave br-4 không touch FE bundle hay performance hot path; handoff §"Wave 1/5" scope 4 audits đủ |
| Defer audit Wave audit-2 (sau Wave meta-1) | Vi phạm 3-day deadline; cadence mandate explicit |

**Q3 (risks):**

| Risk | Recovery |
|---|---|
| Audit agent Sonnet thrash do auto-compact 200k (Wave br-4 lesson) | Audit-only = read scope smaller than impl; thrash unlikely; fallback Opus 1M nếu thrash detected |
| Audit findings surface new P0 blocking gate 80 | Expected — file follow-up gaps per `audit-to-gap-pipeline.md` Step 3; Wave audit-1 NOT block on score, just deliver evidence |
| audits-index.csv merge conflict (4 rows parallel) | Coordinator append 4 rows trong closure PR (single commit), không phải 4 separate PRs |
| Cross-bucket finding miss (audit scoped to category) | Closure PR §7 cross-cut summary catches; META audit ⊂ ops-readiness scope |
| GAP-612 AWS suspended → Ops Readiness can't live-verify CloudWatch | Audit reports current TF code + carry-forward GAP-144/257; mark blocked live verify follow-up |

---

## 2. Task Breakdown

| Bucket | Audit category | Owner | Effort | Disjoint? |
|--------|---------------|-------|--------|-----------|
| A | Security /100 v2 format | bg-agent Sonnet | ~30-45 min | ✅ skill `.claude/skills/quality/security-audit/` + write `audits/security/2026-05-25-*.md` |
| B | Business Logic /100 | bg-agent Sonnet | ~45-60 min | ✅ skill `.claude/skills/quality/business-logic-audit/` + write `audits/business-logic/2026-05-25-*.md` |
| C | API Contract /100 | bg-agent Sonnet | ~30-45 min | ✅ skill `.claude/skills/quality/api-contract-audit/` + write `audits/api-contract/2026-05-25-*.md` |
| D | Ops Readiness /100 | bg-agent Sonnet | ~45-60 min | ✅ skill `.claude/skills/quality/ops-readiness-audit/` + write `audits/ops-readiness/2026-05-25-*.md` |
| Closure | 4-target sync + follow-up gaps | coordinator inline | ~30-45 min | After 4 buckets ship |

Disjoint check:
- Bucket A reads `pom.xml` + Java code + `secrets.tf` + `iam.tf` → writes `audits/security/...`
- Bucket B reads `rules.md` + entity + Java service → writes `audits/business-logic/...`
- Bucket C reads `Controller.java` + `api-contract.md` + `Dto.java` → writes `audits/api-contract/...`
- Bucket D reads `infrastructure/**` + `helm/**` + workflow YAML + smoke scripts → writes `audits/ops-readiness/...`
- 4 disjoint write paths (different audit category folder); 4 partial overlap read paths (Java code) — read OK, no conflict
- Closure bucket runs AFTER all 4 complete; updates audits-index.csv + ROADMAP + output-review-mandate.md §3 + wave-history.jsonl

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM → model: Sonnet 4.6 cho 4 audit agents (read-only); Opus 4.7 cho coordinator inline closure work.
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO — audit = read-only, không touch BE/FE production code. KHÔNG cần Bucket 0 Foundation.

| # | Bucket | Category | Priority | Files (glob) | Spawn order |
|:-:|--------|----------|:--------:|--------------|:-----------:|
| 1 | **A** | Security /100 v2 format | 🟠 P1 | Read `kitehub/{*}/pom.xml` + `infrastructure/terraform-aws/{secrets,iam}.tf` + Wave br-4 Java diff; Write `documents/04-quality/audits/security/2026-05-25-wave-br-4-security-audit.md` | parallel batch 1 |
| 2 | **B** | Business Logic /100 | 🟠 P1 | Read `documents/01-business/{course-pricing,payment-recording,reschedule}/rules.md` + Wave br-4 BE entity diff + ADRs 027/030/034/035; Write `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` | parallel batch 1 |
| 3 | **C** | API Contract /100 | 🟠 P1 | Read `kitehub/kitehub-subscription/.../*Controller.java` + `documents/01-business/{*}/api-contract.md` + DTO; Write `documents/04-quality/audits/api-contract/2026-05-25-wave-br-4-api-contract-audit.md` | parallel batch 1 |
| 4 | **D** | Ops Readiness /100 | 🟠 P1 | Read `.github/workflows/*.yml` + `scripts/smoke-*.sh` + `infrastructure/helm/values-production.yaml` + Wave br-4 deploy changes; Write `documents/04-quality/audits/ops-readiness/2026-05-25-wave-br-4-ops-readiness-audit.md` | parallel batch 1 |
| 5 | **Closure** | 4-target sync + follow-up gaps | 🔴 P0 | Append 4 rows `audits-index.csv` + update ROADMAP + `output-review-mandate.md` §3 REFRESHED markers + `wave-history.jsonl` entry + follow-up gap files | After 4 buckets complete |

### Bucket A — Security /100 v2 audit

- **Skill:** `.claude/skills/quality/security-audit/SKILL.md` + `.claude/skills/quality/security-audit/reference/audit-report-template-v2.md`
- **Scope per handoff:** Bucket B PDPL consent hash chain SHA-256 + audit log immutability + Bucket A IAM wildcard pattern + secrets management
- **Format:** v2 format mandatory per GAP-564 — per-control evidence block (Command run + Output + Verdict + Evidence artifact ID)
- **Acceptance:** Output report `documents/04-quality/audits/security/2026-05-25-wave-br-4-security-audit.md` với 27/27 evidence blocks (Wave 94c precedent); score X/100 với delta vs prior 93/100; file gaps per `audit-to-gap-pipeline.md` Step 3 cho findings

### Bucket B — Business Logic /100 audit

- **Skill:** `.claude/skills/quality/business-logic-audit/SKILL.md` + 5-attribute check per `audit-skill-rubric-business-logic-audit.md`
- **Scope per handoff:** Bucket C BR-COURSE-PRICING-001..004 + BR-PAYMENT-METHOD-001..002 + Bucket D Cal.com reschedule pattern decision + ADRs 027/030/034/035
- **Acceptance:** Report `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md`; score X/100; cite BR rules + entity field mapping; verify error mapping per RFC 7807

### Bucket C — API Contract /100 audit

- **Skill:** `.claude/skills/quality/api-contract-audit/SKILL.md` + `scripts/check-cross-layer-contract-drift.sh` cho new endpoints
- **Scope per handoff:** Bucket B `/api/v1/consent/v2/*` + Bucket C `/api/v1/invoices/{id}/record-payment` + Bucket D `/api/v1/classes/{id}/reschedule` — schema check controller signature vs api-contract.md
- **Acceptance:** Report `documents/04-quality/audits/api-contract/2026-05-25-wave-br-4-api-contract-audit.md`; score X/100; cite per-endpoint pass/fail per `audit-skill-rubric-api-contract-audit.md`; file gap nếu drift detected

### Bucket D — Ops Readiness /100 audit

- **Skill:** `.claude/skills/quality/ops-readiness-audit/SKILL.md` + rubric `audit-skill-rubric-ops-readiness-audit.md`
- **Scope per handoff:** Bucket A env-coverage CI gate WARN-mode + Bucket E VN sample audit WARN-mode + Bucket D Outbox no-op consumer feature flag + post-merge hotfix iteration count (3 hotfixes = quality signal)
- **Acceptance:** Report `documents/04-quality/audits/ops-readiness/2026-05-25-wave-br-4-ops-readiness-audit.md`; score X/100; carry-forward GAP-257 (restore drill) + GAP-144 (AlertManager) status; new gaps cho findings

### Bucket Closure — 4-target sync

Per `post-merge-sync-completeness.md` §2 4-target framework + `wave-closure-scope-completeness.md` §3:

1. **Target 1 — `documents/04-quality/audits/audits-index.csv`:** Append 4 new rows (security + business-logic + api-contract + ops-readiness)
2. **Target 2 — `documents/04-quality/gaps/ROADMAP.md` §🎯 Current Status Snapshot:** Wave audit-1 entry với 4 audit scores + delta + follow-up gap count
3. **Target 3 — `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl`:** Append entry mới format (tag_primary=audit, counter=1)
4. **Target 4 — `~/.claude/projects/.../memory/MEMORY.md`:** No new memory entries expected (audit work doesn't surface new feedback class)
5. **Target 5 — `documents/03-planning/session-handoffs/2026-05-25-wave-audit-1-closure.md`:** Session handoff cho next pickup (Wave 2/5 meta-1 OR Wave 3/5 beta-signup)

Additional:
- **`output-review-mandate.md` §3 matrix REFRESHED markers** cho 4 rows (Security baseline + Business logic + API contracts + Ops readiness) — version PATCH bump
- **Follow-up gap files** filed per `audit-to-gap-pipeline.md` Step 3 cho new findings; expected 3-8 gaps total

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `.claude/skills/quality/security-audit/SKILL.md` | Audit skill | `ls .claude/skills/quality/security-audit/SKILL.md` | exists | ✅ exists |
| `.claude/skills/quality/business-logic-audit/SKILL.md` | Audit skill | `ls .claude/skills/quality/business-logic-audit/SKILL.md` | exists | ✅ exists |
| `.claude/skills/quality/api-contract-audit/SKILL.md` | Audit skill | `ls .claude/skills/quality/api-contract-audit/SKILL.md` | exists | ✅ exists |
| `.claude/skills/quality/ops-readiness-audit/SKILL.md` | Audit skill | `ls .claude/skills/quality/ops-readiness-audit/SKILL.md` | exists | ✅ exists |
| `documents/04-quality/audits/audits-index.csv` | Meta CSV index | `head -1 documents/04-quality/audits/audits-index.csv` | exists | ✅ exists |
| `documents/04-quality/audits/{security,business-logic,api-contract,ops-readiness}/` | Audit folders | `ls -d documents/04-quality/audits/{security,business-logic,api-contract,ops-readiness}/` | 4 folders exist | ✅ exists |
| `2026-05-25-wave-br-4-{category}-post-audit.md` | New audit reports | (post-spawn) | not yet created | 🆕 to-be-created (4 buckets) |

Banned shortcuts:
- ❌ Skip skill SKILL.md reference "because agent knows audit format"
- ❌ Skip `audits-index.csv` append "because narrative ROADMAP enough" — per `meta-csv-index-pattern.md` CSV canonical
- ❌ Output report without explicit /100 score (deal-breaker per `audit-skill-rubric-*.md` §1 transparency mandate)

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `ls documents/04-quality/audits/security/2026-05-25-*.md` + report contains `## Final Score: X/100` | N/A (docs-only PR — path filter skips test workflows) |
| B | `ls documents/04-quality/audits/business-logic/2026-05-25-*.md` + score block present | N/A |
| C | `ls documents/04-quality/audits/api-contract/2026-05-25-*.md` + score block present | N/A |
| D | `ls documents/04-quality/audits/ops-readiness/2026-05-25-*.md` + score block present | N/A |
| Closure | `bash scripts/check-rules-index-csv.sh` (если audits-index has similar validator) + git diff verify 5 target files synced | `meta-csv-indexes` + `script-quality` workflow |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

```
4 agents spawned trong single message với run_in_background: true
Stage 1 (parallel batch 1, all 4 launched simultaneously):
  Agent A: subagent_type=general-purpose, model=sonnet, isolation=worktree
    Prompt: Read .claude/skills/quality/security-audit/SKILL.md + audit-report-template-v2.md, audit Wave br-4 scope per §3 Bucket A, write report.
  Agent B: subagent_type=general-purpose, model=sonnet, isolation=worktree
    Prompt: Read .claude/skills/quality/business-logic-audit/SKILL.md, audit Wave br-4 scope per §3 Bucket B, write report.
  Agent C: subagent_type=general-purpose, model=sonnet, isolation=worktree
    Prompt: Read .claude/skills/quality/api-contract-audit/SKILL.md, audit Wave br-4 scope per §3 Bucket C, write report.
  Agent D: subagent_type=general-purpose, model=sonnet, isolation=worktree
    Prompt: Read .claude/skills/quality/ops-readiness-audit/SKILL.md, audit Wave br-4 scope per §3 Bucket D, write report.

Stage 2 (after all 4 complete — coordinator inline):
  - Read 4 agent reports from worktrees
  - Cherry-pick / copy reports to main worktree
  - Append 4 rows to audits-index.csv
  - Update ROADMAP §🎯 Current Status Snapshot
  - Update output-review-mandate.md §3 matrix 4 REFRESHED markers + version PATCH bump
  - Append wave-history.jsonl entry
  - File follow-up gap files per audit findings
  - Write session handoff note 2026-05-25-wave-audit-1-closure.md
  - Open closure PR + auto-merge per docs-only-pr-auto-merge.md (docs-only scope)
```

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-wave-cleanup.md`:

1. **All 4 audits SHIPPED** — reports exist với score block + cited evidence
2. **5-target sync per `post-merge-sync-completeness.md` §2 + handoff** all DONE
3. **Wave plan status: complete** + closed_at: 2026-05-25 added
4. **Scope-completeness reconciliation table** trong closure PR body (mọi §3 Scope item ✅/🟡/❌)
5. **Worktree cleanup** — `bash scripts/prune-merged-worktrees.sh --yes`
6. **Follow-up gaps filed** cho audit findings; cross-link to next-wave queue
7. **Wave audit-1 SHIPPED announcement** trong ROADMAP §🎯

---

## 8. Log

- **2026-05-25 (status: draft):** Wave plan created per session handoff `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 1/5 — wave-audit-1-post-wave-br-4-suite" pickup. Mandate trigger: `post-wave-audit-mandate.md` §2.2 3-day cadence (Wave br-4 last merge 2026-05-24, deadline 2026-05-27 = T-2 today). 4-audit suite scope locked from handoff. Tag-based naming per `wave-tag-numbering-convention.md` v1.0.0 — first wave với tag_primary=audit, counter=1. Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 row 4 (wave 100% internal scope — audit refresh = ops). Inside-out 3-source pull confirmed scope canonical (handoff §"Wave 1/5" = ROADMAP §🎯 mapped). Author: @nguyenvankiet (solo-dev coordinator).
