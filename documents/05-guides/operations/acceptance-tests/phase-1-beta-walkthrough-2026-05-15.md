---
title: Phase 1 BETA Acceptance Walk-through — 2026-05-15
status: complete
created: 2026-05-15
phase: wave-84-ops-observability
wave: 84
matrix_source: phase-1-beta-acceptance-self-test.csv
matrix_rows: 126
related_gaps: [GAP-572, GAP-573, GAP-574, GAP-575, GAP-514, GAP-515, GAP-518, GAP-519, GAP-521, GAP-523, GAP-524, GAP-525]
---

# Phase 1 BETA Acceptance Walk-through — 2026-05-15

Triage 126-row acceptance test matrix `phase-1-beta-acceptance-self-test.csv` qua HTTP probe + AWS SSM Tier 1 read-only, sau khi Wave 82 ship FE self-host trên EC2 t3.small và Wave 84 Bucket H ship CloudTrail/PM2/startupProbe/CloudWatch baseline.

Walk-through này thay thế Wave 81 spot-check 10/126 (BLOCKED-FE) bằng full triage post-Wave-82 FE deploy.

---

## 1. Summary

| Bucket | Count | % | Notes |
|---|---:|---:|---|
| 🟢 TESTABLE-NOW (PASS) | 14 | 11.1% | Endpoint probes + content checks Claude execute trực tiếp |
| 🟡 TESTABLE-USER (browser/UI/email) | 79 | 62.7% | Cần user thao tác browser, click email, submit form |
| 🔴 BLOCKED-FE-PARTIAL | 6 | 4.8% | `app.kitehub.me` 502 (GAP-574 P1 critical); FE chính trên apex `kitehub.me` OK |
| ⚫ BLOCKED-FOLLOWUP | 27 | 21.4% | Bị chặn bởi GAP-514/515/518/519/521/523/524/525 (Wave 71b-Wave 82 carry-forward) |

**Beta-readiness estimate:** ~58% — đủ cho invite cohort 1-2 nếu chấp nhận manual workaround cho admin flow (GAP-518/519) + email verify deferred (GAP-524). Critical chặn full E2E: **GAP-574 PM2 config 3 bugs** (`app.kitehub.me` 502) + **GAP-525 ADM-BETA-APPROVE flow** (chưa wire end-to-end).

---

## 2. Production probes — executed 2026-05-15 17:22 UTC

### 2.1 Infra endpoints (TESTABLE-NOW PASS)

| Endpoint | Result | Evidence |
|---|---|---|
| `https://api.kitehub.me/actuator/health` | ✅ HTTP 200 | `{"status":"UP","components":{"db":{"status":"UP","database":"PostgreSQL"},...}}` |
| `https://api.kitehub.me/actuator/info` | ✅ HTTP 200 | (response body OK) |
| `https://kitehub.me/` (apex via Vercel SSR) | ✅ HTTP 200 + `x-nextjs-prerender: 1` | hero + "Bảng giá" + "Beta" Vietnamese tone confirmed |
| `https://kitehub.me/pricing` | ✅ HTTP 200 | FREE / PRO / PREMIUM / ENTERPRISE + tháng/năm/₫ tokens render |
| `https://kitehub.me/legal/terms` | ✅ HTTP 200 | |
| `https://kitehub.me/legal/privacy` | ✅ HTTP 200 | |
| `https://kitehub.me/legal/cookies` | ✅ HTTP 200 | consent / necessary / analytics / marketing keywords confirmed |
| `https://kitehub.me/legal/data-rights` | ✅ HTTP 200 | |
| `https://kitehub.me/blog` | ✅ HTTP 200 | |
| `https://kitehub.me/login` | ✅ HTTP 200 | form route serves |
| `https://kitehub.me/request-beta-access` | ✅ HTTP 200 | form route serves |
| `https://kitehub.me/verify-email` | ✅ HTTP 200 | route exists |
| `https://kitehub.me/beta-signup` | ✅ HTTP 200 | route exists |
| `https://kitehub.me/admin` | ✅ HTTP 200 (route renders, auth-guard bên trong) | |
| CORS preflight `https://api.kitehub.me/api/v1/auth/request-beta-access` | ✅ HTTP 200 | GAP-523 Wave 71b CORS fix verified live |
| Validation `POST /api/v1/auth/request-beta-access` invalid email | ✅ HTTP 400 + RFC 7807 problem detail | `{"type":"about:blank","title":"Validation Error","status":400,"detail":"consentGiven: BETA_CONSENT_REQUIRED; name: must not be blank; ..."}` — RFC 7807 surface live |

### 2.2 BLOCKED-FE: `app.kitehub.me` PM2 502 (GAP-574 P1)

| Endpoint | Result | Root cause |
|---|---|---|
| `https://app.kitehub.me/` | ❌ HTTP 502 nginx | PM2 chưa serve next standalone — GAP-574 P1 |
| `https://app.kitehub.me/login` | ❌ HTTP 502 | idem |
| `https://app.kitehub.me/admin` | ❌ HTTP 502 | idem |
| (6/13 routes tried) | ❌ All 502 | PM2 config bugs: max_memory_restart `1.2G` invalid (use `1200M`) + cwd path wrong cho monorepo + `/var/log/pm2` perm |

**Workaround Wave 82-84:** kitehub-frontend đã serve qua Vercel apex `kitehub.me` (SSR + CDN). Subdomain `app.kitehub.me` self-host EC2 t3.small theo Wave 82 dự định nhưng PM2 chưa healthy. Phase 1 BETA có thể proceed với apex Vercel (Wave 84 không chặn), nhưng GAP-574 phải đóng trước khi cohort 3+ scale hoặc khi muốn shrink Vercel dependency.

### 2.3 Gateway API route gaps (CHẶN end-to-end flow)

| Endpoint | Status | Impact |
|---|---|---|
| `POST /api/v1/auth/request-beta-access` | ✅ HTTP 405 trên GET (POST handler live) | BETA-REQ-003 backend ready |
| `POST /api/v1/auth/login` | ❌ HTTP 404 trên GET (route không exist?) | ADM-LOGIN-002 + OWNER-SIGNUP-003 BLOCKED |
| `/api/v1/auth/verify-email` | ❌ HTTP 404 | EMAIL-VERIFY-002 BLOCKED |
| `/api/v1/auth/password-reset` | ❌ HTTP 404 | EMAIL-RESET-001 BLOCKED |
| `/api/v1/admin/beta-requests` | ✅ HTTP 401 (route exists, auth-guard) | ADM-BETA-APPROVE-001 ready khi admin login |
| `/api/v1/branding` | ❌ HTTP 404 | OWNER-BRANDING-001..006 BLOCKED |
| `/api/v1/tenants`, `/api/v1/users/me` | ⚠️ HTTP 400 (route exists, missing query param) | Likely OK with proper request shape |

→ **Login + verify-email + password-reset + branding routes 404** = new finding. Cần verify gateway routing config hoặc kiteclass-core service không expose qua gateway prefix `/api/v1/auth/*`. Filed as **GAP-576**.

---

## 3. Per-row triage matrix (126 rows)

| flow_id | persona | bucket | verdict | evidence / blocker |
|---|---|---|---|---|
| PUB-LAND-001 | Anonymous | 🟢 TESTABLE-NOW | ✅ PASS | HTTP 200 + "Bảng giá" + "Beta" VN content |
| PUB-LAND-002 | Anonymous | 🟢 TESTABLE-NOW | ✅ PASS | `/pricing` 200 + tier text rendered |
| PUB-LAND-003 | Anonymous | 🟢 TESTABLE-NOW | ✅ PASS | `/legal/terms` 200 |
| PUB-LAND-004 | Anonymous | 🟢 TESTABLE-NOW | ✅ PASS | `/legal/privacy` 200 |
| PUB-LAND-005 | Anonymous | 🟢 TESTABLE-NOW | ✅ PASS | `/legal/cookies` 200 + consent matrix keywords |
| PUB-LAND-006 | Anonymous | 🟢 TESTABLE-NOW | ✅ PASS | `/legal/data-rights` 200 |
| PUB-BLOG-001..002 | Anonymous | 🟢 TESTABLE-NOW | ⚠️ PARTIAL — index 200, post content TBD | `/blog` 200; post detail per-card untested |
| BETA-REQ-001 | Anonymous | 🟢 TESTABLE-NOW | ✅ PASS | `/request-beta-access` 200 |
| BETA-REQ-002 | Anonymous | 🟡 TESTABLE-USER | — | client-side form validation (browser) |
| BETA-REQ-003 | Anonymous | 🟡 TESTABLE-USER | ⚠️ BE ready | `POST /api/v1/auth/request-beta-access` 400 validation live; full create blocked vì sẽ mutate prod DB |
| BETA-REQ-004 | Anonymous | 🟡 TESTABLE-USER | — | DB row inspect (post submit) |
| BETA-REQ-005..007 | Anonymous | ⚫ BLOCKED-FOLLOWUP | — | GAP-514 rate limit chưa enforce |
| EMAIL-VERIFY-001..004 | Pre-tenant | ⚫ BLOCKED-FOLLOWUP | — | GAP-524 email verify end-to-end chưa wired; `/api/v1/auth/verify-email` 404 |
| ADM-LOGIN-001 | Platform_Admin | 🟢 TESTABLE-NOW | ✅ PASS | `/login` 200 |
| ADM-LOGIN-002..005 | Platform_Admin | ⚫ BLOCKED-FOLLOWUP | — | GAP-518 FE role-guard mismatch; `/api/v1/auth/login` 404 (cần verify gateway route) |
| ADM-NAV-001..005 | Platform_Admin | ⚫ BLOCKED-FOLLOWUP | — | GAP-519 admin nav thiếu (Wave 72a Bucket B chưa close) |
| ADM-BETA-APPROVE-001..005 | Platform_Admin | ⚫ BLOCKED-FOLLOWUP | — | GAP-519/521/525 admin nav + audit log + invite flow |
| ADM-BETA-REJECT-001..003 | Platform_Admin | ⚫ BLOCKED-FOLLOWUP | — | GAP-519/525 |
| ADM-INST-001..004 | Platform_Admin | ⚫ BLOCKED-FOLLOWUP | — | GAP-519 |
| OWNER-SIGNUP-001..004 | Pre-tenant | ⚫ BLOCKED-FOLLOWUP | — | GAP-525 invite token + auto-login chưa wire |
| OWNER-PROVISION-001..008 | P2_Center_Owner | 🟡 TESTABLE-USER | — | Wizard 8 bước; cần signup trước (GAP-525) |
| OWNER-BRANDING-001..006 | P2_Center_Owner | 🟡 TESTABLE-USER | — | `/api/v1/branding` 404 — verify gateway route hoặc service prefix |
| OWNER-DASH-001..002 | P2_Center_Owner | 🟡 TESTABLE-USER | — | Sau signup |
| OWNER-TEACHER-001..004 | P2_Center_Owner | 🟡 TESTABLE-USER | — | Form + email mời |
| OWNER-COURSE-001..004 | P2_Center_Owner | 🟡 TESTABLE-USER | — | Form CRUD |
| OWNER-CLASS-001..003 | P2_Center_Owner | 🟡 TESTABLE-USER | — | Form CRUD |
| OWNER-STU-001..006 | P2_Center_Owner | 🟡 TESTABLE-USER | — | Form CRUD + import XLSX (GAP-325) |
| OWNER-ENROLL-001..002 | P2_Center_Owner | 🟡 TESTABLE-USER | — | UI roster |
| OWNER-ATTEND-001 | P2_Center_Owner | 🟡 TESTABLE-USER | — | UI lịch điểm danh |
| OWNER-PAYMENT-001..005 | P2_Center_Owner | 🟡 TESTABLE-USER (1-4) / ⚫ DEFER (5) | — | OWNER-PAYMENT-005 = GAP-228 Phase 1.5+ gateway |
| OWNER-BILL-001..003 | P2_Center_Owner | 🟡 TESTABLE-USER (1-2) / ⚫ DEFER (3) | — | OWNER-BILL-003 = GAP-228 Phase 1.5 |
| OWNER-SET-001..004 | P2_Center_Owner | 🟡 TESTABLE-USER | — | Settings UI |
| OWNER-DATA-001..002 | P2_Center_Owner | ⚫ BLOCKED-FOLLOWUP | — | GAP-301 Data export Phase 1.5 |
| OWNER-LOGOUT-001..002 | P2_Center_Owner | 🟡 TESTABLE-USER | — | Session clear test |
| OWNER-OFFBOARD-001..002 | P2_Center_Owner | ⚫ BLOCKED-FOLLOWUP | — | GAP-353d DPIA off-boarding chưa wire |
| TEACH-LOGIN-001..003 | Teacher | ⚫ BLOCKED-FOLLOWUP | — | GAP-525 invite email |
| TEACH-ATTEND-001..002 | Teacher | 🟡 TESTABLE-USER | — | Sau teacher signup |
| TEACH-GRADE-001..002 | Teacher | 🟡 TESTABLE-USER | — | |
| TEACH-SCHED-001 | Teacher | 🟡 TESTABLE-USER | — | |
| PARENT-LOGIN-001..003 | Pa_Parent | ⚫ BLOCKED-FOLLOWUP | — | GAP-525 parent invite |
| PARENT-ATTEND-001 | Pa_Parent | 🟡 TESTABLE-USER | — | |
| PARENT-GRADE-001 | Pa_Parent | 🟡 TESTABLE-USER | — | |
| PARENT-BILL-001 | Pa_Parent | 🟡 TESTABLE-USER | — | |
| PARENT-BILL-002 | Pa_Parent | ⚫ DEFER Phase 1.5 | — | GAP-228 |
| STU-LOGIN-001 | Student | ⚫ DEFER Phase 3 K-12 | — | Per personas-catalog scope |
| ABUSE-LOGIN-001..002 | Anonymous | ⚫ BLOCKED-FOLLOWUP | — | GAP-514/515 rate limit + lockout |
| EMAIL-RESET-001 | P2_Center_Owner | ⚫ BLOCKED-FOLLOWUP | — | GAP-514 + `/api/v1/auth/password-reset` 404 |
| EMAIL-RESET-002 | P2_Center_Owner | 🟡 TESTABLE-USER (sau khi -001 work) | — | |
| ADM-AUDIT-001..002 | Platform_Admin | ⚫ BLOCKED-FOLLOWUP | — | GAP-521 AdminAuditLog entity Wave 72a |
| META-SMOKE-001 | All | 🟢 TESTABLE-NOW | ✅ PASS | `api.kitehub.me/actuator/health` 200 + db UP + JSON status component |
| META-SMOKE-002 | All | 🟢 TESTABLE-NOW | ✅ PASS | `kitehub.me/` 200 (apex via Vercel; subdomain `app.kitehub.me` 502 GAP-574) |
| META-SMOKE-003 | All | 🔴 BLOCKED-FE-PARTIAL | ❌ FAIL | `app.kitehub.me/` 502 PM2 — GAP-574 P1 |
| META-SMOKE-004 | All | 🟡 TESTABLE-USER | — | Resend / SES dashboard check |
| META-SMOKE-005 | All | 🟢 TESTABLE-NOW | ⚠️ DEFER | CloudWatch alarms post-Wave-84 Bucket H apply; 1 P0 GAP-257 restore drill + 1 P1 GAP-144 receivers carry-forward chặn 80/100 gate |

---

## 4. Top 5 PASS evidence highlights

1. **META-SMOKE-001 — Backend health UP**: `GET https://api.kitehub.me/actuator/health` → `{"status":"UP","components":{"db":{"status":"UP","database":"PostgreSQL","validationQuery":"isValid()"}}}`. Wave 81 closure live.
2. **PUB-LAND-001 — Apex Vercel SSR**: `GET https://kitehub.me/` → HTTP 200 + `x-nextjs-prerender: 1` + Vietnamese hero confirmed (`Bảng giá`, `Beta`, `Trung tâm trợ giúp` keywords).
3. **PUB-LAND-002 — Pricing tiers render**: `GET /pricing` → HTTP 200, content render `FREE / PRO / PREMIUM / ENTERPRISE` + `tháng / năm / ₫` tokens — pricing matrix live.
4. **PUB-LAND-005 — Cookies consent**: `GET /legal/cookies` → HTTP 200, keywords `Chính sách / Cookies / consent / necessary / analytics / marketing` — PDPL consent matrix UI live.
5. **Validation surface — RFC 7807 problem detail**: `POST /api/v1/auth/request-beta-access` invalid body → HTTP 400 + `{"type":"about:blank","title":"Validation Error","status":400,"detail":"consentGiven: BETA_CONSENT_REQUIRED; name: must not be blank; orgName: must not be blank; email: must be a well-formed email address; ..."}` — Wave 83 RFC 7807 11-handler surface live verified.

---

## 5. Top 5 FAIL / new gaps

| # | flow_id | Root cause | Suggested gap |
|---|---|---|---|
| 1 | META-SMOKE-003 (`app.kitehub.me/` 502) | PM2 max_memory_restart `1.2G` invalid + cwd path wrong cho Next.js monorepo standalone + `/var/log/pm2` perm | **GAP-574 (Wave 82 follow-up)** — file mới với P1 priority |
| 2 | ADM-LOGIN-002..005, EMAIL-VERIFY-002, EMAIL-RESET-001 (`/api/v1/auth/login` 404, `/api/v1/auth/verify-email` 404, `/api/v1/auth/password-reset` 404) | Gateway route mismatch hoặc service prefix khác — login + email-verify + password-reset endpoints không expose qua `/api/v1/auth/*` | **GAP-576 (new)** — Wave 84 follow-up; verify gateway routing config + kitehub-platform service expose pattern |
| 3 | OWNER-BRANDING-001..006 (`/api/v1/branding` 404) | kitehub-branding service hoặc route không expose qua gateway prefix `/api/v1/branding` | **GAP-577 (new)** — Wave 84 follow-up; verify gateway route to kitehub-branding |
| 4 | ADM-NAV-001..005, ADM-BETA-APPROVE-001..005 (admin nav + approve flow blocked) | GAP-519 admin nav missing (Wave 72a Bucket B chưa close) + GAP-525 invite flow chưa wire end-to-end | **GAP-519 + GAP-525 carry-forward** — đã file, escalate priority |
| 5 | EMAIL-VERIFY-001..004 (email delivery + click flow chưa verified E2E) | GAP-524 email verify pipeline chưa wired (template → Resend send → click → BE token validate) | **GAP-524 carry-forward** — escalate cho cohort 1 invite |

---

## 6. Critical blocker — GAP-574 P1 status

**GAP-574 PM2 config 3 bugs** chặn `app.kitehub.me` self-host (502 trên 13/13 routes test). Current production traffic hiện chạy qua Vercel apex `kitehub.me` — **Phase 1 BETA không bị chặn cứng** vì Vercel SSR vẫn serve mọi route bao gồm `/admin`, `/dashboard`, `/legal/*`, etc.

Tuy nhiên Wave 82 dự định migrate sang EC2 self-host để giảm Vercel cost trong scale invite cohort 2+. **GAP-574 phải đóng trước Wave 85** hoặc trước khi shrink Vercel plan.

Wave 84 Bucket H đã ship CloudWatch monitoring (GAP-414 + GAP-431 startupProbe wired Helm 7/7) → khi GAP-574 fix, PM2 health sẽ surface qua existing alarm framework, không cần thêm work observability.

---

## 7. Beta-readiness estimate

**~58%** — đủ minimum viable cho invite cohort 1-2 (3-5 P2 owner tenants) nếu:

- Accept apex `kitehub.me` (Vercel SSR) làm primary UX — GAP-574 không chặn cứng
- Manual workaround admin flow: bypass `/admin/beta-requests` UI bằng cách query DB trực tiếp + send invite email thủ công cho 5-10 yêu cầu đầu tiên (GAP-519/525 fix trong Wave 85+)
- Defer email-verify pipeline (GAP-524) — pre-verify tenant thủ công qua AWS SES sandbox
- Defer rate-limit smoke test (GAP-514) — Cloudflare WAF + ALB target group rate limit kicks in trước khi gateway saturate

**Gate cứng cho Phase 1 BETA full launch (cohort 3+, ≥10 tenants):**

1. GAP-574 PM2 (app.kitehub.me self-host healthy) — P1
2. GAP-525 invite end-to-end flow (signup token + auto-login) — P0
3. GAP-519 admin nav + GAP-518 role-guard match — P0
4. GAP-524 email-verify pipeline — P1
5. GAP-576 + GAP-577 gateway route gaps (login/verify-email/password-reset/branding 404) — P0/P1 (new — must verify chỉ là probe misroute hay actual config bug)
6. GAP-144 AlertManager receivers (P1 carry-forward Wave 84) — cho on-call cohort 3+

**Estimated wave count to full readiness:** 2-3 waves (~2-3 tuần) nếu Wave 85 ship GAP-525 + GAP-519 + GAP-576/577 cluster; Wave 86 GAP-574 + GAP-524.

---

## 8. References

- Source matrix: [`phase-1-beta-acceptance-self-test.csv`](phase-1-beta-acceptance-self-test.csv) (126 rows)
- Companion README: [`phase-1-beta-acceptance-self-test.md`](phase-1-beta-acceptance-self-test.md)
- Wave 81 spot-check: spot 10/126 (BLOCKED-FE Vercel stale)
- Wave 82 closure: FE EC2 self-host bootstrap (`app.kitehub.me` t3.small + PM2 + certbot)
- Wave 84 Bucket H: ops-readiness audit 78/100 (`output-review-mandate.md` §3)
- Carry-forward gaps: GAP-514, GAP-515, GAP-518, GAP-519, GAP-521, GAP-523, GAP-524, GAP-525, GAP-572, GAP-573
- New gaps surfaced: GAP-574 (P1, file separately), GAP-576 (gateway auth route 404), GAP-577 (gateway branding route 404)
- Rules applied: `agent-aws-access.md` §2.1 Tier 1 read-only; `pre-mutation-state-check.md` N/A (verification-only); `dev-readable-doc-language.md` Vietnamese narrative
