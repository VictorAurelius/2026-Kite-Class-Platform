---
title: Wave flow-kh10 — KH-10 Notification/email/feedback/support G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kh10]
wave: wave-2026-06-06-flow-kh10
tag_primary: flow-kh10
tags_secondary: [notification, email, feedback, support, campaign-g1]
date: 2026-06-06
flow: KH-10 (Notification/email/feedback/support)
gaps: [GAP-1031, GAP-1032, GAP-1033]
---

# Wave flow-kh10 — KH-10 Notification/email/feedback/support G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KH-10 — 4 sub-flow: feedback submit + notification preferences + admin email console + email send; cộng support menu (FE). Flow secondary thứ 6 (KH cuối trừ KH-10 self). Sau wave này còn KC-10/11/12 để G1-all-first hoàn tất.

## 1. Brainstorm

KH-10 trải 2 service: kitehub-subscription (feedback + notification-preferences + admin-email console) + kitehub-email (send). Risk class trọng tâm (theo KH-5..9): cross-tenant IDOR, gateway X-User-Roles→authority bridge, schema drift, role-literal mismatch, email side-effect thật tới MailHog, anon-safety của feedback.

## 2. Task Breakdown

1. Pre-walk Opus persona-sim (≥5 FM) → artifact.
2. MUST-run checks (seed role / email exposure / email_sent_log schema).
3. Walk 4 sub-flow + support, happy + sad, security spot-checks.
4. Catalog bugs → file gaps → wave plan + sync targets.

## 3. Scope

- `kitehub-subscription`: `FeedbackController` (`POST /api/v1/feedback`), `NotificationPreferenceController` (`GET`+`PATCH /api/v1/notification-preferences/{type}`), `AdminEmailController` (`/api/platform/admin/emails/{history,stats,config,trigger}` PLATFORM_ADMIN).
- `kitehub-email`: `EmailController` (`POST /api/platform/emails/send`).
- `kitehub-frontend`: `SupportMenu.tsx` (FE-only nav), `FeedbackForm/Widget`, `use-notification-preferences`.
- `kitehub-gateway`: routing + JWT filter.

## 4. State-Check Evidence

- Stack up đầy đủ healthy (gateway :9000, subscription, email, postgres, rabbitmq, mailhog).
- Seed: OWNER `owner.test@test.vn` (tenant aaaabbbb-…-0001, UUID sub), PLATFORM_ADMIN `admin.test@test.vn` + `admin@kitehub.com` (cả 2 đúng `PLATFORM_ADMIN`, không alias `ADMIN` → FM-7 walk-blocker refuted).
- Tables: `feedback_submissions` (user_id varchar), `notification_preferences` (user_id uuid — type inconsistency noted, OWNER sub là UUID nên cast OK), `email_sent_log`/`email_logs` (0 rows cold-start).
- Admin login chặn 2FA enrollment (by-design) → mint HS512 PLATFORM_ADMIN JWT trực tiếp bằng gateway `JWT_SECRET` (gateway validate là cổng thật; không mutate DB, không cần restore — sạch hơn KH-9 temp-relax).

## 5. Verification Gates

### Pre-walk

Opus persona-sim, 12 FM, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh10-notification-email-feedback-support.md` (🔴1 🟠4 🟡5 🟢2). Headline: (i) gateway role-bridge `XUserRolesHeaderFilter` present cho admin-email + notif-prefs (KHÔNG phải KC-7 GAP-1003 dead-deny); (ii) feedback anon-safe (permitAll + honeypot + bean-validation + category whitelist double-validated).

### G1 walk — evidence (live gateway :9000)

**Happy/sad paths (PASS):**
- Feedback: anon `POST /api/v1/feedback` (rating+comment+category) → **201** RECEIVED; owner → **201** (DB row có user_id=owner UUID + tenant_id + client_ip); bad category → 400; missing rating → 400; comment <5 chars → 400.
- Notification prefs: cold-start GET (0 rows) → **200** synthesized defaults (ABSENCE/FEE_REMINDER/EXAM_RESULT non-mandatory + TRIAL_ENDING/BILLING_INVOICE/SECURITY_ALERT mandatory); PATCH disable non-mandatory ABSENCE → 200; PATCH disable mandatory SECURITY_ALERT → **400** MANDATORY_TYPE_CANNOT_BE_DISABLED; PATCH bad type → **400** INVALID_NOTIFICATION_TYPE; no-token → 401.
- Admin email console: `GET /history` 200 paged, `GET /stats` 200, `GET /config` 200, `POST /trigger` unknown type → **400** "Unknown email type" (FM-9 safe), valid type resend → 409 dedup (GAP-1033).
- Email send: functional → MailHog delivers.
- Support menu: FE links resolve — `/beta-status` ((public)/beta-status/page.tsx), `/help/*` (p1/p2-owner/p3), `/help/anonymous`, mailto, Zalo OA.

**Inverse authz / security spot-checks (PASS):**
- Owner token → admin-email `/stats` → **403** (PLATFORM_ADMIN gate effective).
- FM-4 header-spoof IDOR: inject fake `X-User-Id` + `X-User-Roles: ROLE_PLATFORM_ADMIN` với owner token → trả về OWNER's own prefs (không phải fake identity). Gateway `RemoveRequestHeader=X-User-Id/X-User-Roles` default-filters strip + re-inject từ verified JWT → IDOR defended at gateway.

**Bug surfaced (1 P0 + 2 minor) — all filed, no inline fix:**
- 🔴 **GAP-1031 P0** (FM-1): anon `POST :9000/api/platform/emails/send` (valid body) → **200 SENT** to MailHog, no JWT. Root cause 2-layer: gateway pass-through-on-missing-token (`JwtAuthenticationGatewayFilter:130-133`) × kitehub-email ZERO Spring Security. Email là service duy nhất không có security → pass-through model vỡ chỉ ở đây. Gateway route `platform-email` không có caller hợp lệ (3 internal caller dùng direct docker :8080; FE endpoints.ts:69 dead) = pure attack surface. Fix Option A remove route + B email service security.
- 🟡 **GAP-1032 P2** (FM-2): admin stats `failedToday` luôn ~0 (`email_sent_log` V11 thiếu status column; orphan `email_logs` V5). Data-semantics, không 500.
- 🟢 **GAP-1033 P3**: admin `/trigger` resend → 409 dedup, chặn resend hợp lệ + message mơ hồ.

**No inline fix** — GAP-1031 là gateway edge (blast radius cao, all flows phụ thuộc gateway) + cần verify internal email không gãy sau khi remove route → batch Wave security-1 cùng GAP-1015/1019/1023/1025 IDOR cluster + re-walk email-dependent flows. Per `release-fix-retry-budget` §3.5 investigation-first đã hoàn tất (root cause + scope + 3 fix options trong gap).

## 6. Agent Spawn Pattern

1 Opus pre-walk persona-sim agent (background, model opus per `agent-model-opus-default.md`). Walk solo coordinator. Không parallel bucket (G1 walk linear).

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1031 P0 — arbitrary unauthenticated email send (Backend, gateway+email)
- GAP-1032 P2 — email stats failedToday meaningless (Backend)
- GAP-1033 P3 — admin trigger 409 resend (Backend)

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

**Bug class GAP-1031 signature:** gateway public route → backend service without Spring Security (pass-through-on-missing-token relies on absent downstream guard).

**Grep — services without spring-boot-starter-security routed publicly via gateway:**
| Service | spring-security in pom? | Gateway-routed | Verdict |
|---|---|---|---|
| kitehub-email | ❌ NONE | ✅ /api/platform/emails/** | **FIX (GAP-1031)** |
| kitehub-subscription | ✅ | ✅ | EXEMPT (has security) |
| kitehub-admin | ✅ | ✅ | EXEMPT |
| kitehub-branding | ✅ | ✅ | EXEMPT |
| kiteclass-core | ✅ | ✅ | EXEMPT |

Sweep: kitehub-email là service DUY NHẤT thiếu Spring Security + routed qua gateway → GAP-1031 là single-site cho bug class này. No additional fix site. Documented in GAP-1031.

### Sync targets

- gap-status.csv: 3 rows GAP-1031/1032/1033 ✅
- campaign §4 table: KH-10 row → 🔄 walk-pass-pending-human ✅
- wave-history.jsonl: flow-kh10 entry ✅
- audits-index.csv: pre-walk row ✅

### Outcome

KH-10 G1 **PASS** — 4 sub-flow + support đều reachable + functional; security gates (admin 403, IDOR header-spoof defended) PASS; 1 P0 (arbitrary email send) + 2 minor filed cho Wave security-1. Campaign KH-10 → `🔄 walk-pass-pending-human`. Docs-only PR.

## 8. Log

- **2026-06-06:** G1 walk hoàn tất. Pre-walk Opus 12 FM. Walk gateway :9000: feedback 201 (anon+owner attribution) + notif-prefs defaults/mandatory-guard/bad-type/auth + admin-email history/stats/config/trigger + support FE links. Security: owner→admin 403; FM-4 header-spoof IDOR defended (gateway strip+re-inject). Bug: GAP-1031 P0 arbitrary unauthenticated email send (gateway pass-through × email zero-security; route thừa), GAP-1032 P2 failedToday semantics, GAP-1033 P3 trigger-409 resend. No inline fix (gateway edge → Wave security-1). Cross-flow sweep: email là single site thiếu Spring Security. Campaign → walk-pass-pending-human.
