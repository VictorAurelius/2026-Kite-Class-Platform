# Meta-Gap Priority Rule

**Priority:** 🔴 CRITICAL — governance for gap ordering
**Version:** 1.0.0
**Created:** 2026-04-18
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
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

Apply this ordering when building sprint/wave plans. **Three tiers** at each P-level:

| Level | Category | Order |
|-------|----------|:-----:|
| 🟥 Meta-P0 | Skills/rules/workflow broken or missing, blocking quality | **1st** |
| 🟥 Business-Logic-P0 | BRD docs, persona review, correctness, compliance (wrong business = wrong product) | **2nd** |
| 🟥 Feature-P0 | Product GA blocker | 3rd |
| 🟧 Meta-P1 | Skills/rules gap that risks drift soon | 4th |
| 🟧 Business-Logic-P1 | Business correctness, pricing, persona-specific gaps | 5th |
| 🟧 Feature-P1 | Product growth blocker | 6th |
| 🟨 Meta-P2 | Skills/rules nice-to-have | 7th |
| 🟨 Business-Logic-P2 | Business nice-to-have (GTM, NFR enrichment) | 8th |
| 🟨 Feature-P2 | Feature nice-to-have | 9th |

### What counts as Business-Logic tier

Gaps that touch:
- **BRD documents** (`documents/00-brd/*.md`) — personas, objectives, compliance, pricing, NFR, GTM
- **Per-domain rules.md** (`documents/01-business/*/rules.md`) — business values + constraints
- **Persona review** — role-play, acceptance criteria per persona, review reports
- **Business correctness** — "thing right vs right thing", market validation, legal compliance
- **Acceptance criteria** — formal AC per persona/journey

**Why 2nd after Meta:** Meta gaps fix the machine that builds product; Business-Logic gaps fix WHAT product we're building. Wrong business = wrong product even if skill/rule infrastructure is perfect. Feature gaps are execution of correct business logic — correctness must lead execution.

**Tie-breakers within Meta-P0:**
1. **Blast radius** — how many PRs/sessions are affected? (higher = first)
2. **Regression severity** — silent failure vs loud? (silent = first — it rots in background)
3. **Unblocks other gaps?** — e.g. a review skill unblocks 5 audit gaps → first

**Tie-breakers within Business-Logic-P0:**
1. **Persona coverage impact** — how many Tier 1 personas blocked? (more = first)
2. **Compliance/legal risk** — legal mandate > market optimization
3. **BRD → Rules.md blast** — if BRD doc missing blocks multiple per-domain rules.md, fix BRD first

---

## 4. Examples

Applying the rule to a sample backlog (illustrative only — replace with your project IDs):

| Gap | Type | Per-file P | Meta? | Actual order |
|-----|------|:---------:|:-----:|:------------:|
| <example: GAP-XXX> | Document generation skills (Excel/Word/PDF/PPT) | P0 | ✅ Meta (skills) | **1st** |
| <example: GAP-XXX> | Design patterns applied systematically | P1 | ✅ Meta (rules) | **2nd** |
| <example: GAP-XXX> | Living docs impact scope | P0 | ✅ Meta (docs contract) | **3rd** |
| <example: GAP-XXX> | Template library curation | P0 | Feature | 4th |
| <example: GAP-XXX> | Wave mock plan include AI branding | P0 | Feature | 5th |
| <example: GAP-XXX> | AI queue fair scheduling | P0 | Feature | 6th |

Without this rule, feature gaps would start first (alphabetical / sprint 0 default). With rule: meta first — because every other gap implementing dependent work depends on it.

---

## 5. Why This Matters

### 5.1 Force multiplier
A meta-gap affects N future PRs. Fixing it once pays off N times.

Example: missing script-review skill → every script PR reviewed ad-hoc → inconsistent quality. Fixing it once → every future script PR gets the checklist.

### 5.2 Silent degradation
Meta-gaps rarely surface as obvious breakage. They show up as:
- Inconsistent review quality
- Drift between code and docs
- PRs claiming "done" when missing tests/audits
- Sessions losing context because logs weren't captured

The fix isn't visible in a feature demo — but the cost of NOT fixing compounds.

### 5.3 Output quality dependency
"Chất lượng output giảm do context quá đầy". Root cause is often meta-gap:
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
- **`docs-folder-structure.md`** — if meta-gap touches planning docs, both rules apply

---

## 9. Log

- **2026-04-29** (v1.0.0 upstream import): Imported into starter-kit v2.3.0 from project source. Local project remains source of truth; upstream version may diverge as starter-kit evolves separately. Specific GAP IDs in §4 examples replaced with `<example: GAP-XXX>` placeholders.
- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill — added Version + Last-Reviewed + Reviewer-Approver fields. No content change.
- **2026-04-20:** Added Business-Logic tier between Meta and Feature (§3 Priority Matrix). Triggered by user observation that BRD docs + persona review gaps were treated as regular P0 features, despite "wrong business = wrong product even if code is perfect". Business-logic correctness must lead feature execution. New ordering: Meta → Business-Logic → Feature at each P-level.
- **2026-04-18:** Rule created after observing skills/rules/workflow gaps were being deprioritized behind feature gaps, despite affecting output quality of all future PRs.
