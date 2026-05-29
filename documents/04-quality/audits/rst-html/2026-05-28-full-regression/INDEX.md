---
audience: dev
date: 2026-05-28
session-theme: Full 126-row regression RST walk — index + consolidated findings + Wave A flip verdict
walk_method: 3 Opus agents (parallel, local stack) + Claude direct Wave A verification + user browser walk (5 luồng)
stack: local 13 service healthy, 4 service rebuilt 2026-05-28 từ main HEAD
verdict: Wave A CHƯA ready full DONE — 1 P0 thật (GAP-797 email var-drift, flow signup chết); "P0 tenant isolation" là MISDIAGNOSIS (§2.8 investigation → isolation hoạt động, re-scope GAP-795 P1); GAP-794 DONE-ready; GAP-790/791/792 verified working/unblocked
---

# Full regression RST walk — INDEX (2026-05-28)

> **Bắt đầu từ đây.** Đây là entry point của RST walk 2026-05-28. Đọc §⚠️ CORRECTED (tenant isolation KHÔNG vỡ — đó là misdiagnosis) + §Wave A flip verdict trước khi action. Mỗi cluster doc (`admin-public-smoke.md` / `owner-ops.md` / `teacher-parent-student.md`) là **lớp evidence** (HTTP code + DB row quan sát được). Bug thật để dev fix nằm trong **GAP files** (GAP-795/796/797/787) — walk docs ghi nhận hiện tượng, GAP files chứa Proposed Fix + AC actionable.

126-row acceptance catalog (`phase-1-beta-acceptance-self-test.csv`) walked. 5 luồng Wave A do user walk browser; ~100 rows còn lại do 3 Opus agents (HTTP/DB/MailHog layer; UI-render flag NEEDS-USER).

## Cluster findings docs

| Cluster | File | Rows | PASS / FAIL / NEEDS-USER / NEEDS-DATA |
|---|---|---|---|
| Admin + public + smoke | `admin-public-smoke.md` | 34 | 16 / 7 / 11 / 0 |
| Owner daily ops | `owner-ops.md` | 36 | 9 / 6 / 17 / 4 (+1 NEEDS-ISOLATED-TENANT) |
| Teacher/Parent/Student | `teacher-parent-student.md` | 23 | 0 / 0 / 1 / 18 (+2 DEFERRED) |
| Wave A 5 luồng (user + Claude) | `../2026-05-28-wave-a-5-flow-walk.md` | 5 flows | GAP-794 ✅; 1-4 in progress |

## ⚠️ CORRECTED — gateway tenant isolation KHÔNG vỡ (agent misdiagnosis, fixed by §2.8 investigation)

**Original RST agent claim (owner-ops + T/P/S):** "gateway tenant resolution vỡ → writes land `kiteclass_shared` not tenant DB → P0". **MISDIAGNOSIS.**

**Claude fix-time investigation 2026-05-28 (empirical, per `audit-to-gap-pipeline.md` §2.8):**
- Gateway logs: `Resolved tenant from JWT claim: 877dff9d` + `Routing to instance: sky-edu-test` → X-Tenant-Id set ĐÚNG
- core logs: `Tenant filter enabled for tenant: 877dff9d` → TenantContext + Hibernate filter active
- DB: teacher id=1 `instance_id=877dff9d` ✅ tagged; GET via gateway returns it (HTTP 200)
- **Kiến trúc = shared DB (`kiteclass_shared`) + Hibernate filter + RLS GUC**, KHÔNG per-tenant DB. `kiteclass_877dff9d` empty = legacy. MDC `tenant=-` = logging artifact (red herring agent misread).

→ **Tenant isolation WORKS. No P0 data-isolation bug.** GAP-795 re-scoped (P0→P1) to the REAL bug found same investigation: **`X-User-Id` UUID vs `Long.parseLong` → UserContext null → `created_by` NULL** (P1 auditing, Wave meta-6 #13 recurrence).

**Tác động flip (corrected):**
- **GAP-791/792** UNBLOCKED (tenant filter works; GAP-792 cache-key cross-tenant still needs own verify but not blocked by isolation).
- Owner CRUD walk findings cần re-interpret (agent "empty list" do filter-by-correct-tenant, không phải broken).
- Cluster T/P/S 18 NEEDS-DATA = data chưa tạo (Owner chưa walk tạo teacher/student), không phải routing broken.

## 🟠 P1 cross-cutting — exception masking (NEW)

kiteclass-core `GlobalExceptionHandler` nuốt `NoHandlerFoundException` (404) + `HttpRequestMethodNotSupportedException` (405) → trả `500 SYSTEM_INTERNAL_ERROR`. Che route-mismatch dưới vỏ "internal error" ở /classes, /teacher/dashboard, /parent, /attendance, /grades, /invoices, /payments. (cả 2 agent B+C độc lập confirm.)

→ **Fix: xem GAP-796** (P1, 404/405→500 mask) — Proposed Fix + AC actionable nằm ở gap file, không phải walk doc.

## 🟠 P1 — email-send path gaps

- Teacher invite KHÔNG gửi email (recurrence Wave meta-6 #14). Nhưng EMAIL-RESET ✅ (202 + MailHog "Đặt lại mật khẩu" delivered) → consumer #1938 hoạt động cho 1 số path, miss path khác. → **Fix: xem GAP-787** (staff/teacher-invite publisher) + **GAP-797** (email var-drift beta-invite, P0).
- GAP-525 (beta-reject email không gửi) confirmed còn open. → **Fix: xem GAP-525**.

## Known gaps confirmed còn open (không phải mới)

| Gap | Symptom | Agent |
|---|---|---|
| GAP-515 | account lockout (423) không fire; chỉ rate-limit 429 per-IP | admin |
| GAP-521 | `/api/v1/admin/audit-log` → 404 (entity+repo có, thiếu controller) | admin |
| GAP-525 | beta-reject email không gửi | admin |

## CSV contract drift (P2/P3 — doc fix, recurrence "route versioning")

- login: `/api/auth/login` (KHÔNG `/api/v1/auth/login`)
- email-verify: `/api/auth/verify-email` (KHÔNG `/v1`)
- reject DTO: `approverId`+`rejectionReason` (KHÔNG field CSV)
- payments: `/payments/pending` (KHÔNG bare `/payments`)
- teacher/student DTO: `name` (KHÔNG `full_name`); `phone` bị drop → `phoneNumber:null`
- payment DTO: `invoiceId` (KHÔNG student_id/class_id)
- DELETE instance không enforce password/confirm-phrase

## Wave A flip verdict (per feature-ship-runtime-walk-mandate.md + gap-done-discipline.md §2)

| Gap | Verdict | Lý do |
|---|---|---|
| **GAP-794** PDPL consent | ✅ **DONE-ready** | Claude verified end-to-end: POST 201 + DB row + GET 200, không 401. |
| **GAP-790** gateway TenantResolver | ✅ **verified working** | Gateway resolve JWT tenant + route to instance confirmed (logs). Tenant isolation works. |
| **GAP-791/792** course tenant scope + cache | ✅ UNBLOCKED | Tenant filter works (no isolation bug). GAP-791 list likely OK; GAP-792 Redis cache-key cross-tenant still needs own verify. |
| **GAP-787/793** email consumer | ⚠️ PARTIAL | EMAIL-RESET delivered ✅; teacher/staff/beta-invite paths miss (GAP-787 publisher + GAP-797 var-drift). |
| **GAP-795** (re-scoped) X-User-Id | 🟠 P1 NEW | created_by NULL (UUID vs Long). NOT P0 isolation (misdiagnosis corrected). |
| **GAP-796** 404/405→500 mask | 🟠 P1 | GlobalExceptionHandler. |
| **GAP-797** email var-drift | 🔴 **P0** | beta-invite signup info missing (flow 1 die). Confirmed real. |

**Kết luận:** Wave A CHƯA ready full DONE. **Chỉ 1 P0 thật = GAP-797** (email signup). GAP-795 P0 was misdiagnosis → §2.8 fix-time investigation caught it (saved fix effort on non-bug). Walk + investigation net: 1 true P0 (GAP-797) + 3 P1 (GAP-795/796 + GAP-787) — đúng `feature-ship-runtime-walk-mandate.md` value.
