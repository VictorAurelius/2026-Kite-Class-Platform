# GAP-533: Resend deliverability warm-up — DKIM/DMARC/SPF + IP warm + spam-score baseline

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — BLOCKING Phase 1 BETA invite (downstream of GAP-370 email infra)
**Domain:** DevOps
**Found:** 2026-05-14 (Wave 77 — outside-in audit: persona + benchmark convergence)
**Affects:** All beta personas (P1 Solo teacher + P2 Center owner) — Phase 1 BETA invite email must reach Gmail/Outlook VN inbox, not spam
**Phase:** phase-1-beta

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| GAP-370 Resend infra | `documents/04-quality/gaps/GAP-370-email-transactional-infrastructure.md` | 🟡 PARTIAL 60% — AWS SES denied; Resend pivot OR Sandbox C1 viable |
| Cloudflare DNS terraform | `infrastructure/terraform-cloudflare/dns.tf` | 🟡 verify-at-spawn — may exist with partial records |
| Email deliverability runbook | `documents/05-guides/deploy/email-deliverability-runbook.md` | ❌ missing |
| Spam-score smoke script | `scripts/verify-email-deliverability.sh` | ❌ missing |
| Warm-up schedule docs | (anywhere) | ❌ missing |

**Grep commands run:**
```bash
grep -E "^GAP-370," documents/04-quality/gaps/gap-status.csv
# → PARTIAL 60% confirmed
find documents/05-guides -iname "*deliverab*" 2>/dev/null
# → no matches
find scripts -iname "*email*" -o -iname "*deliverab*" 2>/dev/null
# → none for deliverability scope
```

## Problem

Wave 77 outside-in audit (2026-05-14) — persona walkthrough + external benchmark — convergent finding: **GAP-370 closes email infra (Resend OR SES sandbox) but does NOT cover deliverability**. Domain `kitehub.me` is new, sender reputation cold. Without DKIM + DMARC + SPF active in Cloudflare DNS + IP warm-up schedule, beta invite emails will land in Gmail/Outlook VN spam folder → beta cohort never sees invite → bounce.

Per persona walkthrough (Cô Hương / P1): "check Gmail trên phone, không thấy email Resend. Tìm trong Spam → thấy". Per Linear/Notion playbook benchmark: domain warm-up over 7 days + spam-score test (mail-tester.com) ≥8/10 = baseline for B2B SaaS invite deliverability.

## Proposed Fix

1. **DNS records** in Cloudflare (terraform-managed):
   - SPF: `v=spf1 include:_spf.resend.com ~all` (or include:amazonses.com for SES path)
   - DKIM: Resend-provided selector OR SES-provided 3 CNAME records
   - DMARC: `v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@kitehub.me; pct=100`
2. **Warm-up schedule** documented + executed:
   - Day 1-2: 5 emails/day to known-good test inboxes (Gmail/Outlook/Yahoo)
   - Day 3-5: 10 emails/day
   - Day 6-7: 20 emails/day
   - Day 8+: scale to invite cohort
3. **Spam-score smoke test** `scripts/verify-email-deliverability.sh`:
   - Send test email to mail-tester.com auto-generated address
   - Fetch report API
   - PASS threshold ≥8/10
4. **Runbook** `documents/05-guides/deploy/email-deliverability-runbook.md`:
   - DNS setup sequence
   - Warm-up day-by-day
   - Spam-score gate criteria
   - Provider dashboard interpretation (Resend / SES bounces, complaints, opens)

## Acceptance Criteria

- [ ] SPF + DKIM + DMARC active in Cloudflare DNS (terraform-applied + propagated)
- [ ] Spam-score ≥8/10 on mail-tester.com (3 consecutive runs)
- [ ] Warm-up schedule documented + first 5 days executed (~75 emails total)
- [ ] Runbook ships at `documents/05-guides/deploy/email-deliverability-runbook.md` per `docs-folder-structure.md`
- [ ] Smoke script `scripts/verify-email-deliverability.sh` runnable + green
- [ ] No bounce/complaint rate >0.1% / >0.05% per provider dashboard during warm-up

## Related

- **Parent:** GAP-370 (email infra — this gap is downstream extension)
- **Sibling Wave 77 outside-in:** GAP-534 (invite token single-use), GAP-535 (slug normalize), GAP-536 (idempotency)
- **Downstream:** GAP-530 (email flow end-to-end live verify — runs after deliverability green)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-14-77-beta-invite-launch-foundation.md`
- **Outside-in audit source:** Wave 77 persona agent + benchmark agent convergence (2026-05-14)

## Log

- **2026-05-14** — Initial write-up. Wave 77 outside-in audit convergence (persona + benchmark both surface deliverability gap independent of GAP-370 infra scope). Stub created in wave plan PR; full execution → Bucket A.
