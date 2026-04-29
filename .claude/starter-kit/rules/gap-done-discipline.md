# Gap DONE Discipline — what counts as fully closed

**Priority:** 🟠 MANDATORY — governance for gap status discipline
**Version:** 1.0
**Created:** 2026-04-27
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every transition of `documents/04-quality/gaps/GAP-*.md` Status field to `🟢 DONE`

---

## 1. The Rule

> **A gap MAY be flipped to `🟢 DONE` only when every Acceptance Criterion is verified and demonstrated.**
> If any AC is deferred — for scope, infra, time, manual-run, or local-environment reasons — the gap stays `🟡 PARTIAL`, and the deferred portion is filed as a follow-up gap (or referenced if one already exists).

This rule closes a silent-loss path: when work is "deferred to manual run" yet the gap is still flipped to DONE in the closing commit, the deferred work has no enforcement mechanism — it relies on someone remembering — which is exactly the failure mode this rule prevents.

---

## 2. What a "DONE" gap requires

For Status to transition `OPEN`/`PARTIAL` → `DONE` in a single PR, the gap file must satisfy all six:

1. **Every `- [ ]` checkbox in the Acceptance Criteria section is `- [x]`** in the same diff. No unchecked items.
2. **No banned phrase** appears in the new Log entry without a paired follow-up gap reference. Banned phrases (case-insensitive):
   - `deferred`, `defer to`, `deferred to manual`
   - `out of scope`, `out-of-scope` (in the Log entry — the `## Out-of-scope` section is a different beast and is fine)
   - `manual run`, `manual capture`, `to be captured manually` — when the AC asked for capture
   - `infra block`, `blocked on infra`, `stack not up`
   - `local can't`, `local doesn't`, `WSL2 too slow`
   - `partial`, `partially` — when status is DONE
3. **No `[skip]` / `[wontfix]` annotation** in any AC line. If something genuinely won't be done, drop the AC line from the gap and write the rationale in §Out-of-scope referencing a follow-up.
4. **If the gap had a wave plan or a multi-stage Proposed Fix, every stage is shipped** and referenced by PR number in the Log. PR-style "scope reduced because X" entries must include both the reduction reason AND the follow-up gap that catches the deferred remainder.
5. **For audit-driven gaps** (filed from `quality-audit`/`ui-review`/`security-audit` etc.), the closing PR's Log entry includes a verification artifact pointer: re-audit score, regression-test name, screenshot path, smoke-script output, etc.
6. **For schema/migration/infra/CI gaps**, the closing PR shows verification on a fresh equivalent environment — not just "passed locally on my machine after I worked around X." The Log entry names the environment.

---

## 3. The PARTIAL exit ramp

If criteria above can't be met in this PR, the correct status flip is:

| Source state | Allowed targets |
|--------------|-----------------|
| `🔵 OPEN` | `🟡 PARTIAL` (some progress shipped) or `🟢 DONE` (rare — fully solved in single PR) |
| `🟡 PARTIAL` | `🟡 PARTIAL` (more progress) or `🟢 DONE` (final close, rules above apply) |
| `🟢 DONE` | (never re-opens — file a NEW gap if regression) |

A `🟡 PARTIAL` gap remains visible in `ROADMAP.md` and `/repo-status` checks, so deferred work doesn't fall off the radar. A premature `🟢 DONE` removes those signals.

---

## 4. Concrete examples

### Bad — silent-deferral pattern

```markdown
**Status:** 🟢 DONE 2026-04-27 — All 4 sub-PRs shipped

## Acceptance Criteria
- [x] OpenAPI spec exported
- [x] 10 v2 endpoints mocked
- ...
- [ ] Screenshots captured all 6 lifecycle states     ← unchecked!

## Log
- **2026-04-27** Sub-PR G shipped: ... Live screenshot capture deferred to manual run — Docker stack not up in this session.
```

This violates criterion 1 (`[ ]` unchecked), criterion 2 (`deferred` + `manual run` + `not up`), and criterion 5 (audit-driven gap, no artifact pointer for screenshots).

### Good — the same gap, closed correctly

Option A — keep PARTIAL until screenshots actually exist:

```markdown
**Status:** 🟡 PARTIAL — 5/6 sub-tasks DONE; live screenshot capture pending dev-stack fix

## Acceptance Criteria
- [x] OpenAPI spec exported
- [x] 10 v2 endpoints mocked
- ...
- [ ] Screenshots captured all 6 lifecycle states  →  blocked by GAP-XXX (dev-stack boot)

## Log
- **2026-04-27** ... Live screenshot capture blocked by dev-stack schema mismatch (GAP-XXX filed). Status stays PARTIAL until GAP-XXX lands and capture is run.
```

Option B — drop the AC and document the scope cut:

```markdown
**Status:** 🟢 DONE 2026-04-27

## Acceptance Criteria
- [x] OpenAPI spec exported
- [x] 10 v2 endpoints mocked
- ...
- [x] Playwright spec ready for screenshot capture (manual run)   ← reframed: ship the spec, not the screenshots

## Out-of-scope (track separately)
| Item | Where |
| Live screenshot artifacts | GAP-YYY — after GAP-XXX unblocks dev stack |
```

Either is fine. What's not fine: the original "deferred to manual" text inside a DONE flip.

---

## 5. Why this matters

A `🟢 DONE` gap drops out of the active backlog. `ROADMAP.md` snapshots, `/repo-status` health checks, and pre-merge audits all treat DONE gaps as closed signal. If the gap is closed prematurely, the deferred work has no home:

- It's not in the gap (closed).
- It's not in a new gap (none was filed).
- It's not in any owner's queue (no one's tracking).
- It exists only in the head of whoever wrote the deferral note — and gets forgotten the moment context flushes.

The cost compounds because audit reports reference the closed gap as a precedent ("we've already dealt with X"), so later observers don't re-investigate. Years later the deferred slice is tribal knowledge or, worse, a latent prod bug.

---

## 6. Enforcement

- **Pre-merge automated:** `session-docs-check` skill **Rule 13** (paired same-PR with this rule) detects every `+**Status:** ... 🟢 DONE` line in the diff and runs the §2 checks; failure → BLOCK in `--strict` mode, WARN otherwise. Detection script lives in `quality/session-docs-check`. Skill detector deferred to skills batch upstream (PR 2 v2.4.0).
- **Reviewer manual:** PR template §Gap Tracking — when a checkbox claims a gap is closed, reviewer verifies §2 criteria.
- **Quarterly audit:** `quality-audit` skill samples 5 random recently-closed gaps and verifies §2 retroactively. False-DONE patterns trigger a follow-up gap.
- **Override mechanism:** if rule 2's banned phrase IS legitimate (e.g. "out-of-scope" appears in the §Out-of-scope section, not the Log), the file MUST contain a `## Out-of-scope` heading. The check tolerates that section.

### Override trailer (escape hatch)

For rare cases where reviewer + author agree the gap genuinely is DONE despite tripping a check:

```
git commit -m "...
GAP_DONE_OVERRIDE: GAP-XXX — <reason and link to followup gap>"
```

Detector greps the commit log of the diff range; presence of a properly-formed trailer (gap ID + reason + followup link) downgrades BLOCK → WARN. Audit log captures every override per quarter.

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Flip to DONE because "the main work is done" | Flip to PARTIAL until ALL ACs verified |
| Justify deferral inline ("scope reduced") in the DONE-flipping commit | File a follow-up gap; reference it in the Log |
| "TODO: capture screenshots later" in a DONE gap | PARTIAL status until the TODO actually fires |
| "Verified locally on my machine after I worked around X" | Verify on the actual production-equivalent env or stay PARTIAL |
| Use `## Out-of-scope` to silently shrink AC at close-time | Out-of-scope is for items that were never in scope; shrinking AC at close requires either a follow-up gap or honest PARTIAL |

---

## 8. Relationship to other rules

- **`output-review-mandate.md`** §3 — this rule sharpens the DONE side of the review standard
- **`incident-to-rule-pipeline.md`** (sibling, same PR) — this rule is the first concrete output of that pipeline applied to a silent-deferral incident
- **`rule-change-process.md`** §6.5 (Enforcement Parity Mandate) — formalizes the rule+detection-same-PR pattern this rule demonstrates
- **`audit-to-gap-pipeline.md`** Step 2.5 — state-check at file-time; this rule is the matching state-check at close-time
- **`meta-gap-priority.md`** — meta gaps that fix this discipline (e.g. extending the detector) get meta-boost

---

## 9. Log

- **2026-04-29 (v1.0 upstream import):** Imported into starter-kit v2.3.0 from project source. Skill detector deferred to skills batch upstream (PR 2 v2.4.0). Local project remains source of truth; upstream version may diverge as starter-kit evolves separately.
- **2026-04-27 (v1.0):** Rule created after a Sub-PR shipped DONE despite live-screenshot deferred-to-manual pattern. Paired with `session-docs-check` Rule 13 in same PR for enforcement. New rule with concrete enforcement attached, no constraint loosening for existing work. User decision: "thêm rules hoặc hooks để cảnh báo cấm đánh done cho gap chưa fix triệt để" — direct mapping into this rule + Rule 13 detector.
