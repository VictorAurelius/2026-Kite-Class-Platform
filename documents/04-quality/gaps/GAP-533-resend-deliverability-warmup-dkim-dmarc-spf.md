# GAP-533: Resend deliverability warm-up — DKIM/DMARC/SPF + IP warm + spam-score baseline

**Status:** 🟡 PARTIAL 80% (Wave 77 Bucket A code-side foundation DONE; apply + warm-up + spam-score gate = user-action follow-on)
**Priority:** 🔴 P0 — BLOCKING Phase 1 BETA invite (downstream of GAP-370 email infra)
**Domain:** DevOps
**Found:** 2026-05-14 (Wave 77 — outside-in audit: persona + benchmark convergence)
**Affects:** All beta personas (P1 Solo teacher + P2 Center owner) — Phase 1 BETA invite email must reach Gmail/Outlook VN inbox, not spam
**Phase:** phase-1-beta

## Current State (verified 2026-05-14 — refreshed Wave 77 Bucket A)

| Piece | File / Path | Status |
|-------|-------------|--------|
| GAP-370 Resend infra | `documents/04-quality/gaps/GAP-370-email-transactional-infrastructure.md` | 🟡 PARTIAL 95% (Wave 77 Bucket A code-side updated) — AWS SES denied; Resend pivot via terraform-cloudflare + runbooks shipped |
| Cloudflare DNS terraform | `infrastructure/terraform-cloudflare/dns.tf` | ✅ codified (Wave 77 Bucket A — SPF + DKIM CNAME x 3 + DMARC; DKIM CNAME values placeholder until operator fetches from Resend dashboard) |
| Email deliverability runbook | `documents/05-guides/deploy/email-deliverability-runbook.md` | ✅ shipped (Wave 77 Bucket A) |
| Spam-score smoke script | `scripts/verify-email-deliverability.sh` | ✅ shipped (Wave 77 Bucket A; falls back to manual procedure if `MAIL_TESTER_API_KEY` absent) |
| Resend runtime smoke script | `scripts/smoke-resend.sh` | ✅ shipped (Wave 77 Bucket A; verifies API key + domain status + optional 1-email send) |
| Warm-up schedule docs | `email-deliverability-runbook.md` §3 | ✅ codified 7-day ramp 5→20 emails/day |

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

Code-side (Wave 77 Bucket A — DONE this PR):

- [x] SPF + DKIM + DMARC records codified in Cloudflare terraform (`infrastructure/terraform-cloudflare/dns.tf`)
- [x] Runbook ships at `documents/05-guides/deploy/email-deliverability-runbook.md` per `docs-folder-structure.md`
- [x] Smoke script `scripts/verify-email-deliverability.sh` runnable (manual fallback path when API key absent)
- [x] Smoke script `scripts/smoke-resend.sh` runnable (verifies API key + domain status + optional send)
- [x] Warm-up schedule codified (`email-deliverability-runbook.md` §3 — 7-day ramp 5→20/day)

User-action follow-on (post-merge — operator):

- [ ] Resend dashboard add domain `kitehub.me` + capture 3 DKIM CNAME thật + update `terraform.tfvars`
- [ ] `terraform apply` records → DNS propagated (verified via `dig`) → Resend status Verified
- [ ] Spam-score ≥8/10 on mail-tester.com (3 consecutive runs Day 5-7)
- [ ] Warm-up first 5 days executed (~75 emails total)
- [ ] No bounce rate >0.5% / complaint rate >0.1% per Resend dashboard during warm-up

## Related

- **Parent:** GAP-370 (email infra — this gap is downstream extension)
- **Sibling Wave 77 outside-in:** GAP-534 (invite token single-use), GAP-535 (slug normalize), GAP-536 (idempotency)
- **Downstream:** GAP-530 (email flow end-to-end live verify — runs after deliverability green)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-14-77-beta-invite-launch-foundation.md`
- **Outside-in audit source:** Wave 77 persona agent + benchmark agent convergence (2026-05-14)

## Log

- **2026-05-14** (Wave 77 Bucket A code-side): Status 🔵 OPEN → 🟡 PARTIAL 80%. Shipped:
  - `infrastructure/terraform-cloudflare/{providers.tf,variables.tf,dns.tf,README.md,terraform.tfvars.example,.gitignore}` — codify SPF + DKIM CNAME x 3 + DMARC; operator replaces placeholders + runs `terraform apply` per runbook §2.1
  - `documents/05-guides/deploy/email-deliverability-runbook.md` — DNS sequence + warm-up 7-day schedule + spam-score gate + troubleshooting matrix
  - `scripts/verify-email-deliverability.sh` — automated mail-tester.com smoke; falls back to manual procedure when `MAIL_TESTER_API_KEY` absent
  - `scripts/smoke-resend.sh` — Resend API runtime health check (read-only `--send` optional)
  - PARTIAL because user-action (apply, warm-up, score baseline) is post-merge. No banned phrases per `gap-done-discipline.md` §2 — explicit follow-on tracked in AC.
- **2026-05-14** — Initial write-up. Wave 77 outside-in audit convergence (persona + benchmark both surface deliverability gap independent of GAP-370 infra scope). Stub created in wave plan PR; full execution → Bucket A.
