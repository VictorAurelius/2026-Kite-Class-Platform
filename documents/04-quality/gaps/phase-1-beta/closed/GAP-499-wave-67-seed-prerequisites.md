# GAP-499: Wave 67 production seed prerequisites — 5 bugs + 2 risks pre-execution

**Status:** 🟡 PARTIAL — code/docs/CI shipped; remaining = user trigger workflow_dispatch + verify result
**Scope expansion 2026-05-13:** added CI workflow conversion (PR #1240 prep + PR #1241) — removes local Maven build + SSM tunnel + session-manager-plugin dependency
**Priority:** 🔴 P0 BLOCKING (Wave 67 entry — GAP-376 production seed cannot execute without these)
**Domain:** DevOps / Documentation
**Found:** 2026-05-13 (pre-execution audit per `pre-mutation-state-check.md` §1.5)
**Affects:** Wave 67 first production seed execution, runbook usability

## Problem

User-flagged "verify Wave 67 trước khi execute" surfaced 5 bugs + 2 risks blocking `bash scripts/seed-production.sh` from succeeding:

### Bug 1: Script default `admin@kitehub.vn` (sai domain post Path C decision)

`scripts/seed-production.sh:21,69` defaulted to `admin@kitehub.vn`. Per GAP-458 (Path C `kitehub.me` per AWS Activate Founders Pack rejection) + GAP-459 (sweep stale `.vn` refs) + GAP-497 (.vn deferred Phase 2), domain hiện tại = `kitehub.me`. Violation of `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync — `.vn` ref missed during Wave 61 sweep.

### Bug 2: Runbook references WRONG secret prefix

`production-seed-runbook.md` §3.2 used `kite/prod/database-url`, `kite/prod/seed-admin-password`. Actual AWS prefix per `kitehub/production/*` (per GAP-482 retry #3 PR #1200 fix that swept code; runbook not swept).

### Bug 3: Missing `seed-admin-password` secret

`aws secretsmanager list-secrets` returns 9 secrets; `seed-admin-password` NOT among them. Wave 33 seed runner expects `SEED_ADMIN_PASSWORD` env var (sourced via secrets manager). GAP-379 secrets management didn't ship this specific entry.

### Bug 4: Script designed for LOCAL maven workspace, runbook ambiguous about execution context

`scripts/seed-production.sh:96-116` searches `kitehub/kitehub-subscription/target/*.jar` and launches local JVM. Production EC2 has Docker containers only (no source/build artifacts). Runbook §3 didn't specify execution context (local + tunnel vs on-EC2 docker exec).

### Bug 5: RDS `PubliclyAccessible=false` — local machine cannot reach DB

```
PubliclyAccessible: false
Endpoint: kitehub-postgres.c3awuqw4ugex.ap-southeast-1.rds.amazonaws.com (private VPC)
```

Without SSM port-forward or bastion, `DATABASE_URL` from local is unreachable. Runbook didn't document the tunnel pattern.

### Risk 1: SES sandbox + `admin@kitehub.me`

If seed inserts admin user → app dispatches verification email to `admin@kitehub.me` → SES sandbox rejects (production access pending AWS reply per GAP-370, submitted 2026-05-12).

### Risk 2: Ad-hoc local Java JVM with remote RDS

Local `java -jar` connects via tunnel localhost:5432 → RDS. Works but ugly (no clean exit after seed runs as ApplicationRunner; user Ctrl+C after "seed complete" log).

## Proposed Fix (this PR scope)

### Bug 1 — Script domain sweep
- `scripts/seed-production.sh` default `admin@kitehub.vn` → `admin@kitehub.me`

### Bug 2 — Runbook secret paths
- `production-seed-runbook.md` §3.2: `kite/prod/*` → `kitehub/production/*`
- Use `db-password` JSON structure (existing) — extract username/password/host from JSON via jq instead of separate secrets

### Bug 3 — Provision `seed-admin-password` via terraform
- `infrastructure/terraform-aws/secrets.tf`: add `random_password.seed_admin` + `aws_secretsmanager_secret.seed_admin_password` + version block
- Follows pattern from `random_password.jwt` + lifecycle ignore_changes per GAP-450 Option B
- User triggers `terraform-apply.yml` post-merge to provision

### Bug 4+5 — Execution context section
- Runbook new §3.0 "Execution context (where to run)" — Option A (SSM port-forward tunnel + local build) + Option B (SSM session into EC2 + docker exec)
- Pre-requisites: SSM session-manager plugin installed locally
- Recommends Option A for first run (visibility); Option B for recovery

### Risk 1 (deferred verify) — SES sandbox check
- Pre-seed user must run: `aws ses get-account --query 'ProductionAccessEnabled'`
- If `true` → proceed. If `false` → either (a) wait AWS reply (GAP-370), OR (b) verify `admin@kitehub.me` as test recipient in SES sandbox first, OR (c) accept seed without email verification (admin user inserts; password rotate next step manually)

### Risk 2 (acknowledged) — ApplicationRunner doesn't auto-exit
- Documented in runbook: user observes "seed complete in X.Xs" log, then Ctrl+C
- Future improvement: add CLI flag for one-shot mode (out of scope this gap)

## Acceptance Criteria

- [x] Script default email → `admin@kitehub.me`
- [x] Runbook §3.2 secret paths → `kitehub/production/*`
- [x] Runbook new §3.0 Execution Context section with Option A/B
- [x] `secrets.tf` adds `seed_admin_password` secret resource
- [ ] User triggers `terraform-apply.yml` post-merge to create secret in AWS
- [ ] User verifies SES `ProductionAccessEnabled` OR accepts risk path
- [ ] First Wave 67 seed run succeeds end-to-end → flip GAP-499 + GAP-376 DONE

## Related

- Parent: GAP-376 (production data seed — Wave 67 entry)
- Sister: GAP-379 (secrets management), GAP-370 (SES production access)
- Decision precedent: GAP-458 / GAP-459 / GAP-497 (Path C `kitehub.me` domain)
- Rule violations surfaced: `audit-to-gap-pipeline.md` §2.7 (Bug 1+2), `pre-mutation-state-check.md` §1.5 (this whole audit)

## Log

- **2026-05-13 (PR #1241 — CI conversion):** User-requested "convert ngay" after rule audit confirmed local script ≠ rule violation but workflow_dispatch IS preferred pattern per `release-deploy-standard.md` §9. Sister of `deploy-production.yml`. Shipped:
  - `.github/workflows/seed-production.yml` — workflow_dispatch + confirm=SEED gate + RDS preflight + secret existence check + SSM send-command + log-marker poll (sister of GAP-498 pattern)
  - `scripts/seed-prod-ec2.sh` — on-EC2 script: fetches seed-admin-password + injects KITE_SEED_* into /etc/kite/.env + restarts kitehub-subscription + waits for `ProductionSeedRunner: seed complete` marker + cleans seed env post-run
  - Removes dependencies: local Maven build, SSM session-manager-plugin, port-forward tunnel, ad-hoc `ps aux` secret exposure
  - IAM reuse: github_deploy role already has SSM SendCommand + secrets read + rds describe (scoped `kitehub/production/*`) — no terraform-apply needed
  - Trigger: `gh workflow run seed-production.yml -f confirm=SEED`
- **2026-05-13 (PR #1240 — initial fixes):** Pre-execution audit per `pre-mutation-state-check.md` §1.5 surfaced 5 bugs + 2 risks blocking local script execution. Code/docs/terraform fixes shipped:
  - Script `scripts/seed-production.sh` default email `.vn` → `.me`
  - Runbook secret paths `kite/prod/*` → `kitehub/production/*`
  - Runbook §3.0 Execution Context section (Option A SSM tunnel + Option B docker exec)
  - `secrets.tf` adds `seed-admin-password` secret (terraform-applied 2026-05-13 04:29Z, 32-char password live)
  - SES `admin@kitehub.me` verified (inherited domain DKIM) — sandbox sending unblocked
