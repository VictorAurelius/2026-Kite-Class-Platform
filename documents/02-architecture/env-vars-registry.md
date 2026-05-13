# Production Env Vars Registry

**Owner:** DevOps + Tech Lead
**Last-Updated:** 2026-05-13
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
