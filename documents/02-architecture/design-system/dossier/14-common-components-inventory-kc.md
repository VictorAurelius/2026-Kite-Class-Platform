# 14 — Common Components Inventory — KC FE

**Created:** 2026-04-29 (Wave UI Coverage Audit Agent A)
**Source:** `ls kiteclass/kiteclass-frontend/src/components/{subdir}/*.tsx` per subdir, excluding `__tests__/`, `components/ui/` (shadcn primitives), modal-only files (catalogued in `12-modal-dialog-inventory-kc.md`), and G1..G12 kit components (catalogued in `04-component-gaps.md`).
**Total component files:** 81 across 21 subdirectories (including 1 top-level `error-boundary.tsx`)

**Use this when:** scoping kit screens — any kit covering a page must reference the components that page composes; missing kit coverage of a high-reuse component means kit demos will diverge from production. Filing GAP-274..279 follow-ups uses this inventory as the source list.

---

## Coverage legend

- ✅ explicit — kit has matching component demo
- ⚠️ implicit — kit covers the parent page but no isolated component demo
- ❌ missing — no kit covers; candidate for follow-up GAP

---

## G14 — ConsentBanner (PDPL 2023 cookie consent — public marketing surface)

**Added:** 2026-05-06 (Wave 23 Bucket E — GAP-353 Layer 5)
**Component ID:** G14
**Status:** 🆕 to-be-created (Wave 23 Bucket BC ships production component)

| Attribute | Detail |
|-----------|--------|
| **Purpose** | PDPL 2023 cookie / consent banner — public marketing surface; mandatory before processing personal data per Articles 11-13 (effective 2026-07-01) |
| **Production location** | `packages/shared-ui/src/components/ConsentBanner/` (Wave 23 Bucket BC) — shared between KC and KH frontends |
| **Kit mockup location** | `documents/02-architecture/design-system/ui_kits/kitehub-story-v2/screens/consent-banner.html` (Wave 23 Bucket E — this bucket) |
| **Mounted in** | `kiteclass-frontend/src/app/(public)/layout.tsx` (Wave 23 Bucket BC) — banner visible on `/`, `/about`, `/catalog`, `/contact` and any future `(public)/**` route |
| **Used by pages** | All KC `(public)/**` routes — landing, course catalog, course detail, about, contact (sister-mounted on KH `(public)/**` per shared component) |
| **Kit-covered?** | ✅ explicit — `kitehub-story-v2/screens/consent-banner.html` (kit-shared mockup; KC-side reuse via shared component import) |
| **Props** | `privacyHref` (default `/legal/privacy`), `cookieHref` (default `/legal/cookies`), `termsHref` (default `/legal/terms`), `lang` (default `vi`), `storageKey` (default `kite.consent.v1`) |
| **States** | `NOT_PROMPTED` → `PROMPTED` → `CONSENT_GIVEN[essential|analytics|marketing]` / `REJECTED` → `REVOKED` → `RE_PROMPTED` (12-month expiry default OR material policy change) |
| **Categories** | `essential` (locked-on, mandatory per BR-PDPL-CONSENT-002), `analytics` (opt-in), `marketing` (opt-in) |
| **CTAs (equal visual weight, no dark patterns)** | "Từ chối tất cả" / "Tuỳ chỉnh" / "Đồng ý tất cả" (per BR-PDPL-CONSENT-002) |
| **Dependencies** | LocalStorage adapter (`storage.ts` versioned key), `useConsent` hook (state + revocation flow), shadcn primitives (button, switch, dialog) |
| **WCAG** | AA — focus trap (`role="dialog"` + `aria-modal="false"` non-blocking), Esc to close (defaults reject = privacy-by-default), Tab cycling, `aria-live="polite"` for state changes |
| **Business rules** | `BR-PDPL-CONSENT-001..004` in `documents/01-business/kiteclass/marketing/rules.md` (Wave 23 Bucket A) — banner mandatory; granular toggles; 36mo retention (cross-link DR-03); revocation flow |
| **Related gaps** | GAP-353 (PDPL banner spec), GAP-368 (production legal pages — link targets), GAP-274 (KC marketing port — AC enriched Wave 23 Bucket E) |
| **Compliance** | PDPL 2023 Art 11-13 + Decree 13/2023/NĐ-CP Art 24 (effective 2026-07-01) |

---

## attendance/ — 19 files (largest subdir; teacher + student attendance UI)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `attendance/active-classes-table.tsx` | Table | `(dashboard)/attendance` | ⚠️ implicit |
| `attendance/attendance-calendar.tsx` | Calendar widget | `(dashboard)/students/[id]/attendance`, `(dashboard)/classes/[id]/attendance` | ✅ explicit — `components/G8-attendance-calendar/` |
| `attendance/attendance-detail-dialog.tsx` | Dialog | `(dashboard)/students/[id]/attendance` | ⚠️ implicit (catalogued in `12-modal-dialog-inventory-kc.md`) |
| `attendance/attendance-form-list.tsx` | Form list | `(dashboard)/classes/[id]/attendance` | ✅ explicit — `components/G2-attendance-roster/` |
| `attendance/attendance-form-row.tsx` | Form row | `attendance-form-list.tsx` (composed) | ✅ explicit — `G2-attendance-roster/` |
| `attendance/attendance-history-table.tsx` | Table | `(dashboard)/students/[id]/attendance` | ⚠️ implicit |
| `attendance/attendance-stats-cards.tsx` | KPI cards | `(dashboard)/admin/attendance/stats` | ⚠️ implicit |
| `attendance/attendance-stats-overview.tsx` | Stats panel | `(dashboard)/admin/attendance/stats`, `(dashboard)/attendance/reports` | ⚠️ implicit |
| `attendance/attendance-trends-chart.tsx` | Chart | `(dashboard)/admin/attendance/stats` | ⚠️ implicit |
| `attendance/class-stats-table.tsx` | Table | `(dashboard)/admin/attendance/stats` | ⚠️ implicit |
| `attendance/dynamic-active-classes-table.tsx` | Dynamic-import wrapper | `(dashboard)/attendance` | n/a (lazy variant) |
| `attendance/dynamic-attendance-calendar.tsx` | Dynamic-import wrapper | Same as `attendance-calendar.tsx` | n/a (lazy variant) |
| `attendance/dynamic-attendance-trends-chart.tsx` | Dynamic-import wrapper | Same as trends chart | n/a (lazy variant) |
| `attendance/dynamic-class-stats-table.tsx` | Dynamic-import wrapper | Same as class stats | n/a (lazy variant) |
| `attendance/enhanced-attendance-calendar.tsx` | Calendar (enhanced variant) | `(dashboard)/students/[id]/attendance` (preferred over basic calendar) | ✅ explicit — `G8-attendance-calendar/` |
| `attendance/index.ts` | Barrel export | All attendance pages | n/a (no UI) |
| `attendance/pending-attendance-badge.tsx` | Badge | `(dashboard)/attendance`, header notifications | ⚠️ implicit |
| `attendance/today-classes-widget.tsx` | Widget | `(dashboard)/dashboard`, `(dashboard)/teacher/dashboard` | ⚠️ implicit — covered in `kiteclass-pro-v2/screens/dashboard-default.html` widget grid; no isolated demo |

---

## auth/ — 5 files (login + register + parent-invite forms)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `auth/forgot-password-form.tsx` | Form | `(auth)/forgot-password/page.tsx` | ❌ missing — no auth-flow kit (GAP-276 candidate) |
| `auth/login-form.tsx` | Form | `(auth)/login/page.tsx` | ⚠️ implicit — `kiteclass-student/screens/login.html` covers student variant; no owner/teacher kit-screen for KC login form specifically (just login page mock) |
| `auth/parent-invite-form.tsx` | Form | `(auth)/parent-invite/[token]/page.tsx` | ⚠️ implicit — `kiteclass-parent/` kit covers post-redemption parent flow but not the invite-token redeem form |
| `auth/reset-password-form.tsx` | Form | `(auth)/reset-password/page.tsx` | ❌ missing — GAP-276 candidate |
| `auth/student-register-form.tsx` | Form | `(auth)/register/student/page.tsx` | ❌ missing — GAP-276 candidate (note: student kit has login but no register form) |

---

## billing/ — 7 files

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `billing/dynamic-invoice-detail-panels.tsx` | Dynamic-import wrapper | `(dashboard)/billing/[id]` | n/a (lazy variant) |
| `billing/dynamic-payment-form.tsx` | Dynamic-import wrapper | `(dashboard)/billing/[id]/pay` | n/a (lazy variant) |
| `billing/invoice-detail-panels.tsx` | Panel layout | `(dashboard)/billing/[id]` | ✅ explicit — `components/G6-invoice-detail/` |
| `billing/invoice-status-badge.tsx` | Badge | `(dashboard)/billing` list, `billing/[id]` | ⚠️ implicit |
| `billing/payment-form.tsx` | Form | `(dashboard)/billing/[id]/pay` | ✅ explicit — `components/G5-payment-method-selector/` (closely related; payment-form composes the selector) |
| `billing/payment-method-selector.tsx` | Selector | `payment-form.tsx` (composed) | ✅ explicit — `components/G5-payment-method-selector/` |
| `billing/payment-status-badge.tsx` | Badge | `(dashboard)/billing/[id]/pay` flow | ⚠️ implicit |

---

## branding/ — 11 files (wizard MFE — uses XState per `09-tech-constraints.md`)

### branding/ (top-level wizard infra)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `branding/dynamic-attendance-form-list.tsx` | (misplaced — appears unrelated to branding) | TBD | n/a (likely tech-debt — candidate for refactor; not a real branding component) |

> **Note:** `branding/dynamic-attendance-form-list.tsx` appears mis-located (filename suggests attendance, not branding). Tech-debt; flag for cleanup, not a real branding component.

### branding/wizard/ (provisioning wizard root)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `branding/wizard/BrandingWizard.tsx` | Wizard shell | `(dashboard)/branding/wizard/page.tsx` | ✅ explicit — `ai-branding-wizard-v2/` kit |
| `branding/wizard/RegenerateCounter.tsx` | Counter | Wizard preview step | ✅ explicit — `ai-branding-wizard-v2/` |
| `branding/wizard/SegmentPicker.tsx` | Segment control | Audience + tone steps | ✅ explicit — `ai-branding-wizard-v2/` |
| `branding/wizard/WizardProgress.tsx` | Progress bar | Wizard shell (header) | ✅ explicit — `ai-branding-wizard-v2/` |
| `branding/wizard/types.ts` | Types | All wizard files | n/a (no UI) |
| `branding/wizard/useBrandingWizard.ts` | Hook | Wizard root | n/a (no UI) |
| `branding/wizard/wizard-machine.ts` | XState machine | Wizard root | n/a (no UI) |

### branding/wizard/steps/ — 6 files (one per wizard step)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `branding/wizard/steps/AudienceStep.tsx` | Step screen | Wizard step 3 | ✅ explicit — `ai-branding-wizard-v2/` |
| `branding/wizard/steps/LogoStep.tsx` | Step screen | Wizard step 2 | ✅ explicit — `ai-branding-wizard-v2/` |
| `branding/wizard/steps/PreviewStep.tsx` | Step screen | Wizard step 6 (final) | ✅ explicit — `ai-branding-wizard-v2/` |
| `branding/wizard/steps/TemplateStep.tsx` | Step screen | Wizard step 5 | ✅ explicit — `ai-branding-wizard-v2/` |
| `branding/wizard/steps/ToneStep.tsx` | Step screen | Wizard step 4 | ✅ explicit — `ai-branding-wizard-v2/` |
| `branding/wizard/steps/WelcomeStep.tsx` | Step screen | Wizard step 1 | ✅ explicit — `ai-branding-wizard-v2/` |

---

## class/ — 1 file

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `class/dynamic-attendance-form-list.tsx` | Dynamic-import wrapper | Likely lazy variant of attendance-form-list | n/a (lazy variant) |

---

## cms/ — 1 file (P3 prospects-side editing)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `cms/CMSEditor.tsx` | WYSIWYG / rich-text editor | Likely tenant content editing pages (not yet wired) | ❌ missing — no kit covers CMS editing UI; minor priority since usage is unclear |

---

## common/ — 8 files (shared utilities used everywhere)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `common/CourseCard.tsx` *(also at `landing/CourseCard.tsx`)* | Card | (none in `common/`; this filename also appears in `landing/` — verify duplication) | (see landing/CourseCard.tsx below) |
| `common/data-table.tsx` | Table | All list pages (students, teachers, classes, courses) | ⚠️ implicit — appears in many kit screens but no isolated demo |
| `common/dynamic-data-table.tsx` | Dynamic-import wrapper | Same as data-table | n/a (lazy variant) |
| `common/error-alert.tsx` | Alert | Most pages (error states) | ⚠️ implicit — kit error states demo this implicitly (`dashboard-error.html`, `attendance-error.html`, etc.) |
| `common/index.ts` | Barrel export | All pages | n/a (no UI) |
| `common/loading-spinner.tsx` | Spinner | Most pages (loading states) | ⚠️ implicit — kit loading states demo this (`dashboard-loading.html`, `reports-loading.html`, etc.) |
| `common/search-input.tsx` | Input | List pages with filtering | ⚠️ implicit |
| `common/status-badge.tsx` | Badge | List pages, detail pages | ⚠️ implicit |

> **Note:** `common/CourseCard.tsx` is `3.7K` and `landing/CourseCard.tsx` is also `3.7K` — likely duplicate. Tech-debt; flag for dedup.

---

## features/ — 1 file

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `features/FeatureGate.tsx` | Gate / conditional | Wraps BASIC/PREMIUM-only UI; used in catalog page (verified by grep) | ❌ missing — no kit demos feature-gate UX (locked + upgrade-prompt + unlocked states); candidate for paywall demo gap |

---

## forms/ — 11 files (CRUD forms for all 4 entities + form primitives)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `forms/class-form.tsx` | Form | `(dashboard)/courses/[id]/classes/new`, `classes/[id]/edit` | ⚠️ implicit |
| `forms/course-form.tsx` | Form (heaviest, 13.2K) | `(dashboard)/courses/new`, `courses/[id]/edit` | ⚠️ implicit |
| `forms/dynamic-class-form.tsx` | Dynamic-import wrapper | Same | n/a (lazy variant) |
| `forms/dynamic-course-form.tsx` | Dynamic-import wrapper | Same | n/a (lazy variant) |
| `forms/dynamic-student-form.tsx` | Dynamic-import wrapper | Same | n/a (lazy variant) |
| `forms/dynamic-teacher-form.tsx` | Dynamic-import wrapper | Same | n/a (lazy variant) |
| `forms/form-input.tsx` | Input wrapper | All forms | n/a (primitive) |
| `forms/form-select.tsx` | Select wrapper | All forms | n/a (primitive) |
| `forms/form-textarea.tsx` | Textarea wrapper | All forms | n/a (primitive) |
| `forms/index.ts` | Barrel export | All forms | n/a (no UI) |
| `forms/student-form.tsx` | Form | `(dashboard)/students/new`, `students/[id]/edit` | ⚠️ implicit |
| `forms/teacher-form.tsx` | Form | `(dashboard)/teachers/new`, `teachers/[id]/edit` | ⚠️ implicit |

---

## landing/ — 1 file (public marketing reuse)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `landing/CourseCard.tsx` | Card | `(public)/catalog/page.tsx` (verified by grep) | ❌ missing — no public marketing kit (GAP-274 candidate) |

---

## layout/ — 7 files (app shell)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `layout/auth-layout.tsx` | Layout shell | `(auth)/layout.tsx` | ⚠️ implicit — kits show login screens but layout is structural |
| `layout/dashboard-layout.tsx` | Layout shell | `(dashboard)/layout.tsx` | ✅ explicit — every `kiteclass-pro-v2/screens/dashboard-*.html` demos this layout |
| `layout/footer.tsx` | Footer | Public + auth pages | ❌ missing |
| `layout/header.tsx` | Header (with mobile-nav Sheet) | Dashboard pages | ✅ explicit — covered in `kiteclass-pro-v2/screens/dashboard-default.html` |
| `layout/index.ts` | Barrel export | All layouts | n/a (no UI) |
| `layout/sidebar.tsx` | Sidebar nav | Dashboard pages | ✅ explicit — `kiteclass-pro-v2/screens/dashboard-default.html` |
| `layout/watermark-footer.tsx` | Watermark | FREE-tier owner views | ❌ missing — no kit demos FREE-tier watermark |

---

## onboarding/ — 2 files

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `onboarding/DashboardWelcome.tsx` | Welcome panel | First-time owner dashboard | ❌ missing — no kit demos owner first-run state |
| `onboarding/OnboardingWizard.tsx` | Wizard | First-time owner flow | ❌ missing — distinct from branding/wizard; covers initial tenant setup; candidate for onboarding kit gap |

---

## public/ — 2 files (public-page detail components)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `public/about-details.tsx` | Detail layout | `(public)/about/page.tsx` | ❌ missing — GAP-274 candidate |
| `public/contact-form.tsx` | Form | `(public)/contact/page.tsx` | ❌ missing — GAP-274 candidate |

---

## sections/ — 11 files (CMS-driven landing sections, public marketing)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `sections/AboutSection.tsx` | Section | `(public)/page.tsx` (tenant-customized landing) | ❌ missing — GAP-274 candidate |
| `sections/CTASection.tsx` | CTA | Same | ❌ missing — GAP-274 candidate |
| `sections/CertificatesSection.tsx` | Section | Same | ❌ missing — GAP-274 candidate |
| `sections/ContactSection.tsx` | Section | Same | ❌ missing — GAP-274 candidate |
| `sections/EnrollmentSection.tsx` | Section | Same | ❌ missing — GAP-274 candidate |
| `sections/FeaturesSection.tsx` | Section | Same | ❌ missing — GAP-274 candidate |
| `sections/HeroSection.tsx` | Hero | Same | ❌ missing — GAP-274 candidate |
| `sections/PlaceholderSection.tsx` | Empty/placeholder | When tenant hasn't configured a section | ❌ missing — GAP-274 candidate |
| `sections/PricingSection.tsx` | Section | Same | ❌ missing — GAP-274 candidate |
| `sections/TeachersSection.tsx` | Section | Same | ❌ missing — GAP-274 candidate |
| `sections/TemplateRenderer.tsx` | Renderer dispatch | Composes all sections by tenant config | ❌ missing — GAP-274 candidate |
| `sections/TestimonialsSection.tsx` | Section | Same | ❌ missing — GAP-274 candidate |

---

## seo/ — 1 file

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `seo/JsonLd.tsx` | JSON-LD structured data injector | Public pages (course detail, blog) | n/a (no visible UI) |

---

## settings/ — 2 files

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `settings/branding-settings.tsx` | Settings panel (10.5K) | `(dashboard)/settings/page.tsx` | ⚠️ implicit — kit covers branding wizard but not settings panel after-the-fact branding adjustments |
| `settings/preferences-settings.tsx` | Settings panel (10.4K) | `(dashboard)/settings/page.tsx` | ⚠️ implicit — settings page low-scoring (74/128); kit-coverage gap |

---

## student/ — 2 files (student-side dynamic-imports)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `student/dynamic-attendance-calendar.tsx` | Dynamic-import wrapper | Likely student attendance view | n/a (lazy variant) |
| `student/dynamic-attendance-detail-dialog.tsx` | Dynamic-import wrapper | Same | n/a (lazy variant; modal catalogued in 12-modal-dialog-inventory-kc.md) |

---

## tables/columns/ — 5 files (column defs for `data-table.tsx`)

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `tables/columns/attendance-columns.tsx` | Column defs | Attendance tables | ⚠️ implicit |
| `tables/columns/class-columns.tsx` | Column defs | Class tables | ⚠️ implicit |
| `tables/columns/course-columns.tsx` | Column defs | Course tables | ⚠️ implicit |
| `tables/columns/student-columns.tsx` | Column defs | Student tables | ⚠️ implicit |
| `tables/columns/teacher-columns.tsx` | Column defs | Teacher tables | ⚠️ implicit |

---

## theme/ — 3 files

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `theme/ThemePreviewPanel.tsx` | Preview panel | Branding wizard PreviewStep, settings | ✅ explicit — `components/G11-theme-preview/` + `ai-branding-wizard-v2/` |
| `theme/ThemeReceiver.tsx` | Theme injection (CSS vars) | App root | n/a (no visible UI) |
| `theme/ThemeSync.tsx` | Theme sync (RabbitMQ-driven) | App root | n/a (no visible UI) |

---

## Top-level — 1 file

| File | Type | Used by pages | Kit-covered? |
|------|:----:|---------------|:------------:|
| `error-boundary.tsx` | React error boundary | App root layout | ❌ missing — no kit demos error-boundary fallback UI; candidate for GAP-277 (error pages kit) |

---

## Coverage breakdown (excluding lazy-import variants + barrel exports + theme injectors with no UI)

Counting only files with visible UI that have a meaningful kit-coverage axis (~58 files):

| State | Count | % | Notes |
|-------|:-----:|:-:|-------|
| ✅ explicit | 17 | ~29% | Mostly G1..G12 components + branding wizard steps + dashboard layout/sidebar + attendance forms |
| ⚠️ implicit | 24 | ~41% | Most owner CRUD forms, badges, tables — covered in kit screens but no isolated demos |
| ❌ missing | 17 | ~30% | All `sections/*` (12), `public/*` (2), all auth-flow forms (4 of 5), `onboarding/*` (2), `features/FeatureGate`, `layout/footer` + `watermark-footer`, `cms/CMSEditor`, `error-boundary.tsx` |

**Note:** "lazy-import variants" (10 files prefixed `dynamic-`) are excluded from the coverage count because they wrap their non-dynamic counterparts with no UI difference.

---

## Findings for follow-up GAPs

1. **GAP-274 (KC public marketing kit)** must cover: all 12 `sections/*` files + `public/about-details.tsx` + `public/contact-form.tsx` + `landing/CourseCard.tsx` (= 14 components).

2. **GAP-276 (Auth flows kit)** must cover: 4 of 5 `auth/*` forms (forgot-password, reset-password, student-register, login KC owner variant) + parent-invite redeem-token form.

3. **GAP-277 (Error pages kit)** must cover: `error-boundary.tsx` fallback states + `common/error-alert.tsx` isolated demos.

4. **Onboarding kit gap (new candidate, not in coordinator's GAP-274..279 plan):** `onboarding/DashboardWelcome.tsx` + `onboarding/OnboardingWizard.tsx` — distinct from branding wizard; covers initial tenant first-run flow. Coordinator should consider whether to bundle with GAP-274 (public-side onboarding) or file separately.

5. **Tech-debt to flag (not a coverage gap):**
   - `branding/dynamic-attendance-form-list.tsx` — filename mis-located (attendance content, branding folder).
   - `common/CourseCard.tsx` (3.7K) and `landing/CourseCard.tsx` (3.7K) — likely duplicate, candidate for dedup.

6. **FREE-tier watermark** (`layout/watermark-footer.tsx`) — currently no kit demos the FREE-tier owner experience showing watermark + upgrade-CTA. Worth documenting in GAP-279 modals catalog OR a tier-state showcase.

---

## Log

- **2026-05-06 (Wave 23 Bucket E):** Added G14 ConsentBanner row (PDPL 2023 cookie consent — public marketing surface). Production component shipped Wave 23 Bucket BC (`packages/shared-ui/src/components/ConsentBanner/`); kit mockup at `kitehub-story-v2/screens/consent-banner.html` (Bucket E — this bucket). Cross-link to BR-PDPL-CONSENT-001..004 (Bucket A) + GAP-353 + GAP-368.
- **2026-04-29:** Initial enumeration by Wave UI Coverage Audit Agent A. Walked all 21 subdirs of `kiteclass-frontend/src/components/`. Excluded `__tests__/` (test code), `components/ui/` (shadcn primitives — not in scope per wave plan §3 Bucket A.3 instruction "excluding shadcn/ui primitives"), modal-only files (catalogued in `12-modal-dialog-inventory-kc.md`), and G1..G12 components (catalogued in `04-component-gaps.md`). Verified each file by `wc -l` size + grep for usage in `app/**`. 81 component files identified. Coverage: 17 explicit / 24 implicit / 17 missing of ~58 visible-UI files (excluding lazy-import wrappers + no-UI utilities + barrel exports).
