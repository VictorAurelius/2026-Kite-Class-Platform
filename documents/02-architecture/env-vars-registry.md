---
title: Production Env Vars Registry
audience: mixed
created: 2026-05-14
last-reviewed: 2026-05-19
status: living
---

# Production Env Vars Registry

**Owner:** DevOps + Tech Lead
**Last-Updated:** 2026-05-14
**Rule:** [`.claude/rules/production-env-config-registry.md`](../../.claude/rules/production-env-config-registry.md)
**Audit:** `bash scripts/audit-env-coverage.sh`

Nguồn dữ liệu chính thức duy nhất liệt kê mọi reference env-var `${VAR:<default>}` mà default không hoạt động được trong production, kèm cơ chế override. Per rule §1, mọi suspect default PHẢI được override trong production HOẶC explicit liệt kê là `ACCEPTED-default` kèm rationale.

## Status legend

- ✅ **Overridden** — production override present in docker-compose / fetch-secrets / GH Secret
- 🆕 **Just-added** — added in current session, pending live verification
- ❌ **Missing** — production using dev default → broken in real flows
- ⚠️ **ACCEPTED-default** — feature deferred / mechanism not used, dev default OK

## Registry (Phase 1 BETA scope)

| # | Service | Var | Yaml ref | Default | Override mechanism | Status | Notes |
|---|---------|-----|----------|---------|-------------------|--------|-------|
| 1 | gateway | `CORS_ALLOWED_ORIGINS` | `kitehub-gateway/application.yml:11` | localhost-only | docker-compose env | 🆕 Just-added (GAP-507) | Production: kitehub.me + www + Vercel alias |
| 2 | subscription | `VERIFICATION_BASE_URL` | `kitehub-subscription/application.yml:111` | http://localhost:3001 | docker-compose env | 🆕 Just-added (GAP-508 P0) | Production: https://kitehub.me |
| 2b | subscription | `KITEHUB_BETA_SIGNUP_BASE_URL` (`kitehub.beta.signup-base-url`) | `BetaAccessService.java:154` @Value | https://kitehub.me | docker-compose env (local→http://localhost:3001, prod→https://kitehub.me) | ✅ Added (GAP-801) | Beta-invite signup link base; FE route /beta-signup/code |
| 3 | subscription | `EMAIL_SERVICE_URL` | `kitehub-subscription/application.yml:167` | http://localhost:8083 | docker-compose env | 🆕 Just-added (GAP-508 P0) | Internal docker network: http://kitehub-email:8084 |
| 4 | email | `EMAIL_PROVIDER` | `kitehub-email/application.yml:29` | mock | docker-compose env | 🆕 Just-added (GAP-508 P0) | Production: resend (per ADR-025 Stream A) |
| 5 | email | `AWS_SES_FROM_EMAIL` | `kitehub-email/application.yml:35` | noreply@localhost | docker-compose env | 🆕 Just-added (GAP-508 P0) | Production: noreply@kitehub.me |
| 6 | email | `AWS_SES_FROM_NAME` | `kitehub-email/application.yml:36` | "Local Dev Platform" | docker-compose env | 🆕 Just-added (GAP-508 P0) | Production: "KiteHub Beta" |
| 7 | email | `MANAGEMENT_HEALTH_MAIL_ENABLED` | (Spring Boot default true) | true | docker-compose env | ✅ Overridden (GAP-506 Sub-B) | Production: false (Resend HTTP, not SMTP) |
| 8 | email | `RESEND_API_KEY` | `kitehub-email/application.yml` (Resend block) | empty | fetch-secrets.sh → /etc/kite/.env (Wave 81+ PR pulls qua `fetch_secret resend-api-key`) | 🟡 **PARTIAL — IaC parity DONE, live verify pending** | **Wave br-4 Bucket A (2026-05-24):** declared trong `infrastructure/terraform-aws/secrets.tf` (`random_password.resend_api_key_placeholder` + `aws_secretsmanager_secret.resend_api_key` + `aws_secretsmanager_secret_version.resend_api_key` với `lifecycle ignore_changes = [secret_string]` cho post-apply manual override) closing IaC drift per GAP-508 Phase 2/3 (mirrors jwt-challenge precedent Wave 81 GAP-509 / Wave 105 GAP-717). IAM grant via wildcard `${var.project_name}/${var.environment}/*` pattern trong iam.tf:54 (no edit needed). `scripts/fetch-secrets.sh` lines 88-113 đã wire pull this secret on EC2 boot (GAP-572 dual schema JSON wrapper hoặc plain string). Post AWS account 906286017800 restore (GAP-612 unblock): run `terraform apply` → creates placeholder secret → manual override via AWS console với real Resend API key JSON `{"api_key":"re_<real>","from_email":"noreply@kitehub.me","from_name":"KiteHub Beta"}`. Live verify deferred GAP-NEW-resend-live-verify-post-restore (Wave br-5+) per `local-fix-production-parity-check.md` §3.2 follow-up. |
| 9 | branding | `CDN_DOMAIN` | `kitehub-branding/application.yml:158` | localhost:9100 | docker-compose env | ❌ Missing | Phase 1 BETA assets served via Vercel; revisit if/when CDN provisioned |
| 10 | all kitehub-* | `SPRING_DATASOURCE_URL` | various | jdbc:postgresql://localhost:5433/... | fetch-secrets.sh → /etc/kite/.env | ✅ Overridden | RDS endpoint via secret |
| 11 | all kitehub-* | `SPRING_RABBITMQ_HOST` | various | localhost | fetch-secrets.sh writes SPRING_RABBITMQ_HOST=kite-rabbitmq | ✅ Overridden | Internal docker host |
| 12 | all kitehub-* | `SPRING_RABBITMQ_USERNAME/PASSWORD` | various | guest | fetch-secrets.sh + deploy-prod.sh Step 6.5 self-heal (GAP-504) | ✅ Overridden | Dynamic per deploy until populate-secrets.sh stabilizes (done 2026-05-13) |
| 13 | all kitehub-* | `SPRING_DATA_REDIS_HOST` | various | localhost | fetch-secrets.sh writes SPRING_DATA_REDIS_HOST=kite-redis | ✅ Overridden | Internal docker host |
| 14 | all kitehub-* | `JWT_SECRET` / `ENCRYPTION_KEY` | various | (no default) | fetch-secrets.sh → /etc/kite/.env | ✅ Overridden | AWS Secrets Manager (terraform random_password) |
| 15 | kitehub-subscription, kitehub-admin | `JWT_CHALLENGE_SECRET` | `kitehub-subscription/application.yml` (2FA challenge token) | dev default ≤40 bytes | fetch-secrets.sh → /etc/kite/.env (Wave 81 Bucket F PR #1388) | ✅ Overridden | AWS Secrets Manager `kitehub/production/jwt-challenge-secret`. Wave 81 manual creation 2026-05-15 via jwt-secret-fix-runbook. **Wave 105 Bucket E0 (2026-05-22):** declared in `infrastructure/terraform-aws/secrets.tf` (random_password + secret + version) closing IaC drift per GAP-717/GAP-718; post AWS account 906286017800 restore (GAP-612 unblock) run `terraform import aws_secretsmanager_secret.jwt_challenge kitehub/production/jwt-challenge-secret` to bind existing AWS secret to terraform state. Wave 79 Bucket C `ChallengeTokenService.@PostConstruct` fail-fast guard enforces non-dev-default in production profile |
| 16 | kitehub-subscription, kitehub-admin | `TOTP_ENCRYPTION_KEY` + `KITEHUB_AUTH_TOTP_ENCRYPTION_KEY` (dual-write) | `kitehub-subscription/application.yml:109` (TOTP secret AES-256 encryption at rest) | dev default 36 bytes (`dev-key-32-chars-pad-pad-pad-pad-pad`) | fetch-secrets.sh → /etc/kite/.env (Wave 81 Bucket F PR #1389 + PR #1390 dual-write) | ✅ Overridden | AWS Secrets Manager `kitehub/production/totp-encryption-key`. **Dual-write rationale:** subscription yaml line 109 uses explicit `${TOTP_ENCRYPTION_KEY}` binding; kitehub-admin yaml-less → Spring relaxed binding của property `kitehub.auth.totp.encryption-key` tìm env var `KITEHUB_AUTH_TOTP_ENCRYPTION_KEY` (camelCase → SCREAMING_SNAKE_CASE). Both names point cùng secret value. Wave 72b GAP-516 `TotpSecretCipher.@PostConstruct` fail-fast guard mandate non-dev-default + ≥32 bytes trong production profile |
| 17 | kitehub-subscription, kitehub-admin | `KITEHUB_STAFF_INVITATION_SIGNING_SECRET` | `InvitationTokenService.java` `@Value("${kitehub.staff.invitation.signing-secret:...}")` | dev default 36 bytes (`dev-invitation-secret-32-bytes-pad-pad`) | fetch-secrets.sh → /etc/kite/.env (Wave 81 Bucket F PR #1389) | ✅ Overridden | AWS Secrets Manager `kitehub/production/staff-invitation-signing-secret` (created 2026-05-15 manual). Wave 78 GAP-548 `InvitationTokenService.@PostConstruct` fail-fast guard mandate non-dev-default + ≥32 bytes trong production profile |
| 18 | kitehub-subscription | `SEPAY_API_KEY` (`kitehub.payment.sepay.api-key`) | `application.yml` payment.sepay block (sibling Bucket B) | empty | fetch-secrets.sh → /etc/kite/.env + docker-compose env passthrough | 🟡 **PARTIAL — IaC parity DONE, live verify pending** | **Wave flow-kh3-3:** SePay payment webhook Apikey auth (POST /api/platform/webhooks/payment per api-contract.md UC-SUB-08). Declared trong `infrastructure/terraform-aws/secrets.tf` (`random_password.sepay_api_key_placeholder` + `aws_secretsmanager_secret.sepay_api_key` + `aws_secretsmanager_secret_version.sepay_api_key` với `lifecycle ignore_changes = [secret_string]` cho post-apply manual override) — mirrors resend-api-key precedent. IAM grant via wildcard `${var.project_name}/${var.environment}/*` pattern trong iam.tf (no edit needed). `scripts/fetch-secrets.sh` pulls `kitehub/production/sepay-api-key` on EC2 boot (plain string). Key vendor-provided — configured trong SePay dashboard (https://sepay.vn Free 50tx/month tier). Post AWS account 906286017800 restore (GAP-612 unblock): `terraform apply` → placeholder secret → manual override via AWS console với real SePay key. Live verify deferred post-restore per `local-fix-production-parity-check.md` §3.2 follow-up. Empty → webhook fails closed (401). |
| 19 | kitehub-subscription | `BETA_PAYMENT_OVERRIDE` (`kitehub.payment.beta-mode.enabled`) | `application.yml` payment.beta-mode block (sibling Bucket B) | `false` | docker-compose env | ✅ Added (Wave flow-kh3-3) | Public config — production giữ OFF mặc định. Flip `true` chỉ để force symbolic `BETA_PAYMENT_AMOUNT_VND` tại createPayment time (Phase 1 BETA symbolic transfer). FE mirror flag `NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE` hiển thị BetaModeBanner. |
| 20 | kitehub-subscription | `BETA_PAYMENT_AMOUNT_VND` (`kitehub.payment.beta-mode.override-amount-vnd`) | `application.yml` payment.beta-mode block (sibling Bucket B) | `10000` | docker-compose env | ✅ Added (Wave flow-kh3-3) | Public config — symbolic amount VND (10k = bank minimum; VCB/MBB/TCB accept per failure-mode audit 2026-06-04). Chỉ áp dụng khi `BETA_PAYMENT_OVERRIDE=true`. |

## Wave 78 Bucket 0 — 4 NEW endpoints (added 2026-05-14, GAP-508)

The 4 new endpoints introduced by Wave 78 Bucket 0 (PR #1349) are MSW-only contracts at this time — backend implementations land in Buckets A/B/F. Pre-launch env coverage for each endpoint family is enumerated below so that when the BE implementations ship the matching rate-limit / auth / cache config can be sourced from this single registry.

| # | Endpoint family | Contract source | Owner service (Wave 78) | Required gateway route ID | Rate limit (target) | Cache | Notes |
|---|-----------------|-----------------|--------------------------|---------------------------|--------------------|-------|-------|
| W78-1 | `GET\|PUT /api/v1/onboarding-progress` | `documents/01-business/kitehub/onboarding/api-contract.md` | kitehub-subscription (Wave 78 Bucket A) | `kitehub-onboarding-v1` (new) | 10/30 user-keyed (PUT idempotent) | None (per-user write) | JWT-protected; persisted to `user_onboarding_progress`. |
| W78-2 | `POST /api/v1/feedback` | `documents/01-business/kitehub/feedback/api-contract.md` | kitehub-subscription (Wave 78 Bucket B) | `kitehub-feedback-v1` (new) | 5/10 email-keyed (anonymous OK) | None | Honeypot enforced; rating 1-5; comment 5-2000 chars. |
| W78-3 | `GET /api/v1/beta-status` | `documents/01-business/kitehub/beta-status/api-contract.md` | kitehub-subscription (Wave 78 Bucket F) | `kitehub-beta-status-v1` (new) | 60/120 ip-keyed (public) | 5-minute `Cache-Control: public, max-age=300, stale-while-revalidate=60` | Public; CDN-friendly per ADR-025 + GAP-371 backlog. |
| W78-4 | `POST /api/v1/support-tickets` | `documents/01-business/kitehub/support/api-contract.md` | kitehub-subscription (Wave 78 Bucket B) | `kitehub-support-v1` (new) | 3/5 email-keyed | None | Email required; subject 5-200 / body 10-5000; ticket-number sequence + counter. |

### Env-var implications when BE lands

| Var | Owner | Default in YAML | Production override mechanism (planned) | Required at launch? |
|-----|-------|------------------|------------------------------------------|---------------------|
| `BETA_STATUS_DEFAULT_CONTENT` | kitehub-subscription | Vietnamese markdown blob in `application.yml` | docker-compose env OR DB-backed (Phase 1.5) | No — default OK |
| `BETA_STATUS_CACHE_TTL_SECONDS` | kitehub-subscription | `300` | docker-compose env | No — default OK |
| `FEEDBACK_HONEYPOT_FIELD` | kitehub-subscription | `_hp` | (none — code constant per security advice) | No |
| `SUPPORT_TICKET_NUMBER_PREFIX` | kitehub-subscription | `KH-` | docker-compose env if rebrand needed | No |
| `SUPPORT_NOTIFY_EMAIL` | kitehub-subscription | `support@kitehub.me` (placeholder for Phase 1 BETA) | docker-compose env when Resend domain verified | **Yes** — gated on GAP-508 Phase 2 (Resend API key) |
| `ONBOARDING_TOTAL_STEPS` | kitehub-subscription | `7` (Phase 1 BETA wizard) | (none — code constant; per `documents/01-business/kitehub/onboarding/rules.md`) | No |

### Required gateway route additions (when BE lands)

The 4 routes above MUST land in `kitehub/kitehub-gateway/src/main/resources/application.yml` SAME PR as the BE controllers per `production-env-config-registry.md` §11 audit-gateway-routes.sh. Until then, requests to these paths fall through to the existing `instance-apis` catch-all and forward to kiteclass-core (which will return 404). This is acceptable Wave 78 scope (MSW + contract first); reverting to a hard 404 at the gateway is tracked by `audit-gateway-routes.sh` follow-up.

## Accepted defaults (Phase 1 BETA)

Per `scripts/audit-env-coverage.sh` `ACCEPTABLE_DEFAULTS` array. Wave br-4 Bucket A expansion (GAP-508 Phase 3) — 13 vars total, each row có rationale citing phase / ADR / GAP. Eliminated 10 prior false positives, audit baseline 0 missing 18 accepted.

| Var | Default | Rationale |
|-----|---------|-----------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | http://localhost:4318 | No OTel collector deployed Phase 1 BETA per GAP-115 backlog |
| `AI_OLLAMA_BASE_URL` | http://localhost:11434 | AI deferred Phase 2 (ADR-026) |
| `OPENAI_API_KEY` | sk-mock-key-for-local-testing | AI Phase 2 fallback (kitehub-branding mock OK Phase 1 BETA) |
| `PAYMENT_RETURN_URL` / `PAYMENT_NOTIFY_URL` | localhost | Payment deferred Phase 1.5 per release-1-deploy-plan |
| `SMTP_HOST` / `SMTP_PORT` | kite-mailhog / 1025 | Resend HTTP API used (per ADR-025), SMTP path inactive |
| `S3_ENDPOINT` | http://localhost:9000 | Phase 1 BETA uses native AWS S3 (no endpoint override); MinIO local-dev only |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | mock | Production uses EC2 instance profile via IMDSv2 (no static key per `agent-aws-access.md`); branding mock OK Phase 1 BETA |
| `CDN_DOMAIN` | localhost:9100 | Phase 1 BETA assets via Vercel; CDN deferred (GAP-371 backlog) |
| `DATABASE_ADMIN_URL` / `DATABASE_MASTER_*` | localhost | Admin bootstrap path; production uses managed RDS, admin SQL via psql direct |

### Spring-relaxed binding aliases (Wave br-4 Bucket A — eliminate audit false positives)

Spring auto-resolves these pairs — `${VAR:default}` ở yml binds tới override mechanism qua alias:

| Yml reference | Alias actually overridden | Path |
|---|---|---|
| `${DATABASE_URL:...}` | `SPRING_DATASOURCE_URL` | fetch-secrets.sh writes RDS connection string |
| `${RABBITMQ_HOST:localhost}` | `SPRING_RABBITMQ_HOST` | fetch-secrets.sh writes `kite-rabbitmq` |
| `${SPRING_REDIS_HOST:localhost}` | `SPRING_DATA_REDIS_HOST` | fetch-secrets.sh writes `kite-redis` |
| `${STORAGE_S3_ENDPOINT:...}` | `S3_ENDPOINT` | accepted-default per row above (Phase 1 BETA scope) |

Script `is_overridden()` function handles alias detection automatically — both alias mappings + accepted-default fallback.

## Phase 2/3 actions

### Phase 2 — RESEND_API_KEY provisioning (GAP-508 Phase 2) — 🟡 PARTIAL Wave br-4

**Status update (2026-05-24, Wave br-4 Bucket A):** IaC + script + audit code paths shipped (PARTIAL ~90%). Live verify blocked GAP-612 AWS account 906286017800 suspended.

Done Wave br-4:
1. ✅ Terraform IaC declaration trong `infrastructure/terraform-aws/secrets.tf` (placeholder version với `lifecycle ignore_changes = [secret_string]`)
2. ✅ IAM grant via existing wildcard `${var.project_name}/${var.environment}/*` (iam.tf:54 — no edit needed)
3. ✅ `scripts/fetch-secrets.sh` pull line (Wave 81+ existing, dual schema JSON wrapper hoặc plain string per GAP-572)
4. ✅ Registry row 8 updated (status PARTIAL — IaC parity DONE, live verify pending)

Pending Wave br-5+ (post-AWS-restore, GAP-NEW-resend-live-verify-post-restore):
5. ⏳ Run `terraform apply` once AWS account restored → creates empty placeholder secret
6. ⏳ Provision Resend account + verify domain `kitehub.me` (DNS DKIM/SPF/DMARC)
7. ⏳ Generate real Resend API key
8. ⏳ Manual override via AWS console: Secrets Manager → kitehub/production/resend-api-key → Retrieve secret value → Set new value → JSON `{"api_key":"re_<real>","from_email":"noreply@kitehub.me","from_name":"KiteHub Beta"}`
9. ⏳ Re-deploy → kitehub-email reads key on boot
10. ⏳ Live verify: `docker exec kitehub-email env | grep RESEND_API_KEY` non-empty
11. ⏳ Test send: POST /api/v1/auth/request-beta-access → check Resend dashboard for delivery + user inbox

Blocks: Plan 1 self-test Bước 5 (user receives invite email + clicks link). Post-AWS-restore unblock window ≤24h.

### Phase 3 — CI gate (GAP-508 Phase 3) — ✅ DONE Wave br-4 Bucket A (WARN-mode initially)

Shipped 2026-05-24 (Wave br-4 Bucket A):
1. ✅ Added `.github/workflows/quality-rules-skills.yml` job `env-coverage` (`name: "Production env-var coverage audit (WARN initially — Wave br-4 Bucket A GAP-508 Phase 3)"`)
2. ✅ Path triggers: `kitehub/*/src/main/resources/application*.yml`, `kiteclass/*/src/main/resources/application*.yml`, `docker-compose.production.yml`, `scripts/fetch-secrets.sh`, `scripts/audit-env-coverage.sh`, `scripts/tests/test-audit-env-coverage.sh`, `scripts/tests/fixtures/audit-env-coverage/**`
3. ✅ Fixture self-test runs first (3 fixtures: known good + known false-positive + known missing-override)
4. ✅ Real audit runs WARN-mode (exit 0 always, log warning if FAIL)
5. ⏳ HARD STOP escalation Phase 4 — ≥7 ngày sau merge per `incident-to-rule-pipeline.md` premature-rule guard

### Phase 4 — HARD STOP escalation (deferred ≥7 ngày sau Wave br-4 Bucket A merge)

When repository state stabilizes:
1. Remove `set +e` / `|| true` wrappers trong env-coverage job
2. Audit FAIL → CI blocks PR
3. Reviewer-checklist line in PR template (`.github/PULL_REQUEST_TEMPLATE.md`)
4. Track follow-up gap GAP-NEW-env-coverage-hard-stop-escalation (Wave br-5+ post-stabilization)

## Update workflow

When adding new `${VAR:default}` in source code:
1. Identify suspect default (per rule §2)
2. Choose override mechanism (per rule §4)
3. Add row to this registry SAME PR
4. Run `bash scripts/audit-env-coverage.sh` locally — must PASS or add to acceptable list with rationale
5. PR description references registry update
