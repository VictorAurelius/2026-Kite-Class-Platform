# 14 — Common Components Inventory — KiteHub FE

**Source:** `ls kitehub/kitehub-frontend/src/components/{admin,billing,brand,branding,common,layout,onboarding,seo,ui}/` (2026-04-29, Wave UI Coverage Audit Agent B).

**Scope:** non-G1..G12 reusable components in `kitehub-frontend/src/components/**`. Files already covered as G-components (catalogued in `04-component-gaps.md`) are noted with their G-id; everything else is enumerated here for kit-coverage triage.

**Use this when:** designing a kit, deciding whether to recreate a state or reuse an existing component, or scoping the modals/components catalog kit.

---

## Coverage marker legend

- ✅ **explicit** — depicted in a kit screen as a discrete module
- ⚠️ **implicit** — composed inside a kit screen (e.g., a card layout reuses CurrentPlanCard data shape but not pixel-for-pixel)
- ❌ **missing** — no kit screen depicts this component
- 🔁 **G-component** — already catalogued in `04-component-gaps.md` (out of this inventory's scope; kit coverage tracked there)
- 🧱 **shadcn primitive** — out of audit scope (consumed as design tokens only)

---

## `admin/` — KH platform ops tables (4 files)

| # | File | Used by pages | Kit-covered? | Note |
|:-:|------|---------------|:------------:|------|
| KH-A1 | `AdminInstancesTable.tsx` | `/admin/instances`, `/admin/instances/[id]` | ❌ missing | Tenant instance management table — no kit (kitehub-admin kit targets P5 K-12 Principal, not KH platform ops) |
| KH-A2 | `AdminPaymentsTable.tsx` | `/admin/payments` | ❌ missing | Manual payment confirmation queue — KH ops only, not in any kit |

(2 test files excluded.)

---

## `billing/` — Customer billing (9 files)

| # | File | Used by pages | Kit-covered? | Note |
|:-:|------|---------------|:------------:|------|
| KH-B1 | `ChangeConfirmation.tsx` | `/billing/upgrade` | ❌ missing | Plan-change preview/confirmation card |
| KH-B2 | `CurrentPlanCard.tsx` | `/billing` | ⚠️ implicit | `kitehub-pro-v2/screens/billing-default.html` shows a current-plan summary; component shape may differ |
| KH-B3 | `PaymentHistoryTable.tsx` | `/billing/history` | ❌ missing | Payment history not in `kitehub-pro-v2` (only billing default/empty/payment/dark/loading) |
| KH-B4 | `PaymentInfo.tsx` | `/billing/payment/[id]` | ⚠️ implicit | `kitehub-pro-v2/screens/billing-payment.html` covers the payment flow; component-level reuse implicit |
| KH-B5 | `PaymentStatusCard.tsx` | `/billing/payment/[id]` | ⚠️ implicit | Same — billing-payment.html depicts status visualization |
| KH-B6 | `PlanComparison.tsx` | `/billing/upgrade` | ❌ missing | Pricing-tier comparison table — `(public)/pricing` covers plan compare for prospects but not the upgrade flow |
| KH-B7 | `QRCodeDisplay.tsx` | `/billing/payment/[id]`, admin payments QR preview | ⚠️ implicit | Implied in `kitehub-pro-v2/screens/billing-payment.html` (VietQR pattern); standalone QR component not depicted |
| KH-B8 | `StepIndicator.tsx` | Billing flow + onboarding wizard | ⚠️ implicit | Generic step indicator — appears in branding-wizard-step* screens of `kitehub-pro-v2` and `ai-branding-wizard-v2` |
| KH-B9 | `TierSelector.tsx` | `/billing/upgrade` | ⚠️ implicit | Tier picker — `(public)/pricing` shows tier cards but upgrade-specific selector variation not depicted |

(1 test file excluded.)

---

## `brand/` — KiteHub identity (1 file)

| # | File | Used by pages | Kit-covered? | Note |
|:-:|------|---------------|:------------:|------|
| KH-BR1 | `KiteLogo.tsx` | All KH layouts (header logo) | ✅ explicit | Logo appears across every kitehub kit; not a "screen" but visually consistent. Marking ✅ as logo identity is part of every kit's header |

---

## `branding/` — AI Branding wizard step components (5 files)

⚠️ **Overlap:** these compose into `/branding/wizard` page; pixel-for-pixel design lives in `ai-branding-wizard-v2/screens/`. Component-level reuse is **implicit** in the wizard page kit.

| # | File | Used by pages | Kit-covered? | Note |
|:-:|------|---------------|:------------:|------|
| KH-BW1 | `AnalyzeStep.tsx` | `/branding/wizard` (analyze phase) | ⚠️ implicit | Wizard kit covers welcome/audience/template/preview steps but no dedicated "analyze" screen — covered in `kitehub-pro-v2/screens/instance-GENERATING.html` (analyze + generate combined) |
| KH-BW2 | `GenerateStep.tsx` | `/branding/wizard` (generate phase) | ⚠️ implicit | Same — generation in-progress depicted via lifecycle GENERATING state |
| KH-BW3 | `ReviewStep.tsx` | `/branding/wizard` (review phase) | ✅ explicit | `ai-branding-wizard-v2/screens/step6-preview-default.html` + `step6-quality-gate-pass.html` + `step6-quality-gate-fail.html` cover review states |
| KH-BW4 | `UploadStep.tsx` | `/branding/wizard` (upload logo) | ✅ explicit | `ai-branding-wizard-v2/screens/step2-logo-{default,error,skip,uploaded}.html` — 4 states covered |
| KH-BW5 | `ThemePreviewCard.tsx` | `/branding/wizard` review + `/branding/templates` | ⚠️ implicit | G11 (theme-preview) catalogues this component pattern; check `04-component-gaps.md` for G11 coverage |

(1 test file excluded.)

---

## `common/` — Cross-cutting building blocks (5 files)

| # | File | Used by pages | Kit-covered? | Note |
|:-:|------|---------------|:------------:|------|
| KH-C1 | `EmptyState.tsx` | Many pages (billing empty, instances empty, payments empty, etc.) | ⚠️ implicit | `kitehub-pro-v2/screens/billing-empty.html`, `dashboard-empty.html`, `branding-hub-quota-empty.html` depict empty states — component-level standalone showcase missing |
| KH-C2 | `ErrorAlert.tsx` | Many pages (form errors, API errors) | ⚠️ implicit | `kitehub-pro-v2/screens/dashboard-error.html` shows error state; standalone alert component variants not catalogued |
| KH-C3 | `LoadingSpinner.tsx` | Many pages | ⚠️ implicit | Loading states present in `kitehub-pro-v2/screens/*-loading.html`; standalone spinner not depicted |
| KH-C4 | `StatusBadge.tsx` | Instance pages (lifecycle), billing, payments | ⚠️ implicit | Used in lifecycle screens (`instance-NOT_STARTED..DEPLOYED.html`); standalone badge color/state matrix not depicted |
| KH-C5 | `TrialCountdown.tsx` | `/dashboard` (customer hub) | ⚠️ implicit | Likely visible in `kitehub-pro-v2/screens/dashboard-default.html`; standalone countdown widget pattern not isolated |

(5 test files excluded.)

---

## `layout/` — Page chrome (4 files)

| # | File | Used by | Kit-covered? | Note |
|:-:|------|---------|:------------:|------|
| KH-L1 | `AdminLayout.tsx` | `(admin)/layout` | ❌ missing | KH platform admin chrome (sidebar + topbar for KH ops) — no kit covers; `kitehub-admin/` kit is for P5 K-12 Principal, not KH ops admin |
| KH-L2 | `DashboardLayout.tsx` | `(customer)/layout` | ✅ explicit | Customer dashboard chrome — covered in `kitehub-pro-v2/screens/dashboard-*.html` headers |
| KH-L3 | `PublicLayout.tsx` | `(public)/layout` | ⚠️ implicit | Public marketing chrome — current `(public)/page.tsx` (production score 95/128) implies pattern, but no dedicated `kitehub-story` kit (deferred per Decision 3) |
| KH-L4 | `Sidebar.tsx` | Admin + Customer layouts | ⚠️ implicit | Sidebar nav patterns visible in dashboard kit; standalone sidebar variants (collapsed / expanded / mobile drawer) not isolated |

(1 test file excluded.)

---

## `onboarding/` — First-login wizard (1 file)

| # | File | Used by pages | Kit-covered? | Note |
|:-:|------|---------------|:------------:|------|
| KH-O1 | `OnboardingWizard.tsx` | `/dashboard` (post-register modal) | ⚠️ implicit | 6-step modal-wrapped wizard. Underlying steps covered by `ai-branding-wizard-v2` step screens; the modal-shell + first-login trigger context not depicted as discrete kit screen |

---

## `seo/` — Structured data + metadata (2 files)

| # | File | Used by pages | Kit-covered? | Note |
|:-:|------|---------------|:------------:|------|
| KH-S1 | `JsonLd.tsx` | `(public)` pages (LD+JSON injection) | N/A | Non-visual — SEO metadata only, no kit needed |
| KH-S2 | `schemas.ts` | `JsonLd.tsx` | N/A | Type definitions — out of audit scope |

(1 test file excluded.)

---

## `ui/` — shadcn primitives (out of audit scope)

🧱 All 21 files under `components/ui/` (alert, alert-dialog, badge, button, card, checkbox, dialog, dropdown-menu, gradient-button, gradient-text, input, label, page-header, progress, section-title, select, separator, switch, table, tabs, textarea) are shadcn-grade design primitives — consumed as building blocks, not catalogued as separate components.

Note: KH uses 4 custom shadcn components (`gradient-button`, `gradient-text`, `page-header`, `section-title`) per `dossier/09-tech-constraints.md`. These are noted in `kitehub-pro-v2/README.md` as KH-only stack additions.

(3 test files excluded.)

---

## Coverage breakdown

| Subdir | Total components (excl. tests + `ui/` + `seo/`) | ✅ explicit | ⚠️ implicit | ❌ missing |
|--------|:-----------------------------------------------:|:----------:|:-----------:|:---------:|
| `admin/` | 2 | 0 | 0 | **2** |
| `billing/` | 9 | 0 | 6 | **3** |
| `brand/` | 1 | 1 | 0 | 0 |
| `branding/` | 5 | 2 | 3 | 0 |
| `common/` | 5 | 0 | 5 | 0 |
| `layout/` | 4 | 1 | 2 | **1** |
| `onboarding/` | 1 | 0 | 1 | 0 |
| `seo/` | 2 (excluded) | — | — | — |
| `ui/` | 21 (shadcn) | — | — | — |
| **Total (audited)** | **27** | **4** | **17** | **6** |

**Key findings:**
- 6 ❌ missing components — all platform-admin (KH ops) or customer billing flow specific:
  - 2 platform admin tables (KH-A1 instances, KH-A2 payments)
  - 3 customer billing components (KH-B1 change confirmation, KH-B3 payment history, KH-B6 plan comparison)
  - 1 admin layout (KH-L1) — no kit covers KH platform-admin chrome
- High implicit coverage rate (17/27 = 63%) due to `kitehub-pro-v2` covering customer billing/branding/dashboard pages — but standalone component variants (state matrix per primitive) not isolated as showcase screens
- Wizard step components (KH-BW1..5) overlap with `ai-branding-wizard-v2` kit — coverage is genuinely good for review/upload steps, weaker for analyze/generate phases (covered via lifecycle states, not as dedicated wizard step screens)

These ❌ findings inform:
- **GAP-278 (Platform admin KH ops kit)** — 2 admin tables + 1 admin layout
- **GAP-279 (Common modals + dialogs catalog)** + sister modal kit — shows component-state matrix per primitive
- Possibly a billing-flow refinement gap covering KH-B1/B3/B6 inside an existing or new billing kit

---

## Out of scope

- Test files (`__tests__/**`) — verified-by-test, not user-facing UI
- shadcn `ui/` primitives — consumed as tokens; component-level redesign not appropriate
- SEO components (`seo/JsonLd`, `schemas.ts`) — non-visual metadata
- Storybook — none in repo currently
- API route handlers (`api/**`) — backend, not UI

---

## Cross-references

- Personas: `dossier/01-personas.md`
- Production routes: `dossier/03-screen-inventory.md` § KiteHub
- G-components catalog: `dossier/04-component-gaps.md`
- Modal/dialog inventory: `dossier/12-modal-dialog-inventory-kh.md`
- Wave plan: `documents/03-planning/waves/wave-2026-04-29-ui-coverage-audit.md` § Bucket B
- Sister inventory (KC): `dossier/14-common-components-inventory-kc.md`
