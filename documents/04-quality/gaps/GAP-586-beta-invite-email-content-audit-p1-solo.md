# GAP-586: Beta invite email template content audit — tone + sender + feedback CTA (P1 Solo Teacher)

**Status:** 🟡 PARTIAL (70%) — Wave 86 docs-cluster audit complete + 3 template fixes applied (human sender block + reply-friendly footer + status/help link footers). Audit verdict: 1/5 PASS, 2/5 PARTIAL, 2/5 FAIL on original 5-criterion checklist before fixes. Post-fix verdict: 4/5 PASS, 1/5 FAIL (Criterion 5 `/help/p1-solo-teacher` page pending Wave 80+ Bucket F2; interim `/help/anonymous` link used). Mail-Tester live verify deferred Wave 86 Bucket G send.
**Priority:** 🟠 P1
**Domain:** Frontend / Email / Content
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A persona-outside-in audit cell 2.2)
**Affects:** Resend invite email template for P1 Solo Teacher cohort (2 tenants Wave 86)

## Problem

Persona audit cell 2.2 (chị Hồng — P1 Solo Teacher): Email là first touchpoint cho beta cohort. Wave 86 Bucket G hiện tại có Resend production verified nhưng KHÔNG audit invite email **content**:
- Sender có thể default `noreply@kitehub.me` thay vì `support@kitehub.me` → trust loss
- Tone có thể English template-y thay vì Vietnamese natural → cultural mismatch
- Thiếu tên thật người duyệt (e.g., "chị Mai từ KiteHub") → cảm thấy spam
- Thiếu feedback CTA explicit ("Beta — phản hồi qua ...") → mất signal channel

Impact: 30-40% beta cohort conversion rate damage (industry benchmark first-impression email).

## Root Cause

Wave 83 Bucket F Resend production verified focused on **delivery infrastructure**, không cover template content quality.

## Proposed Fix

1. **Email template audit checklist** trong `documents/05-guides/email-templates/beta-invite-p1-solo.md`:
   - Sender `support@kitehub.me` (human-facing, NOT `noreply@`)
   - Subject line Vietnamese natural: "Chào chị Hồng — KiteHub mời chị tham gia Beta đầu tiên"
   - Body sender identity: "Chị Mai từ KiteHub đây" (human name explicit)
   - Body tone: Vietnamese conversational, không formal English-translated
   - Beta disclaimer: "Phiên bản Beta — chị là 1 trong 5 trung tâm đầu tiên thử nghiệm"
   - Feedback CTA visible: "Có vấn đề gì → reply email này hoặc Zalo (sắp có)"
   - TOS + Privacy + Beta disclaimer links visible footer
   - Magic link button rõ ràng, không bị spam filter (avoid all-caps, exclamation marks)
2. **Resend template** `resend/templates/beta-invite-p1-solo.html`:
   - Apply checklist
   - Test render với sample data "chị Hồng, Trung tâm Anh ngữ Sky Education"
3. **Self-test send**: Resend test send to internal QA email → verify rendering + spam score (Mail-Tester.com ≥ 8/10)

## Acceptance Criteria

- [x] Email template audit checklist documented — `documents/04-quality/audits/email-template/2026-05-16-gap-586-p1-solo-teacher-invite.md` ships 5-criterion audit verdict + per-criterion analysis + 3 recommended fixes
- [x] Resend template applied checklist edits — 3 fixes applied to `kitehub/kitehub-email/src/main/resources/templates/emails/beta-invite.html` (human sender block + reply-friendly footer + status/help footer links)
- [ ] Mail-Tester score ≥ 8/10 — defer Wave 86 Bucket G live send verification (requires Resend production env + actual send)
- [ ] Wave 86 Bucket G first 2 P1 invites use this template — execution scope Wave 86 Bucket G (this gap = template audit; Bucket G = send mechanism)
- [ ] Post-send dashboard verify: open rate >70%, no spam complaints — execution scope Wave 86 Bucket G post-send

## Log

- **2026-05-16** Wave 86 docs-cluster — audit shipped + 3 template fixes applied. Status flipped OPEN → PARTIAL (70%). Per `gap-done-discipline.md` §3 PARTIAL exit ramp: 2 ACs verified (audit doc + template edits); 3 ACs (Mail-Tester score + Bucket G first-send + post-send dashboard) deferred to Wave 86 Bucket G live execution scope. Verification artifact: `documents/04-quality/audits/email-template/2026-05-16-gap-586-p1-solo-teacher-invite.md`. Follow-up: file `GAP-586b template-criterion-5-p1-page-link` Phase 1.5+ if P1-specific `/help/p1-solo-teacher` page ship requires template link backfill.

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.2 cell 2.2 + §4 rank 2 + §5 Bucket G AC + §6 NEW gap proposal #2
- Wave 86 plan §3 Bucket G AC G-AC5 (paired)
- GAP-587 P3 invite email (sibling, P3 cohort scope)
