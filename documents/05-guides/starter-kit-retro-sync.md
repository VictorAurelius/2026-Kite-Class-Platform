# Starter-Kit Retro-Sync Runbook

> Last updated: 2026-04-20 (GAP-195 Phase 1) | Owner: Tech Lead
> Companion script: [`../../scripts/starter-kit-diff.sh`](../../scripts/starter-kit-diff.sh)

Procedure for the **quarterly bulk retro-sync** of `.claude/rules/` + `.claude/skills/` between this project and the remote canonical starter-kit (`github.com/VictorAurelius/claude-starter-kit`).

Complements per-change sync already defined in [`.claude/rules/skill-conventions.md §Remote Repo Sync`](../../.claude/rules/skill-conventions.md). That rule handles each individual PR; this runbook handles the accumulated delta between quarterly snapshots.

---

## 1. When to Run

**Cadence:** once per quarter (e.g. end of Q1, Q2, Q3, Q4) OR after a wave burst that added 5+ new rules/skills.

**Conditions:**
- Main branch is green (CI clean)
- No audit suite stale (per `post-wave-audit-mandate.md`)
- ≥1 day between merging wave and running sync (avoid churn on in-flight work)

---

## 2. Tools

### 2.1 `scripts/starter-kit-diff.sh`

Clones remote to `/tmp/kit`, diffs `.claude/rules/` + `.claude/skills/`, classifies output into 4 buckets:
- 🆕 **NEW (local)** — local file missing on remote → PR candidate
- 🆕 **NEW (remote)** — remote has something local doesn't → import candidate
- ✏️  **MODIFIED** — both sides present but differ → merge decision
- 🔒 **PROJECT-SPECIFIC** — local only, but heuristically detected as project-specific → skip sync

**Usage:**
```bash
./scripts/starter-kit-diff.sh                          # full report to stdout
./scripts/starter-kit-diff.sh --output /tmp/diff.md    # to file
./scripts/starter-kit-diff.sh --category rules         # only rules
./scripts/starter-kit-diff.sh --category skills        # only skills
./scripts/starter-kit-diff.sh --clean                  # fresh clone
```

Script heuristic for "project-specific": filename contains `kitehub`/`kiteclass`/`ai-branding` OR first 30 lines mention those markers. If the heuristic is wrong, manually reclassify during triage.

### 2.2 Remote repo credentials
```bash
gh auth status                                         # ensure gh CLI logged in
git remote -v                                          # verify starter-kit remote
```

---

## 3. Triage Checklist

Run diff → for each item, decide:

### 3.1 Rules (`.claude/rules/`)
For each 🆕 NEW (local):
- [ ] **Generic?** — Does this rule apply to ANY project using the kit? If yes → sync. If no → keep local-only.
- [ ] **Battle-tested?** — Has it been used ≥5 times without major revision?
- [ ] **Self-contained?** — No dependencies on project-specific skills/scripts?
- [ ] **No project names in body?** — Rewrite project-specific examples to placeholders (`my-service`, `SERVICE_A`, etc.)

Pass all 4 → include in remote PR. Fail any → document in local decision log; do not sync.

### 3.2 Skills (`.claude/skills/`)
For each 🆕 NEW (local):
- [ ] **Generic?** — Does the trigger match projects outside this one?
- [ ] **Adaptable?** — Are project-specific hooks (paths, config) parameterized?
- [ ] **Battle-tested?** — Used ≥3 times successfully?
- [ ] **Contains project data?** — Scrub: remove `rules.md`/`use-cases.md` references to business domains unless pattern is generic

Pass all 4 → include. Fail any → keep local-only.

### 3.3 Modified (both sides)
For each ✏️  MODIFIED:
- [ ] **Which version is newer by intent?** — Check git log on both sides for latest edit date + reason
- [ ] **Is remote change restrictive or permissive?** — Never silently relax remote safety rules
- [ ] **Three-way merge candidate?** — If both evolved independently, hand-merge

Do not auto-apply. Always inspect `diff -u` before committing either side.

### 3.4 Remote-only (🆕 remote)
For each item local doesn't have:
- [ ] **Adopt in project?** — If yes, copy to `.claude/starter-kit/` (local kit mirror) or adapt into project
- [ ] **Version-mismatch?** — Check `REMOTE_VERSION` in diff header; if remote leads by >2 minors, sync first before making more local changes

---

## 4. Semver + Version Bump Rules

Applies to both local `.claude/starter-kit/VERSION` AND remote `VERSION`. Per `.claude/rules/skill-conventions.md §Starter-Kit Version Management`:

| Change type | Bump | Example |
|-------------|:----:|---------|
| Remove or restructure existing skill/script/rule | **MAJOR** | 2.0.0 |
| Add new skill, script, or rule | **MINOR** | 1.4.0 |
| Fix content / improve wording in existing | **PATCH** | 1.4.1 |

**During retro-sync:**
- Group additions → one MINOR bump covers all of them (not one per file)
- If retro-sync removes/restructures anything → MAJOR bump
- Both `VERSION` and `CHANGELOG.md` on remote must be updated atomically

---

## 5. Changelog Format

Remote `CHANGELOG.md` — newest-first. Every retro-sync PR appends one entry:

```markdown
## [1.4.0] — 2026-04-20

### Added (from Kite project retro-sync)
- `.claude/rules/meta-gap-priority.md` — gap priority rule (3-tier: meta/business/feature)
- `.claude/rules/post-wave-audit-mandate.md` — audit cadence enforcement
- `.claude/skills/quality/ui-review/` — /128 per-screen UI audit
- `.claude/skills/quality/business-logic-audit/` — /100 code↔rules.md verification

### Fixed
- `.claude/rules/skill-conventions.md` — expanded gotchas from UI audit learnings

### Source
Retro-synced from project `kite-class-platform` (wave 8b; commits abc123..def456).
```

---

## 6. Bulk PR Process (remote side)

One PR per category keeps review tractable:
1. **PR 1: Rules** — all new/modified `.claude/rules/*.md` files
2. **PR 2: Skills** — all new/modified `.claude/skills/**/*.md` + scripts
3. **PR 3: Gotchas** — updates to existing rules' Gotchas sections + skill-conventions

Each PR:
- Title: `chore(retro-sync): Q{N} {YYYY} — {category}`
- Body: enumerate items added/modified; link back to this project + wave/commit range
- Bump VERSION + CHANGELOG in same PR
- Request review from starter-kit maintainer

### 6.1 Template for remote PR body

```markdown
## Summary

Retro-sync from [kite-class-platform](https://github.com/VictorAurelius/2026-Kite-Class-Platform)
through commits {sha-range}. Adds {N} items, modifies {M}.

## Added

- [List each file + 1-line purpose]

## Modified

- [Each with rationale]

## Source commits

- Project wave 8b: {PR URLs}

## Version

`X.Y.Z` → `X.Y+1.0` (MINOR — new items, no breaking)
```

---

## 7. Local-Side Updates

After remote PR merged:
- [ ] Update local `.claude/starter-kit/VERSION` to match remote
- [ ] Update local `.claude/starter-kit/CHANGELOG.md` with same entry
- [ ] Commit in this project as `chore(starter-kit): sync to v{X.Y.Z}`

---

## 8. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Dump every local rule to remote | Triage 4-question checklist (§3) |
| Include project-specific skills (kitehub-branding, etc.) | Omit; keep local-only |
| Bump version per-file | Group into one MINOR per retro-sync |
| Force-push to remote main | Open PR; await review |
| Retro-sync when CI red or audits stale | Wait for steady state |
| Silently accept remote changes | Three-way merge modified items |

---

## 9. First Retro-Sync (Phase 2 in flight)

Phase split (added 2026-04-29):

- **Phase 1 (DONE 2026-04-20):** tooling (`scripts/starter-kit-diff.sh`) + this runbook.
- **Phase 2a (DONE 2026-04-29):** triage report — see `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md`. Identified 110 NEW (local) candidates / 48 PROJECT-SPECIFIC / 0 MODIFIED / 0 NEW (remote). **Top 9 rules selected for first upstream PR** (rules-only, conservative scope per §6 "one PR per category"). Skills batches deferred.
- **Phase 2b (TRACKED in [GAP-262](../04-quality/gaps/GAP-262-starter-kit-upstream-retro-sync-pr.md)):** open the actual upstream PR(s) on `VictorAurelius/claude-starter-kit`, bump remote VERSION 2.2.0 → 2.3.0, add CHANGELOG entry, sync local mirror back. Blocked on user decisions for triage report §4 open questions Q1–Q5.

Original expected scope (waves 1-8 candidates):
- New rules: `meta-gap-priority`, `post-wave-audit-mandate`, `audit-to-gap-pipeline`, `docs-folder-structure`, `planning-docs-structure`, `mcp-first-with-fallback`, `design-patterns`, `output-review-mandate`
- New skills: quality audit suite (ui-review, business-logic-audit, ops-readiness-audit, performance-audit, api-contract-audit, security-audit)
- Skill-conventions gotchas section expansion
- Possibly 3 separate remote PRs

Triage report §3 confirmed several of the above are correctly classified PROJECT-SPECIFIC (not upstream candidates after all): `planning-docs-structure`, `post-wave-audit-mandate`, `design-patterns` cite project-specific paths and stay local. The Top-N is narrower and tighter as a result.

---

## 10. Related

- [`.claude/rules/skill-conventions.md`](../../.claude/rules/skill-conventions.md) §Remote Repo Sync + §Starter-Kit Version Management
- [`scripts/starter-kit-diff.sh`](../../scripts/starter-kit-diff.sh)
- [`../04-quality/gaps/GAP-195-starter-kit-bulk-retro-sync.md`](../04-quality/gaps/GAP-195-starter-kit-bulk-retro-sync.md)

---

## 11. Log

- **2026-04-29:** Phase 2a triage executed (Wave Meta Phase-2 Cleanup Cluster 7 Agent C). Diff script ran clean against remote v2.2.0; produced `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md` with 9-rule Top-N + 5 user open questions. Phase 2b (cross-repo upstream PR) tracked in [GAP-262](../04-quality/gaps/GAP-262-starter-kit-upstream-retro-sync-pr.md). §9 updated to reflect phase split. GAP-195 stays 🟡 PARTIAL until GAP-262 closes.
- **2026-04-20:** Created (GAP-195 Phase 1). First sync deferred to Phase 2.
