# GAP-480: Beta invitation flow undefined — how does first end-user get invited?

**Status:** 🟢 DONE 2026-05-14 — runbook `beta-invite-flow.md` shipped (Wave 78 Bucket D)
**Priority:** 🟠 P1 (blocks Release 1 launch execution — invite step undefined)
**Domain:** Process / Product
**Found:** 2026-05-12 (Wave 64 deploy simulation walkthrough)
**Affects:** Release 1 critical path to invite first beta tenant

## Problem

Wave 64 deploy simulation walkthrough (`documents/04-quality/audits/aws-verification/2026-05-12-wave-64-pre-apply-plan-investigation.md`) surfaced critical process gap: **chưa định nghĩa làm sao mời end-user thực sự đầu tiên**.

Existing artifacts:
- Vercel landing page `https://kitehub.me` ✅ live (HTTP 200)
- Backend `https://api.kitehub.me` will be live after Wave 64 Step E + deploy-production
- SES production email infrastructure → after sandbox approval
- `production-seed-runbook.md` covers admin user seed
- Beta email templates ship Wave 33+ (`kitehub-email` service)

**Missing:**
- Who is first beta tenant? (1 contact? 5 contacts? Self?)
- How do they discover the invite? (Personal email? Landing signup open form? Closed invite codes?)
- What's the onboarding script? (Click link → land → signup form → email verify → first login → wizard?)
- Are there pre-launch screening / consent / NDA?
- Who handles first-day support? (`support@kitehub.me` forwards to vankiet14491@gmail.com — solo handle)
- Communication plan if production incident first 24h?

## Proposed Fix

Define + document beta invitation flow per `release-deploy-standard.md` §3.4 PROD MAJOR + ROADMAP §🚀.

Decision options:
- **Option A — Closed invite only:** create 5 invite codes; send manual email to known contacts; signup gated by invite code
- **Option B — Open signup + manual review:** signup form open, admin reviews + approves each tenant manually within 24h
- **Option C — Hybrid:** waitlist signup form + manual batch approve (smaller cohort)

Once flow chosen:
1. Write `documents/05-guides/operations/beta-invitation-flow.md` runbook
2. If invite codes: implement backend `invite_code` field (or use existing tenant flag)
3. Compose invite email template (`kitehub-email` template if missing)
4. Identify first 1-5 beta contacts (real people, opted-in)
5. Prepare day-1 support readiness checklist

## Acceptance Criteria

- [x] Invitation flow documented in `documents/05-guides/operations/beta-invite-flow.md` (5 bước end-to-end: request → review → email → signup → first login — Wave 78 Bucket D)
- [x] Smoke test checklist for pre-release tags shipped (runbook §9)
- [x] Per-step failure modes + recovery actions documented (runbook §3.4 / §4.4 / §5.4 / §6.4 / §7.4)
- [x] Endpoint + role quick-reference table (runbook §8)
- [x] Cross-link với `pre-handoff-self-test-completeness.md` §2.3 + §2.4 (runbook §9)

### Out-of-scope (separate tracking)

- First N beta contacts identified — operational scheduling, not runbook scope
- Invite email template real Vietnamese marketing copy — `kitehub-email` service domain
- Day-1 support response plan — `incident-response-runbook.md` covers
- Communication plan for first production incident — `incident-comms-runbook.md` covers
- First beta tenant successful onboarding live — actual launch event, gated by runbook smoke checklist PASS

## Out-of-scope

- Mass marketing campaign (Phase 1.5+ after beta validates)
- Public signup at scale (Phase 1.5)
- Multi-language onboarding (current focus Vietnamese-first per CLAUDE.md)

## Related

- **Parent:** Release 1 launch (per `release-1-plan-2026.md`)
- **Reference rules:**
  - `release-deploy-standard.md` §3.4 PROD MAJOR
  - `agent-action-bias.md` (CLI-first execution where possible)
- **Reference docs:**
  - `production-seed-runbook.md`
  - `email-ses-setup-runbook.md` §4.1.1
- **Blocks:** Wave 65 "invite first beta tenant" step
- **Blocked by:** Wave 64 Step F (SES production approval 24-48h wait)

## Log

- **2026-05-14 (Wave 78 Bucket D):** Runbook shipped — `documents/05-guides/operations/beta-invite-flow.md` covers 5-bước end-to-end (public request → admin review with `pre-handoff-self-test-completeness.md` §2.4 checklist → approve+email → invitee signup → owner first login). Includes endpoint reference, smoke test checklist for pre-release tags, per-step failure modes + recovery. Operational items (real contacts list, real email copy, day-1 support) moved to Out-of-scope for separate tracking. CSV row flipped OPEN → DONE (completion_pct=100). Status: 🟢 DONE.
- **2026-05-12:** Filed Wave 64 deploy simulation. Critical missing process artifact preventing actual invite execution despite all infra ready.
