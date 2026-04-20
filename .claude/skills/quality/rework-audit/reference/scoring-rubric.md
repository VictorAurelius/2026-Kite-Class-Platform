# Rework Severity Rubric

Reference for `/rework-audit` skill. Read when assigning severity to rework items.

## Rubric

### 🔴 P0 — Rework URGENT (block next release)

Any of:

- **Functional regression** — feature worked before PR, broken after (verified by re-running AC)
- **Broken acceptance criteria** — PR closed gap X but gap AC not actually met
- **Security / compliance miss** — new endpoint without auth, PII leak, license violation, SVG-XSS pattern
- **Data loss risk** — migration without rollback, missing constraint
- **Production incident root cause** — PR correlates with prod alert within 7 days

**Action:**
- Create gap IMMEDIATELY with P0 priority
- Hotfix PR on same wave branch if possible
- Notify wave owner
- Document in incident log

### 🟠 P1 — Rework NEEDED (next sprint)

Any of:

- **Missing tests** — code added without unit/integration test coverage
- **Docs drift** — `rules.md` / `api-contract.md` / `use-cases.md` didn't update alongside code
- **Incomplete audit** — required audit per `post-wave-audit-mandate.md` §2.1 wasn't run
- **Pattern violation** — anti-pattern from `.claude/rules/design-patterns.md` (God service, primitive obsession, etc.)
- **Calibration drift** — self-audit score >15 pts higher than retroactive specialist audit
- **Business-logic correctness gap** — value hardcoded correctly matches spec BUT spec itself is wrong (feeds Business-Logic tier gap)

**Action:**
- Gap created, assigned to next sprint per `audit-to-gap-pipeline.md` §5
- ROADMAP updated
- Consider meta-gap if pattern repeats (3+ similar rework items)

### 🟡 P2 — Rework NICE-TO-HAVE (quarterly batch)

Any of:

- **Style inconsistency** — naming, formatting, import order
- **Comment gaps** — public API missing javadoc/TSDoc
- **Minor refactor opportunity** — method too long, magic number, etc.
- **Non-critical i18n miss** — English leak in low-traffic admin screen
- **Minor docs typo / formatting**

**Action:**
- Batch multiple P2 into single cleanup PR per sprint/quarter
- No individual ROADMAP entry — aggregate tracking

## Scoring Decision Tree

```
Is there user-facing behavior broken?
├─ YES → P0
└─ NO → Is there AC unmet / audit missing / security risk?
        ├─ YES → P0
        └─ NO → Is there test/doc gap / pattern violation?
                ├─ YES → P1
                └─ NO → P2
```

## Calibration Guidance

Per memory `feedback_audit_calibration`: self-audit overstates specialist audit by 15-20 pts. When re-auditing:

- **Delta 0-10 pts:** within normal calibration drift, NO rework needed
- **Delta 11-20 pts:** normal specialist delta, classify P2 unless specific issue found
- **Delta 21-30 pts:** likely degradation, classify P1
- **Delta >30 pts:** clear degradation, classify P0 (and investigate session conditions)

**Baseline audits (first-run) are exempt** — they're honest, not regression (per §5 of `output-review-mandate.md`).

## Rework ID Convention

Format: `RW-{YYMMDD}-{NN}` (e.g., `RW-260420-01`).

Mapping to gap files:

- Each rework item → one gap file `GAP-XXX-rework-PR{N}-{topic}.md`
- Rework ID recorded in gap's `Related:` section
- Batch P2 items → single gap `GAP-XXX-rework-batch-YYMM-Q{quarter}.md`

## Reporting Template

When output rework backlog, use:

```markdown
# Rework Audit Report — {scope} — {date}

**Audited:** {N} PRs (#{range})
**Candidates (score ≥5):** {M}
**Rework items found:** {total} ({p0} P0, {p1} P1, {p2} P2)
**Heuristics version:** v1

## Items

| ID | PR | Severity | Pattern | Gap file | Fix PR |
|----|-----|----------|---------|----------|--------|
| RW-260420-01 | #362 | P0 | Missing auth | GAP-XXX | TBD |
| RW-260420-02 | #367 | P1 | Docs desync | GAP-XXX | TBD |

## Patterns Observed

- {Top pattern 1 — N occurrences}
- {Top pattern 2 — N occurrences}

## Recommended Actions

1. Immediate: fix P0 items via hotfix PR
2. Next sprint: schedule P1 gaps
3. Meta: if pattern repeats (≥3), consider new rule or skill

## Context Conditions

(If session-lock data available)
- Candidates clustered in: {date range, session IDs}
- Avg turn count at merge: {N}
- Compact events nearby: {Y/N}
```

## Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Re-audit every merged PR indiscriminately | Use heuristic filter (score ≥5) — targeted |
| Fix rework directly without gap | Every item → gap via `audit-to-gap-pipeline.md` |
| Count calibration drift as regression | <20 pts delta = normal — save P1+ for true issues |
| Treat docs-only PR as high-risk | Apply false-positive guards from `heuristics.md` §5 |
| Over-engineer Phase 2 before Phase 1 pilot | Ship skill first, tune weights after pilot evidence |
