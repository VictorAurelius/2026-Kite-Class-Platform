# GAP-480: Beta invitation flow undefined — how does first end-user get invited?

**Status:** 🔵 OPEN
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

- [ ] Invitation flow option chosen + documented in `beta-invitation-flow.md`
- [ ] First N beta contacts identified (with explicit opt-in)
- [ ] Invite email template ready (subject + body + onboarding link)
- [ ] Day-1 support response plan documented
- [ ] Communication plan for first production incident documented
- [ ] First beta tenant successfully onboarded end-to-end (signup → verify → login → use)

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

- **2026-05-12:** Filed Wave 64 deploy simulation. Critical missing process artifact preventing actual invite execution despite all infra ready.
