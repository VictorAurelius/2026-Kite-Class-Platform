# Handoff — Production config-audit (kc-app + subscription env gaps)

**Date:** 2026-06-18
**Context at handoff:** 81% (820k/1M) — config-fix deploy deferred to fresh session
**Trigger:** deploy FE+S3+seed session surfaced 3 pre-existing prod env-config gaps blocking kiteclass-core + beta-signup

---

## ✅ Delivered + verified this session

- **FE (kc-app-fe i-05cfda7c6c60b683f):** docker FE deployed (kitehub-frontend :4701, kiteclass-frontend :4700) replacing PM2. `https://kitehub.me/beta-signup/code?code=...` → **200** (was 404). landing kitehub.me + app.kitehub.me = 200. NEXT_PUBLIC baked correct.
  - Granted `AmazonEC2ContainerRegistryReadOnly` to role `kitehub-production-kc-app-fe` (was missing ECR pull). **IaC follow-up:** add to terraform iam.tf.
- **PRs:** #2489 (S3 IAM wiring + FE build-args) + #2487 (trio-seed Hà/Nhì + theme-reset fix) + #2488 (thesis docs) merged; #2486 closed (redundant).
  - Fixed real bug in #2487: `ThemeContext` reset no-op (added `removeThemeVariables()` on DEFAULT).
- **S3 infra:** bucket `kiteclass-files-production-906286017800` + versioning/encryption/PAB/CORS/lifecycle created; EC2 instance-role `kitehub-production-ec2-app` S3 grant extended. GAP-1480 = terraform import follow-up. Audit: `documents/04-quality/audits/aws-verification/2026-06-18-kiteclass-core-s3-storage-wiring.md`.
- **Subscription seed:** kitehub-subscription (kh-backend i-05d7af46d01436b96) on `latest`+demo-seed → DemoTrioInstanceSeeder created 2 instances in **kitehub DB**: `co-ha-toan` (a1100000-...-0001, FREE = cô Hà) + `thay-nhi-hoa` (b1100000-...-0002, PREMIUM = thầy Nhì).

---

## 🔴 3 pre-existing prod env-config gaps (NOT caused by this deploy — masked by kiteclass-core crash-loop)

kiteclass-core has been **crash-looping on prod** independent of today. Root: kc-app `/etc/kite/.env` (from `fetch-secrets.sh`) is a SINGLE shared env that doesn't fit kiteclass-core's needs.

### Gap 1 — kiteclass-core wrong DB ✅ root-caused, fix identified
- `fetch-secrets.sh:193` writes ONE `SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}` with `DB_NAME` from secret = **`kitehub`**.
- kiteclass-core (`application.yml:26` `${SPRING_DATASOURCE_URL:...}`) → connects to **`kitehub` DB** (subscription's), sees subscription's 75 flyway rows → checksum mismatch + V76 (`landing_sections` needs `landing_pages`) fails.
- Correct DB = **`kiteclass_shared`** (verified EXISTS + EMPTY + kitehub user has access). Migrations V1-V100 (101 files) migrate cleanly there.
- **FIX:** `docker-compose.kc.yml` kiteclass-core `environment:` add `SPRING_DATASOURCE_URL: jdbc:postgresql://kitehub-postgres.c3awuqw4ugex.ap-southeast-1.rds.amazonaws.com:5432/kiteclass_shared` (override shared env_file). Verified working via hot-patch override `dc-fix.yml` — Flyway migrated kiteclass_shared cleanly past V76.

### Gap 2 — kiteclass-core RabbitMQ ACCESS_REFUSED (next blocker after Gap 1)
- `deploy-kc.sh` runs `docker compose up` with only `--preserve-env=KITE_VERSION` → `${RABBITMQ_USER}` UNSET when creating `kite-rabbitmq` → container default user is **blank** (warning "RABBITMQ_USER not set").
- kiteclass-core connects with `SPRING_RABBITMQ_USERNAME=${RMQ_USER}` (real, from `fetch-secrets.sh:209`) → **mismatch → ACCESS_REFUSED** → fatal listener startup → crash.
- **FIX (one of):** (a) `deploy-kc.sh`/`deploy-prod.sh`: `set -a; source /etc/kite/.env; set +a` before `docker compose up` so `${RABBITMQ_USER}/${RABBITMQ_PASS}` substitute; OR (b) compose `rabbitmq` service use `env_file` + map `RABBITMQ_DEFAULT_USER` from it. Then recreate `kite-rabbitmq` with correct creds.

### Gap 3 — subscription beta-signup 500 (tenant DB provisioning)
- beta-signup completion → `AuthService.registerFromBetaInvite` → `InstanceService.createTrialInstance` → `DatabaseProvisioningService.createPhysicalDatabase` → admin conn `${DATABASE_ADMIN_URL:jdbc:postgresql://localhost:5433/postgres}` → **localhost:5433 REFUSED**.
- `application-production.yml` sets `database.lifecycle.enabled: true` (provisioning ON) but `DATABASE_ADMIN_URL`/`DATABASE_MASTER_HOST/PORT`/`DATABASE_ADMIN_USERNAME/PASSWORD` are **NOT written by fetch-secrets** → all default to localhost:5433.
- **FIX:** `fetch-secrets.sh` add (point at RDS master): `DATABASE_ADMIN_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/postgres` + `DATABASE_ADMIN_USERNAME=${DB_USERNAME}` + `DATABASE_ADMIN_PASSWORD=${DB_PASSWORD}` + `DATABASE_MASTER_HOST=${DB_HOST}` + `DATABASE_MASTER_PORT=${DB_PORT}`. (Master user `kitehub` has CREATE DATABASE.) Verify the per-tenant DB then provisions OK (per-tenant `kiteclass_<hash>` DBs are vestigial per shared-DB model but provisioning must succeed for GAP-946 fail-loud check). Consider: is per-tenant physical-DB provisioning still wanted vs shared-DB-only? (separate design question).

---

## Current EC2 state (needs cleanup on next deploy)
- **kc-app:** kiteclass-core crash-looping on `latest` with hot-patch override `/opt/kite-prod/dc-fix.yml` (datasource→kiteclass_shared + demo-seed). Stuck at Gap 2 (rabbitmq). Other override files removed.
- **kh-backend:** subscription on `latest` + `dc-seed.yml` (demo-seed still active — idempotent, harmless; turn off after). gateway/admin/branding/email still on `0.9.0-test.1`.
- **kc-app-fe:** docker FE healthy.

## Remaining work (fresh session — config-fix deploy)
1. Repo fixes: `docker-compose.kc.yml` (Gap1) + `deploy-kc.sh`/`deploy-prod.sh` source-env (Gap2) + `fetch-secrets.sh` DATABASE_ADMIN_* (Gap3) → PR (per `production-env-config-registry.md` + `local-fix-production-parity-check.md`).
2. Deploy: re-run fetch-secrets on both EC2 → recreate kite-rabbitmq (correct creds) + kiteclass-core:latest (kiteclass_shared, no override needed once compose fixed, demo-seed once to seed academic Hà/Nhì then off) + subscription (production, demo-seed off, DATABASE_ADMIN set).
3. Verify full stack: kiteclass-core healthy + landing_pages in kiteclass_shared + academic seeded + `co-ha-toan.kitehub.me` loads + beta-signup completes 200 + S3 upload round-trip.
4. Cleanup hot-patch override files on both EC2s.
5. Per-tenant subdomain routing: confirm `*.kitehub.me` (e.g. co-ha-toan.kitehub.me) routes to kiteclass-frontend (nginx wildcard) — verify/fix.

## Key facts
- RDS: `kitehub-postgres.c3awuqw4ugex.ap-southeast-1.rds.amazonaws.com:5432`, master user `kitehub`, DBs: `kitehub` (subscription, 75 flyway), `kiteclass_shared` (empty, kiteclass-core's correct DB).
- Instances: kh-backend i-05d7af46d01436b96 / kc-app i-01ad56b0067d0213b / kc-app-fe i-05cfda7c6c60b683f.
- ECR latest images (all 9) fresh from main build run 27713323618 (2026-06-18).
