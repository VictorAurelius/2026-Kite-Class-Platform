# GAP-513: Resend manual provisioning (user-action) — unblock email delivery

**Status:** 🔵 OPEN (user-action)
**Priority:** 🔴 P0 (Plan 1 Bước 5 email send blocker)
**Domain:** DevOps / External vendor
**Found:** 2026-05-13 (GAP-508 Phase 2 closure)
**Affects:** All transactional emails (beta-signup confirmation, verify-email links, admin approve/reject notices)

## Problem

`RESEND_API_KEY` empty in production. Wave 71 Bucket D shipped:
- `documents/05-guides/deploy/resend-provisioning-runbook.md` (6 sections)
- `scripts/fetch-secrets.sh` extended to pull `kitehub/production/resend-api-key` from AWS Secrets Manager

But the AWS Secret itself is empty pending vendor account creation + domain verification.

## Proposed Fix (user-action only — Claude cannot do)

Follow `documents/05-guides/deploy/resend-provisioning-runbook.md`:
1. Create Resend account
2. Verify domain `kitehub.me` (DKIM + SPF + DMARC via Cloudflare DNS records, grey-cloud)
3. Generate API key (production scope)
4. Store in AWS Secrets Manager: `kitehub/production/resend-api-key` (JSON: `{api_key, from_email, from_name}`)
5. Trigger deploy-production.yml workflow_dispatch → fetch-secrets pulls + populates /etc/kite/.env
6. Live verify: register a beta-access account → verification email arrives at user's inbox

## Acceptance Criteria

- [ ] Resend account active + domain `kitehub.me` verified
- [ ] AWS Secret `kitehub/production/resend-api-key` exists with valid JSON payload
- [ ] Post-deploy `docker exec kitehub-email env | grep RESEND_API_KEY` returns non-empty
- [ ] Plan 1 Bước 5 verify: register beta-access → email delivered (manual inbox check)

## Related

- Parent: GAP-508 Phase 2
- Runbook: `documents/05-guides/deploy/resend-provisioning-runbook.md` (PR #1272 Wave 71 Bucket D)
- Sibling: GAP-509/510/511/512

## Log

- **2026-05-13:** Filed at Wave 71 closure. BE infrastructure ready; awaits user provisioning.
