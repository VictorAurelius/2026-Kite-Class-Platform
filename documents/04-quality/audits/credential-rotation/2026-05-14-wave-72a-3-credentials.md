---
title: Credential Rotation — Wave 72a (3 credentials from 2026-05-13 session)
status: pending
created: 2026-05-14
trigger: leak
related-gaps: [GAP-525]
wave: 72a
---

# Credential Rotation Audit — Wave 72a (3 credentials)

## Scope

3 production credentials surfaced in the 2026-05-13 Claude Code session transcript per [GAP-525](../../gaps/GAP-525-rotate-credentials-leaked-session-2026-05-13.md). Transcript is stored locally at the user's Claude project directory; Anthropic API traffic carried the values in conversation context. Defense-in-depth response: rotate all 3 even though no external compromise is confirmed.

This artifact documents the rotation procedure (filed pre-rotation per `credential-rotation-runbook.md` §5) and tracks completion status. **User executes actual rotation steps after Wave 72a merge.** Coordinator (this artifact's filer) does NOT have authority to revoke vendor tokens; user-action is mandatory for vendor portal steps.

---

## Credentials (no secret values — IDs only)

| # | Credential class | Secret reference / vendor | Scope | Sensitivity |
|---|---|---|---|---|
| 1 | Admin password (PLATFORM_ADMIN seed account `admin@kitehub.me`) | AWS Secrets Manager secret-id `kitehub/production/admin-seed-password` | Platform admin login (full kitehub-admin console) | 🔴 critical |
| 2 | Cloudflare API token (read-only) | Vendor: Cloudflare (https://dash.cloudflare.com/profile/api-tokens) | "Read all resources" — list DNS, zones, Email Routing rules; cannot modify | 🟠 moderate |
| 3 | Resend API key (sending) | Vendor: Resend (https://resend.com/api-keys) + AWS Secrets Manager secret-id `kitehub/production/resend-api-key` | "Sending access" restricted to `kitehub.me` sending domain | 🟠 moderate |

**Note:** secret values intentionally omitted from this artifact per `.claude/rules/agent-aws-access.md` §2.2 (banned reveals). References only.

---

## Pre-rotation checklist (per `credential-rotation-runbook.md` §3)

- [x] **All consumers identified** — see "Consumers per credential" below
- [x] **Downtime window estimated** — zero downtime for #1 (re-seed atomic) + #2 (vendor-side new token, no service restart needed); ~30s container restart for #3 (kitehub-email reads key at startup)
- [x] **Rollback path noted** — AWS Secrets Manager retains previous version 30 days; vendor old tokens stay alive until user revokes (step 6 of §2.3)
- [x] **Maintenance window communicated** — N/A (zero/minimal downtime; solo-dev mode)
- [x] **AWS Secrets Manager version retention confirmed** — 30-day default applies
- [x] **Audit artifact file path drafted** — this file

### Consumers per credential

| Credential | Consumers |
|---|---|
| Admin password | `scripts/seed-direct-sql.sh` (re-seeding); admin login UI |
| Cloudflare API token | Local DNS audit scripts (e.g., `scripts/check-dns-propagation.sh` if it reads CF API); Cloudflare MCP server config in user's session; potential future Workers deploy CI |
| Resend API key | `kitehub-email` service (`ResendClient` at startup); transactional email flows in `EmailService` |

---

## Rotation status

All entries `pending` at artifact creation. User updates this table as each step completes.

| # | Credential | Status | Rotated_at | Rotated_by | New secret reference |
|---|---|---|---|---|---|
| 1 | Admin password | pending | — | — | AWS Secrets Manager new version-id of `kitehub/production/admin-seed-password` |
| 2 | Cloudflare API token | pending | — | — | New token at Cloudflare → optionally stored in AWS Secrets Manager `kitehub/production/cloudflare-api-token` |
| 3 | Resend API key | pending | — | — | New version-id of AWS Secrets Manager `kitehub/production/resend-api-key` |

**Status enum:** `pending` → `in-progress` → `done` → `verified`

---

## Verification steps per credential

### #1 Admin password (per runbook §2.1)

After rotation, user verifies:

```bash
# (a) New password works
# In browser: POST /api/auth/login with admin@kitehub.me + new password → 200 + JWT
# Then /admin dashboard renders (role-guard accepts)

# (b) Old password rejected (if old value remembered for one final test)
# POST /api/auth/login with admin@kitehub.me + OLD password → 401

# (c) Service startup clean
docker compose logs kitehub-subscription --tail=50 | grep -iE "error|auth|seed"
# Expect zero auth errors post re-seed

# (d) Secrets Manager version progression
aws secretsmanager describe-secret \
  --secret-id kitehub/production/admin-seed-password \
  --query 'VersionIdsToStages'
# Expect new version with AWSCURRENT stage; previous version with AWSPREVIOUS
```

### #2 Cloudflare API token (per runbook §2.3.1)

```bash
# (a) New token works
curl -sH "Authorization: Bearer $NEW_TOKEN" \
  https://api.cloudflare.com/client/v4/user/tokens/verify | jq '.success'
# Expect: true

# (b) Old token rejected (after vendor revocation step)
curl -sH "Authorization: Bearer $OLD_TOKEN" \
  https://api.cloudflare.com/client/v4/user/tokens/verify | jq '.success'
# Expect: false (or HTTP 401)

# (c) Cloudflare dashboard audit log
# Browser: https://dash.cloudflare.com/?to=/account/audit-log → confirm "API Token Deleted" event
```

### #3 Resend API key (per runbook §2.3.2)

```bash
# (a) New key works — send test email
# Use internal email-test endpoint OR direct curl:
curl -X POST https://api.resend.com/emails \
  -H "Authorization: Bearer $NEW_KEY" \
  -H "Content-Type: application/json" \
  -d '{"from":"noreply@kitehub.me","to":"dev@kitehub.me","subject":"rotation test","html":"ok"}'
# Expect: 200 + email_id in response

# (b) Resend dashboard verifies delivery
# Browser: https://resend.com/emails → confirm "delivered" status

# (c) kitehub-email picks up new key (verification post-redeploy)
docker compose logs kitehub-email --tail=20 | grep -iE "resend|email|init"
# Expect: no "401" / "unauthorized" entries

# (d) Old key rejected
curl -sX POST https://api.resend.com/emails \
  -H "Authorization: Bearer $OLD_KEY" \
  -H "Content-Type: application/json" \
  -d '{"from":"noreply@kitehub.me","to":"dev@kitehub.me","subject":"old key test","html":"ok"}'
# Expect: 401
```

---

## Notes

- This is the **first** application of `credential-rotation-runbook.md` (shipped same Wave 72a Bucket D). Per `incident-to-rule-pipeline.md` §6 worked-example pattern, this artifact serves as the self-test for the runbook — if any step is ambiguous in practice, file follow-up to clarify the runbook.
- Per `pre-handoff-self-test-completeness.md` §2.4, the admin-flow rotation (#1) requires (a) credential available + (b) login flow works + (c) role-guard accepts + (d) admin dashboard renders — verification §1 above covers all 4.
- **No vendor revocation by coordinator.** Coordinator (Claude in this Wave 72a Bucket D session) cannot revoke Cloudflare or Resend tokens at vendor portals — those steps are user-action only. AWS Secrets Manager `put-secret-value` is also user-action (Tier 3 mutation per `agent-aws-access.md` §4).
- **Session transcript local file** at `~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/7517e076-d175-4fb2-bbdf-23bb355763d9.jsonl` per GAP-525 §Problem. Optional post-rotation cleanup: delete or redact the local file. Anthropic API traffic context window cannot be recalled retroactively — rotation is the only mitigation.

---

## Next steps (user-action checklist)

In this order:

1. [ ] **#1 Admin password rotation** — terraform re-roll OR `aws secretsmanager put-secret-value` + `seed-direct-sql.sh` re-seed (runbook §2.1). Update row 1 status → `in-progress` → `done` → `verified`.
2. [ ] **#3 Resend API key rotation** — generate new key at Resend portal; `aws secretsmanager put-secret-value`; redeploy `kitehub-email`; smoke-test email send; revoke old key (runbook §2.3.2). Update row 3 status.
3. [ ] **#2 Cloudflare API token rotation** — generate new token at Cloudflare portal (same "Read all resources" scope); optionally store in AWS Secrets Manager; update any local DNS audit scripts / MCP server config; revoke old token (runbook §2.3.1). Update row 2 status.
4. [ ] **Optional:** delete or redact local session transcript file (path in §Notes).
5. [ ] **Flip GAP-525** status `🟡 PARTIAL` → `🟢 DONE` when rows 1–3 all show `verified`. Update gap-status.csv (`completion_pct=100`, `last_verified=<rotation-completion-date>`).
6. [ ] **File a memory entry** at `feedback_credential_leak_session_2026_05_13.md` per `incident-to-rule-pipeline.md` Stage 5 — describes the "Option B paste in chat" pattern that surfaced these credentials + cross-links the runbook.

**Suggested order rationale:** #1 first because admin password is highest-sensitivity. #3 second because email is more frequently exercised than DNS audits (faster smoke-test feedback). #2 last because read-only token has lowest blast radius even if revocation is delayed.

---

## References

- Parent gap: [GAP-525](../../gaps/GAP-525-rotate-credentials-leaked-session-2026-05-13.md)
- General runbook: [`credential-rotation-runbook.md`](../../../05-guides/operations/credential-rotation-runbook.md)
- Sister runbooks: `secrets-rotation-runbook.md` (cadence), `secrets-seeding-runbook.md` (initial), `jwt-rotation-runbook.md` (per GAP-520 if shipped)
- Rules: `.claude/rules/agent-aws-access.md` §2.2 (banned reveals), `.claude/rules/pre-handoff-self-test-completeness.md` §2.4 (admin-flow verify), `.claude/rules/incident-to-rule-pipeline.md` (5-stage pipeline)
- Wave: Wave 72a Bucket D (this PR)
