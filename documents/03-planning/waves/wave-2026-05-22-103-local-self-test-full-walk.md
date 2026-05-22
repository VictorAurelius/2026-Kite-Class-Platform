---
title: Wave 103 — Local self-test full walk (AWS-independent path to beta-ready)
status: draft
created: 2026-05-22
updated: 2026-05-22
waves: [103]
gaps: [GAP-518, GAP-519, GAP-531, GAP-538, GAP-516, GAP-543, GAP-657, GAP-659, GAP-637, GAP-620, GAP-693, GAP-695]
audience: dev
---

# Wave 103 — Local self-test full walk (AWS-independent path to beta-ready)

**Goal:** Close 8 PARTIAL self-test gaps via **LOCAL** persona walks (no AWS dependency) → có browser screenshot evidence + Mailhog email evidence cho Owner + Admin persona end-to-end → ready cho beta cohort invite NGAY khi AWS account 906286017800 restore (chỉ deploy + smoke prod path còn ~2h). Đồng thời draft GAP-693 AWS rebuild SOP playbook (~70% ready) → fast restore khi Ginnette confirm verification.

**Trigger:** User direction 2026-05-22 post Wave 102.9 closure "self-test sớm nhất có thể". Wave 102.9 ship 4 docs-only state-check (GAP-637 60% / GAP-620 0% / GAP-531 50% / GAP-538 95% / GAP-516 80% / GAP-543 80% / GAP-657 80% / GAP-659 80%) — all live verify blocked by GAP-612 AWS suspension. Wave 103 unlock blocker = pivot từ "wait for AWS" sang "verify locally first".

**Estimated wall-clock:** ~3h critical path (Bucket E sequential first ~10min → A+B+C+D+F song song; Bucket A longest ~2h).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**

- **Inside-out source:** GAP-695 self-test catalog Tier 2 + Tier 3 (filed 2026-05-21 từ outside-in synthesis 3-agent F-1 + E-1 + P-1) + Wave 102.9 state-check audit findings (4 PRs documented all PARTIAL gaps retain code-AC từ Wave 78/79/89/98 nhưng live verify pending).
- **Outside-in skip rationale per `outside-in-coverage-trigger.md` §4 exception:** outside-in audit ALREADY happened 2026-05-21 (≤30 days) cho cluster GAP-692/693/694/695 (3-agent synthesis). User explicit "lock scope Wave 103, self-test sớm nhất" 2026-05-22 = scope acknowledgment per §3 Bước 5. Per §5 banned shortcut — skip documented inline với rationale.
- **Inside-out queue check:** `documents/03-planning/inside-out-queue.md` consulted — 4 queued items (Premium plan / Feedback channel / User manual) đều có own wave assignment; KHÔNG có item nào liên quan self-test cohort. No new inside-out items.
- **Personas served:** Phase 1 BETA Owner (chị Hằng) signup → admin approve → tenant init handoff → onboarding wizard 5-step → seed VN sample → invite staff (email tone). Admin (anh Kiệt) RBAC checks + approve flow + 2FA. Beta tenant nhận 5 email types (welcome / approval / invite / verify / password-reset) Vietnamese-first.
- **AWS-independence rationale:** GAP-612 AWS suspended state ~5%; Ginnette reply pending hours-days. Đợi AWS = block beta cohort velocity. Local stack covers 95% verify scope (signup logic, tenant init logic, RBAC, 2FA, email render). Chỉ prod-specific items (DNS cutover smoke, SES production send, CloudWatch alarm fire) cần AWS — defer Wave 104.

**Q2 (trade-offs):**

- **Wait-AWS approach (defer Wave 103 until 612 restore):** REJECTED. AWS restore ETA unknown (rep reply pending). Local verify = decoupled critical path, no calendar dependency.
- **Local-only without rebuild SOP draft (drop Bucket F):** REJECTED. When AWS restores, fast deploy depends on rebuild SOP ready. Bucket F = no-AWS-needed docs work; parallelize with verify buckets.
- **6 buckets parallel (split A into A1 Admin login + A2 RBAC):** REJECTED. Max 4-5 per `feedback_parallel_agent_strategy.md`. 5 buckets safer cohesion (A keeps owner-persona flow intact).
- **Single mega-bucket sequential walk (Owner + Admin in 1 agent):** REJECTED. Persona walks disjoint enough (Owner=tenant scope; Admin=platform scope) → parallel saves 1.5-2h wall-clock.
- **Include GAP-695 Tier 3 polish (VN data realism + GAP-138/139 FE):** REJECTED. Polish KHÔNG block self-test execution; Wave 104+ candidate. Wave 103 = "self-test runnable end-to-end", polish = "self-test pretty enough for beta cohort UX" — different bar.
- **Mailhog vs SES sandbox cho email verify:** Mailhog WINS for local. SES sandbox requires AWS up. Mailhog already in `docker-compose.kitehub.yml`, smoke-tested Wave 102.8.

**Q3 (risks + recovery):**

- **Local stack flaky during 5-bucket parallel (Postgres connection exhaustion / Redis OOM):** Recovery = `bash kitehub/scripts/down.sh && bash kitehub/scripts/up.sh full` + 60s warmup; if persists, serialize Bucket A+B (both touch kitehub-subscription DB heavily).
- **FE container image stale post Wave 102.8.1 rebuild:** Mitigation = Bucket A+B agents run `docker images | grep kitehub-frontend` first; if stale (>24h) rebuild `bash kitehub/scripts/rebuild.sh kitehub-frontend` before persona walks. Wave 102.8.1 already shipped fresh image; coordinator verify timestamp.
- **2FA TOTP local fixture (Google Authenticator) — agent can't scan QR:** Recovery = Bucket C uses `pyotp` Python equivalent OR fixed secret `JBSWY3DPEHPK3PXP` (RFC 6238 test vector) to compute 6-digit code programmatically. Document fixture in audit artifact.
- **Email template render fails on Mailhog (Thymeleaf compile error):** Recovery = `docker logs kitehub-email | grep ERROR`; if template syntax bug → fix in same PR (Bucket D scope) + file `kitehub-email` rebuild step.
- **Concurrent gap-status.csv writes (5 buckets touching different rows):** Mitigation = bucket-to-row mapping non-overlapping (A→GAP-637/620/518/519, B→GAP-531/538, C→GAP-516, D→GAP-543/657/659, F→GAP-693). Coordinator rebase resolution per Wave 102.8 lesson if race.
- **GAP-693 SOP draft scope creep (each step has sub-failure-mode):** Recovery = Bucket F caps at ~70% (13 steps outline + 5 gates table + 8 failure-modes one-liner each); detailed runbook deferred to AWS-restore-day session.
- **AWS restore happens mid-Wave 103 (Ginnette replies during execution):** Mitigation = continue Wave 103 (local verify still valuable); spawn separate "AWS restoration smoke" follow-up wave when account active; do NOT pivot mid-execution.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? | Order |
|--------|--------|-------|--------|-----------|:-----:|
| **E** | GAP-695 Tier 0 verify | bg-agent | ~10min | ✅ infra only (`up.sh full` + health check) | **FIRST** (sequential, gates A/B/C/D) |
| A | GAP-637 + GAP-620 + GAP-518 followup + GAP-519 followup | bg-agent (Opus medium) | ~1.5h | ✅ kitehub-admin + branding + subscription admin scope | parallel batch 2 after E |
| B | GAP-531 + GAP-538 | bg-agent (Opus full) | ~2h | ✅ kitehub-subscription tenant + kitehub-frontend onboarding | parallel batch 2 after E |
| C | GAP-516 | bg-agent (Opus medium) | ~45min | ✅ kitehub-subscription auth/twofactor + FE 2fa route | parallel batch 2 after E |
| D | GAP-543 + GAP-657 + GAP-659 | bg-agent (Opus medium) | ~1h | ✅ kitehub-email templates + headers + Mailhog | parallel batch 2 after E |
| F | GAP-693 (AWS rebuild SOP) | bg-agent (Sonnet) | ~1.5h | ✅ docs only `documents/05-guides/deploy/aws-rebuild-sop.md` | parallel batch 2 after E (no infra dep) |

**Disjoint check:**

- Bucket E = `up.sh full` execution + health check audit doc — disjoint everything else
- Bucket B + C cùng touch `kitehub-subscription` package nhưng:
  - B = tenant + onboarding scope (`TenantInitController`, `OnboardingController`, `TenantAdminService`)
  - C = 2FA scope (`AuthController.twoFactorChallenge()`, `TwoFactorService`)
  - File-disjoint within subscription package
- Bucket A admin scope includes 3 services (admin / branding / subscription admin sub-package) — does NOT overlap B (tenant) hoặc C (2FA auth)
- Bucket F docs only — never overlaps code buckets
- Bucket D email scope isolated (`kitehub-email` service)

**Spawn dependency:** Bucket E ship FIRST (~10min) → local stack healthy 8/9 services (kite-postgres / redis / rabbitmq / minio / mailhog / gateway / subscription / admin / branding / frontend / email) → A/B/C/D/F có thể execute. Without E first, all live verify fail.

---

## 3. Scope (compact schema)

**Stake tier:** MEDIUM-HIGH → model: Opus medium default; Bucket B Opus full (tenant init complexity + cross-FE/BE handoff); Bucket F Sonnet (docs work).
**Cross-layer? PARTIAL** — Bucket B touch FE (onboarding wizard) + BE (tenant init endpoint). `api-contract.md` cho cả 2 domain đã có (Wave 78+98). Per `contract-first-for-cross-layer.md` §2: Bucket 0 Foundation NOT required; reviewer verify api-contract reference trong Bucket B AC.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **E** | GAP-695 Tier 0 verify | 🟠 P1 | none (script execution + audit doc only — `documents/04-quality/audits/local-stack/2026-05-22-wave-103-stack-up-smoke.md`) | **FIRST sequential** (unblocks A/B/C/D/F gateway+stack-dependent verify) |
| 1 | **A** | GAP-637 + GAP-620 + GAP-518 followup + GAP-519 followup | 🔴 P0 | `kitehub/kitehub-admin/src/main/java/.../controller/**`, `kitehub/kitehub-branding/src/main/java/.../controller/**`, `kitehub/kitehub-subscription/src/main/java/.../controller/admin/**`, matching IT, browser walk audit `documents/04-quality/audits/local-stack/2026-05-22-wave-103-admin-persona-walk.md` | Parallel batch 2 after E |
| 2 | **B** | GAP-531 + GAP-538 | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/.../tenant/**` + `kitehub/kitehub-frontend/src/app/(onboarding)/**` + `kitehub/scripts/seed-day1-data.sh` + Playwright `e2e/onboarding-wizard.spec.ts` + browser walk audit `documents/04-quality/audits/local-stack/2026-05-22-wave-103-owner-persona-walk.md` | Parallel batch 2 after E |
| 3 | **C** | GAP-516 | 🟠 P1 | `kitehub/kitehub-subscription/src/main/java/.../auth/twofactor/**` + `kitehub/kitehub-frontend/src/app/(auth)/2fa/**` + 2FA walk audit `documents/04-quality/audits/local-stack/2026-05-22-wave-103-2fa-totp-walk.md` | Parallel batch 2 after E |
| 4 | **D** | GAP-543 + GAP-657 + GAP-659 | 🟠 P1 | `kitehub/kitehub-email/src/main/resources/templates/**` + `kitehub/kitehub-email/src/main/java/.../config/EmailHeadersConfig.java` + Mailhog evidence audit `documents/04-quality/audits/local-stack/2026-05-22-wave-103-email-mailhog-verify.md` | Parallel batch 2 after E |
| 5 | **F** | GAP-693 | 🔴 P0 | `documents/05-guides/deploy/aws-rebuild-sop.md` (new file) + cross-link `documents/05-guides/operations/incident-response-runbook.md` | Parallel batch 2 after E (no infra dep) |

### Bucket E — Local stack-up smoke (GAP-695 Tier 0 verify)

- Files: none (script execution) + audit `documents/04-quality/audits/local-stack/2026-05-22-wave-103-stack-up-smoke.md`
- Steps:
  - `bash kitehub/scripts/down.sh` (clean slate)
  - `bash kitehub/scripts/up.sh full` (start all services)
  - `bash kitehub/scripts/wait-for-healthy.sh` (block until 9/9 healthy or timeout 180s)
  - `bash kitehub/scripts/status.sh` (snapshot service state)
  - `docker ps --format '{{.Names}}\t{{.Status}}' | grep -E "kite-|kitehub-|kiteclass-"`
- Verify endpoints (Tier 1 reachability):
  - `curl -sI http://localhost:9000/actuator/health` → 200 (gateway)
  - `curl -sI http://localhost:8088/actuator/health` → 200 (platform direct)
  - `curl -sI http://localhost:3000` → 200 (frontend SSR)
  - `curl -sI http://localhost:8025` → 200 (Mailhog UI)
- Acceptance:
  - [ ] 9/9 services Up + healthy via `wait-for-healthy.sh`
  - [ ] Audit artifact written với service table + endpoint table + total time elapsed
  - [ ] If any service fails → file P0 follow-up gap, ship Bucket E PARTIAL với recovery notes
  - [ ] GAP-695 Tier 0 verify ✅ flag (parent gap status không flip per phase boundaries)

### Bucket A — Admin persona local walk (GAP-637 + GAP-620 + GAP-518 + GAP-519 followups)

- Files: per §3 row 1 globs
- Steps:
  - Verify Wave 102.9 PR #1705 code-AC: `@PreAuthorize("hasRole('ADMIN')")` on all `/api/v1/admin/*` controllers
  - Run 403 ITs: `cd kitehub && ./mvnw -pl kitehub-admin,kitehub-branding,kitehub-subscription verify -P strict-warnings`
  - Browser walk via headless curl + Playwright (whichever cleaner):
    - Login as seeded `admin@kitehub.com / Admin@KiteHub123` → JWT issued
    - GET `/admin` dashboard → render OK (not 403/redirect)
    - GET `/admin/beta-requests` → list render với pending requests
    - POST `/api/v1/admin/beta-requests/{id}/approve` Bearer JWT → 200 + UI updates
    - Non-admin JWT (seed `owner-test@kitehub.com`) → POST admin endpoint → 403
  - Sidebar navigation: 3 admin links visible (Branding / Beta requests / Audit log) per GAP-519
- Acceptance:
  - [ ] `./mvnw verify -P strict-warnings` PASS với 0 test failures
  - [ ] 403 IT pass cho 3 services
  - [ ] Browser walk evidence: 4 curl PASS + 1 expected 403
  - [ ] Sidebar nav 3 links verify (screenshot HTML grep) per GAP-519
  - [ ] Audit artifact `documents/04-quality/audits/local-stack/2026-05-22-wave-103-admin-persona-walk.md` với evidence per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g)
  - [ ] GAP-637 PARTIAL 60% → DONE 100% (local-verify path); GAP-620 OPEN → DONE 100% (local); GAP-518 PARTIAL 99% → confirmed DONE 100%; GAP-519 confirmed DONE 100%
  - [ ] Note retained: AWS prod live verify defer Wave 104 post-restore

### Bucket B — Owner persona local walk (GAP-531 + GAP-538)

- Files: per §3 row 2 globs
- Steps:
  - Verify FE image fresh (per Wave 102.8.1 rebuild) — `docker inspect kitehub-frontend | grep Created`; if >24h stale → `bash kitehub/scripts/rebuild.sh kitehub-frontend`
  - Curl signup flow: `POST /api/v1/auth/register` Owner persona → 201 + beta request row
  - Switch to admin context (token from seeded admin) → approve beta request → tenant created
  - Switch to new owner context (login first) → tenant init handoff:
    - GET `/api/v1/tenants/me` → tenant_id + subdomain returned
    - Subdomain resolution local: `curl http://${subdomain}.localhost:8088/actuator/health` → 200
  - Onboarding wizard 5-step browser walk (Playwright `onboarding-wizard.spec.ts`):
    - Step 1: tên trung tâm (sample: `Trung tâm Anh ngữ Sky Education`)
    - Step 2: địa chỉ (sample: `123 Lê Lợi, Q.1, TP.HCM`)
    - Step 3: branding upload OR skip (use default)
    - Step 4: import data opt-in (mock CSV `seed-day1-data.sh` output)
    - Step 5: confirm + redirect dashboard
  - DB verify: `tenants` + `tenant_admins` rows seeded + `students.json` ≥3 rows VN sample (`Trần Thị Hồng`, etc.)
- Acceptance:
  - [ ] Curl signup → 201
  - [ ] Admin approve → tenant created
  - [ ] Subdomain resolution local → 200
  - [ ] Playwright `onboarding-wizard.spec.ts` PASS 5 steps
  - [ ] DB seed verify (`docker exec kite-postgres psql -d kitehub -c "SELECT name FROM students WHERE tenant_id = ..."`) → ≥3 VN-formatted rows
  - [ ] VN narrative + sample data per `vn-localization-audit-checklist.md` §2-§3 checklist 16/16 sections PASS
  - [ ] Audit artifact `documents/04-quality/audits/local-stack/2026-05-22-wave-103-owner-persona-walk.md` với 7-row checklist per `pre-handoff-self-test-completeness.md` §2.1
  - [ ] GAP-531 PARTIAL 50% → DONE 100% (local); GAP-538 PARTIAL 95% → DONE 100% (local)
  - [ ] Note retained: prod URL `*.kitehub.me` subdomain resolution defer Wave 104

### Bucket C — 2FA TOTP local walk (GAP-516)

- Files: per §3 row 3 globs
- Steps:
  - Seed admin user with TOTP enabled (V60 migration or seed script — verify schema present)
  - Setup flow: admin first login → `/api/v1/auth/2fa/setup` → QR code data URL returned (base64)
  - Compute 6-digit code programmatically: `python3 -c "import pyotp; print(pyotp.TOTP('JBSWY3DPEHPK3PXP').now())"` (RFC 6238 test secret)
  - Verify setup: `POST /api/v1/auth/2fa/verify` với 6-digit → 200 + 2FA enabled flag in DB
  - Challenge flow next login:
    - `POST /api/v1/auth/login` → 202 + `challenge_id`
    - `POST /api/v1/auth/2fa/verify` `{challenge_id, code}` → 200 + JWT
  - Alternative path nếu TOTP infra incomplete: document disable trong `application-dev.yml` profile + file follow-up
- Acceptance:
  - [ ] Setup endpoint reachable + QR data URL valid (decode + verify TOTP shared secret)
  - [ ] Challenge flow walk: login 202 → verify 200 → JWT
  - [ ] OR fallback path: dev profile disable doc'd + follow-up gap filed
  - [ ] Audit artifact `documents/04-quality/audits/local-stack/2026-05-22-wave-103-2fa-totp-walk.md` với 5-row checklist
  - [ ] GAP-516 PARTIAL 80% → DONE 100% (verify path) HOẶC PARTIAL 90% (disable doc path with follow-up)

### Bucket D — Email Mailhog verify (GAP-543 + GAP-657 + GAP-659)

- Files: per §3 row 4 globs
- Steps:
  - Trigger each of 5 email types via API:
    - Welcome: `POST /api/v1/auth/register` (signup confirmation flows back to welcome)
    - Approval: admin approve beta request → approval email
    - Staff-invite: `POST /api/v1/tenants/{id}/staff-invite` (formal Owner persona)
    - Password-reset: `POST /api/v1/auth/password-reset/request`
    - 2FA-challenge: login flow above
  - Mailhog inspect (http://localhost:8025/api/v2/messages):
    - 5 messages received
    - Each has both HTML + plain-text variant (`Content-Type: multipart/alternative`)
    - Headers: `List-Unsubscribe` + `Reply-To` set
    - VN narrative content per `vn-localization-audit-checklist.md` §2 (Vietnamese label, persona-specific greeting)
    - VN sample data (`Trung tâm Sky Education`, `Trần Thị Hồng`) NOT Lorem Ipsum
  - Persona-tone split verify (GAP-659): Owner staff-invite vs Teacher get-invite — formal `Em chào chị/anh` vs casual `Chào em`
- Acceptance:
  - [ ] 5 email types render PASS qua Mailhog HTTP API
  - [ ] `List-Unsubscribe` + `Reply-To` headers verified mỗi template
  - [ ] Plain-text fallback exists mỗi template (multipart/alternative present)
  - [ ] VN narrative + sample data 5/5 PASS per `vn-localization-audit-checklist.md` §2-§3
  - [ ] Persona-tone split confirmed cho staff-invite (2 variants Owner vs Teacher)
  - [ ] Audit artifact `documents/04-quality/audits/local-stack/2026-05-22-wave-103-email-mailhog-verify.md`
  - [ ] GAP-543 PARTIAL 80% → DONE 100%; GAP-657 PARTIAL 80% → DONE 100%; GAP-659 PARTIAL 80% → DONE 100%

### Bucket F — AWS rebuild SOP playbook draft (GAP-693)

- Files: `documents/05-guides/deploy/aws-rebuild-sop.md` (new) + cross-link `documents/05-guides/operations/incident-response-runbook.md` §rebuild section
- Steps (no infra needed — pure docs work):
  - Outline 13 steps (rebuild from suspended → fully operational):
    1. Account verification re-confirmed (Ginnette reply confirm)
    2. Re-verify CloudTrail observability baseline per `aws-observability-first.md`
    3. State backend re-bootstrap (S3 + DynamoDB lock — chicken-and-egg)
    4. OIDC IAM role re-apply
    5. ECR registry re-confirm + re-push image tags (last green)
    6. Secrets Manager re-populate (JWT_SECRET, ENCRYPTION_MASTER_KEY, DB password)
    7. RDS re-snapshot OR re-create via Flyway (state decision based on whether RDS instance retained or wiped)
    8. EC2 user_data re-apply (terraform-apply.yml workflow_dispatch confirm input)
    9. ALB + target group re-create
    10. DNS cutover Cloudflare (apex + subdomains)
    11. SES production-access resubmit case 177857212400418
    12. Smoke test prod path (signup → admin approve → tenant init → invite staff)
    13. Beta cohort invite trigger
  - Add 5 gates table (each gate = blocking checkpoint before next step):
    - Gate 1 (post step 2): CloudTrail `IsLogging=true`
    - Gate 2 (post step 4): OIDC role assume successful
    - Gate 3 (post step 7): RDS healthy + Flyway baseline migrations applied
    - Gate 4 (post step 9): ALB target healthy 2/2
    - Gate 5 (post step 12): Smoke test PASS (admin login + Owner signup + email send)
  - Add 8 failure-mode prevention (one-liner each):
    - FM1: CloudTrail bootstrap miss → cross-link `aws-observability-first.md`
    - FM2: Terraform state lock collision → DynamoDB lock release procedure
    - FM3: Secrets prefix mismatch → kitehub/production/* vs kite/prod/* per Wave 64 lesson
    - FM4: IAM role tag drift → `default_tags` Project=Kite verify
    - FM5: EC2 user_data + SSM SendCommand concurrent → `concurrent-production-mutation-ops.md` §2 wait-pattern
    - FM6: ECR image-tag missing → audit last green push timestamp
    - FM7: DNS TTL too high → reduce to 60s 24h before cutover
    - FM8: Vercel deployment rate limit → check 100 deploy/day quota pre-trigger
- Acceptance:
  - [ ] File `aws-rebuild-sop.md` created với 13 steps outlined + 5 gates table + 8 failure-mode one-liners
  - [ ] Cross-link from `incident-response-runbook.md` §rebuild section
  - [ ] Frontmatter complete per `planning-docs-structure.md` (title, status, created, audience: dev)
  - [ ] GAP-693 OPEN → PARTIAL ~70% (detailed runbook steps deferred Wave 104 post-AWS-restore day)
  - [ ] Note retained: full execution-validated runbook ship Wave 104 sau AWS restore (concrete commands tested live)

---

## 4. Verification Gates

| Gate | Check | When |
|---|---|---|
| Pre-spawn | `git status` clean + branch `wave/103-local-self-test` | Before agent spawn |
| Mid-bucket | Each agent commits to own branch (no shared push) | During execution |
| Post-bucket | Coordinator cherry-pick from agent branches → wave/103 | After each agent done |
| Pre-merge | `./mvnw verify` PASS (Bucket A) + Playwright PASS (Bucket B) + CI green | Before squash-merge |
| Post-merge | Wave 103 closure sync PR (ROADMAP + wave-history + handoff) | After all bucket PRs merged |

---

## 5. Acceptance Criteria (wave-level)

- [ ] Bucket E SHIPPED — local stack 9/9 healthy + audit artifact
- [ ] Bucket A SHIPPED — admin persona walk evidence + GAP-637/620/518/519 DONE local
- [ ] Bucket B SHIPPED — owner persona walk evidence + GAP-531/538 DONE local
- [ ] Bucket C SHIPPED — 2FA TOTP walk evidence + GAP-516 DONE local
- [ ] Bucket D SHIPPED — 5 Mailhog email render evidence + GAP-543/657/659 DONE local
- [ ] Bucket F SHIPPED — AWS rebuild SOP draft ~70% + GAP-693 PARTIAL
- [ ] GAP-695 self-test catalog status flip: PARTIAL 50% → PARTIAL ~85% (Tier 2 done; Tier 3 polish remain Wave 104)
- [ ] Wave 103 closure sync PR merged
- [ ] Self-test demonstration: full Owner + Admin persona walk reproducible local in ~30min (vs ~5h pre-Wave-103)

---

## 6. Out-of-scope (Wave 104 candidates)

- GAP-693 detailed runbook concrete commands (post-AWS-restore execution-validated)
- GAP-684 AWS prod live admin walk (post AWS-restore)
- AWS prod live verify for GAP-637/620/518/519/531/538/516/543/657/659 (all defer Wave 104)
- GAP-698 ops-readiness audit refresh (Wave 102.10 candidate per Wave 102.9 brainstorm)
- GAP-695 Tier 3 polish (VN data realism + GAP-138/139 FE)
- GAP-622 pre-launch readiness blockers consolidation (meta — Wave 104+ post-AWS-restore)

---

## 7. Cross-links

- Parent catalog: `documents/04-quality/gaps/phase-1-beta/GAP-695-self-test-readiness-comprehensive-plan.md`
- Prior wave: `documents/03-planning/waves/wave-2026-05-21-102.9-self-test-tier-2-3.md` (state-check predecessor)
- Inside-out queue: `documents/03-planning/inside-out-queue.md` (consulted, no new items)
- Self-test rule: `.claude/rules/pre-handoff-self-test-completeness.md` §2 checklist per persona class
- AWS context: GAP-612 (suspended account, Ginnette reply pending 2026-05-22)
- Local-self-test rule: `.claude/rules/local-self-test-before-aws-deploy.md` §3 evidence requirement

---

## 8. Log

- **2026-05-22 (draft):** Wave plan filed. Triggered by user direction "lock scope Wave 103, self-test sớm nhất có thể" post Wave 102.9 closure (#1708 merged 02:55 UTC). Scope = 5 verify buckets (E stack-up + A admin + B owner + C 2FA + D email) parallel + F (AWS rebuild SOP draft) — all AWS-independent path. Goal = beta-cohort-ready local NGAY, fast AWS deploy WHEN account restores. Outside-in skip rationale documented §1 Q1 per `outside-in-coverage-trigger.md` §4 exception (≤30 days). Inside-out queue consulted (no new items). Reviewer: @nguyenvankiet (solo-dev draft self-approve; final scope-lock pending user confirm before agent spawn).
