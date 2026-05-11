---
title: Production Seed Runbook — first cutover + recovery
status: active
created: 2026-05-11
owner: Solo-dev (acting Ops)
runbook_type: deploy
related_gaps: [GAP-376, GAP-377, GAP-378, GAP-379, GAP-449]
related_waves: [33, 45, 61]
---

# Production Seed Runbook

**Purpose.** Seed an empty production database with the minimum data required cho first cutover: 1 super-admin, system config baseline, platform tenant id=0. Sample tenant tier rows (FREE / PRO / PREMIUM) được ship qua separate optional fixture script — KHÔNG mandatory cho cutover.

**Owner.** Solo-dev acting Ops (`@nguyenvankiet`). Per `release-deploy-standard.md` §9 + `agent-aws-access.md` Tier 1, AWS mutation steps (stack resume, secrets fetch) **user-executed**. Agent role = generate runbook + smoke automation, không trigger apply.

**Cycle target.** 1 full start → seed → smoke → stop cycle ≤ **25 phút** (per Wave 61 plan §3 Bucket C acceptance).

---

## 1. When to run

| Trigger | Frequency | Notes |
|---|---|---|
| First production cutover | Once per environment | Hard prerequisite — `kitehub-frontend` requires platform tenant + system_config |
| Recovery sau data loss | Per incident | Run với RDS restored to PITR snapshot; idempotent INSERT-ON-CONFLICT skips existing rows |
| Sandbox → production transition | Once per env promotion | Same script, env vars target the new DB |
| Stop-when-idle resume (no seed needed) | Each demo cycle | Seed already in place; use `seed-production.sh --dry-run` for sanity verify only |

**Do NOT run when:** DB has user data already AND the script body is uncertain (e.g., a future fork adds non-idempotent SQL). Always `--dry-run` first.

---

## 2. Prerequisites

Before invoking seed, the following MUST be true:

| # | Prerequisite | Verify command | Wave / gap |
|---|---|---|---|
| 1 | DNS shipped (kitehub.me apex + api subdomain resolve) | `dig +short api.kitehub.me` returns IP | Wave 61 Bucket A (GAP-369) |
| 2 | SES production approval landed (out of sandbox) | AWS Console → SES → Account Dashboard | Wave 61 Bucket B (GAP-370) |
| 3 | RDS instance resumed + reachable | `aws rds describe-db-instances --query 'DBInstances[0].DBInstanceStatus'` returns `available` (user-executed) | Wave 61 Bucket D (forward-ref) |
| 4 | Secrets populated (DB creds + SEED_ADMIN_PASSWORD) | `aws secretsmanager list-secrets` shows `kite/prod/*` entries | GAP-379 |
| 5 | Flyway migrations applied (V1 → V27) | `psql ... -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"` shows V27 | Wave 33 |
| 6 | Subscription jar built | `ls kitehub/kitehub-subscription/target/kitehub-subscription-*.jar` | CI |

Missing any prerequisite → STOP, fix prerequisite first. Seed assumes a healthy stack.

---

## 3. Standard procedure (first cutover)

### 3.1 Resume stack (USER-EXECUTED)

Per `agent-aws-access.md` §4 Tier 3 — mutation actions require explicit user execution. Agent provides the command, user runs:

```bash
# Forward-reference: scripts/aws/start-stack.sh ships in Wave 61 Bucket D.
# Until shipped, resume manually via AWS Console OR aws CLI:
#   aws ec2 start-instances --instance-ids <id>
#   aws rds start-db-instance --db-instance-identifier <id>
bash scripts/aws/start-stack.sh
```

Wait for healthy state (~5-8 phút cold start).

### 3.2 Populate secrets into shell env

```bash
export DATABASE_URL=$(aws secretsmanager get-secret-value \
    --secret-id kite/prod/database-url --query SecretString --output text)
export DATABASE_USERNAME=$(aws secretsmanager get-secret-value \
    --secret-id kite/prod/database-username --query SecretString --output text)
export DATABASE_PASSWORD=$(aws secretsmanager get-secret-value \
    --secret-id kite/prod/database-password --query SecretString --output text)
export SEED_ADMIN_PASSWORD=$(aws secretsmanager get-secret-value \
    --secret-id kite/prod/seed-admin-password --query SecretString --output text)
export SEED_ADMIN_EMAIL=admin@kitehub.me   # default — override only if pivot away
```

> **Security note.** Per `agent-aws-access.md` §2.2, `get-secret-value` is Tier 2 (always-confirm). Run each command interactively, do NOT batch. Clear shell history after cutover: `history -c`.

### 3.3 Dry-run validation

```bash
bash scripts/seed-production.sh --dry-run
```

Expected exit code: `0` (env validation PASS + DB connectivity PASS). If `1` → env vars missing, re-check §3.2. If `2` → DB unreachable, re-check §2 row 3 (RDS status).

### 3.4 Real seed run

```bash
bash scripts/seed-production.sh
```

Expected output (last lines):

```
[seed-production] launching seed runner — jar=kitehub/kitehub-subscription/target/kitehub-subscription-X.Y.Z.jar
... Spring Boot startup ...
ProductionSeedRunner: seeded admin@kitehub.me (PLATFORM_ADMIN)
ProductionSeedRunner: system_config baseline applied
ProductionSeedRunner: platform tenant id=0 OK
ProductionSeedRunner: seed complete in 12.3s
```

Re-run safety: per Wave 33 Bucket A, seed runner uses `INSERT ... ON CONFLICT DO NOTHING` semantics. Idempotent — re-run on existing data prints `SKIP (already present)` for each row.

### 3.5 DB verify

```bash
psql "${DATABASE_URL}" -c "
SELECT 'admin' AS check, COUNT(*) FROM users WHERE role = 'PLATFORM_ADMIN'
UNION ALL SELECT 'system_config', COUNT(*) FROM system_config
UNION ALL SELECT 'platform_tenant', COUNT(*) FROM tenants WHERE id = 0;
"
```

Expected: each row count ≥ 1.

### 3.6 Smoke verify (13 endpoints + cycle audit)

```bash
STOP_WHEN_IDLE_E2E=1 \
STOP_WHEN_IDLE_STATE_FILE=documents/05-guides/deploy/state/stop-when-idle-cycles.jsonl \
bash scripts/smoke-test.sh https://api.kitehub.me https://kiteclass.me
```

Exit code:
- `0` — all 13 endpoint checks + seed dry-run + cycle envelope PASS → safe to stop stack
- `1` — at least one FAIL → investigate before stopping; data state may be inconsistent
- `2` — WARN-only (e.g. seed env vars cleared post-run) → acceptable for stop

### 3.7 Stop stack (USER-EXECUTED)

```bash
bash scripts/aws/stop-stack.sh   # Forward-ref: Wave 61 Bucket D
```

Verify RDS status returns `stopped` within ~3 phút.

**Record cycle:** `state.json` line emitted by §3.6 logs PASS/FAIL/WARN counts + wall-clock. Review weekly per `agent-aws-access.md` §5 (verification artifact requirement).

---

## 4. Recovery procedure (post-data-loss)

Scenarios: RDS storage corruption, accidental DROP TABLE, restored-from-snapshot mismatch.

1. Restore RDS to PITR snapshot (USER-EXECUTED, AWS Console)
2. Verify Flyway migrations applied (§2 row 5)
3. Re-run §3.3 + §3.4 (seed is idempotent — only writes missing rows)
4. Cross-check against backup of `system_config` if known custom values were lost

> **Do NOT** truncate tables before running seed unless explicitly intended. Seed adds baseline only; user data (tenants, courses) NOT restored by this script — use RDS snapshot restore for that.

---

## 5. Rollback procedure (wrong tier / wrong env seeded)

If seed was run against the wrong environment (e.g., dev creds resolved to prod):

```bash
psql "${DATABASE_URL}" <<'SQL'
BEGIN;
-- Inspect first; verify rows match seed signature before delete
SELECT * FROM users WHERE email = 'admin@kitehub.me' AND created_by = 'ProductionSeedRunner';
SELECT * FROM system_config WHERE created_by = 'ProductionSeedRunner';
SELECT * FROM tenants WHERE id = 0 AND name = 'KiteHub Platform';
-- Delete only if above matches expected seed footprint
DELETE FROM users WHERE email = 'admin@kitehub.me' AND created_by = 'ProductionSeedRunner';
DELETE FROM system_config WHERE created_by = 'ProductionSeedRunner';
-- Do NOT delete tenant id=0 if any user/course references it
ROLLBACK;  -- change to COMMIT only after verifying SELECT output
SQL
```

> **Warning.** If real users registered between seed and rollback, manual reconciliation needed. Per `gap-done-discipline.md` §2, do NOT proceed to DONE flip without reviewing the SELECT output line by line.

---

## 6. Acceptance criteria (per Wave 61 Bucket C)

- [x] `scripts/seed-production.sh` exists (shipped Wave 33 Bucket A — PR #895)
- [x] Idempotent re-run safe (`INSERT ... ON CONFLICT DO NOTHING` semantics via V27)
- [x] Smoke scenario `STOP_WHEN_IDLE_E2E=1` extends `scripts/smoke-test.sh` with seed-dry-run + cycle envelope + optional state log (this PR)
- [x] Runbook covers full cycle (§1-§5)
- [x] Cross-link forward-references `scripts/aws/start-stack.sh` + `stop-stack.sh` (Wave 61 Bucket D)
- [ ] **User-action step:** real production seed execution post-cutover (cannot be agent-completed per `agent-aws-access.md` Tier 3)

The user-action gate ↑ is why GAP-376 stays **🟡 PARTIAL** — agent-shippable artifacts complete; execution = first cutover step.

---

## 7. Related artifacts

| Artifact | Path | Role |
|---|---|---|
| Seed wrapper | `scripts/seed-production.sh` | Pre-flight env + DB check, launches Spring Boot runner |
| Seed runner | `kitehub/kitehub-subscription/src/main/java/com/kite/hub/subscription/seed/ProductionSeedRunner.java` | Idempotent seed logic (Wave 33) |
| Migration V27 | `kitehub/kitehub-subscription/src/main/resources/db/migration/V27__seed_baseline.sql` | Idempotent baseline DDL+DML |
| Sample fixtures | `scripts/seed-staging-fixtures.sh` | Optional 3-tier demo tenants (NOT for production) |
| Smoke | `scripts/smoke-test.sh` | 13 endpoint health + `STOP_WHEN_IDLE_E2E=1` cycle audit |
| Stack resume / stop | `scripts/aws/start-stack.sh` / `stop-stack.sh` | Wave 61 Bucket D (forward-ref) |
| Stack-on-demand runbook | `documents/05-guides/operations/stack-on-demand-runbook.md` | Wave 61 Bucket D (forward-ref) |
| Tier 3 cutover plan | `documents/05-guides/deploy/release-1-tier-3-cutover.md` | Parent cutover narrative |

---

## 8. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Skip `--dry-run` "vì biết env đúng" | Always dry-run first; cost is 2s, blast radius of mistake is hours |
| Hardcode `SEED_ADMIN_PASSWORD` trong shell history | Always pipe from `secretsmanager get-secret-value`; clear history post-cutover |
| Re-run seed when uncertain about tier | Read seed runner logs first — idempotent, but verify intent |
| Use `seed-staging-fixtures.sh` against production | Production seed = baseline only; fixtures = staging/demo only |
| Stop stack before smoke verify | Smoke catches mis-seed early; stop AFTER smoke exit 0 |
| Run agent-initiated `aws ec2 start-instances` | Per `agent-aws-access.md` Tier 3 — USER-EXECUTED only |

---

## 9. Log

- **2026-05-11 (v1.0):** Runbook created — Wave 61 Bucket C. Closes runbook portion of GAP-376 acceptance criteria (production deploy runbook updated with seed step). Seed scripts + runner inherited from Wave 33 Bucket A (PR #895). Forward-references `scripts/aws/start-stack.sh`/`stop-stack.sh` + `stack-on-demand-runbook.md` from Wave 61 Bucket D (parallel ship). GAP-376 stays 🟡 PARTIAL per `gap-done-discipline.md` §3 — user-action execution step pending first cutover.
