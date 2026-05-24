# GAP-508: Production env config registry meta-gap (governance class)

**Status:** 🟡 PARTIAL 90% — Phase 1 (rule + registry + scan + 4 P0 overrides) DONE; Phase 1b (registry extended for Wave 78 Bucket 0 4 NEW endpoints) DONE 2026-05-14; Phase 2 (RESEND_API_KEY IaC + script code path) + Phase 3 (CI gate WARN-mode) DONE Wave br-4 Bucket A (PR #1779); live verify deferred GAP-NEW-resend-live-verify-post-restore (gated GAP-612 AWS suspension)
**Priority:** 🔴 P0 (Phase 1 BETA launch blocker — systemic class of bugs)
**Domain:** Meta / DevOps / Governance
**Found:** 2026-05-13 (Plan 1 self-test surfaced CORS GAP-507 + 5 sibling env-config bugs)
**Affects:** Production config coverage across all kitehub-* + kiteclass-* services

## Problem

Plan 1 self-test 2026-05-13 surfaced 6 P0 production-blocking env-var bugs all sharing same pattern:
1. `CORS_ALLOWED_ORIGINS` localhost-only → CORS 403 (GAP-507)
2. `VERIFICATION_BASE_URL` localhost:3001 → email links dead
3. `EMAIL_PROVIDER` mock → emails never sent
4. `RESEND_API_KEY` empty → no email API access
5. `AWS_SES_FROM_EMAIL` noreply@localhost → invalid sender
6. `CDN_DOMAIN` localhost:9100 → branded asset URLs broken

**Common pattern:** `${VAR:dev-default}` in `application.yml` with NO production override set.

**Why missed by review:** No audit skill scans `${VAR:default}` patterns vs production env state. `quality-audit /100`, `ops-readiness-audit`, `security-audit`, `audit-of-trust` all skipped this. No centralized registry. Multiple override mechanisms (compose, fetch-secrets, GH Secret, AWS Secrets Manager) without single source of truth.

User flagged: "lỗ hổng quá lớn, không có review cấu hình env của production sao, đang quản lý tập trung không?"

Honest answer: NO formal review process, NO centralized management.

## Root Cause

Governance gap. Existing audits cover code/security/ops but not env-var coverage. `output-review-mandate.md` §3 has Ops Readiness row but doesn't drill per-var. No rule mandating registry.

Per `feedback_audit_of_trust_pass.md` — 5th recurrence of "AC `[x]` ≠ production-verified" pattern.

## Proposed Fix (Phase 1 — this PR)

1. ✅ Rule `production-env-config-registry.md` v1.0.0 (mandates registry + scan)
2. ✅ Registry doc `documents/02-architecture/env-vars-registry.md` (audit current state of 14 known vars)
3. ✅ Scan script `scripts/audit-env-coverage.sh` (detects suspect defaults, cross-checks overrides, FAIL on missing)
4. ✅ 3 P0 env overrides added to `docker-compose.production.yml`:
   - subscription: VERIFICATION_BASE_URL + EMAIL_SERVICE_URL
   - email: EMAIL_PROVIDER + AWS_SES_FROM_EMAIL + AWS_SES_FROM_NAME

## Phase 2 — RESEND_API_KEY provisioning (separate gap action)

❌ Still missing in production. Plan:
1. Provision Resend account + verify domain DKIM/SPF/DMARC
2. Generate API key
3. AWS Secrets Manager: `kitehub/production/resend-api-key`
4. Extend `scripts/fetch-secrets.sh` to pull this secret
5. Re-deploy + live verify

Blocks: Plan 1 Bước 5 (email delivery).

## Phase 3 — CI gate + automation (deferred ~7 days)

Per `incident-to-rule-pipeline.md` premature-rule guard:
- `.github/workflows/script-quality.yml` job `env-coverage` running scan script
- Tune scan script `ACCEPTABLE_DEFAULTS` to eliminate false positives (current 24 findings may include false positives)
- PR template checkbox

## Acceptance Criteria

- [x] Phase 1: rule + registry + scan + 3 P0 overrides shipped
- [x] Scan script runnable: `bash scripts/audit-env-coverage.sh` produces FAIL/PASS verdict
- [x] Registry has 14 known vars + 6 accepted-defaults
- [x] Phase 1b (Wave 78 Bucket C 2026-05-14): registry extended with 4 NEW endpoint families from Wave 78 Bucket 0 (PR #1349): `onboarding`, `feedback`, `beta-status`, `support`. Per-endpoint row includes required gateway route id, target rate-limit, cache policy, env-var implications (BETA_STATUS_DEFAULT_CONTENT, FEEDBACK_HONEYPOT_FIELD, SUPPORT_TICKET_NUMBER_PREFIX, SUPPORT_NOTIFY_EMAIL, ONBOARDING_TOTAL_STEPS). Required gateway-route additions explicitly enumerated for the BE-implementation PRs in Buckets A/B/F so they land same-PR per `production-env-config-registry.md` §11 `audit-gateway-routes.sh`.
- [ ] Phase 2: RESEND_API_KEY in production (separate user-action gap — provision Resend account + DKIM/SPF/DMARC + AWS Secrets Manager + extend fetch-secrets.sh)
- [ ] Phase 3: CI gate wired (~7 days post-stabilization)
- [ ] Post-merge: re-run Plan 1 Bước 5 (email send) succeeds

## Related

- Parent: GAP-502 (Wave 70 audit-of-trust + Plan 1 self-test pattern)
- Sibling P0: GAP-507 (CORS — one symptom)
- Rule: `production-env-config-registry.md` v1.0.0
- Memory pattern: `feedback_audit_of_trust_pass.md` recurrence #5
- Wave 71 candidate

## Log

- **2026-05-24 (Wave br-4 Bucket A — PR #1779):** Phase 2 + Phase 3 shipped code-only path. 7 files: `infrastructure/terraform-aws/secrets.tf` (+3 resources `random_password.resend_api_key_placeholder` + `aws_secretsmanager_secret.resend_api_key` + `aws_secretsmanager_secret_version` với `lifecycle ignore_changes` pattern matching jwt-challenge precedent Wave 81 GAP-509); `scripts/fetch-secrets.sh` (+ RESEND_API_KEY fetch line + env passthrough); `scripts/audit-env-coverage.sh` (ACCEPTABLE_DEFAULTS 10→13 entries + Spring-relaxed-binding alias detection + AUDIT_ROOT env override; eliminated 16 false positives, current 0 actionable); `scripts/tests/test-audit-env-coverage.sh` + 9 fixture files (NEW 3-scenario self-test); `.github/workflows/quality-rules-skills.yml` (+`env-coverage` job WARN-mode); `documents/02-architecture/env-vars-registry.md` (RESEND_API_KEY row flipped ❌ MISSING → 🟡 PARTIAL + Wave br-4 IaC parity note). Local verify all PASS (terraform fmt + audit script 0 actionable + 3/3 fixtures PASS). NO terraform apply (GAP-612 AWS suspended). Override trailer `LOCAL_FIX_PROD_PARITY_DEFER: live verify — AWS account suspended GAP-612 — follow-up GAP-NEW-resend-live-verify-post-restore (Wave br-5+ post-AWS-restore)`. Admin merge với `ADMIN_MERGE_OVERRIDE` per Wave 102.8 GAP-698 precedent (terraform-plan OIDC pre-existing infra fail, 25/26 other checks PASS). Status 75% → 90%. Live verify (5% remaining) deferred GAP-NEW-resend-live-verify-post-restore.
- **2026-05-14 (Wave 78 Bucket C):** Phase 1b — registry extended with Wave 78 Bucket 0 4 NEW endpoint families (`onboarding`, `feedback`, `beta-status`, `support`) per PR #1349 contract docs. Each family documented with required gateway route id, target rate-limit, cache policy, env-var implications. Lays the groundwork for Bucket A/B/F BE implementations to add the matching `application.yml` gateway routes + rate-limit filters in their PRs (same-PR per `production-env-config-registry.md` §11). Status 60% → 75%.
- **2026-05-13:** Filed retroactively from Plan 1 self-test 17:00Z. User-flagged systemic governance gap. Phase 1 (rule + registry + scan + 3 P0 overrides) shipped same PR. Phase 2 (Resend API key user-action) + Phase 3 (CI gate) deferred per `incident-to-rule-pipeline.md` premature-rule guard.
