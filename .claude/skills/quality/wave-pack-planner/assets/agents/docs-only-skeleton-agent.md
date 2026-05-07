# Docs-Only Skeleton Agent Template

**Use when:** Phase 1 skeleton-only doc cluster (3-4 disjoint policy/BRD docs, ~200-400 LOC each, no content fill — section structure + cross-refs + TODO markers only). Variant of `docs-only-agent.md` specialized for skeleton work where Phase 2 content (legal counsel, stakeholder sign-off, regulatory citations) is intentionally deferred.

**Spawn config:** `isolation=worktree`, `subagent_type=general-purpose`
**Branch naming:** `feat/wave-{theme}-gap-{id-slug}-{topic}-skeleton`
**Wall-clock budget:** ~5 min/agent (Wave 13 calibration: 4 docs × 5.0 min avg, range 4.1-5.7 min)
**Codified after:** 2nd recurrence (Wave Legal-BRD Phase 1 + Phase 1.5, both 2026-04-29)

## When to use vs base `docs-only-agent.md`

| Situation | Template |
|-----------|---------|
| Skeleton-only Phase-1 cluster (sections + TODO markers, no content) | **THIS template** |
| Full content fill, runbooks, ADRs ready to ship | `docs-only-agent.md` (base) |
| Mixed skeleton + partial content | Use this template, drop the §"Phase 2 TODO marker" rule per file |
| Code change required (Java/TS/SQL) | `feature-tdd-agent.md` |
| Pure test backfill | `test-only-agent.md` |
| Dead-code/cleanup | `p3-cleanup-agent.md` |

The split exists because skeleton work is **deterministic and bounded by section count**, not content depth. Wave 13 calibration showed agent wall-clock scales with prompt clarity, not section count — 22-section privacy skeleton finished in same window as 9-section retention skeleton.

## Prompt template

```
You are Agent {LETTER} of wave-pack {THEME}. Your scope: {GAP_ID} — {GAP_TITLE}.

## Wave context
Wave plan: documents/03-planning/waves/wave-{DATE}-{THEME}.md
Worktree root: {WORKTREE_ROOT} (you are isolated; do NOT cd to main repo)
Branch: feat/wave-{THEME}-gap-{GAP_ID_SLUG}-{TOPIC}-skeleton (already created on your worktree)

## Your task — Phase 1 skeleton ONLY
Read first:
- documents/04-quality/gaps/{GAP_ID}.md (full Acceptance Criteria + Proposed Fix)
- {ANY_REFERENCE_DOCS} (existing skeleton patterns to mimic — usually a sibling skeleton already shipped)
- .claude/rules/business-logic-review.md §2.1 (informed-gut Source category — placeholder marker rationale)

Deliverable: 1 new file at {ALLOWED_PATHS} containing:
- {SECTION_COUNT} sections per gap §Scope (numbered, consistent with sibling skeletons)
- Section structure + headings + TODO markers, NOT content
- Frontmatter (markdown-header style — mimic `documents/00-brd/personas-catalog.md` convention; adjust per folder README if different)
- Cross-references to sibling docs: {SISTER_SKELETONS}
- Legal basis citations (where applicable): {LEGAL_BASIS}
- Phase 2 content deferred per `business-logic-review.md` §2.1 informed-gut category

## Phase 2 TODO marker pattern

For every section requiring legal counsel / stakeholder content / regulatory specifics, insert inline HTML-comment placeholder:

    ## 5. Refund Process

    <!-- Phase 2: Refund Process — informed gut Q3 2026, GAP-154 umbrella -->

    (Placeholder — Phase 2 content blocked on legal counsel review per GAP-154.)

Rationale: aligns with `business-logic-review.md` §2.1 — informed-gut entries get quarterly re-review obligation. HTML-comment retrospective anchor lets future search/grep find all Phase-2-pending sections in one pass.

## Cross-link verification rules

- **Sibling skeletons that exist on main** (post-wave foundation merge): use RELATIVE paths
    See [Privacy Policy section 16](privacy-policy.md#16-pdpl-retention).
- **Sibling skeletons NOT yet shipped** (deferred to follow-up wave): use placeholder text — do NOT write broken markdown links
    See Refund + Dispute Resolution Policy (planned — see GAP-183).
- **Rules / skills cross-refs**: relative path from your file location
    Per [`business-logic-review.md` §2.1](../../.claude/rules/business-logic-review.md#21-source).

Verify before commit: every `.md)` link resolves OR is intentionally a "(planned — see GAP-XXX)" placeholder.

## Frontmatter style

Use **markdown-header style** (NOT YAML), mimicking sibling docs in target folder. Example for `documents/00-brd/`:

    # {Doc Title}

    **Trạng thái:** 🔵 SKELETON
    **Owner:** {Owner role(s)}
    **Reviewer:** {Reviewer role(s) — Phase 2}
    **Last-Updated:** {DATE}
    **Tracking:** {GAP_ID} → {UMBRELLA_GAP}
    **Legal basis:** {LEGAL_BASIS}
    **Phase 1 scope:** Skeleton (sections + TODO markers); Phase 2 content deferred per `business-logic-review.md` §2.1 informed-gut + {UMBRELLA_GAP} umbrella

    ---

Check parent folder's README before committing — some folders use YAML frontmatter (`waves/`, `plans/`) per `planning-docs-structure.md` §6. When in doubt, copy frontmatter style from the most recent file in the same folder.

## Rules

- Files MUST live under: {ALLOWED_PATHS}. Do NOT touch anything else.
- **DO NOT touch README** — foundation PR owns directory map updates centrally. If you think README needs updating, leave a note in PR body for coordinator to handle.
- Cross-link verification: every link to another doc MUST resolve OR be a "(planned — see GAP-XXX)" placeholder.
- Phase 2 TODO markers inline per section requiring deferred content (HTML-comment + placeholder paragraph pattern).
- Vietnamese prose default; English for legal/technical terms (per CLAUDE.md).
- NO emojis except functional ones in tables/status indicators (e.g. 🔵 SKELETON, 🟡 PARTIAL).
- **Status flip is NOT in your scope.** Do NOT modify {GAP_ID} Status field. Coordinator owns status flip per `gap-done-discipline.md` §3 PARTIAL exit-ramp (Phase 2 deferred = stay 🟡 PARTIAL).

## Worktree verify (boilerplate — run before EVERY Write/Edit)

Wave 13 Agent C incident (2026-04-29 GAP-182 privacy skeleton): Write tool initially landed file at MAIN worktree path, not agent's isolated worktree. Caught pre-commit by grep verification; recovered via copy to correct path. Mitigation:

    # Before every Write/Edit
    pwd | grep -q "\.claude/worktrees/agent-" || { echo "NOT IN WORKTREE — abort"; exit 1; }
    git branch --show-current | grep -E "^(worktree-agent-|feat/wave-)" || { echo "WRONG BRANCH — abort"; exit 1; }

    # Use RELATIVE paths in commands — never absolute (/home/.../documents/...)
    ls documents/00-brd/  # relative — OK
    # ls /home/.../documents/00-brd/  # absolute — bypasses worktree

Per `feedback_worktree_absolute_path_contamination.md` — absolute paths in coordinator prompts cause Write to land in main repo. RELATIVE paths in your own commands prevent recurrence.

## Deliverable format

After commits, report back:
1. Branch name + commit SHA
2. Files added (path list — should be exactly 1 NEW file)
3. PR URL (`gh pr create --base main --title "docs(brd): {GAP_ID} {short-title} skeleton"`)
4. File LOC: `wc -l {your-file}` (target ~200-400 LOC)
5. Section count vs gap §Scope: paste your `## N. <heading>` headings
6. Cross-link check: `grep -n "\.md)" {your-file}` proving links resolve OR are "(planned)" placeholders
7. Frontmatter check: `head -15 {your-file}` confirming required fields
8. Phase 2 TODO marker count: `grep -c "<!-- Phase 2:" {your-file}`
9. Note: do NOT flip {GAP_ID} Status — coordinator handles 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md`

## Skip (not in scope)

- TDD section (no code = no unit tests; cross-link check IS the test)
- Migration version reservation (no DB)
- Pattern audit (no Java/TS code)
- Status flip in gap file (coordinator owns)
- README directory map update (foundation PR owns)
- Sibling-doc content (other agents own)
```

## Required placeholders

| Placeholder | Example | Notes |
|---|---|---|
| {LETTER} | A | Agent label per wave plan |
| {THEME} | legal-brd-1-5 | Short theme slug |
| {GAP_ID} | GAP-186 | Single gap per agent |
| {GAP_TITLE} | Child Protection Policy K-12 | Match gap file H1 |
| {DATE} | 2026-04-29 | Wave date |
| {WORKTREE_ROOT} | (auto-assigned by harness) | Coordinator does NOT cite absolute path in prompt |
| {GAP_ID_SLUG} | 186-child-protection | Lowercase, dash |
| {TOPIC} | child-protection | Short suffix for branch name |
| {ALLOWED_PATHS} | `documents/00-brd/child-protection-policy.md` (NEW) | Single new file path; relative to repo root |
| {SECTION_COUNT} | 8 | Per gap §Scope mandated section count |
| {SISTER_SKELETONS} | `terms-of-service.md`, `privacy-policy.md`, `data-retention-deletion-policy.md` | Existing siblings on main (relative paths) |
| {LEGAL_BASIS} | Law on Children 2016 + Decree 56/2017/NĐ-CP + PDPL 2023 Art 16 | Statute citations for frontmatter |
| {ANY_REFERENCE_DOCS} | `documents/00-brd/personas-catalog.md` (frontmatter style) | Patterns to mimic |
| {SCOPE} | brd | Conventional-commit scope |
| {UMBRELLA_GAP} | GAP-154 | Parent gap tracking Phase 2 |

## Gotchas

- **Worktree absolute-path bug** (per `feedback_worktree_absolute_path_contamination.md`, Wave DR/Backup 2026-04-28 + Wave 13 Agent C 2026-04-29): coordinator prompts citing absolute paths (`/home/.../documents/00-brd/foo.md`) make agents bypass worktree cwd → Write lands in MAIN repo, commits land on WRONG branch. Wave 13 case: Write tool landed `privacy-policy.md` at main worktree path; caught pre-commit by `pwd` grep; recovered via copy to correct path. **Mitigation:** verify cwd before every Write/Edit (boilerplate above). Use RELATIVE paths in your own commands. Verify branch before commit.
- **Frontmatter drift across folder conventions:** `00-brd/` uses markdown-header style; `03-planning/waves/` + `03-planning/plans/` require YAML frontmatter (per `planning-docs-structure.md` §6); `02-architecture/adr/` uses MADR template. Check the folder's README + most recent neighbor file before picking style.
- **Cross-link rot to sibling skeletons in same wave:** if Agent A's deliverable references Agent B's deliverable file (both not yet on main), use "(planned — see GAP-XXX)" placeholder. After foundation PR merges siblings, these become resolvable in follow-up gaps. Wave 13 Agent D (retention) cross-linked privacy via relative path — worked because foundation already shipped sibling stubs in `00-brd/README.md` directory map.
- **Phase 2 TODO markers vs `gap-done-discipline.md` banned phrases:** the banned phrase scan in `session-docs-check` Rule 13 looks at gap file Log entries when Status flips → DONE. Your skeleton file's `<!-- Phase 2: ... -->` markers do NOT trip this — they're in the policy doc, not the gap Log. But coordinator MUST keep gap Status at 🟡 PARTIAL until Phase 2 ships, per §"Status flip is NOT in your scope" above.
- **Section-count drift from gap §Scope:** copy gap §Scope's mandated section list into your headings 1:1. If you think a section should split or merge, leave a note in PR body — don't unilaterally restructure (breaks sibling cross-refs).
- **Wall-clock estimate vs reality:** Wave 13 budget was 25-35 min/agent; actual was 4.1-5.7 min/agent (avg 5.0). Skeleton work scales with prompt clarity, not section count. If you're spending >10 min on a skeleton, you're likely filling content (reframe and stop). Coordinator should re-spawn with tighter scope.
- **Banned phrases inside policy doc body** (different concern from gap Log): your skeleton may legitimately use "deferred", "blocked on", "Phase 2" etc. inside placeholder paragraphs. These are policy-doc text, not Log entries — `gap-done-discipline.md` Rule 13 banned-phrase scan does NOT apply to skeleton body. Only applies when coordinator updates gap file Log on Status flip.
- **Foundation PR README dependency:** if `00-brd/README.md` directory map doesn't yet show your skeleton's row, the foundation PR is incomplete. Do NOT add the row yourself — flag in PR body for coordinator. Wave 13 + 14 foundation PRs shipped README updates centrally.

## When NOT to use this template

- **Full content fill:** if Phase 2 ships in same PR (legal counsel ready, regulatory citations finalized), use base `docs-only-agent.md` instead — the §"Phase 2 TODO marker" pattern would be misapplied
- **Mixed scope:** docs + code change → `feature-tdd-agent.md` (treat docs as side artifact)
- **Code-only change:** Java/TS/SQL → `feature-tdd-agent.md`
- **Test backfill:** → `test-only-agent.md`
- **Cleanup/dead-code:** → `p3-cleanup-agent.md`
- **Single-doc work** (1 gap, no cluster): probably overkill to use wave-pack pattern at all — see `SKILL.md` §"When NOT to use"

## PR body — MANDATORY sections

Per Wave 32 rework brief §3.4 + §3.5: every PR body PHẢI có §"Local verification (pre-push)" with literal command output paste + §"AC Coverage" table (mapping mỗi AC line → file/test/verification evidence). Worktree-isolated agents PHẢI paste `pwd | grep -F "/agent-"` confirming CWD inside assigned worktree.

Full spec + reject signals: see `feature-tdd-agent.md` §"PR body — MANDATORY sections" (canonical).

## Reference

- Methodology: [`../../SKILL.md`](../../SKILL.md) Step 3
- Base template: [`docs-only-agent.md`](docs-only-agent.md)
- Spawn pattern: [`../../reference/agent-spawning-template.md`](../../reference/agent-spawning-template.md)
- Wave closure: [`../../reference/retrospective-checklist.md`](../../reference/retrospective-checklist.md) §"4+-agent local-state hazards"
- Status discipline: `.claude/rules/gap-done-discipline.md` §3 PARTIAL exit-ramp
- 5-attribute pattern: `.claude/rules/business-logic-review.md` §2.1 informed-gut Source category
- Worktree contamination: memory `feedback_worktree_absolute_path_contamination.md`

## Worked examples

| Wave | Date | Agents | Gaps | Wall-clock | Notes |
|------|------|:------:|------|:----------:|-------|
| Wave Legal-BRD Phase 1 | 2026-04-29 | 4 (A/B/C/D) | GAP-180 (TOS) / GAP-181 (AUP) / GAP-182 (Privacy) / GAP-184 (Retention) | 4-6 min each (avg 5.0) | First proven application; Agent C contamination caught pre-commit |
| Wave Legal-BRD Phase 1.5 | 2026-04-29 | 4 (A/B/C BRD + D meta) | GAP-183 (Refund) / GAP-185 (Billing-VAT) / GAP-186 (Child Protection) + meta-codify | ~5 min/agent (target) | Sister wave; meta agent codifies THIS template variant |

After Wave 14 ships, append a 3rd row + bump examples count. After 3rd recurrence (e.g. another legal/policy slice), re-evaluate whether to extend or split this template.
