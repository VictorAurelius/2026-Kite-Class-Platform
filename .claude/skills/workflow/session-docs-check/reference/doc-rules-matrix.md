# Doc Rules Matrix — session-docs-check reference

**Purpose:** chi tiết 12 rules từ `SKILL.md` + examples + glob patterns + edge cases.

---

## Rule 1 — New endpoint → api-contract.md

**Trigger:** file matching `**/*Controller.java` modified OR new, AND diff contains new `@(Get|Post|Put|Delete|Patch|Request)Mapping` annotation.

**Required co-change:** `documents/01-business/{platform}/{domain}/api-contract.md` modified in same branch.

**Resolve `{platform}/{domain}`:** parse package path. E.g.:
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/branding/controller/...` → `kiteclass/branding`
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/...` → `kitehub/instance-provisioning` (mapping table: subscription module → instance-provisioning domain)

**Edge cases:**
- Pure refactor (rename method, no annotation change): skip rule 1
- New `@RequestMapping` at class level (no method-level annotations changed): skip
- Internal-only `/internal/**` controller: still required (document under same domain)

---

## Rule 2 — Business rule change → rules.md + use-cases.md

**Trigger:** file matching `**/*Service.java`, `**/*ServiceImpl.java`, `**/*Entity.java`, `**/*Validator.java` modified, AND commit message contains keywords: `feat`, `fix`, `refactor` (NOT `test`/`docs`/`chore`/`ci`).

**Required co-change:** matching domain's `rules.md` + `use-cases.md` updated.

**Heuristic:** if diff contains new constants (`public static final`), new business validations, new state transitions, new database operations → docs almost certainly need update.

**Edge cases:**
- Pure unit-test fixture change in service: skip
- Rename without behavior change: skip
- Adding logging only: skip

---

## Rule 3 — New gap file → ROADMAP entry

**Trigger:** new file matching `documents/04-quality/gaps/GAP-[0-9]+-*.md`.

**Required co-change:** `documents/04-quality/gaps/ROADMAP.md` modified, AND ROADMAP diff contains the new GAP-XXX number.

**Why:** per `audit-to-gap-pipeline.md` Step 5 — gap không trong ROADMAP = gap bị quên.

---

## Rule 4 — Modified gap file → ROADMAP log entry

**Trigger:** existing `GAP-*.md` modified, AND diff contains:
- Status line change (e.g. `🔵 OPEN → 🟡 PARTIAL` or `🟡 PARTIAL → 🟢 DONE`), OR
- New `## Log` entry with today's date

**Required co-change:** ROADMAP.md `## 🎯 Current Status Snapshot` section has matching log entry.

**Skill check:** grep ROADMAP for the GAP number + today's date.

**Edge cases:**
- Cosmetic edits (typo fix in body): WARN only
- Status flipped to DONE but ROADMAP missing entry: FAIL (per `feedback_post_merge_doc_sync.md`)

---

## Rule 5 — New skill → _README-skills-index.md

**Trigger:** new file matching `.claude/skills/**/SKILL.md`.

**Required co-change:** `.claude/skills/_README-skills-index.md` modified, AND diff contains the new skill path/name.

**Edge cases:**
- Skill in `.claude/skills/_README-skills-index.md` itself doesn't need self-reference
- Skill folder rename: trigger Rule 6 (delete + add)

---

## Rule 6 — Skill rename/delete → all references

**Trigger:** SKILL.md path changed (rename) OR deleted.

**Required co-changes (all):**
- `.claude/skills/_README-skills-index.md` updated (remove old, add new if rename)
- Search other `*.md` files in `.claude/skills/` for references → all updated
- `documents/04-quality/gaps/MEMORY.md` if any (NOTE: project memory at `~/.claude/projects/...../memory/MEMORY.md` — script can't easily check; warn user manually)

**Skill check:** `grep -r "old/skill/path" .claude/` returns 0 hits except in archived/historical docs.

---

## Rule 7 — Rule edit → §Log entry + version bump

**Trigger:** file matching `.claude/rules/*.md` modified.

**Required co-change in same diff:**
- Frontmatter `Version:` line bumped (semver per `rule-change-process.md` §4)
- Frontmatter `Last-Reviewed:` line updated to today
- `## Log` section appended with new dated entry

**Skill check:**
- Diff contains `Version:` line change
- Diff contains today's date in body (after a `## Log` line OR `**YYYY-MM-DD**` pattern)

**Edge cases:**
- Typo fix only: PATCH bump still required (per `rule-change-process.md` §4 — every change has log entry)
- Version field missing entirely: MAJOR violation — flag separately

---

## Rule 8 — New folder under documents/ → README chain

**Trigger:** new file in a directory under `documents/` that didn't exist before.

**Required co-changes:**
- New folder has `README.md` (per `docs-folder-structure.md` §3 template)
- Parent folder's `README.md` updated to list new subfolder

**Skill check:**
- For each new file path, walk up directory tree; verify README exists at each level under `documents/`

---

## Rule 9 — Migration file → rules.md reference

**Trigger:** new file matching `**/db/migration/V[0-9]+__*.sql`.

**Required co-change:** matching service's domain `rules.md` references the migration (Config Key column / Reserved table column / explicit reference).

**Heuristic:** open migration, extract table name and CREATE TABLE / ALTER TABLE statements; check `rules.md` mentions that table.

**Edge cases:**
- Migration adds new column: rule.md MAY not need update if column is internal/audit-only; WARN
- Migration is RENAME: matching rules.md should reference new name; FAIL if old name still appears

---

## Rule 10 — application.yml key → rules.md

**Trigger:** `application.yml` (or `application-{profile}.yml`) modified, AND diff adds new top-level keys or sub-keys.

**Required co-change:** matching domain's `rules.md` Config Key column lists the new key.

**Edge cases:**
- Pure value change (e.g. timeout 30 → 60): skip rule 10 (value docs in body, not column)
- New profile (`application-staging.yml` first time): WARN, profile-specific configs may be ops-only

---

## Rule 11 — Skill/rule meta-change → output-review-mandate matrix

**Trigger:** modified `.claude/rules/*.md` OR new/modified skill in `.claude/skills/quality/`.

**Required co-change (conditional):** `.claude/rules/output-review-mandate.md` §3 matrix line for that output type updated IF the change shifts coverage status (PARTIAL → DONE, etc.).

**Skill check:** complex semantic — script outputs WARN with link to `output-review-mandate.md` §3 for human verification.

---

## Rule 12 — Wave merge → audit suite trigger

**Trigger:** `git log main --since='1 day' | grep -c '#[0-9]\+'` ≥ 3 (3+ PRs merged in 24h).

**Required co-change (3-day window):** `documents/04-quality/audits/{category}/audit-YYYY-MM-DD-*.md` for each required category per `post-wave-audit-mandate.md` §2.1 rules.

**Skill check:**
- Cat-fixture mapping per file patterns changed in wave's PRs
- Look for audit reports newer than wave merge in each required `audits/{cat}/` dir
- Missing → FAIL with reference to `post-wave-audit-mandate.md` §4 runbook

---

## Edge cases applying to all rules

### Multi-domain change
A single PR may touch multiple domains (e.g. shared lib refactor). Skill must trigger ALL applicable rules per domain.

### Foundation PRs
Foundation PRs that ship interfaces/shared lib without consumer code: rules 1/2 may not have co-change yet. Pattern: commit message contains `foundation`, `prep`, `extract`, `shared lib`. WARN instead of FAIL.

### Cherry-pick / backport PRs
PR title/body contains `cherry-pick`, `backport`. Skip rules 3/4 (gap already closed in main).

### Rebase artifacts
After rebase, file may show "modified" without semantic change. Script can't detect this; relies on diff content. May produce false-positive WARN — acceptable.

### Stacked PRs
Per memory `feedback_stacked_pr_delete_branch.md`: child PR after parent merges may show parent's diff still. Use `--base=<parent-branch>` to scope correctly.

---

## Output formatting reference

Standardized for `check-docs.sh` consumer parsing:

```
[OK]    Rule N — <pattern> → <co-change> ✅
[WARN]  Rule N — <pattern> → <co-change> ⚠️  reason: <why warn not fail>
[FAIL]  Rule N — <pattern> → <co-change> ❌  fix: <one-line suggestion>
```

Tổng cuối file:
```
SUMMARY: <P> passed, <W> warned, <F> failed.
EXIT_CODE: <0 if --strict not set or F=0, else 1>
```
