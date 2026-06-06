---
title: Wave auth-2 — post-audit auth-1 follow-up fixes
status: active
created: 2026-06-06
updated: 2026-06-06
waves: [auth-2]
tag_primary: auth
tags_secondary: [tenant-auth, parent, teacher, jwt, kiteclass, post-audit]
gaps: [GAP-1009, GAP-1010, GAP-1011, GAP-1013, GAP-1014]
---

# Wave auth-2 — post-audit auth-1 follow-up fixes

Fixes the remaining 5 gaps from the Wave auth-1 post-wave audit suite (GAP-1009..1014; GAP-1012 already DONE PR #2189). User directive 2026-06-06 "optimize wave and fix all" (explicit override of Flow Verification Campaign pick-gap-to-fix pause).

## 1. Brainstorm

- **Scope source:** 3 audit reports `documents/04-quality/audits/{business-logic,api-contract,ops-readiness}/2026-06-06-wave-auth-1-*.md`. No P0 (merge was safe); all P1/P2.
- **Decisions locked (user, 2026-06-06):** GAP-1011 → **Option A** (keep UNIQUE(email) global, document 1-email-1-tenant Phase 1, provisioning rejects cross-tenant email reuse with clear error). GAP-1014 → **defer compose** (kc-core prod deploy stays GAP-444/Phase-7; ship secrets.tf desc + PARENT_PORTAL_ENABLED override + document blocker → PARTIAL).
- **Outside-in audit:** not triggered — audit-driven internal fixes (tests/docs-sync/hardening/devops), not new user-facing scope (`outside-in-coverage-trigger.md` §4 internal-scope exception).
- **Conflict matrix:** GAP-1010/1011/1013 all touch `kiteclass-core` auth module + tests → **same bucket B** (one agent). GAP-1009 docs + GAP-1014 devops are disjoint → parallel-safe.

## 2. Task Breakdown

3 buckets, 3 parallel Opus worktree agents. Agents fix code/docs/tests + push a PR to main. They do **NOT** touch `gap-status.csv` / ROADMAP / file-moves (avoids parallel-CSV conflict — GAP-1009..1014 are adjacent CSV rows). Coordinator does ALL gap closure centrally after merges.

## 3. Scope

### Bucket A — GAP-1009 tenant-auth business docs (Docs)
- Create `documents/01-business/kiteclass/tenant-auth/{rules,use-cases,api-contract}.md` — BR-AUTH-xxx (entity_type CHECK / BCrypt / HS512 claims role+tenantId+referenceId / uniform-401 / TTL 12h) + endpoints `POST /api/v1/tenant-auth/login` + `POST /api/v1/teachers/{id}/credentials` (request/response + error tables) + anti-spoof `X-User-Reference-Id` header contract.
- Update `parent-portal/` + `student-portal/` docs Option A → Option B (referenceId = `auth_credentials.entity_id` claim). Update BR-PARENT-004 (PARENT_PORTAL_ENABLED=true + PDPL note).

### Bucket B — GAP-1010 + GAP-1011 + GAP-1013 (KC auth code+tests)
- **GAP-1010:** AuthServiceTest + AuthTokenServiceTest + AuthCredentialProvisioningServiceTest + AuthCredentialPostgresIT (Flyway, not ddl-auto) + MVC AuthControllerIT (200 + uniform-401×3 + 400).
- **GAP-1011 (Option A):** `AuthCredentialProvisioningService` rejects/409 cross-tenant email reuse (same email, different instance_id) with clear error; do NOT silently return wrong-tenant credential. Document 1-email-1-tenant Phase 1 limit inline.
- **GAP-1013 hardening:** setPassword reject cross-entity (entityType/entityId mismatch); disable credential on entity INACTIVE/deleted; unify password policy parent+teacher; mask login-fail email log; JWT add jti; dummy-BCrypt timing flatten.

### Bucket C — GAP-1014 prod parity (DevOps, → PARTIAL)
- `infrastructure/terraform-aws/secrets.tf` jwt desc HS256→HS512 (consider length margin note).
- `PARENT_PORTAL_ENABLED=true` prod override path (fetch-secrets.sh / production compose env).
- Document kc-core prod-deploy blocked-by GAP-444 (Phase 7) — do NOT add kc-core to docker-compose.production.yml. GAP-1014 stays PARTIAL.

## 4. State-Check Evidence

| Symbol | Verdict |
|---|---|
| `AuthCredentialProvisioningService` (Bucket B 1011/1013 target) | ✅ exists `kiteclass-core/.../module/auth/service/` |
| `AuthService` / `AuthTokenService` / `AuthController` (B 1010 test targets) | ✅ exist (Wave auth-1) |
| `V89__create_auth_credentials.sql` (B 1011 schema ref) | ✅ exists |
| `documents/01-business/kiteclass/tenant-auth/` (A creates) | 🆕 to-be-created (Bucket A owns) |
| `documents/01-business/kiteclass/parent-portal/` (A edits) | ✅ exists |
| `infrastructure/terraform-aws/secrets.tf` (C edits) | ✅ exists (jwt resource line 37) |
| `BR-PARENT-004` (A edits) | ✅ exists `parent-portal/rules.md` |

## 5. Verification Gates

- Each bucket PR: CI green on main (self-hosted) before merge; no `--admin` (per `admin-merge-discipline.md`).
- Bucket B: `./mvnw -pl kiteclass-core test` PASS (new auth tests + hardening); AuthCredentialPostgresIT on Testcontainers Flyway schema (per `postgres-specific-type-testcontainers.md` + `kiteclass-core IT ddl-auto` memory).
- Bucket A: `check-3-layer-completeness.sh` PASS for `kiteclass/tenant-auth`.
- Bucket C: `terraform validate` / shellcheck on edited scripts.
- Caller-sweep (per `api-contract-change-caller-sweep.md`) for any method-contract change in Bucket B (run tests, not just compile).

## 6. Agent Spawn Pattern

3 parallel Opus worktree-isolated agents (per `agent-model-opus-default.md` + `feedback_worktree_absolute_path_contamination.md`), `run_in_background: true`. Each: fix bucket scope, push branch, open PR to main. Agents do NOT edit `gap-status.csv` / `ROADMAP.md` / move gap files (coordinator does central closure — avoids parallel-CSV conflict on adjacent rows GAP-1009..1014).

## 7. Closure Protocol

Per `gap-done-discipline.md` + `gap-folder-organization.md` §3.3, coordinator flips GAP-1009/1010/1011/1013 → DONE (move to `closed/`) + GAP-1014 → PARTIAL after bucket PRs merge + AC verified; one central closure PR (CSV + ROADMAP + wave-history + audits refresh).

## 8. Log

- **2026-06-06:** Plan created. User directive "optimize wave and fix all" (explicit override of Flow Verification Campaign pause). Decisions locked via AskUserQuestion: GAP-1011 Option A + GAP-1014 defer-compose. 3-bucket wave-pack.
