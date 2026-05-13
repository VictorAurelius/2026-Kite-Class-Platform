# GAP-506: deploy-prod.sh tech debt cluster — chicken-and-egg + ephemeral cred pollution + start_period + email healthcheck path

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Phase 1 BETA — observed Wave 70 GAP-502 live ops; non-blocking but accumulates debt)
**Domain:** DevOps / Backend
**Found:** 2026-05-13 (Wave 70 GAP-502 RC1 live verification + retro)
**Affects:** Every production deploy via `deploy-production.yml` workflow

## Problem

Wave 70 GAP-504/505 fix shipped Step 6.5 self-heal in `scripts/deploy-prod.sh` + per-service healthcheck override. Post-merge live verify surfaced 4 residual issues:

### 1. Bash chicken-and-egg: script updates take 2 deploys to take effect

`deploy-prod.sh` does `git pull` mid-execution. Bash loaded file content into memory BEFORE `git pull` runs. New content on disk doesn't override in-memory script → fixes shipped this deploy land in `/opt/kite-prod/scripts/deploy-prod.sh` BUT execute starting NEXT deploy. Observed in Wave 70: PR #1263 merged 09:30Z, deploy 09:31Z git-pulled new code but ran OLD code; deploy 09:50Z finally executed Step 6.5.

### 2. Rabbit user pollution per deploy

`fetch-secrets.sh:67-77` fallback generates ephemeral `kite_admin_$(openssl rand -hex 4)` because `kitehub/production/rabbitmq-default-creds` secret stored null/empty (LastChanged 2026-05-07 but no value). Each deploy generates fresh user. Wave 70 session observed 5 generations: `347c4bb0` → `1bc21f54` → `62a85987` → `af335cf8` (plus another from earlier deploys). rabbit user list grows unbounded. Step 6.5 self-heal adds each new user, never removes old.

### 3. `start_period: 150s` borderline tight

GAP-505 set `start_period: 150s` for 4 BE service healthchecks based on observed Spring init time:
- subscription: 117-128s
- branding: 130-132s ← only 18s buffer
- email: 67-70s
- admin: 128-130s
- gateway: 111-136s

One slow-init event (cold ECR pull, lock contention) → ≥150s → healthcheck fires → false unhealthy → restart triggered → loop.

### 4. kitehub-email "unhealthy" post-fix despite Spring init clean

Wave 70 final probe (2026-05-13 09:55Z): 4/5 services healthy, ONLY `kitehub-email` unhealthy. Auth errors 0, no die events, Spring `Started`. Healthcheck `curl localhost:8084/actuator/health` may not respond on email service in production profile — possibly different management.port, actuator not exposed, or server.port override.

## Root Cause

| # | Root cause |
|---|---|
| 1 | bash reads script into memory eagerly; git pull during execution doesn't reload |
| 2 | `kitehub/production/rabbitmq-default-creds` secret empty → fetch-secrets.sh fallback path always taken; populate-secrets.sh never run |
| 3 | start_period chosen on observed max + minimal buffer; no safety margin |
| 4 | Email service likely has different actuator port OR actuator disabled in production profile |

## Proposed Fix

### Phase 1 — Stop the bleed (immediate)

- **Sub-A: One-shot populate stable secret**
  ```bash
  # From local with AWS access (or one-time SSM):
  bash scripts/populate-secrets.sh --yes
  ```
  Writes `kitehub/production/rabbitmq-default-creds` with stable JSON. Eliminates ephemeral fallback. Eliminates user pollution problem at source.

- **Sub-B: Investigate email healthcheck port**
  ```bash
  docker exec kitehub-email curl -sf http://localhost:8084/actuator/health
  docker exec kitehub-email curl -sf http://localhost:8084/actuator
  docker exec kitehub-email env | grep -E "MANAGEMENT|SERVER_PORT|actuator"
  ```
  If endpoint different → fix `docker-compose.production.yml` kitehub-email healthcheck OR fix application-prod.yml exposure.

### Phase 2 — Script discipline

- **Sub-C: bash chicken-and-egg fix**
  Wrap deploy-prod.sh entry to read self into memory first:
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail
  # If we git-pulled since script started, re-exec with fresh content
  if [[ "${KITE_DEPLOY_REEXEC:-0}" != "1" ]]; then
    # Initial pass: do the git-update steps, then re-exec
    # ... (move git pull to bootstrap section)
    export KITE_DEPLOY_REEXEC=1
    exec bash "$0" "$@"
  fi
  # ... rest of script (now guaranteed to be post-git-pull content)
  ```
  Or simpler: move git pull to a SEPARATE bootstrap script that deploy-production.yml invokes first.

- **Sub-D: Bump start_period 150s → 180s** in `docker-compose.production.yml` 4 BE services.

### Phase 3 — Codify production ops scripts (per CLAUDE.md "Docker Scripts Required" rule)

- `scripts/prod-status.sh` — wraps `docker ps`, log grep, sample API for ad-hoc state probes
- `scripts/prod-rabbit-sync.sh` — extract Step 6.5 logic to standalone script (callable by deploy-prod.sh OR ad-hoc)
- `scripts/prod-restart.sh` — wraps `docker compose restart kitehub-*` with health check wait

Eliminates raw `docker` commands via SSM (rule violation accumulated during Wave 70 live ops).

## Acceptance Criteria

- [ ] **Phase 1 Sub-A:** `kitehub/production/rabbitmq-default-creds` has stable JSON (not null/empty); subsequent deploys don't generate new ephemeral users
- [ ] **Phase 1 Sub-B:** kitehub-email healthcheck endpoint identified + compose updated → service shows `(healthy)` ≥10 min post-deploy
- [ ] **Phase 2 Sub-C:** Script update takes effect SAME deploy (verifiable: edit deploy-prod.sh + push + trigger deploy → log message from new code appears in deploy log)
- [ ] **Phase 2 Sub-D:** start_period bumped 150s → 180s in 4 BE services
- [ ] **Phase 3:** 3 new prod-* scripts exist + deploy-prod.sh refactored to call them; no raw docker via SSM in future runbooks

## Out-of-scope

- Rabbit user cleanup for pollution accumulated this session (4-5 stale users) → quarterly retro / manual cleanup
- Dockerfile HEALTHCHECK port parameterization → next image refresh wave
- Replacing rabbit user provisioning with terraform rabbitmq provider → Phase 2 architecture decision

## Related

- Parent: GAP-502 (Wave 70 root cause this debt cluster surfaced from)
- Sibling: GAP-504 (rabbit user self-heal — partial fix this gap completes)
- Sibling: GAP-505 (healthcheck port override — start_period bump extends)
- Rule violated: CLAUDE.md "CRITICAL: Docker Scripts Required" (ad-hoc raw `docker` via SSM during Wave 70 live ops)
- Memory: file `feedback_deploy_prod_chicken_egg.md` post-fix capturing the pattern for future scripts

## Log

- **2026-05-13:** Filed during Wave 70 GAP-502 live verification retro. 4 residual issues surfaced post GAP-504/505 fix:
  1. Bash chicken-and-egg (deploy #25789336481 missed Step 6.5; deploy #25791611463 ran it)
  2. 5 ephemeral rabbit users accumulated in single session (347c4bb0/1bc21f54/62a85987/af335cf8/+1)
  3. start_period 150s borderline (branding 132s observed; 18s margin)
  4. kitehub-email unhealthy post-fix (port 8084 healthcheck not responding; needs root-cause)
