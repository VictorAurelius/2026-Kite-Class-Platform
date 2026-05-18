---
title: Wave 71 — Pre-launch hardening (gateway routing + port + Spring profile + Resend + audit infra)
status: draft
created: 2026-05-13
updated: 2026-05-13
waves: [71]
gaps: [GAP-509, GAP-510, GAP-511, GAP-508]
---

# Wave 71 — Pre-launch hardening

**Goal:** Fix 4 architectural production bugs blocking Plan 1 self-test + ship 3 audit scripts preventing similar class recurrence.
**Trigger:** Plan 1 self-test 2026-05-13 surfaced CORS (Bug #1 fixed PR #1266) → comprehensive audit Layer 1-5 found 3 more P0 routing/profile bugs + 1 P0 Resend gap. User-flagged "lỗ hổng quá lớn, không có review cấu hình env của production sao".
**Estimated wall-clock:** ~5-6h. Code-prep parallel ~45 min (5 agents background, all LOW-MEDIUM stake — config edits + script writes); live ops sequential ~2-3h (deploy + verify + Resend manual provision).

---

## 1. Brainstorm

**Q1 (alignment):** Phase 1 BETA launch blocker — Plan 1 self-test cannot work end-to-end without fixing 4 architectural bugs (B1-B4). Persona affected: all (P2 Owner first touch fail).

**Q2 (trade-offs):**
- **B2 fix Option A (SERVER_PORT=8080 env per service)** vs **Option B (gateway routes per-correct-port)** — chose A: single env var per service, fewer touchpoints, matches gateway's already-set assumption
- **B3 Option A (rename file `application-prod.yml` → `application-production.yml` × 5 services)** vs **Option B (change compose env `production` → `prod`)** — chose A: more explicit/readable; `production` is the conventional Spring profile name
- **Resend (B4):** Provision NOW (Phase 1 BETA needs email) vs defer — chose NOW: blocker for Plan 1 Bước 3+5 (email-link based)
- Audit scripts as separate Bucket E vs bundled per fix → Bucket E (codify class-of-bug prevention, reusable infra)

**Q3 (risks):**
- **B2 SERVER_PORT=8080 change** — services bind 8080 internally; container_name DNS already unique per service so no port conflicts on docker network. Healthcheck commands in compose (set Wave 70 Bucket C/GAP-505 fix) need port update.
- **B3 profile rename** — Spring loads `application-{ACTIVE}.yml` by literal name. Rename 5 files atomically; verify post-deploy via `docker logs <svc> | grep "active profile"`.
- **B1 gateway route add** — Spring Cloud Gateway routes ordered; add specific routes BEFORE catch-all `/api/v1/**` → kiteclass-core.
- **B4 Resend provisioning** — user-action gap; agent draft runbook only.
- **Image rebuild required** for Bucket B port change (Dockerfile `EXPOSE` may need update) + Bucket C profile rename (file rename means image artifact changes). Compose env override alone insufficient — need release tag bump + redeploy.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Files | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-509 gateway routing | `kitehub-gateway/application.yml` | ~30 min | ✅ gateway only |
| B | GAP-510 port consistency | `docker-compose.production.yml` (5 services) | ~30 min | ✅ compose only |
| C | GAP-511 Spring profile | rename 5x `application-prod.yml` → `application-production.yml` | ~20 min | ✅ rename only |
| D | GAP-508 Phase 2 Resend | `scripts/fetch-secrets.sh` + runbook | ~30 min | ✅ secrets fetch script |
| E | audit infra | 3 new scripts + rule extend | ~40 min | ✅ scripts/* + rule edit |

**Disjoint check:**
- A: only `kitehub-gateway/application.yml` (gateway routes)
- B: only `docker-compose.production.yml` (4 service env blocks)
- C: only rename 5 files (no content change beyond filename — file-system level disjoint per file)
- D: only `scripts/fetch-secrets.sh` + new runbook
- E: only `scripts/audit-*.sh` (new files) + `.claude/rules/production-env-config-registry.md` (append §)

Code-prep agents spawn parallel. Live ops sequential: B → C → release-tag bump → deploy → A test → D Resend manual.

---

## 3. Scope

**Stake tier:** **MEDIUM** (config + script edits, no Java/TS code change) → model: **Opus medium** for all buckets
**Cross-layer?** NO — pure backend/infra. Skip Bucket 0 Foundation.

| # | Bucket | Gap | Priority | Files | Spawn order |
|:-:|--------|-----|:--------:|-------|:-----------:|
| 1 | **A — Gateway routing fix** | GAP-509 | 🔴 P0 | `kitehub/kitehub-gateway/src/main/resources/application.yml` | parallel |
| 2 | **B — Port consistency SERVER_PORT=8080** | GAP-510 | 🔴 P0 | `docker-compose.production.yml` (sub/admin/branding/email blocks + healthcheck override) | parallel |
| 3 | **C — Spring profile naming** | GAP-511 | 🔴 P0 | 5 × `application-prod.yml` rename to `application-production.yml` | parallel |
| 4 | **D — Resend provisioning runbook + fetch-secrets extend** | GAP-508 Phase 2 | 🔴 P0 | `scripts/fetch-secrets.sh` + `documents/05-guides/deploy/resend-provisioning-runbook.md` (NEW) | parallel |
| 5 | **E — Audit infrastructure** | meta (rule extend) | 🟠 P1 | `scripts/audit-gateway-routes.sh` + `scripts/audit-service-ports.sh` + `scripts/audit-spring-profiles.sh` + `.claude/rules/production-env-config-registry.md` v1.1 | parallel |

**Live ops sequencing (post-merge, coordinator-managed):**

1. After all 5 bucket PRs merged + Wave 71 closure merged → image rebuild (Bucket C file rename = image artifact change) via release tag bump (`v0.9.0-beta-staging.12`) + docker-build-push.yml
2. `deploy-production.yml workflow_dispatch version=v0.9.0-beta-staging.12 confirm=DEPLOY`
3. Live verification — Plan 1 Bước 2 POST works end-to-end + email link arrives + click works
4. User completes Bucket D Resend manual provisioning (browser: Resend account + domain verify) + populate AWS Secrets Manager
5. Final deploy (Bucket D secret fetch) + Plan 1 Bước 5 email send verify

### Bucket A — Gateway routing fix (GAP-509)

**Goal:** Add `/api/v1/auth/**` + `/api/v1/admin/**` specific routes → `kitehub-subscription:8080` BEFORE catch-all `/api/v1/**` → kiteclass-core.

- Files: `kitehub/kitehub-gateway/src/main/resources/application.yml`
- Edits: Add 2 new route entries to `spring.cloud.gateway.routes` BEFORE the existing `instance-apis` `/api/v1/**` catch-all:
  ```yaml
  - id: kitehub-auth-v1
    uri: http://kitehub-subscription:8080
    predicates:
      - Path=/api/v1/auth/**
    filters:
      - name: CircuitBreaker
        args:
          name: authCircuitBreaker
          fallbackUri: forward:/fallback/auth
  - id: kitehub-admin-v1
    uri: http://kitehub-admin:8080
    predicates:
      - Path=/api/v1/admin/**
    filters:
      - name: CircuitBreaker
        args:
          name: adminCircuitBreaker
          fallbackUri: forward:/fallback/admin
  ```
- Verify: `docker exec kitehub-gateway curl -s http://localhost:8080/actuator/gateway/routes | jq '.[] | select(.predicate | contains("v1/auth"))'` returns the new route
- Acceptance: POST `https://api.kitehub.me/api/v1/auth/request-beta-access` reaches kitehub-subscription (201 on valid payload, NOT 400-empty)

### Bucket B — Port consistency SERVER_PORT=8080 (GAP-510)

**Goal:** Set `SERVER_PORT: 8080` env var trong compose for 4 BE services. Spring binds 8080, gateway routes (already `:8080`) work.

- Files: `docker-compose.production.yml`
- For each: subscription, admin, branding, email — add `SERVER_PORT: 8080` to `environment:` block
- Update healthcheck override (from GAP-505) to use port 8080:
  ```yaml
  healthcheck:
    test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 150s
  ```
  (Replace per-service 8081/8083/8084 currently in healthcheck commands)
- Verify: post-deploy `docker exec <svc> netstat -ltn | grep 8080` shows listening + Spring log "Tomcat started on port 8080"
- Acceptance: All 5 services Up `(healthy)` ≥10 min + cross-service gateway routes reach backends without 502

### Bucket C — Spring profile naming (GAP-511)

**Goal:** Rename 5 × `application-prod.yml` → `application-production.yml` để match `SPRING_PROFILES_ACTIVE=production`.

- Files (5 rename ops):
  - `kitehub/kitehub-gateway/src/main/resources/application-prod.yml` → `application-production.yml`
  - `kitehub/kitehub-subscription/src/main/resources/application-prod.yml` → `application-production.yml`
  - `kitehub/kitehub-admin/src/main/resources/application-prod.yml` → `application-production.yml`
  - `kitehub/kitehub-branding/src/main/resources/application-prod.yml` → `application-production.yml`
  - `kitehub/kitehub-email/src/main/resources/application-prod.yml` → `application-production.yml`
- Use `git mv` (preserves history)
- Verify: Post-deploy each service log contains `The following 1 profile is active: "production"` AND loads production-only config (e.g. `database.lifecycle.enabled: true`, `captcha.enabled: true`, `logging.level.root: WARN`)
- Acceptance: Live verification command:
  ```bash
  docker logs <svc> 2>&1 | grep -iE "active profile|profiles active"
  ```
  shows production profile picked up

### Bucket D — Resend provisioning runbook + fetch-secrets extend (GAP-508 Phase 2)

**Goal:** Document Resend account provisioning + extend fetch-secrets.sh to populate RESEND_API_KEY from AWS Secrets Manager.

- Files:
  - `documents/05-guides/deploy/resend-provisioning-runbook.md` (NEW per `deployment-naming-convention.md`)
  - `scripts/fetch-secrets.sh` — add Resend fetch logic (similar to JWT_SECRET pattern)
- Runbook sections:
  - §1 Pre-flight: Resend account creation, domain (kitehub.me) verification (DKIM, SPF, DMARC DNS records via Cloudflare)
  - §2 Generate API key in Resend dashboard
  - §3 Store in AWS Secrets Manager: `aws secretsmanager create-secret --name kitehub/production/resend-api-key --secret-string "<key>"`
  - §4 Verify fetch-secrets.sh extension by triggering deploy + checking `docker exec kitehub-email env | grep RESEND_API_KEY` non-empty
  - §5 Test send via `curl -X POST .../request-beta-access` + verify Resend dashboard delivery + recipient inbox
  - §6 Rollback: rotate key in Resend; revoke + replace in AWS SM
- Acceptance:
  - Runbook 6 sections complete
  - `fetch-secrets.sh` block added to fetch + write `RESEND_API_KEY` (idempotent: fall back to GH Secret env var if SM unavailable, per existing pattern)
  - Deploy verification post-merge: env var populated in kitehub-email container

### Bucket E — Audit infrastructure (meta)

**Goal:** 3 new audit scripts catching the class of bugs that escaped Wave 70/pre-Plan-1 review.

- Files (new):
  - `scripts/audit-gateway-routes.sh` — scans gateway application.yml routes; cross-checks every backend controller's `@RequestMapping`/`@PostMapping` path has a matching gateway route. FAIL on orphan routes.
  - `scripts/audit-service-ports.sh` — scans each service application.yml `server.port` default + verifies docker-compose `SERVER_PORT` override OR gateway route URI matches.
  - `scripts/audit-spring-profiles.sh` — scans compose `SPRING_PROFILES_ACTIVE` value + verifies `application-{value}.yml` file exists per service.
- Rule extension: `.claude/rules/production-env-config-registry.md` v1.0.0 → v1.1.0 adds §11 "Three new audits (routing/ports/profiles)" + cross-link to new scripts.
- Acceptance:
  - 3 scripts runnable, exit 1 on detected issues, exit 0 clean
  - Each script self-test: run against current state → all 3 FAIL (catches B1/B2/B3 retroactively)
  - Rule v1.1.0 log entry + paired self-test

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Evidence | Verdict |
|---|---|---|---|---|
| `kitehub-gateway/application.yml` | Gateway routes config | `ls kitehub/kitehub-gateway/src/main/resources/application.yml` | Exists 290+ lines | ✅ exists |
| `instance-apis` route (line ~172, `/api/v1/**` → `kiteclass-core:8080`) | Existing route to fix-around | `grep -n 'kiteclass-core' kitehub/kitehub-gateway/src/main/resources/application.yml` | Line 170 | ✅ exists |
| 5 × `application-prod.yml` files | Files to rename | `ls kitehub/*/src/main/resources/application-prod.yml` | 5 files found | ✅ exists |
| `docker-compose.production.yml` BE service blocks | Files to edit | `grep -n 'kitehub-subscription:\|kitehub-admin:\|kitehub-branding:\|kitehub-email:' docker-compose.production.yml` | 4 service entries | ✅ exists |
| `scripts/fetch-secrets.sh` | File to extend | `ls scripts/fetch-secrets.sh` | Exists | ✅ exists |
| `documents/05-guides/deploy/resend-provisioning-runbook.md` | New runbook | `ls documents/05-guides/deploy/resend-*` | Does not exist | 🆕 to-be-created (Bucket D) |
| `scripts/audit-gateway-routes.sh` | New audit | `ls scripts/audit-gateway-routes.sh` | Does not exist | 🆕 to-be-created (Bucket E) |
| `scripts/audit-service-ports.sh` | New audit | `ls scripts/audit-service-ports.sh` | Does not exist | 🆕 to-be-created (Bucket E) |
| `scripts/audit-spring-profiles.sh` | New audit | `ls scripts/audit-spring-profiles.sh` | Does not exist | 🆕 to-be-created (Bucket E) |
| `.claude/rules/production-env-config-registry.md` | Rule to extend | `ls .claude/rules/production-env-config-registry.md` | Exists v1.0.0 | ✅ exists |
| Bucket A new gateway routes `kitehub-auth-v1`, `kitehub-admin-v1` | Symbols to create | `grep -E 'kitehub-auth-v1|kitehub-admin-v1' kitehub/kitehub-gateway/src/main/resources/application.yml` | 0 matches | 🆕 to-be-created (Bucket A) |

---

## 5. Verification Gates

| Bucket | Local verify | Live verify (post-deploy) |
|--------|--------------|---------------------------|
| A | `bash scripts/audit-gateway-routes.sh` (Bucket E ships) OR manual `cd kitehub && mvnw -pl kitehub-gateway compile` | Gateway actuator routes endpoint shows kitehub-auth-v1 route + POST `/api/v1/auth/request-beta-access` reaches subscription |
| B | `python3 -c "import yaml; yaml.safe_load(open('docker-compose.production.yml'))"` | All 5 services Spring log "Tomcat started on port 8080"; ALB target healthy; cross-service routes reachable |
| C | All 5 files renamed; `cd kitehub && mvnw -pl <each> verify` | Spring log "active profile: production" in 5 services; production-only config loaded (verify via /actuator/env) |
| D | `shellcheck scripts/fetch-secrets.sh` | Post user-action: `docker exec kitehub-email env \| grep RESEND_API_KEY` non-empty |
| E | Each audit script exits 0 on clean state OR exits 1 with specific finding | 3 scripts FAIL pre-Wave-71-deploy (catches B1/B2/B3) → PASS post-deploy |

**Wave-level GA criteria (BEFORE flip Plan 1 unblock):**
- 10 consecutive POST `/api/v1/auth/request-beta-access` via `kitehub.me` → 201 + DB row inserted in `beta_access_requests`
- Email path: at least 1 invite-email sent + delivered to user-provided test address (requires Bucket D Resend complete)
- All 5 services Up `(healthy)` ≥30 min continuous post-deploy
- 3 audit scripts pass post-deploy

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 5 buckets parallel `run_in_background: true` + `isolation: worktree` + RELATIVE paths
- Opus medium (MEDIUM stake — config + scripts, no business logic)
- Coordinator waits all background, merges sequentially A→B→C→D→E
- Live ops sequential post-merge per §3

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Each bucket PR updates GAP-509/510/511/508 Log + status appropriately
- Wave-level GA gate MUST live-pass BEFORE flip GAP-509/510/511 → 🟢 DONE
- ROADMAP §🚀 Next Action updated in closure PR
- Wave plan frontmatter `status: complete`
- `wave-history.jsonl` append
- Run `bash scripts/prune-merged-worktrees.sh --yes` post-all-merge
- `## Release Plan Progress` section in closure PR — Phase 1 BETA readiness update

---

## 8. Log

- **2026-05-13 (draft):** Plan created post-Plan-1 self-test 4-bug surfacing. User-confirmed scope "fix tất cả" + comprehensive audit Layer 1-5 surfaced B1-B4. 5 buckets parallel config + scripts. Live ops sequential. Bucket D Resend has user-action gate.
