# GAP-475: Smoke test coverage extensions — happy-path login, email loop, P95, MFA, migration verify, rollback cycle

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (gates beta tenant launch confidence)
**Domain:** DevOps / QA
**Found:** 2026-05-11 (review session post-Wave 61, audit-driven)
**Affects:** `scripts/smoke-test.sh`, `scripts/smoke-ses.sh`, beta-launch verification confidence

## Problem

Post-Wave 61 smoke-test review identified 6 coverage gaps in `scripts/smoke-test.sh` (636 LOC) + `scripts/smoke-ses.sh` (197 LOC). Current coverage strong on infra/contract/error-path/business-signup; weak on production-grade verification needed BEFORE inviting first beta tenant.

### Current state (verified 2026-05-11)

`smoke-test.sh` has 10 check functions covering:
- Health (4 actuator endpoints)
- Pages (KH landing + 3 legal + login + register)
- API JSON (health, public courses, settings)
- Error handling (empty body → 400 on register + login)
- Compliance (ConsentBanner mounted)
- No-502 (2 gateway routes)
- Beta signup happy E2E
- Logs E2E (env-gated `SMOKE_LOGS_E2E=1`)
- Stop-when-idle (env-gated `STOP_WHEN_IDLE_E2E=1`)

`smoke-ses.sh` verifies domain identity + DKIM tokens + production access status. **Does NOT send email → assert receive.**

### Gaps identified

1. **Auth happy-path login missing** — only error-path (400 on empty body) tested. No seeded admin user login → JWT issued → authenticated request flow. Login regression possible without detection.
2. **Email delivery E2E missing** — `smoke-ses.sh` checks SES domain status only. No actual send → inbox poll → delivery confirmation. SES misconfig (wrong template, bad SPF) escapes smoke.
3. **MFA/OTP path uncovered** — when SES production lives (step 4 cutover), MFA OTP email becomes critical. No test path.
4. **P95 latency assertion absent** — checks binary pass/fail. `curl -w "%{time_total}"` data available but no threshold gate. Slow-but-200 endpoints pass undetected.
5. **DB migration verify missing** — no Flyway `flyway_schema_history` head check. Migration drift between deploy + actual schema undetected.
6. **Rollback cycle test missing** — no "smoke → trigger rollback → smoke again" path. Rollback runbook (per `release-deploy-standard.md` §3.1) ships untested.

## Root Cause

GAP-377 + GAP-378 baseline smoke + rollback runbooks shipped 2026-05-06 (Phase 1 BETA P0) as foundation. Coverage breadth prioritized over depth. Production-grade verification (happy login, email loop, latency gates) explicitly deferred per `release-deploy-standard.md` §3.1 "PRE-RELEASE subset" — appropriate for staging.

Wave 61 stop-when-idle cutover model + imminent beta tenant invitation (step 4-7 of cutover gates) shifts smoke from staging-grade → production-grade requirement.

## Proposed Fix

6 sub-items, P1 cluster suitable for 1 wave-pack bucket (~2-3 hrs serial OR 1 wave with sub-bucket parallel agents):

### Sub-1 — Happy-path login (P1)
- Add `check_auth_happy_path` function: POST seed admin credentials → assert 200 + JWT in body → GET protected endpoint with `Authorization: Bearer <jwt>` → assert 200.
- Env-gated via `SMOKE_AUTH_E2E=1` + `SMOKE_AUTH_USER` + `SMOKE_AUTH_PASS` (avoid hard-coded creds in script).
- Reference `production-seed-runbook.md` for credential source.

### Sub-2 — Email delivery E2E (P1)
- Extend `smoke-ses.sh`: send template email to `SMOKE_EMAIL_RECIPIENT` env var → poll Mailgun/IMAP/SES suppression for receipt within 5min timeout.
- Decouple recipient: dedicated `smoke@kitehub.me` mailbox with IMAP creds (or Mailgun routing).
- Reference `email-ses-setup-runbook.md` §6 (post-approval verify) — that section currently manual.

### Sub-3 — MFA/OTP path (P1)
- Conditional on Sub-2 landing. Trigger signup → assert OTP email received → extract OTP → POST verify-email endpoint → assert success.
- Env-gated `SMOKE_MFA_E2E=1`.

### Sub-4 — P95 latency assertion (P2)
- Replace `check_health` simple curl with timing capture (`time_total`). Per-endpoint threshold map (e.g. `/actuator/health` <500ms, public courses <1500ms).
- Output JSON report `smoke-latency-{timestamp}.json` for trend tracking.
- Reference `performance-audit` skill thresholds (per GAP-135 SLO baseline).

### Sub-5 — DB migration verify (P2)
- Add `check_migration_head` function: query Flyway `flyway_schema_history` via gateway-proxied admin endpoint OR via `kubectl exec` if K8s context — assert latest version matches expected from `pom.xml` migrations dir scan.
- Env-gated `SMOKE_MIGRATION_VERIFY=1` (requires admin auth).

### Sub-6 — Rollback cycle test (P2)
- New script `scripts/smoke-rollback-cycle.sh`: snapshot deploy SHA → trigger rollback via `gh workflow run rollback.yml` → wait health → smoke → restore forward → smoke.
- Documents rollback-readiness time-to-recovery.
- Reference rollback runbook per GAP-378.

## Acceptance Criteria

- [ ] Sub-1 `check_auth_happy_path` added to `smoke-test.sh`, env-gated, dry-run exit 0
- [ ] Sub-2 `smoke-ses.sh` extended with send-receive E2E + dedicated test mailbox documented
- [ ] Sub-3 MFA path added, conditional on Sub-2
- [ ] Sub-4 latency assertion added with per-endpoint threshold map
- [ ] Sub-5 Flyway head check added, env-gated
- [ ] Sub-6 `smoke-rollback-cycle.sh` shipped + dry-run validated
- [ ] All sub-items run clean locally OR documented dry-run-only mode
- [ ] CSV row updated to DONE when all 6 ship OR PARTIAL with per-sub completion %

## Out-of-scope

- Lighthouse / FE performance smoke (covered by `performance-audit` skill cadence)
- Load test / sustained traffic (separate concern, post-beta-stabilization)
- E2E Playwright suite (GAP-403/404/406 scope)

## Related

- **Parent:** GAP-377 (smoke-test.sh baseline, DONE 2026-05-06)
- **Sibling:** GAP-377-followup-auth-route-checks (P3, route-name drift specific — different scope)
- **Sibling:** GAP-378 (rollback runbook baseline)
- **Reference rules:**
  - `release-deploy-standard.md` §3.1 (PRE-RELEASE subset) + §3.4 (PROD MAJOR full)
  - `agent-action-bias.md` §1 (CLI-first)
  - `gap-architecture-v2.md` (CSV canonical)
- **Reference runbooks:**
  - `email-ses-setup-runbook.md` §6 (post-approval verify)
  - `production-seed-runbook.md` (seed credentials)
- **Blocks:** First beta tenant invitation (recommend complete before step 7 final smoke per ROADMAP §🚀 cutover gates)

## Log

- **2026-05-11:** Gap filed audit-driven post-Wave 61 smoke-test review. 6 coverage gaps identified, sub-items decomposed P1 (1-3) + P2 (4-6) cluster.
