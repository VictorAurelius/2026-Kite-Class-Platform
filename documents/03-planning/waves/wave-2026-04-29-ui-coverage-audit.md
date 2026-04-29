---
title: Wave UI Coverage Audit — 100% production frontend coverage with evidence
status: active
created: 2026-04-29
updated: 2026-04-29
waves: [ui-coverage-audit]
gaps: []
predecessor: wave-2026-04-29-ui-kits-round-3.md
spawns_followups: [GAP-274, GAP-275, GAP-276, GAP-277, GAP-278, GAP-279]
---

# Wave UI Coverage Audit — 100% production frontend coverage with evidence

**Type:** Audit-only wave (NOT gap-closing, NOT prototype-shipping). Produces evidence document + spawns follow-up GAPs.
**Methodology:** wave-pack 2-agent parallel + 1 coordinator synthesis per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`
**Governance:** wave plan PR-FIRST per `feedback_wave_plan_through_pr.md`; agents background per `agent-background-spawn-default.md` v1.0.0
**Estimated wall-clock:** ~85 min (foundation 15 + 2 parallel agents ~40 longest + coordinator synthesis 30)

---

## 0. Why this audit

User flagged after Track 2 gap filing (GAP-266..273):
> "UI của tất cả các screen, model, dialog, common của frontend trong dự án đã được 2 gap cover update hết ui kits chưa? ... phải audit để coverage 100% frontend và phải có evidence, có lẽ documents phải có list màn hình, modal, ..."

Quick coordinator audit estimated **~60-70% coverage**. Significant unknowns:
- Public marketing pages (kitehub-story v2 deferred per Decision 3)
- Auth flows beyond login (register/forgot-password/reset-password/verify-email/parent-invite redemption)
- Error pages (404/500/error.tsx/global-error.tsx)
- Modals/dialogs (~16 files identified by quick grep)
- Common component subdirs (cms/, forms/, features/, landing/, brand/, onboarding/, seo/, ui/)

Per `output-review-mandate.md` Section 1 mandate: every output requires review standard + process + evidence preserved. Filing follow-up GAPs without coverage matrix evidence = unreviewed gap claim. This wave produces the evidence.

---

## 1. Brainstorm (per `core/brainstorming-methodology.md`)

### Q1 — Question Assumptions

**Assumption 1:** Existing `dossier/03-screen-inventory.md` (63 routes catalogued) is still accurate.
- Challenge: KC has 40 `page.tsx` files (verified via `find`) vs catalog 39 — drift since dossier creation.
- Mitigation: agents re-enumerate from filesystem, update dossier in same PR.

**Assumption 2:** "Coverage" means kit screens visually match production routes.
- Challenge: kits have 76 demo states/screens; production has 64 unique routes + 16 modals + N components. Not 1:1 mapping. Some kits cover MORE than current routes (e.g., kiteclass-student is greenfield).
- Mitigation: matrix uses 3-state coverage status (✅ explicit / ⚠️ implicit / ❌ missing) per artifact.

**Assumption 3:** All ~16 modals + N common components need separate catalog.
- Challenge: modals are typically inline in pages or in `components/` — they don't always have separate routes.
- Mitigation: agents catalog by file (path + type + parent page + persona), not by route.

### Q2 — Trade-off matrix

| Option | Pro | Con | Decision |
|--------|-----|-----|----------|
| 2 agents (KC + KH parallel) | Clean app boundary, true parallel | Need coordinator synthesis step | ✅ Pick |
| 1 agent enumerates both | Single artifact author | Serial, ~80min vs parallel ~40min | ❌ |
| 4 agents (split by group: auth/dashboard/admin/public per app) | Fine-grained | 4 buckets too granular for enumeration | ❌ |

### Q3 — Decision

**Pick 2 parallel agents + 1 coordinator synthesis.** Agent A = KC FE, Agent B = KH FE. Coordinator builds cross-ref matrix + files GAPs.

---

## 2. State-check (per `audit-to-gap-pipeline.md` Step 2.5)

Verified 2026-04-29:

| Artifact | Status | Action |
|----------|:------:|--------|
| `dossier/03-screen-inventory.md` | ✅ Exists, 63 routes (drift: KC 39→40) | Update in agent A |
| `dossier/04-component-gaps.md` | ✅ Exists, 12 components G1..G12 catalogued | Reference only (Round 3 closes 12/12) |
| `dossier/12-modal-dialog-inventory.md` | 🔴 Does NOT exist | Create in agent A (KC) + agent B (KH) |
| `dossier/14-common-components-inventory.md` | 🔴 Does NOT exist | Create in agent A (KC) + agent B (KH) |
| `dossier/15-error-layout-inventory.md` | 🔴 Does NOT exist | Create in coordinator (smaller scope, both apps) |
| `documents/04-quality/audits/2026-04-29-frontend-ui-coverage-audit.md` | 🔴 Does NOT exist | Create in coordinator synthesis |
| KC FE `page.tsx` count | 40 verified | Agent A re-enumerates |
| KH FE `page.tsx` count | 24 verified | Agent B re-enumerates |
| Modal/Dialog/Sheet files | 10 KC + 6 KH (grep) | Agents enumerate explicitly |
| Component subdirs | 10 KC (`attendance/auth/billing/branding/class/cms/common/features/forms/landing`) + 9 KH (`admin/billing/brand/branding/common/layout/onboarding/seo/ui`) | Agents enumerate per subdir |

State-check **PASS** — clear scope, no duplicate audit.

---

## 3. Scope

### Bucket A — KC frontend enumeration (Agent A)

Output 3 docs in `documents/02-architecture/design-system/dossier/`:

1. **Update** `03-screen-inventory.md` — KC section refreshed: 40 `page.tsx` files with current scores estimated, kit-coverage status per route. Add notes for routes added since v1 (compare with current 39 catalog).

2. **NEW** `12-modal-dialog-inventory-kc.md` — every Dialog/AlertDialog/Sheet/Drawer in `kiteclass-frontend/src/components/**/*.tsx`. Format: `| File | Type | Triggered from | Persona | Use case | Kit-covered? |`. Estimated 10+ files based on grep.

3. **NEW** `14-common-components-inventory-kc.md` — non-G1..G12 reusable components in `kiteclass-frontend/src/components/{cms,forms,features,landing,common}/**`. Skip G1..G12 (already catalogued). Format: `| File | Type | Used by pages | Kit-covered? |`.

**Constraint:** read-only enumeration. Do NOT modify production code. Output dossier docs only.

### Bucket B — KH frontend enumeration (Agent B)

Same 3-doc structure for KiteHub:
1. **Update** `03-screen-inventory.md` — KH section refreshed (24 routes verified)
2. **NEW** `12-modal-dialog-inventory-kh.md`
3. **NEW** `14-common-components-inventory-kh.md`

Subdirs to enumerate: `kitehub-frontend/src/components/{admin,brand,onboarding,seo,ui,common,layout}/**`.

### Bucket C — Coordinator synthesis (after A+B done)

1. **NEW** `dossier/15-error-layout-inventory.md` — smaller scope across both apps:
   - Error pages: `error.tsx`, `global-error.tsx`, `not-found.tsx`, route-level error boundaries
   - Layouts: `(auth)/layout`, `(dashboard)/layout`, `(admin)/layout`, `(customer)/layout`, `(public)/layout`, root `layout.tsx`
   - Loading states: `loading.tsx` files
   - Coverage status per artifact

2. **NEW** `documents/04-quality/audits/2026-04-29-frontend-ui-coverage-audit.md` — synthesis report:
   - Executive summary (% coverage by category)
   - Cross-reference matrix (every UI artifact → kit coverage status)
   - Gaps identified with evidence (file paths + reasoning)
   - Recommendations: GAP-274..279 scope per category

3. **File follow-up GAPs** (one PR commit) referencing audit as evidence:
   - **GAP-274:** KC public marketing kit (`/`, `/about`, `/catalog`, `/catalog/[id]`, `/contact`)
   - **GAP-275:** KH public marketing + blog kit (`/`, `/pricing`, `/blog`, `/blog/[slug]`, `/legal/dmca`) — replaces deferred kitehub-story v2 partially
   - **GAP-276:** Auth flows kit (KC `register`, `register/student`, `forgot-password`, `reset-password`, `parent-invite/[token]` + KH `register`, `verify-email`)
   - **GAP-277:** Error pages kit (404 / 500 / error.tsx / global-error.tsx / maintenance / offline) for both apps
   - **GAP-278:** Platform admin (KH ops) kit — distinct from kitehub-admin K-12 Principal scope. Covers `(admin)/admin/payments`, `revenue`, `instances/[id]` from KH ops viewpoint.
   - **GAP-279:** Common modals + dialogs catalog (D1..Dn — from agent enumeration). Mirror G1..G12 pattern with state showcase.

   Adjust gap count after agent enumeration reveals actual numbers (could split GAP-279 into multiple if >20 modals).

4. **Update** ROADMAP Cluster 14 row with revised follow-up GAP list referencing audit.

5. **Update** `dossier/03-screen-inventory.md` total counts + headers with R3 lessons (e.g., persona × direction matrix complete).

---

## 4. Out of scope (deliberately)

- **Unit test files** (`__tests__/**`) — not user-facing UI
- **Storybook stories** (none in repo currently)
- **API route handlers** (`api/**`) — backend, not UI
- **Third-party component lib internals** (shadcn primitives, Radix UI) — these are consumed, not catalogued
- **Production code modifications** — this is audit-only; modifications happen in Track 2 wave (GAP-266..279 follow-ups)
- **Re-running quality audit /128 scoring per route** — agents estimate coverage status only; full re-score deferred to ui-review skill (separate audit)

---

## 5. File overlap analysis

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `dossier/03-screen-inventory.md` | Agent A (KC section) + Agent B (KH section) | **SOFT** — different sections, coordinator merges at synthesis |
| `dossier/12-modal-dialog-inventory-kc.md` | Agent A only | None |
| `dossier/12-modal-dialog-inventory-kh.md` | Agent B only | None |
| `dossier/14-common-components-inventory-kc.md` | Agent A only | None |
| `dossier/14-common-components-inventory-kh.md` | Agent B only | None |
| `dossier/15-error-layout-inventory.md` | Coordinator only (not in agent scope) | None |
| `documents/04-quality/audits/2026-04-29-frontend-ui-coverage-audit.md` | Coordinator only | None |
| `documents/04-quality/gaps/GAP-274..279-*.md` | Coordinator only | None |
| `documents/04-quality/gaps/ROADMAP.md` | Coordinator only | None |

0 HARD conflicts. 1 SOFT (`03-screen-inventory.md` two sections — handled at coordinator synthesis).

---

## 6. Agent workflow

Per `agent-background-spawn-default.md` v1.0.0 + `feedback_parallel_agent_strategy.md`:
- 2 worktree-isolated agents, RELATIVE paths only
- `run_in_background: true` (default per new rule)
- Single message with 2 Agent tool calls (parallel spawn)
- Coordinator (parent) synthesizes after both report complete

**Agent A prompt scope:** KC FE enumeration → 3 dossier docs (KC sections only)
**Agent B prompt scope:** KH FE enumeration → 3 dossier docs (KH sections only)

Each agent reports back: file count enumerated per category + gap count claims (for coordinator GAP filing).

---

## 7. Acceptance criteria (wave-level)

- [ ] All 40 KC `page.tsx` files catalogued with kit-coverage status
- [ ] All 24 KH `page.tsx` files catalogued with kit-coverage status
- [ ] All Dialog/AlertDialog/Sheet/Drawer files in both apps catalogued
- [ ] All non-G1..G12 component subdirs catalogued (10 KC + 9 KH)
- [ ] Error pages + layouts + loading.tsx catalogued (coordinator scope)
- [ ] `documents/04-quality/audits/2026-04-29-frontend-ui-coverage-audit.md` produced as evidence
- [ ] Cross-reference matrix has 3-state coverage marker per artifact (✅ explicit / ⚠️ implicit / ❌ missing)
- [ ] GAP-274..279 (or revised list per audit findings) filed referencing audit
- [ ] ROADMAP Cluster 14 row updated with new gap list
- [ ] No `[ ]` unchecked AC at closure flip — per `gap-done-discipline.md` §2

---

## 8. Wall-clock target

| Phase | Target |
|-------|--------|
| Foundation PR (this) | ~15 min |
| 2 parallel agents | ~40 min wall (longest) |
| Coordinator synthesis (matrix + audit + GAPs + ROADMAP) | ~30 min |
| **Total** | **~85 min** |

---

## 9. Lessons-learned (filled post-wave SHIP)

_(filled post-wave)_

---

## 10. Log

- **2026-04-29 (kickoff):** Wave plan created on branch `wave/ui-coverage-audit`. Triggered by user-flagged miss after Track 2 gap filing (GAP-266..273): "UI của tất cả screen/model/dialog/common đã cover hết chưa?" Coordinator quick-audit estimated ~60-70% coverage, missing public marketing + auth flows + error pages + modals + non-G1..G12 components. Per `output-review-mandate.md` Section 1 evidence-preserved mandate, audit-with-evidence wave needed before filing follow-up GAPs. After this PR merges: 2 parallel background agents (KC, KH) enumerate + coordinator synthesizes audit + files GAP-274..279.
