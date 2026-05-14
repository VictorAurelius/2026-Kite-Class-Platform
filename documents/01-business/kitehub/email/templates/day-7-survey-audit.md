# Day-7 survey email — content audit

**Template:** ❌ KHÔNG TỒN TẠI tại `kitehub/kitehub-email/src/main/resources/templates/emails/`
**Audited:** 2026-05-14
**Auditor:** Wave 78 Bucket E (GAP-543)
**Verdict:** ❌ TEMPLATE MISSING — sync GAP-542 feedback channel

---

## State check

```bash
ls kitehub/kitehub-email/src/main/resources/templates/emails/ | grep -iE "survey|day.7|feedback"
# → 0 results
```

Template `day-7-survey.html` không có trong codebase. Scheduler `kitehub-email` để gửi survey sau ngày 7 onboarding cũng chưa được wire (sync GAP-542 — feedback channel widget + survey).

---

## Impact on Phase 1 BETA RETAIN flow

Day-7 survey = critical retention signal cho Phase 1 BETA:
- Day-0 (signup) → tenant onboarding flow.
- Day-1 → onboarding step 1 (welcome email gửi).
- Day-7 → ✅ **survey email TRIGGER** (NPS + 1 open-ended question).
- Day-14 → trial midpoint email (đã có template `trial-midpoint.html`).
- Day-28 → trial expiration warning (đã có template `trial-expiration-warning.html`).

Wave 78 Bucket E (GAP-543) audit ghi nhận template thiếu; thực thi tạo template thuộc Wave 79 (cùng với GAP-542 scheduler wire).

---

## 7-dimension scoring (deferred — template chưa tạo)

Khi template tạo trong Wave 79, áp dụng rubric:

| # | Dimension | Required state |
|---|-----------|----------------|
| 1 | Tone | Conversational + grateful ("Cảm ơn bạn đã dùng KiteHub 7 ngày — bạn cảm thấy thế nào?") |
| 2 | Subject | `KiteHub — 1 phút phản hồi sau 7 ngày?` (≤50 chars, no PII) |
| 3 | Body | Greeting → context (7 ngày) → NPS 0-10 scale + 1 open-ended → footer |
| 4 | CTA | "Trả lời 1 câu hỏi" — single CTA → survey URL với prefilled tenant token |
| 5 | Footer | `support@kitehub.me` + opt-out link ("không nhận survey nữa") + /beta-status |
| 6 | HTML render | Survey embed (Tally / Typeform) cross-client test |
| 7 | Plain-text fallback | Mandatory với survey URL raw |

---

## NPS question (suggested)

> "Trên thang 0-10, bạn sẵn sàng giới thiệu KiteHub cho đồng nghiệp đến mức nào?"
>
> [0] (rất không) [5] (trung bình) [10] (rất sẵn sàng)
>
> "Vì sao? (tùy chọn, 1-2 câu)"

---

## Follow-up gap

**GAP-543.1.B (P1 → Wave 79 paired với GAP-542):** Tạo `day-7-survey.html` + `.txt` + scheduler wire + audit 7-dimension complete. Block Wave 80 RETAIN measurement nếu chưa ship.

---

## Related

- Parent: GAP-543
- Sibling: GAP-542 (feedback channel widget + survey scheduler)
- Adjacent templates: `trial-midpoint.html` (day-14), `onboarding-tips.html` (day-0..6)
- Rule: `dev-readable-doc-language.md` §4
