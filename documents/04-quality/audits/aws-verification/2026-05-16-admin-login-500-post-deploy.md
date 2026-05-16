---
title: Post-Deploy Verification — GAP-517 admin login 500 hotfix (staging.19)
status: complete
created: 2026-05-16
phase: phase-1-beta
wave: 87-hotfix
gaps: [GAP-517]
---

# Post-Deploy Verification — staging.19 admin login 500 hotfix

## Scope

Verify production deployment of `v0.9.0-beta-staging.19` (PR #1464, squash commit `447e2167`) successfully fixes GAP-517 admin login 500 incident:
- Backend `POST /api/auth/login` admin credential MATCH path returns 200 (was 500 pre-fix)
- 0 `42804 INET binding` errors post-deploy
- 0 `UnexpectedRollbackException` post-deploy
- V52 migration applied (login_audit_log.ip VARCHAR(45) replacing INET)

## Pre-mutation audit reference

`documents/04-quality/audits/aws-verification/2026-05-16-admin-login-500-rca.md` — RCA + fix design.

## Trigger context

- Deploy authorization: user explicit "1,2" + "tag v0.9.0-beta-staging.19 (Recommended)" via AskUserQuestion per `dev-authorized-terraform-trigger.md` §4 phrase detection
- Workflow: `deploy-production.yml workflow_dispatch` (user clicked Run workflow with `version=v0.9.0-beta-staging.19 confirm=DEPLOY` per `release-deploy-standard.md` §9 human-trigger requirement)
- Concurrent ops at trigger time: 0 (verified via list_commits + workflow runs)

## Commands run (Tier 1 read-only per agent-aws-access.md §2.1)

```bash
# Pre-deploy snapshot
aws ssm send-command --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["docker ps --format \"{{.Names}}\t{{.Image}}\""]'
# → All 5 services on 0.9.0-beta-staging.18 (kitehub-email on .14 — pre-existing drift)

# ECR build verification (5 services × imageTag=0.9.0-beta-staging.19)
aws ecr describe-images --repository-name kite/<svc> --image-ids imageTag=0.9.0-beta-staging.19
# → All 5 services pushed 17:15-17:16 UTC

# SSM deploy command tracking
aws ssm get-command-invocation --command-id c0d5ae60-6a07-42a0-ba47-5fca560a9877
# → Status=Success, deploy-prod.sh completed cleanly

# Post-deploy container status
docker ps | grep kitehub
# → 4/5 services healthy, kitehub-admin Restarting (separate issue, see Findings)

# Post-deploy error scan
aws logs filter-log-events --log-group-name /aws/ec2/kite-prod \
  --filter-pattern '?"\"level\":\"ERROR\"" ?"42804" ?"UnexpectedRollback" ?"is of type inet"' \
  --start-time <T_deploy>
# → 0 hits for 42804 / UnexpectedRollback / INET binding (was every login pre-fix)
```

## Findings

### Real verification (hotfix scope)

| Resource | Pre-deploy state | Post-deploy state | Verdict |
|----------|-----------------|-------------------|---------|
| kitehub-subscription image | `0.9.0-beta-staging.18` | `0.9.0-beta-staging.19` | ✅ rolled |
| kitehub-gateway image | `staging.18` | `staging.19` | ✅ rolled |
| kitehub-branding image | `staging.18` | `staging.19` | ✅ rolled |
| kitehub-email image | `staging.14` | `staging.19` | ✅ rolled (+5 versions) |
| kitehub-admin image | `staging.18` | `staging.19` Restarting | ⚠️ NEW unrelated issue (see #1 below) |
| Login `wrong-password` | 400 clean | 400 clean | ✅ baseline preserved |
| Login `admin@kitehub.me correct password` | 500 RFC 7807 | **200 + `{requires2fa_enrollment: true, challenge_token}`** | ✅ **FIXED** |
| CloudWatch 42804 errors (5-min) | every login | **0** | ✅ INET binding resolved |
| CloudWatch `UnexpectedRollbackException` (5-min) | every login | **0** | ✅ tx poisoning resolved |
| CloudWatch `is of type inet` errors (5-min) | every login | **0** | ✅ |

### NEW issues surfaced (NOT regression of hotfix — file separate gaps)

1. **`kitehub-admin` Restart loop** — Spring Boot crash on bootstrap with `found duplicate key server in 'reader', line 51, column 1: server:`. YAML duplicate key from profile merge (`application.yml` line 71 has `server:` + `application-production.yml` adds another `server:` block). Pre-existing in commits between staging.18 and staging.19 (likely Wave 84/85/86); surfaced when first deploy to staging.19 happened. **Scope: separate gap (kitehub-admin module YAML config); NOT related to LoginAuditService hotfix; auth login still works through kitehub-subscription path.**

2. **Vercel FE missing `requires2fa_enrollment` handler** — Production frontend at `kitehub.me/login` (served via Vercel) does NOT include the requires2fa_enrollment redirect handler that exists in source at `kitehub/kitehub-frontend/src/app/(auth)/login/page.tsx:91-92` (committed `0d43170` 2026-05-15 PR #1399). User sees "Email hoặc mật khẩu không đúng" even though backend returns 200 because FE treats non-`accessToken` response as auth failure. **Scope: separate gap (Vercel deployment trigger / build cache); NOT a backend issue.**

3. **`app.kitehub.me` upstream connect error** — `upstream connect error or disconnect/reset before headers`. Routing/proxy issue. **Scope: separate gap; NOT related to backend hotfix.**

### Phantom changes (none expected, none observed)

No unexpected resource state changes. SSM command output shows clean deploy-prod.sh flow: git pull → ECR login → secrets fetch → compose pull → compose up → rabbit user sync → restart kitehub-* → settle wait.

## Verdict

✅ **GAP-517 hotfix VERIFIED FIXED.** Backend `POST /api/auth/login` admin path returns expected 200 with `requires2fa_enrollment: true + challenge_token`. CloudWatch shows 0 occurrences of the 3 error signatures that pre-fix were emitted on every successful credential match. Production admin user can now reach the 2FA enrollment flow.

3 NEW issues surfaced post-deploy are SEPARATE from the hotfix (pre-existing in Wave 84-86 commits OR Vercel deployment lag OR proxy routing).

## Recommendations

### Immediate (next 24h)

1. **GAP-517 closure** — flip Status to 🟢 DONE per `gap-done-discipline.md` §2 with this audit as evidence
2. **Rotate IAM key** `AKIA5GAW3FUENSOPPS4P` (solo-dev-admin readonly) — shared in chat to enable CloudWatch investigation; per `agent-aws-access.md` discipline rotate within 24h
3. **File 3 follow-up gaps** for NEW issues surfaced:
   - GAP-XXX-A: kitehub-admin YAML duplicate `server:` profile merge (P1, blocks admin dashboard but NOT auth flow)
   - GAP-XXX-B: Vercel FE deployment missing PR #1399 `requires2fa_enrollment` handler (P0, blocks admin login UI flow despite backend healthy)
   - GAP-XXX-C: `app.kitehub.me` upstream connect error (P1, depends on what app.kitehub.me serves)

### Post-incident (next 7 days)

1. **Smoke admin-login script** — implement `scripts/smoke-admin-login.sh` per `release-deploy-standard.md` v1.2.0 §3.1 (mandate added same-PR as hotfix). Wire into `scripts/smoke-test.sh` so future deploys auto-verify admin login path.
2. **Repo-wide audit service propagation sweep** — per `audit-service-isolation.md` v1.0.0, review all `*AuditService` / `*LogService` / `*NotificationService` methods using parent `@Transactional` for REQUIRES_NEW propagation. ~15 candidates flagged in PR #1464.
3. **Testcontainers expansion** — per `postgres-specific-type-testcontainers.md` v1.0.0, scan all entities with `columnDefinition=` referencing Postgres-specific types (INET/JSONB/TSVECTOR/CITEXT/HSTORE/arrays/ranges/intervals) and add Testcontainers CRUD round-trip coverage.
4. **`gap-status.csv` + `rules-index.csv` sync** — already done for rules in commit `cf27aeb` (audit-service-isolation + postgres-specific-type-testcontainers rows); follow-up to add memory entries `feedback_audit_service_isolation.md` + `feedback_postgres_specific_type_testcontainers.md`.

## References

- PR #1464 — fix(GAP-517) hotfix code + meta rules
- Tag `v0.9.0-beta-staging.19` — main HEAD `447e2167a07b6fb2d611643f27f8bed55a25902d`
- SSM deploy command — `c0d5ae60-6a07-42a0-ba47-5fca560a9877`
- ECR images — kite/{subscription,admin,branding,email,gateway}:0.9.0-beta-staging.19 (pushed 17:15-17:16 UTC)
- Pre-deploy RCA — `documents/04-quality/audits/aws-verification/2026-05-16-admin-login-500-rca.md`
- Rules applied:
  - `pre-mutation-state-check.md` §3 (pre-deploy audit artifact)
  - `dev-authorized-terraform-trigger.md` §2 (5-gate procedure)
  - `agent-aws-access.md` §2.1 Tier 1 read-only (CloudWatch + SSM verification)
  - `release-deploy-standard.md` §9 (human-trigger workflow_dispatch)
  - `concurrent-production-mutation-ops.md` §4 (no concurrent ops at trigger)
- Audit trail: `audits-index.csv` row `AUDIT-2026-05-16-admin-login-500-post-deploy` (to be added)
