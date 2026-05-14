# Reset password email — content audit

**Template:** ❌ KHÔNG TỒN TẠI tại `kitehub/kitehub-email/src/main/resources/templates/emails/`
**Audited:** 2026-05-14
**Auditor:** Wave 78 Bucket E (GAP-543)
**Verdict:** ❌ TEMPLATE MISSING — P1 blocker cho password recovery flow

---

## State check

```bash
ls kitehub/kitehub-email/src/main/resources/templates/emails/ | grep -iE "reset|password|forgot"
# → 0 results
```

Template `reset-password.html` (hoặc tương đương) **KHÔNG có** trong codebase. Email gần nhất liên quan = `email-verification.html` (verify email post-signup, không phải reset password).

---

## Impact on Phase 1 BETA flow

Reset password flow là P1 BLOCKING cho Phase 1 BETA: nếu beta tenant Owner quên password (rất khả thi vì onboarding mới), không có path recovery qua email → tenant locked out → support@kitehub.me phải reset manually qua DB → friction lớn + không scalable.

Phase 1 BETA = 10 tenants, manual reset OK trong scope giới hạn. Nhưng:
- ❌ Vi phạm `pre-handoff-self-test-completeness.md` §2.10 (time-sensitive flow — token TTL).
- ❌ Vi phạm `pre-launch-auth-hardening-checklist.md` (password reset flow là standard auth requirement).
- ⚠️ Phase 1.5+ (paid centers) phải có — file P0 cho Wave 79.

---

## 7-dimension scoring (deferred)

N/A — template không tồn tại. Khi template được tạo trong follow-up:

| # | Dimension | Required state |
|---|-----------|----------------|
| 1 | Tone | Calm + reassuring ("Bạn vừa yêu cầu đặt lại mật khẩu...") — không panic-inducing |
| 2 | Subject | `Đặt lại mật khẩu KiteHub` (≤50 chars, no PII) |
| 3 | Body | Reset link + expiry warning (15 min TTL) + "Nếu không phải bạn yêu cầu, vui lòng bỏ qua" |
| 4 | CTA | "Đặt lại mật khẩu" — Vietnamese, single CTA |
| 5 | Footer | `support@kitehub.me` + /beta-status + IP/timestamp request (security audit trail) |
| 6 | HTML render | Cross-client test critical (Outlook + Gmail) |
| 7 | Plain-text fallback | **Mandatory** — reset link must be readable text-only |

---

## Follow-up gap

**GAP-543.1.A (P1 → Wave 79):** Tạo `reset-password.html` + `.txt` với 7-dimension audit completed pre-launch. Block Phase 1.5 paid centers nếu chưa ship.

---

## Related

- Parent: GAP-543
- Rule: `pre-handoff-self-test-completeness.md` §2.10 (time-sensitive TTL)
- Rule: `pre-launch-auth-hardening-checklist.md`
- Existing template (closest scope): `email-verification.html` — verify post-signup, KHÔNG cover reset
