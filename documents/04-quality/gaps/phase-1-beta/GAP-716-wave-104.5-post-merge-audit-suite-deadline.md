---
id: GAP-716
title: Wave 104.5 PR #1715 post-merge audit obligations — business-logic + api-contract + ops-readiness within 3-day deadline 2026-05-25
status: OPEN
priority: P1
phase: phase-1-beta
audience: dev
found: 2026-05-22
related: [GAP-708, GAP-709, GAP-711, GAP-712, GAP-713, GAP-714, GAP-715]
---

# GAP-716 — Wave 104.5 PR #1715 post-merge audit obligations

## Problem

`audit-gate.py` hook post-merge flagged 4 compliance violations on PR #1715 (Wave 104.5 close-loop, merged 2026-05-22T11:43:25Z, commit `7da609f5`):

1. **CI status: unknown** — race between merge timing + hook fetch (33 PASS / 1 skip confirmed prior to merge)
2. **5 Java files, 0 test files** — code changes shipped without paired IT/unit tests
3. **Business logic changed but no `documents/01-business/` docs updated** — auth + onboarding flow semantics changed (JWT fallback, gateway routing) but rules.md / use-cases.md / api-contract.md not synced same PR
4. **Missing audits**: `business-logic-audit`, `api-contract-audit`, `ops-readiness-audit` within 3-day window per `post-wave-audit-mandate.md` §2.2

Per `audit-gate.py` AUDIT_RULES — until these obligations close, subsequent PRs touching same domain will be blocked.

## Code-changed scope summary

| File | Domain | Test paired? |
|---|---|---|
| `kitehub-subscription/.../OnboardingProgressController.java` | onboarding, auth (tenant resolution) | ❌ no IT |
| `kitehub-subscription/.../consumer/EmailConsumer.java` | email (config) | ❌ no IT |
| `kitehub-subscription/.../client/EmailServiceClient.java` | email (config) | ❌ no IT |
| `kitehub-subscription/.../audit/AdminAuditLog.java` | audit (JPA mapping) | ❌ no IT (Testcontainers required per `postgres-specific-type-testcontainers.md`) |
| `kitehub-gateway/.../filter/TenantResolverGatewayFilterFactory.java` | gateway routing, auth | ❌ no IT |
| `kitehub-subscription/src/main/resources/application.yml` | config | n/a |
| `kitehub-gateway/src/main/resources/application.yml` | config (route added) | n/a |
| `kitehub/docker-compose.kitehub.yml` | infra (env var) | n/a |

## Required obligations (within 3-day window — deadline 2026-05-25)

### Audits to run

1. **`business-logic-audit`** — covers Wave 104.5 changes to:
   - Auth flow JWT enrichment (Wave 104 Bucket A) + JWT fallback tenant resolution (GAP-711 + GAP-712)
   - Onboarding-progress tenant scoping changes
   - Score per `quality/business-logic-audit/SKILL.md` /100

2. **`api-contract-audit`** — covers:
   - `/api/v1/onboarding-progress` GET semantics changed (no X-Tenant-Id header now valid via JWT)
   - Gateway routing added: `kitehub-onboarding-progress` route
   - `/api/v1/auth/2fa/enroll-init` via gateway now functional (was blocked by missing JWT_CHALLENGE_SECRET env)
   - Score per `quality/api-contract-audit/SKILL.md` /100

3. **`ops-readiness-audit`** — covers:
   - docker-compose JWT_CHALLENGE_SECRET added (Wave 104.5 config)
   - rebuild.sh patch (gateway alias mapping)
   - Score per `quality/ops-readiness-audit/SKILL.md` /100

### Business docs to sync (`documents/01-business/`)

Per `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync + CLAUDE.md §Living Docs:

- `documents/01-business/auth/api-contract.md` — document JWT tenantId claim enrichment + gateway+controller JWT fallback behavior
- `documents/01-business/onboarding/api-contract.md` — document X-Tenant-Id header now OPTIONAL (JWT fallback acceptable)
- `documents/01-business/email/api-contract.md` (if exists) — N/A (config-only change)

### Tests to add

Per `postgres-specific-type-testcontainers.md` v1.0.0 mandate:
- `AdminAuditLogPostgresIT` — Testcontainers IT exercising CRUD round-trip on `payload_json` + `before_state` + `after_state` JSONB columns (catches `[B` binding regression class)

Per general test discipline:
- `TenantResolverGatewayFilterFactoryTest` — unit test for `extractJwtTenantClaim` + JWT fallback path
- `OnboardingProgressControllerResolveTenantTest` — unit test for 3 paths (header only, JWT only, both with mismatch)

## Acceptance Criteria

- [ ] 3 audits run + reports filed under `documents/04-quality/audits/{category}/2026-05-22-wave-104.5-*.md`
- [ ] `audits-index.csv` rows added for 3 audit reports
- [ ] `documents/01-business/auth/api-contract.md` + `documents/01-business/onboarding/api-contract.md` synced với Wave 104.5 semantic changes
- [ ] `AdminAuditLogPostgresIT` Testcontainers IT shipped + PASS in CI
- [ ] `audit-gate.py` hook re-evaluation post-fix PR returns 4/4 compliance
- [ ] No new P0/P1 surfaced; if surfaced → file Wave 105+ follow-up

## Related

- Triggered by: 2026-05-22 audit-gate.py post-merge hook on PR #1715 (merge commit `7da609f5`)
- Sister: GAP-708 (Wave 103 same audit suite obligation — pattern recurrence)
- Sister: GAP-709 (Wave 103 01-business auth docs sync — pattern recurrence)
- Closes Wave 104.5 loop fully when this gap → DONE
- Reference: `post-wave-audit-mandate.md` §2.2 3-day freshness window
- Reference: `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync
- Reference: `postgres-specific-type-testcontainers.md` v1.0.0 mandate
- Hook log: `documents/03-planning/pr-logs/PR-1715.json`
