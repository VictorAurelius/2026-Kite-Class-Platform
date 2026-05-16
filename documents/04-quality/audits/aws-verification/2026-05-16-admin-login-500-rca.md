---
title: Production 500 RCA — admin@kitehub.me POST /api/auth/login
status: complete
created: 2026-05-16
phase: phase-1-beta
wave: 87-hotfix
gaps: [GAP-517]
---

# AWS Verification Report — admin login 500 RCA (2026-05-16)

## Scope

User-reported P0 production incident 2026-05-16: `POST /api/auth/login` với `admin@kitehub.me` trả 500 (`type:about:blank`, `detail:An unexpected error occurred`). Wrong-password attempt returns 400 sạch → 500 chỉ phát sinh trên code path post-credential-match. Tier 1 read-only CloudWatch investigation per `agent-aws-access.md` §2.1.

## Commands run (Tier 1 read-only)

```bash
# Identity + log group enumeration
aws sts get-caller-identity --profile dev-admin
aws logs describe-log-groups --profile dev-admin
aws ec2 describe-instances --profile dev-admin --filters Name=instance-state-name,Values=running

# Stream + event filtering (read-only CloudWatch)
aws logs describe-log-streams --log-group-name /aws/ec2/kite-prod --profile dev-admin --order-by LastEventTime --descending
aws logs filter-log-events --log-group-name /aws/ec2/kite-prod --profile dev-admin \
  --start-time $START_MS --filter-pattern '"admin@kitehub.me"'
aws logs filter-log-events --log-group-name /aws/ec2/kite-prod --profile dev-admin \
  --start-time $START_MS --filter-pattern '"<traceId>"'
```

## Findings

### Real evidence

```
ERROR: column "ip" is of type inet but expression is of type character varying
  Hint: You will need to rewrite or cast the expression.
  Position: 136
SQLState: 42804
```

Followed by:

```
WARN  LoginAuditService: LoginAuditService.recordLogin failed (login proceeds anyway): could not execute statement
INFO  AuthService: Login requires 2FA enrollment: userId=00000000-0000-0000-0000-000000000001
ERROR GlobalExceptionHandler: Internal server error
  org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
```

Reproduced consistently across 4 of 5 login attempts within the 30-min window (traceIds `094e7112…`, `3211c048…`, `0f6f7c7d…`, `22fede5c…`). 5th attempt (`87fc6a9e…`) used wrong password → 400 (rejected before reaching audit).

### Two compounding bugs

| # | Layer | Bug | Where |
|---|-------|-----|-------|
| 1 | DB binding | Entity `LoginAuditLog.ip` declared `String` with `@Column(columnDefinition="inet")`. Hibernate binds via `PreparedStatement.setString` (VARCHAR) — Postgres `inet` does NOT accept implicit varchar cast → SQLState 42804. | `kitehub-subscription/.../audit/login/LoginAuditLog.java` line ~ip field |
| 2 | Tx propagation | `LoginAuditService.recordLogin` uses default `@Transactional` (propagation=REQUIRED → joins parent txn from `AuthService.login`). When SQLException fires, Spring marks parent txn `rollback-only`. The try/catch swallows the exception inside `recordLogin` but **does NOT clear the rollback-only flag**. Parent txn ném `UnexpectedRollbackException` ở commit phase → 500. | `kitehub-subscription/.../audit/login/LoginAuditService.java:recordLogin` |

Javadoc claim "Audit failures NEVER block authentication — see LoginAuditService#recordLogin contract" (AuthService.java:388) is wrong as implemented: try/catch alone does not isolate failure across `@Transactional` boundary.

### Why not caught in CI

Unit + integration tests run against H2 in-memory database. H2 has no native `INET` type; the entity persists fine as VARCHAR there. Postgres-specific binding error only surfaces in production / staging against real Postgres. Meta-gap candidate: add Testcontainers Postgres run for repository tests.

### Verdict

- 500 reproduces on every admin@kitehub.me login attempt with correct password. P0 production blocker for admin functionality.
- Login UI broken → admin cannot access dashboard / approve beta requests / run platform ops.
- Both bugs need fix; Bug 2 alone is sufficient to restore login (audit will fail silently, matching javadoc intent); Bug 1 fix restores audit functionality.

## Prior actions verified

| Action | When | Where |
|--------|------|-------|
| V38 create login_audit_log + V42 fingerprint_hash CHAR→VARCHAR fix | Wave 72b Bucket C / hotfix | V38, V42 migrations |
| GAP-517 LoginAudit + new-fingerprint alert ship | 2026-05-13 | PR family |
| Wave 85 Bucket B RLS NULL force-fail | 2026-05-14 | V50 migration |
| Wave 86 cookie consent PDPL + lockout + threat models | 2026-05-15 | recent merges |

No prior incident artifact for this 42804 binding error. First occurrence.

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Patch A: `@Transactional(propagation=REQUIRES_NEW)` on `recordLogin` | Agent (this PR) | 1-line + import. Isolates audit txn from auth txn. |
| Patch B: V52 migration `ALTER login_audit_log.ip TYPE VARCHAR(45)` (per V42 precedent) | Agent (this PR) | Restores audit row write. 45 chars = IPv6 max. |
| Local compile + unit test verify (Maven `mvn -pl kitehub-subscription verify`) | Agent | Required by `admin-merge-discipline.md` §3 before merge |
| Deploy production via `deploy-production.yml` workflow_dispatch | **User human-trigger only** | Per `release-deploy-standard.md` §9 — agent does NOT auto-trigger production deploy |
| Concurrent op check before deploy trigger | User + agent | Per `concurrent-production-mutation-ops.md` §4 — confirm no terraform-apply.yml in flight |
| Follow-up gap: Testcontainers Postgres for repository tests | Future wave | Meta-gap: H2 doesn't catch Postgres-specific binding |
| Follow-up gap: review all `@Transactional` audit/log services for REQUIRES_NEW propagation | Future wave | Defense audit — Bug 2 pattern likely repeats |
| Credential rotation: `solo-dev-admin` IAM access key shared in chat | User | Rotate within 24h per session start warning |

## Recommendations

1. **Apply Patch A + B in single PR** to branch `claude/start-session-TSMPw`. Both small + co-located. Compile + run unit tests locally before push.
2. **Human-triggered production deploy** per `release-deploy-standard.md` §9. Agent only prepares + opens PR.
3. **Post-deploy verification commands:** retry login on admin@kitehub.me (user runs with real credential — NOT via tool); check CloudWatch for 0 "UnexpectedRollbackException" trong 30-min window post-deploy.
4. **Rotate the IAM key** for `solo-dev-admin` immediately after session (long-lived static creds pasted in chat per user choice).
5. **File 2 follow-up gaps** for meta-issues: (a) Testcontainers Postgres in CI, (b) audit propagation review across all log services.

## References

- Code: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/audit/login/LoginAuditService.java`
- Code: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/audit/login/LoginAuditLog.java`
- Code: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/AuthService.java` line 423-425 (audit call site)
- Migration: V38 (create), V42 (fingerprint fix precedent), V52 (this fix)
- Rules applied: `pre-mutation-state-check.md` §3, `agent-aws-access.md` §2.1/§5, `concurrent-production-mutation-ops.md` §4, `release-deploy-standard.md` §9
- Originating user message: 2026-05-16 — production login 500 incident report

## Override trailers anticipated

This hotfix may need:
- `PRE_MUTATION_OVERRIDE` not required — audit artifact present.
- `RELEASE_DEPLOY_OVERRIDE` not required — fix is in scope of normal hotfix per `release-deploy-standard.md` §3.2 PATCH bump.
