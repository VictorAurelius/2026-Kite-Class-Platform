# Post-Merge Sync Completeness — every status flip syncs 4 targets

**Priority:** 🟠 MANDATORY — sync-completeness governance
**Version:** 1.0.0
**Created:** 2026-05-12
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every PR that flips a `documents/04-quality/gaps/GAP-*.md` Status field, closes wave-scoped work, or adds a new memory entry under `~/.claude/projects/.../memory/`

---

## 1. The Rule

> **When a PR changes gap state, closes wave work, or introduces a new memory entry, the same PR MUST sync ALL FOUR canonical targets so they reflect the same reality.**

Existing rules cover the targets individually but no single rule enumerates them together. Wave 64 session-close audit (2026-05-12) found 4 sync misses recurring across one session — a systemic governance gap, not isolated mistakes. This rule consolidates the 4 sync targets, the matching detector (Rule 17), and the override mechanism.

---

## 2. The four sync targets

| # | Target | When sync required | Canonical source per |
|---|--------|-------------------|----------------------|
| 1 | `documents/04-quality/gaps/gap-status.csv` | PR flips Status / Priority / completion_pct on any `GAP-*.md` | `gap-architecture-v2.md` (CSV is canonical for status fields) |
| 2 | `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action | PR closes / re-prioritises / queues work that ROADMAP currently references | `feedback_post_merge_doc_sync.md` + `audit-to-gap-pipeline.md` Step 5 |
| 3 | `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` | Wave plan flips `status: complete` (per Rule 15) OR wave-scoped overflow work outside a wave plan files a gap | `feedback_wave_history_append_required.md` + `wave-pack-planner` SKILL.md §Rules |
| 4 | In-repo memory mirror (if introduced — see §5) OR user-memory `MEMORY.md` index | A new `feedback_*.md` / `project_*.md` memory entry is created | `incident-to-rule-pipeline.md` Stage 5 |

A PR is **complete** when it ships every target affected by its scope. A PR that flips a Status without updating the CSV row is incomplete — even if the markdown frontmatter is correct.

---

## 3. Why this rule exists

Wave 64 session-close audit (2026-05-12) caught four sync misses in ONE session:

| # | Miss | Sync target | Detection |
|---|------|-------------|-----------|
| 1 | GAP-482 status OPEN→PARTIAL but CSV row not updated | `gap-status.csv` | User nudge |
| 2 | Cutover work missing from ROADMAP §🚀 | `ROADMAP.md` | User nudge |
| 3 | Wave 64 cutover phase not in wave-history.jsonl | `wave-history.jsonl` | Coordinator self-catch |
| 4 | `feedback_pre_mutation_state_check.md` not indexed in MEMORY.md | `MEMORY.md` | Coordinator self-catch |

Existing rules covered PARTS but no rule enumerated ALL FOUR in one place, so the per-PR mental checklist defaulted to "did I update the markdown" rather than "did I sync every canonical store". After 4 misses in one session, the pattern is governance, not human error.

---

## 4. Decision flow per PR

Before merging, walk through this matrix:

```
1. Does the diff touch documents/04-quality/gaps/GAP-*.md?
   ├─ If a +**Status:** line is added → MUST also update gap-status.csv row (target 1)
   └─ If diff changes Priority / Domain / Phase → MUST also update CSV row (target 1)

2. Does the gap closure / re-prioritisation invalidate a ROADMAP §🚀 entry?
   ├─ YES → MUST update ROADMAP.md in same PR (target 2)
   └─ NO  → skip target 2 (e.g., low-priority gap not on §🚀 radar)

3. Did this PR close a wave plan (status: complete flip) OR ship wave-scoped
   overflow work outside a wave plan (e.g., a cutover step bigger than its gap)?
   ├─ YES → MUST append a wave-history.jsonl entry (target 3)
   └─ NO  → skip target 3

4. Did this PR introduce a new memory entry under the user-memory dir?
   ├─ YES → MUST add an index entry in MEMORY.md (target 4)
   └─ NO  → skip target 4
```

Targets 2 and 3 are PR-scope-dependent (skip if irrelevant). Target 1 fires every time Status flips. Target 4 fires every time a new memory file is added.

---

## 5. Memory mirror scope clarification

The user-memory directory `~/.claude/projects/.../memory/MEMORY.md` lives OUTSIDE the repo. The repo CANNOT enforce sync there via CI — it can only enforce sync if the project keeps an **in-repo mirror** of new memory entries.

**Current decision (this PR):** in-repo memory mirror is **out of scope**.

Reason: this repo does not maintain an in-repo copy of `feedback_*.md` / `project_*.md` files. The canonical MEMORY.md sits at `~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/MEMORY.md`. Files there are written by Claude across sessions; the repo only sees them when the user manually copies an excerpt into a rule or skill.

**Enforcement for target 4 therefore relies on:**
- Reviewer manual check ("does this rule cite a new memory entry? Was MEMORY.md auto-load also updated?")
- Author self-discipline per `incident-to-rule-pipeline.md` Stage 5
- The PR description includes the memory entry text inline (so the user can copy-paste into user-memory dir) — see §7.5 PR template handling

**Future scope (deferred follow-up gap):** if the project adopts an in-repo memory mirror (e.g., `documents/09-memory/`), Rule 18 in `check-docs.sh` would gain detection logic. Until then, target 4 is enforced by reviewer + author manual.

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Flip `**Status:** 🟢 DONE` in markdown without updating CSV row | Edit both in same PR; CSV is canonical |
| "ROADMAP update can wait for next session" | Update ROADMAP in same PR that closes the work it references |
| Skip wave-history append "because the wave isn't fully closed yet" | If your PR ships wave-scoped overflow work, append a phase-event entry, not just wait for closure |
| Save new memory file but forget MEMORY.md index | Always update index in same session; the index IS the entry point |
| Treat 4 targets as 4 separate tasks for 4 future PRs | One PR, one scope, all 4 sync targets updated |
| Override silently — flip Status, skip CSV, hope CI doesn't notice | Use `POST_MERGE_SYNC_OVERRIDE:` trailer with reason + follow-up gap link |

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Rule 17 detector (paired same PR — `session-docs-check`)

`scripts/check-docs.sh` adds Rule 17: when the diff contains a line `+**Status:**` on any `documents/04-quality/gaps/GAP-*.md` file, the diff MUST also touch `documents/04-quality/gaps/gap-status.csv`. Failure → FAIL in strict mode, WARN otherwise.

**Override trailer:** `POST_MERGE_SYNC_OVERRIDE: GAP-NNN — <reason>` in commit body between `BASE_REF..HEAD` downgrades FAIL → WARN.

Self-test fixtures: 3 cases under `test/fixtures/post-merge-sync/`:
- `good-status-flip-with-csv-sync` → PASS
- `bad-status-flip-no-csv-sync` → FAIL
- `bad-status-flip-no-csv-sync-with-override` → WARN

### 7.2 Rule 18 — memory mirror (PARTIAL, see §5)

Out of scope for this PR — repo currently has no in-repo memory mirror. Tracked as deferred follow-up if the project later adopts `documents/09-memory/`. Enforcement currently = reviewer-checklist + PR description embedding of new memory entry text.

### 7.3 PR template checkbox

`.github/PULL_REQUEST_TEMPLATE.md` Output Review section adds:

> - [ ] **Post-merge sync (4 targets)** — per `.claude/rules/post-merge-sync-completeness.md` §2, if PR changes gap Status / closes wave-scoped work / adds new memory entry: (1) `gap-status.csv` row updated; (2) `ROADMAP.md` §🚀 reflects current state; (3) `wave-history.jsonl` appended if wave-scoped; (4) MEMORY.md index updated if new memory entry. Override trailer: `POST_MERGE_SYNC_OVERRIDE: <target> — <reason + follow-up gap>`

### 7.4 Reviewer-checklist

When reviewing a PR that flips gap Status / closes wave work, reviewer walks §4 decision flow and confirms each applicable target was synced. Pattern of misses → file follow-up gap referencing this rule.

### 7.5 PR description embedding for memory entries

When a PR's content motivates a new memory entry (per `incident-to-rule-pipeline.md` Stage 5), the PR description MUST include the memory entry text inline under a `## Memory entry (copy to user-memory)` heading, so the user can copy-paste into `~/.claude/projects/.../memory/feedback_*.md` and update MEMORY.md index. The text counts as the "memory mirror" until §5 in-repo mirror lands.

### 7.6 Override mechanism

Genuine exception (e.g., emergency hotfix, regulator deadline):

```
git commit -m "...
POST_MERGE_SYNC_OVERRIDE: <target>(s) — <reason — e.g. P0 incident, CSV row update in follow-up PR>
POST_MERGE_SYNC_FOLLOWUP: <follow-up gap link with explicit completion date>"
```

Trailer logged in quarterly retro. Pattern frequency >5% triggers meta-review of taxonomy.

---

## 8. Self-test (worked example — Wave 64 session-close incident)

**Scenario:** 2026-05-12 Wave 64 session close. Coordinator shipped 5 PRs flipping gap statuses; user audit found 4 sync misses.

**Apply §4 decision flow retroactively to the GAP-482 status flip:**

1. Does diff touch a `GAP-*.md` file? ✅ YES — `GAP-482-...md` with `+**Status:** 🟡 PARTIAL` line.
2. Therefore target 1 (CSV row update) is REQUIRED.
3. Actual PR: CSV row NOT updated.

→ Rule 17 would have caught this at PR-review time: FAIL "Rule 17 — GAP-482 status flipped but gap-status.csv row not updated in same diff."

Counterfactual: with Rule 17 active at the time, the coordinator either updates the CSV row or commits `POST_MERGE_SYNC_OVERRIDE:` trailer. User retro for 4-miss pattern eliminated.

**Verdict:** rule fires correctly on the originating incident. Self-test PASS ✅.

---

## 9. Relationship to other rules

- **`gap-architecture-v2.md`** §3 — CSV is canonical for status fields; this rule enforces that CSV row update IS part of every status flip.
- **`feedback_post_merge_doc_sync.md`** (memory) — covers ROADMAP + gap Log sync; this rule extends and enumerates all 4 targets.
- **`feedback_wave_history_append_required.md`** (memory) + Rule 15 — covers wave-history.jsonl append; this rule adds wave-history to the 4-target enumeration.
- **`incident-to-rule-pipeline.md`** Stage 5 — every miss-driven memory entry must update MEMORY.md index; this rule formalises that step as target 4 and ships the §7.5 PR-description embedding mechanism.
- **`gap-done-discipline.md`** §2 (Rule 13) — DONE flip has its own completeness criteria (AC checked, no banned phrases, follow-up filed); Rule 17 adds the CSV sync layer ALONGSIDE Rule 13 (both fire on the same diff but check different invariants).
- **`audit-to-gap-pipeline.md`** Step 5 — every new gap must be in ROADMAP; this rule extends the same discipline to status updates on existing gaps.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — this rule + Rule 17 detector + fixtures + PR-template + worked self-test all ship same PR.
- **`docs-only-pr-auto-merge.md`** — gap-status.csv + ROADMAP.md + memory entries are all docs-only → auto-merge eligible once Rule 17 passes.

---

## 10. Log

- **2026-05-12 (v1.0.0):** Rule created. Triggered by Wave 64 session-close audit 2026-05-12 (user-flagged 4 sync misses in ONE session: GAP-482 status flip without CSV sync, cutover missing from ROADMAP §🚀, Wave 64 not in wave-history.jsonl, `feedback_pre_mutation_state_check.md` not indexed in MEMORY.md). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged systemic 4-miss pattern in single session) → Classify ✓ (no existing rule enumerated all 4 sync targets; `feedback_post_merge_doc_sync.md` covered ROADMAP slice, `gap-architecture-v2.md` covered CSV slice, `feedback_wave_history_append_required.md` covered wave-history slice — none combined into per-PR checklist) → Rule+Enforce ✓ (this rule + `session-docs-check` Rule 17 detector + 3 fixtures + PR-template Output Review row + reviewer-checklist all paired same-PR per `rule-change-process.md` §6.5; Rule 18 memory mirror deferred per §5 scope clarification) → Self-Test ✓ (§8 worked example on Wave 64 GAP-482 incident — Rule 17 fires correctly; counterfactual: 4 misses eliminated) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — extends existing per-target rules into single enforced checklist; no constraint loosening; existing PRs grandfathered, rule applies prospectively from next session).
