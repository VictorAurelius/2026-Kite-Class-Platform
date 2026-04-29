# 03 — Screen Inventory

**Total: 64 routes** (40 KiteClass + 24 KiteHub). Verified by `find ... -name page.tsx` 2026-04-29 (Wave UI Coverage Audit Agent A re-enumeration). Drift since last update: KC 39 → 40 (added `(dashboard)/teacher/dashboard/page.tsx` 230 LOC).

**Use this when:** Claude Design needs to know which screen to design next. Pick by Priority × Score (low score = high priority).

**UI score legend** (out of 128 — `/128` rubric: 4 dimensions × 4 points × 4 = 16 per dim, 4 dims, ~32 sub-criteria total):
- 🟢 100+ = good baseline
- 🟡 75–99 = needs polish
- 🟠 50–74 = needs major rework
- 🔴 <50 = redesign from scratch

**Mock data legend:**
- ✅ = realistic VN data ready (MSW seeded)
- 🟡 = partial mock data
- 🔴 = error state / mock auth failure / blank
- ⬜ = not yet captured

**Kit-coverage legend** (added 2026-04-29 Wave UI Coverage Audit):
- ✅ explicit — kit has matching screen (e.g., `kiteclass-pro-v2/screens/dashboard-default.html` covers `(dashboard)/page.tsx`)
- ⚠️ implicit — kit covers persona but no exact 1:1 screen
- ❌ missing — no kit covers; candidate for follow-up GAP

---

## KiteClass — 40 routes (avg 81/128)

### `(public)` — 5 routes — avg ~88/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority | Kit-coverage |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|:-------------|
| `/` | 92 | P1 | 90 | ✅ | All (landing) | — | — keep | ❌ missing — no public marketing kit (GAP-274 candidate) |
| `/about` | 255 | P3 | 88 | ✅ | All | — | — keep | ❌ missing (GAP-274) |
| `/catalog` | 231 | P2 | 85 | ✅ | Prospects | course discovery | — keep | ❌ missing (GAP-274) |
| `/catalog/[id]` | 403 | P2 | 86 | ✅ | Prospects | course detail | low | ❌ missing (GAP-274) |
| `/contact` | 105 | P3 | 88 | ✅ | All | — | — keep | ❌ missing (GAP-274) |

### `(auth)` — 6 routes — avg ~85/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority | Kit-coverage |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|:-------------|
| `/login` | 50 | P0 | **97** 🟢 | ✅ | All | — | — best in app | ✅ explicit — `kiteclass-student/screens/login.html` (student variant); KC owner login indirectly via dashboard kits' login state |
| `/register` | 90 | P0 | 88 | ✅ | All | UC-AUTH-01 | low | ❌ missing — no auth-flow kit (GAP-276 candidate) |
| `/register/student` | 43 | P1 | **82** | 🟡 | S. Student | student-self-reg | medium — date locale | ❌ missing (GAP-276) |
| `/forgot-password` | 42 | P2 | 85 | ✅ | All | — | low | ❌ missing (GAP-276) |
| `/reset-password` | 55 | P2 | **76** | 🟡 | All | — | medium — minimal styling | ❌ missing (GAP-276) |
| `/parent-invite/[token]` | 45 | P0 | 84 | 🟡 | Pa. Parent | parent-portal | high — Direction D core flow | ⚠️ implicit — `kiteclass-parent/` PWA kit covers parent post-redemption flows but not the redeem-token landing |

### `(dashboard)` — 28 routes — avg ~80/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority | Kit-coverage |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|:-------------|
| `/settings` | 43 | P1 | **74** 🟠 | 🟡 | P2/P3 | tenant-config | **medium — color pickers, file upload** | ⚠️ implicit — `kiteclass-pro-v2` covers owner settings; no dedicated settings screen yet |
| `/billing` | 160 | P1 | 86 | ✅ | P2/P3 | UC-PAY | low | ⚠️ implicit — `kiteclass-pro-v2` covers owner billing list (no dedicated screen); `components/G6-invoice-detail` covers invoice |
| `/billing/[id]` | 166 | P1 | 84 | ✅ | P2/P3 | invoice-detail | medium — VN format check | ✅ explicit — `components/G6-invoice-detail/` |
| `/billing/[id]/pay` | 104 | P0 | 82 | ✅ | P2/Student | UC-PAY-04 | **HIGH — Vietnamese payment flow** | ✅ explicit — `components/G5-payment-method-selector/` + `kiteclass-parent/screens/billing-pay.html` (parent variant) |
| `/branding/wizard` | 15 | P0 | 80 | 🟡 | P2/P3 | ai-branding | **HIGH — Direction C target** | ✅ explicit — `ai-branding-wizard-v2/` kit |
| `/classes` | 173 | P1 | 84 | ✅ | Teacher/Owner | class-mgmt | low | ⚠️ implicit — `kiteclass-pro-v2` owner class list |
| `/classes/[id]` | 405 | P1 | 82 | ✅ | Teacher | UC-CLASS | medium | ⚠️ implicit — covered partially by `kiteclass-teacher/screens/attendance-default.html` for class detail flow |
| `/classes/[id]/edit` | 79 | P2 | 80 | ✅ | Owner/Admin | class-edit | low | ⚠️ implicit — owner edit forms not specifically demoed |
| `/classes/[id]/attendance` | 249 | P0 | 84 | ✅ | Teacher | UC-ATT-01 | **HIGH — daily teacher flow** | ✅ explicit — `components/G2-attendance-roster/` + `kiteclass-teacher/screens/attendance-marking.html` |
| `/courses` | 102 | P1 | 84 | ✅ | Owner | course-list | low | ⚠️ implicit — `kiteclass-pro-v2` covers owner course list |
| `/courses/new` | 46 | P1 | 80 | ✅ | Owner | course-create | low | ⚠️ implicit — owner CRUD forms not specifically demoed |
| `/courses/[id]` | 239 | P2 | 82 | ✅ | Owner | course-detail | low | ⚠️ implicit |
| `/courses/[id]/edit` | 81 | P2 | 80 | ✅ | Owner | course-edit | low | ⚠️ implicit |
| `/courses/[id]/classes/new` | 73 | P2 | 80 | ✅ | Owner | class-create | low | ⚠️ implicit |
| `/students` | 110 | P0 | 78 | ✅ | Owner/Admin | UC-STU-01 | **HIGH — needs bulk-import button (GAP-137)** | ✅ explicit — `components/G1-bulk-import-dropzone/` + `kiteclass-pro-v2/screens/dashboard-default.html` student widgets |
| `/students/new` | 51 | P1 | 82 | ✅ | Owner/Admin | student-create | low | ⚠️ implicit |
| `/students/[id]` | 198 | P1 | 80 | ✅ | All staff | student-detail | low | ⚠️ implicit |
| `/students/[id]/edit` | 72 | P2 | 80 | ✅ | Owner/Admin | student-edit | low | ⚠️ implicit |
| `/students/[id]/attendance` | 249 | P1 | 82 | ✅ | Teacher/Parent | UC-ATT-VIEW | medium | ✅ explicit — `components/G8-attendance-calendar/` + `kiteclass-parent/screens/attendance-calendar.html` |
| `/teachers` | 110 | P2 | 84 | ✅ | Owner/Admin | teacher-list | low | ⚠️ implicit |
| `/teachers/new` | 49 | P2 | 80 | ✅ | Owner | teacher-create | low | ⚠️ implicit |
| `/teachers/[id]` | 157 | P2 | 80 | ✅ | Owner | teacher-detail | low | ⚠️ implicit |
| `/teachers/[id]/edit` | 81 | P3 | 80 | ✅ | Owner | teacher-edit | low | ⚠️ implicit |
| `/attendance` | 117 | P1 | 82 | ✅ | Teacher | attendance-overview | medium | ⚠️ implicit — `kiteclass-teacher/screens/attendance-default.html` is single-class scope |
| `/attendance/reports` | 417 | P0 | 80 | ✅ | Teacher/Admin | UC-ATT-REPORT | **HIGH — heaviest screen 417 LOC** | ✅ explicit — `kiteclass-teacher/screens/reports-overview-default.html` + variants |
| `/admin/attendance/stats` | 275 | P2 | 80 | ✅ | Admin | admin-att | low | ⚠️ implicit — covered by `kiteclass-pro-v2` admin section partially |
| `/parent` | 159 | P0 | **76** 🟡 | 🔴 | Pa. Parent | UC-PARENT | **HIGH — MVP placeholder only (GAP-139)** | ✅ explicit — `kiteclass-parent/screens/home-default.html` |
| `/teacher/dashboard` *(NEW since v1 — added 2026-04-29 enumeration)* | 230 | P0 | ⬜ | ⬜ | Teacher | UC-TEACHER-DASH | **HIGH — teacher home, was missing from prior catalog** | ✅ explicit — `kiteclass-teacher/` kit covers teacher home pattern |

### Top-level `/dashboard` (route group escape) — 1 route

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority | Kit-coverage |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|:-------------|
| `/dashboard` *(top-level, distinct from `(dashboard)/` group root which doesn't exist as a `page.tsx`)* | 362 | P0 | 84 | ✅ | P2 Center Owner | UC-DASH | **HIGH — Direction B core (kiteclass-pro)** | ✅ explicit — `kiteclass-pro-v2/screens/dashboard-default.html` |

> **Note (2026-04-29 reconciliation):** `(dashboard)/` is a Next.js route group (parens) — it does NOT contribute its own `page.tsx`. The dashboard landing route is `/dashboard` served by `app/dashboard/page.tsx` at top level. Prior v1 catalog conflated the two; clarified here.

---

## KiteHub — 24 routes (avg 59/128)

> **Note:** KH section is updated by Wave UI Coverage Audit Agent B. Content below preserved from prior version pending Agent B re-enumeration.

### `(public)` — 5 routes — avg ~92/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|
| `/` | 28 | P0 | 95 | ✅ | Prospects | marketing | — keep (study pattern) |
| `/pricing` | 16 | P0 | **98** 🟢 | ✅ | Prospects | pricing | — best in repo |
| `/blog` | 12 | P2 | 88 | ✅ | All | content | low |
| `/blog/[slug]` | 14 | P1 | 78 | ✅ | All | blog-detail | medium — needs custom 404 |
| `/legal/dmca` | 8 | P3 | 88 | ✅ | All | — | low |

### `(auth)` — 3 routes — avg ~88/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|
| `/login` | 32 | P0 | 88 | ✅ | P2/P3 | — | medium — missing forgot-password link (H-5) |
| `/register` | 28 | P0 | 88 | ✅ | Prospects | UC-SIGNUP | low |
| `/verify-email` | 24 | P1 | 88 | ✅ | Prospects | email-verify | low |

### `(customer)` — 11 routes — avg ~64/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|
| `/dashboard` | 88 | P0 | 80 | 🟡 | P2 Center Owner | UC-CUST-DASH | **HIGH — Direction B candidate** |
| `/settings` | 48 | P1 | 62 🟠 | ✅ | P2/P3 | tenant-config | medium (boosted +23 from baseline 39) |
| `/billing` | 20 | P1 | 39 🔴 | 🔴 | P2/P3 | billing-overview | **HIGH** |
| `/billing/history` | 18 | P2 | 50 🟠 | 🔴 | P2/P3 | payment-history | medium |
| `/billing/upgrade` | 16 | P0 | 39 🔴 | 🔴 | P2 | UC-UPGRADE | **HIGH — paywall conversion screen** |
| `/billing/payment/[id]` | 14 | P0 | **33** 🔴 | 🔴 | P2 | payment-detail | **CRITICAL — lowest score** |
| `/branding` | 14 | P0 | **33** 🔴 | 🔴 | P2/P3 | branding-hub | **CRITICAL — Direction C** |
| `/branding/templates` | 22 | P0 | 56 🟠 | 🟡 | P2/P3 | template-pick | **HIGH — Direction C step 5** |
| `/branding/assets` | 26 | P1 | 68 🟠 | 🟡 | P2/P3 | asset-mgmt | medium |
| `/branding/wizard` | 18 | P0 | 33 🔴 | 🔴 | P2/P3 | UC-BRAND-WIZ | **CRITICAL — Direction C heart** |
| `/instances/[id]` | 24 | P0 | 33 🔴 | 🔴 | P2/P3 | instance-detail | **CRITICAL — lifecycle UI** |

### `(admin)` — 5 routes — avg ~52/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|
| `/admin` | 16 | P1 | 43 🔴 | 🔴 | Internal admin | admin-dash | medium |
| `/admin/instances` | 18 | P0 | 51 🟠 | 🔴 | Internal admin | tenant-list | **HIGH — internal ops critical** |
| `/admin/instances/[id]` | 20 | P0 | 45 🔴 | 🔴 | Internal admin | tenant-detail | **HIGH** |
| `/admin/payments` | 16 | P1 | 52 🟠 | 🔴 | Internal admin | admin-pay | medium |
| `/admin/revenue` | 14 | P2 | 68 🟠 | 🟡 | Internal admin | admin-rev | low |

---

## Aggregate scoreboard

| App | Routes | Avg | Best | Worst | Mock-data ready | Round-2 priority count |
|-----|:------:|:---:|:----:|:-----:|:---------------:|:-----------------------:|
| **KiteClass** | 40 | 81/128 🟡 | Login 97 | Settings 74 | 36/40 (90%) | 8 HIGH |
| **KiteHub** | 24 | 59/128 🟠 | Pricing 98 | Branding/Billing-pay/Instance 33 | 8/24 (33%) | 9 CRITICAL/HIGH |
| **Total** | 64 | ~73/128 | — | — | 44/64 (69%) | **17 priority screens** |

---

## KC kit-coverage breakdown (Wave UI Coverage Audit 2026-04-29)

| State | Count | % | Notes |
|-------|:-----:|:-:|-------|
| ✅ explicit | 9 | 22.5% | login, billing/[id], billing/[id]/pay, branding/wizard, classes/[id]/attendance, students, students/[id]/attendance, attendance/reports, parent, /dashboard, teacher/dashboard (parent has explicit kit-screen mapping) |
| ⚠️ implicit | 21 | 52.5% | Most owner CRUD screens (courses/teachers/classes/students new+edit+detail) — covered indirectly by `kiteclass-pro-v2` owner kit; parent-invite/[token] redeem flow |
| ❌ missing | 10 | 25% | All 5 `(public)/` marketing pages + 5 of 6 `(auth)/` pages (register, register/student, forgot-password, reset-password, register, register/student) |

**Follow-up GAP candidates:**
- **GAP-274** (KC public marketing kit): covers 5 `(public)/` pages
- **GAP-276** (Auth flows kit): covers 5 KC `(auth)/` pages (register, register/student, forgot-password, reset-password, parent-invite/[token])

---

## Round 2 priority list — 17 screens

**Direction B / kiteclass-pro (4 screens):**
1. KC `/dashboard` (362 LOC, 84/128) — owner home
2. KC `/parent` (159 LOC, 76/128) — Direction D pivot target
3. KC `/attendance/reports` (417 LOC, 80/128) — heaviest screen
4. KH `/dashboard` (88 LOC, 80/128) — owner home

**Direction C / ai-branding-wizard-v2 (4 screens):**
5. KH `/branding` 33/128 🔴
6. KH `/branding/wizard` 33/128 🔴
7. KH `/branding/templates` 56/128 🟠
8. KC `/branding/wizard` (provisioning flow)

**Lifecycle + payment critical (5 screens):**
9. KH `/instances/[id]` 33/128 🔴 — lifecycle UI (NOT_STARTED → DEPLOYED)
10. KH `/billing/payment/[id]` 33/128 🔴
11. KH `/billing/upgrade` 39/128 🔴 — conversion paywall
12. KH `/billing` 39/128 🔴
13. KC `/billing/[id]/pay` — VN payment flow

**Admin internal ops (2 screens):**
14. KH `/admin/instances` 51/128 🟠
15. KH `/admin/instances/[id]` 45/128 🔴

**Daily teacher + bulk import (2 screens):**
16. KC `/classes/[id]/attendance` — teacher daily flow
17. KC `/students` — needs bulk import entry (GAP-137 P0)

---

## Out of scope for Round 2

- All `(public)` marketing pages (already 88-98/128) — but flagged for GAP-274/275 follow-up since no kit covers them
- Auth flows in good shape (login 97/128, register 88/128) — but flagged for GAP-276 follow-up since no kit covers them
- Course/teacher/student CRUD edit screens (80-82/128, batch redesign possible Round 3)
- Catalog detail, blog detail (P3, low traffic)

---

## Log

- **2026-04-29:** Wave UI Coverage Audit Agent A — re-enumerated KC FE from filesystem (`find ... -name page.tsx`). Drift: 39→40 (added `(dashboard)/teacher/dashboard/page.tsx` 230 LOC). LOC counts updated from current `wc -l`. Added Kit-coverage column with 3-state markers (✅/⚠️/❌). Reconciled `(dashboard)/` route group note vs top-level `/dashboard` page. Coverage breakdown: 9 explicit / 21 implicit / 10 missing. Drives GAP-274 (public marketing) + GAP-276 (auth flows) candidate filings.
