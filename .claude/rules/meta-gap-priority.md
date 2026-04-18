# Meta-Gap Priority Rule

**Priority:** 🔴 CRITICAL — governance for gap ordering
**Created:** 2026-04-18
**Applies to:** All gap triage, sprint planning, wave planning

---

## 1. The Rule

> **Gaps affecting skills, rules, or workflow have the HIGHEST priority — above feature gaps of equal nominal priority.**
>
> Meta-gaps (skills/rules/workflow) fix a force multiplier: 1 broken skill degrades every subsequent PR. A broken feature affects 1 use case.

When two gaps share the same P-level (e.g. both P0), the meta-gap fixes first.

---

## 2. What Counts as Meta

Meta-gaps touch infrastructure that Claude or developers use to DO the work — not the work itself.

| Category | Examples | Affected by gap |
|----------|----------|-----------------|
| **Skills** | `.claude/skills/**/*.md`, document-generation, quality-audit, review skills | Every PR using that skill |
| **Rules** | `.claude/rules/**/*.md`, CLAUDE.md, pre-commit standards | Every PR in the rule's scope |
| **Workflow/Hooks** | `audit-gate.py`, CI workflows, PR templates, pr-logs governance | Every PR merged via the workflow |
| **Audit/Check standards** | How we score PR/wave, definitions of DONE | Every quality decision |
| **Living docs contracts** | Docs structure rules, 3-layer business docs pattern | Every doc change |

Non-meta (= feature) gaps touch product surface: code behavior, UI, business logic, data.

---

## 3. Priority Matrix

Apply this ordering when building sprint/wave plans:

| Level | Category | Order |
|-------|----------|:-----:|
| 🟥 Meta-P0 | Skills/rules/workflow broken or missing, blocking quality | **1st** |
| 🟥 Feature-P0 | Product GA blocker | 2nd |
| 🟧 Meta-P1 | Skills/rules gap that risks drift soon | 3rd |
| 🟧 Feature-P1 | Product growth blocker | 4th |
| 🟨 Meta-P2 | Skills/rules nice-to-have | 5th |
| 🟨 Feature-P2 | Feature nice-to-have | 6th |

**Tie-breakers within Meta-P0:**
1. **Blast radius** — how many PRs/sessions are affected? (higher = first)
2. **Regression severity** — silent failure vs loud? (silent = first — it rots in background)
3. **Unblocks other gaps?** — e.g. a review skill unblocks 5 audit gaps → first

---

## 4. Examples from Current Backlog

Applying rule to the 6 remaining Block GA gaps (as of 2026-04-18):

| Gap | Type | Per-file P | Meta? | Actual order |
|-----|------|:---------:|:-----:|:------------:|
| GAP-047 | Document generation skills (Excel/Word/PDF/PPT) | P0 | ✅ Meta (skills) | **1st** |
| GAP-046 | Design patterns applied systematically | P1 | ✅ Meta (rules) | **2nd** |
| GAP-016 | Living docs impact scope | P0 | ✅ Meta (docs contract) | **3rd** |
| GAP-011 | Template library curation | P0 | Feature | 4th |
| GAP-014 | Wave mock plan include AI branding | P0 | Feature | 5th |
| GAP-005 | AI queue fair scheduling (Phase 2) | P0 | Feature | 6th |

Without this rule, GAP-011 would start first (alphabetical / sprint 0 default). With rule: GAP-047 first — because every other gap implementing generated documents depends on it.

---

## 5. Why This Matters

### 5.1 Force multiplier
A meta-gap affects N future PRs. Fixing it once pays off N times.

Example: missing script-review-skill (GAP-081) → every script PR reviewed ad-hoc → inconsistent quality. Fixing it once → every future script PR gets the checklist.

### 5.2 Silent degradation
Meta-gaps rarely surface as obvious breakage. They show up as:
- Inconsistent review quality
- Drift between code and docs
- PRs claiming "done" when missing tests/audits
- Sessions losing context because logs weren't captured

The fix isn't visible in a feature demo — but the cost of NOT fixing compounds.

### 5.3 Output quality dependency
User's exact concern (2026-04-18): "chất lượng output giảm do context quá đầy". Root cause is often meta-gap:
- Stale ROADMAP → wrong priority decisions
- Missing PR logs → can't audit what shipped
- Out-of-date skills → new work not covered by review checklists

Fixing meta-gaps first prevents these quality drops at source.

---

## 6. Enforcement

### 6.1 Gap triage
When triaging new gaps (`audit-to-gap-pipeline.md` §6 "Fix Priority & Ordering"), add the meta-boost filter BEFORE applying dependency chain rules.

### 6.2 ROADMAP sections
- "Current Status Snapshot" must list Meta-P0 gaps first
- "Block GA" list orders by meta-first within each tier

### 6.3 Sprint planning
When selecting next work from Open gaps:
1. Filter: `priority = P0 AND type = meta` → start here
2. Then: `priority = P0 AND type = feature`
3. Only escalate to P1 after all P0 done

### 6.4 PR review checklist
Reviewers check: does this PR depend on an unmet meta-gap? If yes, flag — we may be building on shaky foundation.

---

## 7. Exceptions

| Case | Allowed override |
|------|------------------|
| Production incident (P0 hotfix) | Fix feature first — operational |
| External deadline (customer, legal) | Fix feature first — business |
| Meta-gap has no maintainer available | Document, defer, revisit next sprint |

Never override silently — always log the override reason in gap/PR description.

---

## 8. Relationship to Other Rules

- **`audit-to-gap-pipeline.md`** §6 — this rule adjusts the Fix Priority ordering
- **`output-review-mandate.md`** — meta-gaps often ARE missing review standards; both rules reinforce each other
- **`skill-conventions.md`** — meta-gaps that touch skills follow this convention
- **`planning-docs-structure.md`** — if meta-gap touches planning docs, both rules apply

---

## 9. Log

- **2026-04-18:** Rule created after user observation that skills/rules/workflow gaps were being deprioritized behind feature gaps, despite affecting output quality of all future PRs. Triggered by discussion of GAP-047 (document generation skills) being listed at same P0 level as feature gaps.
