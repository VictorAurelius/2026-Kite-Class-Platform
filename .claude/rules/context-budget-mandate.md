---
paths:
  - ".claude/rules/**"
  - "CLAUDE.md"
  - ".claude/skills/**/SKILL.md"
---

# Context Budget Mandate — base auto-load < 120k tokens

**Priority:** 🟠 MANDATORY — meta-governance for base context size
**Version:** 1.1.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (PR template + reviewer-checklist + worked self-test on Wave 73 baseline) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies Wave 73 Meta Context Optimization outcome)
**Applies to:** Every change touching `.claude/rules/**/*.md`, `.claude/skills/**/SKILL.md`, hoặc `CLAUDE.md` (artifacts that auto-load into base context every session)

---

## 1. The Rule

> **Base auto-load context per session PHẢI giữ <120k tokens (target ~100k).** Rules auto-load thêm ≥1k tokens vào base context PHẢI dùng `paths:` frontmatter (path-scoped deferred-load) HOẶC include `## Auto-load justification` section explain rationale.

Wave 73 (2026-05-14) baseline measured: 54 `.claude/rules/*.md` auto-loading without `paths:` = ~237k tokens trong base context. Combined with CLAUDE.md + memory entries → ~347k tokens (~34% context budget) per fresh `/start-session`. Per `meta-gap-priority.md` §3 Meta-P0 force-multiplier — every future session benefits permanently from path-scoping.

---

## 2. What counts as base context

| Source | Loaded when | Token measurement |
|---|---|---|
| `CLAUDE.md` (project root) | Every session, every turn | `wc -w CLAUDE.md` × 1.3 ≈ tokens |
| `.claude/rules/*.md` không có `paths:` frontmatter | Every session | `wc -l .claude/rules/*.md` × ~7 ≈ tokens |
| `.claude/rules/*.md` CÓ `paths:` frontmatter | Chỉ khi Claude đọc file matching glob | Deferred — KHÔNG count base |
| `~/.claude/projects/*/memory/*.md` | Every session (auto-load) | `wc -w` × 1.3 |
| `~/.claude/projects/*/memory/MEMORY.md` | Every session | (always loaded) |
| `.claude/skills/**/SKILL.md` frontmatter (description) | Every session (~100 tokens each) | description string only |
| `.claude/skills/**/SKILL.md` body | Khi skill activated (NOT base) | Deferred |

Base context = sum of "Every session" rows above.

---

## 3. Per-rule check

Mỗi rule mới hoặc rule edit MUST satisfy ONE of:

### 3.1 Path-scoped (preferred)

YAML frontmatter ở top:

```yaml
---
paths:
  - "<glob-1>"
  - "<glob-2>"
---
```

Path-scoped rules KHÔNG count vào base context — chỉ load khi Claude đọc file matching glob.

### 3.2 Auto-load justification (rare)

Rules auto-load mọi session phải:
- Có Priority `🔴 CRITICAL` (governance/meta level)
- HOẶC có section `## Auto-load justification` giải thích why must always-load (vd: `meta-gap-priority.md`, `incident-to-rule-pipeline.md`, `rule-change-process.md`, `output-review-mandate.md`)
- Tổng số CRITICAL auto-load rules giữ <15 (per `.claude/rules/README.md` Tier convention)

### 3.3 Hook-covered (alternative for non-file-scope rules)

Rules trigger qua tool patterns (Bash command, edit pattern) thay vì file read:
- Implement enforcement trong `.claude/hooks/*.py` (PreToolUse / PostToolUse / Stop)
- Rule body documents hook reference
- Rule frontmatter SHOULD have `paths:` empty (hook handles trigger)

---

## 4. Banned patterns

| ❌ Banned | ✅ Required |
|---|---|
| Add new MANDATORY rule >1k tokens không có `paths:` không có justification | Add `paths:` glob OR `## Auto-load justification` section |
| Multiple narrative `Last-Reviewed` updates → bloat base context | Path-scope rule first; subsequent edits don't grow base |
| Migrate hook → auto-load rule "for visibility" | Hooks là deterministic enforcement; rules là human-readable narrative — không trộn |
| CRITICAL count >15 ("everything is critical") | Reserve CRITICAL for cross-cutting governance only; downgrade to MANDATORY + path-scope when scope narrow |
| Add `paths:` glob too broad (`**/*.md`) — every doc edit triggers load | Narrow scope to actual trigger files (vd `documents/04-quality/gaps/**`) |

---

## 5. Worked self-test (Wave 73 baseline)

**Pre-Wave-73 state (2026-05-14):**
- 54 rules auto-loaded
- ~237k tokens trong base context (rules alone)
- + CLAUDE.md ~25k + memory ~85k = ~347k total base load
- /start-session consumed ~34% of 1M context window

**Post-Wave-73 target:**
- 14 CRITICAL auto-load (~50k tokens)
- ~30 MANDATORY path-scoped (deferred — load only when relevant file in context)
- ~10 MANDATORY hook-covered (no auto-load)
- Base context drop: ~237k → ~50k rules + ~25k CLAUDE.md + ~85k memory = **~160k** (vs ~347k pre)
- Net savings: **~187k tokens (~18% context window per session)**

Per Bucket E baseline measurement (TBD post-merge of all buckets) — actual savings will be reported in `documents/04-quality/audits/meta/2026-05-14-wave-73-context-budget-baseline.md`.

→ Rule fires correctly: target met if Bucket E measures <120k base context. ✅

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching `.claude/rules/**/*.md`:
- [ ] New rule: `paths:` frontmatter present? OR `## Auto-load justification` section? OR explicit hook-covered note?
- [ ] Existing rule edit growing >500 lines: re-evaluate path-scope còn đúng?
- [ ] CRITICAL priority justified (cross-cutting governance, NOT domain-specific)?
- [ ] `rules-index.csv` `path_trigger` column matches `paths:` value?

### 6.2 Memory auto-load

Bucket D PR ships `feedback_meta_context_optimization.md` reminder per session — Claude reviews context-budget mandate before adding new rules.

### 6.3 Detector (IMPLEMENTED 2026-05-31 — defer window elapsed)

`scripts/check-context-budget.sh` (CI job `context-budget` trong `quality-rules-skills.yml`) — enforces two gates on always-load rules (rules WITHOUT `paths:` frontmatter), byte-based (deterministic, ~4 bytes ≈ 1 token proxy):

| Gate | Threshold | CI behavior |
|---|---|---|
| **TOTAL ceiling** — sum of always-load rule bytes | WARN ≥ 250000 B (~62k tok) / FAIL ≥ 300000 B (~75k tok) | FAIL blocks PR (exit 1) |
| **PER-RULE §3.2** — always-load rule ≥4000 B (~1k tok), NOT Priority CRITICAL, no `## Auto-load justification` | any violation | FAIL blocks (must `paths:`-scope / justify / hook) |

Thresholds tunable via env (`WARN_TOTAL` / `FAIL_TOTAL` / `MIN_BYTES` / `CI_FAIL_PER_RULE`). This is the durable guard against per-session start-context creep — any new always-load rule pushing total over ceiling FAILs CI, forcing path-scope (the §3.1 default). Baseline at implementation: 13 always-load rules / 232665 B (~58k tok) — PASS.

### 6.4 Override mechanism

Genuine exception (rule must auto-load mọi session despite size):

```
git commit -m "...
CONTEXT_BUDGET_OVERRIDE: <reason — explain why path-scope/justification both N/A>"
```

Trailer logged in quarterly retro. Pattern frequency >5%/quarter triggers meta-review.

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| "Make it CRITICAL just to be safe" | Default MANDATORY + path-scope; CRITICAL requires explicit justification |
| Add big §Self-test + §Worked example to MANDATORY rule auto-loaded | Move §Self-test to `tests/` fixture; rule body keeps essence |
| Skip `paths:` "because rule is small (~500 lines)" | Even small rule × 54 rules = 54 × 500 = 27k lines × 7 tokens = ~189k tokens |
| Path-scope `paths: ["**/*.md"]` (too broad — every doc edit triggers) | Narrow scope: `documents/04-quality/gaps/**` |
| Allow MEMORY.md to grow unbounded | Per existing memory governance — keep MEMORY.md <200 lines |

---

## 8. Relationship to other rules

- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — this rule + reviewer-checklist + worked self-test all ship same PR
- **`meta-gap-priority.md`** §3 Force-multiplier — context optimization is Meta-P0 (touches every session)
- **`meta-csv-index-pattern.md`** §3 — `rules-index.csv` `path_trigger` column tracks `paths:` per rule
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-14 user-flagged miss "/start-session 34% context" applied through 5-stage pipeline
- **`output-review-mandate.md`** §3 — adds row "Context budget" tracking review standard
- **`.claude/rules/README.md`** — Tier convention (CRITICAL auto / MANDATORY path-scoped / hook-covered)
- **`feedback_meta_context_optimization.md`** (memory, Bucket D) — per-session reminder

---

## 9. Log

- **2026-05-31** (v1.1.0): MINOR — §6.3 detector IMPLEMENTED (was deferred ≥7 days; defer window elapsed — rule created 2026-05-14, now 17 days). Shipped `scripts/check-context-budget.sh` + CI job `context-budget` in `quality-rules-skills.yml`: TOTAL ceiling (WARN 250k B / FAIL 300k B) + PER-RULE §3.2 gate (always-load ≥1k tok must be CRITICAL OR have `## Auto-load justification`). Triggered by user-flagged concern 2026-05-31 "sợ qua nhiều session, start-context lại tăng" — durable guard against always-load creep. Paired same batch: 11 rules path-scoped + output-review-mandate streamlined + cross-flow-bug-class-sweep gained justification (now 100% §3.2-compliant). Baseline 232665 B / 13 rules PASS. MINOR per §4 (enforcement activation, new §6.3 ceiling thresholds — could BLOCK a future PR that previously passed). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — activates previously-deferred enforcement; no constraint loosening; existing rules grandfathered, gate applies to future always-load growth).
- **2026-05-14** (v1.0.1): PATCH — thêm `paths:` frontmatter — Wave 73 miss fix (rule này nằm trong 13 MANDATORY rules wave plan §3 Scope bỏ sót, vẫn auto-load base context dù scope rule có path trigger rõ ràng). PATCH bump per `rule-change-process.md` §5 — additive frontmatter, no constraint change, deferred-load khi no matching file in context. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve). Scope: context budget governance (rule/skill edits).
- **2026-05-14 (v1.0.0):** Rule created. Triggered by user-flagged 2026-05-14 miss "/start-session tốn ~34% context (~347k tokens)" — Wave 73 Meta Context Optimization (per `meta-gap-priority.md` §3 Meta-P0 force-multiplier). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (no rule mandates context budget; CLAUDE.md + 54 rules + memory bloat) → Rule+Enforce ✓ (this file + paired same-PR with `output-review-mandate.md` §3 row + `rules-index.csv` row + CLAUDE.md tier note + memory entry per `rule-change-process.md` §6.5) → Self-Test ✓ (§5 worked example on Wave 73 baseline 237k → target <120k) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifies Wave 73 outcome; no constraint loosening; existing rules grandfathered until next refresh; rule applies prospectively to new rules từ next session). Detector wiring deferred ≥7 days per `incident-to-rule-pipeline.md` premature-rule guard; enforcement = reviewer-checklist + memory + worked self-test sufficient cho v1.0.0.
