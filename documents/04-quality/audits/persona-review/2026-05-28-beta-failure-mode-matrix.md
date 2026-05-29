---
audience: dev
date: 2026-05-28
session-theme: Plan D Hybrid close-loop beta failure-mode projection — bug class × user flow matrix
audit-type: persona-review/failure-mode-matrix
priority: P0 (Plan D scope-refinement prerequisite)
input-sources:
  - documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md (17 bugs baseline)
  - documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md (46 features retro)
  - documents/04-quality/gaps/phase-1-beta/GAP-786-staff-invite-accept-user-provision-missing.md
  - documents/04-quality/gaps/phase-1-beta/GAP-787-staff-invite-email-send-never-implemented.md
bug-classes: 13 (8 from Walk 1 + 5 AWS-deploy predictable)
critical-flows-enumerated: 15
top-failure-prone-flows: 5
cumulative-hard-blocker-flows: 9
estimated-fix-effort-eng-days: 28-42
aws-deploy-additional-bugs-projection: 15-25
plan-d-realistic-scope-post-refinement: 3-4 flows (vs 15 enumerated)
related-rule: .claude/rules/feature-ship-runtime-walk-mandate.md v1.0.0
---

# Plan D Hybrid Close-Loop Beta — Failure-Mode Matrix

## Tóm lược điều hành (TL;DR)

Plan D đề xuất hybrid close-loop beta 1-3 friend trong ~3 tuần. Failure-mode projection dựa trên:
- **Walk 1 baseline** (Wave meta-6 Bucket A staff invite — 17 bugs trong 1 shipped-DONE feature)
- **Retro audit** (46 Wave 80+ features — 50% NONE walk evidence + 30% PARTIAL)
- **13 bug classes** (8 từ Walk 1 + 5 AWS-deploy predictable)
- **15 critical user flows** enumerated cho Owner persona

**Projection top-line:**
- **9/15 flows = HARD BETA-BLOCKER** (cumulative ~80% probability ít nhất 1 P0 bug fire on Plan D launch)
- **Top-5 most-failure-prone flows total risk score ≥7 HIGH ratings/flow**
- **Cumulative fix effort: 28-42 engineer-days** (vs Plan D 10-ngày fix sprint estimate → 2.8-4.2x over)
- **Plan D 10-day timeline NOT REALISTIC** unless scope shrink từ 15 flows xuống 3-4 flows
- **AWS deploy adds 15-25 additional bugs** (Classes I-M production-only) → +5-7 ngày deploy debug
- **Realistic Plan D refined scope:** 3 flows (signup + login + view dashboard) sau khi defer invite/payment/wizard/CRUD/branding/staff-mgmt sang Phase 3

User decision required: shrink Plan D scope OR extend timeline 5-6 tuần OR accept HARD blocker risk + rollback contingency.

---

## §1. Bug class catalog (13 classes)

### 1.1 Wave meta-6 Bucket A — 8 confirmed bug classes (Walk 1 baseline)

| Class | Name | Severity profile | Walk 1 bugs | Characterization |
|---|---|---|---|---|
| **A** | Auth/role mismatches | P0 dominant | #8, #13, #16 | `@PreAuthorize` ghost-guards (SecurityConfig `.anyRequest().permitAll()` makes annotation ineffective); UserContext `Long` vs gateway forwards UUID string → null; TenantResolver rejects public-but-tenant-scoped endpoints |
| **B** | FE-BE contract drift | P1 dominant | #7, #12, #15 | FE Wave 80 era + BE Wave meta-6 reshape DTOs/endpoints WITHOUT FE catch-up; ApiResponse wrapper unwrap missing → `e.map is not a function`; by-token preview endpoint missing in BE despite FE consume |
| **C** | Test fixture incompleteness | P2 (dev only) | #9 | Seed fixture có user nhưng thiếu tenant link → mọi Owner walk session fail without manual SQL hack |
| **D** | Reactor/async architecture | P0 dominant | #10 | `.block()` in webflux reactor parallel thread → `IllegalStateException` → circuit breaker open → 503 cascading |
| **E** | UX/navigation completeness | P1 | #11 | Pages shipped nhưng FE wiring incomplete — không có nav link/sidebar entry → user phải manually type URL |
| **F** | Critical feature gaps (incomplete MVP) | **P0 CRITICAL** | #14, #17 | Service saves DB row but ZERO outbox/event/email path — feature non-functional in production; accept marks ACCEPTED but no user provisioning |
| **G** | Architecture/security (peripheral) | P2-P3 | (CSP, 404) | CSP report-only blocks dev paths; Footer link 404 |
| **H** | Dev-stack provisioning | P2 (dev) | #6 (RabbitMQ) | Queue auto-declare missing → consumer fails to bind |

### 1.2 AWS-deploy predictable — 5 additional classes (production-only surface)

| Class | Name | Severity profile | Likely sites | Source of confidence |
|---|---|---|---|---|
| **I** | CSP / CORS / DNS | P0 dominant | FE deploy domain mismatch, CORS rule incomplete cho prod, DNS A/CNAME record wrong | Wave meta-6 already surfaced CSP report-only on dev (Bug "CSP" peripheral); prod = strict mode no longer report-only |
| **J** | Secrets manager binding / env var coverage | P0 dominant | New env var local compose KHÔNG có terraform IaC (GAP-717 pattern Wave 81 + 104.5 recurrence); fetch-secrets.sh không có line cho new secret | `.claude/rules/local-fix-production-parity-check.md` v1.0.0 codified exactly this class; sister rule born from Wave 81+104.5 same incident |
| **K** | SSL/TLS cert | P0 if missing, P1 if mis-config | ACM cert binding mismatch (Wave 64-65 Phase 2.3 had this class), self-signed leakage | Pre-mutation-state-check rule v1.2.0 §1.5 IAM cross-reference matrix mandate — implies sites missed pre-rule |
| **L** | SES email DKIM verification | P0 cho email-dependent flows | Domain identity not verified, DKIM record missing, SPF record drift, list-unsubscribe header missing (GAP-703 Wave 105 recurrence) | GAP-703 Wave 105 already shipped this class; recurrence likely for new email senders |
| **M** | Reactor blocking on production load | P0-P1 if missed | Same `.block()` pattern Bug #10 (dev) but compounded by production load: thread pool exhaustion, p99 latency spike, cascade failure | Class D explicitly notes "Audit ALL synchronous controllers in webflux gateway. Likely 5-10 more sites with same pattern" — only 1 site walked Bug #10 |

**Total: 13 bug classes.** Walk 1 baseline + AWS-deploy predictable = comprehensive coverage cho Plan D close-loop projection.

---

## §2. Critical user flow catalog (15 flows)

Plan D scope = Owner-priority focus (per user direction). Beta-relevant flows enumerated:

### 2.1 Auth flows (3)

| # | Flow | Persona | Entry → Exit |
|---|---|---|---|
| 1 | **Signup → email verify → first login** | Anonymous → P2 Owner | Landing `/` → Request Beta Access form → admin approve → verify email → set password → first login → /dashboard |
| 2 | **Login → dashboard** (recurring) | All personas | `/login` → submit credentials → JWT issue → role-route to `/dashboard` |
| 3 | **Password reset** | Any persona | `/login` → "Quên mật khẩu" → email magic link → reset form → re-login |

### 2.2 Onboarding (3)

| # | Flow | Persona | Entry → Exit |
|---|---|---|---|
| 4 | **Owner onboarding wizard** (first-time) | P2 Owner | First login → wizard step 1-7 (skippable) → save progress → complete |
| 5 | **Owner adds first class** | P2 Owner | Dashboard → "Tạo lớp" → form (tên + môn + giờ + giáo viên) → submit → list refresh |
| 6 | **Owner enrolls first student** | P2 Owner | Class detail → "Thêm học sinh" → form → save → enrollment row |

### 2.3 Invite flows (3)

| # | Flow | Persona | Entry → Exit |
|---|---|---|---|
| 7 | **Owner invites staff** (TEACHER/STAFF/MANAGER) | P2 Owner → Staff | Dashboard → `/admin/staff` → "Mời" → form → BE save row → email send → staff receive email → click link → set password → first login |
| 8 | **Owner invites parent** | P2 Owner → Parent | (Wave 80 era — similar pattern) |
| 9 | **Staff/parent accepts invitation** | Staff/Parent | Email link → set password page → submit → user provisioned → first login |

### 2.4 Daily ops (4)

| # | Flow | Persona | Entry → Exit |
|---|---|---|---|
| 10 | **Owner views revenue dashboard** | P2 Owner | Dashboard → "Doanh thu" → KPI cards + chart |
| 11 | **Owner takes attendance** | P2 Owner / Teacher | Class detail → "Điểm danh" → toggle per student → save |
| 12 | **Owner generates invoice** | P2 Owner | "Hóa đơn" → select tháng + lớp → generate → preview → email/print |
| 13 | **Parent pays invoice** | Parent | Email link → invoice page → bank transfer/VietQR/Momo → confirmation |

### 2.5 Settings (2)

| # | Flow | Persona | Entry → Exit |
|---|---|---|---|
| 14 | **Owner updates branding** (AI Branding wizard) | P2 Owner | Settings → Branding → upload logo / pick color / preview → save |
| 15 | **Owner manages staff list** | P2 Owner | `/admin/staff` → list → edit/delete row |

**Total: 15 flows.** Owner persona covers 13/15; Anonymous + Parent + Staff covers 2.

---

## §3. Failure-mode matrix

Probability cell convention:
- **HIGH** = ≥70% probability bug class fires on flow within first 3 walks
- **MEDIUM** = 30-69% probability
- **LOW** = 5-29% probability
- **N/A** = bug class doesn't apply (e.g., Class L SES DKIM applied to a flow with no email send)

Risk score = count of HIGH ratings per row.

| # | Flow | A | B | C | D | E | F | G | H | I | J | K | L | M | HIGH count |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Signup → verify → first login | HIGH | HIGH | MED | LOW | MED | **HIGH** | LOW | LOW | HIGH | **HIGH** | HIGH | **HIGH** | MED | **8** |
| 2 | Login → dashboard | HIGH | HIGH | LOW | LOW | HIGH | LOW | LOW | LOW | MED | HIGH | HIGH | N/A | MED | **5** |
| 3 | Password reset | HIGH | MED | LOW | LOW | MED | **HIGH** | LOW | LOW | MED | **HIGH** | HIGH | **HIGH** | LOW | **5** |
| 4 | Owner onboarding wizard | HIGH | HIGH | MED | LOW | HIGH | MED | LOW | LOW | LOW | MED | LOW | LOW | LOW | **3** |
| 5 | Owner adds first class | HIGH | HIGH | MED | LOW | MED | LOW | LOW | LOW | LOW | MED | LOW | N/A | LOW | **2** |
| 6 | Owner enrolls first student | HIGH | HIGH | MED | LOW | MED | LOW | LOW | LOW | LOW | LOW | LOW | N/A | LOW | **2** |
| 7 | **Owner invites staff** (Walk 1) | **HIGH** | **HIGH** | HIGH | HIGH | **HIGH** | **HIGH** | LOW | MED | HIGH | HIGH | LOW | **HIGH** | MED | **9** |
| 8 | Owner invites parent | HIGH | HIGH | MED | LOW | HIGH | **HIGH** | LOW | LOW | HIGH | **HIGH** | LOW | **HIGH** | LOW | **7** |
| 9 | Staff/parent accepts invitation | HIGH | HIGH | MED | LOW | HIGH | **HIGH** | LOW | LOW | MED | MED | LOW | LOW | LOW | **4** |
| 10 | Owner views revenue dashboard | HIGH | **HIGH** | MED | MED | HIGH | LOW | LOW | LOW | LOW | LOW | LOW | N/A | MED | **3** |
| 11 | Owner takes attendance | HIGH | HIGH | MED | LOW | HIGH | LOW | LOW | LOW | LOW | LOW | LOW | N/A | LOW | **3** |
| 12 | Owner generates invoice | HIGH | HIGH | MED | LOW | HIGH | **HIGH** | LOW | LOW | LOW | MED | LOW | **HIGH** | LOW | **5** |
| 13 | Parent pays invoice | MED | HIGH | LOW | LOW | HIGH | **HIGH** | MED | LOW | HIGH | HIGH | HIGH | **HIGH** | LOW | **7** |
| 14 | Owner updates branding | HIGH | HIGH | MED | LOW | HIGH | LOW | LOW | LOW | MED | MED | LOW | N/A | LOW | **3** |
| 15 | Owner manages staff list | HIGH | **HIGH** | MED | LOW | HIGH | LOW | LOW | LOW | LOW | LOW | LOW | N/A | LOW | **3** |

### 3.1 HIGH-rating justifications (key cells)

**Class A (Auth/role) HIGH on Flows 1-15 except Flow 13 (Parent payment public flow):**
- Wave meta-6 retro audit §6.3.3 explicitly states: "ALL kiteclass-core controllers with @PreAuthorize = ALL ghost guards." Owner-scoped pages dùng kiteclass-core security context → guaranteed fail at minimum 1 site per flow.
- GAP-637 (Wave 92 admin controllers) fixed only 3 controllers; sweep pending.

**Class B (FE-BE contract drift) HIGH on Flows 1-15 except minor variants:**
- Wave meta-6 retro §6.3.4 estimates "5-10 FE pages" affected by ApiResponse unwrap bug. With 15 Owner-facing pages = ~50% probability per page; aggregate cross-flow ≥70%.

**Class D (Reactor) HIGH on Flow 7 (already confirmed Walk 1):**
- Class D MED elsewhere (gateway routes most flows hit, ≥1 reactor site per flow possible but not guaranteed).

**Class F (Feature gaps) HIGH on Flows 1, 3, 7, 8, 12, 13:**
- Flow 1 signup: email verify path may have Bug #14 pattern (DB row saved without email send) — DIRECT recurrence Wave meta-6 + GAP-702
- Flow 3 password reset: email send path likely affected
- Flow 7 staff invite: CONFIRMED via Wave meta-6 walk
- Flow 8 parent invite: pattern recurrence likely
- Flow 12 invoice generate: invoice email send may have same path missing
- Flow 13 parent pays: invoice email link click path

**Class I (CSP/CORS/DNS) HIGH on Flows 1, 7, 13:**
- Flow 1 signup involves landing page first-touch → CSP strict (production) most likely breaks
- Flow 7 staff invite email link → must resolve subdomain DNS (already Bug #16 on dev with TenantResolver — production-equivalent issue)
- Flow 13 parent pays → external redirect to bank/VietQR/Momo → CORS rule must allow

**Class J (Secrets/env) HIGH on Flows 1, 2, 3, 7, 8, 13:**
- All flows requiring JWT secret, email service URL, payment processor key
- GAP-717 pattern Wave 81+104.5 = 2x recurrence already; high probability 3rd

**Class K (SSL/TLS) HIGH on Flows 1, 2, 3, 13:**
- Initial signup + login + payment redirect = highest-stakes TLS surface

**Class L (SES DKIM) HIGH on Flows 1, 3, 7, 8, 12, 13:**
- All email-send dependent flows. GAP-703 Wave 105 already shipped this class for one sender domain; new senders likely repeat.

**Class M (Reactor on production load) MED on Flows 1-3, 10:**
- Production load adds compound risk; not isolated per flow.

---

## §4. Top-5 most-failure-prone flows for Plan D

Sort by HIGH count desc:

| Rank | Flow # | Flow | HIGH count | Likely bugs (specific) | Fix effort (eng-days) | Beta-blocker severity |
|---|---|---|---|---|---|---|
| 1 | **7** | Owner invites staff | **9** | All Walk 1 bugs (#7, #8, #10, #11, #12, #13, #14, #15, #16, #17) + Class I CSP + Class J secrets + Class L DKIM | 5-7 (per Wave meta-6 retro estimate) | **HARD BLOCKER** — flow is core beta scope; cannot defer if Plan D wants any staff/manager testing |
| 2 | **1** | Signup → verify → first login | **8** | Class A (role), Class B (ApiResponse unwrap signup confirm), Class F (verify email never sent — GAP-702 sister pattern), Class I (CSP strict), Class J (JWT_CHALLENGE_SECRET — Wave 104.5+105 recurrence), Class K (SSL cert binding), Class L (SES DKIM cho verify email) | 3-5 | **HARD BLOCKER** — every beta friend MUST signup; flow 0 of all flows |
| 3 | **8** | Owner invites parent | **7** | Same as Flow 7 but lower confidence (no walk done yet) | 3-4 (after Flow 7 fixes; shared Bug #14 cluster fix applies) | **SOFT BLOCKER** — defer Phase 3 acceptable; not needed for Owner-only close-loop |
| 4 | **13** | Parent pays invoice | **7** | Class B (payment gateway response), Class E (no payment success UI), Class F (idempotency may not exist — see `pre-handoff-self-test-completeness.md` §2.6 payment checklist), Class I (CORS to gateway), Class J (gateway API key), Class K (TLS), Class L (invoice email send) | 8-12 (payment integration complex; spans 3-4 services) | **HARD BLOCKER if** invoice testing in Plan D scope; **SOFT BLOCKER if** invoice deferred Phase 3 |
| 5 | **3** | Password reset | **5** | Class A (role bypass), Class F (reset email may not send — Class F recurrence), Class J (reset token secret), Class L (SES DKIM cho reset email) | 2-3 | **HARD BLOCKER** — beta friend phải có cách recover password; cannot defer |

### 4.1 Justifications for severity classification

**HARD = block beta launch (cannot ship without fix):**
- Flow 1 signup: entry point — 0 beta friend can join without it
- Flow 3 password reset: support burden + friend friction — must work day 1
- Flow 7 staff invite: if invite tested = MUST work; if invite NOT tested = defer (downgrade SOFT)
- Flow 13 parent pays: if invoice tested = MUST work; defer Phase 3 = downgrade SOFT

**SOFT = workaround possible (manual SOP or feature defer):**
- Flow 8 parent invite: can defer Phase 3 (Phase 1 BETA = Owner-only)
- Flow 7 if scope excludes staff testing: defer to Phase 3

---

## §5. Cumulative beta-blocker estimate

### 5.1 HARD blocker flows (must-fix-before-launch)

Conservative interpretation Plan D close-loop = signup + login + dashboard + 1-2 daily ops:

| Flow | HARD/SOFT (conservative scope) | Fix effort (eng-days) |
|---|---|---|
| 1 Signup → verify → first login | HARD | 3-5 |
| 2 Login → dashboard | HARD | 2-3 |
| 3 Password reset | HARD | 2-3 |
| 4 Onboarding wizard | HARD | 2-3 |
| 5 Owner adds first class | HARD | 2-3 |
| 6 Owner enrolls first student | HARD | 2-3 |
| 10 Owner views revenue dashboard | HARD | 2-3 |
| 11 Owner takes attendance | HARD | 2-3 |
| 15 Owner manages staff list | HARD (paired Flow 7 deferred) | 1-2 |
| (Flows 7, 8, 9, 12, 13, 14 deferred Phase 3) | SOFT (defer) | 0 (defer) |

**Total HARD blocker = 9 flows × ~3 ngày avg = 27 eng-days for fix sprint alone.**

### 5.2 Bug count cross-flow (avoiding double-count)

Wave meta-6 retro §4.2 estimate: ~100-160 bugs surface-able across 32 features; ~10-30 P0/P1 high-severity.

Applied to 9 HARD blocker flows in scope:
- 9 flows × ~3-5 bugs/flow avg = 27-45 bugs surface
- ~20% P0 critical = **6-9 P0 bugs blocking Plan D launch**
- ~30% P1 = 8-13 P1 bugs (workaround possible but degraded UX)

### 5.3 Engineer-day estimate breakdown

| Phase | Activity | Effort (eng-days) |
|---|---|---|
| Walk preparation | Stack-up runbook + persona credential seed + smoke automation | 1-2 |
| 9-flow walk execution | 9 flows × ~0.5-1 ngày walk + retro | 6-9 |
| P0/P1 fix sprint | 6-9 P0 + 8-13 P1 bugs × ~0.3-0.7 ngày fix avg | 6-12 |
| Re-walk verification (per `pre-handoff-self-test-completeness.md` §3) | 9 flows × 0.3 ngày re-walk after fix | 3 |
| Cross-cutting sweeps (Class A ghost-guards, Class B ApiResponse interceptor, Class F email/event) | 3 sweeps × 2-3 ngày each | 6-9 |
| Bug discovery contingency (unknown unknowns) | 25% buffer | 6-9 |
| **Total Plan D HARD blocker scope** | — | **28-44 eng-days** |

### 5.4 Plan D 10-day estimate reality check

**Plan D estimate: 10 ngày fix sprint** vs **realistic: 28-44 eng-days = 2.8-4.4x over estimate.**

Even with parallel agent assist (5x speedup theory per `wave-pack-planner` SKILL):
- 28-44 eng-days ÷ 5 = **5.6-8.8 calendar days** at maximum parallelism
- BUT: many bugs are sequential (Flow 1 must fix before Flow 4 can walk; Flow 2 dashboard must fix before Flow 10-11 can walk)
- Realistic calendar days with mix sequential + parallel: **2-3 calendar weeks**

→ **Plan D 3-week timeline tight but feasible IF parallel agent execution + scope refinement.**

---

## §6. Plan D refinement — flow-by-flow recommendation

### 6.1 Include / defer matrix

| Flow | Plan D Include? | Rationale | Mitigation if include |
|---|---|---|---|
| 1 Signup → verify → first login | ✅ YES | Cannot run beta without — entry flow | Walk + fix Bug #14 email path Wave-equivalent; verify SES DKIM prod; CSP strict mode test |
| 2 Login → dashboard | ✅ YES | Recurring; cannot run beta without | Walk; sweep Class A ghost-guards on dashboard role-routes |
| 3 Password reset | ✅ YES | Friend support burden | Walk; verify reset email send (Class F + L) |
| 4 Onboarding wizard | ⚠️ CONDITIONAL | If Phase 1 BETA = "skipped onboarding OK" → defer | Communicate "wizard may have bugs; skip to dashboard" if include |
| 5 Owner adds first class | ✅ YES | Friend wants to "do something" after login | Walk; fix Bug #8 ghost-guards on POST /classes |
| 6 Owner enrolls first student | ✅ YES | Continuation of Flow 5 | Walk paired with Flow 5 |
| **7 Owner invites staff** | ❌ DEFER Phase 3 | 9-HIGH risk score; full Walk 1 bug set; ~5-7 eng-days alone | Communicate clearly to friends: "staff invite not yet available" |
| 8 Owner invites parent | ❌ DEFER Phase 3 | Same as Flow 7 + Phase 1 BETA = Owner-only persona | Skip |
| 9 Staff/parent accepts invitation | ❌ DEFER Phase 3 | Dependency on Flow 7/8 | Skip |
| 10 Owner views revenue dashboard | ⚠️ READ-ONLY OK | If empty state acceptable | Walk; fix Class B ApiResponse unwrap on KPI fetch |
| 11 Owner takes attendance | ⚠️ CONDITIONAL | If class created (Flow 5) | Walk; fix Class A role-guard on POST /attendance |
| 12 Owner generates invoice | ❌ DEFER Phase 3 | Invoice + email chain too complex Plan D scope | Communicate: "invoice feature beta later" |
| 13 Parent pays invoice | ❌ DEFER Phase 3 | Payment integration not in scope Plan D | Skip |
| 14 Owner updates branding | ❌ DEFER Phase 3 | AI Branding cluster (Wave 2-4) own quality risk | Communicate: "branding cosmetic later" |
| 15 Owner manages staff list | ⚠️ READ-ONLY OK | If list display works without invite/edit | Walk; fix Class B ApiResponse unwrap (Wave meta-6 Bug #12 confirmed site) |

### 6.2 Realistic Plan D refined scope

**Realistic close-loop beta (Phase 1 BETA Owner only, ~3 tuần):**

| Tier | Flows | Count | Effort estimate |
|---|---|---|---|
| **MUST work** | 1 Signup + 2 Login + 3 Password reset | 3 | 7-11 eng-days |
| **SHOULD work** | 4 Onboarding (optional skip) + 5 Add class + 6 Enroll student | 3 | 6-9 eng-days |
| **NICE to work** | 10 Revenue dashboard (read-only) + 11 Attendance + 15 Staff list (read-only) | 3 | 5-9 eng-days |
| **DEFER Phase 3** | 7 Staff invite + 8 Parent invite + 9 Accept invitation + 12 Invoice + 13 Payment + 14 Branding | 6 | (skip) |

**Total realistic Plan D effort: 18-29 eng-days.** Still over Plan D 10-day estimate but achievable in 3 tuần với parallel execution + scope discipline.

### 6.3 Communication plan with beta friends

When inviting 1-3 friends to close-loop beta, MUST clearly communicate:

```
Tính năng có sẵn để test:
✅ Đăng ký tài khoản + xác thực email + đăng nhập
✅ Tạo lớp học đầu tiên
✅ Thêm học sinh
✅ Xem điểm danh + dashboard doanh thu (read-only)

Tính năng CHƯA có (defer Phase 3):
❌ Mời nhân viên / giáo viên / quản lý
❌ Mời phụ huynh nhận thông tin
❌ Tạo + gửi hóa đơn cho phụ huynh
❌ Thanh toán điện tử
❌ Customize logo + màu sắc trung tâm

Phản hồi mong muốn:
- Bug report (đăng ký fail, login lỗi, dashboard không hiện số)
- UX feedback (chỗ nào confusing, button nào không tìm thấy)
- Performance feedback (chậm chỗ nào)
```

Honest scope communication = critical to maintain friend trust. Trying to ship Flow 7-14 broken = friend frustration + retro burden.

---

## §7. AWS deploy projection

### 7.1 Per-class bug count estimate

| Class | Description | Estimated bugs surface during deploy | Confidence basis |
|---|---|---|---|
| **I** | CSP / CORS / DNS | **3-5** | CSP strict on prod vs report-only on dev (Wave meta-6 surfaced 1 dev-side; prod strict will surface more); DNS subdomain routing different from localhost; CORS rules need explicit prod origin allowlist |
| **J** | Secrets manager binding / env var coverage | **4-7** | GAP-717 Wave 81+104.5 = 2x recurrence already; Plan D MVP adds JWT_CHALLENGE_SECRET sister fields; payment gateway keys; SES API key; Resend backup key; OAuth client secret cho Zalo OA — each one potentially repeats local-fix-production-parity miss pattern |
| **K** | SSL/TLS cert | **2-4** | ACM cert binding mismatch (Phase 2.3 Wave 64-65 had this); FE deploy domain vs cert SAN; HTTP→HTTPS redirect; HSTS preload (low-priority) |
| **L** | SES email DKIM verification | **3-5** | GAP-703 Wave 105 already shipped for one sender; new senders cho signup verify + password reset + invoice + admin alert each need verify + DKIM + SPF; List-Unsubscribe header GAP-703 sister pattern likely repeat |
| **M** | Reactor blocking on production load | **3-4** | Bug #10 walked 1 site; Wave meta-6 retro notes "5-10 more sites with same pattern"; production load amplifies (dev never saw thread pool exhaust) |

**Total AWS deploy additional bugs: 15-25.**

### 7.2 Deploy timeline impact

| Activity | Effort estimate |
|---|---|
| Deploy infrastructure (terraform apply Phase 2.3) | 1-2 eng-days (already partially done Wave 64-65) |
| Initial production smoke test | 1 eng-day |
| 15-25 deploy bugs × ~0.3 ngày fix avg | 5-7 eng-days |
| Re-deploy + re-smoke (2-3 cycles likely) | 2-3 eng-days |
| **Total AWS deploy phase additional** | **9-13 eng-days** |

### 7.3 Compound Plan D timeline (HARD + deploy)

- HARD blocker fix sprint: 28-44 eng-days (per §5.3)
- AWS deploy phase: 9-13 eng-days
- **Compound total: 37-57 eng-days = 7-11 calendar weeks** at solo-dev pace
- **With parallel agent + scope shrink to refined Plan D §6.2: 18-29 + 9-13 = 27-42 eng-days = 5-8 calendar weeks**

→ **Plan D 3-week target tight but achievable IF:** (a) scope refined to 3-tier per §6.2, (b) parallel agent execution applied, (c) accept beta friends see "feature not available" notice for deferred 6 flows.

---

## §8. Risk register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Plan D timeline blows past 3 weeks | HIGH | Friend trust loss + scope re-negotiate | Refine scope to §6.2 BEFORE invite; communicate honestly upfront |
| Walk session surfaces > 5 bugs per flow (worse than Wave meta-6 baseline) | MED | Fix sprint balloons | 25% buffer in §5.3 estimate; accept early-stop signal at 2 critical flows |
| AWS deploy hits 2-3 cycles | HIGH | +5-7 days delay | Pre-flight per `pre-mutation-state-check.md` v1.2.0 §1.5 IAM cross-reference matrix; deploy 1 service at a time vs whole stack |
| Beta friend hits Bug #17 user-provisioning class on signup (Flow 1) | MED | Friend cannot use product day 1 | Walk Flow 1 FIRST in retro batch; verify user record creation explicitly |
| New bug class emerges not covered by 13 (unknown unknown) | LOW-MED | Schedule slip | Walk evidence per `feature-ship-runtime-walk-mandate.md` v1.0.0 = primary catch; retro after each walk session |
| Class L SES DKIM blocks email send last-minute | MED | Cannot launch beta until resolved | Pre-walk SES smoke `aws ses send-email --to <test@test.vn>` 24h before launch |

---

## §9. Recommendations

### 9.1 Immediate (this session / next)

1. **REFINE Plan D scope** to §6.2 — 9 flows MUST/SHOULD/NICE tier; defer 6 Phase 3
2. **Pre-walk SES smoke test** 24h before launch (Class L early-catch)
3. **Pre-deploy infrastructure verify** via `pre-mutation-state-check.md` v1.2.0 §1.5 cross-reference matrix (Class J + K early-catch)
4. **Walk Flow 1 FIRST** in retro batch — entry flow, blocks all others

### 9.2 Plan D execution sequence (parallel-aware)

Wave packaging suggestion (3-week beta-prep wave):

| Wave | Scope | Calendar week |
|---|---|---|
| **Wave beta-prep-walk-1** (signup chain) | Flow 1 + Flow 2 + Flow 3 walk + fix | Week 1 |
| **Wave beta-prep-walk-2** (Owner core) | Flow 4 + Flow 5 + Flow 6 walk + fix (parallel agents) | Week 1-2 |
| **Wave beta-prep-walk-3** (Owner read-only) | Flow 10 + Flow 11 + Flow 15 walk + fix | Week 2 |
| **Wave beta-prep-deploy** (AWS deploy phase) | Deploy + 15-25 deploy bugs + smoke + DKIM | Week 2-3 |
| **Wave beta-prep-soft-launch** (1 friend day 1, scale to 3 by day 7) | Real beta test + immediate bug triage | Week 3 |

### 9.3 Block / accept decision

User to decide:
- **Option A (recommended):** Accept refined scope §6.2 → execute 5-wave plan → realistic 3-week timeline. Risk: 6 deferred flows feedback gap.
- **Option B:** Stick with original 15-flow scope → extend to 6-8 weeks → higher fix cost. Risk: friend fatigue waiting.
- **Option C:** Bypass walk batch → launch directly → accept friend bug exposure. Risk: 80% probability ≥1 P0 fires during friend session → trust loss.

### 9.4 Future scope (Phase 3+)

Six deferred flows (7, 8, 9, 12, 13, 14) belong Phase 3 sau khi:
1. Bug #14 cluster (Class F email/event/outbox) sweep cross-cutting fix (Wave retro-walk-3 per Wave 80+ retro §5.3)
2. Bug #17 user-provisioning sister fix
3. Payment integration full lifecycle (gateway + idempotency + webhook + reconciliation per `pre-handoff-self-test-completeness.md` v1.2.0 §2.6)
4. AI Branding cluster post-quality-gate (GAP-225 cluster Phase 2-4 future scope)

---

## §10. Cross-references

- `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` — 17-bug baseline, originating trust-pass recurrence #7
- `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md` — 46-feature retro, 50%/30%/20% verdict distribution
- `documents/04-quality/gaps/phase-1-beta/GAP-786-staff-invite-accept-user-provision-missing.md` — Bug #17 catalog
- `documents/04-quality/gaps/phase-1-beta/GAP-787-staff-invite-email-send-never-implemented.md` — Bug #14 catalog
- `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0 — applied retroactively to Plan D
- `.claude/rules/pre-handoff-self-test-completeness.md` v1.2.0 §2-§3 — per-flow checklist (auth/wizard/file-upload/payment/multi-tenant/SSE/async/time/i18n) + post-fix re-walk mandate
- `.claude/rules/local-fix-production-parity-check.md` v1.0.0 — Class J source
- `.claude/rules/pre-mutation-state-check.md` v1.2.0 §1.5 — Class K + IAM cross-reference matrix
- `.claude/rules/release-fix-retry-budget.md` v1.2.0 §3.5 — investigation phase mandate (apply to deploy retry cycles)
- `.claude/rules/release-deploy-standard.md` v1.2.0 §3.1 — smoke admin-login mandatory post-deploy
- `.claude/rules/feedback_outside_in_recurring_miss.md` (memory) — pattern: outside-in trigger for new scope misses bucket-internal refinement

---

## §11. Audit verdict + sign-off

**Audit type:** Persona-review / Failure-mode matrix (pure projection, no walk execution)

**Methodology:** Bug class × flow probability matrix grounded in:
- Wave meta-6 Bucket A 17-bug concrete baseline (8 classes empirically confirmed)
- Wave 80+ retro audit projections (50% NONE walk evidence → 23 features carrying invisible bugs)
- AWS-deploy predictable classes (sister-rule documented = high-confidence prediction)
- Owner-persona priority focus per Plan D scope

**Self-test against rubric:**
- ✅ Bug class catalog complete (13 classes, 8 confirmed + 5 predicted)
- ✅ Flow catalog explicit (15 enumerated, Owner-priority)
- ✅ Matrix dimensions concrete (13 × 15 = 195 cells, all populated)
- ✅ HIGH-rating justifications cited with specific evidence
- ✅ Fix effort projection transparent (per-flow + cumulative + buffer)
- ✅ Refinement recommendations actionable
- ✅ Vietnamese narrative + English identifiers per `dev-readable-doc-language.md` v1.0.2
- ⚠️ Caveat — projection accuracy depends on Wave meta-6 17-bug baseline being representative; close-loop beta walk may surface higher OR lower bug counts

**Conclusion:**

Plan D Hybrid close-loop beta projection = **HIGH risk if launched at full 15-flow scope within 10-day estimate.** Top-5 most-failure-prone flows total 9-7-7-5-5 HIGH ratings, with Flow 7 (staff invite) being direct recurrence of Wave meta-6 originating bug class.

Cumulative HARD blocker estimate **28-44 engineer-days** (4.4x over Plan D 10-day estimate). AWS deploy adds **9-13 additional eng-days** (15-25 bugs surface — Classes I-M production-only).

**Refined Plan D recommendation:** 3-tier scope (3 MUST + 3 SHOULD + 3 NICE = 9 flows; defer 6 Phase 3) → realistic 18-29 eng-days = 3-4 calendar weeks với parallel agent execution. Communicate clearly to beta friends about deferred features upfront.

**Reviewer:** @nguyenvankiet (user decides post-read — Option A refined / Option B extended / Option C accept-risk)
**Audit author:** Claude Opus 4.7 (1M context) via Agent tool
**Audit date:** 2026-05-28
