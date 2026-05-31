---
paths:
  - ".claude/rules/**"
  - "CLAUDE.md"
---

# Rule Change Process — ADR-Like Governance for `.claude/rules/**`

**Priority:** 🔴 CRITICAL — meta-governance for project DNA
**Version:** 1.1.5
**Created:** 2026-04-20
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.1.3 PATCH self-approve per §5; Wave 76 Bucket C adds §5.1 atomic-unique-bar checklist for new rules (grounded in ESLint rule guidelines per outside-in benchmark Wave 75) — clarification of existing review bar, not new constraint on prior rules. v1.1.2 (kept): Wave 76 Bucket D adds §3.5 Last-Reviewed staleness policy. v1.1.1 (kept): Wave 76 Bucket A added §6.1 Deprecation lifecycle. v1.1.0 (kept): MINOR self-approve per §5 — adds §6.5 Enforcement Parity Mandate paired with `incident-to-rule-pipeline.md` + `gap-done-discipline.md` in same PR, no constraint loosening)
**Supersedes:** ad-hoc rule edits (no prior formal process)
**Applies to:** Every `.md` file under `.claude/rules/` and every rule-like top-level file the team decides is meta-governance (CLAUDE.md §rules, `.claude/skills/_README-skills-index.md` conventions)

---

## 1. Purpose

Rules are project DNA. A single rule edit force-multiplies every future PR in its scope. Before 2026-04-20, rule changes landed via single-author commits with no review, no changelog, no frontmatter versioning — the irony of meta-governance without meta-review, surfaced by `output-review-mandate.md` §4 VIOLATION #2.

This document IS the process it describes (self-referential bootstrap — it ships with its own frontmatter populated).

---

## 2. The Rule

> **Every change to a `.claude/rules/*.md` file MUST:**
> 1. Bump `Version` (semver — see §4)
> 2. Update `Last-Reviewed` date
> 3. Append an entry to the rule's `## Log` section
> 4. Be reviewed by Lead + 1 dev (see §5)
> 5. Ship with measurable enforcement (see §6)

No rule merges without all five.

---

## 3. Required Frontmatter Fields

Every rule file MUST open with a header block containing these fields. Markdown-header style (used in existing rules) is acceptable — YAML frontmatter is NOT required because rules are docs humans read, not tools parse.

```markdown
# Rule Name — One-line purpose

**Priority:** 🔴 CRITICAL | 🟠 MANDATORY | 🟡 ADVISORY
**Version:** MAJOR.MINOR.PATCH
**Created:** YYYY-MM-DD
**Last-Reviewed:** YYYY-MM-DD
**Reviewer-Approver:** @github-handle (+ @secondary if 2-person review)
**Applies to:** {scope}
```

Optional fields:
- `**Supersedes:** <link>` — when replacing older rule
- `**Superseded-by:** <link>` — when this rule is superseded but kept for history
- `**Depends-on:** <link(s)>` — rules this one assumes

### Backfill policy

Existing rules without `Version` / `Last-Reviewed` / `Reviewer-Approver` fields: backfill on the NEXT edit to that file (not a mass migration). A dedicated tracking gap may be filed if needed.

### 3.5 Last-Reviewed staleness policy (added v1.1.1 — Wave 76 Bucket D)

`Last-Reviewed` field MUST be updated when:
- Rule content changes (any version bump — auto-paired with §7 Log entry)
- Rule's enforcement mechanism changes (hook, CI script, skill, PR template)
- During quarterly retro audit (forced review even without content change → PATCH bump)

**CI enforcement** (`scripts/check-rule-staleness.sh`, job `rule-staleness` in `quality-rules-skills.yml`):

| Age (days since Last-Reviewed) | Status | CI behavior |
|---|---|---|
| 0-59 | Fresh | PASS (silent) |
| 60-179 | WARN | Informational message in CI output; PR does not fail |
| ≥180 | FAIL | WARN-mode: reported but exit 0 (currently active); HARD STOP at FAIL after 30-day grace period from Wave 76 Bucket D merge — target re-enable date 2026-06-13 |
| Missing or invalid `Last-Reviewed` | FAIL-MISSING | Always blocking (exit 1) — overlaps with `check-rule-frontmatter.sh` |

**Stale rule action paths** (when WARN or FAIL surfaces):
1. Re-read rule + verify still applicable → bump `Last-Reviewed` to today + PATCH version + Log entry
2. Rule no longer applicable → deprecate per `rule-change-process.md` Open Items (future §6 deprecation lifecycle)
3. Rule needs major rewrite → MAJOR version bump, full §5 review

**Override mechanism** (rare exception — rule genuinely cannot be reviewed in current window):

```
git commit -m "...
RULE_STALENESS_OVERRIDE: <rule-slug> — <reason + follow-up review date>"
```

Trailer logged; pattern frequency >5% per quarter triggers meta-review of thresholds.

**Self-test:** Wave 76 Bucket D merge → script reports `55 fresh / 0 stale-WARN / 0 stale-FAIL / 0 missing-or-invalid` (all rules recently refreshed during Wave 73). Future sessions adding rules without timely re-review will surface WARN at 60 days.

---

## 4. Semver for Rules

| Change type | Bump | Example |
|-------------|------|---------|
| Remove an anti-pattern, loosen constraint, delete a §section, repeal a rule | MAJOR | 1.x.y → 2.0.0 |
| Add new §section, new anti-pattern, tighten constraint, add a required field | MINOR | 1.2.x → 1.3.0 |
| Clarification, typo, link fix, example reformat | PATCH | 1.2.3 → 1.2.4 |

**Hard rule:** any change that could BLOCK a PR that previously passed = MINOR or MAJOR, never PATCH.

---

## 5. Review Requirement

### 5.1 Atomic-unique rule bar (mandatory for new rules)

Before drafting a new rule, verify it passes:

- [ ] **Atomic concept** — rule has SINGLE responsibility (not "rule A and B and C"). If trying to enforce 2+ things, file 2 rules.
- [ ] **Unique scope** — rule's responsibility not duplicated by existing rule. Run `grep -ril "<key concept>" .claude/rules/` first.
- [ ] **Widely applicable** — anticipated trigger ≥3 distinct cases. If only 1 case, may not need a rule (use review checklist).
- [ ] **Body discipline** — §1 The Rule contains ≤2 "and" conjunctions. >2 = compound rule, split.

If ANY checkbox fails: don't file the rule. Either narrow scope, consolidate with existing rule, or use lighter mechanism (checklist / skill reference).

Source: ESLint rule guidelines (per outside-in benchmark Wave 75).

### 5.2 Reviewer matrix

| Change scope | Minimum reviewers |
|--------------|------------------|
| PATCH only | 1 dev (any team member) |
| MINOR | Lead + 1 dev |
| MAJOR | Lead + 2 devs + team consensus (post to team channel 24h before merge) |
| New rule file | Lead + 1 dev + explicit relationship entry in §Relationship section of each adjacent rule |

**Reviewer checks:**
1. Does the change conflict with any other rule in `.claude/rules/`?
2. Is enforcement concrete (hook, PR template, CI, human checklist)? If no enforcement, rule is advisory fiction.
3. Is it testable / auditable? Reviewer can simulate a PR that would hit this rule.
4. Log entry present and dated?
5. Version bumped correctly?
6. Every §Related rule (including `docs-folder-structure.md`, `skill-conventions.md`, `meta-gap-priority.md`, `audit-to-gap-pipeline.md`, `output-review-mandate.md`) reviewed for contradiction?

Use `.claude/skills/quality/rule-review/SKILL.md` for the full step-by-step.

---

## 6. Enforcement Clause (mandatory in the rule itself)

Every rule MUST include a §Enforcement (or §6 Enforcement) section that describes at least one of:

- **Pre-commit hook** — path + purpose
- **Pre-merge CI check** — workflow file + job name
- **PR template item** — reviewer must tick
- **Audit skill** — which audit category picks it up + frequency
- **Hook warn/block** — `audit-gate.py` condition

Rules without enforcement = advisory fiction and WILL be rejected. If enforcement is deferred, the rule ships with a **tracking gap** cited inline so the clock is on.

### 6.1 Deprecation lifecycle (added v1.1.1 — Wave 76 Bucket A)

Rules don't live forever. When a rule's scope becomes obsolete, contradicts a newer rule, or is superseded by an automated detection, it transitions through this lifecycle:

| Stage | `rules-index.csv` `lifecycle_status` | Allowed action | Timeline |
|-------|--------------------------------------|----------------|----------|
| **Active** | `active` (default for all new + existing rules) | Rule is enforced; PRs must comply | Indefinite |
| **Deprecated** | `deprecated` + `deprecated_at` ISO date | Rule body keeps `## 13. DEPRECATED` banner; new code MAY ignore it; reviewer manual no longer blocks | **60-day WARN window** between `deprecated_at` and removal |
| **Removed** | (row deleted from CSV; file moved to `.claude/rules/archived/` OR deleted entirely) | Rule no longer applies; replaced rule (if any) cited via `replaced_by` column at deprecation time | After 60 days minimum |

#### Required artifacts when transitioning Active → Deprecated

1. **Bump rule version** MINOR (constraint change — `Last-Reviewed` updated, body gains DEPRECATED banner)
2. **Edit `rules-index.csv`** — set `lifecycle_status=deprecated`, `deprecated_at=YYYY-MM-DD` (today), `replaced_by=<successor-rule-slug>` (or empty if no successor)
3. **Add DEPRECATED banner** at top of rule body:
   ```markdown
   > ⚠️ **DEPRECATED 2026-XX-XX** — see `<replaced-by-rule>.md` for current guidance. This rule will be REMOVED after 2026-YY-YY (60-day window).
   ```
4. **Append Log entry** describing deprecation rationale + replacement pointer
5. **Update cross-links** in other rules (replace `<this-rule>` references with `<replaced-by>` where applicable)

#### After 60 days — Removed

Either:
- **Archive** — move file to `.claude/rules/archived/<original-name>.md` + remove CSV row (preserves history)
- **Hard delete** — `rm` the file + remove CSV row (only if zero remaining references)

CI `check-rules-index-csv.sh` enforces:
- `lifecycle_status=deprecated` requires `deprecated_at` date set
- `deprecated_at` cannot be set when `lifecycle_status=active` (inconsistent)
- Validator runs on every PR touching `.claude/rules/**` per `quality-rules-skills.yml` workflow

Initially **WARN mode** for new rule edits ≥7 days; transition to **HARD STOP** (CI BLOCK) for late edits past 60-day grace once first deprecation lands per `incident-to-rule-pipeline.md` premature-rule guard.

---

## 6.5. Enforcement Parity Mandate (added v1.1.0 — paired with `incident-to-rule-pipeline.md`)

> **A new rule and its detection mechanism MUST land in the same PR. No "rule today, detection later" patterns.**

§6 says rules need enforcement; §6.5 says enforcement must be **wired** in the same PR, not just **described**. The distinction matters: a rule that says "PR template should have a checkbox" without that checkbox actually being added to `.github/PULL_REQUEST_TEMPLATE.md` is half-shipped — the rule lands, the enforcement doesn't, drift starts immediately.

### What "wired in the same PR" means by enforcement type

| Enforcement type | Concrete artifact required in same PR |
|-----------------|---------------------------------------|
| Pre-commit hook | New file under `.husky/` OR modification of existing one, executable bit set |
| Pre-merge CI check | New job in `.github/workflows/*.yml` OR new step in existing job |
| PR template item | New checkbox in `.github/PULL_REQUEST_TEMPLATE.md` |
| Audit skill | New rule in `.claude/skills/quality/<skill>/reference/*.md` AND detection logic in the skill's script |
| `audit-gate.py` rule | New AUDIT_RULES entry in `.claude/hooks/audit-gate.py` |
| `session-docs-check` rule | New rule in `reference/doc-rules-matrix.md` AND detection branch in `scripts/check-docs.sh` |
| Reviewer-checklist | New row in the rule's §Enforcement table referencing concrete review checklist items |

### Self-test mandate

The PR description (or commit message body) MUST quote a self-test demonstrating the new detection fires on at least one synthetic positive case. For pure-doc rules, the self-test can be a worked example showing the rule applied; for detection rules, the self-test runs the script against a fixture and shows expected output.

Per `incident-to-rule-pipeline.md` §2 Stage 4 — same requirement, this rule formalizes it for non-incident rule additions too.

### Tracking-gap exception

If enforcement is genuinely impossible in the same PR (e.g. requires infra not yet in place — see GAP-244 dev-stack class of issue), the rule MAY ship with `🟡 PARTIAL` enforcement IF the same PR includes:

1. A tracking gap file with concrete acceptance criteria for the missing enforcement
2. The rule's `Enforcement` section explicitly says "Enforcement deferred to GAP-XXX, ETA <timeframe>"
3. A reviewer-checklist line covering the rule manually until enforcement lands

Tracking gaps stay open in `ROADMAP.md` and are revisited each wave.

### Examples — same PR enforcement parity in action

| Rule | Enforcement landed | Same PR? |
|------|-------------------|----------|
| `gap-done-discipline.md` (v1.0, this PR) | Rule 13 in matrix + detection in `check-docs.sh` + self-test on 3 fixtures | ✅ |
| `incident-to-rule-pipeline.md` (v1.0, this PR) | §5 PR-template checkbox + Rule 14 deferred via tracking-gap exception | ✅ |
| `rule-change-process.md` (this version, v1.1.0) | §5 review matrix already exists; §6.5 self-applies via reviewer-checklist | ✅ |

---

## 7. Changelog Format (per-rule `## Log` section)

Every rule has a `## N. Log` section at the bottom. Entries are **newest-first**:

```markdown
## 9. Log

- **2026-05-31** (v1.1.5): PATCH — fixed 2 stale CI reference(s) `script-quality.yml` → `quality-rules-skills.yml` (workflow was split into quality-{code,docs,rules-skills,infra}.yml 2026-05-22; this rule's §Enforcement still pointed to the removed file). Historical Log entries left unchanged per `rule-change-process.md` §7 append-only. No constraint change. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — broken-link fix).

- **2026-05-31** (v1.1.4): PATCH — added `paths:` frontmatter per `context-budget-mandate.md` §3.2 (rule was always-load, violating §3.2 size-gate ≥1k tokens requires path-scope/justification/hook). Scope matches rule's own **Applies to** — no behavior change (rule still fires when relevant files touched); removes ~20k chars from base session context. Part of Wave meta context-budget rule-scoping batch 2026-05-31. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — path-scope correction, no constraint loosening).

- **YYYY-MM-DD** (v1.3.0): {summary of change}. {Reviewer: @handle, @handle.} {Motivation — user feedback, incident, audit finding.}
- **2026-04-14** (v1.0.0): Rule created.
```

Date + version tag + 1-line summary + reviewer names + motivation. Never rewrite history — append only.

---

## 8. Workflow (how to change a rule in practice)

```
1. Branch:        git checkout -b rule/{slug}
2. Edit:          make change to the .md
3. Frontmatter:   bump Version, Last-Reviewed, Reviewer-Approver
4. Log:           append entry to rule's ## Log section
5. Cross-check:   grep other rules for contradictions
                  grep -l "{keyword}" .claude/rules/*.md
6. Enforcement:   add/update §Enforcement, OR file tracking gap
7. PR:            title "rule: v{old}→v{new} — {summary}"
                  body cites §Enforcement + links cross-impacted rules
8. Review:        per §5 matrix (1 / 2 / 3 reviewers depending on bump)
9. Merge:         squash; no fast-forward (keeps reviewer trail in 1 commit)
```

---

## 9. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Edit a rule with a typo fix and skip log entry | Every change, even typo, gets a log entry + PATCH bump |
| Bump PATCH when tightening a constraint that blocks PRs | MINOR at minimum — honest semver |
| Add a rule without §Enforcement | Cite hook/CI/template or file tracking gap |
| Write `last-reviewed: 2026-04-20` without actually re-reading the rule end-to-end | Read the whole file each review cycle |
| Lead self-approve for MAJOR bump | MAJOR requires Lead + 2 devs + team channel heads-up |
| Leave `version` out of the frontmatter "will fix later" | Fix it in the same commit; frontmatter is mandatory |

---

## 10. Relationship to Other Rules

- **`output-review-mandate.md`** §4 VIOLATION #2 — this rule closes that violation; §3 matrix row "Rules docs (meta)" transitions to ✅ DONE upon adoption
- **`skill-conventions.md`** — parallel governance for `.claude/skills/**`; some concepts overlap (versioning intent) but skill-conventions owns skill files, this rule owns rule files
- **`audit-to-gap-pipeline.md`** — when a rule change requires backfill or fixes a concrete gap, reference the gap ID in the Log entry
- **`meta-gap-priority.md`** — meta gaps that touch rules use THIS process as part of their fix; rule edits are Meta by definition
- **`planning-docs-structure.md`** / **`docs-folder-structure.md`** — they're rules too; every change to them follows this process

---

## 11. Enforcement

- **Pre-merge PR review** (manual, enforced via CODEOWNERS once configured): any PR touching `.claude/rules/**` requires the reviewers per §5 matrix
- **PR template** (`.github/pull_request_template.md`) — "Output Review Checklist" per `output-review-mandate.md` §6.2 already has a `Skills/Rules — lead approved` checkbox; this rule makes that checkbox specifically about following §8 workflow
- **Skill** — `.claude/skills/quality/rule-review/SKILL.md` provides reviewer step-by-step
- **Tracking** — rules without frontmatter Version/Last-Reviewed: backfill-on-next-edit (see §3 backfill policy). Bulk backfill deferred to a tracking gap if needed

---

## 12. Exceptions

| Case | Process |
|------|---------|
| Typo-only fix (single-word) | 1-reviewer PATCH; log entry still mandatory |
| Broken link fix | 1-reviewer PATCH; log entry still mandatory |
| Emergency rule repeal (rule causing prod incident) | Merge after Lead approval alone; post-merge retro within 48h; version bump MAJOR |
| Adding `Log` entry after the fact (historical record) | No version bump; note that Log entry was added retrospectively |

Never skipped: enforcement section, log entry, version bump.

---

## 13. Log

- **2026-05-14** (v1.1.3): PATCH — Wave 76 Bucket C adds §5.1 Atomic-unique rule bar checklist (atomic concept / unique scope / wide applicability / body discipline ≤2 "and" conjunctions) before existing reviewer matrix (renumbered §5.2). Triggered by Wave 76 meta-governance hygiene — codifies ESLint rule guidelines (outside-in benchmark Wave 75) as pre-draft gate for new rules to prevent compound/redundant/narrow rules proliferation. Self-approved per §5 PATCH row (1 dev) — clarification of existing review bar, not new constraint on prior rules. Reviewer: @nguyenvankiet.
- **2026-05-14** (v1.1.2): PATCH — Wave 76 Bucket D — added §3.5 Last-Reviewed staleness policy with WARN/FAIL thresholds (60d/180d), CI enforcement via `scripts/check-rule-staleness.sh` + `script-quality.yml` job `rule-staleness` (WARN mode initially; HARD STOP after 30-day grace period from Wave 76 Bucket D merge). Paired same-PR with `scripts/check-rule-count-ceiling.sh` + `rule-count-ceiling` CI job + `.claude/rules/README.md` "Rule count ceiling policy" section per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Self-test: script reports 55 fresh / 0 stale across current rule set. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — codifies existing implicit Last-Reviewed expectation, adds machine-enforced threshold; no constraint loosening; existing rules grandfathered until next refresh).
- **2026-05-14** (v1.1.1): PATCH — Wave 76 Bucket A — added §6.1 Deprecation lifecycle (active → deprecated → removed, 60-day WARN window, paired same-PR with `rules-index.csv` 3 new columns `lifecycle_status` + `deprecated_at` + `replaced_by` + `check-rules-index-csv.sh` validator extension per `rule-change-process.md` §6.5 Enforcement Parity Mandate). All 55 existing rule rows backfilled `active`; rule applies prospectively to next deprecation transition. PATCH bump per §5 — additive process documentation, no existing constraint loosened. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5).
- **2026-04-27** (v1.1.0): MINOR — added §6.5 Enforcement Parity Mandate (rule + detection same PR; self-test mandate; tracking-gap exception). Paired in same PR with new sister-rule `incident-to-rule-pipeline.md` (governs how misses become rules) and `gap-done-discipline.md` (the first concrete application — rule + Rule 13 detector + 3-fixture self-test all shipped together). Triggered by user feedback "có quy trình khi thêm 1 skill, 1 rules vào dự án chưa, mà vẫn miss kiểu này" surfacing that prior process governed rule edits but not enforcement parity at addition time. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5; new constraint on rule authors but does not loosen any existing constraint).
- **2026-04-20** (v1.0.0): Rule created (GAP-171). Closes `output-review-mandate.md` §4 VIOLATION #2 ("Rules docs — meta governance without meta review"). Self-referential bootstrap: author self-approved v1.0 as there was no prior process; subsequent versions require §5 matrix reviewers. Paired with `.claude/skills/quality/rule-review/SKILL.md` and the gap-review skill (GAP-170 / Wave 8b-A).
