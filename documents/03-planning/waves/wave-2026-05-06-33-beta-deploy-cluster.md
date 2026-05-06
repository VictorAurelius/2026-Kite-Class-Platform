---
title: Wave 33 — Phase 1 BETA deploy cluster — P0 BLOCKING (seed + email + beta invite + DNS/secrets)
status: draft
created: 2026-05-06
updated: 2026-05-06
waves: [33]
gaps: [GAP-376, GAP-370, GAP-372, GAP-369, GAP-379]
---

# Wave 33 — Phase 1 BETA deploy cluster — P0 BLOCKING

**Goal:** Ship tất cả CODE + SCRIPTS + RUNBOOKS cần thiết cho Phase 1 BETA launch: production data seed runner, email beta-invite template + SES production config, beta tenant invite flow (FE + BE), DNS setup runbook + secrets management template. **Closes GAP-376 PARTIAL** (seed runner ships; production execution = user step post-deploy) + **GAP-370 PARTIAL→more-complete** (beta-invite template + SES config docs) + **GAP-372 PARTIAL** (invite flow code shipped; email send depends on production SES active) + **GAP-369 PARTIAL** (DNS runbook + certbot scripts; domain registration = user step) + **GAP-379 PARTIAL** (secrets template + rotation policy docs).
**Trigger:** Wave 32 plan §7 closure recommends "Phase 1 BETA P0 deploy cluster" sau Wave 32 (ai-branding-wizard). Tất cả 4 P0 BLOCKING gaps (GAP-369/370/372/376) plus P1 STRONGLY-recommend GAP-379 chưa started. PDPL deadline 2026-07-01 (~7 tuần countdown) tạo áp lực launch. Drafted PIPELINED per `feedback_pipelined_wave_planning.md` §Step 5.5 — Wave 32 4 agents đang in-flight khi wave này draft (5th consecutive pipelined application).
**Estimated wall-clock:** ~20-25 min/agent parallel. Bucket C nặng nhất (BE entity + FE 3 pages).

---

## 1. Brainstorm

**Q1 (alignment):**
- **Persona:** Platform Coordinator (tôi) + P2 Center Owner (beta invite recipient). Wave này không mang lại feature mới cho end-users ngay — nhưng unlock Phase 1 BETA: không có seed runner thì first deploy fail; không có email thì beta invite fail; không có beta invite flow thì không có tenant nào onboard; không có DNS thì beta URL không accessible.
- **Domain:** Mixed — BE (`kitehub-subscription`, `kitehub-email`) + FE (`kitehub-frontend`) + Infra docs (`documents/05-guides/operations/` + `scripts/`). Multi-domain → KHÔNG eligible `AUDIT_DEFER_DOMAIN_MILESTONE`; audit suite phải run ≤3 ngày post-merge per `post-wave-audit-mandate.md` §2.1.
- **Character of work:** Khác với kit-port waves (UI pixel-push). Wave này là backend entity + FE form + scripts + runbooks. Agents cần `pnpm build` + `mvn verify` đầy đủ per `feedback_agent_local_verify_both_layers.md`.
- **User-executed steps (không automate được):**
  - GAP-369: Đăng ký domain (kitehub.vn / kiteclass.vn) với registrar — agent chuẩn bị hướng dẫn + scripts, user execute.
  - GAP-370: AWS SES sandbox → production approval (Submit form AWS + wait 24-48h) + DKIM/SPF verification — agent tạo config + runbook, user execute.
  - GAP-379: AWS Secrets Manager provisioning — agent tạo template + policy docs, user execute trên AWS console.
  - GAP-376: Production seed execution — agent ships seed runner code, user chạy `scripts/seed-production.sh` khi first deploy.

**Q2 (trade-offs):**
- **PARTIAL acceptance là expected:** Các gaps này sẽ flip PARTIAL không phải DONE vì họ phụ thuộc user-executed production steps. Đây là đúng per `gap-done-discipline.md` §3 — code ships, user runs it later.
- **GAP-370 already PARTIAL:** `SESEmailService.java` + 13 email templates đã tồn tại (Wave 18a Bucket B). Wave 33 Bucket B thêm beta-invite template + beta-request-confirmation template + SES production config docs. Bucket B KHÔNG rewrite existing service — chỉ extend.
- **GAP-376 seed-data.sh EXISTS (local dev):** `kitehub/scripts/seed-data.sh` là local dev seed (HTTP API calls). Wave 33 Bucket A tạo `scripts/seed-production.sh` — production variant: Spring Boot seed runner (`--seed-mode=production`) chạy trực tiếp DB (không qua API), idempotent. Khác nhau: dev script = curl via gateway; prod runner = Spring `CommandLineRunner`.
- **GAP-372 beta invite email dependency:** Bucket C code kết nối với email service nhưng email send thực tế chỉ work khi GAP-370 production SES active. Giải pháp: `EmailClient.sendBetaInvite()` với fallback log "BETA_INVITE_EMAIL_QUEUED: {email}" nếu SES chưa active. Flow vẫn work logic, email delivery deferred.
- **Bucket split (4 buckets disjoint):**
  - **A:** GAP-376 Production seed runner — BE-only, no FE, no email dep.
  - **B:** GAP-370 Email beta templates + SES docs — email-only, no subscription BE, no FE.
  - **C:** GAP-372 Beta tenant invite — BE + FE cross-cutting (subscription entity + auth pages + admin page).
  - **D:** GAP-369 DNS runbook + GAP-379 Secrets template — pure docs/scripts, no Java/TS changes.

**Q3 (risks):**
- **R1: V27/V28 Flyway version race.** Bucket A owns `V27__seed_admin_system_config.sql`; Bucket C owns `V28__create_beta_access_request.sql`. Cần brief cả 2 agents về version assignment TRƯỚC spawn. Coordinator merge A→C sequential đảm bảo không conflict.
- **R2: @EntityScan update cho BetaAccessRequest.** Bucket C thêm entity trong package mới `com.kite.hub.subscription.beta`. Cần update `@EntityScan` trong `KiteHubSubscriptionApplication.java` + potentially `KiteHubAdminApplication.java` per `feedback_admin_scan_packages_after_module_add.md`. Bucket A KHÔNG thêm entity (SeedRunner là `@Component` không phải `@Entity`) → chỉ Bucket C touch Application file.
- **R3: EmailClient cross-service call từ kitehub-subscription.** Bucket C gọi `kitehub-email` via REST (EmailClient) để send invite email. Existing pattern: `BrandingClient.java` trong `kitehub-email`. Nhưng gọi ngược từ `kitehub-subscription` → `kitehub-email` chưa có client. State-check tại agent runtime — nếu không có `EmailClient` trong subscription → Bucket C tạo mới (simple RestClient/Feign stub). OR: Bucket C tạo `BetaEmailEvent` → publish via Outbox → `kitehub-email` subscribes. Khuyến nghị: Outbox pattern per `design-patterns.md` §3.5.
- **R4: Public `/auth/request-beta-access` route conflict.** `(auth)/` route group đang có login/register/verify-email. Bucket C thêm `request-beta-access/page.tsx` + `beta-signup/page.tsx`. Không edit layout.tsx (Bucket C owns new files only, không shared edit). Coordinator merge sau A+B để tránh.
- **R5: DNS/secrets runbooks người dùng execute.** Bucket D tạo runbooks nhưng accuracy phụ thuộc state của Oracle Cloud VM IP + AWS account structure. Agent cần state-check VM IP + AWS account region từ `terraform-aws/` trước khi viết. Nếu không có → placeholder với `[USER_INPUT_REQUIRED]` markers.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | GAP-376 Production seed runner — Spring Boot `CommandLineRunner` + `V27__seed_admin_system_config.sql` + `scripts/seed-production.sh` | bg-agent | ~18-22 min | ✅ `kitehub-subscription/src/main/java/*/seed/` + `V27` + `scripts/seed-production.sh` |
| B | GAP-370 Email beta templates + SES production config runbook | bg-agent | ~12-15 min | ✅ `kitehub-email/src/main/resources/templates/emails/beta-*.html` + `kitehub-email/src/main/resources/application.yml` SES section + `documents/05-guides/operations/email-ses-setup-runbook.md` |
| C | GAP-372 Beta tenant invite flow (BE entity + FE 3 pages) | bg-agent | ~25-30 min | ✅ `kitehub-subscription/*/beta/` + `V28` + `kitehub-frontend/(auth)/request-beta-access/` + `(auth)/beta-signup/` + `(admin)/admin/beta-requests/` |
| D | GAP-369 DNS runbook + GAP-379 Secrets template | bg-agent | ~15-18 min | ✅ `documents/05-guides/operations/dns-setup-runbook.md` + `secrets-management-runbook.md` + `scripts/ssl-cert-setup.sh` + `scripts/check-dns-propagation.sh` + `.env.production.template` |

**Disjoint check:** Mỗi bucket touch độc lập:
- A: `kitehub-subscription/src/main/java/*/seed/` + V27 migration + scripts
- B: `kitehub-email/src/` + email templates + email docs
- C: `kitehub-subscription/src/main/java/*/beta/` + V28 migration + KH frontend auth/admin pages
- D: `documents/05-guides/operations/` + `scripts/ssl-cert-setup.sh` + `.env.production.template`

**Shared edits chưa ổn định:**
- `kitehub-subscription/src/main/resources/db/migration/` — A claims V27, C claims V28 (KHÔNG conflict nếu brief đúng; coordinator merge A trước C)
- `KiteHubSubscriptionApplication.java` — Bucket C ONLY (thêm `@EntityScan` cho `beta` package). Bucket A dùng `@Component` không phải `@Entity` → không cần.
- `kitehub-subscription/pom.xml` — KHÔNG được touch bởi A hay C (existing deps đủ: Spring Data JPA, Spring Security, Spring Web)

**Cross-bucket dependency:** Không có dependency giữa A↔B↔D. Bucket C phụ thuộc GAP-370 email template (Bucket B) NHƯNG chỉ dependency ở production behavior, không phải compile-time → C có thể ship stub email call, B ship template. Coordinator merge B trước C.

---

## 3. Scope (per bucket)

### Bucket A — GAP-376 Production Data Seed

- **Spec source:** `documents/04-quality/gaps/GAP-376-production-data-seed.md` + Proposed Fix §Option B (Spring Boot seed runner)
- **State-check findings:**
  - `kitehub/scripts/seed-data.sh` ✅ EXISTS — local dev seed via curl/HTTP. Wave 33 tạo production variant khác biệt.
  - Latest migration: `V26__create_dsar_ticket.sql`. **Bucket A owns V27.**
  - No existing `SeedRunner` or production seed class found.
- **Files to create:**
  - `kitehub-subscription/src/main/java/com/kite/hub/subscription/seed/ProductionSeedRunner.java` — `@Component` + `ApplicationRunner`, đọc `--seed-mode=production` flag; idempotent (check trước khi insert)
  - `kitehub-subscription/src/main/java/com/kite/hub/subscription/seed/SeedProperties.java` — `@ConfigurationProperties("kite.seed")` với `mode` + `adminEmail` + `adminPassword`
  - `kitehub-subscription/src/main/resources/db/migration/V27__seed_admin_system_config.sql` — system_config records: default tier FREE, default currency VND, default locale vi (INSERT IF NOT EXISTS pattern)
  - `scripts/seed-production.sh` — wrapper script: check DB connection → run seed runner via gateway or direct Spring
- **Seed data scope:**
  - Admin user: `admin@kitehub.vn` với role `PLATFORM_ADMIN` (password từ env var `SEED_ADMIN_PASSWORD`)
  - System config records: `default_tier=FREE`, `default_currency=VND`, `default_locale=vi`
  - KiteHub platform tenant row (tenant 0)
  - **KHÔNG seed demo content** — demo content là separate optional script
- **Tests:** ≥4 — `ProductionSeedRunnerTest.java` (idempotent assertion + skip when no `--seed-mode` flag + admin user created correctly), `V27SeedMigrationTest.java` (Flyway migration clean)
- **Acceptance (Bucket A):** `mvn verify -pl kitehub-subscription` clean + `scripts/seed-production.sh --dry-run` exits 0

### Bucket B — GAP-370 Email Beta Templates + SES Docs

- **Spec source:** `documents/04-quality/gaps/GAP-370-email-transactional-infrastructure.md`
- **State-check findings:**
  - `SESEmailService.java` ✅ EXISTS — full AWS SES SDK integration with `SesClient` + `JavaMailSender` fallback
  - `email.provider: ${EMAIL_PROVIDER:mock}` — production switch via env var ✅
  - 13 email templates ✅ EXISTS — but NO `beta-invite.html` or `beta-request-confirmation.html`
  - `SESConfig.java` ✅ EXISTS — AWS region + credentials config beans
  - GAP-370 remaining AC: beta-invite template + DNS TXT records for SPF/DKIM (runbook) + rate limit config + bounce/complaint handling
- **Files to create:**
  - `kitehub-email/src/main/resources/templates/emails/beta-invite.html` — Thymeleaf template: org name, invite token link, expiry date, beta disclaimer, CTA "Hoàn tất đăng ký"
  - `kitehub-email/src/main/resources/templates/emails/beta-request-confirmation.html` — Thymeleaf template: "Đã nhận yêu cầu beta" + expected review time + contact
  - `documents/05-guides/operations/email-ses-setup-runbook.md` — Production SES setup: domain verification → DKIM/SPF TXT records → sandbox→production request → sending limits → bounce/complaint SNS → warmup schedule
- **Files to modify:**
  - `kitehub-email/src/main/resources/application.yml` — thêm SES bounce/complaint config placeholders + rate limit properties
- **Email type enum:** Thêm `BETA_INVITE` + `BETA_REQUEST_CONFIRM` vào `EmailType` enum nếu tồn tại, hoặc tạo nếu absent
- **Tests:** ≥3 — template Thymeleaf rendering test (variables replaced, link correct), `EmailType` enum completeness, SES config properties load
- **Acceptance:** `mvn verify -pl kitehub-email` clean + both templates render correctly với sample data

### Bucket C — GAP-372 Beta Tenant Invite Flow

- **Spec source:** `documents/04-quality/gaps/GAP-372-beta-tenant-invite-mechanism.md`
- **State-check findings:**
  - `V26__create_dsar_ticket.sql` = latest. **Bucket C owns V28** (V27 = Bucket A). Brief explicitly.
  - NO `BetaAccessRequest` entity — 🆕 to-be-created
  - NO `(auth)/request-beta-access/` route — 🆕 to-be-created
  - NO `(auth)/beta-signup/` route — 🆕 to-be-created
  - NO `(admin)/admin/beta-requests/` — 🆕 to-be-created
  - Auth routes existing: `(auth)/login/`, `(auth)/register/`, `(auth)/verify-email/`
  - Admin routes existing: `(admin)/admin/instances/`, `(admin)/admin/payments/`, `(admin)/admin/revenue/`
  - Email send pattern: kitehub-subscription publishes event → kitehub-email subscribes (Outbox pattern per `design-patterns.md` §3.5)
- **Files to create (BE — kitehub-subscription):**
  - `com.kite.hub.subscription.beta.BetaAccessRequest.java` — `@Entity` với fields: id, email, name, orgName, persona, referralSource, status, createdAt, approvedAt, approverId, inviteToken (UUID), inviteTokenExpiry, invitesentAt
  - `com.kite.hub.subscription.beta.BetaAccessRequestStatus.java` — enum: `PENDING`, `APPROVED`, `REJECTED`, `SIGNED_UP`
  - `com.kite.hub.subscription.beta.BetaRequestRepository.java` — JPA repo
  - `com.kite.hub.subscription.beta.BetaAccessService.java` — submitRequest(), approveRequest() → generate token + publish `BetaInviteSentEvent` via Outbox, rejectRequest(), validateToken(), completeBetaSignup()
  - `com.kite.hub.subscription.beta.BetaAccessController.java` — 5 endpoints
  - `com.kite.hub.subscription.beta.dto.BetaRequestDto.java`, `BetaApproveCommand.java`, `BetaSignupCommand.java`
  - `kitehub-subscription/src/main/resources/db/migration/V28__create_beta_access_request.sql`
- **Files to modify (BE):**
  - `KiteHubSubscriptionApplication.java` — thêm `@EntityScan("com.kite.hub.subscription")` nếu chưa có, hoặc verify package scan covers `beta` subpackage
- **Files to create (FE — kitehub-frontend):**
  - `src/app/(auth)/request-beta-access/page.tsx` — form: email + name + orgName + persona dropdown + referralSource + honeypot
  - `src/app/(auth)/beta-signup/page.tsx` — `?token=XXX` validation + complete signup form
  - `src/app/(admin)/admin/beta-requests/page.tsx` — paginated list (PENDING first) + approve/reject actions
  - `src/components/auth/BetaRequestForm.tsx` — form component với validation
  - `src/components/auth/BetaSignupForm.tsx` — token-validated signup form
- **Tests (BE):** ≥6 — `BetaAccessControllerTest.java` (submit/approve/reject/validate-token + rate-limit smoke), `BetaAccessServiceTest.java` (token expiry), `V28MigrationTest.java`
- **Tests (FE):** ≥4 — request form render + validation, beta-signup token validation, admin list render
- **Acceptance:** `mvn verify -pl kitehub-subscription` clean + `pnpm type-check && pnpm test --run && pnpm build` KH frontend clean

### Bucket D — GAP-369 DNS Runbook + GAP-379 Secrets Template

- **Spec source:** `documents/04-quality/gaps/GAP-369-production-dns-domain-setup.md` + `documents/04-quality/gaps/GAP-379-secrets-management-rotation.md`
- **State-check findings:**
  - NO DNS runbooks — 🆕 to-be-created
  - NO `scripts/ssl-cert-setup.sh` — 🆕 to-be-created
  - NO `.env.production.template` — 🆕 to-be-created
  - `terraform-aws/` exists — agent reads for current VPC/security-group config + region
  - GAP-379: AWS Secrets Manager integration code deferred (Terraform changes) — Wave 33 ships docs + templates only; full Terraform integration → Wave 34 nếu cần
- **Files to create:**
  - `documents/05-guides/operations/dns-setup-runbook.md` — end-to-end DNS guide: domain registrar (Nhân Hòa / Cloudflare registrar), A/AAAA records, MX records, TXT records (SPF/DKIM/DMARC), subdomain strategy (beta.kitehub.vn → production cutover), Cloudflare proxy setup, Let's Encrypt certbot. Include `[USER_INPUT_REQUIRED]` markers cho IP addresses + domain names.
  - `documents/05-guides/operations/secrets-management-runbook.md` — AWS Secrets Manager setup: create secrets, IAM policy for EKS workload identity, rotation policy for DB password + JWT secret + API keys. Include tiered approach: Wave 33 = manual setup; Wave 34 = Terraform-managed.
  - `scripts/ssl-cert-setup.sh` — certbot automation: install certbot → request cert → cron renewal + webhook notify
  - `scripts/check-dns-propagation.sh` — verify all required DNS records propagated (A, MX, TXT SPF/DKIM, CNAME); exits 0 if all pass
  - `.env.production.template` — complete env var listing với `[REQUIRED]`/`[OPTIONAL]`/`[USER_INPUT]` markers cho all services (subscription, email, branding, core, gateway)
- **Tests:** shellcheck on `ssl-cert-setup.sh` + `check-dns-propagation.sh`
- **Acceptance:** `shellcheck scripts/ssl-cert-setup.sh scripts/check-dns-propagation.sh` clean + `.env.production.template` covers all services

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|--------|------|-------------|---------|
| `V26__create_dsar_ticket.sql` (latest) | migration | `ls kitehub-subscription/src/main/resources/db/migration/` sort tail | ✅ V26 latest → **Bucket A=V27, Bucket C=V28** |
| `SESEmailService.java` | existing class | `find kitehub-email -name "SESEmailService.java"` | ✅ exists — Bucket B extends, không rewrite |
| `email.provider: ${EMAIL_PROVIDER:mock}` | config | `cat kitehub-email/src/main/resources/application.yml` | ✅ switchable via env var |
| `beta-invite.html` template | email template | `ls kitehub-email/src/main/resources/templates/emails/` | ❌ missing → 🆕 Bucket B |
| `BetaAccessRequest.java` | entity | `grep -rl "BetaAccessRequest" kitehub-subscription/src/` | ❌ missing → 🆕 Bucket C |
| `(auth)/request-beta-access/` | FE route | `ls kitehub-frontend/src/app/(auth)/` | ❌ missing → 🆕 Bucket C |
| `(admin)/admin/beta-requests/` | FE route | `ls kitehub-frontend/src/app/(admin)/admin/` | ❌ missing → 🆕 Bucket C |
| `seed-data.sh` | dev seed script | `ls kitehub/scripts/` | ✅ exists (dev-only via HTTP API) → Bucket A tạo production variant |
| `ProductionSeedRunner.java` | seed runner | `grep -rl "ProductionSeedRunner" kitehub-subscription/src/` | ❌ missing → 🆕 Bucket A |
| `dns-setup-runbook.md` | ops doc | `ls documents/05-guides/operations/` | ❌ missing → 🆕 Bucket D |
| `.env.production.template` | config template | `ls kitehub/` | ❌ missing → 🆕 Bucket D |
| `KiteHubSubscriptionApplication.java` @EntityScan | annotation | `grep -n "EntityScan" kitehub-subscription/src/main/java/...` | needs agent verify at runtime |
| `EmailClient` in kitehub-subscription | REST client | `find kitehub-subscription -name "EmailClient.java"` | needs agent verify; if absent → Bucket C dùng Outbox event pattern |

**Pre-spawn verify (coordinator):**
1. Wave 32 closure SHIPPED (Wave 32 4 bucket PRs + closure merged) — Wave 33 spawn AFTER
2. `pnpm -F @kite/kitehub-frontend build` baseline clean
3. `mvn verify -pl kitehub/kitehub-subscription` baseline clean

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | Notes |
|--------|---------------------|-------|
| A | `mvn verify -pl kitehub/kitehub-subscription -am` | Focus: SeedRunner tests + V27 Flyway migration; NO pom.xml changes |
| B | `mvn verify -pl kitehub/kitehub-email -am` | Focus: template rendering + EmailType enum + SES config load |
| C | `mvn verify -pl kitehub/kitehub-subscription -am && pnpm -F @kite/kitehub-frontend type-check && pnpm -F @kite/kitehub-frontend test --run && pnpm -F @kite/kitehub-frontend build` | Both layers per `feedback_agent_local_verify_both_layers.md`; focus: BetaAccess BE tests + FE 3 pages |
| D | `shellcheck scripts/ssl-cert-setup.sh scripts/check-dns-propagation.sh` | Docs-only; shellcheck for bash scripts |

Coordinator post-merge: `mvn -f kitehub/pom.xml verify --fail-at-end` (multi-module) + `pnpm -F @kite/kitehub-frontend build`.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- Tất cả 4 buckets `run_in_background: true` + `isolation: worktree`
- RELATIVE paths only (per `feedback_worktree_absolute_path_contamination.md`)
- Coordinator merge sequential: A → B → C → D
- `(auth)/layout.tsx` + `(admin)/layout.tsx` → KHÔNG được sửa bởi Bucket C
- Migration version briefing: **A=V27, C=V28** — mention EXPLICITLY trong brief của cả 2 buckets

**Spawn timing:** Wave 33 plan drafted PIPELINED trong khi Wave 32 agents in-flight. Spawn AFTER Wave 32 closure SHIPPED + token budget verify per `feedback_token_quota_spawn_timing.md`. `/clear` recommended nếu cùng session với Wave 32 closure.

**Domain-milestone audit:** Wave 33 là multi-domain (BE + FE + Docs) → KHÔNG đủ điều kiện `AUDIT_DEFER_DOMAIN_MILESTONE`. Audit suite (API Contract /100 vì new endpoints + UI /128 cho 3 new FE pages) PHẢI run ≤3 ngày post-merge per `post-wave-audit-mandate.md` §2.1.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Mỗi bucket PR update gap file Log entry tương ứng
- **Status flips post-Wave-33:**
  - GAP-376: 🔵 OPEN → 🟡 PARTIAL (code shipped; production execution pending)
  - GAP-370: 🔵 OPEN → 🟡 PARTIAL (beta templates + docs; production SES domain verification = user step)
  - GAP-372: 🔵 OPEN → 🟡 PARTIAL (code shipped; production email delivery deps on GAP-370 SES active)
  - GAP-369: 🔵 OPEN → 🟡 PARTIAL (runbook + scripts; domain registration + DNS records = user step)
  - GAP-379: 🔵 OPEN → 🟡 PARTIAL (template + policy docs; AWS Secrets Manager provisioning = user step)
- ROADMAP §🚀 Next Action update
- **Release Plan Progress section** (per `feedback_wave_closure_release_progress_report.md`):
  - Current Phase: Phase 1 BETA (v0.9.0-beta target)
  - Deploy cluster: 4/4 P0 BLOCKING gaps now PARTIAL (code shipped, user execution pending)
  - Track 2: 3/7 kits (sau Wave 32) or 2/7 nếu Wave 32 chưa close
  - PDPL deadline countdown 2026-07-01
  - Next gate: user executes production deployment steps
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append
- `bash scripts/prune-merged-worktrees.sh --yes`
- AUDIT trailer: **KHÔNG dùng** `AUDIT_DEFER_DOMAIN_MILESTONE` (multi-domain wave); phải run audit ≤3 ngày

**Follow-up gaps to file at closure (nếu chưa có):**
- GAP-272b — GAP-272g (từ Wave 32 — backend endpoints absent cho wizard)
- GAP-370-production-activation — user-executed steps: SES domain verification + sandbox approval
- GAP-372b — admin email notification on new beta request (Bucket C stub, wire later)
- Wave 34 candidates: GAP-380 staging environment activation + GAP-371 CDN Cloudflare + GAP-373 status page + GAP-374 tag-CI automation

---

## 8. Log

- **2026-05-06 (draft):** Wave 33 plan drafted PIPELINED trong khi Wave 32 4 agents in-flight (5th consecutive `wave-pack-planner` §Step 5.5 pipelined application — waves 28→29, 29→30, 30→31, 31→32, 32→33). State-check findings: `SESEmailService.java` + 13 email templates đã tồn tại (Wave 18a partial work on GAP-370) → Bucket B chỉ extends không rewrite. `seed-data.sh` local dev script tồn tại → Bucket A tạo production variant khác biệt. V26 = latest migration → A=V27, C=V28 pre-assigned. `BetaAccessRequest` + beta FE routes đều absent → Bucket C greenfield. GAP-379 scoped down (docs + template only; Terraform integration → Wave 34). Multi-domain wave → NO domain-milestone audit deferral. Spawn timing: AFTER Wave 32 closure + `/clear` recommended.
