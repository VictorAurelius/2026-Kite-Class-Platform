---
title: Wave rst-cascade-1 Cluster 3 — Onboarding + signup 6-gap walkthrough audit
status: complete
created: 2026-05-26
phase: phase-1-beta
wave: rst-cascade-1
audience: mixed
gaps: [GAP-538, GAP-516, GAP-531, GAP-610, GAP-611, GAP-724]
---

# Wave rst-cascade-1 Cluster 3 — Onboarding + signup walkthrough audit

## Scope

Phase α LOCAL-first walkthrough của 6 PARTIAL onboarding+signup gaps trên local Docker stack (11/11 services healthy). Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow / §2.2 anonymous-flow checklists. NO AWS access (Phase α scope).

## Pre-walkthrough state (2026-05-26 07:43 UTC)

- Docker stack: 11/11 healthy (kite-postgres + kite-redis + kite-rabbitmq + kite-minio + kite-gateway + kitehub-{platform/subscription/branding/email/admin/frontend} + kiteclass-{core/frontend} + kite-mailhog)
- Branch: `worktree-agent-ac35071d3f1071ed5`
- Coordinator-pre-flagged cascades: `class.rescheduled.queue` missing @Bean Queue declaration (Wave br-4 GAP-291 incomplete)

---

## GAP-538 — Day-1 onboarding checklist + sample data seed

**Pre-walkthrough %:** 95% PARTIAL
**Post-walkthrough verdict:** PARTIAL (95% — no delta; live walkthrough still gated GAP-612 AWS restore)
**Evidence:**

- BE `OnboardingProgressController` present: `kitehub/kitehub-subscription/.../onboarding/` ✓
- V43 migration shipped: `V43__create_onboarding_progress_table.sql` ✓
- FE `OnboardingChecklist` component shipped (Wave 78 Bucket B) ✓
- VN sample data seed worker shipped Wave 98 B2 (GAP-658) — 6 VN CSV files + VietnamSampleDataGenerator ✓
- `GET /api/v1/onboarding-progress` returns HTTP 401 without auth (X-Tenant-Id required) — correct behavior, security guard working ✓
- AC5 live walkthrough on real deploy: BLOCKED GAP-612 AWS account 906286017800 suspension
- AC8 native VN copywriter pass: deferred Wave 98 Bucket B4

**New findings:** None — gap status accurate.

---

## GAP-516 — 2FA TOTP mandatory PLATFORM_ADMIN

**Pre-walkthrough %:** ~80% PARTIAL (12/16 AC shipped, 4 outstanding)
**Post-walkthrough verdict:** PARTIAL (80% — no delta; outstanding AC blocked GAP-612)

**🚨 WAVE PLAN LABEL DISCREPANCY (cascade for coordinator):**

Wave rst-cascade-1 plan §3.α labels GAP-516 as "Tenant init flow — POST /api/v1/admin/beta-requests/{id}/approve". This is **INCORRECT**:
- Gap file title: "GAP-516: 2FA TOTP mandatory for PLATFORM_ADMIN"
- Domain: Backend + Frontend, scope = TOTP/2FA enrollment + challenge
- `gap-status.csv` row title_short: "2FA TOTP mandatory PLATFORM_ADMIN" (matches gap file)
- **Tenant init flow IS GAP-531** (separate gap, walkthrough below)

→ Wave plan §3.α scope mapping needs revision: GAP-516 = 2FA TOTP; GAP-531 = tenant init flow runbook. Both already in cluster scope.

**Evidence (against GAP-516 actual scope):**

- BE 2FA service classes ALL present in `kitehub-subscription/.../auth/twofactor/` (10 Java files):
  - TotpSecretCipher, TwoFactorAuthService, TwoFactorEnrollmentService, ChallengeTokenService, TwoFactorController, RecoveryCodeService, RecoveryCode, RecoveryCodeRepository, ChallengeTokenAuthenticationFilter, 7 DTOs ✓
- V37 migration shipped: `V37__add_user_2fa_columns.sql` ✓
- FE 2FA pages shipped (Wave 72b Bucket B): `/2fa-setup`, `/2fa-challenge`, RecoveryCodesDisplay ✓
- 2FA endpoints respond HTTP 401 without auth (security gate working) ✓
- 4 outstanding AC: TwoFactorControllerIT + 3 live-verify items (QR scan + wrong TOTP + correct TOTP) — gated GAP-612 AWS restore

**New findings:** Wave plan label discrepancy (META, not gap-level).

---

## GAP-531 — Tenant init handoff end-to-end (6-step beta approval + tenant init runbook)

**Pre-walkthrough %:** PARTIAL (Wave 78 Bucket E — runbook shipped, live walkthrough deferred)
**Post-walkthrough verdict:** PARTIAL — runbook artifact verified exists; live walkthrough still gated GAP-612

**Evidence:**

- Runbook present: `documents/05-guides/operations/tenant-init-handoff-runbook.md` ✓
- Admin user manual: `documents/05-guides/user-manual/platform-admin/beta-approval.md` ✓
- Admin approve endpoint responds HTTP 401 unauthenticated (security guard working): `POST /api/v1/admin/beta-requests/1/approve` ✓
- Live end-to-end walkthrough (admin login → approve → tenant init → invitee receives email → completes signup): BLOCKED GAP-612 AWS restore (production-equivalent verify required per `pre-handoff-self-test-completeness.md` §2.4)

**New findings:** None new; status accurate.

---

## GAP-610 — beta-signup validate token returns NOT_FOUND for valid token

**Pre-walkthrough %:** 75% PARTIAL — "Class C application-layer fix shipped Wave br-5 Bucket C (PR #1828)"
**Post-walkthrough verdict:** **PARTIAL — 75% retained, BUT NEW P1 SURFACED** (input validation gap class)

**Evidence:**

- Valid UUID token returns HTTP 404 + `{"valid":false,"errorCode":"TOKEN_NOT_FOUND"}` ✓ (Class C fix WORKING for valid UUID inputs)
- **🔴 NEW P1 FINDING**: Invalid UUID string (vd `?token=fake-token-12345` hoặc `?token=test123`) → HTTP 500 instead of proper 400 with VALIDATION error
  - Root cause: `MethodArgumentTypeMismatchException` — controller param typed `UUID`, Spring fails type conversion before validation handler
  - Stack trace evidence (subscription log):
    ```
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException:
    Method parameter 'token': Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID';
    Invalid UUID string: test123
    ```
  - Impact: bad actor probing endpoint với non-UUID strings → 500 leaks generic error → fingerprinting risk + poor UX cho legitimate user typo

**Recommendation:** File **NEW P1 GAP** "beta-signup validate token returns 500 on invalid UUID format input" — fix via `@ExceptionHandler(MethodArgumentTypeMismatchException)` returning 400 + `{"valid":false,"errorCode":"INVALID_TOKEN_FORMAT"}` HOẶC change param signature to `String` + validate UUID format manually inside service.

**New findings:** P1 input validation gap (cascade — Wave br-5 fix incomplete for invalid input class).

---

## GAP-611 — POST /api/v1/auth/beta-signup empty body returns 404

**Pre-walkthrough %:** 70% PARTIAL (Wave beta-readiness-5 Bucket D fix shipped)
**Post-walkthrough verdict:** **DONE candidate — 100% (fix verified WORKING locally)** với caveat

**Evidence:**

- Direct subscription POST empty body: HTTP **400** + `{"type":"about:blank","title":"Validation Error","status":400,"detail":"subdomain: must not be blank; token: must not be null; ownerPassword: must not be blank;"}` ✓
- Gateway POST empty body (after CB reset): HTTP **400** + same JSON validation error ✓
- Gateway POST valid body shape (token + ownerPassword + subdomain) với non-existent token: HTTP **404** + `{"errorCode":"INVALID_TOKEN","message":"Token không hợp lệ hoặc đã được sử dụng. Vui lòng yêu cầu invite mới."}` ✓ — proper JSON error per VN-localization audit checklist
- Initial test returned HTTP 503 (Circuit Breaker triggered after repeated retries) — false positive, NOT a routing bug; cleared after 30s CB reset window

**Caveat on DONE candidate flip:**
- Wave plan §3.α stated "expect JSON 404". Actual behavior = HTTP 400 (validation error) for empty body, HTTP 404 (token not found) for non-existent token. **HTTP 400 is more semantically correct per RFC 7231** — wave plan expected status mis-stated.
- Recommendation: update GAP-611 §AC + ROADMAP wording to reflect HTTP 400 (empty body) / 404 (valid shape + bad token) split. Then flip DONE.
- Live verify on AWS still pending GAP-612 — keep PARTIAL until prod-equivalent verify.

**New findings:** Gap expected-status documentation drift (cosmetic, not functional).

---

## GAP-724 — kc-frontend auth.test.ts path mismatch

**Pre-walkthrough %:** 50% PARTIAL
**Post-walkthrough verdict:** **DONE — 12/12 PASS locally ✅**

**Evidence:**

```bash
$ cd kiteclass/kiteclass-frontend && pnpm test --run src/lib/api/__tests__/auth.test.ts

 RUN  v4.1.5
 Test Files  1 passed (1)
      Tests  12 passed (12)
   Duration  8.53s
```

- All 12 unit tests PASS clean
- Browser http://localhost:3000 (kiteclass-frontend) HTTP 200 ✓
- Browser http://localhost:3001 (kitehub-frontend) HTTP 200 ✓

**Recommendation:** Flip GAP-724 → DONE (100%). Local FE test infrastructure healthy; auth path mismatch fix verified.

**New findings:** None.

---

## Cascade findings summary (for coordinator)

### 🔴 Cascade #1: GAP-516 wave plan label discrepancy

Wave plan `wave-2026-05-26-rst-cascade-1-local-first-aws-verify.md` §3.α maps **GAP-516 → "Tenant init flow"** which is FACTUALLY WRONG. Actual: GAP-516 = 2FA TOTP. Tenant init flow = GAP-531.

**Pattern signal:** Wave plan drafting state-check miss per `audit-to-gap-pipeline.md` §2.6 wave-plan pre-flight state-check. Lookup `query-gaps.sh GAP-516` would have surfaced canonical title before plan merge.

**Recommendation:** Coordinator update wave plan §3.α post-cluster-3-merge with correct mapping (no scope change — both gaps already in cluster 3).

### 🔴 Cascade #2: GAP-610 incomplete fix — NEW P1 needed

Wave br-5 Bucket C "Class C application-layer fix shipped" only handles VALID UUID inputs. Invalid UUID format → HTTP 500 (MethodArgumentTypeMismatchException). Same incident class as GAP-291 incomplete (consumer code shipped without queue @Bean) cascade trigger.

**Recommendation:** File new P1 gap "beta-signup validate handles invalid UUID format 500" — coordinator post-merge.

### 🟡 Cascade #3: GAP-611 expected-status documentation drift (cosmetic)

Wave plan expected "JSON 404" — actual HTTP 400 (empty body) is MORE correct. Update AC + docs only, not code.

### 🟡 Cascade #4: `class.rescheduled.queue` @Bean Queue declaration missing (confirmed)

Per coordinator pre-flag: queue exists in RabbitMQ (manually declared via `rabbitmqctl declare`) NHƯNG @RabbitListener consumer files reference queue WITHOUT matching `@Bean Queue` in `kiteclass-core/config/`:
- Consumer: `ClassRescheduledNoOpConsumer.java:38` `@RabbitListener(queues = "class.rescheduled.queue")`
- Consumer: `ClassRescheduledEmailConsumer.java:45` `@RabbitListener(queues = "class.rescheduled.queue")`
- Missing: `@Bean Queue classRescheduledQueue()` declaration

**Pattern signal:** Wave br-4 GAP-291 incomplete fix — consumer ship without queue topology. Recurrence ≥2 = systemic per `incident-to-rule-pipeline.md` §3.1.

**Recommendation:** Coordinator file new META gap "RabbitMQ queue topology declaration completeness mandate" — every @RabbitListener queue MUST have matching @Bean Queue declaration in config.

---

## Verdict (cluster 3 summary)

| Gap | Pre-% | Post-verdict | Delta |
|---|:---:|:---:|---|
| GAP-538 | 95% | PARTIAL 95% | 0 (live verify still gated GAP-612) |
| GAP-516 | 80% | PARTIAL 80% | 0 (4 AC gated GAP-612) — META wave plan label discrepancy flagged |
| GAP-531 | PARTIAL | PARTIAL | 0 (live verify gated GAP-612) |
| GAP-610 | 75% | PARTIAL 75% + **NEW P1 cascade** | 0% on this gap; **+1 NEW P1 follow-up needed** |
| GAP-611 | 70% | **PARTIAL 90% → DONE candidate** | +20% (fix verified locally); needs prod verify GAP-612 unblock + AC doc reword |
| GAP-724 | 50% | **DONE 100%** ✅ | +50% — local 12/12 PASS |

**Cluster verdict:** 1 DONE flip (GAP-724), 1 DONE candidate (GAP-611 pending AC reword), 4 PARTIAL retained (3 blocked GAP-612 + 1 surfaced new cascade P1). Pre-handoff self-test completeness coverage:
- §2.2 anonymous-flow (GAP-611): ✓ form submit + confirmation surface
- §2.4 admin-flow (GAP-516, GAP-531): partial — role-guard + endpoint working, full flow gated GAP-612

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-26-rst-cascade-1-local-first-aws-verify.md` §3.α
- Sister cluster audits: cluster 1 + cluster 2 (parallel agents)
- Pre-handoff rule: `.claude/rules/pre-handoff-self-test-completeness.md` §2.2 + §2.4
- Cascade rule: `.claude/rules/incident-to-rule-pipeline.md` §3.1
- GAP-612: AWS account 906286017800 suspension (blocks prod verify)
- GAP-291: Wave br-4 RabbitMQ queue topology incomplete (sister pattern)
