# GAP-525: Rotate 3 credentials leaked in 2026-05-13 session transcript

**Status:** 🟡 PARTIAL — runbook + incident artifact shipped Wave 72a Bucket D 2026-05-14; actual rotation = user-action pending
**Priority:** 🔴 P0 (operational security — credentials appeared in transcript log)
**Domain:** DevOps / Security
**Found:** 2026-05-13 (Wave 71c-meta-Phase-2 — self-audit of residual session items)
**Affects:** 3 credentials surfaced in Claude Code session transcript today

## Problem

Per `agent-aws-access.md` §2.2 + `pre-handoff-self-test-completeness.md` v1.0.0, certain credential operations CAN leak the value into session transcripts. In Wave 71/71b/71c session 2026-05-13, 3 credentials surfaced:

| # | Credential | How surfaced | Sensitivity |
|---|---|---|---|
| 1 | admin@kitehub.me production password | `aws secretsmanager get-secret-value` (Tier 2 user-confirmed) per user request "log cho tôi pass của admin" | 🔴 critical — platform admin access |
| 2 | Cloudflare API token `cfut_B5d8tYY...` (read-only "Read all resources" scope) | User pasted in chat to enable DNS audit per `third-party-platform-automation-discovery.md` | 🟠 moderate — read-only, can list DNS + zones but not modify |
| 3 | Resend API key `re_hoMkdPyz_NNZikknUkX7Ne3ovGJ7LuEkJ` | User pasted in chat (Option B explicit choice over Option A user-runs-locally) | 🟠 moderate — sending access only, can send emails as kitehub.me |

Session transcript stored at `/home/nguyenvankiet/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/7517e076-d175-4fb2-bbdf-23bb355763d9.jsonl` on local disk; not in repo, not in cloud. But Anthropic API traffic includes these values in context.

## Proposed Fix (user-action — Claude cannot self-rotate own credentials user provided)

Run after Wave 71c Phase 1 P0s land + admin UI verified working:

1. **Admin password** — generate new via terraform `terraform apply -target=random_password.seed_admin_password -replace=random_password.seed_admin_password` + re-seed via seed-direct-sql.sh + verify login
2. **Cloudflare token** — Cloudflare dashboard → API Tokens → revoke `cfut_B5d8tYY...` + create new (or reuse different token for future audits)
3. **Resend API key** — Resend dashboard → API Keys → revoke `kitehub-production` key + create new + `aws secretsmanager put-secret-value --secret-id kitehub/production/resend-api-key` with new key + redeploy to pull

## Acceptance Criteria

- [ ] Admin password rotated; old password no longer works
- [ ] CF token revoked (verify via Cloudflare dashboard audit log)
- [ ] Resend API key rotated; kitehub-email container picks up new key (SSM verify length=35 prefix differs from `re_ho`)
- [ ] Session transcript local file optionally redacted/deleted post-rotation

## Related

- Triggered by: 2026-05-13 session pattern of "Option B paste in chat" + Tier 2 confirmed AWS read for self-test enablement
- Meta lesson: `pre-handoff-self-test-completeness.md` §2.1 row (a) credential delivery — Option A (user-runs-locally) preferred to avoid this; user explicitly chose B for speed; file this gap as the cost
- Rotation cadence going forward: `pre-launch-auth-hardening-checklist.md` §2.6 (JWT) + GAP-520 (JWT runbook) — extend to all platform secrets quarterly
- Runbook: [`documents/05-guides/operations/credential-rotation-runbook.md`](../../05-guides/operations/credential-rotation-runbook.md) (shipped Wave 72a Bucket D)
- Incident artifact: [`documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md`](../audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md)

## Log

- **2026-05-14** (Wave 72a Bucket D): Status flipped 🔵 OPEN → 🟡 PARTIAL. Runbook `documents/05-guides/operations/credential-rotation-runbook.md` + incident audit artifact `documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md` shipped. Procedure ready; actual rotation steps deferred to user-action per `agent-aws-access.md` §4 (Tier 3 mutations are user-execute only) + vendor portal revocations (Cloudflare, Resend) require user login. Per `gap-done-discipline.md` §3 PARTIAL exit ramp: AC items 1–4 remain unchecked until user completes the rotation per the incident artifact §"Next steps" checklist. Gap stays PARTIAL until rows 1–3 of the artifact's rotation-status table show `verified` status, at which point user flips to 🟢 DONE.
- **2026-05-13** (Wave 71c-meta-Phase-2): Gap filed. 3 credentials surfaced in session transcript identified.
