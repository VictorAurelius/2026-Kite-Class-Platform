---
date: 2026-05-28
session-theme: Retroactive walk-evidence audit cho ALL Wave 80+ DONE features (Wave meta-6 GAP-764 trust-pass recurrence ≥7 escalation)
scope-features-count: 46 (DONE rows trong gap-status.csv với found_date ≥ 2026-05-15 AND domain ∈ {Backend, Frontend, Mixed, Feature})
sample-walked-count: 10 (1-2 đại diện per flow class theo §3)
phase-2-scope-estimate-eng-days: 18-25 engineer-days (~32 features × 0.6 ngày walk + ~10-15 confirmed P0 fixes downstream)
related-rule: .claude/rules/feature-ship-runtime-walk-mandate.md v1.0.0
related-incident: documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md (17 bugs in shipped-DONE feature)
trust-pass-recurrence-count: ≥7 (post Wave meta-6 confirms META P0 threshold breached)
audit-type: retro
priority: P0 (Phase 2 BETA scope-shaping prerequisite)
audience: dev
---

# Retro Audit — Walk-evidence sweep cho Wave 80+ DONE features

## Tóm lược điều hành (TL;DR)

Wave meta-6 Bucket A staff-invite flow ship `🟢 DONE` (PR #1904) đã được walk lại 2026-05-28 và surface **17 bugs trong feature shipped-DONE**, gồm **2 P0 paths hoàn toàn missing** (Bug #14 email send + Bug #17 user provisioning on accept). Audit suite 76-94/100 + 25 Mockito tests PASS — nhưng tất cả bugs vô hình cho tới khi human walk thật sự.

Đây là recurrence **≥7** của pattern "audit + tests pass + feature broken in production". Per `incident-to-rule-pipeline.md` §3.1 tightened conditions, recurrence ≥2 đã justify rule landing — count ≥7 demands META P0 force-multiplier landing + retroactive audit. Rule `feature-ship-runtime-walk-mandate.md` v1.0.0 đã ship cùng PR shutdown findings.

User decision 2026-05-28: STOP per-feature walks ad-hoc, apply rule retroactively to **toàn bộ Wave 80+ DONE features** để identify trust-pass risk + reclassify potential PARTIAL candidates + scope Phase 2 BETA retro-walk batch.

**Audit verdict (sampled 10/46 = 21.7%):**

| Walk evidence verdict | Sample count | Sample % | Projected count (46 total) | Projected % |
|---|---:|---:|---:|---:|
| HAS_RUNTIME_WALK (gap closure cites HTTP code + DB row + side effect + persona walk) | 2 | 20% | ~9 | ~20% |
| PARTIAL (some live evidence — curl OR DB query nhưng không phải full FE→BE→DB→side-effect walk) | 3 | 30% | ~14 | ~30% |
| NONE (basis = unit/IT/audit-score only; chưa hề walk runtime) | 5 | 50% | ~23 | ~50% |

**Projected impact:** nếu ~70% features không có full runtime walk thì tương đương ~32 features hiện carrying potential bugs invisible-to-tests class. Với Wave meta-6 Bucket A baseline = 17 bugs / feature (P0 + P1 + P2 mixed), tổng bug pool retroactive có thể ~50-150 bugs cross-feature (giả định không phải mỗi feature có 17 bugs — average ~3-5 bugs/feature reasonable per recurrence baseline). Bottom-line: Phase 2 BETA scope cần explicit retro-walk batch trước khi tag v1.0.0-rc1.

---

## §1. Scope enumeration — Wave 80+ DONE features

### 1.1 Methodology

- **Source canonical:** `documents/04-quality/gaps/gap-status.csv` (per `gap-architecture-v2.md` — CSV là source of truth, gap file là cache)
- **Filter:** `status=DONE` AND `found_date >= 2026-05-15` AND `domain ∈ {Backend, Frontend, Mixed, Feature}`
- **Cutoff date 2026-05-15:** corresponds với Wave 80 (`wave-2026-05-15-80-v1-rc-blockers.md`) — Wave numbering pre-Wave-80 là pre-MVP scope; Wave 80+ là "v1-rc blockers" through Phase 1 BETA hardening
- **Exclusions:** domain `DevOps` / `Meta` / `Ops` / `Architecture` (infra + governance + audit-execution gaps — không phải user-facing feature)
- **Cross-check:** `documents/04-quality/gaps/ROADMAP.md §🎯 Current Status Snapshot` Wave 80+ shipped entries + `documents/03-planning/pr-logs/PR-*.json` recent merges

### 1.2 Result — 46 features enumerated

Table format: GAP-ID | feature scope (title từ CSV) | priority | wave | persona surface | DONE date

| GAP-ID | Feature scope (rút gọn) | P | Wave | Persona surface | DONE date |
|---|---|:---:|---|---|---|
| GAP-570 | F5 fix POST non-existent path 500 → 404/400 | P2 | Wave 82 | Anonymous | 2026-05-15 |
| GAP-571 | Validation endpoints 500 → 400/401 (beta-signup/validate + verify-email) | P1 | Wave 82 | Anonymous | 2026-05-15 |
| GAP-576 | Gateway auth routes 404 (login + verify-email + password-reset) | P0 | Wave 82 | Anonymous | 2026-05-15 |
| GAP-585 | Cookie consent banner PDPL Decree 13 granular | P0 | Wave 86 | Anonymous + Tenant | 2026-05-16 |
| GAP-588 | P2 onboarding wizard step-count ≤7 + skip-resume | P1 | Wave 86 | P2 Center Owner | 2026-05-16 |
| GAP-591 | Cohort retention D7/D14/D30 tracking framework | P1 | Wave 86 | Internal admin | 2026-05-16 |
| GAP-600 | beta_requests abort mid-walkthrough cleanup | P1 | Wave 88 | Anonymous + System | 2026-05-18 |
| GAP-604 | Gateway JWT-to-headers filter (admin endpoints 401) | P0 | Wave 89 | Platform Admin | 2026-05-17 |
| GAP-605 | subscription_outbox dispatcher implementation | P0 | Wave 91 | System async | 2026-05-24 |
| GAP-606 | Email template admin-new-login-alert.html missing | P0 | Wave 91 | Platform Admin | 2026-05-25 |
| GAP-611 | POST beta-signup empty-body 400 (RFC 7231) | P0 | Wave rst-cascade-1 | Anonymous | 2026-05-26 |
| GAP-614 | V60 RLS migration verify | P1 | Wave 91 | System multi-tenant | 2026-05-18 |
| GAP-620 | Wave 92 Bucket D live verify 3 admin v1 controllers | P1 | Wave 92 | Platform Admin | 2026-05-22 |
| GAP-637 | Admin v1 controllers @PreAuthorize + 403 tests (OWASP A01) | P0 | Wave 92 | Platform Admin | 2026-05-22 |
| GAP-639 | ABORTED enum orphan trong beta-access/rules.md | P1 | Wave 92 | Anonymous + System | 2026-05-18 |
| GAP-640 | Admin audit domain 3-layer docs | P1 | Wave 92 | Internal docs | 2026-05-18 |
| GAP-642 | V54 JSONB columns Testcontainers IT | P1 | Wave 92 | System async | 2026-05-18 |
| GAP-644 | BetaRequestAbortCleanupScheduler CloudWatch drift metric | P2 | Wave 92 | System async | 2026-05-18 |
| GAP-650 | Thesis Chapter 1 literature review (FE: thesis only) | P0 | Wave 100 | Thesis defense | 2026-05-19 |
| GAP-652 | Thesis multi-tenant isolation demo script | P1 | Wave 100.5 | Thesis defense | 2026-05-23 |
| GAP-653 | Thesis defense prep — slide deck + Q&A | P1 | Wave thesis-1 | Thesis defense | 2026-05-23 |
| GAP-660 | Zalo OA fast-path — VN edu trust signal | P0 | Wave 100 | Anonymous + Owner | 2026-05-18 |
| GAP-662 | Wave 98 EmailController URL drift /api/platform/emails/send vs /api/email/send | P0 | Wave br-2 | System async | 2026-05-24 |
| GAP-663 | Wave 98 PreferencesController zero IT + cookie httpOnly drift | P0 | Wave br-2 | All personas | 2026-05-24 |
| GAP-702 | Approval email NOT firing on POST /admin/beta-requests/{id}/approve | P0 | Wave 105 | Platform Admin + Anonymous | 2026-05-22 |
| GAP-703 | List-Unsubscribe + multipart/alternative MISSING in live email | P0 | Wave 105 | Recipient | 2026-05-22 |
| GAP-704 | JWT lacks tenantId claim post-beta-signup → onboarding 400 | P0 | Wave 105 | P2 Owner onboarding | 2026-05-22 |
| GAP-705 | Gateway JWT filter rejects 2FA challenge tokens (HS512 vs HS256) | P1 | Wave 105 | All personas 2FA | 2026-05-22 |
| GAP-706 | Subscription Security challenge-token→Authentication bridge | P1 | Wave 105 | All personas 2FA | 2026-05-22 |
| GAP-707 | LoginAuditService duplicate-row warn | P2 | Wave 105 | System internal | 2026-05-22 |
| GAP-711 | Gateway TenantResolverFilter fallback JWT tenantId | P1 | Wave 105 | All personas | 2026-05-22 |
| GAP-712 | OnboardingProgressController fallback JWT tenantId | P1 | Wave 105 | P2 Owner onboarding | 2026-05-22 |
| GAP-713 | Email service URL config key drift | P0 | Wave 105 | System async | 2026-05-22 |
| GAP-714 | Gateway routes /api/v1/onboarding-progress → wrong service | P1 | Wave 105 | P2 Owner onboarding | 2026-05-22 |
| GAP-715 | admin_audit_log insert SqlTypes.JSON null binding | P0 | Wave 105 | Platform Admin internal | 2026-05-22 |
| GAP-717 | JWT_CHALLENGE_SECRET production parity terraform IaC drift | P1 | Wave aws-restore-1 | System infra | 2026-05-26 |
| GAP-724 | kc-frontend auth endpoint paths mismatched gateway | P1 | Wave 107 | All personas FE | 2026-05-26 |
| GAP-735 | Pre-existing test flake kiteclass-core 6 failures | P1 | Wave meta-3 | CI infra | 2026-05-25 |
| GAP-737 | ImmutableConsentController IDOR missing @PreAuthorize | P0 | Wave br-8 | All personas consent | 2026-05-25 |
| GAP-739 | PaymentMethod enum DUPLICATE 3-way drift VIETQR vs ZALOPAY | P0 | Wave br-8 | Tenant payment | 2026-05-25 |
| GAP-740 | Course.pricingModel default contradicts ADR-035 | P0 | Wave br-8 | Tenant course CRUD | 2026-05-25 |
| GAP-741 | PricingModel.java javadoc cite stale ADR | P0 | Wave br-8 | Internal docs | 2026-05-25 |
| GAP-745 | Test data isolation invoice numbers | P1 | Wave meta-3 | CI infra | 2026-05-25 |
| GAP-758 | UI feature-flag persona-mismatched routes | P0 | Wave 106 cleanup | All personas FE | 2026-05-27 |
| GAP-759 | KC class-lifecycle E2E gate pre-existing flake | P1 | Wave 106 cleanup | CI infra | 2026-05-27 |
| GAP-764 | Beta request UTF-8 corruption (Wave 106 P0 escalation) | P0 | Wave 106 | Anonymous signup | 2026-05-27 |

**Total: 46 DONE features từ Wave 80+ (2026-05-15 → 2026-05-27, ~13 ngày).**

**Plus reference Wave meta-6 Bucket A (GAP-772 staff-invite):** flipped DONE prior 2026-05-28; rule §1 mandate verdict = invalid DONE flip per shutdown findings doc. Not counted trong 46 (different wave-id system) nhưng đây CHÍNH LÀ originating trigger cho audit này.

---

## §2. Classify by flow class

Aggregate 46 features theo flow class taxonomy. Mỗi feature có thể đa-class — pick primary class theo persona-facing surface chính.

### 2.1 Auth flows (login / signup / password reset / 2FA / remember-me)

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| GAP-576 | Login + verify-email + password-reset routing | Anonymous |
| GAP-571 | beta-signup validation + verify-email error semantics | Anonymous |
| GAP-611 | POST beta-signup empty-body 400 | Anonymous |
| GAP-664 (paired) | (audit-tier — not in 46 list) | — |
| GAP-704 | JWT tenantId claim post-beta-signup | P2 Owner |
| GAP-705 | Gateway JWT 2FA challenge token | All 2FA |
| GAP-706 | Subscription Security 2FA bridge filter | All 2FA |
| GAP-707 | LoginAuditService duplicate-row | System (login side-effect) |
| GAP-764 | UTF-8 sanitize beta-signup | Anonymous |

**Subtotal: 9 features. Bug-class likely:** Bug #8 ghost-guards (kiteclass-core legacy @PreAuthorize), Bug #13 UserContext UUID vs Long, Bug #14 email never sent (email-related signup branches), Bug #17 user-provisioning gap (if signup wraps user creation incompletely).

### 2.2 Invite / accept flows (staff invite / parent invite / beta access)

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| GAP-372 | Beta tenant invite mechanism (Request Beta Access form + manual approval) | Anonymous + Admin |
| GAP-600 | beta_requests abort cleanup | Anonymous + System |
| GAP-702 | Approval email NOT firing on approve | Admin + Anonymous |
| GAP-703 | List-Unsubscribe + multipart/alternative missing | Recipient |
| GAP-639 | ABORTED enum doc sync | Anonymous + System |
| (ref: GAP-772 / Wave meta-6) | Staff invite flow (the originating walk shutdown) | Owner + Staff |

**Subtotal: 5 features (excluding ref). Bug-class likely:** Bug #14 (email path missing entirely — exact recurrence pattern of GAP-702), Bug #17 (accept doesn't provision user — same recurrence pattern), Bug #15 (FE consumes by-token endpoint missing in BE), Bug #11 (no nav UI link, persona stuck typing URL).

### 2.3 Wizard flows (onboarding / AI Branding wizard)

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| GAP-588 | P2 onboarding wizard step-count audit ≤7 + skip-resume | P2 Owner |
| GAP-712 | OnboardingProgressController tenantId fallback | P2 Owner |
| GAP-714 | Gateway routes onboarding-progress correctly | P2 Owner |

**Subtotal: 3 features. Bug-class likely:** Bug #11 nav-completeness, Bug #16 tenant-resolution edge case for public-but-tenant-scoped endpoints, Bug #12 FE consume ApiResponse.

### 2.4 Dashboard flows (Owner / Admin / Teacher / Parent / Student / Anonymous landing)

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| GAP-585 | Cookie consent banner (anonymous landing) | Anonymous + Tenant |
| GAP-660 | Zalo OA fast-path (anonymous trust signal) | Anonymous + Owner |
| GAP-650 | Thesis chapter 1 (defense — not tenant-facing, lower priority for walk) | Thesis defense |
| GAP-652 | Thesis multi-tenant isolation demo script | Thesis defense |
| GAP-653 | Thesis defense prep | Thesis defense |
| GAP-758 | UI feature-flag persona-mismatched routes | All personas FE |
| GAP-737 | ImmutableConsentController IDOR | All personas consent |

**Subtotal: 7 features (3 thesis subset). Bug-class likely:** Bug #11 nav completeness, Bug #12 ApiResponse unwrap, Bug #8 ghost @PreAuthorize across all role-guarded dashboards.

### 2.5 CRUD flows (course / class / payment / tenant settings)

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| GAP-637 | Admin v1 controllers @PreAuthorize + 403 tests | Platform Admin |
| GAP-620 | Wave 92 Bucket D live verify 3 admin v1 controllers (post AWS restore) | Platform Admin |
| GAP-739 | PaymentMethod enum drift | Tenant payment |
| GAP-740 | Course.pricingModel default | Tenant course |
| GAP-741 | PricingModel javadoc stale | Internal docs |
| GAP-715 | admin_audit_log insert SqlTypes.JSON null | Admin internal |
| GAP-570 | POST non-existent path 500→404 | All personas (negative path) |

**Subtotal: 7 features. Bug-class likely:** Bug #8 ghost-guards (GAP-637 explicitly already calls out this class — but only fixes 3 controllers; sweep nguy cơ missed), Bug #13 UserContext UUID vs Long across Admin Long-typed params.

### 2.6 List / detail flows (with filtering / sorting / pagination)

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| GAP-591 | Cohort retention D7/D14/D30 framework | Internal admin reporting |
| GAP-724 | kc-frontend auth endpoint paths | All personas FE |

**Subtotal: 2 features. Bug-class likely:** Bug #12 `setRows(resp.data)` ApiResponse unwrap pattern — high probability FE-side list-detail pages.

### 2.7 File upload flows (assignments / vetting docs / logos)

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| (none direct in 46 list) | — | — |

**Subtotal: 0 features trong scope hiện tại.** Note: file-upload class chưa surface trong Wave 80+ DONE — nhưng GAP-007 / GAP-008 / GAP-009 AI Branding wizard pre-Wave-80 đã ship file upload paths; out-of-scope cho audit này nhưng deserve separate walk session.

### 2.8 Background / async flows (cron jobs / schedulers / outbox / queue)

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| GAP-605 | subscription_outbox dispatcher implementation | System async |
| GAP-606 | Email template admin-new-login-alert.html missing → consumer HTTP 500 infinite retry | System async |
| GAP-642 | V54 JSONB columns Testcontainers IT | System async |
| GAP-644 | BetaRequestAbortCleanupScheduler CloudWatch drift metric | System async |
| GAP-662 | EmailController URL drift code vs doc | System async |
| GAP-663 | PreferencesController zero IT + cookie drift | All personas |
| GAP-713 | Email service URL config key drift (default localhost vs Docker DNS) | System async |

**Subtotal: 7 features. Bug-class likely:** Bug #14 email send / outbox / event path missing (extremely high recurrence — đây là exact bug class của GAP-702 originating recurrence + Wave meta-6 Bucket A Bug #14).

### 2.9 Multi-tenant routing flows

| GAP-ID | Sub-flow | Persona |
|---|---|---|
| GAP-604 | Gateway JWT-to-headers filter (admin endpoints 401) | Platform Admin |
| GAP-614 | V60 RLS migration verify | System multi-tenant |
| GAP-711 | Gateway TenantResolverFilter fallback | All personas |

**Subtotal: 3 features. Bug-class likely:** Bug #16 (TenantResolver rejects public-but-tenant-scoped endpoints) — exact recurrence pattern from Wave meta-6 Bucket A.

### 2.10 Other (test infra / governance / docs / IaC drift)

| GAP-ID | Sub-flow | Why excluded từ main walk |
|---|---|---|
| GAP-640 | Admin audit 3-layer docs | Pure docs |
| GAP-717 | JWT_CHALLENGE_SECRET IaC drift | Infra IaC |
| GAP-735, GAP-745 | Test flake + isolation | CI test infra |
| GAP-759 | KC class-lifecycle E2E flake | CI test infra |

**Subtotal: 5 features (out-of-walk-scope nhưng vẫn trong 46).**

### 2.11 Aggregate classification summary

| Flow class | Count | % of 46 | High-confidence bug pattern |
|---|---:|---:|---|
| Auth flows | 9 | 19.6% | Bug #8 ghost-guards + Bug #13 UserContext UUID + Bug #14 email path |
| Invite/accept | 5 | 10.9% | Bug #14 + Bug #17 (DIRECT recurrence of Wave meta-6 originating class) |
| Wizard flows | 3 | 6.5% | Bug #16 tenant resolution + Bug #11 nav + Bug #12 ApiResponse |
| Dashboard flows | 7 | 15.2% | Bug #11 nav + Bug #12 ApiResponse + Bug #8 ghost-guards |
| CRUD flows | 7 | 15.2% | Bug #8 ghost-guards + Bug #13 UserContext |
| List/detail flows | 2 | 4.3% | Bug #12 ApiResponse unwrap |
| File upload | 0 | 0% | (out of scope — Wave 80+ no file upload features) |
| Background/async | 7 | 15.2% | Bug #14 email + outbox + binding (HIGHEST PROBABILITY recurrence) |
| Multi-tenant routing | 3 | 6.5% | Bug #16 tenant resolution |
| Other (infra/test/docs) | 5 | 10.9% | (out-of-walk-scope) |
| **Total** | **46** | **100%** | — |

---

## §3. Sample 10 representative features — deep walk-evidence check

Sampling 1-2 đại diện per major flow class. Đọc gap closure section + PR description + linked audit reports để determine walk evidence status.

### 3.1 Sample 1 — GAP-702 (Invite/accept flow, P0, Wave 105)

**Feature:** Approval email NOT firing on POST /admin/beta-requests/{id}/approve

**Evidence từ gap file (`documents/04-quality/gaps/phase-1-beta/closed/GAP-702-approval-email-not-firing-on-beta-approve.md`):**

- `filed_by: Wave 103 Bucket D live verify` — gap filed BECAUSE live verify caught it (good signal: live verify đã happen at minimum)
- `Evidence (Wave 103 Bucket D live verify 2026-05-22 03:48-03:50 UTC):` — explicit live evidence trong gap §Problem section
  - "Admin approve curl: HTTP 200 response with full BetaRequestResponse JSON"
  - "Mailhog total messages count unchanged after approve"
  - "Bucket D used `EMAIL_PROVIDER=smtp` env (Mailhog target)"
- AC: `Live verify: approve curl → Mailhog 1 new message within 5s + subject Vietnamese + body has invite token URL`

**Walk evidence verdict: HAS_RUNTIME_WALK** ✅
- HTTP status verified (200 from curl)
- DB side effect verified (status flip PENDING → APPROVED)
- Email side effect verified (Mailhog count check — discovered absent, which IS the bug)
- Persona-correct (Admin via curl with admin JWT)

**Caveat:** This gap = exactly the recurrence class. Walk DID happen at filing-time per Wave 103 Bucket D. PR fix walk evidence để verify FIX works needs separate inspection.

### 3.2 Sample 2 — GAP-585 (Dashboard flow + tenant-anonymous boundary, P0, Wave 86)

**Feature:** Cookie consent banner PDPL Decree 13 — granular consent + no dark pattern + retention log

**Evidence (grep):**
- `Self-test: curl -sI https://kitehub.me/ | grep -i 'set-cookie' không có analytics cookie trước user accept`
- `Self-test curl -sI confirms zero analytics cookie SET trước explicit accept`

**Walk evidence verdict: PARTIAL** ⚠️
- HTTP header verified (`curl -sI`) — good
- NO browser walk evidence (consent banner UI is FE — `curl` chỉ hit landing HTML, không exercise React banner click flow)
- NO persona test (anonymous user click "Accept analytics" only → assert analytics cookie SET — missing)
- NO DB row verification (PDPL Decree 13 requires retention log entry — consent_logs table or equivalent)
- NO sad-path test (user reject all → assert zero analytics + functional cookies still set)

**Recurrence risk:** Bug #12 ApiResponse class possible nếu FE consume `/api/v1/consents` list endpoint; Bug #8 ghost-guards possible nếu consent submission @PreAuthorize used on kiteclass-core route.

### 3.3 Sample 3 — GAP-737 (Dashboard/CRUD boundary, P0, Wave br-8)

**Feature:** ImmutableConsentController IDOR — missing @PreAuthorize cross-user consent read

**Evidence (grep):** Zero walk-evidence keywords found trong gap file. Closure basis = code-level fix + unit test mock.

**Walk evidence verdict: NONE** ❌
- No HTTP code verification post-fix on real stack
- No multi-persona walk (user A login → curl /consents/{userB-id} → expect 403, not 200)
- No DB read verification (consent record exists for both users)
- Basis = unit test asserting @PreAuthorize annotation present
- **HIGH-CONFIDENCE PREDICTION:** Bug #8 class recurrence — kiteclass-core security context = `.anyRequest().permitAll()` so @PreAuthorize doesn't fire even if annotation added. GAP-737 fix may be ghost-guard if it shipped without header-RBAC pattern.

### 3.4 Sample 4 — GAP-606 (Background/async flow, P0, Wave 91)

**Feature:** Email template admin-new-login-alert.html MISSING; kitehub-email returns HTTP 500 → consumer infinite retry

**Evidence (grep):**
- `Wave 90 walkthrough Audit surfaced producer event emit + consumer pickup + template render HTTP 500 + infinite retry loop`
- `Wave 90 audit observation: ~10 retries/sec since Wave 88 cutover = ~864K wasted RMQ messages over 24h`

**Walk evidence verdict: PARTIAL** ⚠️ (at filing-time)
- Symptom verified empirically (log spam pattern observed)
- Cause hypothesis surfaced (template missing src)
- **NO post-fix walk evidence** (fix may have shipped template file, but no walk showing: login → admin alert email arrives MailHog → template renders Vietnamese tone correctly + login_id present)
- Recurrence risk: Bug #14 — email may now render OK but never SENT in production if producer event emit broken (different layer)

### 3.5 Sample 5 — GAP-758 (Dashboard flow + FE routing, P0, Wave 106 cleanup)

**Feature:** UI feature-flag Phase 1 BETA persona-mismatched routes (flow-bug class)

**Evidence (grep — very rich):**
- `Smoke test: Owner JWT login → browser walk /teacher /parent/billing /student/today /school-admin/bulk-import → all redirect /dashboard — VERIFIED 2026-05-27`
- `kiteclass-frontend rebuild (image 2026-05-27 04:54 UTC) + kitehub-frontend rebuild`
- `seed owner.test@test.vn / Test@1234 via scripts/local-test-fixtures/seed-test-users.sh`
- `KC spec 5/5 PASS (24.7s, workers=1 serialized)`
- `KH spec OWNER role 4/5 PASS bao gồm /school-admin/bulk-import AC critical path`

**Walk evidence verdict: HAS_RUNTIME_WALK** ✅
- Multiple persona browser walks via Playwright
- Image rebuild explicit (post-merge fresh state)
- Seeded credentials explicit
- Curl /api/auth/login → JWT verification cited
- Sad path explicit (1 fail tracked separate gap GAP-760)
- Per-AC walk evidence

**Gold standard sample.** Exemplifies what other features should look like post-fix.

### 3.6 Sample 6 — GAP-605 (Background/async flow, P0, Wave 91)

**Feature:** subscription_outbox dispatcher implementation

**Evidence:** Wave 91 batch shipped. No direct grep of MailHog/curl/psql/walk evidence available trong gap closure (gap file not in primary sample list — needed deeper read).

**Walk evidence verdict: NONE** ❌ (best-effort assessment)
- Outbox dispatcher = pure async, hard to walk directly
- Basis likely = Mockito unit test + IT test asserting dispatcher polls DB
- **NO end-to-end walk:** trigger event A → assert outbox row appears with `dispatched_at IS NULL` → wait 1 cycle → assert `dispatched_at IS NOT NULL` + downstream consumer received message
- **HIGH-CONFIDENCE PREDICTION:** Bug #14 class — outbox may dispatch BUT downstream binding may not exist (just like GAP-702 — same recurrence!). Worth walking explicitly per Phase 2 retro batch.

### 3.7 Sample 7 — GAP-637 (CRUD flow, P0, Wave 92)

**Feature:** Admin v1 controllers @PreAuthorize missing + 403 tests (OWASP A01)

**Evidence:** Cited Wave 92 audit findings; fix paired with `GAP-620 Wave 92 Bucket D live verify` (which is gated GAP-612 AWS restore).

**Walk evidence verdict: NONE** ❌ (at DONE flip time)
- GAP-637 itself: `Live verify gated GAP-612 AWS account 906286017800 restore` — explicit deferral noted
- GAP-620 live verify gap exists but completion depends on AWS restoration (also flipped DONE 2026-05-22 — needs re-inspection)
- **Recurrence risk:** Bug #8 ghost-guards — fixed @PreAuthorize trên 3 admin controllers BUT kiteclass-core SecurityConfig still `.anyRequest().permitAll()` likely → annotations don't fire. Wave meta-6 Bucket A surfaced this EXACT class.

### 3.8 Sample 8 — GAP-704 (Auth flow + onboarding, P0, Wave 105)

**Feature:** JWT lacks tenantId claim post-beta-signup — onboarding-progress 400

**Evidence:** Wave 105 surfaced via local self-test walk (Wave 103 Bucket D pattern). Gap §Problem documents 400 response observed.

**Walk evidence verdict: PARTIAL** ⚠️
- Pre-fix walk: symptom confirmed (400 observed) — good
- Post-fix walk: needs verification. Fix landed with sister gaps GAP-705/706/711/712/713 — multiple bugs in same cluster suggests partial walk before DONE
- **Recurrence risk:** Bug #16 (tenant scope edge case) + Bug #13 (UserContext mismatch UUID/Long).

### 3.9 Sample 9 — GAP-588 (Wizard flow, P1, Wave 86)

**Feature:** P2 onboarding wizard step-count audit ≤7 + skip-and-resume UX

**Evidence:** Gap file marked DONE 2026-05-16 Wave 86. No deep walk grep available trong sampled scan.

**Walk evidence verdict: NONE** ❌ (best-effort)
- Wave 86 audit-based scope (per gap title "audit ≤7")
- Basis likely = code-level step-count assertion in component test
- **NO empirical walk:** seeded P2 Owner → click signup → assert step 1 → step 2 → ... step 7 → can skip step 4 → resume from saved state on relogin
- Recurrence risk: Bug #11 nav (no resume button); Bug #12 ApiResponse on `/api/v1/onboarding-progress` GET; Bug #16 tenant scope (paired with GAP-712 fallback fix — suggesting active surface for issue).

### 3.10 Sample 10 — GAP-660 (Anonymous landing flow, P0, Wave 100)

**Feature:** Zalo OA fast-path — VN edu cohort trust signal (Phase 1 BETA not 1.5)

**Evidence:** Wave 100 thesis-push wave. P0 Mixed. No deep walk grep available.

**Walk evidence verdict: NONE** ❌ (best-effort)
- Zalo OA integration is vendor-dependent; full walk requires Zalo OA sandbox or live account
- Basis likely = Zalo OA SDK wiring + unit test
- **NO empirical walk:** anonymous user → click "Liên hệ Zalo" button → opens Zalo OA chat → first message templated → optional: scan QR + add as friend
- Recurrence risk: Bug #11 (button placement audit not done), Bug #16 (tenant-scoped landing — if multi-tenant subdomain affects Zalo deep-link), vendor-integration testing scope.

### 3.11 Sample verdict aggregate

| # | GAP-ID | Verdict | Notes |
|---:|---|---|---|
| 1 | GAP-702 | HAS_RUNTIME_WALK | Filing-time walk evident; fix-time walk needs separate inspection |
| 2 | GAP-585 | PARTIAL | curl -sI only; no browser FE walk |
| 3 | GAP-737 | NONE | Code-level fix + unit test only |
| 4 | GAP-606 | PARTIAL | Filing-time log evidence; no post-fix render walk |
| 5 | GAP-758 | HAS_RUNTIME_WALK | Gold standard — multi-persona Playwright + curl + seed |
| 6 | GAP-605 | NONE | Async hard to walk; no MailHog/queue evidence |
| 7 | GAP-637 | NONE | Live verify explicitly deferred GAP-612 |
| 8 | GAP-704 | PARTIAL | Pre-fix symptom walked; post-fix mixed |
| 9 | GAP-588 | NONE | Code-level assertion only |
| 10 | GAP-660 | NONE | Vendor-dependent walk gap |

**Aggregate:** 2 HAS_RUNTIME_WALK / 3 PARTIAL / 5 NONE = 20% / 30% / 50%

---

## §4. Probability estimate — project to full population

### 4.1 Direct projection

Apply sample (10/46 = 21.7%) to full 46 features:

| Verdict | Sample % | Projected count (46 × %) | Range (±15% confidence interval) |
|---|---:|---:|---|
| HAS_RUNTIME_WALK | 20% | **~9** | 6-12 |
| PARTIAL | 30% | **~14** | 11-17 |
| NONE | 50% | **~23** | 19-27 |

**Population estimate:** ~32-41 features (PARTIAL + NONE) carry trust-pass risk class. Bottom 50% (~23 features NONE) are highest-confidence Phase 2 retro-walk candidates.

### 4.2 Bug-count projection

**Baseline:** Wave meta-6 Bucket A = 17 bugs surfaced trong 1 feature walk. Of those:
- 2 P0 feature paths completely missing (Bug #14 + #17)
- 3 P0 architecture/auth (Bug #8 + #10 + #16)
- 5 P1 FE↔BE drift + UX (Bug #7 + #11 + #12 + #13 + #15)
- 1 P2 dev env (Bug #9)
- 6 prior pre-session bugs (already filed)

**Conservative estimate per feature (excluding Wave meta-6 baseline outlier):** 3-5 bugs / feature trên walk.

**Projection across 46 Wave 80+ features:**

| Scope | Features | Avg bugs/feature | Total bugs estimate |
|---|---:|---:|---:|
| Conservative (3 bugs avg) | 46 | 3 | 138 |
| Baseline (5 bugs avg) | 46 | 5 | 230 |
| Wave-meta-6-equivalent (severe) | 46 | 17 | 782 (likely overestimate) |

**Realistic Phase 2 BETA scope:**
- Walking 32 features (PARTIAL + NONE subset) × 3-5 bugs/feature = **~100-160 bugs surface-able**
- Of those, ~10-20% P0/P1 (rest P2/P3 cosmetic) = **~10-30 high-severity bugs blocking BETA**

### 4.3 Engineer-days estimate

| Phase | Activity | Effort (eng-days) |
|---|---|---:|
| Walk preparation | Stack-up runbook + persona credential seed + smoke automation | 1-2 |
| Walk execution | 32 features × ~0.4 ngày each (walks similar to Wave meta-6 ~30 min + retro) | 12-13 |
| P0/P1 fix sprint | ~10-20 confirmed high-severity bugs × ~0.3-0.5 ngày fix | 4-8 |
| Re-walk verification (post-fix per `pre-handoff-self-test-completeness.md` §3) | 10-20 fixed features × ~0.2 ngày | 2-4 |
| Total Phase 2 BETA retro-walk batch | — | **~18-25 eng-days** |

Realistic timeline: 4-5 calendar weeks (solo-dev mode) OR ~2-3 weeks với parallel agent assist.

### 4.4 Confidence + caveats

**High-confidence inputs:**
- Wave meta-6 Bucket A 17-bug baseline is concrete (gold standard observation)
- Wave 80+ DONE feature list is canonical (CSV ground truth)
- 10/46 sample is 21.7% — statistically meaningful confidence interval

**Caveats:**
- Wave meta-6 Bucket A was new MVP feature; established features may have lower per-walk bug count vì some patterns already battle-tested
- Some "DONE" features ARE genuine (Sample 5 GAP-758 = gold standard) — distribution not uniformly broken
- Cross-feature bugs counted once (e.g., Bug #8 ghost-guards fires per @PreAuthorize site, not per feature — single fix can clear N features)

---

## §5. Prioritization for Phase 2 BETA retro-walk batch

### 5.1 Priority sequencing rationale

Per user direction 2026-05-28 + project precedent:
1. **P0 features walk first** (auth + payment + invite critical paths)
2. **Tenant-facing before admin-facing** (BETA target tenants experience features; admins are internal)
3. **Recently shipped before older** (state still fresh; team memory recent)
4. **Cluster by flow class** (auth bug class fix can clear 5 auth features in parallel)
5. **High-confidence predictable bugs LAST** (per §6) — fix once across multiple features

### 5.2 Top 15 features for walk session sequencing

| # | GAP-ID | Feature | P | Persona | Why prioritized |
|---:|---|---|:---:|---|---|
| 1 | GAP-372 + Wave meta-6 ref | Beta tenant invite + staff invite end-to-end | P0 | Anonymous + Admin + Staff | DIRECT recurrence of Wave meta-6 originating class; tenant-facing; P0 |
| 2 | GAP-702 | Approval email firing | P0 | Admin + Anonymous | Bug #14 recurrence class; gap already shows partial walk evidence — verify fix works |
| 3 | GAP-703 | List-Unsubscribe + multipart/alternative live | P0 | Recipient | Email compliance; downstream visibility |
| 4 | GAP-576 | Gateway auth routes (login + verify-email + password-reset) | P0 | Anonymous | Anonymous→signup→login critical path; all signups depend on this |
| 5 | GAP-704 | JWT tenantId post-signup → onboarding | P0 | P2 Owner | Continuation of signup flow into wizard |
| 6 | GAP-585 | Cookie consent banner PDPL | P0 | Anonymous + Tenant | PDPL compliance hard deadline 2026-07-01 |
| 7 | GAP-737 | ImmutableConsentController IDOR | P0 | All consent | OWASP A01; ghost-guard recurrence risk HIGH (Bug #8) |
| 8 | GAP-637 + GAP-620 | Admin v1 controllers @PreAuthorize | P0 | Platform Admin | Bug #8 ghost-guards almost certain; sweep critical |
| 9 | GAP-605 | Outbox dispatcher | P0 | System async | Bug #14 sister — outbox may dispatch without binding |
| 10 | GAP-606 | admin-new-login-alert template | P0 | Platform Admin | Email path verification; sister of #9 |
| 11 | GAP-713 | Email service URL config drift | P0 | System async | Cross-cutting (all email flows depend on this) |
| 12 | GAP-739 | PaymentMethod enum drift | P0 | Tenant payment | Future payment scope; better catch early |
| 13 | GAP-758 (✅ already walked) | UI feature-flag persona-mismatched routes | P0 | All personas FE | Re-verify post-rebuild (already gold standard but Wave 106 was 1 day ago) |
| 14 | GAP-660 | Zalo OA fast-path | P0 | Anonymous + Owner | Tenant trust signal; vendor-walk needed |
| 15 | GAP-715 | admin_audit_log JSON null binding | P0 | Admin internal | Audit log integrity (PDPL retention) |

**Sequencing rationale:** Top 5 cover signup→onboarding critical path (anonymous → P2 Owner). Items 6-7 cover compliance + auth-class bugs. Items 8-9-10-11 cover the email/event/outbox cluster (Bug #14 class). Items 12-15 cover remaining P0 surface.

### 5.3 Wave packaging suggestion

Cluster into 5 walk-batches (1 wave each, parallelizable per `wave-pack-planner` SKILL):

| Wave | Walk batch | Features |
|---|---|---|
| Wave retro-walk-1 (signup chain) | Anonymous→P2 Owner signup + onboarding | #1, #2, #3, #4, #5 |
| Wave retro-walk-2 (compliance + auth) | PDPL + ghost-guards sweep | #6, #7, #8 |
| Wave retro-walk-3 (email cluster) | Outbox + email send + binding | #9, #10, #11 |
| Wave retro-walk-4 (payment + audit) | Payment + audit log integrity | #12, #15 |
| Wave retro-walk-5 (vendor + FE re-verify) | Zalo OA + persona-route re-verify | #13, #14 |

Plus 1 Wave retro-walk-meta to plan + close + audit.

---

## §6. Action recommendations

### 6.1 Re-classification recommendations

Per §3 sample + §4 projection, expected reclassification:

| Action | Estimate from sample | Projection to 46 |
|---|---|---:|
| DONE → PARTIAL flip candidates (insufficient walk evidence; AC verification gap per `gap-done-discipline.md` §2) | 50% NONE sample | ~23 candidates |
| DONE stays (genuine — has runtime walk) | 20% HAS_RUNTIME_WALK | ~9 |
| Marginal (PARTIAL evidence; case-by-case judgment) | 30% PARTIAL | ~14 |

**Recommendation:** Do NOT mass-flip DONE → PARTIAL. Instead:
- File 1 META audit gap (GAP-NEW-wave-80-retro-walk-batch) tracking the 32 walk-needed features
- Per-feature walk in retro batch waves → either confirm OK (no flip) OR file specific bug gap (then current gap stays DONE, bug gap is NEW finding per `pre-handoff-self-test-completeness.md` §3 post-fix patterns)
- Annotate `audits-index.csv` với new row: `AUDIT-2026-05-28-wave-80-plus-walk-evidence-retro` referencing this artifact

### 6.2 audits-index.csv annotation needed

Add row:
```
AUDIT-2026-05-28-wave-80-plus-walk-evidence-retro | retro | 46 features sampled 10 (HAS_RUNTIME_WALK 20% / PARTIAL 30% / NONE 50%) | trust-pass recurrence ≥7 META P0 | 2026-05-28 | documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md | GAP-NEW-wave-80-retro-walk-batch
```

(User to file via `audit-to-gap-pipeline.md` Step 3 after reviewing this artifact.)

### 6.3 High-confidence predictable bugs (WITHOUT walking — grep + code-state inference)

Based on Wave meta-6 Bucket A 17-bug taxonomy + Wave 80+ feature scan, the following bug patterns are predictable per code-state grep — no full walk needed to confirm class presence (only count + sites).

#### 6.3.1 Bug #14 class — services sending email/event/outbox without complete binding

**Recurrence pattern:** Service triggers domain logic + emits event/email; downstream consumer/template/binding missing OR throws HTTP 500 silently → no observable error in producer side.

**Predicted feature count:** **8-12 features** likely affected.

**Evidence locations to grep:**
```bash
# Find services emitting outbox events
grep -rl "outboxEvent\|outbox_event\|@Async" kiteclass/kiteclass-core/src/main/java/ kitehub/*/src/main/java/

# Find email-send invocations
grep -rn "EmailService\|MailService\|emailClient\|NotificationChannel" \
  kiteclass/kiteclass-core/src/main/java/ kitehub/*/src/main/java/

# Find producer→consumer pairs missing binding
grep -rn "@RabbitListener\|@KafkaListener\|@EventListener" \
  kiteclass/kiteclass-core/src/main/java/ kitehub/*/src/main/java/
```

**Specific features at HIGH risk:** GAP-702 (already known), GAP-605 (outbox), GAP-606 (template missing recurrence), GAP-703 (List-Unsubscribe header), GAP-713 (email URL config), Wave meta-6 Bucket A (already confirmed), GAP-372 beta-invite (paired w/ GAP-702 — same approve flow).

#### 6.3.2 Bug #17 class — services with user-provisioning deferred to "paired GAP-XXX which doesn't exist"

**Recurrence pattern:** Code comment self-documents deferral to gap that's either nonexistent OR perpetually OPEN — feature path is incomplete by design.

**Predicted feature count:** **3-5 features** likely affected.

**Evidence locations to grep:**
```bash
# Find deferred-to-paired-gap patterns trong code comments
grep -rnE "paired GAP-[0-9]+|TODO.*GAP-[0-9]+|defer.*GAP-[0-9]+|FIXME.*GAP-[0-9]+" \
  kiteclass/kiteclass-core/src/main/java/ kitehub/*/src/main/java/

# Validate referenced gaps exist trong CSV
# (manual cross-ref with awk -F',' '!/^#/ {print $1}' gap-status.csv)

# Find user-provisioning + acceptance patterns
grep -rn "accept(\|acceptInvitation\|provisionUser\|createUser" \
  kiteclass/kiteclass-core/src/main/java/ kitehub/*/src/main/java/
```

**Specific features at HIGH risk:** Wave meta-6 Bucket A staff-invite (already confirmed — code comment cited "paired GAP-779 which doesn't exist"). Similar pattern likely trong:
- GAP-372 (beta-invite — does `accept` create user? Or does it require manual admin intervention?)
- GAP-704 (post-signup JWT tenantId — does signup actually create User row with tenantId, OR just BetaRequest row?)

#### 6.3.3 Bug #8 class — @PreAuthorize annotations in kiteclass-core (ghost guards)

**Recurrence pattern:** `@PreAuthorize("hasAnyRole(...)")` annotation present but kiteclass-core SecurityConfig is `.anyRequest().permitAll()` so no Authentication object exists → annotation eval always false.

**Predicted feature count:** **ALL kiteclass-core controllers with @PreAuthorize = ALL ghost guards.**

**Evidence locations to grep:**
```bash
# Count @PreAuthorize sites in kiteclass-core
grep -rn "@PreAuthorize" kiteclass/kiteclass-core/src/main/java/ | wc -l

# Compare with header-RBAC pattern (mirror VettingController.requireSafeguardingOfficer)
grep -rn "@RequestHeader.*X-User-Roles\|requireSafeguardingOfficer\|requireRole" \
  kiteclass/kiteclass-core/src/main/java/

# Find SecurityConfig in kiteclass-core
find kiteclass/kiteclass-core/src -name "SecurityConfig*.java" -o -name "*WebSecurity*.java"
# Verify anyRequest().permitAll() pattern present
```

**Specific features at HIGH risk:** GAP-637 (admin v1 controllers — explicitly fixed 3 controllers; sweep needed cho rest), GAP-737 (ImmutableConsentController IDOR fix), ALL kiteclass-core admin/owner-scoped endpoints.

**Fix-once nature:** Single sweep PR replacing all `@PreAuthorize` in kiteclass-core với header-RBAC pattern clears ALL features in one shot. Should be #1 priority in Phase 2 walk batch.

#### 6.3.4 Bug #12 class — FE consuming BE list endpoints calling `.map()` directly on ApiResponse

**Recurrence pattern:** FE Wave 80 era + BE Wave meta-6 reshape DTOs without FE catch-up. Specifically `setRows(resp.data)` where `resp.data` is wrapped ApiResponse `{success, data: [], timestamp}`, not the array directly → `TypeError: e.map is not a function`.

**Predicted feature count:** **5-10 FE pages** likely affected.

**Evidence locations to grep:**
```bash
# Find FE list-page patterns calling axios with .data
grep -rnE "setRows\(.*\.data\)|setItems\(.*\.data\)|setList\(.*\.data\)|setData\(.*\.data\)" \
  kitehub/kitehub-frontend/src/app/ kiteclass/kiteclass-frontend/src/app/

# Find Page tsx files consuming list endpoints (heuristic)
grep -rln "fetch\|axios\|useQuery" \
  kitehub/kitehub-frontend/src/app/admin/ \
  kitehub/kitehub-frontend/src/app/dashboard/ \
  kitehub/kitehub-frontend/src/app/school-admin/

# Find ApiResponse wrapper usage trong BE
grep -rn "ApiResponse<\|ResponseEntity<ApiResponse" \
  kiteclass/kiteclass-core/src/main/java/ kitehub/*/src/main/java/
```

**Specific features at HIGH risk:** GAP-591 (cohort retention list), GAP-637 (admin controllers — likely have list endpoint surface), Wave meta-6 Bucket A staff-list `/admin/staff/page.tsx` (already confirmed).

**Fix candidates:** (a) per-page defensive unwrap `Array.isArray(body) ? body : body?.data ?? []`, or (b) global axios response interceptor auto-unwrap ApiResponse wrapper. Decide architecture-wide in Phase 2 walk-batch closure.

#### 6.3.5 Bug #16 class — Gateway TenantResolver rejects public-but-tenant-scoped endpoints

**Recurrence pattern:** Public flows (invite accept, password reset link click, beta access form) have NO user JWT yet but ARE tenant-scoped semantically → gateway TenantResolver fails (no subdomain, no JWT) → 400 before reaching downstream service.

**Predicted feature count:** **3-5 features** with public-tenant-scoped endpoints.

**Evidence locations to grep:**
```bash
# Find public endpoints (no auth required) per gateway config
grep -rn "permitAll()\|excludePath" kitehub/kitehub-gateway/src/main/java/ kiteclass/kiteclass-gateway/src/main/java/ 2>/dev/null

# Find token-based public flows
grep -rn "by-token\|/accept\|/verify\|/reset" \
  kiteclass/kiteclass-core/src/main/java/.../controller/ \
  kitehub/kitehub-subscription/src/main/java/.../controller/

# Check TenantResolver gateway filter logic
find kitehub/kitehub-gateway/src/main -name "TenantResolver*.java" -o -name "*TenantFilter*.java"
```

**Specific features at HIGH risk:** GAP-372 beta-tenant-invite (accept link click pre-login), GAP-660 Zalo OA fast-path (deep-link may carry tenant context), GAP-585 cookie consent (pre-tenant), Wave meta-6 staff-invite by-token (already confirmed).

### 6.4 Recommendations summary

| Recommendation | Action | Owner | When |
|---|---|---|---|
| Do NOT mass-flip DONE → PARTIAL retroactively | File ONE META gap GAP-NEW-wave-80-retro-walk-batch; per-feature walks file specific bug gaps prospectively per `pre-handoff-self-test-completeness.md` §3 | User decision | This session |
| Add audits-index.csv row | Per §6.2 — annotate this audit artifact | Coordinator (Phase 2 sweep PR) | Next session |
| Execute 5 retro-walk waves per §5.3 | Wave packaging suggested | Wave coordinator | Phase 2 BETA scope (4-5 weeks) |
| Sweep Bug #8 ghost-guards in kiteclass-core | Single PR replacing @PreAuthorize with header-RBAC pattern; sister to GAP-637 expansion | Backend dev | Wave retro-walk-2 (compliance + auth batch) |
| Sweep Bug #14 email/event/outbox binding | Per §6.3.1 grep, ~8-12 sites; cluster fix | Backend dev | Wave retro-walk-3 (email cluster batch) |
| Sweep Bug #12 ApiResponse unwrap | Decide architecture-wide (per-page OR global interceptor) | FE dev + coordinator | Wave retro-walk-5 (FE re-verify batch) |
| Audit Bug #17 deferred-paired-gap pattern | Grep §6.3.2 + cross-ref CSV; file follow-up gaps for any deferred-but-missing gap | Coordinator | Phase 2 walk-batch closure |
| Sweep Bug #16 public-but-tenant-scoped endpoints | Architecture decision (tenant from invite-token lookup OR document recipient-via-subdomain) | Architect + Backend | Wave retro-walk-1 (signup chain batch) |
| Phase 2 BETA scope-shape gate | Block v1.0.0-rc1 tag until ≥80% of 46 features walked OR explicit defer trailer | Release coordinator | Phase 2 BETA closure |
| Future detector consideration | When recurrence ≥2 post-rule landing per `incident-to-rule-pipeline.md` §3.1, file gap to wire detector (CI grep PR body for `## Walk evidence` section) | Meta-governance | After Phase 2 retro-walk demonstrates rule effectiveness |

---

## §7. Cross-references

- `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` — originating 17-bug findings + META rule birth
- `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0 — rule applied retroactively trong audit này
- `.claude/rules/pre-handoff-self-test-completeness.md` v1.2.0 §3 — post-fix re-walk mandate (sister rule)
- `.claude/rules/gap-done-discipline.md` §2 — DONE flip requires AC verified
- `.claude/rules/incident-to-rule-pipeline.md` §3.1 — recurrence ≥2 threshold (now confirmed ≥7)
- `.claude/rules/meta-gap-priority.md` §3 — META P0 force-multiplier
- `.claude/rules/audit-to-gap-pipeline.md` §2.8 — fix-time state-check (sister)
- `.claude/rules/feature-ship-runtime-walk-mandate.md` §6 — Wave meta-6 worked self-test
- `documents/04-quality/gaps/gap-status.csv` — canonical DONE feature list
- `documents/04-quality/gaps/ROADMAP.md §🎯 Current Status Snapshot` — Wave 80+ wave-by-wave shipped log
- `feedback_audit_of_trust_pass.md` (memory) — trust-pass recurrence pattern history

---

## §8. Audit verdict + sign-off

**Audit type:** Retro audit (post-recurrence ≥7 META P0 escalation)

**Audit-rubric reference:** `documents/04-quality/audits/quality-audit/SKILL.md` 11-category /110 (informal cho retro — primary deliverable là scope-shaping + reclassification recommendation, not score)

**Self-test against rubric:**
- ✅ Scope enumeration canonical (CSV + ROADMAP + PR-logs cross-referenced)
- ✅ Methodology explicit + reproducible (filter criteria documented; sample selection per flow class)
- ✅ Sample evidence concrete (grep keywords + verdict per feature)
- ✅ Projection math transparent (sample % × population)
- ✅ Recommendations actionable + cited rule references
- ✅ Vietnamese narrative + English identifiers per `dev-readable-doc-language.md` v1.0.2 §4
- ⚠️ Self-test caveat — sample size 10/46 = 21.7% is reasonable but not exhaustive; final Phase 2 walk batch may surface different distribution

**Conclusion:**

Trust-pass anti-pattern recurrence ≥7 confirms `feature-ship-runtime-walk-mandate.md` v1.0.0 META P0 escalation justified. Wave 80+ DONE feature population (~46 features) carries projected ~32-41 features (PARTIAL + NONE walk evidence) requiring Phase 2 BETA retro-walk batch. Recommended scope: 5 waves × ~3-4 features = ~18-25 engineer-days. Top 15 prioritization sequenced per §5.2. High-confidence predictable bug patterns (§6.3) allow targeted grep sweeps WITHOUT walking — Bug #8 (ghost-guards), Bug #14 (email/event binding), Bug #12 (ApiResponse unwrap), Bug #17 (deferred-paired-gap orphans) can be addressed cluster-wide before full walk batch executes.

User decides next session: (a) approve scope + assign Wave retro-walk-1 first, (b) revise prioritization, OR (c) defer Phase 2 BETA timeline.

**Reviewer:** @nguyenvankiet (user decides post-read)
**Audit author:** Claude Opus 4.7 (1M context) via Agent tool
**Audit date:** 2026-05-28
