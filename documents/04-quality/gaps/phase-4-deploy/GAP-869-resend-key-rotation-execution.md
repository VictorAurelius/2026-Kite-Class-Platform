# GAP-869: Resend API key rotation execution (Wave local-doable-7 Bucket E follow-up)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (production secret rotation hygiene + GAP-572 leak follow-up)
**Domain:** DevOps
**Found:** 2026-06-02
**Phase:** phase-1-beta
**Affects:** Resend production API key trong AWS Secrets Manager `kitehub/production/resend-api-key`
**Related Gaps:** GAP-572 (parent — schema mismatch + leak), GAP-370 (email production E2E parent), GAP-525 (Wave 81 Bucket C rotation pattern)

## Current State (verified 2026-06-02)

| Item | Status |
|---|---|
| AWS Secrets Manager `kitehub/production/resend-api-key` exists | ✅ (terraform IaC declared `secrets.tf` lines 137-167; manual real value set via console post Resend account verified) |
| Secret payload schema accept dual (JSON wrapper OR plain string) | ✅ `scripts/fetch-secrets.sh` lines 94-105 (Wave email-finalize-1 fix shipped 2026-06-01) |
| Schema-fail-fast guard in fetch-secrets.sh | ✅ Phase 4 GAP-572 (line 108-110 WARN khi key empty) |
| Terraform IaC parity (random_password + lifecycle ignore_changes + import block) | ✅ Wave aws-restore-1 (2026-05-26) shipped + Wave local-doable-7 schema doc comment lines 119-127 |
| Resend rotation runbook (Resend-specific Phase 1-5) | ✅ `documents/05-guides/operations/resend-rotation-runbook.md` shipped Wave local-doable-7 Bucket E (this PR) |
| **Actual rotation execution** (revoke 2026-05-15 leaked key + provision new key) | ❌ **PENDING — this gap scope** |
| Live email smoke verification post-rotate | ❌ PENDING (Phase 3 runbook) |
| Rotation log artifact | ❌ PENDING (`documents/05-guides/operations/credential-rotation-2026-XX-XX.md`) |

## Problem

GAP-572 (2026-05-15) surfaced 2 concerns:

1. **Schema mismatch** — `scripts/fetch-secrets.sh` JSON expectation vs Wave 81 plain-string storage → fetch returns empty → emails undelivered. **RESOLVED Phase 4 Wave email-finalize-1** (dual-schema accept).
2. **Key leak** — Claude vô tình ran `aws secretsmanager get-secret-value | head -c 30` để diagnose schema → exposed first 30 chars `re_6kMZaPV7_9dBEuh18zPeGs3BZQP` vào chat output. **PENDING rotation execution** (this gap).

Schema concern closed; leak concern requires:
- Dev access Resend dashboard `https://resend.com/api-keys`
- Dev access AWS Secrets Manager (`dev-admin` profile)
- AWS stack up (EC2 running, RDS available) for SSM SendCommand re-fetch + container restart
- Smoke email test path (welcome email signup flow live)

These dependencies blocked Wave local-doable-7 Bucket E from executing rotation; runbook + this follow-up gap ship instead.

## Root Cause

3 contributing factors blocked actual rotation execution Wave local-doable-7:

1. **AWS stack state uncertain** — agent execution context (worktree) không có AWS credentials; cannot verify EC2 running OR SSM SendCommand permissions per `agent-aws-access.md` Tier 2 always-confirm boundary
2. **Resend dashboard interactive** — vendor portal requires browser session + 2FA; per `agent-action-bias.md` §3 row 1 "No command path exists" exception (Resend API có endpoint create key nhưng requires admin-scope key, chưa provisioned)
3. **Production secret mutation** — per `agent-aws-access.md` §4.3 Tier 3 banned `put-secret-value` agent-initiated; requires user-trigger pattern per `dev-authorized-terraform-trigger.md` analog (no equivalent rule yet for secret rotation explicit override)

Per `outside-in-coverage-trigger.md` §3 — task spec explicit "do NOT execute actual Resend dashboard rotate — that's dev follow-up". Compliance with scope.

## Proposed Fix

### Phase 1 — Dev triggers rotation when ready

Dev (user) follows `documents/05-guides/operations/resend-rotation-runbook.md` §3 Phase 1-5 sequentially:

1. **Phase 1** (~5 min) — Generate new key Resend dashboard + store as JSON wrapper trong AWS Secrets Manager (stdin pipe pattern, no plaintext echo)
2. **Phase 2** (~3 min) — SSM SendCommand re-fetch + force-recreate `kitehub-email` container
3. **Phase 3** (~2 min) — Smoke welcome email signup flow → verify Resend dashboard log `Delivered` < 30s
4. **Phase 4** (~1 min) — Revoke old key Resend dashboard
5. **Phase 5** (rollback IF Phase 3 FAIL) — Restore AWSPREVIOUS version + verify production healthy

Total estimated dev wall-clock: **~15-20 min** (assuming AWS stack already up + Resend dashboard logged in).

### Phase 2 — Document rotation in artifact

Per `output-review-mandate.md` §3 (AWS verification reports row):

Create `documents/05-guides/operations/credential-rotation-2026-XX-XX.md` (new dated file per rotation) với:

```markdown
# Credential Rotation 2026-XX-XX

## Resend API key

- **Trigger:** GAP-869 leak follow-up (GAP-572 origin 2026-05-15)
- **Old key prefix:** re_6kMZaPV7_ (first 7 + last 4 chars only)
- **New key prefix:** re_<new-prefix>
- **Pre-rotate version-id:** <uuid>
- **Post-rotate version-id:** <uuid>
- **Smoke email recipient:** smoke-test+rotate-2026-XX-XX@kitehub.me
- **Smoke delivery time:** XX seconds
- **Resend dashboard log URL:** https://resend.com/emails/<uuid>
- **kitehub-email container restart time:** YYYY-MM-DDTHH:MM:SSZ
- **Old key revoke time:** YYYY-MM-DDTHH:MM:SSZ
- **Next rotation due:** YYYY-MM-DD (+90 days)
```

### Phase 3 — Update meta CSV + cross-links

Per `post-merge-sync-completeness.md` §2 4-target sync:

1. Update `gap-status.csv` row GAP-869: status `OPEN` → `DONE`, completion_pct `0` → `100`, last_verified `2026-XX-XX`
2. Update `gap-status.csv` row GAP-572: status `PARTIAL` → `DONE` (rotation closes original leak concern), completion_pct `75` → `100`
3. Update `documents/05-guides/operations/secrets-rotation-runbook.md` §3.5 last-rotated date for `resend-api-key` row
4. Append rotation log entry to `resend-rotation-runbook.md` §9 Log

## Acceptance Criteria

- [ ] Phase 1 — new Resend key generated via Resend dashboard `https://resend.com/api-keys` (named `kitehub-production-YYYY-MM-DD`, permission "Sending access")
- [ ] Phase 1 — new key stored as JSON wrapper `{api_key, from_email, from_name}` trong AWS Secrets Manager `kitehub/production/resend-api-key` via `aws secretsmanager put-secret-value` stdin pipe
- [ ] Phase 1 — old key version stage moves to AWSPREVIOUS automatically (AWS default behavior; verify via `aws secretsmanager list-secret-version-ids --include-deprecated`)
- [ ] Phase 2 — SSM SendCommand to kh-backend EC2 succeeds: `fetch-secrets.sh` re-runs, `/etc/kite/.env` `RESEND_API_KEY=` length > 0, `kitehub-email` container force-recreated healthy
- [ ] Phase 3 — Smoke welcome email signup flow returns HTTP 201, Resend dashboard shows `Delivered` status within 30s, recipient inbox receives email với correct From/Subject/body
- [ ] Phase 4 — Old key `re_6kMZaPV7_...` revoked via Resend dashboard, disappears from API Keys list, no further usage trong Resend Logs post-revoke timestamp
- [ ] Phase 5 (conditional) — IF Phase 3 FAIL: rollback to AWSPREVIOUS via `update-secret-version-stage`, kitehub-email restart, re-smoke verify OLD key path works
- [ ] Rotation log artifact `documents/05-guides/operations/credential-rotation-2026-XX-XX.md` created với schema per §Phase 2 above
- [ ] `secrets-rotation-runbook.md` §3.5 `resend-api-key` row last-rotated date updated
- [ ] `resend-rotation-runbook.md` §9 Log entry appended với rotation event
- [ ] `gap-status.csv` GAP-869 + GAP-572 synced per `post-merge-sync-completeness.md` §2

## Related

- GAP-572 (parent — schema mismatch + leak; Wave email-finalize-1 closed schema slice, this gap closes leak slice)
- GAP-370 (parent epic — email transactional production E2E)
- GAP-525 (Wave 81 Bucket C — rotation pattern precedent, schema diff)
- `documents/05-guides/operations/resend-rotation-runbook.md` (this PR — Resend-specific Phase 1-5 procedure)
- `documents/05-guides/operations/credential-rotation-runbook.md` §2.3.2 (generic 3rd-party API key shape)
- `documents/05-guides/operations/secrets-rotation-runbook.md` §3.5 (cadence — Resend = quarterly)
- `.claude/rules/agent-aws-access.md` §2.2 (BANNED `get-secret-value` — incident origin)
- `.claude/rules/pre-launch-secrets-hardening-checklist.md` §2.1 (zero hardcoded secrets — stdin pipe pattern enforcement)
- `.claude/rules/pre-flight-aws-lifecycle-check.md` §3 (Phase 2 SSM SendCommand prerequisite cred + state check)
- `.claude/rules/agent-action-bias.md` §3 row 1 (No command path exists — Resend dashboard interactive justified exception)

## Log

- **2026-06-02:** Filed during Wave local-doable-7 Bucket E. Resend rotation runbook shipped (`resend-rotation-runbook.md` Resend-specific 9-section runbook); actual rotation execution defer to dev trigger when (a) AWS stack up + cred check passes, (b) Resend dashboard access available, (c) maintenance window allocated. Per task spec explicit "do NOT execute actual Resend dashboard rotate — that's dev follow-up" — compliance documented. GAP-572 PARTIAL 60 → 75% (Phase 4 + IaC parity + runbook ship; Phase 1+2+3+5 execution = this gap scope). Per `outside-in-coverage-trigger.md` §4 row "Wave 100% internal scope (ops, refactor, tech debt)" — outside-in audit skipped, internal ops scope. Per `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync — sweep stale Resend refs in code completed (this PR); per `audit-to-gap-pipeline.md` §2.8 fix-time state-check — current state verified via `scripts/query-gaps.sh GAP-572` (returns CSV row PARTIAL 60% pre-this-PR).
