---
title: Wave DR/Backup — restore drill + MinIO/S3 backup + platform DR runbook
status: active
created: 2026-04-28
updated: 2026-04-28
gaps: [GAP-117, GAP-118, GAP-119]
deferred_to_next_wave: []
deferred_separate_track: [GAP-030]
---

# Wave DR/Backup — Cluster Pack 2

**Wave date:** 2026-04-29 (kicked off 2026-04-28 evening)
**Cluster theme:** Data safety + business continuity — close the second `audit-to-gap` queue cluster (after Wave Observability). Backup exists (GAP-093 DONE) but never restored, MinIO has no replication, no platform-wide DR runbook.
**Strategy reference:** First **real-world** validation of `quality/wave-pack-planner/SKILL.md` (PR #630) end-to-end. Self-test was meta (skill built itself); this wave consumes the skill on production-data gaps. Wall-clock target ~60 min per skill SKILL.md §"Wall-clock target" benchmark.

## Scope

| # | Gap | Title | Priority | Agent | Disjoint files |
|:-:|-----|-------|:--------:|:-----:|----------------|
| 1 | **GAP-117** | Backup restore drill automation (Phase 1+2) | 🔴 P0 | A — `feature-tdd-agent` | `documents/05-guides/restore-procedure.md` (NEW), `scripts/verify-restore.sh` (NEW), `.github/workflows/restore-drill.yml` (NEW) |
| 2 | **GAP-118** | MinIO + S3 backup + replication strategy | 🟠 P1 | B — `feature-tdd-agent` | `infrastructure/terraform-aws/s3-ecr.tf`, `infrastructure/terraform-oracle/main.tf` (versioning resource block), `kitehub/docker-compose.kitehub.yml` MinIO section + setup container |
| 3 | **GAP-119** | Platform-wide DR runbook + RTO/RPO matrix | 🟠 P1 | C — `docs-only-agent` | `documents/05-guides/disaster-recovery-plan.md` (NEW), `documents/05-guides/operations/dr-rto-rpo-matrix.md` (NEW or section in plan) |

## Deferred (separate track)

- **GAP-030** — AI branding-specific DR (P2). Different scope (Ollama/MinIO branding tables/RabbitMQ AI scope only). GAP-119 covers platform-scope; GAP-030 may be subsumed or recast after GAP-119 lands.

## File overlap analysis

Run via `./.claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh GAP-117 GAP-118 GAP-119`. Script reported HARD on `documents/05-guides/` (directory level — false positive: §Related citations, not file edits) and SOFT on the audit-doc citation (read-only references). Coordinator-reviewed real matrix:

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `documents/05-guides/restore-procedure.md` (NEW) | A only | None |
| `scripts/verify-restore.sh` (NEW) | A only | None |
| `.github/workflows/restore-drill.yml` (NEW) | A only | None |
| `infrastructure/terraform-aws/s3-ecr.tf` | B only | None |
| `infrastructure/terraform-oracle/main.tf` | B only | None |
| `kitehub/docker-compose.kitehub.yml` | B only (MinIO section + new setup container) | None |
| `documents/05-guides/disaster-recovery-plan.md` (NEW) | C only | None |
| `documents/05-guides/operations/dr-rto-rpo-matrix.md` (NEW) | C only | None |
| `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` | A+B+C cite | **SOFT** — read-only references in §Related |

Net: 0 HARD, 1 SOFT (citation only). Wave proceeds. Lesson for analyze-overlap.sh v1.1: filter §Related citations + treat dir-only mentions as informational (TODO already in `data/self-test-result.md`).

## Cross-gap coordination

GAP-117 documents the restore procedure; GAP-118 documents MinIO/S3 versioning; GAP-119 references both in RTO/RPO matrix. To avoid forward references:

- A's `restore-procedure.md` may stub-reference `disaster-recovery-plan.md` for "see also"
- B's Terraform changes documented inline + cross-linked from C's matrix
- C's matrix uses GAP-117/118 ACs as inputs; if A/B haven't merged at C's spawn time, C uses the gap files as authoritative spec
- All 3 cite the same audit-doc → SOFT only, integrator accepts

## Agent workflow

Per `feedback_parallel_agent_strategy.md`:

1. Each agent gets `isolation: "worktree"` (separate git checkout off main)
2. Branches off main AFTER this foundation PR merges
3. Branch naming: `feat/wave-dr-backup-gap-{117,118,119}-<slug>`
4. Each agent commits + pushes + creates own PR + reports PR number
5. Coordinator merges sequentially: **C → A → B**
   - C first (pure docs, no test deps, lowest risk)
   - A second (script + CI workflow validates against running DB; needs B's S3 versioning eventually but Phase 1 = procedure doc only, doesn't depend on B)
   - B last (Terraform changes — most reviewable surface, lands after foundations are documented)
6. SOFT conflict on audit-doc → integrator (me) merges latest cite-block, no manual resolution needed
7. Wave closure ROADMAP entry after all 3 merge

## Acceptance criteria (wave-level)

- [ ] 3 PRs merged (one per gap) with green CI
- [ ] All 3 gap files transitioned 🔵 OPEN → 🟢 DONE per `gap-done-discipline.md` §2 (every AC checked, no banned phrases in DONE-flip Log)
  - GAP-117: Phase 1+2 ACs done; Phase 3 (quarterly DR exercise) explicitly deferred + tracked separately = 🟡 PARTIAL acceptable per `gap-done-discipline.md` §3
  - GAP-118: All ACs done OR PARTIAL with cost/replication review left for stakeholder approval
  - GAP-119: All ACs done (pure markdown deliverable)
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry (counts 100 → 97-98)
- [ ] ROADMAP §"Active wave queue" — DR/Backup row marked SHIPPED, queue rotated
- [ ] No conflicts left unresolved on main
- [ ] Worktrees + branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6
- [ ] `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` entry appended (Wave 2 of cluster pipeline)
- [ ] Lessons-learned section below filled (first real-world validation = high-value data)

## Wall-clock target

- Foundation PR (this doc + ROADMAP + Day 2 backlog cleanup): ~15 min
- 3 parallel agents: ~25-35 min wall (B is heaviest — Terraform research; A is medium — needs Postgres restore knowledge; C is lightest — markdown synthesis)
- Sequential merge + SOFT conflict resolution: ~15 min
- Closure (ROADMAP + cleanup + retrospective fill): ~10 min
- **Total wave: ~65-75 min** (matches Wave Obs benchmark)

## Lessons-learned (Wave DR/Backup, to be filled after merge)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold?
- [ ] Cross-contamination details if any:

### File-overlap accuracy (analyze-overlap.sh v1.0 calibration)
- [ ] Predicted SOFT: ops-readiness-audit-2026-04-19.md citation; actual:
- [ ] Predicted HARD: 0 (after coordinator review of script's directory-level false positive); actual:
- [ ] Unpredicted conflicts:
- [ ] Improvement notes for analyze-overlap.sh v1.1:

### Wall-clock
- [ ] Estimated: 65-75 min; actual:; variance source:

### Agent prompt quality (first real-world test of `assets/agents/*` templates)
- [ ] Clarification rounds: A=, B=, C=
- [ ] Template updates needed: which template, what change
- [ ] Did `feature-tdd-agent.md` cover Terraform/CI workflow case adequately?
- [ ] Did `docs-only-agent.md` cover RTO/RPO matrix synthesis case adequately?

### Token cost
- [ ] Total tokens:; per gap:

### Cleanup
- [ ] Worktrees removed
- [ ] Local branches deleted
- [ ] Remote branches deleted

### Novel patterns
- [ ] New memory entry filed?
- [ ] Rule update proposed?

## Log

- 2026-04-28 — Wave plan created. Bundles also: ROADMAP §"Day 2 framework deliverable" → SHIPPED (PR #630 closed it 2026-04-28); `data/wave-history.jsonl` entry for Wave Meta-Day-2 (1 wave-of-1 single-PR self-validated). After foundation PR merges, 3 agents spawn from main.
