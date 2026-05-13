# GAP-508: Production env config registry meta-gap (governance class)

**Status:** 🟡 PARTIAL — Phase 1 (rule + registry + scan + 4 P0 overrides) DONE; Phase 2 (RESEND_API_KEY + CI gate) OPEN
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
- [ ] Phase 2: RESEND_API_KEY in production (separate user-action gap)
- [ ] Phase 3: CI gate wired (~7 days post-stabilization)
- [ ] Post-merge: re-run Plan 1 Bước 5 (email send) succeeds

## Related

- Parent: GAP-502 (Wave 70 audit-of-trust + Plan 1 self-test pattern)
- Sibling P0: GAP-507 (CORS — one symptom)
- Rule: `production-env-config-registry.md` v1.0.0
- Memory pattern: `feedback_audit_of_trust_pass.md` recurrence #5
- Wave 71 candidate

## Log

- **2026-05-13:** Filed retroactively from Plan 1 self-test 17:00Z. User-flagged systemic governance gap. Phase 1 (rule + registry + scan + 3 P0 overrides) shipped same PR. Phase 2 (Resend API key user-action) + Phase 3 (CI gate) deferred per `incident-to-rule-pipeline.md` premature-rule guard.
