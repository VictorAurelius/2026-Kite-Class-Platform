# 12 — Modal/Dialog Inventory — KC FE

**Created:** 2026-04-29 (Wave UI Coverage Audit Agent A)
**Source:** `grep -rln "Dialog\|AlertDialog\|Sheet\|Drawer\|Popover" kiteclass/kiteclass-frontend/src --include="*.tsx"` (excluding `__tests__/`)
**Total component files containing modals/sheets/popovers:** 7 (including 4 shadcn/ui primitives + 3 domain-specific dialogs + 1 inline page-embedded dialog)

**Use this when:** designing kit screens — any kit must demo every dialog state (open/close/loading/error/success) for the dialogs that compose its persona's flows. Filing follow-up GAPs (e.g., GAP-279 modals catalog) uses this inventory as the source list.

---

## Coverage legend

- ✅ explicit — kit has matching modal demo (state showcase)
- ⚠️ implicit — modal triggered from kit-covered page but no explicit modal-state demo
- ❌ missing — no kit covers; candidate for follow-up GAP

---

## Domain-specific dialogs (3 files)

| File | Type | Triggered from | Persona | Use case (1 line) | Kit-covered? |
|------|:----:|----------------|:-------:|--------------------|:------------:|
| `components/attendance/attendance-detail-dialog.tsx` | Dialog | `(dashboard)/students/[id]/attendance/page.tsx` | Teacher / Parent | Show attendance details for a specific date (status, notes, recorded-by) | ⚠️ implicit — `kiteclass-teacher/screens/attendance-marking.html` covers marking, no separate detail-by-date dialog |
| `components/student/dynamic-attendance-detail-dialog.tsx` | Dialog (dynamic-import wrapper around `attendance-detail-dialog.tsx`) | Same as above (lazy-loaded variant) | Teacher / Parent | Code-split version of attendance detail dialog | ⚠️ implicit — same as above (kit-coverage status follows underlying dialog) |
| `components/ui/confirm-dialog.tsx` | AlertDialog (shadcn) | Multi-page generic confirmation primitive | All staff | Generic destructive-action confirm (publish / archive / delete) — used 3× in `(dashboard)/courses/[id]/page.tsx` for publish + archive + delete | ❌ missing — no kit demos confirm-dialog states (open + danger styling + cancel/confirm); candidate for GAP-279 modals catalog |

---

## Page-inline dialogs (1 file)

| File | Type | Triggered from | Persona | Use case (1 line) | Kit-covered? |
|------|:----:|----------------|:-------:|--------------------|:------------:|
| `(dashboard)/classes/[id]/page.tsx` | Inline conditional Card-as-dialog (`showCancelDialog` state) | Self (same page) | Teacher / Owner | Cancel a scheduled class with reason input — currently rendered as Card not Dialog component | ❌ missing — and arguably an anti-pattern (should use `<Dialog>` component); candidate for GAP-279 + tech-debt note |

---

## Layout-level sheets (1 file)

| File | Type | Triggered from | Persona | Use case (1 line) | Kit-covered? |
|------|:----:|----------------|:-------:|--------------------|:------------:|
| `components/layout/header.tsx` | Sheet (shadcn — left side) | Mobile header hamburger button | All authenticated users on mobile | Mobile sidebar navigation drawer (`SidebarNav` mounted inside Sheet) | ✅ explicit — `kiteclass-pro-v2/screens/dashboard-default.html` and `kiteclass-parent/screens/home-default.html` demo mobile nav drawer state |

---

## shadcn/ui primitives (3 files — building blocks, not standalone modals)

These are reusable primitives, not standalone modals — they are the building blocks the domain dialogs above import. Listed for completeness; kit demos should focus on the domain dialogs that compose them.

| File | Type | Used by | Kit-covered? |
|------|:----:|---------|:------------:|
| `components/ui/dialog.tsx` | Dialog (Radix UI wrapper) | `attendance-detail-dialog.tsx` + 0 other places (low usage — most dialogs use `confirm-dialog.tsx`) | n/a (primitive) |
| `components/ui/popover.tsx` | Popover (Radix UI wrapper) | `components/ui/calendar.tsx` (date picker), forms with date inputs | n/a (primitive) |
| `components/ui/sheet.tsx` | Sheet (Radix UI wrapper) | `components/layout/header.tsx` mobile nav | n/a (primitive) |

---

## Coverage breakdown

| State | Count | % | Notes |
|-------|:-----:|:-:|-------|
| ✅ explicit | 1 | 20% (of 5 non-primitive dialogs) | Mobile nav Sheet (covered by both pro-v2 + parent kits) |
| ⚠️ implicit | 2 | 40% | Attendance-detail dialog (2 file variants — eager + dynamic-import) — covered by attendance flows in teacher kit but no dedicated detail-modal demo |
| ❌ missing | 2 | 40% | Confirm-dialog (generic destructive-action AlertDialog), inline cancel-class dialog (anti-pattern, render-as-Card) |

**Distinct dialog instances** (deduplicating the dynamic-import wrapper):
- 4 distinct domain dialogs: attendance-detail, confirm-dialog (publish + archive + delete = 3 instances of same component), cancel-class inline, mobile-nav sheet

---

## Findings for follow-up GAPs

1. **GAP-279 candidate (Common modals + dialogs catalog kit):** Should include kit screens demoing:
   - `confirm-dialog` states (open, hovering destructive button, confirm, cancel, loading) — used by 3 distinct destructive flows in `courses/[id]/page.tsx` alone, plus likely more sites in KH FE
   - `attendance-detail-dialog` open state with realistic mock data (date + status grid + notes)
   - Mobile nav `Sheet` open state (already covered by 2 existing kits — confirm and document)

2. **Tech-debt note (not a coverage gap):** `(dashboard)/classes/[id]/page.tsx` cancel-class flow renders as `<Card>` instead of `<Dialog>` — recommend refactor when GAP-279 design lands so the kit and production code use the same primitive.

3. **Low Dialog usage caveat:** Only 1 file (`attendance-detail-dialog.tsx`) imports from `components/ui/dialog.tsx`. Most KC dialogs use `confirm-dialog` (AlertDialog) or render inline. Kit catalog should reflect actual usage frequency, not theoretical primitive count.

---

## Log

- **2026-04-29:** Initial enumeration by Wave UI Coverage Audit Agent A. Searched `kiteclass-frontend/src/{components,app}/**/*.tsx` for `Dialog`/`AlertDialog`/`Sheet`/`Drawer`/`Popover` usage; verified each match by reading surrounding context. 7 component files identified (4 primitives + 3 domain) + 1 inline page-embedded dialog. Coverage: 1 explicit / 2 implicit / 2 missing among 5 distinct domain dialogs.
