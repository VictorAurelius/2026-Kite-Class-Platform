# GAP-586: Beta invite email template content audit — tone + sender + feedback CTA (P1 Solo Teacher)

**Status:** 🔵 OPEN
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

- [ ] Email template audit checklist documented
- [ ] Resend template applied checklist; rendering verified với 2 sample P1 personas
- [ ] Mail-Tester score ≥ 8/10
- [ ] Wave 86 Bucket G first 2 P1 invites use this template
- [ ] Post-send dashboard verify: open rate >70%, no spam complaints

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.2 cell 2.2 + §4 rank 2 + §5 Bucket G AC + §6 NEW gap proposal #2
- Wave 86 plan §3 Bucket G AC G-AC5 (paired)
- GAP-587 P3 invite email (sibling, P3 cohort scope)
