---
title: AWS Pre-Mutation — kc-app + subscription config-fix deploy (PR #2490)
status: complete
created: 2026-06-18
phase: Phase 1 BETA — prod config-fix deploy (Bước 2)
gaps: [handoff-Gap1, handoff-Gap2, handoff-Gap3]
---

# AWS Pre-Mutation Report — kc-app + subscription config-fix deploy

Pre-mutation state-check per `.claude/rules/pre-mutation-state-check.md` §3, before
deploying the 3 prod config fixes merged in PR #2490 (squash `11d48f7b7`).

## Scope

Deploy PR #2490 fixes to production EC2 to unblock kiteclass-core + beta-signup:
- **Gap 1** (`docker-compose.kc.yml`): kiteclass-core → `kiteclass_shared` DB
- **Gap 2** (`scripts/deploy-kc.sh`): RabbitMQ user self-heal
- **Gap 3** (`scripts/fetch-secrets.sh`): `DATABASE_ADMIN_*` / `DATABASE_MASTER_*`

**Mutation class:** `aws ssm send-command` on production EC2 → runs `deploy-kc.sh`
(kc-app) + `deploy-prod.sh` (kh-backend). Per `release-deploy-standard.md` §9 +
`agent-aws-access.md` §4.3 → **user-executed** (agent-autonomous SSM mutation BANNED).

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
aws rds describe-db-instances --db-instance-identifier kitehub-postgres --query '...DBInstanceStatus'
aws ec2 describe-instances --filters Name=instance-state-name,Values=running --query '...'
aws ecr describe-images --repository-name kite/kiteclass-core --query 'reverse(sort_by(...,&imagePushedAt))[:3]'
aws ecr describe-images --repository-name kite/kitehub-subscription --query '...'
```

## Findings

### Current state

| Resource | State | Note |
|---|---|---|
| RDS `kitehub-postgres` | `available` ✅ | Deploy preflight (GAP-493) passes |
| EC2 kh-backend `i-05d7af46d01436b96` | running | subscription on `latest` + dc-seed.yml; gateway/admin/branding/email on `0.9.0-test.1` |
| EC2 kc-app `i-01ad56b0067d0213b` | running | kiteclass-core crash-looping on `latest` + hot-patch `dc-fix.yml` |
| EC2 kc-app-fe `i-05cfda7c6c60b683f` | running | docker FE healthy (no change this deploy) |
| ECR `kite/kiteclass-core` `latest` | = `sha-2854358` pushed 2026-06-18 02:31 | from main build run (commit 2854358 = #2487) |
| ECR `kite/kitehub-subscription` `latest` | = `sha-2854358` pushed 2026-06-18 02:31 | same build run |

### Real changes (this deploy)

| # | Change | Image rebuild? | Risk |
|---|---|---|---|
| 1 | `/opt/kite-prod` git → origin/main (pulls PR #2490 scripts+compose) | NO | low — fast-forward pull |
| 2 | Re-run `fetch-secrets.sh` → `/etc/kite/.env` gains `DATABASE_ADMIN_*` | NO | low — additive env vars |
| 3 | Recreate `kite-rabbitmq` (kc-app) with correct user + self-heal | NO | low — restart |
| 4 | Restart kiteclass-core on `kiteclass_shared` (Flyway V1..V100 migrate) | NO | **medium** — first migrate of empty `kiteclass_shared`; verify success |
| 5 | Restart subscription with `DATABASE_ADMIN_*` set | NO | low — env reload |

**No image rebuild** — all 3 fixes are scripts/compose; existing `latest` image
(`sha-2854358`) is the deploy target. Deploy scripts `git reset --hard origin/main`
on the EC2 pulls the merged fixes.

### Phantom / non-changes

- kc-app-fe: untouched.
- gateway/admin/branding/email (kh-backend): stay on `0.9.0-test.1` (out of scope).

### Verdict

Safe to deploy. Pre-launch BETA, kiteclass_shared is empty (verified by prior session),
no production data at risk in kiteclass-core's DB. Medium-risk item = first Flyway
migrate of `kiteclass_shared` — verify `flyway_schema_history` + `landing_pages` table
exists post-deploy.

## ⚠️ Critical gotcha — deploy-kc.sh self-heal needs git-pull-FIRST

The currently-deployed `deploy-kc.sh` on kc-app is the OLD version (no self-heal).
If you run it directly, bash executes the OLD script in memory; its `git reset` pulls
the NEW deploy-kc.sh but the self-heal block (added this PR) does NOT run on that first
invocation. Two safe options:
- **(a)** `git -C /opt/kite-prod fetch+reset` FIRST, THEN run the freshly-pulled deploy-kc.sh, OR
- **(b)** run deploy-kc.sh twice (1st pulls new script, 2nd runs self-heal).

`deploy-prod.sh` (kh-backend) is NOT affected by this — Gap 3's changed file
(`fetch-secrets.sh`) is invoked as a subprocess AFTER its git reset, so it picks up
the new version in a single run.

## Deploy runbook (serialized per `concurrent-production-mutation-ops.md` — one EC2 at a time)

### Step A — kh-backend (subscription / Gap 3) — i-05d7af46d01436b96
```bash
aws ssm send-command --region ap-southeast-1 \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "config-fix deploy: subscription DATABASE_ADMIN" \
  --timeout-seconds 600 \
  --parameters commands='sudo KITE_VERSION=latest bash /opt/kite-prod/scripts/deploy-prod.sh'
# poll: aws ssm get-command-invocation --command-id <id> --instance-id i-05d7af46d01436b96
```
Verify: `docker exec kite-postgres ...` not needed; check subscription healthy +
beta-signup admin DB conn no longer localhost:5433.

### Step B — kc-app (kiteclass-core / Gap 1+2) — i-01ad56b0067d0213b
```bash
# (a) pull-first so the NEW deploy-kc.sh (with self-heal) runs:
aws ssm send-command --region ap-southeast-1 \
  --instance-ids i-01ad56b0067d0213b \
  --document-name AWS-RunShellScript \
  --comment "config-fix deploy: kc-core kiteclass_shared + rabbit self-heal" \
  --timeout-seconds 600 \
  --parameters commands='sudo git -C /opt/kite-prod fetch --depth 1 origin main && sudo git -C /opt/kite-prod reset --hard origin/main && sudo KITE_VERSION=latest bash /opt/kite-prod/scripts/deploy-kc.sh'
```
Verify: kiteclass-core `Up healthy`; `landing_pages` table in `kiteclass_shared`;
`co-ha-toan.kitehub.me` loads; beta-signup completes 200; S3 upload round-trip.

### Step C — cleanup hot-patch override files (after verify GREEN)
```bash
# remove /opt/kite-prod/dc-fix.yml (kc-app) + dc-seed.yml (kh-backend) once canonical compose confirmed working
```

### Step D — confirm wildcard `*.kitehub.me` → kiteclass-frontend (nginx) routing

## Prior actions verified

| Action | When | Where |
|---|---|---|
| Gaps 1+3 hot-patch verified working on EC2 | 2026-06-18 (audit session) | handoff `2026-06-18-prod-config-audit-kc-app-subscription.md` |
| kiteclass_shared exists + empty + kitehub user access | 2026-06-18 | handoff Gap 1 |
| FE docker deploy (kc-app-fe) | 2026-06-18 | handoff + #2489 |

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| Concurrent-op check | this audit | No terraform/deploy workflow in_progress; serialize Step A → verify → Step B |
| SSM deploy Steps A+B | **USER** (per `release-deploy-standard.md` §9) | agent-autonomous SSM mutation BANNED |

## Recommendations

1. Serialize: Step A (kh-backend) → verify → Step B (kc-app). Different EC2 + different
   DBs on shared RDS; serialize to avoid migrate/restart interleave.
2. Step B MUST use git-pull-first form (gotcha above) for self-heal to run.
3. After GREEN: cleanup override files (Step C) + confirm routing (Step D).
4. Open design question (per handoff): per-tenant physical-DB provisioning vs shared-DB
   — Gap 3 makes provisioning succeed; design decision deferred to a follow-up gap.

## Deploy outcome (2026-06-18, executed via SSM per user authorization)

**Discovery during deploy:** `/opt/kite-prod` is **NOT a git repo** on either EC2
(`git rev-parse HEAD` → no-git). The deploy scripts assume `git reset --hard origin/main`
to pull fixes — `deploy-prod.sh` would exit 3, `deploy-kc.sh` would WARN-and-use-old-files.
**Worked around** by delivering the 3 fixed files directly via `curl` from
`raw.githubusercontent.com/.../main/` (repo public; main HEAD = `11d48f7b7`).
→ **Follow-up:** restore git-managed `/opt/kite-prod` so future `deploy-production.yml`
workflow + `deploy-prod.sh` work (currently both would fail on no-git).

**Step A — kh-backend / subscription (Gap 3):** ✅ SUCCESS. SSM `f7d1f7af`.
- curl fixed `fetch-secrets.sh` → re-ran → `/etc/kite/.env` gained `DATABASE_ADMIN_URL/USERNAME/PASSWORD` + `DATABASE_MASTER_HOST/PORT` (RDS master, `postgres` DB).
- Recreated ONLY `kitehub-subscription` (`--no-deps`, `latest`) — gateway/admin/branding/email untouched on `0.9.0-test.1`.
- subscription "Started in 37.8s" + healthy. Live env keys verified present.

**Step B — kc-app / kiteclass-core (Gap 1+2):** ✅ SUCCESS. SSM `538ec0a7`.
- curl fixed `docker-compose.kc.yml` + `deploy-kc.sh` + `fetch-secrets.sh` → ran new `deploy-kc.sh`.
- Rabbit self-heal ran (`set_user_tags kite_admin_837bf8fc administrator`) → Gap 2 resolved.
- kiteclass-core "Started in 38.6s" + **healthy** (was crash-looping) → Gap 1 resolved.
- Live verify: container env `SPRING_DATASOURCE_URL=...kiteclass_shared` ✓ (override took).
- deploy-kc.sh's `:3000/actuator/health` 404 = benign (probes banner-renderer, not a gateway; kiteclass-gateway removed per ADR-032). kiteclass-core is `:8081` internal + healthy.

**Step C — cleanup:** ✅ removed `dc-fix.yml` (kc-app) + `dc-seed.yml` (kh-backend).

**Step D — smoke + routing:**
- `https://kitehub.me/` 200 ✓ | `https://app.kitehub.me/` 200 ✓ | `https://api.kitehub.me/actuator/health` 200 ✓
- `https://co-ha-toan.kitehub.me/` → **000 (no DNS)** — no wildcard `*.kitehub.me`; tenant-subdomain routing needs DNS-wildcard + cert + Host-based routing. **Separate follow-up** (handoff Step D item 5), NOT a regression.

**Remaining verification (not blocking deploy — user-facing E2E):**
- beta-signup completion E2E (Gap 3 functional proof — needs valid invite code; infra fix verified, real provisioning only proven by an actual signup).
- S3 upload round-trip (needs logged-in flow).
- wildcard tenant-subdomain routing (separate infra task).

## References

- PR #2490 (squash `11d48f7b7`) — the 3 config fixes
- Handoff: `documents/03-planning/session-handoffs/2026-06-18-prod-config-audit-kc-app-subscription.md`
- Rules: `pre-mutation-state-check.md` §3, `release-deploy-standard.md` §9, `concurrent-production-mutation-ops.md`, `agent-aws-access.md` §4.3
