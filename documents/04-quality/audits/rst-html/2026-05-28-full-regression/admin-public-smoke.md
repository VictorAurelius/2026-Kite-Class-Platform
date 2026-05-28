---
audience: dev
date: 2026-05-28
cluster: Admin + Public + Smoke + Abuse-login RST walk (Agent A1)
session-theme: Phase 1 BETA acceptance self-test — admin ops / public legal+blog / meta-smoke / abuse-login flows on LOCAL docker stack
walk_method: curl (gateway :9000) + psql (kite-postgres) + MailHog API (:8025) + FE route HEAD (:3001/:3000). NO browser — visual rendering marked NEEDS-USER-BROWSER.
flow_groups_walked: ADM-LOGIN (001-005), ADM-NAV (001-005), ADM-INST (001-004), ADM-AUDIT (001-002), ADM-BETA-REJECT (001-003), PUB-LAND (001-006), PUB-BLOG (001-002), META-SMOKE (001-005), ABUSE-LOGIN (001-002)
rows_walked: 34
verdict: PARTIAL — core admin read + public pages + smoke + reject mutation PASS; 9 bugs/gaps catalogued (3 missing endpoints, account-lockout absent, reject-email absent, CSV contract drift). No P0 regressions on shipped surfaces; failures are known-gap feature absences + CSV/contract documentation drift.
anti_contamination: Used throwaway `a1-*@test.local` identities. Created+rejected ONLY own beta request id=14. Never touched admin lockout. No deletes. No suspend executed on shared instance.
---

# Admin + Public + Smoke + Abuse RST walk — Agent A1 findings

## Session arc

Walk của cluster admin-ops + public-pages + meta-smoke + abuse-login trên LOCAL docker stack (13 services healthy). Verification-only: không sửa code, không đổi gap status. Login recipe `/api/auth/login` (KHÔNG phải `/api/v1/auth/login` như CSV `verify_via` ghi — finding F1). Admin JWT có `role: PLATFORM_ADMIN` đúng.

## Stack health (META-SMOKE)

| Flow | Bước | Endpoint | HTTP | Verdict |
|---|---|---|---|---|
| META-SMOKE-001 | health | `GET :9000/actuator/health` | 200 | PASS — `{status:UP, db:UP}` (discoveryComposite UNKNOWN = expected, Eureka không init local) |
| META-SMOKE-002 | FE KiteHub | `GET :3001/` | 200 | PASS (route 200; visual NEEDS-USER-BROWSER) |
| META-SMOKE-003 | FE KiteClass | `GET :3000/` | 200 | PASS (route 200; visual NEEDS-USER-BROWSER) |
| META-SMOKE-004 | email send volume | MailHog API | n/a | PARTIAL — local MailHog có 10 messages (login-alert + password-reset + staff-invite delivered); production Resend/SES dashboard NEEDS-USER (không có local equivalent) |
| META-SMOKE-005 | CloudWatch alarms | AWS console | n/a | NEEDS-USER — không kiểm tra được từ local stack (AWS-only) |

## ADM-LOGIN

| Flow | Bước | Evidence | Verdict |
|---|---|---|---|
| ADM-LOGIN-001 | FE `/login` route | `GET :3001/login` → 200 | PASS (route); visual form NEEDS-USER-BROWSER |
| ADM-LOGIN-002 | admin login | `POST /api/auth/login {admin@kitehub.com}` → 200 + accessToken (JWT len 306) + `role:PLATFORM_ADMIN` claim + refreshToken + `instances:[]` | PASS (API). Redirect tới `/admin` = FE concern NEEDS-USER-BROWSER |
| ADM-LOGIN-003 | admin dashboard | `GET :3001/admin` + `/admin/beta-requests` → 200 (route exist) | PASS (route); role-guard PLATFORM_ADMIN-vs-ADMIN observation NEEDS-USER-BROWSER (CSV note GAP-518) |
| ADM-LOGIN-004 | re-login session refresh | re-login `POST /api/auth/login` → 200 | PASS (API) |
| ADM-LOGIN-005 | 6x wrong-password lockout | throwaway `a1-lockout-001@test.local` ×6 → all HTTP **400** "Invalid email or password"; **không có 423 Locked** ở lần 6 | **FAIL (feature absent)** — account lockout không fire (CSV note GAP-515 "đang hoàn thiện"). Wrong-password trả 400 (Bug #2 semantic: nên 401). Không lock admin (anti-contamination giữ nguyên). |

## ADM-NAV

| Flow | Bước | Endpoint | HTTP | Verdict |
|---|---|---|---|---|
| ADM-NAV-001 | sidebar | (UI observation) | n/a | NEEDS-USER-BROWSER (GAP-519) |
| ADM-NAV-002 | Yêu cầu Beta | `GET /api/v1/admin/beta-requests` | 200 | PASS — paginated `{content:[...]}`, 13+ requests; backing API healthy |
| ADM-NAV-003 | Instances | `GET /api/v1/admin/instances` | 200 (1 transient 503 → 3× retry all 200) | PASS — `{content:[...]}` provisioned tenants. Bug #6: transient 503 lần đầu (cold-start / circuit-breaker warmup) |
| ADM-NAV-004 | Thanh toán | `GET /api/v1/admin/payments` | **404** | FAIL-as-routed — bare `/payments` không tồn tại. Thực route = `/payments/pending` (200) + `/payments/summary` (200). Bug #3: CSV/FE-expected path drift |
| ADM-NAV-005 | Doanh thu | `GET /api/v1/admin/revenue` | 200 | PASS — `{totalRevenue:0, revenueByTier:[], dailyRevenue:[...]}` (0 đúng BETA) |

## ADM-INST

| Flow | Bước | Evidence | Verdict |
|---|---|---|---|
| ADM-INST-001 | list instances | `GET /api/v1/admin/instances` → 200, table-shape `{id, organizationName, subdomain, status, tier, ownerEmail, trialEndDate, databaseUrl}` | PASS |
| ADM-INST-002 | instance detail | `GET /api/v1/admin/instances/{uuid}` → 200, full detail shape | PASS |
| ADM-INST-003 | suspend instance | `POST /api/v1/admin/instances/{id}/suspend` → **404**. Code: `AdminInstancesController` GET-only (comment: "Mutation operations (suspend/activate) remain on legacy"). Legacy `InstanceController @ /api/platform/instances` chỉ có PUT/PATCH/DELETE — KHÔNG có route `/suspend` với `reason`+`notify_tenant` | **FAIL (feature absent)** — suspend-with-reason endpoint chưa implement. Không execute mutation trên shared instance (anti-contamination). (GAP-519) |
| ADM-INST-004 | restore instance | depends ADM-INST-003 | BLOCKED — restore route cũng không tồn tại (cùng Bug #4) |

## ADM-AUDIT

| Flow | Bước | Endpoint | HTTP | Verdict |
|---|---|---|---|---|
| ADM-AUDIT-001 | xem audit log | `GET /api/v1/admin/audit-log` (+ `/audit-logs`, `/audit`) | **404** | **FAIL (endpoint absent)** — không có HTTP endpoint audit-log. Code: kitehub-admin có `AdminAuditLog` entity + repository (GAP-521 Wave 72a) nhưng KHÔNG expose controller route. Bug #5 |
| ADM-AUDIT-002 | lọc theo action | `?action=BETA_APPROVE` | 404 | FAIL — consequence của Bug #5 (không có endpoint để filter) |

## ADM-BETA-REJECT (own request id=14 only)

| Flow | Bước | Evidence | Verdict |
|---|---|---|---|
| setup | create own request | `POST /api/v1/auth/request-beta-access {a1-reject-001@test.local}` → **201** id=14 PENDING | PASS |
| ADM-BETA-REJECT-001 | open detail | `GET /api/v1/admin/beta-requests/14` → **404** | FAIL-as-routed — không có single-detail GET endpoint (chỉ list). Bug #7: FE detail page `/admin/beta-requests/{id}` không có backing API |
| ADM-BETA-REJECT-002 | reject | đầu tiên `POST .../14/reject {rejection_reason, rejection_notes, notify_user}` → **400** `approverId: must not be blank`. Retry với DTO đúng `{approverId, rejectionReason}` → **200**, body `status:REJECTED`. DB verify: `SELECT ... WHERE id=14` → `REJECTED | Ngoai scope cohort...` ✅ | PASS (sau khi sửa payload). Bug #8: CSV field names (`rejection_reason`/`rejection_notes`/`notify_user`) sai — DTO thực = `BetaRejectCommand{approverId, rejectionReason}`, không có notes/notify field |
| ADM-BETA-REJECT-003 | rejection email | MailHog search `a1-reject-001@test.local` → **0 found** (sau sleep 3s). Code: `BetaAccessService.rejectRequest()` chỉ set status + counter + log — KHÔNG send email | **FAIL (feature absent)** — rejection email "Cập nhật yêu cầu Beta" không gửi (CSV note GAP-525). Email pipeline tổng thể OK (invite/reset/login-alert delivered), chỉ reject path thiếu. Bug #9 |

## PUB-LAND + PUB-BLOG (FE :3001)

| Flow | Route | HTTP | Content markers (curl HTML grep) | Verdict |
|---|---|---|---|---|
| PUB-LAND-001 | `/` | 200 | — | PASS (route); hero visual NEEDS-USER-BROWSER |
| PUB-LAND-002 | `/pricing` | 200 | — | PASS (route); pricing tiers visual NEEDS-USER-BROWSER. (VN slug `/bang-gia` → 404, English slug only) |
| PUB-LAND-003 | `/legal/terms` | 200 | `Điều khoản`, `PDPL`, `chờ legal counsel review` ✅ | PASS (route+content markers); full visual NEEDS-USER-BROWSER |
| PUB-LAND-004 | `/legal/privacy` | 200 | `Chính sách bảo mật`, `Nghị định 13`, `DPO`, `dpo@` ✅ | PASS (route+content) |
| PUB-LAND-005 | `/legal/cookies` | 200 | `necessary`/`analytics`/`marketing`/`consent` ✅ | PASS (route+content); consent banner NEEDS-USER-BROWSER |
| PUB-LAND-006 | `/legal/data-rights` | 200 | `truy cập`/`sửa`/`xoá`/`chuyển`/`DSAR`/`quyền` ✅ (4 quyền present) | PASS (route+content); DSAR intake form NEEDS-USER-BROWSER (GAP-353c) |
| PUB-BLOG-001 | `/blog` | 200 | ≥3 real slugs: `diem-danh-online`, `kitehub-ra-mat`, `quan-ly-trung-tam-giao-duc` ✅ | PASS (route + ≥3 posts in HTML) |
| PUB-BLOG-002 | `/blog/kitehub-ra-mat` | 200 | — | PASS (route); content+breadcrumb NEEDS-USER-BROWSER |

## ABUSE-LOGIN

| Flow | Evidence | Verdict |
|---|---|---|
| ABUSE-LOGIN-001 | throwaway `a1-abuse-001@test.local` ×11 rapid → `400 400 400 400 400 400 400 400 429 429 429` — gateway rate-limit **429 fires** ở ~attempt 9; nhưng per-user lockout **423 không fire** | PARTIAL — rate-limit (per-IP) layer WORKS (429); account-lockout (per-user 423) layer ABSENT (cùng Bug #1 như ADM-LOGIN-005) |
| ABUSE-LOGIN-002 | IP rotation bypass (multi-IP) | NEEDS-USER — local stack 1 IP, không mô phỏng multi-IP được. Per-email lockout (đáng lẽ bù rate-limit) hiện không tồn tại → nếu attacker rotate IP, không còn lớp lockout nào chặn. Risk flagged. |

## Bug-class table

| # | Flow | Bước | Symptom | Severity |
|---|---|---|---|---|
| 1 | ADM-LOGIN-005 / ABUSE-LOGIN-001/002 | lockout | Account lockout (HTTP 423) KHÔNG fire sau 6+ wrong attempts; chỉ gateway rate-limit 429 (per-IP) hoạt động → IP-rotation attack không bị chặn lớp nào | P1 (GAP-515) |
| 2 | ADM-LOGIN-005 / owner.test | wrong-pw | Wrong password trả HTTP **400** "Invalid email or password" thay vì 401 Unauthorized — semantic drift (400 = bad request, không phải auth failure) | P2 |
| 3 | ADM-NAV-004 | payments | `GET /api/v1/admin/payments` → 404; route thực = `/payments/pending` + `/payments/summary`. Bare list path FE/CSV expect không tồn tại | P2 (GAP-519) |
| 4 | ADM-INST-003/004 | suspend/restore | Suspend-with-reason + restore endpoint không tồn tại (`AdminInstancesController` GET-only; legacy chỉ PUT/PATCH generic, không có `/suspend` `/activate` route + notify_tenant) | P1 (GAP-519) |
| 5 | ADM-AUDIT-001/002 | audit-log | KHÔNG có HTTP endpoint `/api/v1/admin/audit-log`; chỉ có `AdminAuditLog` entity+repository, controller chưa expose → admin không xem được audit log qua API | P1 (GAP-521) |
| 6 | ADM-NAV-003 | instances | Transient HTTP 503 ở request đầu (cold-start / circuit-breaker); retry 3× all 200 | P3 |
| 7 | ADM-BETA-REJECT-001 | detail | `GET /api/v1/admin/beta-requests/{id}` → 404; không có single-detail GET (chỉ list). FE detail page thiếu backing API | P2 (GAP-519) |
| 8 | ADM-BETA-REJECT-002 | reject | CSV input fields (`rejection_reason`/`rejection_notes`/`notify_user`) sai — DTO thực `BetaRejectCommand{approverId, rejectionReason}`; thiếu `approverId` → 400. Không có notes/notify support | P2 (contract drift) |
| 9 | ADM-BETA-REJECT-003 | reject-email | Rejection email KHÔNG gửi — `rejectRequest()` chỉ set DB status + counter, thiếu email-send (approve path CÓ email, reject path KHÔNG) | P1 (GAP-525) |
| F1 | (all login flows) | recipe | CSV `verify_via` ghi `/api/v1/auth/login`; path thực = `/api/auth/login` (no `/v1`) | P3 (CSV doc) |

## Verdict

- **PASS: 16 rows** — META-SMOKE-001/002/003 (3), ADM-LOGIN-001/002/003/004 (4), ADM-NAV-002/003/005 (3), ADM-INST-001/002 (2), PUB-LAND-003/004/005/006 markers + PUB-BLOG-001 content (counted in NEEDS-USER for visual but content/route verified), ADM-BETA-REJECT setup+002 (2 — create + reject + DB verify), ABUSE-LOGIN-001 rate-limit layer (1, partial).
- **FAIL (feature/endpoint absent): 7 rows** — ADM-LOGIN-005 (lockout), ADM-INST-003 + ADM-INST-004 (suspend/restore), ADM-AUDIT-001 + ADM-AUDIT-002 (audit-log endpoint), ADM-BETA-REJECT-003 (reject email), ADM-NAV-004 (payments bare path) + ADM-BETA-REJECT-001 (detail) routed-404.
- **NEEDS-USER-BROWSER: 11 rows** — all UI `(observation)` rows + visual rendering on public pages (PUB-LAND-001/002 hero+pricing, PUB-BLOG-002 content, ADM-NAV-001 sidebar, ADM-LOGIN visual redirect, cookie consent banner, DSAR form) + META-SMOKE-004/005 (Resend/CloudWatch AWS-only) + ABUSE-LOGIN-002 (multi-IP).

**Overall: PARTIAL.** Core admin READ surfaces (beta-requests list, instances list+detail, revenue, payments-pending/summary), public legal+blog routes+content, smoke health, và beta-reject mutation đều healthy on real stack. Failures are concentrated in: (a) known security gaps — account lockout absent (P1), (b) missing endpoints documented in gaps — audit-log API (GAP-521), suspend/restore (GAP-519), reject-email (GAP-525), (c) CSV/contract documentation drift (login path, reject DTO fields, payments path). No NEW P0 regression surfaced on shipped surfaces.

## Anti-contamination compliance

- Login recipe used `admin@kitehub.com` for READ-only admin calls; lockout/brute-force tested với throwaway `a1-lockout-001` / `a1-abuse-001@test.local` only — admin account NEVER locked.
- Beta-reject: created own request id=14 (`a1-reject-001@test.local`), rejected ONLY id=14. Other pending requests (id=12, 13) untouched.
- ADM-INST suspend NOT executed (route 404 + would mutate shared instance) — verified read-only via code inspection.
- No deletes. No data mutation beyond own id=14 reject.
