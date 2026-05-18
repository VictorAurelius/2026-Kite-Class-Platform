---
title: Wave 33 — Phase 1 BETA deploy cluster — P0 BLOCKING (seed + email + beta invite + DNS/secrets)
status: complete
created: 2026-05-06
updated: 2026-05-07
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
| B | GAP-370 Email beta templates + SES production config runbook | bg-agent | ~12-15 min | ✅ `kitehub-email/src/main/resources/templates/emails/beta-*.html` + `kitehub-email/src/main/resources/application.yml` SES section + `documents/05-guides/deploy/email-ses-setup-runbook.md` |
| C | GAP-372 Beta tenant invite flow (BE entity + FE 3 pages) | bg-agent | ~25-30 min | ✅ `kitehub-subscription/*/beta/` + `V28` + `kitehub-frontend/(auth)/request-beta-access/` + `(auth)/beta-signup/` + `(admin)/admin/beta-requests/` |
| D | GAP-369 DNS runbook + GAP-379 Secrets template | bg-agent | ~15-18 min | ✅ `documents/05-guides/deploy/dns-setup-runbook.md` + `secrets-management-runbook.md` + `scripts/ssl-cert-setup.sh` + `scripts/check-dns-propagation.sh` + `.env.production.template` |

**Disjoint check:** Mỗi bucket touch độc lập (paths overlap = none). Shared edits: `KiteHubSubscriptionApplication.java` (Bucket C only — `@EntityScan` cho `beta` package; A dùng `@Component` không cần). Migration version: A=V27, C=V28 — coordinator merge A trước C.

**Cross-bucket dependency:** Không có giữa A↔B↔D. Bucket C phụ thuộc GAP-370 email template (Bucket B) chỉ ở production behavior, không compile-time → C ship stub email call, B ship template. Coordinator merge B trước C.

---

## 3. Scope (per bucket)

### Bucket A — GAP-376 Production Data Seed

- **Spec:** GAP-376 §Proposed Fix Option B (Spring Boot `CommandLineRunner`)
- **State-check:** V26 latest → **owns V27**; no existing `ProductionSeedRunner`; `kitehub/scripts/seed-data.sh` is dev-HTTP variant
- **Files+:**
  - `kitehub-subscription/src/main/java/com/kite/hub/subscription/seed/ProductionSeedRunner.java` — `@Component` + `ApplicationRunner`, idempotent, reads `--seed-mode=production`
  - `.../seed/SeedProperties.java` — `@ConfigurationProperties("kite.seed")` (mode + adminEmail + adminPassword)
  - `kitehub-subscription/src/main/resources/db/migration/V27__seed_admin_system_config.sql` — INSERT IF NOT EXISTS
  - `scripts/seed-production.sh` — wrapper với DB conn check + dry-run mode
- **Seed scope:** `admin@kitehub.vn` PLATFORM_ADMIN (password từ `SEED_ADMIN_PASSWORD` env); system_config (`default_tier=FREE`, `currency=VND`, `locale=vi`); platform tenant id=0; **NO demo content** (separate optional script)
- **Tests ≥4:** `ProductionSeedRunnerTest` (idempotent + skip-no-flag + admin created), `V27SeedMigrationTest` (Flyway clean)
- **AC:** `mvn verify -pl kitehub-subscription` clean + `scripts/seed-production.sh --dry-run` exits 0

### Bucket B — GAP-370 Email Beta Templates + SES Docs

- **Spec:** GAP-370 (P1 STRONGLY-recommend)
- **State-check:** `SESEmailService` + `SESConfig` + 13 templates + `email.provider: ${EMAIL_PROVIDER:mock}` ✅ existing (Wave 18a). Missing: `beta-invite.html`, `beta-request-confirmation.html`, SES production runbook, bounce/complaint config, rate limit
- **Files+:**
  - `kitehub-email/src/main/resources/templates/emails/beta-invite.html` — Thymeleaf: org name, invite token link, expiry date, CTA "Hoàn tất đăng ký", beta disclaimer
  - `.../emails/beta-request-confirmation.html` — Thymeleaf: "Đã nhận yêu cầu beta" + expected review time + contact
  - `documents/05-guides/deploy/email-ses-setup-runbook.md` — sandbox→production approval, DKIM/SPF TXT, sending limits, bounce/complaint SNS, warmup schedule
- **Files~:** `kitehub-email/src/main/resources/application.yml` — bounce/complaint config + rate limit properties
- **EmailType enum:** add `BETA_INVITE` + `BETA_REQUEST_CONFIRM` (or create enum if absent)
- **Tests ≥3:** template Thymeleaf rendering (variables + link), `EmailType` enum completeness, SES config properties load
- **AC:** `mvn verify -pl kitehub-email` clean + cả 2 templates render với sample data

### Bucket C — GAP-372 Beta Tenant Invite Flow

- **Spec:** GAP-372 (P0 BLOCKING)
- **State-check:** **owns V28** (V27 = Bucket A); `BetaAccessRequest` entity ❌; `(auth)/request-beta-access`, `(auth)/beta-signup`, `(admin)/admin/beta-requests` ❌; existing auth routes: `login/register/verify-email`; admin: `instances/payments/revenue`
- **Files+ BE** (`com.kite.hub.subscription.beta.*`):
  - `BetaAccessRequest.java` — `@Entity`: id, email, name, orgName, persona, referralSource, status, createdAt, approvedAt, approverId, inviteToken (UUID), inviteTokenExpiry, inviteSentAt
  - `BetaAccessRequestStatus.java` — enum PENDING/APPROVED/REJECTED/SIGNED_UP
  - `BetaRequestRepository.java` — JPA repo
  - `BetaAccessService.java` — submitRequest / approveRequest→token+`BetaInviteSentEvent` via Outbox / rejectRequest / validateToken / completeBetaSignup
  - `BetaAccessController.java` — 5 endpoints
  - DTOs: `BetaRequestDto`, `BetaApproveCommand`, `BetaSignupCommand`
  - `db/migration/V28__create_beta_access_request.sql`
- **Files~ BE:** `KiteHubSubscriptionApplication.java` — `@EntityScan` covers `beta` subpackage
- **Files+ FE** (`kitehub-frontend/`):
  - `src/app/(auth)/request-beta-access/page.tsx` — form: email + name + orgName + persona + referralSource + honeypot
  - `src/app/(auth)/beta-signup/page.tsx` — `?token=XXX` validation + complete signup form
  - `src/app/(admin)/admin/beta-requests/page.tsx` — paginated list (PENDING first) + approve/reject
  - `src/components/auth/BetaRequestForm.tsx`, `BetaSignupForm.tsx`
- **Tests BE ≥6:** `BetaAccessControllerTest` (submit/approve/reject/validate-token + rate-limit smoke), `BetaAccessServiceTest` (token expiry), `V28MigrationTest`
- **Tests FE ≥4:** request form render+validation, beta-signup token validation, admin list render
- **AC:** `mvn verify -pl kitehub-subscription` clean + `pnpm type-check && test --run && build` KH frontend clean

### Bucket D — GAP-369 DNS Runbook + GAP-379 Secrets Template

- **Spec:** GAP-369 (P0 BLOCKING) + GAP-379 (P1 STRONGLY-recommend; scope cut: docs+template only, Terraform → Wave 34)
- **State-check:** `terraform-aws/` ✅ exists (agent reads VPC/security-group + region); DNS/secrets runbooks ❌
- **Files+:**
  - `documents/05-guides/deploy/dns-setup-runbook.md` — registrar (Nhân Hòa/Cloudflare), A/AAAA + MX + TXT (SPF/DKIM/DMARC), subdomain (beta.kitehub.vn → prod cutover), Cloudflare proxy, Let's Encrypt certbot. `[USER_INPUT_REQUIRED]` markers cho IP + domain
  - `documents/05-guides/operations/secrets-management-runbook.md` — AWS Secrets Manager: create secrets, IAM policy EKS workload identity, rotation (DB password / JWT / API keys). Tiered: Wave 33 manual; Wave 34 Terraform
  - `scripts/ssl-cert-setup.sh` — certbot install + request cert + cron renewal + webhook notify
  - `scripts/check-dns-propagation.sh` — verify A/MX/TXT/CNAME propagated; exit 0 if all pass
  - `.env.production.template` — env vars all services (subscription/email/branding/core/gateway) với `[REQUIRED]`/`[OPTIONAL]`/`[USER_INPUT]` markers
- **Tests:** shellcheck on 2 bash scripts
- **AC:** `shellcheck scripts/ssl-cert-setup.sh scripts/check-dns-propagation.sh` clean; `.env.production.template` covers all services

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Verdict |
|--------|---------|
| `V26__create_dsar_ticket.sql` (latest migration) | ✅ → **A=V27, C=V28** |
| `SESEmailService.java` + `SESConfig.java` + 13 templates | ✅ exists (Wave 18a) — Bucket B extends only |
| `email.provider: ${EMAIL_PROVIDER:mock}` | ✅ env-var switchable |
| `beta-invite.html` + `beta-request-confirmation.html` | ❌ → 🆕 Bucket B |
| `BetaAccessRequest` entity + `(auth)/request-beta-access/` + `(auth)/beta-signup/` + `(admin)/admin/beta-requests/` | ❌ → 🆕 Bucket C |
| `kitehub/scripts/seed-data.sh` (dev-HTTP) | ✅ exists; production variant 🆕 Bucket A |
| `ProductionSeedRunner.java` | ❌ → 🆕 Bucket A |
| `dns-setup-runbook.md` + `secrets-management-runbook.md` + `.env.production.template` | ❌ → 🆕 Bucket D |
| `KiteHubSubscriptionApplication` `@EntityScan` covers `beta` subpackage | ⚠️ agent verify at runtime |
| `EmailClient` in `kitehub-subscription` | ⚠️ agent verify; if absent → Bucket C uses Outbox event pattern (per `design-patterns.md` §3.5) |
| `terraform-aws/` (VPC + region) | ✅ exists — Bucket D reads for accuracy |

Pre-spawn coordinator verify per `wave-pack-planner` skill: Wave 32 closure SHIPPED + `mvn verify` + `pnpm build` baselines clean.

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

- **2026-05-07 (optimized):** Plan compacted §3 Scope per bucket (bullet schema replacing prose) + §State-Check Evidence streamlined (verification command column dropped, redundant rows merged). Net saving ~36 lines (253→217). Strategy B+C from coordinator session 2026-05-07 wave-plan optimization analysis. Sections §1/§2/§5/§6/§7 unchanged (Strategy A — shared `_CONVENTIONS.md` extraction — deferred to post-Wave-32-rework retro per `wave-pack-planner` skill update plan).
- **2026-05-07 (complete):** All 4 buckets shipped — A #895 (ProductionSeedRunner + V27), B #896 (beta email templates + SES runbook + EmailType enum extension), C #898 (BetaAccessRequest BE entity + 24h UUID token + Outbox + 3 FE pages + 21 BE + 10 FE tests; coordinator-applied admin scan fix on agent branch + force-push), D #897 (DNS runbook + secrets runbook + 2 bash scripts + .env.production.template). 5 GAPs flipped 🔵 OPEN → 🟡 PARTIAL: GAP-376/370/372/369/379 — production execution / SES domain verification / domain registration / Secrets Manager provisioning all user-executed steps per `gap-done-discipline.md` §3 PARTIAL exit ramp. CI fix incident: PR #898 first run failed Admin Service test (UnsatisfiedDependencyException — BetaAccessRequestRepository not in admin @EnableJpaRepositories scan); 1-file 7-line mechanical fix per `feedback_admin_scan_packages_after_module_add.md` recurrence + `feedback_coordinator_ci_fix_pattern.md` force-push pattern. CI 2nd run: 1 transient Docker registry 502 → rerun → 3rd run xanh. Multi-domain wave (BE + FE + Docs) — NO domain-milestone deferral; audit suite must run ≤3 ngày per `post-wave-audit-mandate.md` §2.1.
- **2026-05-06 (draft):** Wave 33 plan drafted PIPELINED trong khi Wave 32 4 agents in-flight (5th consecutive `wave-pack-planner` §Step 5.5 pipelined application — waves 28→29, 29→30, 30→31, 31→32, 32→33). State-check findings: `SESEmailService.java` + 13 email templates đã tồn tại (Wave 18a partial work on GAP-370) → Bucket B chỉ extends không rewrite. `seed-data.sh` local dev script tồn tại → Bucket A tạo production variant khác biệt. V26 = latest migration → A=V27, C=V28 pre-assigned. `BetaAccessRequest` + beta FE routes đều absent → Bucket C greenfield. GAP-379 scoped down (docs + template only; Terraform integration → Wave 34). Multi-domain wave → NO domain-milestone audit deferral. Spawn timing: AFTER Wave 32 closure + `/clear` recommended.
