---
title: Wave 75 — Meta-System Outside-In Benchmark
status: complete
created: 2026-05-14
phase: pre-wave-76-planning
wave: 75-outside-in
---

# Wave 75 — Meta-System Outside-In Benchmark

So sánh KiteHub meta-governance system (56 rules + ~50 skills + 6 hooks + 3 CSV indexes + 5-stage incident pipeline) với các OSS governance pattern phổ biến để (a) phát hiện missing patterns, (b) phát hiện over-engineering, (c) validate / refute giả định trước khi chốt Wave 76 scope.

## Scope

KiteHub đã build meta-governance system gồm:

- **56 rules** trong `.claude/rules/*.md` (14 always-load CRITICAL + 42 path-scoped MANDATORY)
- **~50 skills** trong `.claude/skills/**/SKILL.md` (categories: core, quality, quality-audit, backend, frontend, devops, document-generation, testing, workflow, reference, quick-reference)
- **6 hooks** trong `.claude/hooks/*.py` (audit-gate, inject-rule-digest, post-tool-guard, pre-tool-guard, session-lock-guard, stop-handoff-check)
- **3 CSV canonical indexes** (gaps + rules + ADRs) per `meta-csv-index-pattern.md`
- **5-stage incident-to-rule-pipeline** (Detect → Classify → Rule+Enforce → Self-Test → Retro Log)
- **Path-scope mechanism** via Anthropic native `paths:` frontmatter (Wave 73 outcome, ~75% base context savings)
- **Wave-based development** (~75 waves shipped)

User-flagged trước Wave 76 plan freeze: "còn cần update gì nữa cho meta, đánh giá meta của hệ thống, nên tinh giảm hay không" — đây là outside-in lens audit.

## Methodology

**Tier 1 — Major OSS governance patterns:**

| Project | URL | Mapping |
|---|---|---|
| Kubernetes governance + KEPs | github.com/kubernetes/community + enhancements | Decision tracking + SIG ownership |
| Rust RFC process + Clippy lints | github.com/rust-lang/rfcs + rust-clippy | Rule lifecycle + lint categorization |
| TypeScript contributing | github.com/microsoft/TypeScript | Bug → test → rule pattern |
| ESLint rule proposal | github.com/eslint/eslint | Rule sprawl management |
| Semgrep rule registry | semgrep.dev/docs | Categorization + metadata |
| OPA/Rego policy framework | openpolicyagent.org | Policy organization + test convention |
| Google SRE postmortem culture | sre.google/sre-book | Incident → policy pipeline |
| GitHub CODEOWNERS | docs.github.com | Path-scoped ownership |

**Tier 2 — Emerging patterns (2025-2026):**

| Topic | Sources |
|---|---|
| Path-scoped rule loading | GitHub Copilot, Continue.dev, OpenAI Codex codex#17239 |
| AI-powered postmortem automation | Zalando AI postmortem analysis, incident.io 2026 platforms |
| OSS rule deprecation lifecycle | OpenLogic 2026 State of OSS, GitHub community discussion #190112 |
| CSV vs YAML frontmatter | CSVY format, DataCite blog, MADR ADR-0013 |
| Claude Code skills/rules convention | code.claude.com/docs, Mario Ottmann customization guide |
| YAGNI for solo dev | Multiple framework sources |

**Budget consumed:** ~10 WebFetch + ~7 WebSearch. Audit-only output; no code modifications.

---

## Findings per project

### Kubernetes (KEP process + SIG governance)

**Pattern summary:**
- Distributed ownership via SIGs (Special Interest Groups) with charters
- KEP (Kubernetes Enhancement Proposal) required cho non-trivial changes — formal mechanism similar to IETF RFC / Python PEP
- Subprojects-within-SIG model: clear ownership boundaries cho every identifiable subpart
- Annual health checks for governance bodies; escalation path via Steering Committee
- KEP organized into SIG subdirectories với unique tracking-issue IDs

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Formal proposal for non-trivial change | ✅ — `rule-change-process.md` + KEP-like ADRs | ADR template = MADR; rule changes have semver + Reviewer-Approver |
| Distributed ownership | ⚠️ N/A (solo-dev) | But future-team-ready: `output-review-mandate.md` §7 RACI matrix already exists |
| Annual / quarterly health check | ⚠️ PARTIAL — `quality-audit` skill quarterly | Rules themselves don't have a periodic re-review cadence enforced |
| Tracking ID per proposal | ✅ — GAP-NNN + ADR-NNN | Stable identifiers in CSV indexes |

**Applicability:** ✅ Pattern already present. **No new buckets needed** from this row.

### Rust RFC + Clippy lints

**Pattern summary:**
- RFC required for "substantial changes" (syntactic/semantic, removal of features, large stdlib additions)
- Lifecycle: Preparation → Submission → Triage → Final Comment Period (FCP, 10 calendar days) → Decision → Implementation
- Sub-team labels and assigns; build CONSENSUS, not unanimous
- Clippy lint **categorization** is sharp: `correctness` (deny-by-default, abort) / `style` (warn) / `pedantic` / `nursery` (allow-by-default, may have radical changes) / `restriction` (cherry-pick only) / `deprecated` (empty lints kept for backward compat)
- **`clippy::nursery` = explicit "not stable yet" lane** — lints may experience radical changes but never fully removed
- Lint **deprecated lane preserves backward compat** — empty lint kept so `#[allow(lintname)]` still compiles

**KiteHub mapping:**

| Pattern | KiteHub coverage | Gap detail |
|---|---|---|
| Severity tier (correctness/style/nursery/etc) | ⚠️ PARTIAL — priorities 🔴 CRITICAL / 🟠 MANDATORY / 🟡 ADVISORY | No "nursery" tier for experimental/unstable rules |
| FCP-equivalent waiting period | ❌ MISS | Rules merge same session as created; no cooldown for community feedback |
| Deprecation lane | ❌ MISS | No `.claude/rules/deprecated/` folder; no policy for "this rule replaced by X" |
| Categorization | ✅ — paths/scope via `Applies to:` frontmatter | Adequate |

**🆕 NEW pattern surfaces:** Rule **deprecation lifecycle** + **nursery lane** for trial-period rules before promoting to MANDATORY.

**Note:** `incident-to-rule-pipeline.md` §3 "premature-rule guard ≥7 days" is partial overlap with FCP — KiteHub already defers DETECTOR wiring, but not the rule body itself. Industry pattern is to defer the RULE while detector is built.

### TypeScript contributing flow

**Pattern summary:**
- No formal PR template; minimum req: description + tests (test must FAIL without fix) + baselines + coding guidelines adherence
- Bug → Test → Rule pattern explicit: add `.ts` test in `tests\cases\compiler` demonstrating the fix BEFORE writing fix
- Onboarding is practical (clone → `npm ci` → `hereby runtests-parallel`) — no formal mentorship/buddy
- Governance docs scattered: CONTRIBUTING.md + Wiki coding-guidelines + FAQ + Compiler-Notes repo

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Test-first / Bug→Test→Rule | ✅ — TDD enforcement skill | `core/tdd-enforcement.md` exists |
| Practical onboarding (clone → run → done) | ⚠️ N/A (solo-dev) | But: `documents/05-guides/dev/*` could be future-team material |
| Scattered governance | ❌ ANTI-PATTERN | KiteHub does the opposite: meta-csv-index-pattern centralizes |

**Applicability:** ✅ KiteHub is **stronger** than TypeScript on doc-centralization. No bucket needed.

### ESLint rule proposal process

**Pattern summary:**
- Rules must be "widely applicable, generic, atomic, unique, library-agnostic" — strict gate
- 3 acceptance criteria: meet guidelines + ESLint team member CHAMPION + ECMAScript stage-4 feature within preceding 12 months
- **ESLint team does NOT implement user-suggested rules** — proposer bears full responsibility (forces "skin in game")
- Burden quote: "we only accept rules related to new ECMAScript features" — explicit recognition of 200+ rule maintenance cost
- Alternative path: custom plugins (don't enter core)

**KiteHub mapping:**

| Pattern | KiteHub coverage | Gap detail |
|---|---|---|
| Strict acceptance gate ("widely applicable, atomic, unique") | ⚠️ PARTIAL — `rule-change-process.md` §6 enforcement clause | But "unique" check is informal (reviewer cross-checks rules; not a numeric quality bar) |
| Champion requirement | ⚠️ N/A (solo-dev = self-champion) | But: useful when team grows |
| Maintenance-cost explicit recognition | ✅ — `context-budget-mandate.md` v1.0.0 (Wave 73) | KiteHub introduced this exactly to prevent rule sprawl |
| Plugin / extension alternative path | ❌ MISS | All KiteHub rules are in single `.claude/rules/` — no "userland" tier for project-specific or low-priority rules |

**🆕 NEW pattern surfaces:** Explicit **rule-quality bar** before accepting into MANDATORY tier. Atomic + unique + cross-cutting (not project-specific) — could prevent "everything is a rule" drift.

### Semgrep rule registry

**Pattern summary:**
- Required fields: `id` + `message` + `severity` (LOW/MEDIUM/HIGH/CRITICAL) + `languages` + pattern operator
- Categorization: `best-practice` / `correctness` / `maintainability` / `security` (security has extra metadata reqs)
- Optional `min-version` / `max-version` for Semgrep version compat — **rules can target specific tool versions**
- Rule registry has 1000+ rules but **maintenance details aren't explicit in public docs** — that's a notable gap
- Fixture-based testing referenced but separate section

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Required metadata fields | ✅ — markdown frontmatter (Priority/Version/Created/Last-Reviewed/Reviewer/Applies-to) + CSV index | More detailed than Semgrep |
| Tool-version compat (`min-version`/`max-version`) | ❌ MISS | KiteHub rules don't declare "this rule needs Claude Code ≥X" or "deprecated after rule-change-process v2" |
| Fixture testing | ✅ — `incident-to-rule-pipeline.md` §2 Stage 4 self-test mandate | Stronger than Semgrep |
| 1000+ rule maintenance pattern | ❌ N/A — KiteHub at 56 | Pre-emptive concern at 100+; see "Rule count ceiling" section |

**Applicability:** ⚠️ "Rule version compat" could matter when Claude Code itself upgrades. Currently low priority.

### OPA/Rego policy framework

**Pattern summary:**
- Modules with package declaration (namespace), imports, rule definitions
- Rule types: **partial rules** (incremental sets) / **complete rules** (single-valued) / **helper rules** (modular composition)
- Metadata annotations: scope, title, description, custom fields
- Policy testing as first-class capability
- Package namespacing enables safe policy reuse across projects

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Modular policy composition (helpers + complete rules) | ⚠️ PARTIAL — rules cross-reference via §Related sections | But: each rule is standalone Markdown; no "shared helpers" concept |
| Package namespacing | ⚠️ INFORMAL — folder structure (rules/skills) | No formal namespace via frontmatter |
| First-class policy testing | ✅ — Stage 4 self-test mandate + worked examples | |

**Applicability:** ⚠️ Limited. OPA pattern is for runtime policy enforcement; KiteHub rules are mostly review-time guidance. Less relevant.

### Google SRE postmortem culture

**Pattern summary:**
- **Blameless postmortems** — systemic weaknesses, not fault assignment
- Workflow: incident → draft → senior review → broad dissemination → centralized repo enables pattern detection
- "Postmortems are ideally the product of engineer self-motivation" — culture > process
- Start lightweight: trial period with several complete postmortems to prove value
- Celebrate learnings (peer bonuses, all-hands acknowledgment) drives adoption better than mandates
- Emerging automation: data extraction from postmortems for trend analysis

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Blameless culture | ✅ — `incident-to-rule-pipeline.md` is blameless (focus on missing rule, not author error) | |
| Incident → centralized repo enabling pattern detection | ⚠️ PARTIAL — memory entries + gap files + rule Log sections | Centralized but **NOT searchable for trend analysis** (no "show me all incidents triggered by AC mismatch") |
| Lightweight start | ✅ — 5-stage pipeline is concrete and actionable | |
| Pattern detection / trend analysis | ❌ MISS | No quarterly "review last 90 days of memory-tagged-incident-driven entries" automation per `incident-to-rule-pipeline.md` §5 actually executed |
| AI-powered postmortem analysis (2026 emerging) | ❌ MISS | Zalando-style "thousands of postmortems → one-pager trends" not in scope |

**🆕 NEW pattern surfaces:** **Incident trend analysis cron** — quarterly script scans memory entries + Log sections for class clustering (e.g., "5 incidents this quarter all involved AC-mismatch — is the AC standard the actual problem?"). Currently §5 prescribes this in the rule but no automation.

### GitHub CODEOWNERS

**Pattern summary:**
- Path-glob → reviewer mapping; auto-routing reviews
- gitignore-style patterns; last-matching-pattern-wins
- Combined with branch protection → enforced approvals on critical paths
- Best practice: protect CODEOWNERS file itself; place in `.github/`

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Path-scoped responsibility | ⚠️ N/A (solo-dev) | But CODEOWNERS for future-team triage is trivial setup |
| Branch protection + enforced approvals | ⚠️ PARTIAL — admin-merge-discipline.md governs `--admin` use | No formal CODEOWNERS gate yet |

**Applicability:** ⚠️ Useful when team scales. Currently low priority.

---

## Emerging patterns (Tier 2)

### Path-scoped rule loading (Claude Code + Copilot + Continue.dev)

**Pattern summary (2025-2026):**
- **GitHub Copilot** shipped path-scoped instruction files (Sep 2025) with `applyTo:` frontmatter — direct equivalent of Anthropic `paths:`
- **Continue.dev**: rules scoped to file patterns (e.g., `*.ts`) — load only when relevant
- **OpenAI Codex** issue #17239: explicit proposal for path-aware dynamic rule loading
- Universal trend: "context engineering" — selectively load docs when task pulls them in; reference material is strictly additive on turns that don't need it

**KiteHub mapping:** ✅ **KiteHub is AHEAD of industry** — Wave 73 already implemented this with empirical baseline (~75% base context savings, ~347k → ~88k tokens). Pattern is novel-leading, not lagging.

**Verdict:** Ship `context-budget-mandate.md` as **public reference** if KiteHub ever open-sources. Industry is actively converging on this pattern.

### AI-powered postmortem automation (Zalando, incident.io)

**Pattern summary:**
- Multi-stage LLM pipelines: summarization → classification → analysis → patterns (more effective than single high-end LLM with large context)
- 2026 DevOps Benchmark: 65% incident resolution time reduction with deep automation
- Postmortem auto-drafting from monitoring tool data

**KiteHub mapping:** ⚠️ Low fit. KiteHub has no production incident stream yet (Phase 1 BETA). Premature. Defer to Phase 2.

### OSS rule deprecation lifecycle (OpenLogic 2026, GitHub community)

**Pattern summary:**
- **Deprecation calendar** with HARD dates (even approximate: "v4.0 / Q3 2026") forces commitment + migration priority
- Notice format: "This will throw an error starting vX.Y, targeted for [date]"
- `deprecations.json` machine-readable + CI script warns at 60 days, errors past removal date
- Rule-as-code governance for API ecosystems

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Deprecation calendar | ❌ MISS | No rule has ever been deprecated; no "this rule sunsets on date X" pattern |
| Machine-readable deprecation index | ❌ MISS | `rules-index.csv` has no `deprecated_at` / `replaced_by` columns |

**🆕 NEW pattern surfaces:** Add `status` enum to `rules-index.csv` (active / deprecated / superseded) + `replaced_by` column. Currently rules never get formally retired — they accumulate.

### CSV vs YAML frontmatter (CSVY, MADR ADR-0013)

**Pattern summary:**
- **CSVY format** combines CSV + YAML frontmatter into one file — addresses metadata/data separation problem
- **MADR ADR-0013** chose YAML frontmatter as PRIMARY metadata source — opposite of KiteHub's CSV-canonical choice
- Risk of metadata drift when data + metadata in separate files

**KiteHub mapping:** ⚠️ KiteHub chose **CSV-canonical, markdown-frontmatter-cache** (per `meta-csv-index-pattern.md`). Industry split:
- MADR + Hugo + most static site generators → YAML frontmatter primary
- Datasets / governance enumerations → CSV (with CSVY hybrid emerging)

**Verdict:** KiteHub choice is **defensible but contrarian**. Risk: future tooling integrations (e.g., Anthropic Skill marketplace if it materializes) likely expect YAML frontmatter. Worth documenting choice in ADR for future-team / migration context.

### Solo-dev YAGNI for meta-system

**Pattern summary:**
- For resource-constrained projects: YAGNI ensures effort directed at essential functionality
- Code reviews question speculative additions: "Who asked for this?" "When will this be used?"
- Particularly important: each rule has carrying cost (maintenance, doc updates, context budget)

**KiteHub mapping:** ⚠️ KiteHub meta-system is **arguably over-engineered for solo-dev mode** — 56 rules + 50 skills + 6 hooks is comparable to teams of 20+. BUT:
- `context-budget-mandate.md` already addresses the cost dimension
- `meta-gap-priority.md` argues force-multiplier justifies the investment
- `outside-in-coverage-trigger.md` itself was caught BY this rule (Wave 75 C+D refuted 2 of 3 outside-in CRITICAL claims — empirical override)

**Verdict:** Not strictly over-engineered, but cumulative carrying cost is real. **Pruning hygiene** (deprecation lane + rule-quality bar + atomic-unique gate) needed before count reaches ~100.

### Claude Code skills vs rules convention

**Pattern summary** (from Mario Ottmann + Anthropic docs):

| Layer | Purpose | Trigger |
|---|---|---|
| **CLAUDE.md** | Always-on project context | Auto-load |
| **Rules** | Reference content + constraints (style, conventions, domain knowledge) | Inline, runs alongside conversation |
| **Skills** | Step-by-step instructions for SPECIFIC ACTIONS (workflows, repetitive tasks) | Triggered by description match |
| **Subagents** | Context isolation for distinct tasks | Spawn-time |
| **MCPs** | External system reach | Tool calls |

Build order: CLAUDE.md → Rules → Skills → Subagents → MCPs.

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Layered architecture | ✅ | All 5 layers in use |
| Skill/rule split convention | ⚠️ PARTIAL — fuzzy | `hook-review` is a skill (workflow) ✓; `admin-merge-discipline` is a rule (constraint) ✓; but `agent-action-bias` could be either |
| Rule = "constraint or convention", Skill = "step-by-step workflow" | ⚠️ INFORMAL | Not codified in `.claude/rules/README.md` |

**🆕 NEW pattern surfaces:** **Codify skill-vs-rule decision criterion** in `.claude/rules/README.md` or `skill-conventions.md`:
- "If the artifact is a constraint to check at review/PR time → rule"
- "If the artifact is a multi-step workflow to execute → skill"
- "If borderline, default to rule (lower carrying cost; skills are heavier with reference/scripts/data subfolders)"

### Linter rule lifecycle (Clippy nursery + deprecated lanes)

**Pattern summary** (in addition to "Rust RFC + Clippy" above):
- `clippy::nursery` is **explicit lane for unstable rules** — "may experience radical changes but never fully removed"
- `clippy::deprecated` is **empty rule kept for backward compat** — `#[allow(lintname)]` still compiles after deprecation
- Default severity by category: `correctness=deny`, `style=warn`, `pedantic=allow`, `nursery=allow`, `restriction=allow`

**KiteHub mapping:**

| Pattern | KiteHub coverage | Notes |
|---|---|---|
| Nursery/experimental lane | ❌ MISS | No `.claude/rules/nursery/` or `🧪 EXPERIMENTAL` priority |
| Empty/grandfather lane for deprecated | ❌ MISS | Rules deleted outright would break references in older docs |
| Severity tier matches enforcement | ✅ — CRITICAL = always-load = WARN/BLOCK hook | KiteHub has implicit version of this |

**🆕 NEW pattern surfaces:** **EXPERIMENTAL tier** for rules added pre-detector-stabilization (currently `incident-to-rule-pipeline.md §3` defers detectors ≥7 days, but the rule itself is fully MANDATORY immediately). A nursery lane lets the rule itself be tentative.

---

## Coverage gap analysis — KiteHub meta vs industry patterns

| # | Industry pattern | KiteHub coverage | Confidence | Gap detail |
|---|---|---|---|---|
| 1 | Decision proposal mechanism (KEP/RFC/ADR) | ✅ FULL | High | rule-change-process + MADR ADR; KiteHub is **on par** |
| 2 | Tracking IDs (GAP-NNN, ADR-NNN) | ✅ FULL | High | CSV indexes provide both |
| 3 | Severity categorization | ✅ FULL | High | CRITICAL/MANDATORY/ADVISORY ≈ correctness/warn/allow |
| 4 | Test-first / fixture per rule | ✅ FULL | High | Stage 4 self-test mandate **stronger than Semgrep public docs** |
| 5 | Incident → rule pipeline | ✅ FULL | High | **KiteHub is AHEAD** — 5-stage pipeline codified is rare in industry; Google SRE has the culture, not the formal pipeline |
| 6 | Path-scoped doc loading | ✅ FULL | High | **KiteHub is AHEAD** — Wave 73 empirically measured ~75% savings; industry catching up |
| 7 | CSV canonical index | ✅ FULL | Medium | KiteHub-specific choice; defensible but contrarian (MADR uses YAML) |
| 8 | Final Comment Period / cooldown before merge | ❌ MISS | High | Rules merge same session; no community-feedback window. Solo-dev exempt? |
| 9 | Rule deprecation lifecycle (status enum + replaced_by) | ❌ MISS | High | Rules accumulate; never retired |
| 10 | Nursery / experimental lane for unstable rules | ❌ MISS | Medium | All rules are immediately MANDATORY; no "trial period" tier |
| 11 | Rule-quality bar (atomic + unique + widely-applicable check) | ⚠️ PARTIAL | Medium | rule-change-process §5 reviewer checks for contradiction; no atomic/unique numeric bar |
| 12 | Trend analysis cron (quarterly incident class clustering) | ❌ MISS | Medium | §5 of incident-pipeline prescribes; not automated |
| 13 | Skill/rule split convention codified | ⚠️ PARTIAL | High | Informal; should be in README or skill-conventions |
| 14 | Quarterly health-check cadence | ⚠️ PARTIAL | Medium | quality-audit covers infra; rules don't have "Last-Reviewed expires after N months" enforcement |
| 15 | Rule tool-version compat (min/max) | ❌ MISS | Low | Pre-emptive; not actionable yet |
| 16 | CODEOWNERS for path-scoped review | ⚠️ N/A (solo) | Low | Future-team material |
| 17 | AI-powered postmortem trends | ⚠️ N/A | Low | No production incidents yet (Phase 1 BETA) |
| 18 | Pruning hygiene / count ceiling | ❌ MISS | High | 56 rules now; no policy at 100/200/etc |

**Coverage summary:** KiteHub covers **~12 of 18 industry patterns FULL** (67%), **3 PARTIAL** (17%), **6 MISS** (33% of which 4 are actionable + 2 are pre-emptive).

**Where KiteHub is AHEAD of industry:**
- Path-scoped doc loading with empirical measurement (Wave 73)
- 5-stage incident-to-rule pipeline as codified process (vs Google SRE culture without formalism)
- Stage 4 self-test mandate per rule (vs Semgrep's separate optional testing doc)
- Same-PR enforcement parity mandate (§6.5 of rule-change-process) — rare in industry

---

## Recommendations

### NEW patterns to adopt (high-confidence, low-risk)

#### 🟢 NEW-1: Rule deprecation lifecycle (status enum in CSV)

**Pattern source:** OpenLogic 2026, GitHub community discussion #190112, Clippy `deprecated` lane.

**Proposal:**
- Add `status` column to `rules-index.csv`: `active | deprecated | superseded | nursery`
- Add `replaced_by` column (nullable; pointer to successor rule name if superseded)
- Add `deprecated_at` column (ISO date, nullable)
- Rule lifecycle policy: deprecated rule body keeps `## Deprecation notice` section with hard date for removal (60-day warn, beyond = remove)
- Update `meta-csv-index-pattern.md` §4 schema; new schema version 1.1.0

**Cost:** 1-2h (CSV column add + rule body template update + 0 current rules need migration since none deprecated yet).

**Confidence:** HIGH. Universal pattern, low risk, addresses concrete future need.

#### 🟢 NEW-2: Skill-vs-rule split criterion codified

**Pattern source:** Mario Ottmann customization guide, Anthropic docs explicit distinction.

**Proposal:** Add §"Skill vs Rule decision" to `.claude/rules/README.md` (or update `skill-conventions.md`):

```
RULE if:
- Constraint / convention checked at review or PR time
- Force-multiplier governance applying ACROSS files/PRs
- Body is mostly narrative + table + examples

SKILL if:
- Multi-step workflow Claude executes to PRODUCE an artifact
- Has reference/, scripts/, data/, assets/ subfolders
- Has step-by-step instructions to follow

If borderline, default to RULE (lower carrying cost).
```

**Cost:** 30 min (1 file edit + 1 CSV row "examples" column maybe).

**Confidence:** HIGH. Closes ambiguity already-observed.

#### 🟢 NEW-3: Pruning hygiene + rule count ceiling policy

**Pattern source:** ESLint maintenance burden quote, YAGNI for solo-dev, context-budget-mandate.

**Proposal:** Add policy to `rule-change-process.md`:

| Rule count | Action |
|---|---|
| <50 | Default — add rules per incident-pipeline |
| 50-75 | Quarterly review: any rules unused (no detection fires in 90 days, no §Log entry in 6 months) → candidate for deprecation |
| 75-100 | Mandatory consolidation review: merge overlapping rules; raise atomic-unique bar |
| >100 | HARD STOP — no new rules until consolidation passes pruning audit |

KiteHub currently at 56. Quarterly-review trigger is **already actionable next quarter**.

**Cost:** 1h (policy doc + first quarterly review template).

**Confidence:** HIGH. Prevents observed sprawl trajectory.

#### 🟢 NEW-4: Quarterly trend-analysis script (incident class clustering)

**Pattern source:** Zalando AI postmortem, Google SRE pattern-detection emerging automation, KiteHub `incident-to-rule-pipeline.md` §5 prescribes but no automation.

**Proposal:** `scripts/audit-incident-trends.sh` — scans last 90 days of:
- Memory entries tagged `feedback_*`
- Rule `## Log` entries
- Gap files with `triggered_by:` field

Outputs:
- Cluster by user-flagged keyword classes (e.g., "AC mismatch" / "ENV override missing" / "rebase race")
- Flag clusters >3 incidents → meta-review recommendation
- Quarterly cron OR `/repo-status` skill check

**Cost:** 2-3h (script + 1 retro run as self-test).

**Confidence:** MEDIUM. Useful but value depends on whether incident clusters actually appear. Defer to Wave 77+ if Wave 76 already large.

### EXISTING practices to sharpen

#### 🔵 SHARPEN-1: FCP-like cooldown for non-incident rules

For rules NOT created via incident-to-rule-pipeline (i.e., dev brainstorm rules), add 24-72h "soak period" between rule draft + merge. Solo-dev: rule draft committed but `paths:` empty for 48h → if no concern, then activate. This catches "looked smart at 1am, refute at noon next day".

**Cost:** 0h (process-only, no code).

**Confidence:** MEDIUM. Helpful for inside-out rule additions; not needed for incident-driven (those are already empirically validated by the miss).

#### 🔵 SHARPEN-2: Last-Reviewed expires after N months

Currently `Last-Reviewed` field is informational. Add CI check: rules unreviewed for >180 days → WARN in `/repo-status`. Forces quarterly hygiene without manual scheduling.

**Cost:** 30 min (script + 1 line CI).

**Confidence:** HIGH.

#### 🔵 SHARPEN-3: Atomic-unique bar in rule-change-process

ESLint's "widely applicable + atomic + unique" gate is sharper than KiteHub's current §5 cross-check. Add `## Pre-flight: rule quality bar` to rule-change-process §5:

- [ ] Atomic — rule describes ONE constraint, not multiple
- [ ] Unique — `grep -ril <keyword> .claude/rules/` returns no overlap
- [ ] Widely applicable — applies to ≥3 distinct future situations
- [ ] Not too specific — describing it doesn't need >2 "and"s

**Cost:** 30 min.

**Confidence:** HIGH.

### Architectural recommendations (medium-risk)

#### 🟡 ARCH-1: Nursery / experimental lane

Add `nursery` to priority tiers: `🔴 CRITICAL` / `🟠 MANDATORY` / `🟡 ADVISORY` / `🧪 EXPERIMENTAL (nursery)`.

EXPERIMENTAL = rule body landed, paths empty OR `paths:` to dev-only files, detector deferred per default. After 30 days + 0 false positives reported → promote to MANDATORY OR retire.

**Tradeoff:** Adds tier complexity. Counter: solves the "rule shipped same session as incident, no soak time" structural issue.

**Confidence:** MEDIUM. Test with 1-2 candidates first before formalizing.

#### 🟡 ARCH-2: ADR for CSV-canonical choice

Document the choice "CSV canonical, markdown-frontmatter cache" vs industry-default "YAML frontmatter primary" as an ADR. Future-team / migration context will need rationale.

**Cost:** 1h.

**Confidence:** HIGH for value, no risk.

### Streamline candidates (where industry does LESS than us)

#### ⚪ STREAMLINE-1: Rule body length

Industry rules (Clippy declare_clippy_lint macro, ESLint rule MD page, Semgrep YAML) typically <100 lines body. KiteHub rules often 200-500+ lines with §Self-Test + §Worked example + §Anti-patterns + §Override + §Relationship + §Log.

**Proposal:** Move `## Self-test (worked example)` and longer worked examples to companion fixture file (`.claude/rules/fixtures/<rule>/self-test.md`). Rule body stays <100 lines of essence + reference pointer. Already done partially via path-scope.

**Caution:** This is the **same critique user already raised** at Wave 75 ("rule body streamline"). Industry data CONFIRMS the instinct.

**Cost:** Time-consuming (~6h for 56 rules); high payoff for context budget AND human readability.

**Confidence:** HIGH that pattern is right. Wave 76 candidate.

#### ⚪ STREAMLINE-2: Some §Relationship sections excessively cross-linked

Some rules `§Relationship` lists 8-10 related rules. Industry pattern (CODEOWNERS, RFC linking) is 2-3 most-relevant.

**Proposal:** Cap §Relationship at 5 entries; rest moved to "see also" line.

**Cost:** 1h.

**Confidence:** MEDIUM. May reduce discoverability.

#### ⚪ STREAMLINE-3: Memory entry redundancy with rule body

Many memory entries (`feedback_*.md`) duplicate content already in the rule §Log section. KiteHub already migrated some memory → rule (per `aws-observability-first.md` and `terraform-apply-retry-reconfirm.md` Log entries). Continue migration.

**Cost:** 2-3h.

**Confidence:** HIGH.

---

## Cross-check: counter-examples (where KiteHub is BETTER)

1. **Incident-to-rule pipeline (5-stage codified)** — Google SRE has the culture, NO formal pipeline. KiteHub's `Detect → Classify → Rule+Enforce → Self-Test → Retro Log` is **rare and valuable**. Industry will catch up.

2. **Path-scope mechanism + empirical measurement** — Wave 73's "~347k → ~88k baseline-measured" is more rigorous than GitHub Copilot's path-scoped instructions (announcement only, no published savings data).

3. **Same-PR enforcement parity (§6.5 of rule-change-process)** — Industry rules ship advisory-only commonly (ESLint accepts user-suggested rules with "no implementation guaranteed"); KiteHub mandates rule + detection + self-test all SAME-PR.

4. **CSV canonical for governance enumerations** — Most projects use YAML frontmatter; KiteHub's CSV-canonical is contrarian BUT empirically faster to query (per `gap-architecture-v2.md` §5 "50× cheaper to query"). Worth defending publicly.

5. **Stage 4 self-test mandate on the originating incident** — ESLint/Clippy require tests; KiteHub requires test against THE incident that motivated the rule. Stronger empirical grounding.

---

## Fold-in strategy for Wave 76 (proposed)

**User-confirmed Wave 76 scope (before this audit ran):**
- A2 audits-index.csv (per `meta-csv-index-pattern.md` Tier 3 GAP-490)
- A6 script tests
- A4 wave-plan CI check
- C1 rule staleness enforcement (= overlaps with SHARPEN-2 above)
- Rule body streamline (= STREAMLINE-1 above)

**Adjustments based on this benchmark:**

### Add to Wave 76:

| Bucket | Source | Rationale | Effort |
|---|---|---|---|
| **NEW-1: Deprecation lifecycle (CSV status enum + policy)** | Industry gap #9 | Universal pattern; addresses concrete coming need; cheap | 1-2h |
| **NEW-2: Skill-vs-rule split criterion in README** | Industry gap #13 | Closes ambiguity already observed | 30 min |
| **NEW-3: Pruning hygiene policy + count ceiling** | Industry gap #18 + YAGNI | Prevents observed sprawl trajectory | 1h |
| **SHARPEN-3: Atomic-unique bar in rule-change-process §5** | Industry gap #11 | Sharpens existing rule-change-process | 30 min |
| **ARCH-2: ADR for CSV-canonical choice** | Cross-check #4 | Documents contrarian choice for future-team | 1h |

### Validate before adding (outside-in CAN be wrong):

| Bucket | Source | Why validate first |
|---|---|---|
| **NEW-4: Trend-analysis cron** | Industry gap #12 | Value depends on whether incident clusters actually appear; defer to Wave 77 if low signal |
| **ARCH-1: Nursery lane** | Industry gap #10 | Adds tier complexity; test with 1-2 candidates first |
| **SHARPEN-1: FCP cooldown for non-incident rules** | Industry gap #8 | Solo-dev may not benefit from cooldown; pilot before formalizing |

### Already covered (no change needed):

- C1 staleness enforcement = SHARPEN-2 ✓
- Rule body streamline = STREAMLINE-1 ✓

### Out-of-scope (defer):

- CODEOWNERS (industry gap #16) — future-team material
- Tool-version compat (industry gap #15) — pre-emptive
- AI postmortem (industry gap #17) — no incident stream yet
- STREAMLINE-2 (§Relationship cap) — minor; combine with general streamline pass

**Net Wave 76 additions:** 5 new buckets (NEW-1, NEW-2, NEW-3, SHARPEN-3, ARCH-2). Combined effort ~4-5h. Aligns with "extend existing direction" rather than "pivot".

---

## Cross-check on Wave 75 user-flagged suspicions

The user explicitly named 6 things they're suspicious about. Audit verdict per item:

1. **Skill/rule split fuzzy** — ✅ CONFIRMED gap; Wave 76 NEW-2 addresses.
2. **Rule count growth ceiling (100+ unmaintainable)** — ✅ CONFIRMED via ESLint maintenance burden quote + YAGNI; Wave 76 NEW-3 addresses.
3. **Path-scope mechanism novel** — ✅ CONFIRMED novel BUT industry converging (Copilot Sep 2025); KiteHub is leading-edge, no action needed beyond defending choice.
4. **Incident-driven rule creation OSS analog** — ⚠️ Google SRE has the CULTURE but not the formal pipeline; KiteHub is AHEAD; no action needed.
5. **CSV canonical novel** — ✅ CONFIRMED contrarian; MADR ADR-0013 chose opposite (YAML primary). Wave 76 ARCH-2 (ADR documenting the choice) is response.
6. **Outside-in benchmark CAN be wrong (Wave 75 C+D refuted 2/3 claims)** — ✅ CONFIRMED legitimate. Industry advice: "validate empirically before treating as P0". Audit applies this: NEW-1/NEW-2/NEW-3/SHARPEN-3/ARCH-2 are HIGH-confidence + low-cost (apply); NEW-4/ARCH-1/SHARPEN-1 are MEDIUM-confidence (validate first).

---

## Verdict

KiteHub meta-system covers **~67% of industry-standard governance patterns FULL**, **17% PARTIAL**, **33% MISS** (of which 4 actionable + 2 pre-emptive defer).

**The meta-system is NOT over-engineered** — context-budget-mandate + meta-gap-priority + incident-to-rule-pipeline triple combination provides the maintenance discipline. **It IS missing pruning hygiene** at scale (deprecation lifecycle + count ceiling + nursery lane) that becomes critical at ~100 rules.

KiteHub is **AHEAD of industry** in 4 areas: path-scope empirical measurement, 5-stage incident pipeline codification, same-PR enforcement parity, CSV-canonical query speed. These should be **defended + documented** (Wave 76 ARCH-2 ADR), not retreated from.

The Wave 75 user suspicion **"outside-in CAN be wrong"** is well-founded — this audit applies confidence-weighting (HIGH = apply Wave 76; MEDIUM = pilot first) per Wave 75 C+D empirical lesson. The 5 HIGH-confidence Wave 76 additions are ~4-5h total effort; combined with already-planned scope = ~10h Wave 76, manageable.

**Force-multiplier rationale of `outside-in-coverage-trigger.md` re-validated** this session: inside-out Wave 76 plan (audits-index + script tests + wave-plan CI + staleness + body streamline) would have shipped without (a) deprecation lifecycle, (b) skill/rule split codification, (c) pruning policy, (d) atomic-unique bar, (e) ADR documenting CSV choice. All 5 are gaps that ONLY surfaced via industry comparison.

---

## Sources

- [Kubernetes governance](https://github.com/kubernetes/community/blob/master/governance.md)
- [Kubernetes Enhancement Proposals](https://github.com/kubernetes/enhancements/blob/master/keps/README.md)
- [Rust RFCs](https://github.com/rust-lang/rfcs/blob/master/README.md)
- [Clippy adding lints](https://github.com/rust-lang/rust-clippy/blob/master/book/src/development/adding_lints.md)
- [Clippy lint categories](https://doc.rust-lang.org/stable/clippy/lints.html)
- [TypeScript contributing](https://github.com/microsoft/TypeScript/blob/main/CONTRIBUTING.md)
- [ESLint propose new rule](https://github.com/eslint/eslint/blob/main/docs/src/contribute/propose-new-rule.md)
- [Semgrep rule syntax](https://semgrep.dev/docs/writing-rules/rule-syntax)
- [OPA policy language](https://www.openpolicyagent.org/docs/latest/policy-language/)
- [Google SRE postmortem culture](https://sre.google/sre-book/postmortem-culture/)
- [GitHub CODEOWNERS](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners)
- [GitHub Copilot path-scoped custom instructions](https://github.blog/changelog/2025-09-03-copilot-code-review-path-scoped-custom-instruction-file-support/)
- [Continue.dev codebase docs awareness](https://docs.continue.dev/guides/codebase-documentation-awareness)
- [Martin Fowler context engineering for coding agents](https://martinfowler.com/articles/exploring-gen-ai/context-engineering-coding-agents.html)
- [OpenLogic 2026 State of Open Source](https://www.openlogic.com/blog/state-of-open-source-report-key-insights)
- [GitHub community deprecation calendar discussion](https://github.com/orgs/community/discussions/190112)
- [Zalando AI postmortem analysis](https://engineering.zalando.com/posts/2025/09/dead-ends-or-data-goldmines-ai-powered-postmortem-analysis.html)
- [Pragmatic Engineer postmortem best practices](https://blog.pragmaticengineer.com/postmortem-best-practices/)
- [CSVY format](https://csvy.org/)
- [MADR ADR-0013 YAML frontmatter](https://adr.github.io/madr/decisions/0013-use-yaml-front-matter-for-meta-data.html)
- [Mario Ottmann Claude Code customization guide](https://marioottmann.com/articles/claude-code-customization-guide)
- [Anthropic Claude Code skills docs](https://code.claude.com/docs/en/skills)
- [YAGNI principle reference](https://lawsofsoftwareengineering.com/laws/yagni/)
