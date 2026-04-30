# 16 — Design Layer Mapping (Japanese 4-layer V-model)

**Companion to:** `.claude/rules/design-layer-coverage.md` v1.0.0 (governance rule)
**Use this when:** verifying §2 matrix per-scope (gap / kit / wave / Track 2 port). This doc is the lookup table for "where in repo does layer N live for context X?"

---

## The 4 layers — 1-line summaries

| # | 日本語 | English | What it answers |
|---|--------|---------|-----------------|
| 1 | **要件定義** (yōken teigi) | Requirements Definition | "WHAT does the system MUST do? Who benefits? Why?" |
| 2 | **基本設計** (kihon sekkei / 外部設計 gaibu) | External / High-level design | "WHAT does the user SEE? Screen flow + mockups + system boundary" |
| 3 | **詳細設計** (shōsai sekkei / 内部設計 naibu) | Internal / Low-level design | "HOW does developer IMPLEMENT? State machines + sequences + algorithms" |
| 4 | **コンポーネント設計** (konpōnento sekkei) | Component / Parts design | "WHAT reusable parts compose this? Props + types + interface contracts" |

---

## Layer 1 — 要件定義 (Requirements Definition)

**Where it lives in this repo:**

| Artifact category | Path | Coverage |
|-------------------|------|----------|
| Business requirements documents (legal-mandated) | `documents/00-brd/legal/{TOS,AUP,Privacy,Retention,Refund,Billing,ChildProtection}.md` | ✅ 7/7 Phase 1 skeletons |
| Per-domain business rules + use cases | `documents/01-business/{kiteclass,kitehub}/{domain}/rules.md` + `*/use-cases.md` + `*/api-contract.md` | ✅ Strong (3-file structure per domain mandated by CLAUDE.md) |
| Persona catalog + design implications | `documents/00-brd/personas-catalog.md` + `dossier/01-personas.md` | ✅ Tier 1 + Tier 2 personas |
| Cultural / UX constraints | `dossier/02-vietnamese-ux-musts.md` | ✅ |
| Business flows (cross-domain journeys) | `dossier/05-business-flows.md` | ✅ |
| Existing pain points (drives requirements) | `dossier/07-existing-pain-points.md` | ✅ |
| Direction decisions (ruled-in/out scope) | `dossier/08-direction-decisions.md` | ✅ |
| Acceptance criteria (testable requirements) | `dossier/10-acceptance-criteria.md` | ✅ 100-item checklist |

**Per-feature gap pointer template:**

```markdown
## Layer 1 — 要件定義 pointers
- Persona: `dossier/01-personas.md` §{Tier X — {Persona}}
- Use case: `documents/01-business/{kiteclass|kitehub}/{domain}/use-cases.md` UC-{ID}
- Business rule: `documents/01-business/{kiteclass|kitehub}/{domain}/rules.md` BR-{ID}
- BRD (if legal): `documents/00-brd/legal/{doc}.md`
- AC: `dossier/10-acceptance-criteria.md` items {N..M}
```

---

## Layer 2 — 基本設計 (External / High-level Design)

**Where it lives:**

| Artifact category | Path | Coverage |
|-------------------|------|----------|
| Screen mockups (HTML prototypes) | `documents/02-architecture/design-system/ui_kits/{kit-name}/screens/*.html` | ✅ 76 screens R2+R3 |
| Screen inventory + scoring | `dossier/03-screen-inventory.md` | ✅ 64 routes catalogued |
| Component-level inventory | `dossier/04-component-gaps.md` (G1..G12) + `12-modal-dialog-inventory-{kc,kh}.md` (D-prefix) + `14-common-components-inventory-{kc,kh}.md` | ✅ 201 artifacts catalogued |
| Error/Layout/Loading inventory | `dossier/15-error-layout-inventory.md` | ✅ 15 cross-app artifacts |
| Quality bar (per-screen /128 rubric) | `dossier/06-quality-bar.md` | ✅ |
| Tech constraints (browser/device/perf) | `dossier/09-tech-constraints.md` | ✅ |
| Kit-level README (entry doc per kit) | `ui_kits/{kit}/README.md` | ✅ per-kit (R2+R3) |

**Per-feature gap pointer template:**

```markdown
## Layer 2 — 基本設計 pointers
- HTML kit reference: `ui_kits/{kit-name}/` (avg score X/128)
- Specific screens: `ui_kits/{kit}/screens/{screen-1}.html`, `{screen-2}.html`, ...
- Screen inventory row: `dossier/03-screen-inventory.md` line {N}
- Acceptance criteria: `dossier/10-acceptance-criteria.md` items {N..M}
- Error/loading states (if relevant): `dossier/15-error-layout-inventory.md` row {N}
```

---

## Layer 3 — 詳細設計 (Internal / Low-level Design)

**Where it lives:**

| Artifact category | Path | Coverage |
|-------------------|------|----------|
| Architecture Decision Records (ADRs) | `documents/02-architecture/adr/*.md` | ✅ ADR-001..ADR-023+ shipped |
| State machines (codified in rules) | `.claude/rules/ai-branding-guidelines.md` §6 (lifecycle) + `feedback_*` memories for behavioral rules | ✅ AI Branding lifecycle codified |
| Design patterns (project-wide) | `.claude/rules/design-patterns.md` (Strategy/State/Outbox/Saga/etc.) + `documents/02-architecture/ai-branding-design-patterns.md` | ✅ Catalog + anti-patterns |
| API contracts (per-domain) | `documents/01-business/{kiteclass,kitehub}/{domain}/api-contract.md` | ✅ Per-domain (3-layer mandate) |
| Outbox / event flows | ADR-021 (per-module outbox pattern) + ADR-016 (FE↔BE contract) | ✅ |
| Algorithms / sequences (when complex) | ADR + inline sequence diagrams in 02-architecture/diagrams or 06-diagrams | ⚠️ partial (some flows without explicit sequence) |
| Tech-stack decisions | `dossier/09-tech-constraints.md` + ADRs | ✅ |
| Logging / observability standards | `.claude/rules/logs-format-standard.md` + GAP-114/115 ADRs | ✅ |

**Per-feature gap pointer template:**

```markdown
## Layer 3 — 詳細設計 pointers
- ADR (if architectural decision): `documents/02-architecture/adr/ADR-{NNN}-{slug}.md`
- State machine: `.claude/rules/{rule}.md §{section}` OR component spec.md "State machine" section
- API contract: `documents/01-business/{kiteclass|kitehub}/{domain}/api-contract.md`
- Sequence diagram (if flow-heavy): `documents/06-diagrams/{flow-name}.puml`
- Design pattern applied: `.claude/rules/design-patterns.md §{N} {Pattern}`
```

**Common 詳細設計 gaps in our project (informal observation):**
- Per-kit state machines for FE flows (currently codified in shared rule files only)
- Sequence diagrams for cross-service flows (some documented in ADRs, some not)
- Frontend internal state management decisions (Redux vs Zustand vs server state) — partial

---

## Layer 4 — コンポーネント設計 (Component / Parts Design)

**Where it lives:**

| Artifact category | Path | Coverage |
|-------------------|------|----------|
| Component spec.md (G1..G12) | `ui_kits/components/G*/spec.md` | ✅ 12/12 Round 3 closed |
| Component state files (HTML demos) | `ui_kits/components/G*/states/*.html` | ✅ 76 demo states |
| Component dependency catalog | `dossier/04-component-gaps.md` | ✅ G1..G12 |
| Modal/dialog catalog | `dossier/12-modal-dialog-inventory-{kc,kh}.md` | ✅ 14 sites catalogued, D1..D10 specs pending GAP-279 |
| Common (non-G*) components catalog | `dossier/14-common-components-inventory-{kc,kh}.md` | ✅ 108 artifacts |
| Shared lib decision (Track 2) | GAP-273 §"Tech direction" — Option A/B/C deferred to wave kickoff | 🔵 OPEN |
| Per-kit "Components used" reference | Kit README sections (some kits explicit, some implicit) | ⚠️ inconsistent — rule §2.2 mandates explicit |

**Per-feature gap pointer template:**

```markdown
## Layer 4 — コンポーネント設計 pointers
- Components used: G{N} ({Component name}) — `dossier/04-component-gaps.md` §G{N}
- Component spec: `ui_kits/components/G{N}-{slug}/spec.md`
- Modal used (if any): D{N} — `dossier/12-modal-dialog-inventory-{kc|kh}.md` row {M}
- Common components: `dossier/14-common-components-inventory-{kc|kh}.md` §{subdir}
- Shared lib (Track 2 production): post-GAP-273 — `packages/shared-ui/{component}/` OR `{frontend}/src/components/{component}/`
```

---

## Per-kit completeness check (apply §2.2 matrix)

For each kit folder under `ui_kits/{kit-name}/`, kit README MUST contain (or link to) all 4 layer pointers:

| Layer | Required README content | Sample pointer text |
|-------|------------------------|---------------------|
| 1. 要件定義 | "Persona" + "Use cases" sections | "Designed for Pa. Parent (Tier 2) per `dossier/01-personas.md`. Implements UC-PARENT-01, UC-PARENT-02 from `documents/01-business/kiteclass/parent-portal/use-cases.md`." |
| 2. 基本設計 | "What's in this kit" + "Self-scoring" sections | "17 mobile-first PWA-grade screens. Avg 114/128 ⭐. Score table below." |
| 3. 詳細設計 | "State machines / Data flow" section + cross-link to ADRs | "Notification permission state machine: idle → requesting → granted/denied (browser API). Web Push payload contract: ADR-019. PWA installability: ADR-020." |
| 4. コンポーネント設計 | "Components used" section enumerating G*/D* | "Imports G6 (Invoice Detail), G7 (Parent Invite), G10 (Payment Timeline). Modal D1 (generic confirm) used in destructive actions." |

**Kit README compliance status (2026-04-30 spot-check):**

| Kit | Layer 1 | Layer 2 | Layer 3 | Layer 4 |
|-----|:-------:|:-------:|:-------:|:-------:|
| kiteclass-pro-v2 | ✅ | ✅ | ⚠️ implicit (no kit-specific ADR) | ⚠️ implicit |
| kiteclass-parent | ✅ | ✅ | ⚠️ partial (PWA + Zalo OA pattern noted) | ✅ explicit |
| kiteclass-teacher | ✅ | ✅ | ⚠️ partial | ⚠️ implicit |
| kiteclass-student | ✅ | ✅ | ⚠️ partial (PWA + saved-draft pattern noted) | ⚠️ implicit |
| kitehub-pro-v2 | ✅ | ✅ | ⚠️ implicit | ⚠️ implicit |
| kitehub-admin | ✅ | ✅ | ⚠️ partial (hierarchy nav) | ⚠️ implicit |
| ai-branding-wizard-v2 | ✅ | ✅ | ✅ explicit (cites `ai-branding-guidelines.md` §6 lifecycle + §2.5 input cap) | ✅ explicit |

**Gap:** 6 of 7 kits ⚠️ implicit at layer 3. **Recommendation:** when any kit's README is next touched (e.g., during Track 2 port), upgrade Layer 3 + 4 sections to explicit pointer enumeration.

This is grandfather-friendly — no retroactive cleanup wave required, but each touch must improve.

---

## Mapping per-domain (KC + KH)

### KiteClass domains

| Domain | Layer 1 (rules.md) | Layer 2 (kit screens) | Layer 3 (ADR / api-contract) | Layer 4 (components used) |
|--------|--------------------|-----------------------|------------------------------|---------------------------|
| Owner dashboard | `01-business/kiteclass/owner-dashboard/rules.md` | `ui_kits/kiteclass-pro-v2/screens/*.html` | `api-contract.md` + ADR-014 | G2/G5/G6/G7/G12 |
| Parent portal | `01-business/kiteclass/parent-portal/rules.md` | `ui_kits/kiteclass-parent/screens/*.html` | ADR-019 (Web Push) + ADR-020 (PWA) | G6/G7/G10 |
| Teacher | `01-business/kiteclass/teacher/rules.md` | `ui_kits/kiteclass-teacher/screens/*.html` | `api-contract.md` | G2/G3/G4/G8 |
| Student | (NEW persona — pending Phase 2 BRD) | `ui_kits/kiteclass-student/screens/*.html` | (Phase 2 ADR pending Track 2 port) | G6/G8/G10 + (TBD) |
| Public marketing (KC) | (NEW persona — Prospects, pending GAP-274 BRD update) | (NEW kit — pending GAP-274) | (NEW ADR — landing page architecture) | (TBD) |

### KiteHub domains

| Domain | Layer 1 | Layer 2 | Layer 3 | Layer 4 |
|--------|---------|---------|---------|---------|
| Customer dashboard (P2 Owner KH) | `01-business/kitehub/billing/rules.md` + `branding/rules.md` | `ui_kits/kitehub-pro-v2/screens/*.html` | ADR-016 (FE↔BE contract) + ADR-021 (per-module outbox) | G9/G10/G11 |
| K-12 School Principal | (Phase 2 BRD pending — `00-brd/personas-catalog.md` P5 partial) | `ui_kits/kitehub-admin/screens/*.html` | (Phase 2 ADR pending — hierarchy nav + MoET compliance) | G1/G3/G4/G8/G10 |
| AI Branding (Direction C wizard) | `01-business/kiteclass/ai-agent-workflow/rules.md` (BR-INPUT-CAP-001..007) | `ui_kits/ai-branding-wizard-v2/screens/*.html` | `.claude/rules/ai-branding-guidelines.md` §6 lifecycle + §2.5 input cap + ADR-021 outbox | G11 |
| Platform admin (KH ops) | (NEW persona — pending GAP-278 BRD) | (NEW kit — pending GAP-278) | (NEW ADR — admin separation from K-12) | (TBD — likely uses G9 + admin tables) |

---

## Track 2 production port — per-port checklist

When kicking off any Track 2 port wave (GAP-266..280), verify:

```markdown
## Pre-port 4-layer check

| Layer | Question | Status |
|-------|----------|:------:|
| 1 | Persona + use cases documented? | ✅/⚠️/❌ |
| 2 | Kit screens exist with score ≥105/128? | ✅/⚠️/❌ |
| 3 | State machines + ADRs identified? Components dependencies clear? | ✅/⚠️/❌ |
| 4 | Shared lib components ported (GAP-273) OR per-app duplication path? | ✅/⚠️/❌ |
```

**Block port if any layer ❌.** File follow-up gap inline + retry after.

---

## Anti-patterns (from real incidents)

| ❌ Anti-pattern | ✅ Correct application |
|-----------------|------------------------|
| Filing 8 Track 2 GAPs (GAP-266..273) without layer-2 inventory check (2026-04-29 incident) | Always run §2.1 4-layer check including layer-2 enumeration BEFORE filing |
| Building kit but skipping layer-3 cross-link to ADRs (6 of 7 R2/R3 kit READMEs ⚠️) | Add "State machines / Data flow" section linking ADRs |
| Treating "implicit" coverage as permanent | Implicit acceptable short-term; flag for explicit upgrade in next iteration |
| Using `dossier/04-component-gaps.md` only for G1..G12 (component lib is bigger) | Extended via `dossier/12-modal-dialog-inventory.md` (D-prefix) + `14-common-components-inventory.md` (subdir-organized) |
| Skip layer 4 because "it's just shadcn" | Project-specific composition patterns ARE layer 4 — needs spec |

---

## Log

- **2026-04-30 (v1.0):** Created paired same-PR with `.claude/rules/design-layer-coverage.md` v1.0.0 per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Reference mapping doc consumed by rule §2 matrix. Per-kit compliance spot-check completed (6 of 7 kits ⚠️ at layer 3 — grandfather-friendly upgrade path documented). Per-domain mapping covers KC + KH.
