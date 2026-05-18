# GAP-504: RabbitMQ user provisioning self-heal in deploy-prod.sh

**Status:** 🟢 DONE
**Priority:** 🟠 P1 (Phase 1 BETA — recurring deploy blocker for GAP-502)
**Domain:** DevOps / Backend
**Found:** 2026-05-13 (Wave 70 GAP-502 live ops)
**Affects:** Every `deploy-production.yml` run on kh_backend EC2 + rabbit connection auth across 5 kitehub-* services

## Problem

`scripts/fetch-secrets.sh` lines 67-77 generates ephemeral rabbit creds (`kite_admin_$(openssl rand -hex 4)`) when `kitehub/production/rabbitmq-default-creds` secret returns empty/null JSON. Even when secret is populated, **rabbit container itself doesn't know about the `kite_admin_*` user** — it only has the default `user` + `guest`. Spring Boot services hit `ACCESS_REFUSED` on every fresh deploy until manual `rabbitmqctl add_user` runs.

Wave 70 GAP-502 RC1 live ops session 2026-05-13 observed username rotation (`kite_admin_347c4bb0` → `kite_admin_1bc21f54`) within ~10 minutes between two SSM diagnostic runs — confirming ephemeral cred path firing per deploy.

## Root Cause

Two compounding gaps:
1. **Secret state empty:** `kitehub/production/rabbitmq-default-creds` exists in AWS Secrets Manager (LastChanged 2026-05-07) but stored value is null/empty → fetch-secrets.sh fallback fires `openssl rand` cred generation
2. **No rabbit user provisioning:** deploy-prod.sh writes .env with rabbit creds but never tells rabbit container to create the user → broker rejects auth

## Proposed Fix

**Step 6.5 in `scripts/deploy-prod.sh`** — after `docker compose up -d` and before final healthcheck, self-heal rabbit user:

1. Read `RABBITMQ_USER` + `RABBITMQ_PASS` from `/etc/kite/.env`
2. Wait up to 60s for rabbit broker reachable (`rabbitmqctl status`)
3. If user exists in `rabbitmqctl list_users` → `change_password` (handles cred rotation)
4. If user missing → `add_user` + `set_permissions -p / ".*" ".*" ".*"` + `set_user_tags administrator`
5. Restart 5 kitehub-* services to refresh AMQP connections

Idempotent: works across stable creds AND ephemeral rotation. Self-heal on every deploy.

## Acceptance Criteria

- [x] `scripts/deploy-prod.sh` Step 6.5 added (rabbit user sync block)
- [x] Logic: list_users grep → change_password OR add_user + permissions + admin tag
- [x] Idempotent across deploys (no error if user already exists with same creds)
- [x] Restart kitehub-* services post-sync to clear stale auth-fail Spring contexts
- [x] Wave 70 GAP-502 RC1 live verification post-merge: 0 auth errors across 5 services post-deploy

## Out-of-scope (follow-up)

- Populate `kitehub/production/rabbitmq-default-creds` with stable JSON via `populate-secrets.sh` — orthogonal; self-heal works regardless. File separately if needed.
- Provision rabbit user via terraform rabbitmq provider — cleaner architecturally but requires terraform-rabbitmq dependency on running broker (chicken-and-egg). Deferred.
- Migrate to RabbitMQ definitions.json bootstrap — requires building custom rabbit image. Deferred.

## Related

- Parent: GAP-502 RC1 (root cause this addresses)
- Sibling: GAP-505 (Dockerfile HEALTHCHECK port mismatch)
- Scripts: `scripts/fetch-secrets.sh:67-77` (ephemeral fallback), `scripts/deploy-prod.sh:118` (new Step 6.5)
- Rule applied: `release-deploy-standard.md` §4.2 (deploy steps), `agent-aws-access.md` §4 (SSM mutation discipline)

## Log

- **2026-05-13:** Filed during Wave 70 GAP-502 live ops. Self-heal block added to `scripts/deploy-prod.sh` Step 6.5 in same fix PR. Verified end-to-end post-merge via re-deploy → 0 auth errors across 5 services.
