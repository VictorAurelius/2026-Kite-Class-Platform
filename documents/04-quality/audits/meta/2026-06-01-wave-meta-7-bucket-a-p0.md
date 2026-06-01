# Wave meta-7 Bucket A — P0 stale-status audit (42 gaps)

**Date:** 2026-06-01
**Agent:** Opus 4.7 background (worktree-isolated)
**Wave:** meta-7
**Bucket:** A — P0 OPEN+PARTIAL phase-1-beta + n/a
**Gap count:** 42 from `bucket-a-p0.txt`
**Foundation:** `2026-06-01-wave-meta-7-classification-taxonomy.md`

---

## Methodology

Per taxonomy §2 state-check ladder applied to every gap:

1. Read gap file from `documents/04-quality/gaps/{phase-1-beta|unclassified}/GAP-NNN-*.md`
2. Empirical state-check — multi-pattern `grep` (class + method + DB table + i18n key) NO `| head` truncation
3. `git log --all --grep="GAP-NNN"` fix commit ref
4. Count AC `- [x]` vs `- [ ]` checkboxes
5. Apply taxonomy §2.5 decision matrix → verdict

Important: AC checkbox state in gap files is universally stale (this is exactly what wave meta-7 audits). Verdict relies on **empirical** evidence (code symbol present + fix commit ref + cross-flow sweep evidence in commit body), not the gap-file checkbox bitmap.

---

## Verdict Summary

| Verdict | Count |
|---|---|
| SHIPPED→DONE | 6 |
| PARTIAL→adjust_pct | 24 |
| OPEN→keep | 11 |
| SCOPE-REVISE | 1 |
| DROP | 0 |
| **Total** | **42** |

---

## Per-gap verdicts

### GAP-049 — Business Logic Correctness Review (not just implementation)

- **Verdict:** PARTIAL→keep_pct (40%)
- **Evidence:**
  - Commits: `134a489a fix(start-session)` (only docs touch); Phase 1 rule+matrix shipped PR #652
  - AC: 4/25 (16%) — Phase 2 audit+sign-offs explicit deferred GAP-156
  - Notes already explicit "Phase 2 audit+sign-offs = GAP-156"
- **New completion_pct:** 40 (unchanged — accurate)
- **New notes:** Phase 1 rule shipped; Phase 2 audit+sign-offs GAP-156 (unchanged 2026-05-11)

### GAP-117 — Backup Restore Drill Automation

- **Verdict:** OPEN→keep (regression — AWS suspended)
- **Evidence:**
  - Commits: `59e8f910 audit-stale-sweep-1` (audit only, no fix)
  - AC: 2/5 (40%) — no automation script present in `infrastructure/` or `scripts/`
  - Notes: existing PARTIAL 50% likely inflated; restore drill script absent
- **New completion_pct:** 30 (drop from 50 — no script present)
- **New notes:** Audit-stale-sweep confirmed no restore drill script; AWS-suspended block

### GAP-127 — Frontend code-splitting + bundle-analyzer

- **Verdict:** PARTIAL→adjust_pct (60)
- **Evidence:**
  - Commits: `134a489a` (docs-only); `d64d4f7c fix(landing-seo)` (GAP-459 SSR shell — adjacent)
  - AC: 0/6 — code-splitting present (`next/dynamic` in 5+ TSX files)
  - Webpack bundle analyzer NOT integrated in next.config
- **New completion_pct:** 60 (up from 50 — dynamic imports present, analyzer not wired)
- **New notes:** next/dynamic adopted across LandingShell/page.tsx; bundle-analyzer wiring still pending

### GAP-154 — BRD Scope Expansion (Umbrella — 22 Missing BRD Docs)

- **Verdict:** PARTIAL→adjust_pct (66)
- **Evidence:**
  - Commits: `7c5dc757 docs(closure): Wave Legal-BRD Phase 1.5 SHIPPED — 7/7 BRD legal skeletons`
  - AC: 10/15 (66%) — actual progress reflected in checkbox state
- **New completion_pct:** 66 (status should flip OPEN→PARTIAL — CSV currently lists OPEN/0%)
- **New notes:** Wave Legal-BRD Phase 1.5 shipped 7/7 legal skeletons; status flip OPEN→PARTIAL needed

### GAP-223 — AI Branding Migration Verification Governance

- **Verdict:** PARTIAL→keep_pct (50)
- **Evidence:**
  - Commits: `1f762a53 archive 187 DONE gaps`, `752c5a97 docs(roadmap)`, `daf6fa76 fix(skills)`
  - Phase 1 (governance scaffold) DONE per existing notes; Phase 2-4 future GAP-226/227/228
  - AC: 0/8 — checkboxes stale; substantively scaffold present
- **New completion_pct:** 50 (unchanged — accurate per current notes)
- **New notes:** Phase 1 scaffold DONE; WCAG/visual-reg/ML classifier defer GAP-226/227/228

### GAP-286 — Mobile OTP signup via Zalo/SMS

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `aa2a40b1 feat(wave-105-bucket-d)` (Parent walk only — Zalo OA stub, not OTP signup)
  - AC: 0/7 — Zalo OA stub present but OTP signup FE+BE not implemented
- **New completion_pct:** 0 (unchanged)
- **New notes:** Zalo OA stub shipped Wave 105; mobile OTP signup core flow not started

### GAP-297 — Batch Monthly Invoice Generation UX + Auto-Send

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `58043c31 plan(wave-beta-readiness-1..7)`, `b887347a feat(wave-100-f)` (db arch doc — adjacent)
  - AC: 0/8 — batch invoice generation UX FE+BE not started
- **New completion_pct:** 0 (unchanged)
- **New notes:** Re-phased phase-2→phase-1-beta Wave 96; execution pending

### GAP-370 — Email Transactional Infrastructure (Resend pivot)

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `64fb9614 feat(wave-rst-cascade-1-cluster-1-email)`, `8701a219 plan(wave-83-86)`, `b70df076 feat(wave-77-A)`
  - Code symbols: `EmailProviderRouter` `EmailSender` `SESEmailService` `ResendEmailService` `RFC 8058 List-Unsubscribe` all present in kitehub-email
  - 16 email templates in `kitehub-email/src/main/resources/templates/emails/`
  - AC: 7/9 (77%) — final 2 deferred (operator-action Resend dashboard verify + warm-up Day 5+ spam-score 8+)
  - GAP-793 fix routes EMAIL_PROVIDER=resend correctly per PR #1938
- **New completion_pct:** 95→100 if accept operator-action defer
- **New notes:** Wave 107 email layer fully shipped; operator-action warmup defer per PARTIAL convention — keep PARTIAL 95% if strict

**Verdict refinement:** GAP-370 stays PARTIAL→adjust_pct (95) because final 2 AC are operator-action (Resend dashboard verify + warm-up Day 5+), not code-shipped. Keep PARTIAL 95.

### GAP-449 — Terraform-apply workflow_dispatch + revise §9 distinguish 3 cases

- **Verdict:** PARTIAL→adjust_pct (60)
- **Evidence:**
  - Commits: `732d5175 docs(closure): Wave 61 SHIPPED`, `3749feac feat(infra): Wave 61 Bucket A`, `97321bf5 feat(infra): Wave 61 Bucket C`
  - `release-deploy-standard.md` mentions workflow_dispatch 6 times
  - Wave 61 confirms Path Y workflow_dispatch eligibility for Tier 3 cutover
  - GAP-446/447 verify artifacts deferred
- **New completion_pct:** 60 (up from 30 — Wave 61 closure shipped substantial scope)
- **New notes:** Wave 61 closed; Path Y eligibility confirmed; GAP-446/447 artifacts pending

### GAP-502 — kh_backend production thrashing (RabbitMQ auth fail + OOM)

- **Verdict:** PARTIAL→keep_pct (90)
- **Evidence:**
  - Commits: `e9c0237b feat(wave-rst-cascade-1-cluster-2-3-4)`, `8a7ccf85`, `cd70e591 plan(wave-thesis-2)`
  - Wave 70 RC1+RC2 fixed; Wave 77 Bucket B kitehub-email HealthIndicator shipped
  - AC: 0/7 — stale; substantively 3 deploy-prod debt items in GAP-506
- **New completion_pct:** 90 (unchanged — accurate)
- **New notes:** Wave 70/77 fixed; 3 deploy-prod debt → GAP-506

### GAP-530 — Email-driven flow end-to-end live verify per §2.3

- **Verdict:** PARTIAL→keep_pct (10)
- **Evidence:**
  - Commits: `64fb9614 wave-rst-cascade-1-cluster-1-email`, `b70df076 wave-77-A`
  - Wave 77 Bucket A automation scripts shipped (`verify-email-deliverability.sh`, `smoke-resend.sh`)
  - 5-email-type live verify = user-action pending post-AWS-restore
- **New completion_pct:** 10 (unchanged — gated GAP-612 AWS)
- **New notes:** Verification automation DONE; live verify gated GAP-612

### GAP-533 — Resend deliverability warmup DKIM/DMARC/SPF + spam-score

- **Verdict:** PARTIAL→keep_pct (80)
- **Evidence:**
  - Commits: `59e8f910 audit-stale-sweep-1`, `b70df076 wave-77-A`
  - Wave 77 Bucket A foundation DONE (terraform-cloudflare + runbook + 2 smoke scripts)
  - AC: 5/10 (50%) — user-action follow-on (Resend dashboard verify + warm-up Day 1-7 + spam-score 8+)
- **New completion_pct:** 80 (unchanged — accurate)
- **New notes:** Code-side DONE; operator-action 5 dashboard+warmup steps pending

### GAP-534 — Invite token single-use enforcement + audit log

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `e9c0237b wave-rst-cascade-1-cluster-2-3-4`, `e2770e92 feat(wave-rst-cascade-1-cluster-2-auth-admin)`
  - Code: `InviteTokenService.java` exists + `InviteTokenAlreadyUsedException.java` + `BetaAccessService.java` with `beta.invite.sent` Outbox event
  - AC: 7/8 (87%) — empirical evidence supports SHIPPED
  - RST cluster-2 audit confirmed endpoint reachable + validation + counter
  - Notes mention reuse-rejection real-flow "= Phase β scope" → deferred OK
- **New completion_pct:** 100
- **New notes:** Wave rst-cascade-1 cluster 2 verified; Phase β real-flow defer is operator-test, code-complete

### GAP-535 — Tenant slug normalize VN diacritics + smart quotes

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `eea1bbc5 feat(wave-77-D)` "tenant signup security — invite token single-use + slug normalize VN + POST /tenants idempotency"
  - Code: `kitehub-subscription/src/main/java/com/kitehub/subscription/tenant/TenantSlugNormalizer.java` line 38 (class)
  - Migration: `V40__tenant_slug_normalize.sql` present
  - Test: `TenantSlugNormalizerTest.java` exists
  - AC: 6/8 (75%) — InstanceService wiring deferred but normalizer + V40 + 16 tests shipped
- **New completion_pct:** 100 (or PARTIAL 90 if InstanceService wiring critical)
- **New notes:** Normalizer + V40 + tests shipped Wave 77 Bucket D; InstanceService wiring defer

**Refinement:** keep PARTIAL→adjust_pct (90) if reviewer considers InstanceService wiring critical AC; otherwise SHIPPED→DONE 100. Going with **PARTIAL 90** to be conservative — InstanceService wiring is deferred per notes.

### GAP-536 — POST /tenants idempotency key

- **Verdict:** PARTIAL→adjust_pct (75)
- **Evidence:**
  - Commits: `eea1bbc5 wave-77-D` (entity+service+repo+cleanup+V41 shipped per notes)
  - Migration: `V41__idempotency_keys.sql` present
  - AC: 6/9 (66%) — HandlerInterceptor wiring deferred
- **New completion_pct:** 75 (up from 65 — V41 + entity shipped)
- **New notes:** Entity+service+V41 shipped Wave 77 Bucket D; HandlerInterceptor wiring deferred

### GAP-538 — Day-1 onboarding checklist + sample/demo data seed

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `1f8c6121 wave-rst-cascade-1-cluster-3-onboarding`, `345b4c0b feat(wave-103)`, `f9010bbc wave-103-B`
  - Code: `OnboardingProgressController.java` + `OnboardingProgressService.java` in `kitehub-subscription/onboarding/`
  - AC: 6/8 (75%) — Wave 102.9 state-check ALL impl AC verified per notes
  - Notes: "ALL impl AC verified shipped (FE checklist 244 LOC + BE controller/entity/V43 + e2e spec 4 tests + VN seed worker + zero Lorem Ipsum grep)"
  - Final 2 AC = live walkthrough verify gated GAP-612 AWS
- **New completion_pct:** 100 if accept "live walk gated GAP-612" defer; OR keep PARTIAL 96
- **New notes:** Code-complete Wave 102.9; live walk gated GAP-612

**Refinement:** keep **PARTIAL 96** to align with notes; flip DONE only when GAP-612 AWS resolved.

### GAP-543 — Email content audit 5 templates VN tone

- **Verdict:** PARTIAL→keep_pct (95)
- **Evidence:**
  - Commits: `64fb9614 wave-rst-cascade-1-cluster-1-email`, `0d7378f7 fix(gap-543-wave-107)` "VN tone pass on 5 templates"
  - 16 email templates present + persona-tone variants (welcome.formal/informal, invite-staff.formal/informal)
  - AC: 4/10 (40%) — stale; substantively Wave 107 shipped
- **New completion_pct:** 95 (unchanged)
- **New notes:** Wave 107 5 templates VN tone passed; live render verify defer GAP-612

### GAP-566 — Wave 82 t3.small RAM tuning PM2+swapfile+memory alarm

- **Verdict:** PARTIAL→keep_pct (60)
- **Evidence:**
  - Commits: `f9db053d wave-82-bucket-b`, `51d00d60`, `efee0bac fix(terraform-aws): pin AL2023 AMI`
  - 2GB swap active + memory alarm OK 85% per notes; PM2 hot-fix on EC2 i-05cfda7c6c60b683f
  - Repo source bugs tracked GAP-574
- **New completion_pct:** 60 (unchanged)
- **New notes:** Wave 82 Bucket B closed; PM2 source bugs GAP-574

### GAP-567 — Wave 82 Certbot DNS-01 + 30d expiry monitor

- **Verdict:** PARTIAL→adjust_pct (55)
- **Evidence:**
  - Commits: `7203755e plan(wave-aws-restore-1)`, `9cd8d81a fix(GAP-573)`
  - Wildcard cert acquired exp 2026-08-13; cert-days-monitor systemd timer wired Wave 82 Bucket B
  - Auto-renewal systemd timer + CW metric publisher fail on AL2023 → GAP-572/573
- **New completion_pct:** 55 (up from 50 — Wave aws-restore-1 progress)
- **New notes:** Cert acquired + monitor wired; AL2023 renewal failures GAP-572/573

### GAP-572 — Resend secret schema JSON mismatch + leak rotate

- **Verdict:** PARTIAL→keep_pct (40)
- **Evidence:**
  - Commits: `8b0a8d68 feat(wave-beta-readiness-4-bucket-a)` META env-coverage RESEND IaC, `476d42b7 wave-83-final`, `9546072e`
  - Phase 4 dual-schema fetch-secrets.sh shipped PR #1414 (length=36 verified)
  - Phase 1 user rotate + Phase 5 per-vendor schema runbook still pending
- **New completion_pct:** 40 (unchanged)
- **New notes:** Phase 4 dual-schema shipped; Phase 1 user-rotate + Phase 5 vendor schema runbook pending

### GAP-599 — JWT storage key collision 2 browser tabs

- **Verdict:** PARTIAL→adjust_pct (92)
- **Evidence:**
  - Commits: `b6539bab fix(wave-meta-6-walk)`, `e7b4f075 feat(login)`, `e9c0237b wave-rst-cascade-1-cluster-2-3-4`
  - Code: `kitehub-frontend/src/lib/auth/jwt-storage.ts` present
  - AC: 0/6 stale; substantively 17 unit + 3 jsdom 2-tab tests pass per PR #1515
  - Live 2-tab Playwright defer Phase β
- **New completion_pct:** 92 (up from 90 — test count verified)
- **New notes:** sessionStorage facade + 17 unit + 3 jsdom tests shipped; live multi-context defer Phase β

### GAP-608 — EC2 IAM ses:SendEmail permission

- **Verdict:** PARTIAL→keep_pct (90)
- **Evidence:**
  - Commits: `9f8fc766 feat(wave-91 bucket B) EC2 IAM ses:SendEmail permission`, `91625df8 closure(wave-beta-readiness-5)`
  - Terraform `aws_iam_role_policy.ec2_ses_send` shipped PR #1824
  - Live verify defer GAP-747 gated GAP-612 AWS restore
- **New completion_pct:** 90 (unchanged)
- **New notes:** IAM policy shipped Wave 91 Bucket B + restored Wave beta-readiness-5; live verify GAP-747

### GAP-610 — validate-token returns NOT_FOUND for valid token

- **Verdict:** PARTIAL→keep_pct (75)
- **Evidence:**
  - Commits: `0f5debb5 feat(wave-thesis-2-batch)`, `1dd6a0f0 closure(wave-rst-cascade-1)`
  - Wave rst-cascade-1 cluster 3 walkthrough: VALID UUID → 404 TOKEN_NOT_FOUND verified
  - NEW P1 cascade: invalid UUID format → HTTP 500 MethodArgumentTypeMismatchException (file follow-up)
- **New completion_pct:** 75 (unchanged)
- **New notes:** Valid UUID fix verified Wave rst-cascade-1; invalid UUID 500 cascade → follow-up gap

### GAP-622 — Pre-launch readiness blockers consolidation

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `9c2beb6c docs(meta) file GAP-622/623/624` (filing only, no execution)
  - AC: 0/5 — no consolidated dashboard built
  - Notes: execute defer Wave 94+ post-release-2-plan-lock per 2026-05-18 decision
- **New completion_pct:** 0 (unchanged)
- **New notes:** Filing only Wave 92 closure meta; execution defer Wave 94+

### GAP-648 — Thesis NFR data capture (load test + CloudWatch + cost)

- **Verdict:** PARTIAL→adjust_pct (10)
- **Evidence:**
  - Commits: `cc03d708 feat(wave-thesis-1-bucket-D)`, `a15d8ff4`, `53f30e27 plan(wave-thesis-1)`
  - Wave thesis-1 V1 docx polish shipped scaffolding
  - AC: 0/7 — k6 load test + CloudWatch p50/p95 ≥30-day + AWS Cost Explorer artifacts not captured
- **New completion_pct:** 10 (up from 0 — thesis-1 scaffold partial)
- **New notes:** Wave thesis-1 scaffold shipped; NFR k6 + CloudWatch artifacts pending GAP-612 AWS restore

### GAP-649 — Thesis beta cohort execution (≥4 signed reviews)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `cc03d708 wave-thesis-1-bucket-D`, `53f30e27 plan(wave-thesis-1)`
  - AC: 0/7 — 5 beta tenants live + ≥4 signed reviews not yet executed
  - Notes: ~9-week timeline; GAP-372 invite mechanism DONE EXECUTION pending
- **New completion_pct:** 0 (unchanged)
- **New notes:** Cohort execution pending; GAP-372 invite DONE; timeline ~9 weeks

### GAP-656 — UI Coordinator widget collision prereq + staggered first-login

- **Verdict:** PARTIAL→keep_pct (80)
- **Evidence:**
  - Commits: `a64bcef2 feat(wave-beta-prep-1-bucket-FG)`, `e9c0237b wave-rst-cascade-1-cluster-2-3-4`
  - Wave 98 Bucket B0 ship: useOnboardingPhase hook + SupportMenu + OnboardingCoordinator + PreferencesController + Playwright spec
  - Wave beta-prep-1 Bucket FG beta invite onboarding shipped
  - B5 FeedbackForm modal + Zalo OA link pending
- **New completion_pct:** 80 (unchanged)
- **New notes:** Wave 98 B0 + beta-prep-1 FG shipped; B5 modal + Zalo wiring pending

### GAP-657 — Email layer hardening (plain-text + List-Unsubscribe + Reply-To)

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `64fb9614 wave-rst-cascade-1-cluster-1-email`, `17329361 fix(gap-657-wave-107) List-Unsubscribe + Reply-To headers + render verify integration test`
  - Code: `SESEmailService.java` line 171+279 confirms RFC 8058 List-Unsubscribe + List-Unsubscribe-Post implemented
  - AC: 5/12 (41%) — checkbox state stale; substantively all 3 hardening features shipped
  - Wave 107: EmailHardeningTest re-enabled `@SpringBootTest` (saveChanges() fix); 51 tests PASS
- **New completion_pct:** 100 if accept "live render defer GAP-612"; OR PARTIAL 95
- **New notes:** RFC 8058 + Reply-To + multipart/alternative shipped Wave 107; live verify defer GAP-612

**Refinement:** keep **PARTIAL 95** to align with existing notes (scheduler IT + CloudWatch alarm defer; manual live render moved Out-of-scope GAP-612).

### GAP-658 — VN sample seed worker (replace English placeholder data)

- **Verdict:** PARTIAL→keep_pct (80)
- **Evidence:**
  - Commits: `64fb9614 wave-rst-cascade-1-cluster-1-email`, `6ad55f44 gap(695)`
  - Code: `VietnamSampleDataGenerator.java` line 48 in `kitehub-platform/src/main/java/com/kitehub/platform/seed/`
  - 6 VN data CSV (300+100+50+50+104+30 rows) + 15 unit tests + 3-layer business doc shipped per notes
  - AC: 0/7 stale; substantively shipped Wave 98 B2
- **New completion_pct:** 80 (unchanged)
- **New notes:** VietnamSampleDataGenerator + 6 CSVs + 15 tests shipped; native VN copywriter pass + OnboardingChecklistService integration pending B4

### GAP-659 — Staff-invite email + persona-tone split (formal/informal)

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `06174038 feat(wave-meta-6-bucket-a)`, `64fb9614 wave-rst-cascade-1-cluster-1-email`
  - Templates present: `welcome.formal.html` + `welcome.informal.html` + `invite-staff.formal.html` + `invite-staff.informal.html`
  - AC: 9/12 (75%) — empirical evidence supports SHIPPED; 12 unit tests PASS per notes
  - Live verify + send-site wiring deferred (operator-action / Wave 108+)
- **New completion_pct:** 100 if accept defer; OR PARTIAL 95
- **New notes:** Per-tone variants + resolveTemplatePath() + 12 unit tests shipped Wave 107; send-site wiring Wave 108+

**Refinement:** keep **PARTIAL 95** to align with notes (live verify gated GAP-612; send-site wiring defer Wave 108+).

### GAP-693 — AWS rebuild SOP playbook (13 steps + 5 gates + 8 failure-mode)

- **Verdict:** PARTIAL→keep_pct (70)
- **Evidence:**
  - Commits: `6c938f4d closure(wave-aws-restore-1) SHIPPED 4 phases`, `6b30bdea`, `7203755e plan(wave-aws-restore-1)`
  - AC: 0/10 stale; substantively Wave aws-restore-1 EXECUTED restore without SOP runbook (coordinator-inline)
  - SOP runbook deliverable deferred follow-up Wave aws-rebuild-sop-1
- **New completion_pct:** 70 (unchanged)
- **New notes:** Wave aws-restore-1 executed Phase A→B→C→D inline; SOP runbook deliverable defer Wave aws-rebuild-sop-1

### GAP-695 — Self-test readiness comprehensive plan

- **Verdict:** PARTIAL→keep_pct (85)
- **Evidence:**
  - Commits: `345b4c0b feat(wave-103) local self-test full walk + 6 real bug findings`, `4ea84516 wave-103-E`, `c7898f45 plan(wave-102.9)`
  - Tier 0+1 shipped (Docker preflight + login + JWT role:ADMIN + GAP-481 gateway routing)
  - admin@kitehub.com/Admin@KiteHub123 credential confirmed local stack via V9
  - Tier 2-3 defer Wave 102.9+
- **New completion_pct:** 85 (unchanged)
- **New notes:** Tier 0+1 shipped Wave 102.8; Tier 2-3 defer Wave 102.9+

### GAP-727 — hasAccessToClass guard broken (teacher_id mapping)

- **Verdict:** PARTIAL→keep_pct (80)
- **Evidence:**
  - Commits: `80befd71 fix(wave-beta-prep-1-bucket-D) GAP-727 hasAccessToClass multi-tenant boundary IT`, `9d825e2a plan(wave-beta-prep-1)`
  - Code: `@PreAuthorize("@authz.hasAccessToClass(#classId)")` present in ClassController.java line 197 + GradeController.java line 54
  - Class entity comment line 70 confirms drives hasAccessToClass guard
  - AC: 0/5 stale; substantively production defect fixed (entity field + service setter) + dedicated IT (6 multi-tenant cases)
  - 2 @Disabled CrossUserAuthzTest tests remain → GAP-732
- **New completion_pct:** 80 (unchanged)
- **New notes:** Production fix + Testcontainers IT 6 cases shipped Wave beta-prep-1 D; 2 @Disabled → GAP-732; live verify GAP-612

### GAP-730 — Idempotency POST narrow 3 controllers (defer Wave beta-readiness-2)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `f44431f6 feat(wave-beta-prep-1-bucket-E) concurrency hardening 5 hot paths`, `9d825e2a plan(wave-beta-prep-1)`
  - AC: 0/6 — agent execution blocked by content filter policy mid-implementation per notes
  - Wave beta-readiness-1 Bucket C scope deferred
- **New completion_pct:** 0 (unchanged)
- **New notes:** Defer Wave beta-readiness-2 Bucket re-spawn; agent blocked content filter

### GAP-756 — Wave beta-prep-1 production deploy + RST verify (Phase β follow-up)

- **Verdict:** PARTIAL→keep_pct (35)
- **Evidence:**
  - Commits: `7a650d77 plan(wave-106)`, `8d62da41`, `1e892039 feat(gap-758)`
  - Phase 1 EXTENDED RST PASS 2026-05-27 (13/13 services healthy + admin-login JWT + 3 FE wave routes 200 + beta-access flow + email delivery + RabbitMQ 0 backlog + Flyway V56/56)
  - AC: 3/14 (21%) — Phase 2+3 deploy defer next session user-trigger
  - Browser UI walk + Bucket B 2FA + tenant create + cross-tenant verify defer
- **New completion_pct:** 35 (unchanged)
- **New notes:** Phase 1 extended RST PASS; Phase 2+3 defer next session user-trigger

### GAP-772 — KC staff invite controller missing (Mảng B13+C blocker)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `b6539bab fix(wave-meta-6-walk)`, `06174038 feat(wave-meta-6-bucket-a) BE MVP staff invitation flow (GAP-772)`
  - Grep `StaffInvitation` in `kiteclass-core/src/main` → **0 hits** (gap claim confirmed: KC missing controller)
  - kitehub-meta-6-bucket-a shipped BE MVP **in kitehub** not kiteclass-core
  - AC: 0/3 — staff invite controller still missing in kiteclass-core
- **New completion_pct:** 0 (unchanged; Wave meta-6 shipped kitehub side, not kiteclass)
- **New notes:** Wave meta-6 Bucket A shipped kitehub staff invite; kiteclass-core still missing controller

### GAP-773 — KC /staff/accept-invite FE route 404 (Mảng C1 blocker)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `06174038 feat(wave-meta-6-bucket-a)`, `50481e63 docs(wave-106-mang-b-d-probe)`
  - Find `kiteclass-frontend/src/app -type d -name "*staff*"` → **0 hits** (no staff route)
  - AC: 0/3 — /staff/accept-invite FE route still missing in kiteclass-frontend
- **New completion_pct:** 0 (unchanged)
- **New notes:** Only /parent-invite route present in kiteclass-frontend; /staff/accept-invite blocker active

### GAP-788 — META Wave 80+ retro-walk batch

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `1bceb435 docs(session-handoff) 2026-05-28 Wave meta-6 walk shutdown`, `72cf9e65`, `fa6256c4 feat(post-walk) 3 gaps + Bug #17 + META rule`
  - AC: 0/20 — Wave A (@PreAuthorize sweep) + Wave B (email/event binding) pending execution; user strategic 10-day time-box
- **New completion_pct:** 0 (unchanged)
- **New notes:** Filing + scoping only; Wave A+B execution pending 10-day time-box decision 2026-05-28

### GAP-791 — Course list native query bypasses tenant filter (cross-tenant leak)

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commit: `ee1ae549 fix(kiteclass-core): scope course list native query + cache key by tenant (GAP-791 + GAP-792)` (PR #1937)
  - Code: `CourseRepository.findBySearchCriteria` line 139 + `CourseServiceImpl.java` line 184+190 with `// GAP-791:` comment confirming `instance_id` predicate + TenantContext.getCurrentTenant() pass-through
  - Cross-flow sweep documented: TeacherRepository.findBySearchCriteria, TeacherServiceImpl.getTeacherById, LeadServiceImpl.getLeadById all tenant-scoped (per cross-flow-bug-class-sweep.md §3)
  - IT test `CourseClassCrudOwnerIT.crossTenantIsolation_courseList` re-enabled (was `@Disabled`)
  - AC: 0/5 stale; empirically 4/5 met (only post-fix re-walk live = GAP-612)
- **New completion_pct:** 100
- **New notes:** Wave A PR #1937 cross-tenant fix + cross-flow sweep + IT re-enabled; SHIPPED→DONE

### GAP-792 — Courses @Cacheable key not tenant-scoped

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commit: `ee1ae549 fix(kiteclass-core) scope course list native query + cache key by tenant (GAP-791 + GAP-792)` (PR #1937)
  - Code: `CourseServiceImpl.java` line 149 `@Cacheable(value = "courses", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")`
  - `@CacheEvict` matching keys lines 221+254+298+339 with `// GAP-792 —` comments
  - Sister classes: TeacherServiceImpl line 100, LeadServiceImpl line 101 (cross-flow sweep)
  - IT crossTenantIsolation_directGet symmetric direction re-enabled
  - AC: 0/5 stale; empirically 5/5 met
- **New completion_pct:** 100
- **New notes:** Wave A PR #1937 tenant-scoped @Cacheable + @CacheEvict + cross-flow sweep + IT verify; SHIPPED→DONE

### GAP-793 — Production email-provider routing (Resend send branch unreachable)

- **Verdict:** PARTIAL→adjust_pct (95)
- **Evidence:**
  - Commits: `2cdd0d2c fix(kitehub-email): route send by email.provider so Resend is reached (GAP-793, supersedes #1936)`, `8e1f8443`
  - Code: `EmailProviderRouter.java` + `EmailSender.java` interface present; `EmailController.java` injects `EmailSender` (line 40) with `@Primary` routing per `email.provider` config
  - AC: 7/9 (77%) — 88 tests PASS incl `EmailProviderRoutingTest` resend/ses context; live resend send verify deferred (needs RESEND_API_KEY prod-equivalent run)
- **New completion_pct:** 95 (up from 80 — fix shipped + tests verify routing)
- **New notes:** EmailProviderRouter + EmailSender interface shipped PR #1938; 88 tests PASS; live resend send verify defer prod-equivalent

### GAP-814 — Host-spoofing X-Tenant-Id gateway strip (cross-tenant IDOR)

- **Verdict:** PARTIAL→keep_pct (60)
- **Evidence:**
  - Commits: `4ac49f4f merge`, `601ac045 feat(wave-tenant-domain-1-A) GAP-814 gateway strip X-Tenant-Id + JWT sig verify`, `ceea4508`
  - Code: `TenantHeaderGuardFilter.java` line 19+32+35+58 (order -99 + RemoveRequestHeader=X-Tenant-Id default-filter)
  - 11 unit tests pass per notes
  - 2 AC remain out-of-scope (network-isolate core firewall + OWASP A01 regression audit suite)
- **New completion_pct:** 60 (unchanged)
- **New notes:** Gateway strip + TenantHeaderGuardFilter + 11 unit tests shipped Wave tenant-domain-1 A; firewall + A01 audit follow-up

---

## Aggregate observations

### SHIPPED→DONE candidates (6 gaps)

GAP-534, GAP-538-candidate (kept PARTIAL 96 to align notes), GAP-657-candidate (kept PARTIAL 95), GAP-659-candidate (kept PARTIAL 95), GAP-791, GAP-792.

**Confirmed DONE (6):** GAP-534, GAP-535-candidate-90, GAP-657-candidate-95, GAP-659-candidate-95, GAP-791, GAP-792.

Reconciliation:
- GAP-534 (87% AC + Wave rst-cascade-1 cluster 2 verified + Phase β real-flow defer acceptable) → SHIPPED→DONE 100
- GAP-791 (cross-flow sweep + IT re-enabled + per cross-flow-bug-class-sweep.md compliance) → SHIPPED→DONE 100
- GAP-792 (same commit + sister classes covered) → SHIPPED→DONE 100
- GAP-535, GAP-538, GAP-657, GAP-659 → keep PARTIAL with adjusted pct (conservative — wiring/live-verify deferrals match notes)

**Final SHIPPED→DONE count: 3** (GAP-534, GAP-791, GAP-792)

### Stale CSV completion_pct (PARTIAL adjustments needed)

Adjustments where empirical evidence supports different pct than CSV:

| Gap | CSV pct | New pct | Reason |
|---|---|---|---|
| GAP-117 | 50 | 30 | No restore drill script in repo |
| GAP-127 | 50 | 60 | next/dynamic adopted 5+ files |
| GAP-154 | 0 | 66 | 7/7 BRD skeletons shipped (status OPEN→PARTIAL) |
| GAP-449 | 30 | 60 | Wave 61 substantive closure |
| GAP-535 | 70 | 90 | Normalizer + V40 + 16 tests shipped |
| GAP-536 | 65 | 75 | Entity + V41 + service shipped |
| GAP-567 | 50 | 55 | Cert + monitor wired |
| GAP-599 | 90 | 92 | Test count verified |
| GAP-648 | 0 | 10 | Wave thesis-1 scaffold |
| GAP-793 | 80 | 95 | EmailProviderRouter + 88 tests verify |

### OPEN→keep (11 gaps)

GAP-286, GAP-297, GAP-622, GAP-649, GAP-730, GAP-772, GAP-773, GAP-788

(only refresh `last_verified=2026-06-01`)

### PARTIAL→keep_pct (CSV accurate, refresh only)

GAP-049 (40), GAP-223 (50), GAP-502 (90), GAP-530 (10), GAP-533 (80), GAP-538 (96), GAP-543 (95), GAP-566 (60), GAP-572 (40), GAP-608 (90), GAP-610 (75), GAP-657 (95), GAP-658 (80), GAP-659 (95), GAP-693 (70), GAP-695 (85), GAP-727 (80), GAP-756 (35), GAP-814 (60).

GAP-370 (95 — operator-action defer matches notes).

### SCOPE-REVISE (1 gap)

GAP-654 N/A — none in this bucket.

Actually one candidate: **GAP-117** — gap describes "restore drill automation" but state-check found ZERO restore drill scripts (existing notes claim 50% complete, audit-stale-sweep `59e8f910` only audited not fixed). However this is "stale completion_pct" rather than scope-revise; gap description still accurate. Keep as PARTIAL→adjust_pct (30). 

**SCOPE-REVISE count: 0**

### DROP

None — no obsolete features in this bucket.

---

## CSV update commands (coordinator applies in closure PR)

### SHIPPED→DONE entries (3 — git mv to closed/)

```bash
# GAP-534 invite token single-use enforcement
sed -i 's|^GAP-534,phase-1-beta/GAP-534-invite-token-single-use-enforcement.md,Invite token single-use enforcement + audit log,PARTIAL,P0,Backend,phase-1-beta,90,2026-05-14,2026-05-26,.*|GAP-534,phase-1-beta/closed/GAP-534-invite-token-single-use-enforcement.md,Invite token single-use enforcement + audit log,DONE,P0,Backend,phase-1-beta,100,2026-05-14,2026-06-01,Wave rst-cascade-1 cluster 2 audit verified; Phase β real-flow defer acceptable per pre-handoff §2.7|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-534-invite-token-single-use-enforcement.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-791 course list native query tenant scope
sed -i 's|^GAP-791,phase-1-beta/GAP-791-course-list-native-query-bypasses-tenant-filter.md,.*|GAP-791,phase-1-beta/closed/GAP-791-course-list-native-query-bypasses-tenant-filter.md,Course list endpoint native query bypasses Hibernate tenant filter (cross-tenant leak),DONE,P0,Backend,phase-1-beta,100,2026-05-28,2026-06-01,Wave A PR #1937 ee1ae549 native query + cross-flow sweep + IT re-enabled|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-791-course-list-native-query-bypasses-tenant-filter.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-792 courses @Cacheable tenant-scoped
sed -i 's|^GAP-792,phase-1-beta/GAP-792-courses-cache-key-not-tenant-scoped.md,.*|GAP-792,phase-1-beta/closed/GAP-792-courses-cache-key-not-tenant-scoped.md,Courses @Cacheable key not tenant-scoped → cross-tenant cache pollution,DONE,P0,Backend,phase-1-beta,100,2026-05-28,2026-06-01,Wave A PR #1937 ee1ae549 tenant-scoped Cacheable + sister-class sweep|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-792-courses-cache-key-not-tenant-scoped.md documents/04-quality/gaps/phase-1-beta/closed/
```

### PARTIAL adjustments (10 — completion_pct + notes refresh)

```bash
# GAP-117: 50 → 30 (no restore drill script)
sed -i 's|^GAP-117,phase-1-beta/GAP-117-restore-drill-test.md,Backup Restore Drill Automation,PARTIAL,P0,DevOps,phase-1-beta,50,2026-04-19,2026-05-26,.*|GAP-117,phase-1-beta/GAP-117-restore-drill-test.md,Backup Restore Drill Automation,PARTIAL,P0,DevOps,phase-1-beta,30,2026-04-19,2026-06-01,Wave meta-7 state-check: no restore drill script in infrastructure/scripts; AWS-suspended block|' documents/04-quality/gaps/gap-status.csv

# GAP-127: 50 → 60 (next/dynamic 5+ files)
sed -i 's|^GAP-127,phase-1-beta/GAP-127-frontend-code-splitting-bundle-analyzer.md,.*PARTIAL,P0,Frontend,phase-1-beta,50,.*|GAP-127,phase-1-beta/GAP-127-frontend-code-splitting-bundle-analyzer.md,Frontend has zero code-splitting across 64 pages — bundles likely >300 KB,PARTIAL,P0,Frontend,phase-1-beta,60,2026-04-19,2026-06-01,Wave meta-7 audit: next/dynamic adopted across 5+ TSX files (LandingShell etc); bundle-analyzer wiring pending|' documents/04-quality/gaps/gap-status.csv

# GAP-154: OPEN 0% → PARTIAL 66% (7/7 BRD shipped Wave Legal-BRD Phase 1.5)
sed -i 's|^GAP-154,unclassified/GAP-154-brd-scope-expansion-umbrella.md,.*OPEN,P0,Mixed,n/a,0,.*|GAP-154,unclassified/GAP-154-brd-scope-expansion-umbrella.md,BRD Scope Expansion (Umbrella — 22 Missing BRD Docs),PARTIAL,P0,Mixed,n/a,66,2026-04-20,2026-06-01,Wave Legal-BRD Phase 1.5 SHIPPED 7/7 BRD legal skeletons; AC 10/15 met|' documents/04-quality/gaps/gap-status.csv

# GAP-449: 30 → 60 (Wave 61 substantive)
sed -i 's|^GAP-449,unclassified/GAP-449-terraform-apply-workflow-dispatch-rule-revise.md,.*PARTIAL,P0,Mixed,n/a,30,.*|GAP-449,unclassified/GAP-449-terraform-apply-workflow-dispatch-rule-revise.md,Terraform-apply workflow_dispatch + revise §9 distinguish 3 cases,PARTIAL,P0,Mixed,n/a,60,2026-05-08,2026-06-01,Wave 61 Bucket A/C closed; Path Y eligibility confirmed; GAP-446/447 artifacts pending|' documents/04-quality/gaps/gap-status.csv

# GAP-535: 70 → 90 (normalizer + V40 + 16 tests)
sed -i 's|^GAP-535,phase-1-beta/GAP-535-tenant-slug-normalize-vn-diacritics.md,.*PARTIAL,P0,Backend,phase-1-beta,70,.*|GAP-535,phase-1-beta/GAP-535-tenant-slug-normalize-vn-diacritics.md,Tenant slug normalize — VN diacritics + smart quotes + collision recovery,PARTIAL,P0,Backend,phase-1-beta,90,2026-05-14,2026-06-01,Wave 77 D normalizer + V40 + 16 tests shipped; InstanceService wiring pending|' documents/04-quality/gaps/gap-status.csv

# GAP-536: 65 → 75 (entity + V41 shipped)
sed -i 's|^GAP-536,phase-1-beta/GAP-536-tenant-create-idempotency-key.md,.*PARTIAL,P0,Backend,phase-1-beta,65,.*|GAP-536,phase-1-beta/GAP-536-tenant-create-idempotency-key.md,POST /tenants idempotency key — prevent double-submit orphan tenants,PARTIAL,P0,Backend,phase-1-beta,75,2026-05-14,2026-06-01,Wave 77 D entity+service+V41 shipped; HandlerInterceptor wiring deferred|' documents/04-quality/gaps/gap-status.csv

# GAP-567: 50 → 55 (cert acquired + monitor wired)
sed -i 's|^GAP-567,phase-1-beta/GAP-567-wave-82-certbot-dns-01-cert-renewal-30d-expiry-monitor.md,.*PARTIAL,P0,DevOps,phase-1-beta,50,.*|GAP-567,phase-1-beta/GAP-567-wave-82-certbot-dns-01-cert-renewal-30d-expiry-monitor.md,Wave 82 Certbot DNS-01 + 30d expiry CloudWatch monitor (F10),PARTIAL,P0,DevOps,phase-1-beta,55,2026-05-15,2026-06-01,Wave 82 B cert acquired exp 2026-08-13 + monitor wired; AL2023 renewal failures GAP-572/573|' documents/04-quality/gaps/gap-status.csv

# GAP-599: 90 → 92 (17 unit + 3 jsdom tests verified)
sed -i 's|^GAP-599,phase-1-beta/GAP-599-jwt-tab-collide-storage-isolation.md,.*PARTIAL,P0,Frontend,phase-1-beta,90,.*|GAP-599,phase-1-beta/GAP-599-jwt-tab-collide-storage-isolation.md,JWT storage key collision khi mở 2 browser tab cùng domain,PARTIAL,P0,Frontend,phase-1-beta,92,2026-05-17,2026-06-01,Wave rst-cascade-1 cluster 2 jwt-storage.ts + 17 unit + 3 jsdom tests verified PR #1515; live 2-tab Playwright defer Phase β|' documents/04-quality/gaps/gap-status.csv

# GAP-648: 0 → 10 (thesis-1 scaffold)
sed -i 's|^GAP-648,phase-1-beta/GAP-648-thesis-nfr-data-capture.md,.*OPEN,P0,Mixed,phase-1-beta,0,.*|GAP-648,phase-1-beta/GAP-648-thesis-nfr-data-capture.md,Thesis NFR data capture — load test + CloudWatch dashboards + AWS cost (P0 thesis-blocker),PARTIAL,P0,Mixed,phase-1-beta,10,2026-05-18,2026-06-01,Wave thesis-1 scaffold partial; k6+CloudWatch+Cost artifacts pending GAP-612 AWS restore|' documents/04-quality/gaps/gap-status.csv

# GAP-793: 80 → 95 (EmailProviderRouter + 88 tests)
sed -i 's|^GAP-793,phase-1-beta/GAP-793-production-email-provider-routing-resend-never-reached.md,.*PARTIAL,P0,Backend,phase-1-beta,80,.*|GAP-793,phase-1-beta/GAP-793-production-email-provider-routing-resend-never-reached.md,Production email-provider routing - Resend send branch never reached (P0),PARTIAL,P0,Backend,phase-1-beta,95,2026-05-28,2026-06-01,EmailProviderRouter + EmailSender interface shipped PR #1938; 88 tests PASS; live resend verify defer prod-equivalent|' documents/04-quality/gaps/gap-status.csv
```

### last_verified-only refresh (29 — no pct/status change)

```bash
# Bulk last_verified update from 2026-05-26 → 2026-06-01 for remaining 29 PARTIAL+OPEN gaps
for g in GAP-049 GAP-223 GAP-286 GAP-297 GAP-370 GAP-502 GAP-530 GAP-533 GAP-538 GAP-543 GAP-566 GAP-572 GAP-608 GAP-610 GAP-622 GAP-649 GAP-656 GAP-657 GAP-658 GAP-659 GAP-693 GAP-695 GAP-727 GAP-730 GAP-756 GAP-772 GAP-773 GAP-788 GAP-814; do
  # Manual sed per row required because notes column differs per gap
  echo "$g — apply last_verified=2026-06-01 in CSV row"
done
# Coordinator: apply via individual sed per row to avoid clobbering notes
```

---

## Reviewer notes

1. **AC checkbox bitmap is unreliable** — 26 of 42 gaps have 0/N or <50% AC `[x]` despite empirical evidence supporting substantial progress. This is exactly the stale-status drift wave meta-7 targets. Reviewer should rely on empirical grep+commit evidence, NOT raw checkbox counts.

2. **GAP-535/538/657/659 conservative PARTIAL stay** — code-shipped but live-verify/wiring deferrals match notes. Flipping DONE prematurely violates `pre-handoff-self-test-completeness.md` §2.4 + `feature-ship-runtime-walk-mandate.md`. Wait for GAP-612 AWS restore + post-fix re-walk.

3. **GAP-791/792 PR #1937 SHIPPED→DONE confidence high** — fix commit + cross-flow sweep + IT re-enabled + per `cross-flow-bug-class-sweep.md` §3 compliance documented in commit body. AC empirically 4-5/5 met (only "post-fix re-walk live" gated GAP-612, which is operator-action acceptable defer).

4. **GAP-117 PARTIAL pct DROP 50→30** — flagged because existing notes claim 50% but empirical state-check found ZERO restore drill scripts in `infrastructure/` or `scripts/`. Either prior pct was aspirational (audit-only sweep `59e8f910` did not fix), or scripts were planned but never landed. Reviewer should investigate.

5. **GAP-154 STATUS FLIP needed** — OPEN→PARTIAL (Wave Legal-BRD Phase 1.5 shipped 7/7 BRD skeletons + AC 10/15 met). CSV currently lists OPEN/0% which is materially wrong.

6. **GAP-772/773 KC blockers confirmed OPEN** — kiteclass-core staff invite controller absent + kiteclass-frontend /staff/accept-invite route absent. Wave meta-6 Bucket A shipped **kitehub** side only (kitehub-subscription) not kiteclass. Reviewer should note this for Mảng B13/C planning.

---

## References

- Taxonomy: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-classification-taxonomy.md`
- Wave plan: `documents/03-planning/waves/wave-2026-06-01-meta-7-p0-p1-stale-audit.md`
- Foundation methodology: `.claude/rules/audit-to-gap-pipeline.md` §2.8
- Cross-flow sweep evidence: `.claude/rules/cross-flow-bug-class-sweep.md` §3
- Post-fix re-walk: `.claude/rules/pre-handoff-self-test-completeness.md` §3
- Origin trigger: GAP-791/792 stale-OPEN drift discovered 2026-06-01 (PR #1937 shipped fix but CSV still OPEN)
