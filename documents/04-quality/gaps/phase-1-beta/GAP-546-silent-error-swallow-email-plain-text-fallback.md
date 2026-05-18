# GAP-546: Silent error swallow trong FE catch blocks + email plain-text fallback (RFC 2049)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / Email
**Found:** 2026-05-14 (ui-review /128 post-Wave-78 audit, Bucket F + Bucket E review)
**Affects:**
- FE debugging: production issues khó trace khi widget hứng non-JSON error response
- Email accessibility: 5 email templates (welcome / approve-tenant / reset-password / beta-invite / day-7-survey) không có `.txt` plain-text fallback → email client text-mode (some Outlook config, screen readers) render HTML raw, violate RFC 2049 best practice

## Problem

Hai issue nhỏ surfaced trong post-Wave-78 audit, gộp 1 gap P2 vì cùng nature (defensive coding + cross-client compat):

### Issue A — Silent JSON parse error swallow

`kitehub/kitehub-frontend/src/components/feedback-widget/FeedbackWidget.tsx:120-124`:

```tsx
try {
  const body = (await res.json()) as { message?: string };
  if (body?.message) msg = body.message;
} catch {
  // ignore JSON parse error
}
```

Khi BE trả non-JSON 500 (e.g., Nginx error page, gateway timeout HTML), parse fail → silent. User chỉ thấy "Gửi thất bại (HTTP 500)" — nhưng production debug không có trace.

### Issue B — 5 email templates thiếu plain-text fallback

Per Bucket E audit notes (`documents/01-business/kitehub/email/templates/welcome-audit.md` §"Plain-text fallback" = 0/10):

- Vi phạm RFC 2049 ("Multipart Internet Mail Extensions" best practice — `multipart/alternative` với text/plain + text/html)
- Email client text-mode renders HTML raw (Outlook plain-mode config, lynx, w3m, accessibility tools)
- Screen reader compatibility partial

Tracked trong GAP-543 PARTIAL với Status `Wave 78 Bucket E: 5 audit notes shipped (2 PASS + 3 missing tracked); content rewrite + plain-text defer Wave 79`. Re-flag riêng vì plain-text fallback là cross-cutting RFC compliance, không phải content rewrite.

## Root Cause

- Issue A: defensive try/catch shipped đúng nhưng quên emit debug log
- Issue B: Thymeleaf template structure dev focus HTML-only; plain-text generation cần parallel `.txt` file hoặc auto-strip-HTML script

## Proposed Fix

### Issue A

```tsx
} catch (parseErr) {
  // Body wasn't JSON — log for production debugging, fallback message stays.
  if (typeof console !== 'undefined') {
    console.debug('[FeedbackWidget] non-JSON error response', { status: res.status, parseErr });
  }
}
```

### Issue B

Path A — Manual `.txt` companion (1 per template, ~5 files):

```
kitehub/kitehub-email/src/main/resources/templates/emails/welcome.html
kitehub/kitehub-email/src/main/resources/templates/emails/welcome.txt   ← NEW
```

Sender code wraps both as `multipart/alternative` (Spring `MimeMessageHelper.setText(text, html)` API).

Path B — Auto-strip script:

```bash
# scripts/email/generate-plain-text-templates.sh
for html in kitehub/kitehub-email/src/main/resources/templates/emails/*.html; do
  txt="${html%.html}.txt"
  pandoc "$html" -t plain -o "$txt"
done
```

Path A preferred — fine-tune VN tone manually (auto-strip drop emoji + format).

## Acceptance Criteria

- [ ] FeedbackWidget catch block emit `console.debug` (not silent)
- [ ] 5 `.txt` companion templates created (welcome / approve-tenant / reset-password / beta-invite / day-7-survey)
- [ ] `MimeMessageHelper.setText(plainText, html)` wired trong sender service
- [ ] Manual test: send to Outlook plain-mode config — text-only version renders correctly
- [ ] Manual test: lynx / w3m render `.txt` body cleanly (no HTML tags leak)

## Related

- Audit: [`documents/04-quality/audits/ui/2026-05-14-post-wave-78.md`](../audits/ui/2026-05-14-post-wave-78.md) §4 P2-A finding
- Parent: GAP-543 (email content audit umbrella; content rewrite stays Wave 79)
- Wave 78 Bucket F shipped widget (GAP-542)
- RFC 2049 §2 — `multipart/alternative` plain+html best practice

## Log

- **2026-05-14:** Filed from Wave 78 post-wave ui-review audit. Issue A surfaced reading FeedbackWidget.tsx line 122; Issue B from Bucket E welcome-audit.md §7 score 0/10. Gộp 1 gap vì cùng "cross-client defensive" nature. P2 vì không block beta launch — silent error swallow tolerable ngắn hạn; plain-text fallback impacts ~5% email recipients (Outlook plain-mode + accessibility tools). Defer Wave 79+ batch.
