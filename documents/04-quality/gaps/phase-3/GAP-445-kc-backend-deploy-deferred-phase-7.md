# GAP-445: KC backend deploy deferred to Phase 7 (3-fix pivot per release-fix-retry-budget)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (not blocking Phase 1 BETA — KC frontend on Vercel)
**Domain:** DevOps / Backend
**Found:** 2026-05-08 (Phase 1 BETA deploy session)
**Affects:** kiteclass-core + kiteclass-gateway production deploy on kc-app EC2 (i-04f65503ace7febe4)

## Problem

During Phase 1 BETA deploy (Wave 41 closure session 2026-05-08), attempt to bring up KC backend stack on kc-app encountered cascading failures across 3 fault domains:

| Fix # | Fault | Action taken | Result |
|---|---|---|---|
| 1 | `INTERNAL_API_SECRET` placeholder unresolved → gateway crash | Added `INTERNAL_API_SECRET` to `/etc/kite/.env` on both EC2s; created `kitehub/production/internal-api-secret` in Secrets Manager | gateway started but core failed |
| 2 | Flyway checksum mismatch — RDS has stale migrations from prior deploy attempts (v1/v2/v3 checksums diverge from local image) | Added `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false` + `SPRING_FLYWAY_REPAIR_ON_MIGRATE=true` to `/etc/kite/.env` | core still restart loop (3rd unknown fault) |
| 3 | Unknown — core in restart loop, root cause not investigated | STOP per `release-fix-retry-budget.md` §3 | KC stack stopped, deferred |

Per user's own rule (codified in `.claude/rules/release-fix-retry-budget.md` from this same session): **"đối với những lỗi fix retry 2 lần trở lên của release thì ngay lập tức phải nghĩ đến phương án loại bỏ"** → pivot to removal at retry #2+ rather than continue iterating.

## Architectural context

KC backend was **never in Phase 1 BETA scope** per ADR-025 + `docker-compose.production.yml:7`:

> "KC stack (kiteclass-core + gateway + frontend) deferred to Phase 7 polish wave per GAP-444. Vercel still serves KC frontend at kiteclass.vercel.app."

KC backend was added mid-session ad-hoc when user asked to verify KC parallel with KH restart. The 3-retry saga confirms architectural decision was correct: KC backend deploy needs a dedicated wave, not a piggyback.

## Current State (verified 2026-05-08)

- **KH backend**: ✅ LIVE on kh-backend EC2 (i-0b65c3947d36cae61), ALB HTTP 200, RDS UP
- **KC frontend**: ✅ Vercel kiteclass.vercel.app (pre-existing, not touched)
- **KC backend stack**: ⏸️ stopped via `docker compose down` on kc-app
- **kc-app EC2**: still provisioned (m7i-flex.large $59/mo) — repo cloned at `/opt/kite-prod`, `/etc/kite/.env` populated, ready for re-deploy when investigated
- **Secrets Manager**: `kitehub/production/internal-api-secret` created — usable by KC + KH both
- **RDS Postgres**: kiteclass schema has stale migrations (v1/v2/v3 checksums mismatch); needs Flyway repair OR schema drop+reinit on next attempt

## Root Cause (partial)

3 issues encountered, only fix #1 root-caused:
1. **fetch-secrets.sh** does not fetch `INTERNAL_API_SECRET` (KH-flavored secrets only)
2. **RDS kiteclass schema** has stale Flyway state — likely from prior staging or bootstrap attempts
3. **Unknown #3** — needs investigation (DB connection? entity scan? bean wiring?)

## Proposed Fix (Phase 7)

Dedicated wave for KC backend deploy:

1. **Phase 7.A — Investigate fix #3 root cause**
   - Pull `kiteclass-core` v0.9.0-beta-staging.8 image locally
   - Run with prod-like env (RDS endpoint via SSM tunnel) → reproduce restart
   - Capture full startup log, identify what fails after Flyway skipped

2. **Phase 7.B — Update fetch-secrets.sh + populate-secrets.sh**
   - Add `INTERNAL_API_SECRET` fetch from `kitehub/production/internal-api-secret`
   - Update populate-secrets.sh to ensure all required secrets exist before deploy

3. **Phase 7.C — Schema strategy**
   - DECIDE: drop+reinit kiteclass schema (Phase 1 BETA = no real data) OR Flyway repair script
   - Document outcome in `documents/05-guides/kc-deploy-runbook.md`

4. **Phase 7.D — Re-deploy on kc-app**
   - Bootstrap already done (`/opt/kite-prod` cloned)
   - Run `bash /opt/kite-prod/scripts/deploy-kc.sh` per existing pattern

5. **Phase 7.E — ALB listener rule for KC**
   - Currently no listener rule for kc_app target group (port 3000 on kc-app)
   - Add rule: hostname-based or path-based routing
   - OR: keep KC frontend on Vercel, KC backend internal-only (gateway↔core via private IP)

## Acceptance Criteria

- [ ] Fix #3 root cause documented in this gap's Log section
- [ ] `fetch-secrets.sh` includes `INTERNAL_API_SECRET` fetch
- [ ] Schema reset/repair strategy decided + executed
- [ ] `kiteclass-gateway` healthcheck returns 200 on kc-app:3000
- [ ] `kiteclass-core` healthcheck returns 200 on kc-app:8081 (internal)
- [ ] If KC backend public-facing: ALB listener rule routes traffic to kc_app target group
- [ ] Smoke test: at least 1 KC API endpoint returns 200 via ALB

## Cost note

kc-app EC2 m7i-flex.large = ~$59/mo. While KC backend deferred, consider:
- **Option A**: stop EC2 instance (no cost) → restart for Phase 7
- **Option B**: keep running for staging KC builds / experiments (~$59/mo)
- **Option C**: downsize to t3.micro free-tier until Phase 7 (~$0/mo)

Decision tracked in this gap's Log; recommend Option A for cost discipline (stop until Phase 7 spawns).

## Related

- `release-fix-retry-budget.md` (rule that triggered the pivot, created same session)
- `docker-compose.production.yml:7` (architectural decision: KC backend deferred Phase 7)
- ADR-025 (Architecture B 2-instance topology)
- GAP-444 (Phase 7 polish parent gap)

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-3 (title 'Phase 7'; Phase 7 mapped to phase-3 (no later subdir)).
- **2026-05-08**: GAP filed after pivot decision per `release-fix-retry-budget.md` §3 STOP at retry #2+. KC stack stopped via `docker compose down`. KH backend confirmed LIVE (ALB HTTP 200, DB UP). 3 fixes attempted: INTERNAL_API_SECRET (added), Flyway validate=false (added), unknown #3 (deferred). Architectural alignment: KC backend was always Phase 7 scope per ADR-025; mid-session add-on was scope drift.
