# GAP-262: Starter-Kit Upstream Retro-Sync PR (Phase 2b)

**Status:** 🟡 PARTIAL — PR 1 (rules) MERGED upstream + local mirror SYNCED. PR 2 (skills:core+workflow → v2.4.0) + PR 3 (skills:quality+reference → v2.5.0) deferred per Q3=C incremental decision.
**Priority:** 🟡 P2 Meta
**Domain:** Meta / Cross-repo / Starter-Kit
**Found:** 2026-04-29 (Wave Meta Phase-2 Cleanup, Cluster 7 Agent C — GAP-195 Phase 2a triage report)
**Affects:** `github.com/VictorAurelius/claude-starter-kit` (canonical kit) + downstream projects forking the kit

---

## Problem

GAP-195 Phase 1 (2026-04-20) shipped the diff tooling (`scripts/starter-kit-diff.sh`) and runbook (`documents/05-guides/contributing/starter-kit-retro-sync.md`). Phase 2a (2026-04-29, this wave) ran the triage and produced `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md` identifying 110 NEW (local) candidates of which **9 rules pass the 4-question triage checklist with no scrubbing required (or only light scrubbing)**.

What remains is **Phase 2b — the actual cross-repo upstream PR work**. This is decoupled from Phase 2a because:

1. Triage is an in-repo activity (this project's reviewers can verify).
2. Upstream PR work touches a separate repo (`VictorAurelius/claude-starter-kit`) with its own review cycle, semver discipline, CHANGELOG format, and VERSION sync.
3. User decisions on §4 open questions (Q1–Q5 in triage report) need to land before Phase 2b can ship.

Without Phase 2b, the 9 generic rules + ~85 generic skills built up over 8+ waves stay locked in this project. Future projects forking the kit miss the meta-governance backbone that took weeks of incidents to evolve.

---

## Proposed Fix

### Step 1 — Land Phase 2a (this PR — `feat/wave-meta-p2-gap-195a-starter-kit-triage`)
Triage report + this gap file shipped. User reviews open questions Q1–Q5.

### Step 2 — User decisions captured
User answers Q1 (marketing/legal localization), Q2 (skills-index pattern), Q3 (PR split), Q4 (local mirror), Q5 (cadence). Recorded in this gap's Log section.

### Step 3 — Open PR 1 on remote starter-kit (rules-only)
```bash
git clone git@github.com:VictorAurelius/claude-starter-kit.git /tmp/kit-pr1
cd /tmp/kit-pr1
git checkout -b retro-sync/2026-Q2-rules
# Copy 9 rules from triage §2 Top-N (with scrubbing per §3.3 + §4 decisions)
# Bump VERSION 2.2.0 → 2.3.0
# Append CHANGELOG.md entry per runbook §5 format
# Push + open PR with title: "chore(retro-sync): Q2 2026 — rules (9 items)"
```

### Step 4 — Sync local mirror after upstream PR merges
Per Q4 decision (default A), create `.claude/starter-kit/` local mirror at v2.3.0 so future per-change syncs (per `skill-conventions.md §Remote Repo Sync`) have a target.

### Step 5 — Optionally schedule PR 2 (skills batch — core/workflow) and PR 3 (quality/reference)
Deferred to a follow-up gap if scope grows. Not blocking GAP-262.

---

## Acceptance Criteria

### Cross-repo upstream work (PR 1: rules)
- [ ] User decisions captured for triage report Q1–Q5 (recorded in this gap's Log)
- [ ] PR 1 opened on `VictorAurelius/claude-starter-kit` with the 9 rules from triage §2 Top-N (with scrubbing per §3.3 + Q-decisions)
- [ ] PR 1 includes `VERSION` bump 2.2.0 → 2.3.0 (MINOR per `.claude/rules/skill-conventions.md §Starter-Kit Version Management`)
- [ ] PR 1 includes `CHANGELOG.md` entry per `documents/05-guides/contributing/starter-kit-retro-sync.md §5` format
- [ ] PR 1 reviewed and merged on remote
- [ ] Sync confirmation: PR # + merge SHA recorded in this gap's Log

### Local-side reconciliation
- [ ] Local `.claude/starter-kit/` mirror created at v2.3.0 (per Q4 decision; if user picks B/C, document the deviation here)
- [ ] Local `.claude/starter-kit/VERSION` matches remote
- [ ] Local `.claude/starter-kit/CHANGELOG.md` matches remote v2.3.0 entry
- [ ] Project commit `chore(starter-kit): sync to v2.3.0` lands on main

### Documentation closure
- [ ] Triage report `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md` Log section updated with PR 1 outcome
- [ ] GAP-195 flipped to 🟢 DONE with explicit PR 1 reference + sync SHA + GAP-262 closing reference

---

## Out-of-scope

- **PR 2 (skills:core+workflow batches)** — separate gap if scope grows; not blocking GAP-262 closure
- **PR 3 (skills:quality+reference batches)** — same
- **Localization rewrites of marketing-legal-review skill** for non-Vietnamese jurisdictions — Q1 decision may defer this entirely
- **Re-triage of false-negative project-specific items** flagged in triage §3.3 — those candidates can be re-evaluated when their respective PRs are scoped
- **Modifying any local `.claude/rules/*.md` or `.claude/skills/**`** in this gap's PR — those are upstream import candidates only; local content is the source

---

## Related

- **Parent gap:** [GAP-195](GAP-195-starter-kit-bulk-retro-sync.md) — bulk retro-sync; this gap covers Phase 2b specifically
- **Triage report:** `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md`
- **Runbook:** `documents/05-guides/contributing/starter-kit-retro-sync.md`
- **Diff script:** `scripts/starter-kit-diff.sh` (GAP-195 Phase 1)
- **Sync rule:** `.claude/rules/skill-conventions.md §Remote Repo Sync` + `§Starter-Kit Version Management`
- **Priority rule:** `.claude/rules/meta-gap-priority.md` §3 (Meta-P2 — force-multiplier for downstream projects)
- **Output review mandate:** `.claude/rules/output-review-mandate.md` (the 9 selected rules in triage Top-N inherit this mandate)

---

## Log


- 2026-06-14: phase re-triage — n/a→phase-2 (starter-kit upstream retro-sync PR Phase 2b; meta dev-tooling).
- **2026-04-29 (Phase 2b PR 1 MERGED UPSTREAM + LOCAL MIRROR SYNCED):** Upstream PR `claude-starter-kit#10` MERGED (squash). 9 rules shipped at v2.3.0 (VERSION 2.2.0 → 2.3.0; CHANGELOG entry per runbook §5; 3 sources synchronized). Local mirror created at `.claude/starter-kit/` per Q4=A decision: VERSION + CHANGELOG.md + README.md + rules/{9 files} pulled from upstream v2.3.0. Local pointer `.claude/.starter-kit-version` 2.1.0 → 2.3.0. Status flipped 🔵 OPEN → 🟡 PARTIAL — PR 1 ACs all satisfied; gap stays PARTIAL because PR 2 (skills:core+workflow → v2.4.0) + PR 3 (skills:quality+reference → v2.5.0) deferred per Q3=C incremental decision and Q5 staged MINORs cadence. Future PR 2/3 work tracked in this gap until DONE.
- **2026-04-29 (Phase 2b PR 1 OPENED):** User decisions captured: Q1=B (generic checklist with TODO markers, applies to PR 2/3) · Q2=A (generic skills-index template, applies to PR 2/3) · Q3=C (incremental — PR 1 rules-only first) · Q4=A (create local mirror after upstream merges) · Q5=staged MINORs (v2.3.0 rules → v2.4.0 skills:core+workflow → v2.5.0 skills:quality+reference). PR 1 opened: https://github.com/VictorAurelius/claude-starter-kit/pull/10. Awaiting maintainer review + merge. Status stays 🔵 OPEN until PR 1 merges + local mirror sync (Step 4 of §Proposed Fix) lands.
- **2026-04-29:** Filed as Wave Meta Phase-2 Cleanup Cluster 7 Agent C deliverable. Branch `feat/wave-meta-p2-gap-195a-starter-kit-triage` shipped triage report + this gap. Status 🔵 OPEN until user decisions on Q1–Q5 land + PR 1 opens upstream.
