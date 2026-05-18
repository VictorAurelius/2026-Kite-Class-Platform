---
title: Wave 96 — Gap folder reorg per user inside-out proposal (phase subdirs + creation-time enforcement) — STUB
status: draft
created: 2026-05-18
updated: 2026-05-18
waves: [96]
gaps: [GAP-645]
---

# Wave 96 — Gap folder reorg (STUB plan)

**Goal:** Reorganize `documents/04-quality/gaps/` folder per user inside-out proposal 2026-05-18 — addresses Rule 3 active gap cap violation (364 / 200 = 182%) via phase subdirs + creation-time enforcement.

**Trigger:** User proposal 2026-05-18 same session với Wave 94c audit suite. Wave 96 = full execution; this file = STUB documenting scope + outside-in audit mandate.

**Status:** `draft` — not yet executable. Defer trigger: Phase 1 BETA gate close (GAP-619 ✅ + GAP-612 AWS restore + score ≥80) + GAP-637 P0 admin auth fix.

**Estimated wall-clock:** ~5h (1 medium session).

---

## 1. Brainstorm (5-10 min)

### Q1 — Alignment
- Phase relevance: Meta-governance (cross-phase force-multiplier per `meta-gap-priority.md` §3)
- Personas served: dev + Claude both struggle navigate 364-file flat folder
- Triggered by: Rule 3 `docs-folder-volume-budget.md` cap violation (182% active)

### Q2 — Trade-offs (outside-in audit pending per `outside-in-coverage-trigger.md` v1.1.0)
| Alternative | Concern |
|---|---|
| Phase subdir only (no PARTIAL subdir) | Simpler taxonomy nhưng KHÔNG separate PARTIAL flag — meta-priority surface lost |
| Status subdir only (OPEN/PARTIAL/PENDING/PLANNED/closed) | Cartesian product với phase = N × M subdirs = complex |
| Hybrid phase + PARTIAL flag (user proposal) | Aligns Rule 3 §4 + addresses meta-priority surface |
| KHÔNG reorg | Continues Rule 3 violation; cognitive load grows |

### Q3 — Risks
| Risk | Mitigation |
|---|---|
| Drift between file path + CSV `phase` column | CI validator `check-gap-phase-classification.sh` |
| Cross-reference break trong rules/skills/ROADMAP | Grep audit + bulk Edit phase |
| Migration cost (364 file moves + CSV update) | Batch script `migrate-gaps-to-phase-subdirs.py` |

---

## 2. Task Breakdown

| Bucket | Owner | Effort | Status |
|--------|-------|--------|--------|
| A | Outside-in audit 3 agents (persona + benchmark + failure-mode) | ~30 min wall-clock parallel | ⏳ Wave 96 trigger |
| B | Migrate 364 active gaps → 6 phase subdirs | ~2h | ⏳ Wave 96 |
| C | CSV filename column update 364 rows | ~30 min batch script | ⏳ Wave 96 |
| D | Cross-reference patches | ~1h grep + Edit | ⏳ Wave 96 |
| E | New rule `gap-phase-classification-enforcement.md` v1.0.0 + CI validator | ~1h | ⏳ Wave 96 |
| F | Outside-in findings consolidation + follow-up gaps | ~30 min | ⏳ Wave 96 |

---

## 3. Scope (compact schema)

**Stake tier:** MEDIUM → outside-in audit MANDATE before lock per Rule v1.1.0
**Cross-layer:** NO → docs-only scope

### 3.1 Files created Wave 96

- `documents/04-quality/gaps/{phase-1-beta,phase-1.5-paid,phase-2,phase-3,partial,n/a}/README.md` × 6 subdirs
- `.claude/rules/gap-phase-classification-enforcement.md` v1.0.0
- `scripts/check-gap-phase-classification.sh` CI validator
- `scripts/migrate-gaps-to-phase-subdirs.py` batch migrator (one-time)
- `documents/04-quality/audits/persona-review/2026-05-NN-wave-96-gap-folder-reorg-outside-in.md` audit report (consolidated)
- Wave 96 closure plan (this file flipped status: complete)

### 3.2 Files moved Wave 96

- 364 active gap files → 6 phase subdirs

### 3.3 Files amended Wave 96

- `documents/04-quality/gaps/gap-status.csv` — 364 rows filename column update
- `documents/04-quality/gaps/ROADMAP.md` — cross-references update
- `.claude/rules/rules-index.csv` — +1 row for new rule
- `output-review-mandate.md` §3 — +1 row "Gap phase classification"

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Verification | Verdict |
|---|---|---|
| 6 phase subdirs to create | `ls documents/04-quality/gaps/` — chỉ có closed/ + pending/ pre-Wave-96 | 🆕 to-be-created Wave 96 |
| 364 active gap files at root | `find documents/04-quality/gaps -maxdepth 1 -type f -name 'GAP-*.md' \| wc -l` | ✅ 338 (CSV 364 = 26 closed/orphan delta) |
| Rule 3 cap violation | 182% (364/200) | ❌ over cap — Wave 96 trigger justified |
| CSV `phase` column canonical | per `gap-architecture-v2.md` §3 | ✅ source of truth |

---

## 5. Verification Gates (per bucket)

- [ ] Outside-in audit 3 agents complete + findings documented
- [ ] 364 files migrated successfully (zero data loss)
- [ ] CSV filename column 100% sync (CI validator PASS)
- [ ] Cross-references updated trong rules + skills + ROADMAP
- [ ] New rule `gap-phase-classification-enforcement.md` shipped với CI validator wired
- [ ] Pre-handoff self-test per `pre-handoff-self-test-completeness.md`

---

## 6. Agent Spawn Pattern

3 outside-in agents Bucket A (parallel background):
1. Persona audit — dev workflow walkthrough new gap creation/update/close
2. External benchmark — SaaS project gap-tracking taxonomy patterns
3. Failure-mode matrix — drift scenarios + CI validator coverage gaps

Spawn via `agent-background-spawn-default.md` pattern `run_in_background: true`.

---

## 7. Closure Protocol (template — populate at Wave 96 execution)

### 7.1 Scope-Completeness Reconciliation
(populate at closure per `wave-closure-scope-completeness.md` §3)

### 7.2 Post-wave cleanup
```bash
bash scripts/prune-merged-worktrees.sh --yes
```

### 7.3 Post-wave audit cadence
Meta-governance scope per `post-wave-audit-mandate.md` §2.4 registry — NO AUDIT REQUIRED.

---

## 8. Log

- **2026-05-18 (STUB draft):** Filed as Wave 96 plan stub per user inside-out proposal 2026-05-18. Status `draft` — not yet executable. Defer trigger: Phase 1 BETA gate close (GAP-619 ✅ + GAP-612 AWS restore + score ≥80) + GAP-637 P0 admin auth fix. Outside-in audit mandate per `outside-in-coverage-trigger.md` v1.1.0 §2 architecture-decision keywords ("subfolder organization", "meta enforcement at creation time"). Parent gap GAP-645. Wave 96 execution deferred next session post-prerequisites met.
