---
title: Production Env Vars Registry
audience: dev
created: 2026-05-14
last-reviewed: 2026-05-19
status: living
---

# Production Env Vars Registry

**Owner:** DevOps + Tech Lead
**Last-Updated:** 2026-05-14
**Rule:** [`.claude/rules/production-env-config-registry.md`](../../.claude/rules/production-env-config-registry.md)
**Audit:** `bash scripts/audit-env-coverage.sh`

Single source of truth listing every `${VAR:<default>}` env-var reference whose default would not function in production, plus its override mechanism. Per rule §1, every suspect default MUST be overridden in production OR explicitly listed as `ACCEPTED-default` with rationale.

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
| 3 | subscription | `EMAIL_SERVICE_URL` | `kitehub-subscription/application.yml:167` | http://localhost:8083 | docker-compose env | 🆕 Just-added (GAP-508 P0) | Internal docker network: http://kitehub-email:8084 |
| 4 | email | `EMAIL_PROVIDER` | `kitehub-email/application.yml:29` | mock | docker-compose env | 🆕 Just-added (GAP-508 P0) | Production: resend (per ADR-025 Stream A) |
| 5 | email | `AWS_SES_FROM_EMAIL` | `kitehub-email/application.yml:35` | noreply@localhost | docker-compose env | 🆕 Just-added (GAP-508 P0) | Production: noreply@kitehub.me |
| 6 | email | `AWS_SES_FROM_NAME` | `kitehub-email/application.yml:36` | "Local Dev Platform" | docker-compose env | 🆕 Just-added (GAP-508 P0) | Production: "KiteHub Beta" |
| 7 | email | `MANAGEMENT_HEALTH_MAIL_ENABLED` | (Spring Boot default true) | true | docker-compose env | ✅ Overridden (GAP-506 Sub-B) | Production: false (Resend HTTP, not SMTP) |
| 8 | email | `RESEND_API_KEY` | `kitehub-email/application.yml` (Resend block) | empty | fetch-secrets.sh → /etc/kite/.env | ❌ **MISSING** | **Phase 2 follow-up:** populate AWS Secrets Manager + extend fetch-secrets.sh to pull |
| 9 | branding | `CDN_DOMAIN` | `kitehub-branding/application.yml:158` | localhost:9100 | docker-compose env | ❌ Missing | Phase 1 BETA assets served via Vercel; revisit if/when CDN provisioned |
| 10 | all kitehub-* | `SPRING_DATASOURCE_URL` | various | jdbc:postgresql://localhost:5433/... | fetch-secrets.sh → /etc/kite/.env | ✅ Overridden | RDS endpoint via secret |
| 11 | all kitehub-* | `SPRING_RABBITMQ_HOST` | various | localhost | fetch-secrets.sh writes SPRING_RABBITMQ_HOST=kite-rabbitmq | ✅ Overridden | Internal docker host |
| 12 | all kitehub-* | `SPRING_RABBITMQ_USERNAME/PASSWORD` | various | guest | fetch-secrets.sh + deploy-prod.sh Step 6.5 self-heal (GAP-504) | ✅ Overridden | Dynamic per deploy until populate-secrets.sh stabilizes (done 2026-05-13) |
| 13 | all kitehub-* | `SPRING_DATA_REDIS_HOST` | various | localhost | fetch-secrets.sh writes SPRING_DATA_REDIS_HOST=kite-redis | ✅ Overridden | Internal docker host |
| 14 | all kitehub-* | `JWT_SECRET` / `ENCRYPTION_KEY` | various | (no default) | fetch-secrets.sh → /etc/kite/.env | ✅ Overridden | AWS Secrets Manager (terraform random_password) |
| 15 | kitehub-subscription, kitehub-admin | `JWT_CHALLENGE_SECRET` | `kitehub-subscription/application.yml` (2FA challenge token) | dev default ≤40 bytes | fetch-secrets.sh → /etc/kite/.env (Wave 81 Bucket F PR #1388) | ✅ Overridden | AWS Secrets Manager `kitehub/production/jwt-challenge-secret` (created 2026-05-15 manual via Wave 81 jwt-secret-fix-runbook). Wave 79 Bucket C `ChallengeTokenService.@PostConstruct` fail-fast guard enforces non-dev-default in production profile |
| 16 | kitehub-subscription, kitehub-admin | `TOTP_ENCRYPTION_KEY` + `KITEHUB_AUTH_TOTP_ENCRYPTION_KEY` (dual-write) | `kitehub-subscription/application.yml:109` (TOTP secret AES-256 encryption at rest) | dev default 36 bytes (`dev-key-32-chars-pad-pad-pad-pad-pad`) | fetch-secrets.sh → /etc/kite/.env (Wave 81 Bucket F PR #1389 + PR #1390 dual-write) | ✅ Overridden | AWS Secrets Manager `kitehub/production/totp-encryption-key`. **Dual-write rationale:** subscription yaml line 109 uses explicit `${TOTP_ENCRYPTION_KEY}` binding; kitehub-admin yaml-less → Spring relaxed binding của property `kitehub.auth.totp.encryption-key` tìm env var `KITEHUB_AUTH_TOTP_ENCRYPTION_KEY` (camelCase → SCREAMING_SNAKE_CASE). Both names point cùng secret value. Wave 72b GAP-516 `TotpSecretCipher.@PostConstruct` fail-fast guard mandate non-dev-default + ≥32 bytes trong production profile |
| 17 | kitehub-subscription, kitehub-admin | `KITEHUB_STAFF_INVITATION_SIGNING_SECRET` | `InvitationTokenService.java` `@Value("${kitehub.staff.invitation.signing-secret:...}")` | dev default 36 bytes (`dev-invitation-secret-32-bytes-pad-pad`) | fetch-secrets.sh → /etc/kite/.env (Wave 81 Bucket F PR #1389) | ✅ Overridden | AWS Secrets Manager `kitehub/production/staff-invitation-signing-secret` (created 2026-05-15 manual). Wave 78 GAP-548 `InvitationTokenService.@PostConstruct` fail-fast guard mandate non-dev-default + ≥32 bytes trong production profile |

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

| Var | Default | Rationale |
|-----|---------|-----------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | http://localhost:4318 | No OTel collector deployed Phase 1 BETA (GAP-115 backlog) |
| `AI_OLLAMA_BASE_URL` | http://localhost:11434 | AI deferred Phase 2 per ADR-026 |
| `PAYMENT_RETURN_URL` / `PAYMENT_NOTIFY_URL` | localhost | Payment deferred Phase 1.5 per release-1-deploy-plan |
| `SMTP_HOST` / `SMTP_PORT` | kite-mailhog / 1025 | Resend HTTP API used (per ADR-025), SMTP path inactive |
| `S3_ENDPOINT` | http://localhost:9000 | Phase 1 BETA may not use S3 directly; revisit if branding asset upload added |
| `DATABASE_ADMIN_URL` / `DATABASE_MASTER_*` | localhost | Admin bootstrap path; production uses managed RDS, admin SQL via psql direct |

## Phase 2/3 actions

### Phase 2 — RESEND_API_KEY provisioning (GAP-508 Phase 2)

**Status:** ❌ MISSING in production (verified 2026-05-13 via `docker exec kitehub-email env | grep RESEND`).

Plan (when ready):
1. Provision Resend account + verify domain `kitehub.me` (DNS DKIM/SPF/DMARC)
2. Generate Resend API key
3. Store in AWS Secrets Manager: `aws secretsmanager create-secret --name kitehub/production/resend-api-key --secret-string "<key>"`
4. Extend `scripts/fetch-secrets.sh` to pull this secret + write `RESEND_API_KEY=...` to /etc/kite/.env
5. Re-deploy → kitehub-email reads key on boot
6. Live verify: `docker exec kitehub-email env | grep RESEND_API_KEY` non-empty
7. Test send: POST /api/v1/auth/request-beta-access → check Resend dashboard for delivery + user inbox

Blocks: Plan 1 self-test Bước 5 (user receives invite email + clicks link).

### Phase 3 — CI gate (GAP-508 Phase 3)

When registry stabilizes (~7 days post-rule-creation per `incident-to-rule-pipeline.md` premature-rule guard):
1. Add `.github/workflows/script-quality.yml` job `env-coverage`
2. Run `bash scripts/audit-env-coverage.sh` on PRs touching `application*.yml`
3. FAIL build if scan FAIL
4. Reviewer-checklist line in PR template

## Update workflow

When adding new `${VAR:default}` in source code:
1. Identify suspect default (per rule §2)
2. Choose override mechanism (per rule §4)
3. Add row to this registry SAME PR
4. Run `bash scripts/audit-env-coverage.sh` locally — must PASS or add to acceptable list with rationale
5. PR description references registry update
