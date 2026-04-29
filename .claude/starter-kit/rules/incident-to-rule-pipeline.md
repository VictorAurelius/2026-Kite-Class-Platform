# Incident → Rule Pipeline — turning misses into permanent guards

**Priority:** 🔴 CRITICAL — meta-governance preventing the same miss twice
**Version:** 1.0
**Created:** 2026-04-27
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every user-flagged miss, regression, "we caught this manually," or "should have a rule for this" comment in conversation, PR review, or retro

---

## 1. The Rule

> **Every miss the user/reviewer catches must convert into a permanent guard within the same session it was discovered. The guard is a rule + enforcement mechanism + self-test, not a memory entry.**

When a human spots a problem the existing tooling didn't catch, the existing tooling has a coverage gap. Memory entries record what happened; rules + enforcement prevent recurrence. Memory alone is insufficient — context flushes between sessions, future Claude doesn't read every memory file, and the same class of miss reoccurs.

---

## 2. The Pipeline (5 stages)

```
Stage 1 — DETECT       (user/reviewer/audit flags a miss)
       ↓
Stage 2 — CLASSIFY     (Is it covered by an existing rule? Is the rule enforced?)
       ↓
Stage 3 — RULE+ENFORCE (Add or extend a rule WITH detection mechanism in same PR)
       ↓
Stage 4 — SELF-TEST    (Synthetic fixture demonstrating the rule fires on the original miss)
       ↓
Stage 5 — RETRO LOG    (Memory entry + ROADMAP entry + cross-link from existing rules)
```

### Stage 1 — Detect (informal)

A miss is *anything* a human catches that the tooling could plausibly have caught:
- "Hey this PR shipped without an X check"
- "We deferred this work but marked it DONE — that's wrong"
- "Why didn't CI flag this?"
- "I noticed Y is inconsistent across files"
- "This used to break in [date], why didn't we add a guard?"

Vagueness OK at this stage. Move to Stage 2 immediately — don't drop into a "we'll get to it" backlog. Backlogs are where misses go to die.

### Stage 2 — Classify (5-minute audit)

Three questions, in order:

1. **Is there an existing rule that mentions this case?** `grep -ril <keyword> .claude/rules/`. If yes:
   - Does the rule have an enforcement mechanism (hook, CI check, skill)? If no → add one (Stage 3).
   - Is the enforcement actually wired (cron, PR template, audit-gate.py)? If no → wire it (Stage 3).
   - Is the enforcement covering this specific case? If no → extend it (Stage 3).
2. **Is there an existing skill or matrix entry that should have caught it?** `grep -ril <keyword> .claude/skills/`. If yes → extend the skill's detection logic (Stage 3).
3. **Is the miss actually a case existing rules disagree on?** Cross-reference. If yes → file a separate gap to reconcile rules (don't paper over with new rules).

Output: a 1-line classification — "missing rule," "rule exists but no enforcement," "skill exists but doesn't catch this case," or "rule conflict."

### Stage 3 — Rule + Enforcement (same PR — non-negotiable)

Per `rule-change-process.md` §6.5 (Enforcement Parity Mandate, paired with this rule), every rule MUST ship with a detection mechanism in the same PR. Concretely:

| New rule type | Required enforcement |
|---------------|---------------------|
| File-state invariant (e.g. gap status matches AC) | Skill detection rule + check-docs.sh-style pre-merge script |
| Diff-pattern guard (e.g. banned phrases in commits) | Hook in `.husky/` OR `audit-gate.py` AUDIT_RULES entry |
| Process step (e.g. "X must happen before Y") | PR template checkbox + reviewer-checklist line |
| Cadence requirement (e.g. "audit every 7 days") | Cron job, scheduled workflow, OR `/repo-status` skill check |

Advisory-only rules (no detection) **are not allowed**. They drift, they're forgotten, and they re-trigger Stage 1 weeks later when someone hits the same miss.

### Stage 4 — Self-Test (synthetic fixture)

Before the PR merges, the new detection must be exercised on the original miss:

```bash
# Reconstruct the scenario that caused the miss
mkdir -p /tmp/incident-fixtures
cat > /tmp/incident-fixtures/repro.md <<'EOF'
<the exact pattern that wasn't caught originally>
EOF

# Run the new check against it
bash .claude/skills/...check-docs.sh --branch=fixture-test
# Expect: FAIL with the new rule's message
```

The PR description quotes the self-test output. If self-test doesn't fire on the original incident, the rule isn't fixing the incident — back to Stage 3.

### Stage 5 — Retro Log

After the rule lands, three updates close the loop:

1. **Memory entry**: `feedback_<topic>.md` describing the miss, why it was missed, and which rule now prevents it. One paragraph max — pointers, not detail.
2. **`MEMORY.md` index entry**: one-line link.
3. **ROADMAP**: log entry under `## 🎯 Current Status Snapshot` referencing the incident → rule chain.
4. **Cross-link**: every existing related rule gains a `## Related` line pointing to the new rule. Future rule readers see the cross-reference.

---

## 3. When to NOT use this pipeline

| Case | Why exempt | Do this instead |
|------|-----------|-----------------|
| One-off content typo | Not a class of misses | Fix the typo, no rule needed |
| User preference (e.g. language) | Already in CLAUDE.md | Memory entry sufficient |
| External tooling bug | Outside repo control | File upstream issue, memory entry |
| Audit finding with single-instance fix | Use `audit-to-gap-pipeline.md` instead | That pipeline is for findings; this is for *coverage gaps that produce findings* |

The line: if you can imagine the same class of miss happening again with different specifics, run the pipeline. If it's a one-off, don't.

---

## 4. The "rule about rules" anti-pattern (and why this one is OK)

Adding a rule to govern when to add rules sounds like infinite regress. It's not, because:

- Stage 2 explicitly checks if existing rules cover the case before adding a new one.
- Stage 3 mandates enforcement, so this rule self-applies — its enforcement is the §5 checklist below + paired updates landed in same PR as this rule.
- Stage 4 demands self-test, so the rule cannot ship as advisory.

If a meta-meta-miss is found ("this incident-to-rule pipeline missed an incident") → run the pipeline on itself. That's the test.

---

## 5. Enforcement

This rule's own enforcement (Stage 3 self-application):

- **PR template checkbox** (under `Output Review Checklist`): "If this PR addresses a user-flagged miss, the matching rule + enforcement is included in this PR."
- **`session-docs-check` Rule 14** (paired follow-up — see §8 Open Items): detects PR description containing phrases like "missed in", "user caught", "should have flagged", "regression noticed" without a corresponding new/modified rule file → WARN with reference to this pipeline.
- **Quarterly retro**: review the last 90 days of memory entries tagged `incident-driven`. For each, verify a rule landed within the same session. Any orphan memory entries (incident logged but no rule shipped) → backfill or document why exempt.
- **Reviewer manual**: when reviewing a PR labeled "miss-fix" or with an incident in the description, verify §2 Stage 3-5 artifacts in the diff.

### Override mechanism (rare)

If reviewer + author agree the miss genuinely doesn't warrant a rule (e.g. genuinely one-off content, or upstream-only):

```
git commit -m "...
INCIDENT_NO_RULE: <reason — explain why pipeline is exempt>"
```

Trailer logged in quarterly retro. Pattern frequency triggers a meta-meta review.

---

## 6. Concrete worked example — silent-deferral incident

The case that motivated this rule.

**Stage 1 — Detect:** User flagged "Gap marked DONE but live screenshots deferred to manual run; that's a miss."

**Stage 2 — Classify:**
- Existing rules touched: `output-review-mandate.md` §3 (no row for gap closure), `audit-to-gap-pipeline.md` (filing pipeline, not closure), `session-docs-check` (Rules 1-12, no DONE check).
- Classification: **missing rule + missing skill matrix entry**.

**Stage 3 — Rule + Enforcement:**
- New rule: `gap-done-discipline.md` (file-state invariant for gap closure).
- Enforcement: `session-docs-check` Rule 13 in `doc-rules-matrix.md` + `check-docs.sh` detection logic (DONE flip + AC check + banned-phrase scan + override trailer).
- Cross-cuts: `output-review-mandate.md` §3 row "Gap closure" added; `rule-change-process.md` §6.5 added (formalizes "rule + enforcement same PR" mandate).

**Stage 4 — Self-Test:** Three synthetic gap files (good, unchecked AC, deferred Log) committed temporarily; `check-docs.sh` correctly returned PASS / FAIL Rule 13.1 / FAIL Rule 13.2.

**Stage 5 — Retro Log:** Memory `feedback_incident_to_rule_pipeline.md` saved; ROADMAP entry cross-references incident → rule chain; existing rules `audit-to-gap-pipeline.md`, `output-review-mandate.md` gain `Related` lines pointing here.

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Save memory entry, plan rule "later" | Rule lands in same session as the discovery |
| Ship advisory rule with no enforcement | Pair with hook/skill/template — no exceptions |
| Skip self-test because "the logic is obvious" | Self-test catches typos, off-by-one in regex, edge cases |
| Add new rule that contradicts an existing one | Stage 2 reconciliation first; new gap if rules conflict |
| Re-classify an incident as "edge case" to avoid rule work | Edge cases are exactly when rules earn their cost |
| Add rule but skip the cross-link updates | Future readers won't find the rule |

---

## 8. Open Items / Follow-ups

This rule itself ships with a known short-term gap: **Rule 14 (PR description scan for incident-keywords) is mentioned in §5 but not yet implemented**. Tracked here intentionally so the next session has a concrete first task:

- [ ] Add `session-docs-check` Rule 14: scan PR description / commit messages for incident keywords (`missed in`, `user caught`, `regression noticed`, `should have flagged`) and check that the diff contains a new or modified `.claude/rules/*.md` file. WARN if missing.

This is a genuine PARTIAL — per `gap-done-discipline.md` §3, Rule 14 deferred to follow-up GAP gets filed as a normal gap (not as deferral inside a DONE flip).

---

## 9. Relationship to other rules

- **`gap-done-discipline.md`** (sister rule, same PR) — concrete instance of the pipeline applied
- **`rule-change-process.md`** §6.5 (Enforcement Parity Mandate, same PR) — extension that formalizes Stage 3
- **`output-review-mandate.md`** §3 matrix Gap closure row (same PR) — completes the §3 coverage
- **`audit-to-gap-pipeline.md`** — sister pipeline for audit findings (this rule is its meta-cousin for coverage gaps)
- **`meta-gap-priority.md`** — meta-rule about prioritizing meta gaps; this rule produces meta gaps when triggered

---

## 10. Log

- **2026-04-29 (v1.0 upstream import):** Imported into starter-kit v2.3.0 from project source. Local project remains source of truth; upstream version may diverge as starter-kit evolves separately.
- **2026-04-27 (v1.0):** Rule created. Triggered by user comment "có quy trình khi thêm 1 skill, 1 rules vào dự án chưa, mà vẫn miss kiểu này" — pointing out that we have processes to ADD rules but no process to DETECT what rules are missing. Paired in same PR with `gap-done-discipline.md`, `rule-change-process.md` §6.5, `output-review-mandate.md` §3 row, and `session-docs-check` Rule 13 detector. Rule 14 (PR-description scan) explicitly deferred to follow-up gap to demonstrate `gap-done-discipline.md` §3 PARTIAL exit-ramp working in practice.
