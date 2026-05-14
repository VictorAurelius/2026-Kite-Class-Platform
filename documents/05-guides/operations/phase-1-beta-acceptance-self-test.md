---
title: Phase 1 BETA Acceptance Self-Test Matrix — README
status: active
created: 2026-05-14
updated: 2026-05-14
wave: 72a
gaps: [GAP-372, GAP-480, GAP-514, GAP-515, GAP-516, GAP-517, GAP-518, GAP-519, GAP-520, GAP-521, GAP-523, GAP-524, GAP-525]
supersedes: documents/03-planning/end-user/plan-1-self-test-e2e.md
---

# Phase 1 BETA Acceptance Self-Test Matrix

**File:** [`phase-1-beta-acceptance-self-test.csv`](phase-1-beta-acceptance-self-test.csv)

**Mục đích:** Exhaustive, pre-filled, tick-trackable matrix cho mọi flow user-facing trong Phase 1 BETA. User mở CSV trong spreadsheet, walk từng row, tick `status` column. Zero data composition required — mọi input field đã được pre-fill với sample test data.

**Replaces:** [`documents/03-planning/end-user/plan-1-self-test-e2e.md`](../../03-planning/end-user/plan-1-self-test-e2e.md) (Wave 69 7-step checklist) — file cũ scope hẹp (chỉ E2E request→approve→signup→create-class), không cover Owner provisioning wizard, branding regenerate, attendance, billing tier, admin ops, off-boarding, parent/student flows. CSV mới expand thành matrix toàn diện cho mọi persona Phase 1 BETA.

---

## 1. Cách dùng CSV

### 1.1 App khuyến nghị

| Platform | Recommended app |
|---|---|
| Linux / WSL | LibreOffice Calc (`libreoffice --calc phase-1-beta-acceptance-self-test.csv`) |
| Windows | Microsoft Excel (double-click) hoặc Google Sheets (File → Import) |
| macOS | Numbers, Excel, hoặc Google Sheets |
| Terminal-only | `column -t -s, phase-1-beta-acceptance-self-test.csv \| less -S` |

### 1.2 Workflow

1. **Mở CSV** trong spreadsheet app.
2. **Walk row-by-row theo `flow_id` order** (CSV đã sort theo flow prefix + step_num).
3. **Đọc `prerequisite` column** — nếu bước trước chưa pass, KHÔNG tick step hiện tại.
4. **Đọc `action` + `input_data`** — copy `input_data` cells vào form/API/UI tương ứng.
5. **Verify per `expected_result`** — dùng method trong `verify_via` column (UI banner, DB query, Network tab, email inbox, v.v.).
6. **Tick `status` cell:**
   - `pass` — bước work hoàn toàn như expected
   - `fail` — bước fail; copy error message vào `notes`
   - `blocked` — bước không chạy được vì gap blocker (xem `blocker_gap` column)
   - `-` — bước skip (out-of-scope cho phase này, vd `Phase 1.5+ scope`)
7. **Nếu `blocker_gap` có giá trị** (vd `GAP-518`) → bước này CHẮC CHẮN fail/blocked cho đến khi gap đó DONE. Tick `blocked` + tham chiếu trong `notes`.
8. **Khi gặp bug ngoài expected** → file follow-up gap theo `audit-to-gap-pipeline.md` §3 + reference dòng CSV trong gap description.

### 1.3 Status column legend

| Value | Meaning |
|---|---|
| (empty) | Chưa walk bước này |
| `pass` | Bước work end-to-end như expected |
| `fail` | Bước có expected behavior nhưng không hoạt động → file gap |
| `blocked` | Bước bị block bởi gap chưa fix → xem `blocker_gap` |
| `-` | Bước skip per scope (Phase 1.5+, deferred feature, v.v.) |

### 1.4 Sample test data convention

Mọi `input_data` cells đã pre-fill với:

- **Email pattern:** `*@kitehub-test.local` (clearly non-prod namespace; nếu cần real email cho verify email flow, swap by user)
- **Password:** `KiteTest!2026A` (single test password reused; meets §2.3 12+ char + mixed case + digit + symbol per `pre-launch-auth-hardening-checklist.md`)
- **Phone:** Vietnamese format `09xxxxxxxx`
- **Name:** Vietnamese sample names
- **Dates:** Sample DOB / schedule dates relative to test session

---

## 2. Scope coverage

Matrix covers TẤT CẢ user-facing flows Phase 1 BETA per:

- **Personas (P1+P2 in scope per Release 1 plan §3.1):** Anonymous, Pre-tenant, Platform_Admin, P2_Center_Owner, Teacher, Pa_Parent, Student
- **Phases (per CSV `phase` column):** Setup, Auth, Onboarding, Provisioning, Branding, Class_Mgmt, Attendance, Grade, Payment, Settings, Admin_Ops, Data_Export, Off-boarding
- **Flow prefix conventions:**

| Prefix | Persona | Description |
|---|---|---|
| `BETA-REQ-*` | Anonymous | Beta access request submission |
| `ADM-*` | Platform_Admin | Admin platform ops (approve/reject/suspend/instances) |
| `OWNER-*` | P2_Center_Owner | Tenant owner workflows (provisioning, branding, class mgmt, teachers, students, payments, settings, off-boarding) |
| `TEACH-*` | Teacher | Teacher workflows (dashboard, attendance, grades) |
| `PARENT-*` | Pa_Parent | Parent workflows (dashboard, attendance/grades/payments view) |
| `STU-*` | Student | Student workflows (Phase 1.5+ scope mostly — marked `-` in status) |
| `EMAIL-*` | Multi-persona | Email-driven flows (verify, reset, beta invite) |
| `PUB-*` | Anonymous | Public marketing pages |

### 2.1 Out-of-scope flows (marked `-` in status với note)

Per Release 1 Plan §3 — Phase 1 BETA scope LOẠI những features sau (in CSV nhưng status pre-set `-`):

- **Payment processing** — Phase 1.5 scope (per Release 1 plan §1.5). BETA = no real payment, only billing read-only views.
- **Student persona K-12** — Phase 3 scope (P5 K-12). BETA chỉ P1+P2 personas.
- **Public signup form** — Phase 1.5 scope. BETA chỉ "Request Beta Access" → admin approve → invite email.
- **AI Branding regenerate full** — quota counter visible nhưng regenerate có thể limited trong BETA period.
- **DSAR/RTBF user-facing** — Phase 1.5 + Phase 2 K-12 scope per PDPL phases.

---

## 3. Blocker gaps (Wave 72a in-flight)

Tại 2026-05-14, các blocker gaps sau đây đang in-flight; rows có `blocker_gap` column reference chúng:

| Gap | Title | Affected rows |
|---|---|---|
| `GAP-514` | Rate limit auth endpoints | `BETA-REQ-005`, `OWNER-LOGIN-005`, abuse-smoke rows |
| `GAP-515` | Account lockout after N failed logins | `OWNER-LOGIN-006`, `OWNER-LOGIN-007` |
| `GAP-518` | FE admin role-guard mismatch (BE seed `PLATFORM_ADMIN` vs FE expects `'ADMIN'`) | ALL `ADM-*` rows |
| `GAP-519` | Admin dashboard nav missing | `ADM-NAV-*` rows |
| `GAP-520` | Admin credential not in handoff message | `ADM-LOGIN-*` rows |
| `GAP-521` | Admin audit log entity | `ADM-AUDIT-*` rows |
| `GAP-523` | Beta request endpoint route mismatch | `BETA-REQ-*` rows |
| `GAP-524` | Verification email not delivered | `EMAIL-VERIFY-*` rows |
| `GAP-525` | Beta-signup flow incomplete | `BETA-REQ-007+`, `OWNER-SIGNUP-*` |

Sau Wave 72a closure PR ships, user re-walk CSV và xóa giá trị `blocker_gap` từ rows có gap đã DONE.

---

## 4. Iteration cadence

| When | Action |
|---|---|
| After Wave 72a closes | Re-walk all `ADM-*` rows (GAP-514..521 unblock) |
| After Wave 72b closes | Re-walk OWNER provisioning wizard rows (KitehHub branding) |
| After 5 P1+P2 tenants live | Re-walk full matrix as "soft launch acceptance test" → trigger Phase 2 advance per Release 1 plan §2 phase progression |
| Each new Phase 1.5 PAID feature ship | Append rows for new flow OR mark previously `-` rows as `pass` candidates |

---

## 5. Persona Coverage Map

| Persona (per `documents/00-brd/personas-catalog.md`) | Phase 1 BETA scope | Flow prefixes |
|---|---|---|
| Anonymous (public visitor) | ✅ In scope | `PUB-*`, `BETA-REQ-*` |
| Pre-tenant (post-approve, pre-signup) | ✅ In scope | `EMAIL-VERIFY-*`, `OWNER-SIGNUP-*` |
| **P1 Solo Teacher** | ✅ In scope | Maps to `OWNER-*` flows với tenant size <10 students |
| **P2 Small Center Owner** | ✅ In scope | `OWNER-*`, `TEACH-*`, `PARENT-*` (full breadth) |
| P3 Medium Center | ⏳ Phase 2 | (Not covered) |
| P5 K-12 School | ⏳ Phase 3 | (Not covered) |
| Student (S) | ⏳ Phase 3 K-12 mostly | `STU-*` rows marked `-` Phase 1.5+ |
| Platform Admin (internal) | ✅ In scope (post Wave 72a) | `ADM-*` |

---

## 6. Reference

- **Personas catalog:** [`documents/00-brd/personas-catalog.md`](../../00-brd/personas-catalog.md)
- **Release 1 plan §3 Phase 1 BETA scope:** [`documents/03-planning/roadmap/release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md)
- **Superseded plan (file cũ):** [`documents/03-planning/end-user/plan-1-self-test-e2e.md`](../../03-planning/end-user/plan-1-self-test-e2e.md) — Wave 69 7-step E2E. Kept cho lịch sử nhưng new walks dùng CSV.
- **ROADMAP §🚀 Next Action:** [`documents/04-quality/gaps/ROADMAP.md`](../../04-quality/gaps/ROADMAP.md)
- **Rule mandate (verify the FLOW):** [`.claude/rules/pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md)

---

## 7. How findings flow back

Mỗi `fail` row → file gap theo `audit-to-gap-pipeline.md` §3 với:
- **`## Problem`** copies CSV row's `expected_result` vs actual finding
- **`## Related`** references CSV `flow_id` (vd `Triggered by phase-1-beta-acceptance-self-test.csv row BETA-REQ-003`)
- **Update CSV `notes` column** với gap ID once filed
- **Re-tick `status` to `blocked`** với new gap ID in `blocker_gap`

---

## 8. Log

- **2026-05-14:** Matrix created (Wave 72a Bucket F). Supersedes `plan-1-self-test-e2e.md` (Wave 69 7-step). Format CSV (NOT markdown checkbox per user mandate). Every `input_data` pre-filled — zero data composition required. Wave 72a in-flight blocker gaps (GAP-514..525) referenced trong `blocker_gap` column; re-walk after Wave 72a closure.
