# Design Layer Coverage — Japanese 4-layer V-model SI methodology

**Priority:** 🔴 CRITICAL — design completeness governance, prevents docs miss
**Version:** 1.0.0
**Created:** 2026-04-30
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every PR/wave that touches UI / design / kit / business-flow / persona scope. Scope explicitly includes: filing new GAPs, building HTML kits, porting Track 2 to production, designing new features, deprecating screens, refactoring component lib.

---

## 1. The Rule

> **Before claiming any UI/design scope is "covered" — whether filing a GAP, shipping a kit, or porting to production — verify all FOUR Japanese design layers exist with concrete artifact pointers. Missing any of the 4 layers = scope is incomplete = STOP.**

The 4 layers (V-model SI Japanese methodology):

| # | Japanese | Romaji | Western equivalent | "What is this?" question |
|---|----------|--------|--------------------|---------------------------|
| 1 | **要件定義** | yōken teigi | Requirements Definition | What does the system MUST do? Who benefits? Why? |
| 2 | **基本設計** | kihon sekkei (外部設計 gaibu) | External / High-level design | What does the user SEE? Screen flow, mockups, system boundary |
| 3 | **詳細設計** | shōsai sekkei (内部設計 naibu) | Internal / Low-level design | How does developer IMPLEMENT? Classes, sequences, state machines |
| 4 | **コンポーネント設計** | konpōnento sekkei | Component / Parts design | What reusable pieces compose this? Props, types, interface contracts |

Skipping any layer = future maintainer cannot build/maintain/audit the feature with confidence. Coverage gaps produce real bugs (the 2026-04-29 UI Coverage Audit incident is the worked example — see §6).

---

## 2. Per-context coverage matrix (the checklist)

Apply this matrix BEFORE marking scope as covered:

### 2.1 Per-feature gap (e.g. GAP-266 Track 2 port)

| Layer | Required pointer | Artifact location |
|-------|-----------------|-------------------|
| 要件定義 | Persona + use case + business rule | `documents/00-brd/personas-catalog.md` + `documents/01-business/{domain}/rules.md` + `*/use-cases.md` |
| 基本設計 | Screen mockup + flow + acceptance criteria | `ui_kits/{kit}/screens/*.html` + `dossier/03-screen-inventory.md` row + `dossier/10-acceptance-criteria.md` items |
| 詳細設計 | State machine OR ADR OR sequence | `documents/02-architecture/adr/*.md` OR `dossier/{component}/spec.md` "State machine" section OR API contract |
| コンポーネント設計 | Component spec + props + state files | `dossier/04-component-gaps.md` G* row + `ui_kits/components/G*/spec.md` |

### 2.2 Per-kit (e.g. `ui_kits/your-product-b-pro-v2/`)

Kit README must link or contain pointers to all 4 layers:

| Layer | Where to pointer |
|-------|------------------|
| 要件定義 | "Persona" + "Use cases" sections in kit README + cross-link `dossier/01-personas.md` + relevant `documents/01-business/*/use-cases.md` |
| 基本設計 | Kit README "What's in this kit" + screens listing + score table (this layer IS the kit) |
| 詳細設計 | Kit README "State machines / Data flow" section + cross-link to ADRs (`02-architecture/adr/`) + state machines codified in `*-guidelines.md` files (e.g., `ai-branding-guidelines.md` §6) |
| コンポーネント設計 | Kit README "Components used" section listing G* components imported + their spec.md pointers |

### 2.3 Per-wave (e.g. UI Kits Round 3)

Wave plan must explicitly verify all 4 layers covered before kicking off:

| Layer | Wave plan section requiring coverage |
|-------|--------------------------------------|
| 要件定義 | §1 Brainstorm Q1 (persona alignment) + §3 Scope (each bucket cites persona) |
| 基本設計 | §3 Scope (screen list per bucket) |
| 詳細設計 | §1 Brainstorm Q2 (trade-offs) — must cite state machines/ADRs if architectural decisions affect layer 3 |
| コンポーネント設計 | §3 Scope (component list — which G*/D* used or shipped) |

If wave touches scope where one layer is genuinely irrelevant (e.g., docs-only wave doesn't touch コンポーネント設計), explicitly state "N/A — reason" instead of silently omitting.

### 2.4 Per-Track 2 production port

Same as §2.1 + ADD:
- **Verify production state-check** (per `audit-to-gap-pipeline.md` Step 2.5) — does the production code already have pieces of layer 2/3/4? If yes, port = redesign, not greenfield.

---

## 3. Coverage states (per artifact, applied per layer)

For each (artifact × layer) cell, mark with 3-state from `2026-04-29-frontend-ui-coverage-audit.md` pattern:

- **✅ explicit** — documented artifact exists pointing to this layer
- **⚠️ implicit** — covered indirectly (e.g., layer-3 covered via shared rule rather than per-kit ADR) — acceptable but flag for future explicit conversion
- **❌ missing** — no artifact covers this layer for this scope — **MUST be addressed before claiming scope covered**

Rule applies: a scope with **any ❌ at any layer is INCOMPLETE.** Either fix or file follow-up gap before proceeding.

---

## 4. Why this rule exists

The 2026-04-29 UI Coverage Audit incident:
- 8 Track 2 GAPs (GAP-266..273) filed without verifying 100% production frontend coverage
- User flagged miss: "UI của tất cả screen/model/dialog/common đã cover hết chưa?"
- Audit shipped → 7 additional follow-up GAPs needed (GAP-274..280) → Track 2 scope revised 8 → 15 gaps

If this rule existed at GAP-266..273 filing time, the per-feature checklist (§2.1) would have asked:
- 要件定義 layer for KC: ✅ all personas covered (P2 Owner, Pa. Parent, Teacher, Student)?
  - Realized at filing: ❌ Prospects (public marketing visitors) NOT covered → would have surfaced GAP-274 earlier
  - Realized at filing: ❌ Pre-tenant (auth flows users) NOT explicitly enumerated → GAP-276
- 基本設計 layer: ✅ all `page.tsx` mapped to a kit screen?
  - 26 pages ❌ missing kit coverage — surfaced at audit, NOT at gap-filing
- コンポーネント設計 layer: ✅ all 14 modal sites have spec?
  - ❌ 10 modals NOT catalogued — surfaced at audit, NOT at gap-filing

Cost of missing the rule: 1 wave of audit work + 7 follow-up gaps + Track 2 estimate +5 weeks. This rule prevents that recurrence.

---

## 5. Enforcement

### 5.1 PR template checkbox (lands same PR)

Add line to `.github/PULL_REQUEST_TEMPLATE.md` under design/UI scope:

```
- [ ] **Design layer coverage** — if PR touches UI/design/kit/feature scope, all 4 layers (要件定義 / 基本設計 / 詳細設計 / コンポーネント設計) verified per `.claude/rules/design-layer-coverage.md` §2 matrix. Missing layer = file follow-up gap inline OR explicitly mark N/A with reason.
```

(Note: if PR template doesn't exist yet at this repo or is minimal, add the line directly. If line already exists with similar intent, extend.)

### 5.2 Reviewer-checklist line

When reviewing a PR with UI/design scope, reviewer asks:
> Did this PR touch UI/design/kit/feature scope? If yes:
> - For each new GAP filed: §2.1 4-layer matrix complete?
> - For each new kit shipped: §2.2 4-layer pointers in README?
> - For each wave plan: §2.3 4-layer coverage verified in §1+§3?

### 5.3 Audit gate (manual for now, automation deferred)

Quarterly audit (folded into `quality-audit` skill): sample 5 random recently-closed UI/design GAPs. Verify §2.1 4-layer matrix complete in their final state. Pattern of misses → meta-review of this rule.

### 5.4 Dossier mapping doc (paired same PR)

`documents/02-architecture/design-system/dossier/16-design-layer-mapping.md` ships same PR as this rule. Reference mapping showing for every layer: where in repo to find (or create) artifacts for this layer. Per-kit + per-domain breakdowns.

### 5.5 Override mechanism (rare)

Genuine N/A for a layer (e.g., docs-only wave doesn't have コンポーネント設計):

```
git commit -m "...
DESIGN_LAYER_OVERRIDE: <layer> N/A — <reason>"
```

Override logged. Pattern frequency >5% per-quarter triggers meta-review (likely the rule's scope mis-defined).

---

## 6. Worked self-test — apply rule to 2026-04-29 UI Coverage Audit incident

**Scenario:** Filing GAP-266 (Track 2 port — your-product-b-pro v2 → production Next.js) at 2026-04-29.

**Apply §2.1 matrix BEFORE filing:**

| Layer | Required pointer | Status at GAP-266 filing | Verdict |
|-------|-----------------|--------------------------|---------|
| 要件定義 | Persona + use case + business rule | ✅ `dossier/01-personas.md` P2 Center Owner + `documents/01-business/your-product-b/*/use-cases.md` | OK |
| 基本設計 | Screen mockup + flow + AC | ✅ `ui_kits/your-product-b-pro-v2/screens/*.html` (10 screens) + `dossier/03-screen-inventory.md` | OK |
| 詳細設計 | State machine + ADR | ⚠️ partial — kit references shared `colors_and_type.css` but no kit-specific ADR for ⌘K palette / sparkline pattern | FLAG for follow-up |
| コンポーネント設計 | Component spec | ⚠️ partial — kit screens use components NOT all in dossier `04-component-gaps.md` | FLAG for follow-up |

**Verdict at filing time:** GAP-266 itself = OK to file (4 layers covered for ITS scope). But ⚠️ flags would have prompted "is component coverage at 100%?" — leading to systematic check of OTHER gaps not yet filed.

**Apply §2.2 matrix to kit `ui_kits/your-product-b-pro-v2/`:**

| Layer | Pointer in kit README | Status | Verdict |
|-------|----------------------|--------|---------|
| 要件定義 | "Persona" link to `dossier/01-personas.md` | ✅ exists | OK |
| 基本設計 | Screen list + score table | ✅ exists | OK |
| 詳細設計 | "State machines" section linking ADRs | ❌ missing | **MISS** |
| コンポーネント設計 | "Components used" listing G* | ⚠️ implicit (uses G2/G5/G6/G7/G12 but not enumerated in README) | FLAG |

**Verdict:** kit README has gaps at layer 3+4. Self-test confirms rule applied to 2026-04-29 state would have surfaced these.

**Apply §2.3 matrix to Wave UI Kits Round 3 plan:**

Wave plan §1 Brainstorm Q1 covers persona alignment ✅. §3 Scope cites screen list per bucket ✅. §1 Q2 trade-offs ⚠️ doesn't explicitly cite state machines or ADRs (which AI Branding lifecycle uses). コンポーネント設計 ⚠️ implicit (Bucket C/D ship components but Bucket A/B don't enumerate which existing G* used).

**Verdict:** Wave plan WAS covered at layers 1+2, ⚠️ at layers 3+4. Rule would have prompted explicit enumeration → cleaner traceability.

**Self-test conclusion:** rule's §2 matrix surfaces real coverage gaps in our existing artifacts. Worth shipping.

---

## 7. Scope clarifications

### 7.1 What this rule does NOT mandate

- Not mandating Japanese language in artifact contents — terminology is methodology label only. Doc content stays English (technical) + Vietnamese (business per CLAUDE.md).
- Not mandating waterfall execution — agile/iterative welcome, but each iteration must end with all 4 layers present for whatever ships in that iteration.
- Not mandating extensive docs for trivial scope — scope-proportional. Trivial bug fix: 4 layers checked-but-may-be-1-line each. New feature: full 4 layers.
- Not mandating per-FILE 4-layer marking — applies per scope-unit (gap / kit / wave / port).

### 7.2 When to invoke

| Context | Apply | Frequency |
|---------|:-----:|:---------:|
| Filing new UI/design GAP | ✅ §2.1 | Always |
| Building new HTML kit | ✅ §2.2 | Per-kit |
| Wave plan kickoff | ✅ §2.3 | Per-wave |
| Track 2 production port | ✅ §2.4 | Per-port |
| Trivial typo fix in existing covered scope | ❌ skip | — |
| Pure backend scope (no UI/design surface) | ❌ skip | — |
| Tech-debt PR (e.g., refactor without behavior change) | ⚠️ verify still covered, no new layers needed | — |

If unsure: **default to apply §2 matrix.** Cost of unnecessary check is minutes; cost of missed check is wave + follow-up gaps.

---

## 8. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| File GAP citing only "user request" without 4-layer evidence | Cite 4-layer pointers in gap §Related section |
| Build kit with screens but skip kit README linking to BRD persona | Kit README links to `dossier/01-personas.md` row + use-case doc |
| Wave plan §3 Scope lists screens but skips component enumeration | List BOTH screens + component dependencies (which G*/D* used) |
| Mark layer ❌ silently ("we'll do it later") | File follow-up gap explicitly OR flip status to PARTIAL with deferral citation |
| Skip layer 3 (詳細設計) because "frontend doesn't have classes" | State machines + data flow + ADRs ARE 詳細設計 for FE — different artifacts, same layer |
| Use "implicit covered" as escape hatch perpetually | Implicit acceptable short-term; flag for explicit conversion in next iteration |
| Skip layer 4 because "we're using shadcn primitives" | Even shadcn-based UIs have project-specific composition patterns — those need spec.md |

---

## 9. Relationship to other rules

- **`output-review-mandate.md`** — every output requires evidence preserved. This rule extends Section 1 mandate for design scope: the "evidence" is 4-layer pointer set per §2.
- **`rule-change-process.md`** §6.5 Enforcement Parity — paired same-PR with §5 enforcement (PR template + reviewer-checklist + dossier mapping doc + self-test §6).
- **`incident-to-rule-pipeline.md`** — this rule is direct output of 2026-04-29 UI Coverage Audit incident. Detect → Classify → Rule (this) → Self-test (§6) → Retro Log (§10).
- **`gap-done-discipline.md`** §2 — DONE flip requires AC checked + verification artifact pointer. This rule extends: for UI/design scope, the "verification artifact" includes 4-layer pointer set.
- **`audit-to-gap-pipeline.md`** Step 2.5 — state-check before filing. This rule extends with 4-layer check (which layers exist before scope, which need creating).
- **`meta-gap-priority.md`** §3 — meta-gaps (rules/skills/workflow) trump feature gaps at same P-level. This rule itself is meta — adopting it boosts subsequent rule/process gaps.
- **`feedback_parallel_agent_strategy.md`** — wave-pack agents must enumerate which layers their bucket covers (per §2.3). Wave plan template should be extended with per-bucket layer mapping.

---

## 10. Log

- **2026-04-30 (v1.0.0):** Rule created at user request "tôi mong muốn sử dụng 4 layer này để tránh miss docs như vừa rồi" — direct response to 2026-04-29 UI Coverage Audit incident. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged miss + audit confirmed 32% missing coverage) → Classify ✓ (no existing rule mandates 4-layer completeness; existing layered docs but no enforcement that ALL 4 must coexist per scope) → Rule+Enforce ✓ (this file + paired same-PR `dossier/16-design-layer-mapping.md` + `output-review-mandate.md` §3 row update + PR template extension per §6.5) → Self-test ✓ (§6 worked example on UI Coverage Audit incident demonstrates rule would catch ⚠️ flags at 2 of 3 contexts checked) → Retro Log ✓ (this entry). Solo-dev MINOR self-approve per `rule-change-process.md` §5 — new rule with built-in enforcement, no constraint loosening for prior work. Existing artifacts grandfathered (no retroactive 4-layer audit until quarterly cycle).
