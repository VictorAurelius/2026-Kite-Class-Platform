# 03 — Screen Inventory

**Total: 63 routes** (39 KiteClass + 24 KiteHub). Round 1 bundle recreated ~6. Round 2 should prioritize the 12 lowest-scoring + the 8 missing-but-needed screens.

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

---

## KiteClass — 39 routes (avg 81/128)

### `(public)` — 5 routes — avg ~88/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|
| `/` | 12 | P1 | 90 | ✅ | All (landing) | — | — keep |
| `/about` | 8 | P3 | 88 | ✅ | All | — | — keep |
| `/catalog` | 14 | P2 | 85 | ✅ | Prospects | course discovery | — keep |
| `/catalog/[id]` | 18 | P2 | 86 | ✅ | Prospects | course detail | low |
| `/contact` | 16 | P3 | 88 | ✅ | All | — | — keep |

### `(auth)` — 6 routes — avg ~85/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|
| `/login` | 50 | P0 | **97** 🟢 | ✅ | All | — | — best in app, study pattern |
| `/register` | 42 | P0 | 88 | ✅ | All | UC-AUTH-01 | low |
| `/register/student` | 38 | P1 | **82** | 🟡 | S. Student | student-self-reg | medium — date locale |
| `/forgot-password` | 28 | P2 | 85 | ✅ | All | — | low |
| `/reset-password` | 22 | P2 | **76** | 🟡 | All | — | medium — minimal styling |
| `/parent-invite/[token]` | 36 | P0 | 84 | 🟡 | Pa. Parent | parent-portal | high — Direction D core flow |

### `(dashboard)` — 28 routes — avg ~80/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:----------------:|
| `/dashboard` | 363 | P0 | 84 | ✅ | P2 Center Owner | UC-DASH | **HIGH — Direction B core (kiteclass-pro)** |
| `/settings` | 74 | P1 | **74** 🟠 | 🟡 | P2/P3 | tenant-config | **medium — color pickers, file upload** |
| `/billing` | 18 | P1 | 86 | ✅ | P2/P3 | UC-PAY | low |
| `/billing/[id]` | 28 | P1 | 84 | ✅ | P2/P3 | invoice-detail | medium — VN format check |
| `/billing/[id]/pay` | 22 | P0 | 82 | ✅ | P2/Student | UC-PAY-04 | **HIGH — Vietnamese payment flow** |
| `/branding/wizard` | 52 | P0 | 80 | 🟡 | P2/P3 | ai-branding | **HIGH — Direction C target** |
| `/classes` | 16 | P1 | 84 | ✅ | Teacher/Owner | class-mgmt | low |
| `/classes/[id]` | 24 | P1 | 82 | ✅ | Teacher | UC-CLASS | medium |
| `/classes/[id]/edit` | 32 | P2 | 80 | ✅ | Owner/Admin | class-edit | low |
| `/classes/[id]/attendance` | 20 | P0 | 84 | ✅ | Teacher | UC-ATT-01 | **HIGH — daily teacher flow** |
| `/courses` | 14 | P1 | 84 | ✅ | Owner | course-list | low |
| `/courses/new` | 38 | P1 | 80 | ✅ | Owner | course-create | low |
| `/courses/[id]` | 26 | P2 | 82 | ✅ | Owner | course-detail | low |
| `/courses/[id]/edit` | 35 | P2 | 80 | ✅ | Owner | course-edit | low |
| `/courses/[id]/classes/new` | 30 | P2 | 80 | ✅ | Owner | class-create | low |
| `/students` | 20 | P0 | 78 | ✅ | Owner/Admin | UC-STU-01 | **HIGH — needs bulk-import button (GAP-137)** |
| `/students/new` | 24 | P1 | 82 | ✅ | Owner/Admin | student-create | low |
| `/students/[id]` | 28 | P1 | 80 | ✅ | All staff | student-detail | low |
| `/students/[id]/edit` | 30 | P2 | 80 | ✅ | Owner/Admin | student-edit | low |
| `/students/[id]/attendance` | 22 | P1 | 82 | ✅ | Teacher/Parent | UC-ATT-VIEW | medium |
| `/teachers` | 18 | P2 | 84 | ✅ | Owner/Admin | teacher-list | low |
| `/teachers/new` | 26 | P2 | 80 | ✅ | Owner | teacher-create | low |
| `/teachers/[id]` | 24 | P2 | 80 | ✅ | Owner | teacher-detail | low |
| `/teachers/[id]/edit` | 28 | P3 | 80 | ✅ | Owner | teacher-edit | low |
| `/attendance` | 16 | P1 | 82 | ✅ | Teacher | attendance-overview | medium |
| `/attendance/reports` | 24 | P0 | 80 | ✅ | Teacher/Admin | UC-ATT-REPORT | **HIGH — heaviest screen 417 LOC** |
| `/admin/attendance/stats` | 18 | P2 | 80 | ✅ | Admin | admin-att | low |
| `/parent` | 52 | P0 | **76** 🟡 | 🔴 | Pa. Parent | UC-PARENT | **HIGH — MVP placeholder only (GAP-139)** |

---

## KiteHub — 24 routes (avg 59/128) — re-enumerated 2026-04-29 (Wave UI Coverage Audit Agent B)

**Kit-coverage legend:** ✅ explicit (kit screen depicts route directly) · ⚠️ implicit (kit covers parent flow / state subset) · ❌ missing (no kit screen).

**`(admin) nuance:** existing KH `(admin)` group is **KH platform ops viewpoint** (KiteHub internal admin managing tenants, payments, revenue across the SaaS), DIFFERENT from `kitehub-admin/` HTML kit which targets **P5 K-12 School Principal persona** (institutional tenant admin). The `kitehub-admin` kit is therefore NOT coverage for `(admin)/admin/**` production pages — those are uncovered by any kit (informs GAP-278).

### `(public)` — 5 routes — avg ~92/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Kit-coverage | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:------------:|:----------------:|
| `/` | 28 | P0 | 95 | ✅ | Prospects | marketing | ❌ missing (kitehub-story v2 deferred per Decision 3) | — keep (study pattern) |
| `/pricing` | 16 | P0 | **98** 🟢 | ✅ | Prospects | pricing | ❌ missing (no marketing kit) | — best in repo |
| `/blog` | 12 | P2 | 88 | ✅ | All | content | ❌ missing | low |
| `/blog/[slug]` | 14 | P1 | 78 | ✅ | All | blog-detail | ❌ missing | medium — needs custom 404 |
| `/legal/dmca` | 8 | P3 | 88 | ✅ | All | — | ❌ missing | low |

→ **5/5 ❌ missing** — informs **GAP-275 (KH public marketing + blog kit)**.

### `(auth)` — 3 routes — avg ~88/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Kit-coverage | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:------------:|:----------------:|
| `/login` | 32 | P0 | 88 | ✅ | P2/P3 | — | ❌ missing | medium — missing forgot-password link (H-5) |
| `/register` | 28 | P0 | 88 | ✅ | Prospects | UC-SIGNUP | ❌ missing | low |
| `/verify-email` | 24 | P1 | 88 | ✅ | Prospects | email-verify | ❌ missing | low |

→ **3/3 ❌ missing** — informs **GAP-276 (Auth flows kit)**.

### `(customer)` — 11 routes — avg ~64/128

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Kit-coverage | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:------------:|:----------------:|
| `/dashboard` | 88 | P0 | 80 | 🟡 | P2 Center Owner | UC-CUST-DASH | ✅ explicit (`kitehub-pro-v2/screens/dashboard-{default,dark,empty,error,loading,success}.html`) | **HIGH — Direction B candidate** |
| `/settings` | 48 | P1 | 62 🟠 | ✅ | P2/P3 | tenant-config | ❌ missing (no settings screen in kitehub-pro-v2) | medium (boosted +23 from baseline 39) |
| `/billing` | 20 | P1 | 39 🔴 | 🔴 | P2/P3 | billing-overview | ✅ explicit (`kitehub-pro-v2/screens/billing-{default,dark,empty,loading}.html`) | **HIGH** |
| `/billing/history` | 18 | P2 | 50 🟠 | 🔴 | P2/P3 | payment-history | ❌ missing (no history screen in kit) | medium |
| `/billing/upgrade` | 16 | P0 | 39 🔴 | 🔴 | P2 | UC-UPGRADE | ⚠️ implicit (`(public)/pricing` shows tiers but not upgrade-flow specific) | **HIGH — paywall conversion screen** |
| `/billing/payment/[id]` | 14 | P0 | **33** 🔴 | 🔴 | P2 | payment-detail | ✅ explicit (`kitehub-pro-v2/screens/billing-payment.html`) | **CRITICAL — lowest score** |
| `/branding` | 14 | P0 | **33** 🔴 | 🔴 | P2/P3 | branding-hub | ✅ explicit (`kitehub-pro-v2/screens/branding-hub-{default,dark,loading,quota-empty}.html`) | **CRITICAL — Direction C** |
| `/branding/templates` | 22 | P0 | 56 🟠 | 🟡 | P2/P3 | template-pick | ✅ explicit (`ai-branding-wizard-v2/screens/step5-template-{grid,fullscreen,with-custom-prompt}.html`) | **HIGH — Direction C step 5** |
| `/branding/assets` | 26 | P1 | 68 🟠 | 🟡 | P2/P3 | asset-mgmt | ⚠️ implicit (assets surfaced inside branding hub kit; standalone assets manager not depicted) | medium |
| `/branding/wizard` | 18 | P0 | 33 🔴 | 🔴 | P2/P3 | UC-BRAND-WIZ | ✅ explicit (`ai-branding-wizard-v2/` — 28 screens × 6 wizard steps + states) | **CRITICAL — Direction C heart** |
| `/instances/[id]` | 24 | P0 | 33 🔴 | 🔴 | P2/P3 | instance-detail | ✅ explicit (`kitehub-pro-v2/screens/instance-{NOT_STARTED,GENERATING,REGENERATING,DEPLOYED,FAILED}.html`) | **CRITICAL — lifecycle UI** |

→ **6 ✅ + 2 ⚠️ + 3 ❌** — `/settings`, `/billing/history` are uncovered (customer-side); `/billing/upgrade` is implicit at best.

### `(admin)` — 5 routes — avg ~52/128 — KH platform ops viewpoint

| Path | LOC | Priority | Score | Mock | Persona | BR ref | Kit-coverage | Round 2 priority |
|------|-----|:--------:|:-----:|:----:|---------|--------|:------------:|:----------------:|
| `/admin` | 16 | P1 | 43 🔴 | 🔴 | KH platform ops admin (NOT P5) | admin-dash | ❌ missing (kitehub-admin kit targets P5 K-12 Principal — different persona) | medium |
| `/admin/instances` | 18 | P0 | 51 🟠 | 🔴 | KH platform ops admin | tenant-list | ❌ missing | **HIGH — internal ops critical** |
| `/admin/instances/[id]` | 20 | P0 | 45 🔴 | 🔴 | KH platform ops admin | tenant-detail | ❌ missing | **HIGH** |
| `/admin/payments` | 16 | P1 | 52 🟠 | 🔴 | KH platform ops admin | admin-pay | ❌ missing | medium |
| `/admin/revenue` | 14 | P2 | 68 🟠 | 🟡 | KH platform ops admin | admin-rev | ❌ missing | low |

→ **5/5 ❌ missing** — informs **GAP-278 (Platform admin KH ops kit)**. This is the largest uncovered cluster on the KH side because it's a viewpoint mismatch with `kitehub-admin/` kit (which serves P5 K-12 Principal, a tenant-side persona, not the KH platform's internal ops staff).

---

## Aggregate scoreboard

| App | Routes | Avg | Best | Worst | Mock-data ready | Round-2 priority count |
|-----|:------:|:---:|:----:|:-----:|:---------------:|:-----------------------:|
| **KiteClass** | 39 | 81/128 🟡 | Login 97 | Settings 74 | 35/39 (90%) | 8 HIGH |
| **KiteHub** | 24 | 59/128 🟠 | Pricing 98 | Branding/Billing-pay/Instance 33 | 8/24 (33%) | 9 CRITICAL/HIGH |
| **Total** | 63 | ~73/128 | — | — | 43/63 (68%) | **17 priority screens** |

---

## Round 2 priority list — 17 screens

**Direction B / kiteclass-pro (4 screens):**
1. KC `/dashboard` (363 LOC, 84/128) — owner home
2. KC `/parent` (52 LOC, 76/128) — Direction D pivot target
3. KC `/attendance/reports` (24 LOC, 80/128, 417 LOC component) — heaviest screen
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

- All `(public)` marketing pages (already 88-98/128)
- Auth flows in good shape (login 97/128, register 88/128)
- Course/teacher/student CRUD edit screens (80-82/128, batch redesign possible Round 3)
- Catalog detail, blog detail (P3, low traffic)
