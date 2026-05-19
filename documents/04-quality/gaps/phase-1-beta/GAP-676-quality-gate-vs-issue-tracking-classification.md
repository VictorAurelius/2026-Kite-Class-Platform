# GAP-676: Quality Gate vs Issue Tracking classification audit per SonarQube pattern

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 META
**Domain:** Meta (output review governance refinement)
**Found:** 2026-05-19 (Wave 98 META audit 3-agent outside-in — External Benchmark agent recommendation)
**Affects:** `output-review-mandate.md` §3 Review Standards Matrix (40+ rows) — classification clarity for downstream prioritization

## Problem

Wave 98 META audit External Benchmark agent surfaced SonarQube industry pattern: **binary distinction between "Quality Gate" (release-blocking conditions) vs "Issue Tracking" (ongoing improvement metrics)** prevents scope creep + every-rule-trends-toward-must-block pressure.

KiteHub `output-review-mandate.md` §3 has 40+ rows (audits, output types, processes) but does NOT explicitly classify each as:
- **Quality Gate (Q-GATE):** binary release-blocking — fail = no merge/deploy
- **Issue Tracking (TRACK):** continuous improvement metric — drift signals + retro input

Without distinction, every rule trends toward "must block" pressure → CI gate inflation → maintainer fatigue. Industry mature SaaS (SonarQube, Linear, Google SRE) explicitly separate these tiers.

Failure-Mode also flagged related: 6/6 most recent rules sit at E1-E2 advisory tier (vs E3-E4 blocking) — KiteHub uses Issue Tracking de facto but lacks taxonomy.

## Proposed Fix

### Step 1: Classify §3 matrix rows

For each of 40+ rows in `output-review-mandate.md` §3, assign tier:

| Row | Q-GATE candidate | TRACK candidate |
|---|---|---|
| Code (two-stage-code-review) | YES (PR-blocking) | — |
| Quality audit reports /110 | — | YES (continuous improvement) |
| API contracts | YES (3-way drift = bug class) | — |
| Persona Coverage Cat 11 | — | YES (qualitative trend) |
| ... (37+ more rows) | per row analysis | per row analysis |

Add new column `Tier: Q-GATE | TRACK | DUAL` to matrix.

### Step 2: Document selection criteria

Q-GATE criteria (per SonarQube pattern):
- Failure has bounded blast radius + clear remediation
- Detection is deterministic (CI script, not human judgment)
- Cost of false positive < cost of regression slip
- Maps to industry-standard quality dimension (security / contract / test coverage)

TRACK criteria:
- Failure is qualitative (UX, narrative quality, persona alignment)
- Detection requires human judgment OR external benchmark
- Sample-based vs comprehensive (UI rubric, persona audit)
- Trend signal more useful than per-PR pass/fail

### Step 3: Audit override semantics per tier

Q-GATE: override requires `<RULE>_OVERRIDE:` trailer + follow-up gap (existing pattern)
TRACK: override implicit (it's a metric, not a gate) — no per-PR override needed

### Step 4: Update related rules

- `incident-to-rule-pipeline.md` — Stage 3 enforcement decision matrix references Q-GATE vs TRACK distinction
- `rule-change-process.md` §5.1 — atomic-unique bar adds "tier classification declared in §3 row"

## Acceptance Criteria

- [ ] §3 matrix new column "Tier" added with values Q-GATE / TRACK / DUAL per row
- [ ] Selection criteria documented per Step 2 (5-7 bullet points each tier)
- [ ] Audit overrides clarified per Step 3 (Q-GATE only, TRACK implicit)
- [ ] Cross-link 2 related rules per Step 4
- [ ] Quarterly retro counter: % of rules at each tier; target balance ~60% TRACK / 40% Q-GATE per industry pattern

## Related

- **Parent:** Wave 98 META audit 3-agent outside-in 2026-05-19 (External Benchmark agent — SonarQube pattern reference)
- **Sister gap:** GAP-675 (premature-rule-guard audit — same parent META audit)
- **Industry reference:** [SonarQube Quality Gates Documentation](https://docs.sonarsource.com/sonarqube-server/quality-standards-administration/managing-quality-gates/introduction-to-quality-gates)
- **Rule under refinement:** `output-review-mandate.md` §3 matrix
- **Rule:** `meta-gap-priority.md` §3 — META P2 (force-multiplier but lower urgency than GAP-675 detector debt)

## Why P2 (defer from immediate ship)

External Benchmark agent explicit recommendation: "File as META P1 follow-up — NOT same wave (avoid scope creep)." Wave 99C this PR ships GAP-675 detector debt closure first; GAP-676 classification refactor can wait Wave 100+ when capacity available.
