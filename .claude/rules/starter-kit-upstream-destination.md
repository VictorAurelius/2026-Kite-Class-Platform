# Starter-Kit Upstream Destination — bundle PR goes to upstream repo, not downstream

**Priority:** 🟠 MANDATORY — cross-repo destination governance
**Version:** 1.0.0
**Created:** 2026-06-04
**Last-Reviewed:** 2026-06-04
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test on 2026-06-04 PR #2154 originating incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies Wave 2026-04-29 lesson (`wave-history.jsonl` entry `wave-2026-04-29-ui-kits-round-2-add-ons`) that was tribal knowledge until now)
**Applies to:** Mọi session edit dưới `.claude/starter-kit/**` mà thay đổi semantic content (VERSION bump / new skill / rule version bump / CHANGELOG entry). Out-of-scope: thuần local-mirror sync update (.starter-kit-version pin update only).

---

## 1. The Rule

> **Starter-kit bundle changes (new skill / rule version bump / VERSION + CHANGELOG entry) PHẢI mở PR trên upstream canonical repo `github.com/VictorAurelius/claude-starter-kit`, KHÔNG dump bundle vào downstream project repo.** Downstream project (this repo) chỉ nhận **companion sync PR** nhỏ (≤5 LOC: `.claude/.starter-kit-version` pin update + optional CHANGELOG mirror line) SAU KHI upstream PR merge.

Force-multiplier: 1 chuẩn destination → bundle propagate đúng tới mọi adopter (precedent Wave 04-29 PR #10 1620 LOC merged same-day upstream + companion #676 5-LOC downstream). Skip rule → bundle stuck trong downstream → other adopters không receive value + downstream mirror drift khỏi canonical.

---

## 2. Why this rule exists

### 2.1 Architectural model

`.claude/starter-kit/` trong THIS project là **read-only mirror** của upstream — distilled from project experience nhưng canonical store lives upstream. Layout convention:

| Downstream (this project) | Upstream (`claude-starter-kit`) | Rationale |
|---|---|---|
| `.claude/rules/foo.md` | `rules/foo.md` (bare) | Upstream template gets copied INTO `.claude/` by downstream `bin/init-project.sh` |
| `.claude/skills/<cat>/<skill>/` | `skills/<cat>/<skill>/` (bare) | Same template-to-installed mapping |
| `.claude/starter-kit/` (mirror) | (repo root) | Mirror flatten-prefixed for downstream tracking |
| `.claude/.starter-kit-version` | (N/A) | Downstream-only version pin |

### 2.2 Originating incident — 2026-06-04 PR #2154

Background agent shipped 17-file v2.7.0 thesis tooling bundle:
- Created PR **#2154 on THIS project repo** (`VictorAurelius/2026-Kite-Class-Platform`) ❌ wrong destination
- 1429 LOC dumped into downstream mirror, never reached upstream `claude-starter-kit`
- User caught after PR creation. Cost: 1 round-trip + close PR + redo upstream work + spawn second agent.

Wave 04-29 precedent (`wave-history.jsonl` entry `wave-2026-04-29-ui-kits-round-2-add-ons`) had explicit lesson:
> "First parallel cross-repo work: starter-kit Phase 2b agent works in /tmp/kit-pr1 absolute path (legitimate exception for cross-repo) WHILE other 3 agents in worktrees use RELATIVE."
> "Upstream layout differs from downstream: starter-kit uses rules/ (not .claude/rules/) — agent adapted at runtime. Document in cross-repo prompt template."

Lessons existed but never codified → recurrence 2026-06-04. This rule closes the gap.

---

## 3. Trigger pattern — when this rule fires

Rule fires khi PR diff touches `.claude/starter-kit/**` AND ANY of:

| Pattern | Example | Rule fires? |
|---|---|---|
| New skill added under `.claude/starter-kit/skills/**` | thesis-citation-extract NEW directory | ✅ YES — upstream PR mandatory |
| Rule version bump under `.claude/starter-kit/rules/**` | `thesis-content-standard.md` v1.0.0 → v2.0.0 | ✅ YES |
| `.claude/starter-kit/VERSION` bump | 2.6.0 → 2.7.0 | ✅ YES |
| `.claude/starter-kit/CHANGELOG.md` entry added | `[2.7.0] — 2026-06-04 — ...` | ✅ YES |
| Rule body content edit (non-trivial) under `.claude/starter-kit/rules/**` | major rewrite | ✅ YES |
| `.claude/.starter-kit-version` pin update SOLO (companion sync) | `2.6.0` → `2.7.0` after upstream merge | ❌ NO — downstream-only sync acceptable |
| Typo fix / formatting under `.claude/starter-kit/**` | comma fix in README | ⚠️ Discretionary — small fix can be downstream-only with note "trivial, defer upstream batch" |

---

## 4. Required workflow

When rule fires, agent (or coordinator) MUST follow cross-repo workflow:

```bash
# 1. Clone upstream OUTSIDE this project tree
cd /tmp && rm -rf claude-starter-kit-pr 2>/dev/null
git clone https://github.com/VictorAurelius/claude-starter-kit.git claude-starter-kit-pr
cd claude-starter-kit-pr
git checkout -b feat/<topic>-v<version>

# 2. Copy from downstream mirror with PATH REMAP
DOWN=/home/nguyenvankiet/projects/2026-Kite-Class-Platform/.claude/starter-kit
cp $DOWN/VERSION ./VERSION
cp $DOWN/CHANGELOG.md ./CHANGELOG.md
cp $DOWN/rules/<rule>.md ./rules/<rule>.md
cp -r $DOWN/skills/<cat>/<skill> ./skills/<cat>/
chmod +x ./skills/<cat>/<skill>/scripts/*.sh   # preserve exec bit

# 3. Verify scrub (zero project-specific leaks)
grep -rinE "kitehub|kiteclass|GAP-[0-9]+|Wave [0-9]+" rules/ skills/<cat>/<skill>/

# 4. Commit + push + PR to upstream
git commit -m "feat: ..." && git push -u origin feat/<topic>-v<version>
gh pr create --base main ...
```

### 4.1 Companion downstream PR (small, AFTER upstream merge)

After upstream PR merges, create companion downstream PR on THIS project:
- `.claude/.starter-kit-version` bump (pin to new upstream version)
- Optional CHANGELOG entry in downstream mirror (≤5 LOC)
- Reference upstream PR # in commit body

Per Wave 04-29 precedent: upstream PR #10 (1620 LOC, full bundle) + downstream PR #676 (5 LOC, version pin only).

---

## 5. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Open PR on this project repo for starter-kit bundle | Open upstream PR on `github.com/VictorAurelius/claude-starter-kit` |
| Dump 17-file bundle into downstream mirror "for visibility" | Bundle lives upstream; downstream mirror gets pin update only |
| Apply scrub but skip upstream — keep in downstream mirror | Scrub IS for upstream consumption; without upstream PR scrub is wasted |
| Forget path remap (downstream `.claude/rules/` → upstream `rules/`) | Always remap at copy time; verify with `git ls-tree` |
| Skip companion downstream PR after upstream merge | Companion PR keeps downstream mirror current — without it, drift |
| Lose `+x` exec bit on `.sh` scripts during copy | Use `chmod +x` after copy; verify via `git ls-tree HEAD` |
| Trust agent prompt without rule cite | Cite this rule in agent task prompt — `Per starter-kit-upstream-destination.md §4 workflow:` |
| Treat starter-kit edits as ordinary docs PR (auto-merge eligible downstream) | Auto-merge BANNED for starter-kit bundle scope — must go upstream first |

---

## 6. Override mechanism

Genuine exception (upstream auth unavailable, upstream repo down, time-critical fix needed before upstream review window):

```
git commit -m "...
STARTER_KIT_UPSTREAM_DEFER: <reason — e.g., 'upstream PR auth pending, downstream mirror staged for visibility'>
STARTER_KIT_UPSTREAM_FOLLOWUP: <gap or commit reference scheduling upstream PR within Ndays>"
```

Trailer logged trong quarterly retro. Pattern frequency >5%/quarter triggers meta-review (likely upstream access friction needs systematic fix).

---

## 7. Worked self-test — 2026-06-04 PR #2154 incident

**Scenario:** Agent ran "Update starter-kit thesis bundle" task. Output: 17 files added/modified under `.claude/starter-kit/**`.

### 7.1 Apply §3 trigger check

| Diff pattern | Trigger? |
|---|---|
| `.claude/starter-kit/VERSION` 2.6.0 → 2.7.0 | ✅ YES |
| `.claude/starter-kit/CHANGELOG.md` `[2.7.0]` entry | ✅ YES |
| `.claude/starter-kit/rules/thesis-content-standard.md` v1.0.0 → v2.0.0 | ✅ YES |
| `.claude/starter-kit/skills/quality/thesis-citation-extract/**` NEW | ✅ YES |
| `.claude/starter-kit/skills/quality/thesis-figure-curation/**` NEW | ✅ YES |

→ Rule fires (multiple triggers).

### 7.2 Required action per §4

Workflow: clone upstream `claude-starter-kit` to `/tmp` → branch → copy with path remap → push upstream → PR upstream.

### 7.3 Actual behavior (BEFORE rule)

Agent ran in main worktree, modified files in-place at `.claude/starter-kit/`, committed within session. Coordinator (me) created PR #2154 on THIS project repo. User caught immediately:

> "tôi tưởng theo rule là PR trên repo remote của starter kit nhỉ"

Counterfactual với rule applied:
- Agent prompt would cite `starter-kit-upstream-destination.md §4 workflow`
- Coordinator (or agent) clones upstream FIRST, applies bundle there
- PR opens on upstream `VictorAurelius/claude-starter-kit` from start
- Companion downstream PR (5 LOC pin update) opens AFTER upstream merge

Cost saved: ~1 user round-trip + PR #2154 close cleanup + agent re-spawn for redo (~30 min agent wall-clock + token).

**Verdict:** Rule fires correctly on originating incident. Self-test PASS ✅.

---

## 8. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 8.1 Reviewer-checklist (active now)

Pre-merge review for PR touching `.claude/starter-kit/**`:

- [ ] Diff matches §3 trigger pattern (VERSION/CHANGELOG/skill/rule version)?
- [ ] If YES → upstream PR opened FIRST? Link in PR body OR commit trailer?
- [ ] If downstream-only PR exists → is it companion sync (≤5 LOC pin update) per §4.1?
- [ ] If override trailer present, valid reason + follow-up linked per §6?
- [ ] Path remap correct (downstream `.claude/rules/` → upstream `rules/`)?

### 8.2 Agent prompt template

Future agent prompts updating starter-kit MUST cite this rule explicitly:

```
Per `.claude/rules/starter-kit-upstream-destination.md` §4 workflow:
1. Clone upstream `github.com/VictorAurelius/claude-starter-kit` to /tmp
2. Apply bundle with path remap (.claude/starter-kit/X → bare X)
3. PR upstream FIRST
4. Companion downstream PR (≤5 LOC pin update) opens AFTER upstream merge
```

Coordinator self-checks agent prompts before spawning.

### 8.3 Memory auto-load (paired same-PR)

Memory entry `feedback_starter_kit_upstream_destination.md` loads at session start. 4-bullet checklist:
1. Edit dưới `.claude/starter-kit/**` với semantic change? → §3 trigger
2. Path remap downstream `.claude/<X>` → upstream `<X>` bare
3. Upstream PR FIRST, downstream companion (≤5 LOC) AFTER
4. Wave 04-29 precedent: PR #10 (1620 LOC upstream) + PR #676 (5 LOC downstream)

### 8.4 Detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1 tightened conditions)

- **Detector complexity:** Scan PR diff for `.claude/starter-kit/**` semantic changes (VERSION/CHANGELOG/skill/rule) + check PR body for upstream PR reference — moderate (NLP cross-repo link detection)
- **Recurrence count:** 1 (2026-06-04 PR #2154 originating + Wave 04-29 PR #676 precedent didn't surface as recurrence at time)
- **FP risk:** Low — clear binary (upstream PR linked OR not)
- **Decision:** Reviewer-checklist §8.1 + memory auto-load §8.3 + worked self-test §7 sufficient cho v1.0.0; revisit detector khi recurrence-count ≥2 post-rule

### 8.5 Override mechanism

Per §6 trailer `STARTER_KIT_UPSTREAM_DEFER:` — logged quarterly retro. Pattern frequency >5%/quarter triggers meta-review.

---

## 9. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Treat `.claude/starter-kit/` as "just another docs folder" | Mirror layer; canonical lives upstream |
| Allow agent to "decide" destination at runtime | Rule mandates upstream; agent prompt cites rule |
| Skip upstream PR "because downstream is faster" | Speed kills propagation value — adopters miss bundle |
| Merge downstream mirror update before upstream lands | Causes mirror drift; upstream is source of truth |
| Use `--admin` on downstream starter-kit PR to ship faster | `--admin` BANNED per `admin-merge-discipline.md`; rule forces correct destination instead |
| Drop scrub validation "because content already clean" | Scrub IS the upstream-readiness check; skip = risk leak |
| Forget executable bit on .sh files during copy | `chmod +x` mandatory; verify via `git ls-tree HEAD` |

---

## 10. Relationship to other rules

- **`incident-to-rule-pipeline.md`** — this rule = direct output 2026-06-04 PR #2154 incident applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + memory + worked self-test §7 + rules-index.csv row all ship same PR
- **`docs-only-pr-auto-merge.md`** §3 — starter-kit bundle PRs are explicitly OUT of auto-merge scope (cross-repo destination); rule này provides correct workflow
- **`admin-merge-discipline.md`** — no override option to ship wrong-destination PR faster; rule này routes correctly instead
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (1 chuẩn destination → mọi starter-kit edit subsequent auto-comply prospectively → eliminate wrong-destination class)
- **`agent-action-bias.md`** §1 Part A — "do it yourself" extends: do CROSS-REPO step yourself (clone upstream + path remap + PR upstream) instead of dumping downstream
- **`output-review-mandate.md`** §3 — paired same-PR matrix row "Starter-kit bundle destination" tracking review standard
- **`feedback_starter_kit_upstream_destination.md`** (memory, paired same-PR per Enforcement Parity)

---

## 11. Log

- **2026-06-04 (v1.0.0):** Rule created in response to user direction 2026-06-04 mid-session: "update meta để tránh lần sau sai" sau PR #2154 wrong-destination incident (17-file v2.7.0 starter-kit bundle dumped vào project repo thay vì upstream `claude-starter-kit`). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged PR #2154 wrong destination) → Classify ✓ (no existing rule codifies upstream-vs-downstream destination cho starter-kit bundle; Wave 04-29 wave-history.jsonl had lessons but not enforced; closest `agent-action-bias.md` covers general do-it-yourself không specific cho cross-repo destination) → Rule+Enforce ✓ (this file + reviewer-checklist §8.1 + agent prompt template §8.2 + memory `feedback_starter_kit_upstream_destination.md` + rules-index.csv row + worked self-test §7 paired same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example on 2026-06-04 PR #2154 originating incident — rule fires correctly + counterfactual ~30 min agent wall-clock + 1 user round-trip eliminated) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn destination → mọi starter-kit edit subsequent auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-tribal Wave 04-29 lesson; no constraint loosening; existing downstream-only starter-kit PRs grandfathered until next refresh; rule applies prospectively từ this PR forward 2026-06-04). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: bundle → upstream destination) + ✅ unique (no overlap với existing cross-repo rules) + ✅ widely applicable (every starter-kit bundle edit) + ✅ body discipline §1 has 0 "and" conjunctions. CI detector (§8.4) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (recurrence count 1 + reviewer-checklist + memory + self-test sufficient cho v1.0.0).
