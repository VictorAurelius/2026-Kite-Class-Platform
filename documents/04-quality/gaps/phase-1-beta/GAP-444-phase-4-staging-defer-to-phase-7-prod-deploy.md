# GAP-444: Phase 4 staging deploy artifacts — defer to Phase 7 production prep

**Status:** 🟡 PARTIAL — Phase 3 image push ✅ SHIPPED; Phase 4 staging deploy artifacts deferred to Phase 7 T-7 prep window
**Priority:** 🟠 P1 — required before v0.9.0-beta production deploy
**Domain:** DevOps / Release engineering
**Found:** 2026-05-08 (Phase 3 staging.8 SHIPPED retro — Phase 4 prerequisites missing)
**Affects:** Production deploy readiness for v0.9.0-beta tag

## Problem

Phase 3 (image push to ECR) shipped via `v0.9.0-beta-staging.8` ✅. Phase 4 (staging deploy gate per `release-1-deploy-runbook.md` §4) blocked because:

1. `docker-compose.staging.yml` does NOT exist in repo. Workflow `.github/workflows/deploy-staging.yml:110` references it.
2. `deploy-staging.yml` workflow `kitehub/$service` image naming wrong (ECR uses `kite/kitehub-*` namespace per CLAUDE.md Docker Naming Convention)
3. `deploy-staging.yml` workflow REBUILDS + REPUSHES images (line 75-87), redundant with Phase 3 + risks Trivy regression
4. `STAGING_INSTANCE_ID` GH secret not set (preflight Cat #7 finding)
5. EC2 `/opt/kite-staging` directory not bootstrapped — workflow `cd /opt/kite-staging` fail
6. EC2 user_data installs Docker + ECR login script but does NOT pull images or start any compose stack

## Root Cause

Phase 4 staging architecture from earlier roadmap (separate staging EC2 + dedicated compose file) was scaffold-only — was never wired end-to-end. Phase 1 BETA invite-only context per ADR-025 has 2 production-grade EC2 instances (`kh-backend` + `kc-app`) already running; separate staging tier provides no signal value at solo-dev + ~10-20 invite tenant scope.

## Decision: Defer Phase 4 to Phase 7 production deploy work

Per `release-fix-retry-budget.md` §4 pivot matrix (workflow over-spec'd; multiple gates failing):
- ❌ Continued patching Phase 4 staging deploy infrastructure = wasted iteration
- ✅ Combine Phase 4 deploy artifacts into Phase 7 production deploy work, where they'll be built once + correctly for actual prod target

Phase 7 (production deploy v0.9.0-beta launch) per `release-1-deploy-runbook.md` §7 T-7 → T-0 prep window will produce:
- `docker-compose.production.yml` (NEW — replaces non-existent `docker-compose.staging.yml`)
- Fixed `deploy-production.yml` workflow (correct ECR naming + skip rebuild + correct compose ref)
- EC2 `/opt/kite-prod` bootstrap (compose file + .env populated from AWS Secrets Manager)
- SSM run-command tested end-to-end on production EC2

Phase 1 BETA risk acceptance (per ADR-025 + release-deploy-standard.md §3.1):
- Invite-only ~10-20 tenants — no concurrent staging traffic to validate against
- Single EC2 architecture — staging environment ≠ prod environment difference is minimal (same t3.medium amd64)
- Manual smoke-test on production EC2 with first invite tenants serves same signal Phase 4 E2E was designed to surface
- Phase 1 BETA disclaimer banner shipped Wave 23 covers "this is BETA" risk acceptance

## Proposed Fix (= Phase 7 T-7 prep checklist)

When Phase 7 prep window opens:

### 7.1.1 Create production compose file
- [ ] `docker-compose.production.yml` at repo root
  - 9 service entries with `image: 906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/kite/<service>:${KITE_VERSION}`
  - Network: shared `kite-network` for inter-service comms
  - Healthchecks per service
  - Resource limits (per ADR-025 RAM partitioning §"Negative consequences" t3.medium 2-4GB)
  - Restart policy: `unless-stopped`
  - env_file references `/etc/kite/.env` (populated by SSM secrets-fetcher)

### 7.1.2 Bootstrap EC2 directory + secrets fetcher
- [ ] SSM run-command to `i-0b65c3947d36cae61` (kh-backend) + `i-04f65503ace7febe4` (kc-app):
  ```
  mkdir -p /opt/kite-prod /etc/kite
  # Pull docker-compose.production.yml from S3 or git via deploy-key
  # Generate /etc/kite/.env from `aws secretsmanager get-secret-value` for each
  # of: db-password, jwt-secret, encryption-key, rabbitmq-default-creds
  ```

### 7.1.3 Fix deploy-production.yml workflow
- [ ] Drop redundant `docker build + push` (Phase 3 docker-build-push.yml already pushes tag-driven)
- [ ] Reference `kite/<service>` ECR namespace (currently mistypes as `kitehub/<service>`)
- [ ] Compose ref `docker-compose.production.yml`
- [ ] Working dir `/opt/kite-prod`

### 7.1.4 Provision missing GitHub Variables/Secrets
- [ ] `secrets.STAGING_INSTANCE_ID` → use `i-0b65c3947d36cae61` (kh-backend) since Phase 7 deploys to prod EC2 directly (rename to `secrets.PROD_KH_INSTANCE_ID`?)
- [ ] OR add `secrets.PROD_KH_INSTANCE_ID` + `secrets.PROD_KC_INSTANCE_ID` (2 EC2 split deploy)
- [ ] (Phase 4-only deferred) `secrets.LHCI_GITHUB_APP_TOKEN`, `secrets.BACKUP_S3_BUCKET`, `vars.BACKUP_DRILL_ENABLED` per preflight Cat #7

### 7.1.5 First-deploy SSM exec test
- [ ] Trigger workflow_dispatch on `deploy-production.yml`
- [ ] Verify SSM `aws ssm send-command` 200 OK
- [ ] Verify on EC2: 6-8 containers Up via `docker compose ps`
- [ ] Verify `curl https://<alb-dns>/actuator/health` → 200

### 7.1.6 Run smoke test
- [ ] `bash scripts/smoke-test.sh https://<alb-dns> https://<kc-alb-dns>` per Wave 26 GAP-377
- [ ] 18 assertions pass

### 7.1.7 First invite tenant signup test
- [ ] Coordinator self-test signup with own email per Wave 33 GAP-372 beta-invite flow
- [ ] Tenant provisioned with beta-flag=true + dashboard banner

## Acceptance Criteria

- [ ] `docker-compose.production.yml` committed to repo
- [ ] `deploy-production.yml` workflow fixed (ECR naming + skip rebuild)
- [ ] EC2 bootstrapped with compose file + .env via SSM run-command
- [ ] Workflow_dispatch → SSM deploy 200 OK
- [ ] Smoke test 18/18 pass against ALB DNS
- [ ] First coordinator-self-test signup succeeds
- [ ] Status flip `🟡 PARTIAL → 🟢 DONE` only when all above checked

## Phase 1 BETA risk acceptance (this PARTIAL state)

Phase 4 staging deploy gate skip is documented exception per:
- `release-deploy-standard.md` §3.1 PRE-RELEASE artifact set: staging environment listed as P1 STRONGLY recommend, NOT P0 BLOCKING
- `release-fix-retry-budget.md` §4 pivot matrix: workflow over-spec'd → relax for tag class
- ADR-025 single-EC2 Architecture B = staging-prod parity is structural (same instance shape + Docker Compose), not test-environment-driven
- Solo-dev mode + invite-only ~10-20 tenants = manual smoke-test signal value > E2E gate value

Phase 7 prep window (T-7 → T-0) will compress all 7 sub-tasks into single sprint. ETA = ~3-4h focused work; can be wave-pack parallelized (compose file + workflow fix + EC2 bootstrap + smoke test = 4 disjoint buckets).

## Related

- Parent: `release-1-deploy-runbook.md` Phase 4 (staging) + Phase 7 (production)
- Sibling preflight findings:
  - `secrets.STAGING_INSTANCE_ID` missing (Cat #7)
  - `secrets.RDS_ENDPOINT` ✅ provisioned 2026-05-08
  - `secrets.REDIS_ENDPOINT` ✅ provisioned 2026-05-08
- Standards: AWS Well-Architected OPS-04 + Twelve-Factor V (build/release/run separation)
- Rule applied: `release-fix-retry-budget.md` v1.0.0 §4 pivot matrix

## Log

- 2026-06-01 — **Wave meta-8 Bucket B SCOPE-REVISE:** SCOPE-REVISE — defer-by-design; Phase 3 image push DONE; Phase 4 staging deferred Phase 7 T-7 prep; consider DROP or reclassify as deferred-by-design tracker CSV completion_pct adjusted to 30%; gap body Status/AC reflect documented scope BEFORE Wave meta-7 audit — re-read audit artifact for current empirical reality. Source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-d-p1-partial.md`.

- **2026-05-08** Filed during Phase 3 SHIPPED retro (staging.8 success). Phase 4 deploy infrastructure was scaffold-only; no compose file + multiple workflow bugs surfaced when attempting to ship. Per `release-fix-retry-budget.md` §4 pivot matrix: defer Phase 4 to Phase 7 prep where prod-equivalent artifacts will be built once correctly. Phase 1 BETA risk acceptance documented inline.
EOF
