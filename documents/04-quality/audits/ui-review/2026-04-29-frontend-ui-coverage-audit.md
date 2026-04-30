# Frontend UI Coverage Audit — 2026-04-29

**Audit type:** Cross-reference matrix audit (production frontend × HTML kit prototypes)
**Wave:** Wave UI Coverage Audit (`documents/03-planning/waves/wave-2026-04-29-ui-coverage-audit.md`)
**Methodology:** 2 parallel background agents (KC + KH enumeration) + coordinator synthesis
**Trigger:** User-flagged miss after Track 2 gap filing — "UI của tất cả screen/model/dialog/common đã cover hết chưa? phải audit để coverage 100% frontend và phải có evidence"
**Evidence sources:**
- `dossier/03-screen-inventory.md` — 64 routes (40 KC + 24 KH)
- `dossier/12-modal-dialog-inventory-{kc,kh}.md` — 14 distinct modals (5 KC + 9 KH usage sites)
- `dossier/14-common-components-inventory-{kc,kh}.md` — 108 components (81 KC + 27 KH)
- `dossier/15-error-layout-inventory.md` — 15 system files (5 KC + 10 KH)

---

## 1. Executive summary

| Layer | Total artifacts | ✅ Explicit | ⚠️ Implicit | ❌ Missing | Coverage |
|-------|:---------------:|:-----------:|:-----------:|:----------:|:--------:|
| **Pages** | 64 | 15 | 23 | **26** | 59% non-missing |
| **Modals/Dialogs** | 14 | 1 | 3 | **10** | 29% non-missing |
| **Common components** | 108* | 21 | 41 | **23** | 57% non-missing (*excl. shadcn primitives) |
| **Error/Layout/Loading** | 15 | 2 | 7 | **6** | 60% non-missing |
| **Aggregate** | 201 | 39 (19%) | 74 (37%) | **65 (32%)** | **56% non-missing** |

**Verdict:** Track 2 GAP-266..273 covers ~56% of production frontend UI. **65 artifacts (32%) are explicitly missing kit coverage.** Filing GAP-274..280 closes the gap.

**Note:** "Implicit" coverage = persona/category covered but no exact 1:1 screen/modal/component in HTML kits. These benefit from the existing 8 Track 2 GAPs (designers can reference sister kits) but may need touch-ups.

---

## 2. Findings — uncovered artifacts driving follow-up GAPs

### 2.1 Public marketing pages (KC) → **GAP-274**

KC `(public)/` routes + landing components — 5 pages + 14 supporting components:

| Path | Type | Coverage |
|------|:----:|:--------:|
| `(public)/page.tsx` | Marketing landing | ❌ |
| `(public)/about/page.tsx` | About page | ❌ |
| `(public)/catalog/page.tsx` | Public course catalog | ❌ |
| `(public)/catalog/[id]/page.tsx` | Course detail (public) | ❌ |
| `(public)/contact/page.tsx` | Contact form | ❌ |
| `components/landing/CourseCard.tsx` + 12 `sections/*.tsx` + 2 `public/*.tsx` | Marketing components | ❌ |

**Persona:** Prospects (pre-tenant + course-discovery visitors).
**Why missing:** Direction A (kitehub-story marketing) was deliberately deferred per `dossier/08-direction-decisions.md` Decision 3. KC marketing was never planned in R2/R3.

### 2.2 Public marketing + blog (KH) → **GAP-275**

KH `(public)/` routes + 12 marketing components in `components/{public,landing,sections}`:

| Path | Type | Coverage |
|------|:----:|:--------:|
| `(public)/page.tsx` | KH SaaS marketing landing | ❌ |
| `(public)/pricing/page.tsx` | Pricing tier page | ❌ |
| `(public)/blog/page.tsx` | Blog index | ❌ |
| `(public)/blog/[slug]/page.tsx` | Blog post detail (MDX) | ❌ |
| `(public)/legal/dmca/page.tsx` | DMCA legal page | ❌ |
| `(public)/layout.tsx` | Public site shell | ❌ |

**Persona:** Prospects (pre-tenant evaluating SaaS).
**Why missing:** Direction A deferred (Decision 3). Replaces `kitehub-story v2` partially with content-realistic marketing kit + blog templates.

### 2.3 Auth flows beyond login → **GAP-276**

**KC** (4 pages + 5 form components):
| Path | Coverage |
|------|:--------:|
| `(auth)/register/page.tsx` | ❌ |
| `(auth)/register/student/page.tsx` | ❌ (student-specific signup) |
| `(auth)/forgot-password/page.tsx` | ❌ |
| `(auth)/reset-password/page.tsx` | ❌ |
| `components/auth/{Register,ForgotPassword,Reset}*Form.tsx` | ❌ |

**KH** (2 pages):
| Path | Coverage |
|------|:--------:|
| `(auth)/register/page.tsx` | ❌ |
| `(auth)/verify-email/page.tsx` | ❌ |

**Note:** `(auth)/parent-invite/[token]/page.tsx` (KC) is partially covered by component G7 (parent-invite flow) in `ui_kits/components/G7-parent-invite/` — implicit coverage. Listed under GAP-276 for unified auth flow kit.

**Persona:** All (pre-auth users).
**Scope:** Login + register + forgot-password + reset-password + verify-email + parent-invite redemption + (future: MFA setup, social login expansion).

### 2.4 Error pages + global error boundaries → **GAP-277**

| File | App | Coverage |
|------|:---:|:--------:|
| `(public)/error.tsx` | KC | ❌ |
| `(public)/not-found.tsx` | KC | ❌ |
| `(public)/loading.tsx` | KC | ❌ |
| `error.tsx` | KH | ❌ |
| `global-error.tsx` | KH | ❌ |
| `not-found.tsx` | KH | ❌ |

**Plus tech-debt items** flagged in `dossier/15-error-layout-inventory.md`:
- KC missing `global-error.tsx` (root error boundary)
- KC missing route-segment `error.tsx` for `(dashboard)` + `(auth)`
- KH missing `(admin)/error.tsx`
- Loading states inconsistent across both apps

**Persona:** All (degraded-UX moments).
**Scope:** 404 / 500 / runtime crash / maintenance / offline / loading skeletons — kit + best-practice fixes.

### 2.5 Platform admin (KH ops) → **GAP-278**

5 KH admin pages + 2 admin tables + 1 admin layout. **Distinct from `kitehub-admin/` HTML kit which targets P5 K-12 School Principal.**

| Path | Type | Coverage |
|------|:----:|:--------:|
| `(admin)/admin/page.tsx` | KH ops dashboard | ❌ |
| `(admin)/admin/instances/page.tsx` | Tenant instances list | ❌ |
| `(admin)/admin/instances/[id]/page.tsx` | Tenant instance detail | ❌ |
| `(admin)/admin/payments/page.tsx` | Cross-tenant payments | ❌ |
| `(admin)/admin/revenue/page.tsx` | Platform revenue analytics | ❌ |
| `(admin)/layout.tsx` | Platform admin shell | ❌ |
| `components/admin/AdminInstancesTable.tsx` | Admin table primitive | ❌ |
| `components/admin/AdminPaymentsTable.tsx` | Admin table primitive | ❌ |

**Persona:** KiteHub internal staff (not customer-facing).
**Why missing:** R2/R3 kits intentionally focused on tenant-facing personas. Internal admin scope deferred.
**Critical clarification:** `kitehub-admin/` HTML kit is for P5 K-12 School Principal (institutional tenant) — completely different surface (academic-calendar, bulk-import, conduct, fees, report-cards, multi-class-roster). Both kits coexist; this GAP-278 is for KH platform-ops viewpoint.

### 2.6 Common modals + dialogs catalog → **GAP-279**

10 distinct modal/dialog ❌ missing across both apps:

**KC (2 missing):**
| File | Use case |
|------|---------|
| `components/class/cancel-class.tsx` (rendered as `<Card>`, anti-pattern flagged) | Cancel class confirmation |
| `confirm-dialog.tsx` (`common/`) — exists but states not catalogued | Generic confirm with destructive variant |

**KH (8 missing):**
| File | Use case |
|------|---------|
| 2 `branding/Danger Zone` dialogs | Reset branding / Delete instance |
| 1 `(admin)/admin/instances/` row-action dialog | Suspend/resume tenant |
| 2 `(admin)/admin/instances/[id]/` detail dialogs | Force-rebrand / Migrate tenant |
| 3 `(admin)/admin/payments/` confirm dialogs | Confirm payment / Reject payment / Show QR |

**Scope:** Mirror G1..G12 component pattern (each modal = 4-6 state HTML demos + spec.md). Number → D1..D10. May split into 2 GAP-279.A (KC modals) + 279.B (KH modals) at wave kickoff if oversized.

### 2.7 Onboarding wizard (NEW finding) → **GAP-280**

Agent A flagged distinct from AI Branding wizard:

| File | Use case | Coverage |
|------|---------|:--------:|
| `components/onboarding/DashboardWelcome.tsx` | First-run welcome card | ❌ |
| `components/onboarding/OnboardingWizard.tsx` | Initial tenant first-run wizard (likely covers tenant creation, first class, invite teacher, etc.) | ❌ |

**Why distinct from `ai-branding-wizard-v2`:** AI Branding wizard is brand-asset generation. Onboarding wizard is tenant initial-setup flow. Different persona moments — both needed.

**Scope:** First-run welcome + multi-step initial-setup wizard for new tenants.

### 2.8 KH onboarding components → **GAP-280** extension

KH `components/onboarding/` (3 files) — same wizard/welcome pattern. Roll into GAP-280.

---

## 3. Findings — implicit coverage worth refining

These artifacts are NOT urgent gaps but warrant attention during Track 2 port:

### 3.1 Customer-flow refinement (KH)

3 pages marked ⚠️ implicit but used heavily:
- `(customer)/settings/page.tsx`
- `(customer)/billing/history/page.tsx`
- `(customer)/billing/upgrade/page.tsx`

**Action:** existing `kitehub-pro-v2` kit covers main customer dashboard but these sub-pages may need new HTML mocks. **Track 2 GAP-270 should explicitly include these 3 sub-pages in scope.** Annotation only — not new GAP.

### 3.2 Tech-debt (deferred to separate cleanup pass)

Flagged but NOT new GAPs:

| Issue | Location | Severity |
|-------|----------|:--------:|
| `branding/dynamic-attendance-form-list.tsx` filename mis-located (attendance pattern in branding folder) | KC `components/` | 🟡 P3 |
| `common/CourseCard.tsx` vs `landing/CourseCard.tsx` likely duplicate | KC `components/` | 🟡 P3 |
| `(dashboard)/classes/[id]/page.tsx` cancel-class renders as `<Card>` instead of `<Dialog>` | KC | 🟡 P2 |
| KC missing root `not-found.tsx` and `global-error.tsx` | KC `app/` | 🟡 P2 |
| Loading states inconsistent (only `(public)/loading.tsx` in KC, none in KH) | Both apps | 🟠 P1 |

These should be filed under existing tech-debt umbrella OR as small chore PRs (out of scope this audit).

---

## 4. Coverage matrix sample (illustrative — full data in dossier docs)

| UI artifact | Path | Type | Persona | Kit-covered? | Track 2 GAP |
|-------------|------|:----:|:-------:|:------------:|:-----------:|
| Owner home | KC `(dashboard)/page.tsx` | Page | P2 Owner | ✅ kiteclass-pro-v2 | GAP-266 |
| Parent home | KC `(dashboard)/parent/page.tsx` | Page | Pa. Parent | ✅ kiteclass-parent | GAP-267 |
| Teacher dashboard | KC `(dashboard)/teacher/dashboard/page.tsx` | Page | Teacher | ✅ kiteclass-teacher | GAP-268 |
| Student dashboard | KC `(dashboard)/student/page.tsx` | Page | Student | 🔴 NEW route | GAP-269 |
| KH SaaS dashboard | KH `(customer)/dashboard/page.tsx` | Page | P2 Owner KH | ✅ kitehub-pro-v2 | GAP-270 |
| K-12 Principal | (NEW route) | Page | P5 Principal | ✅ kitehub-admin | GAP-271 |
| AI Branding wizard | KH `(customer)/branding/wizard` | Page | P2/P3 | ✅ ai-branding-wizard-v2 | GAP-272 |
| Forgot password | KC `(auth)/forgot-password/` | Page | All | ❌ NONE | **GAP-276** |
| KH marketing landing | KH `(public)/page.tsx` | Page | Prospects | ❌ NONE | **GAP-275** |
| 404 page | KH `not-found.tsx` | Error | All | ❌ NONE | **GAP-277** |
| Platform admin instances | KH `(admin)/admin/instances/page.tsx` | Page | KH ops | ❌ NONE | **GAP-278** |
| Cancel class confirm | KC `components/class/cancel-class.tsx` | Modal | Owner/Teacher | ❌ NONE | **GAP-279** |
| Onboarding wizard | KC `components/onboarding/OnboardingWizard.tsx` | Wizard | New tenant | ❌ NONE | **GAP-280** |

Full per-app data in:
- `dossier/03-screen-inventory.md` (KC 40 + KH 24)
- `dossier/12-modal-dialog-inventory-kc.md`, `12-modal-dialog-inventory-kh.md`
- `dossier/14-common-components-inventory-kc.md`, `14-common-components-inventory-kh.md`
- `dossier/15-error-layout-inventory.md`

---

## 5. Recommendations — follow-up GAPs

| Gap | Title | Scope summary | Priority | Estimate |
|-----|-------|---------------|:--------:|:--------:|
| **GAP-274** | KC public marketing kit | 5 pages + 14 marketing components | P2 | ~1 wave |
| **GAP-275** | KH public marketing + blog kit (replaces kitehub-story v2 partially) | 5 pages + 12 components + MDX blog template | P2 | ~1-2 waves |
| **GAP-276** | Auth flows kit (KC+KH) | 6 pages + 5+ form components | P2 | ~1 wave |
| **GAP-277** | Error pages kit | 6 pages + best-practice fixes | P1 | ~1 wave |
| **GAP-278** | Platform admin kit (KH ops, distinct from kitehub-admin K-12) | 5 pages + 2 tables + 1 layout | P2 | ~1-2 waves |
| **GAP-279** | Common modals + dialogs catalog (D1..D10) | 10 modals × 4-6 states + specs | P2 | ~1 wave (component-style) |
| **GAP-280** | Onboarding wizard kit (initial tenant first-run) | 1 welcome card + multi-step wizard | P2 | ~1 wave |

**Total Track 2 scope after follow-ups:** GAP-266..280 = **15 gaps** (was 8). **Estimated Track 2 = 15-20 weeks** across multiple waves (was 10-15).

**Recommended sequence:**
1. **GAP-273** (12 components shared lib) — BLOCKING, port FIRST
2. **GAP-279 + GAP-277** — modals catalog + error pages (cross-cutting, used by all 7 kit ports)
3. **GAP-276** — auth flows (entry point for users)
4. **GAP-269 (student) + GAP-272 (wizard)** — highest-quality kits (116/115)
5. **GAP-266 + GAP-270** — owner dashboards (KC + KH)
6. **GAP-267 + GAP-268** — parent + teacher (KC)
7. **GAP-271 + GAP-280** — K-12 Principal + onboarding
8. **GAP-274 + GAP-275 + GAP-278** — public marketing + platform admin (low-traffic, last)

---

## 6. Confidence + limitations

**Confidence: HIGH** for ✅/❌ markers. **MEDIUM** for ⚠️ implicit markers (subjective — designer judgment whether sister kit suffices vs. needs new mock).

**Limitations:**
- Did NOT re-score each route per /128 rubric (deferred to ui-review skill — separate audit)
- Did NOT enumerate component INTERNAL props/states (covered by `dossier/04-component-gaps.md` for G1..G12, not extended to others)
- Did NOT include unit test files, Storybook (none), API routes
- Did NOT verify shadcn UI primitive usage patterns (assumes consumed correctly)

**Future audits** that would extend this:
- Per-route /128 scoring refresh (~2026-Q3 cycle)
- Modal state-coverage audit (does each modal have empty/loading/success/error/destructive states?)
- Accessibility audit (axe DevTools across full route map)

---

## 7. Sign-off

| Field | Value |
|-------|-------|
| **Auditor** | @nguyenvankiet (coordinator) + Agent A (KC enumeration) + Agent B (KH enumeration) |
| **Date** | 2026-04-29 |
| **Review standard** | `output-review-mandate.md` Section 1 (audit reports require evidence preserved) |
| **Status** | DONE — Phase 1 evidence + recommendations shipped. Phase 2 (file follow-up GAPs + ROADMAP sync) in same wave closure PR. |
| **Triggered follow-ups** | GAP-274 / GAP-275 / GAP-276 / GAP-277 / GAP-278 / GAP-279 / GAP-280 (filed in this PR) |

---

## 8. Log

- **2026-04-29:** Audit shipped via Wave UI Coverage Audit. 2 parallel background agents (PR #708 KH + #709 KC) enumerated 64 pages + 14 modal sites + 108 components + 15 error/layout files = **201 production UI artifacts**. Coverage finding: ✅ 19% explicit / ⚠️ 37% implicit / ❌ 32% missing. 7 follow-up GAPs (GAP-274..280) filed referencing this audit. Track 2 estimate revised 10-15 → 15-20 weeks.
