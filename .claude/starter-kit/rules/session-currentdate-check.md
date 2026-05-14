# Session currentDate Check Before Dating Artifacts

**Priority:** 🟠 MANDATORY — date-stamping discipline
**Version:** 1.0.0
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every artifact this agent writes that contains a date field — rule frontmatter (`Last-Reviewed`, `Created`), gap files, session logs, memory entries, audit reports, ADR dates, wave plan frontmatter, skill SKILL.md retro sections, ROADMAP entries

---

## 1. The Rule

> **At session start, the harness injects `currentDate` into the system context (e.g., "Today's date is 2026-05-07."). Read this value FIRST and use it for all date fields. Do NOT infer the current date from filenames, recent session logs, or existing frontmatter — those may be forward-dated planning docs.**

CI `Rule frontmatter` job (`scripts/check-rule-frontmatter.sh`) already enforces `Last-Reviewed ≤ today` for `.claude/rules/*.md`. This rule generalizes the discipline to all date-stamped artifacts.

---

## 2. Where to find currentDate

The harness injects today's date into auto-loaded system context near top of MEMORY.md or in a system reminder. Pattern:

```
# currentDate
Today's date is YYYY-MM-DD.
```

If present → use that value verbatim. If absent → ask user via AskUserQuestion: "Confirm today's date for date-stamped artifacts?"

---

## 3. Banned inference sources

| ❌ Don't infer from | Why banned |
|---|---|
| Repo filenames using ISO date (e.g., `wave-2026-05-08-41-...md`) | May be forward-dated planning doc; filename ≠ session date |
| Recent session log entries | May have been written by other sessions or forward-dated |
| Existing file frontmatter dates | Could be drafts; could be future-effective ADRs |
| Git commit dates from main | Reflects merge time, not author session time |
| `date` shell command without verifying timezone matches harness | UTC vs local mismatch can drift by 1 day at edges |

The harness `currentDate` value is the single source of truth.

---

## 4. Forward-dated content rules

### 4.1 Allowed forward-dated

- **Plan-only artifacts** — wave plans, deploy runbooks: `Created: <plan-draft-date>` may reflect when plan was drafted (slight forward OK)
- **Decision deltas / ADRs scheduled but not effective yet** — ADR with `Effective: 2026-06-01` for future-dated decision
- **Scheduled cron / scheduled-deploy artifacts** — schedule-bearing fields may reference future
- **Future-dated commits from other sessions on same repo** — not authored by this session

### 4.2 NOT allowed forward-dated

- **Rule `Last-Reviewed`** — CI-enforced via `scripts/check-rule-frontmatter.sh`
- **Audit artifacts** (`documents/04-quality/audits/**`) — must reflect actual run date
- **Session log entries** — must reflect actual session date
- **Memory entries** (`*.md` under user-memory dir) — must reflect actual capture date
- **Gap file Log entries** — actual log-write date
- **Skill SKILL.md retro sections** — actual retro-write date

---

## 5. Decision flow

Before writing any date field:

1. **Did I confirm today's date from `currentDate` context this turn?**
   - YES → use that value
   - NO → check now (scan auto-loaded MEMORY.md / system reminder for `currentDate`)
2. **If currentDate not visible →** AskUserQuestion: "Confirm today's date for date-stamped artifacts?"
3. **Is the field a `Created` for a plan-only artifact (§4.1)?** May use plan-draft date if intentional
4. **Is the field in §4.2 banned-forward list?** MUST equal currentDate (or earlier for historical entries)
5. **Write the field with verified value.**

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Copy date from `wave-2026-05-08-41-foo.md` filename and write `Last-Reviewed: 2026-05-08` | Read `currentDate` context; use that value |
| Assume "today" from recent commit message dates | Commit messages reflect prior sessions |
| Write rule `Last-Reviewed: <future-date>` to "stay ahead" | CI fails the build; future = invalid |
| Skip `currentDate` check "because I just looked yesterday" | New session = re-check; date may have advanced |
| Use `date +%Y-%m-%d` shell output without verifying it matches harness UTC | Harness `currentDate` is canonical |
| Forward-date Log entries to match planned merge date | Log entries reflect actual write time |

---

## 7. Enforcement

### 7.1 CI `Rule frontmatter` job (already active)

`.github/workflows/script-quality.yml` job `rule-frontmatter` runs `scripts/check-rule-frontmatter.sh` on every PR touching `.claude/rules/*.md`. Validates `Last-Reviewed ≤ today` (UTC). Fails build on forward-date.

### 7.2 Memory auto-load (per-session)

Memory entry `feedback_session_currentdate_check.md` (now a pointer to this rule) loads at session start, reminding Claude to check `currentDate` before any date write.

### 7.3 Reviewer manual (active now)

Pre-merge PR review for diffs touching audit artifacts / gap files / session logs / memory entries: reviewer scans for forward-dated entries and confirms either (a) date ≤ today, OR (b) §4.1 forward-allowed exception applies + intent documented.

### 7.4 Self-detection (in-turn)

Before writing any line containing `YYYY-MM-DD` pattern in date-stamping context, agent runs §5 decision flow mentally. If decision flow not run → high probability of forward-date drift.

### 7.5 PR-template item (deferred)

Future enhancement — `.github/PULL_REQUEST_TEMPLATE.md` checkbox: "Date-stamped artifacts (gap files, audit reports, session logs) reflect actual session date per `session-currentdate-check.md`." Tracked as future enhancement; CI + memory + reviewer manual sufficient for solo-dev mode.

---

## 8. Self-test (worked example — 2026-05-07 session)

**Scenario:** 2026-05-07 session shipped many artifacts with `2026-05-08` dates.

**What happened:**
- Wave 41 plan file already existed in repo with filename `wave-2026-05-08-41-...md` (forward-dated handoff written earlier session)
- Agent treated filename as "today's date" signal
- Forward-dated leaks into:
  - `agent-aws-access.md` frontmatter (caught by CI `Last-Reviewed: 2026-05-08 is in the future`)
  - GAP-438 file Log entry
  - Wave 42 plan filename + frontmatter
  - Session log entries
  - 5 memory entries
  - Skill SKILL.md retro section

**Detection:** CI `Rule frontmatter` validator on PR #995 caught the rule frontmatter forward-date. Other forward-dated content (filenames + body text) NOT validated by CI; left as session record.

**Counterfactual with rule:** §5 decision flow at start of session →
1. Check `currentDate` context → "Today's date is 2026-05-07."
2. Write all date fields as `2026-05-07`
3. Wave 42 plan filename uses `2026-05-07` (or kept as planning forward-date with intent documented per §4.1)
4. Zero CI failures, zero forward-date drift

**Verdict:** rule fires correctly on the original incident. CI `Rule frontmatter` job catches the rule-file slice; this rule generalizes to non-CI-validated artifacts. ✅

---

## 9. Override mechanism

Genuine forward-date for §4.1 plan-only artifacts:

```
git commit -m "...
SESSION_CURRENTDATE_FORWARD: <field — reason — e.g., 'wave plan Created field reflects 2026-05-08 plan-draft date for handoff to next session'>"
```

Trailer logged. Pattern frequency >5% triggers meta-review (likely the rule's §4.1 list mis-defined).

---

## 10. Relationship to other rules

- **`rule-change-process.md`** §3 — frontmatter Last-Reviewed format; this rule is the date-source discipline for that field
- **`scripts/check-rule-frontmatter.sh`** — CI script that catches the rule-file slice; this rule generalizes to non-rule artifacts
- **`gap-done-discipline.md`** §2 — gap closure Log entries must reflect actual close date; this rule prevents forward-date in those entries
- **`audit-to-gap-pipeline.md`** §3 (gap file template) — `Found: [date]` field must be actual audit date
- **`output-review-mandate.md`** §3 — every audit artifact preserves evidence with date; this rule covers the date-correctness slice
- **`incident-to-rule-pipeline.md`** — this rule originated from 2026-05-07 forward-date drift incident caught by CI; codified per 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory auto-load + existing CI `Rule frontmatter` job + reviewer manual all in place
- **`feedback_session_currentdate_check.md`** (memory pointer to this rule)

---

## 11. Log

- **2026-05-07 (v1.0.0):** Migrated from session memory `feedback_session_currentdate_check.md` per user request "memory persistence strategy = migrate to .claude/rules/ for git-tracked durability". Original incident: 2026-05-07 session shipped artifacts with `2026-05-08` dates because Wave 41 plan filename `wave-2026-05-08-41-...md` (forward-dated handoff) was treated as "today's date" signal. CI `Rule frontmatter` validator caught it on PR #995; other forward-dated content (filenames + body text) leaked into Wave 42 plan, GAP-438, session log, 5 memory entries, skill SKILL.md retro. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5). Enforcement: existing CI `Rule frontmatter` job catches rule-file slice + memory auto-load + reviewer manual generalizes to non-rule artifacts; PR-template item deferred.
