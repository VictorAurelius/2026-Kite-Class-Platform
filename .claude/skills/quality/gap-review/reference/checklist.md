# Gap Review Checklist — Detailed Criteria

10 criteria grouped in 4 sections. Reviewer scores each PASS / FAIL / N/A.

Gap passes peer-review when **all mandatory criteria PASS**. Any FAIL blocks status change 🔵 OPEN → 🟡 PLANNED.

---

## Section 1 — Problem Clarity (mandatory)

### 1.1 Problem statement is end-user / outcome observable (mandatory)

- ✅ PASS: "Users cannot log out from the student dashboard — logout button does nothing"
- ✅ PASS: "Business audit score dropped from 70 to 55 because X category lost Y points"
- ❌ FAIL: "Refactor `StudentService`" (implementation detail, not observable outcome)
- ❌ FAIL: "Technical debt in module Z" (no concrete failure mode)

The reader must be able to reproduce OR cite an audit report / screenshot / metric.

### 1.2 Evidence attached (mandatory)

At minimum one of:
- Audit report path (e.g. `documents/04-quality/audits/ui-review/2026-04-20.md`)
- File path + line number + code snippet
- Screenshot path (committed under `documents/04-quality/screenshots/`)
- Metric dump (lighthouse, k6, curl output)
- Quote from user / stakeholder

---

## Section 2 — Root Cause & Scope (mandatory)

### 2.1 Root cause analyzed OR explicitly deferred (mandatory)

- ✅ PASS: "Root cause: `UserRepository.findById` returns `Optional<User>` but controller unwraps without check — NPE when user deleted"
- ✅ PASS: "Root cause: **needs investigation** — symptom visible but reproduction inconsistent"
- ❌ FAIL: Root cause section missing entirely

If deferred, gap should be P0 investigation sub-task only — the fix gap comes later.

### 2.2 State-check performed per `audit-to-gap-pipeline.md` §2.5 (mandatory)

The gap file MUST include either:
- Explicit statement "code state: nothing exists — build from scratch"
- OR a `## Current State (verified YYYY-MM-DD)` section listing existing files / line counts / symbols with evidence

This guards against gaps filed against already-shipped implementations (GAP-190/197 lesson, 2026-04-20).

### 2.3 Duplicate check evidence (mandatory)

Reviewer re-runs on a keyword from the gap title:
```bash
grep -rli "<keyword>" documents/04-quality/gaps/ | head
```

- ✅ PASS: No overlap, OR overlap documented in `## Related` with reason for separate gap
- ❌ FAIL: Another gap covers same scope — merge or link, don't duplicate

---

## Section 3 — Acceptance & Planning (mandatory)

### 3.1 Acceptance criteria are check-boxable (mandatory)

Each AC item must be a binary PASS/FAIL a reviewer can verify.

- ✅ PASS: `- [ ] GET /api/v1/students/{id} returns 200 in <200ms p95 (measured via k6 on staging)`
- ✅ PASS: `- [ ] File .claude/rules/rule-change-process.md exists with §Purpose + §Process + §Frontmatter template`
- ❌ FAIL: `- [ ] Improve performance` (no threshold)
- ❌ FAIL: `- [ ] Better UX` (no metric)

### 3.2 Priority correctly tiered per `meta-gap-priority.md` §3 (mandatory)

Three tiers at each P-level: Meta → Business-Logic → Feature.

Reviewer classifies by scope:
- Meta — touches `.claude/skills/**`, `.claude/rules/**`, `.husky/`, `.github/workflows/`, `audit-gate.py`, CLAUDE.md
- Business-Logic — touches `documents/00-brd/`, `documents/01-business/*/rules.md`, persona docs, pricing/compliance
- Feature — code behaviour, UI, data

A gap mis-labeled Feature when it's Meta deprioritizes a force multiplier → FAIL.

### 3.3 Priority label matches real severity

- 🔴 P0: blocks GA, CI green, audit, or cascades to ≥3 other PRs
- 🟠 P1: blocks growth, significant UX harm, or drifts convention
- 🟡 P2: nice-to-have within quarter
- 🟢 P3: opportunistic / cleanup

### 3.4 Dependencies identified

- `blocked-by:` — which gap/PR/decision must land first
- `blocks:` — downstream gaps this one unlocks
- `related:` — same-area gaps not strictly blocking

Standalone gaps: explicitly state "no blockers" — don't leave the field empty.

### 3.5 ROADMAP registration (mandatory)

Reviewer confirms gap appears in `documents/04-quality/gaps/ROADMAP.md` (epic + sprint assigned). If absent, author must add before status change.

---

## Section 4 — Metadata hygiene

### 4.1 Domain tagged

Exactly one domain: `Frontend`, `Backend`, `DevOps`, `Business`, `Meta / Governance`, `Documentation`, `Testing`, `Security`.

---

## Review Signature Format

Append to gap file's `## Log` section:

```markdown
- YYYY-MM-DD — Peer review by @reviewer — PASS
  - 10/10 criteria passed
  - Notes: {optional brief notes}
```

Or for failed reviews:

```markdown
- YYYY-MM-DD — Peer review by @reviewer — BLOCK
  - Failures: 2.2 (state-check missing), 3.1 (ACs not measurable)
  - Action: author revises; re-review after fix
```

---

## Anti-Patterns

| ❌ | ✅ |
|----|----|
| "Looks fine, approving" without running checks | Work through each of the 10 criteria |
| Approving gap with "Root cause: needs investigation" AND "Proposed fix: full rewrite" | Force author to split — investigation gap first, fix gap after |
| Ignoring state-check because gap looks sensible | State-check is §2.2 — MANDATORY (gates meta governance) |
| Approving Feature-tagged gap that clearly touches `.claude/skills/` | Relabel Meta before approving |
