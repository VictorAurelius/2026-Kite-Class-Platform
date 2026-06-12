# Beta invite email — content audit

**Template:** `kitehub/kitehub-email/src/main/resources/templates/emails/beta-invite.html`
**Audited:** 2026-05-14
**Auditor:** Wave 78 Bucket E (GAP-543)
**Verdict:** ✅ PARTIAL PASS (content excellent; plain-text fallback thiếu; cross-client render chưa test)

---

## 7-dimension scoring

| # | Dimension | Score | Findings |
|---|-----------|-------|----------|
| 1 | Tone | **9/10** | "Bạn được mời tham gia beta ${...}!" — friendly + excited. Beta badge styled chuyên nghiệp. Disclaimer "v1 pending counsel review" rõ ràng (per `business-logic-review.md`). Phù hợp entry point Phase 1 BETA. |
| 2 | Subject line | **9/10** | Title `\|Lời mời beta - ${branding?.displayName ?: 'KiteClass'}\|`. ≤50 chars (sau brand thay). Cụ thể về beta. Tự nhiên Vietnamese. |
| 3 | Body content | **10/10** | Structure rõ: greeting → context → 3-step instruction → claim code → expiry → disclaimer. Vietnamese natural. Disclaimer color-coded (yellow `#FEF3C7`) thu hút mắt. |
| 4 | CTA button + claim code | **10/10** | "Mở trang đăng ký" — Vietnamese. URL `${inviteUrl}`. Claim code 6-digit format chuyên nghiệp (font monospace, letter-spacing 6px). Per GAP-388 Wave 36 — replaces raw UUID leak in href (security improvement). |
| 5 | Footer | **⚠️ 5/10** | `support@kitehub.me` default. Thiếu `/beta-status` link (GAP-539). Cần switch default → `support@kitehub.me` (GAP-540). |
| 6 | HTML render | **⚠️ chưa test** | CSS với CSS custom properties (`--brand-primary`). Outlook desktop KHÔNG support CSS custom properties → fallback color quan trọng. Cần test Litmus. |
| 7 | Plain-text fallback | **❌ 0/10** | KHÔNG có `beta-invite.txt`. Đặc biệt critical vì email beta-invite cần delivery rate cao + lọc spam filter ưu tiên `.txt` paired `.html`. |

---

## Subject line PII check

- ❌ Subject KHÔNG chứa user name / email / tenant ID.
- ✅ Chỉ có brand displayName (tenant brand, không phải user PII).
- Verdict: **0 PII leak in subject**.

---

## Vietnamese tone correctness (per `dev-readable-doc-language.md` §4)

- ✅ Narrative Vietnamese xuyên suốt.
- ✅ Technical token natural inline: "Đây là phiên bản beta — một số tính năng có thể thay đổi, dữ liệu beta có thể được reset trước khi ra mắt chính thức."
- ✅ English identifier giữ nguyên: "Beta Program" badge (brand-style label, không dịch).
- Verdict: tone-perfect cho Phase 1 BETA Vietnamese-speaking tenants.

---

## Security note (claim code)

Per GAP-388 Wave 36 Bucket A 388-B: template dùng 6-digit `${claimCode}` thay raw UUID trong href. Email forwarding/screenshot không expose usable token một mình — attacker cần cả code surface. **Tốt — không cần đổi.**

---

## Recommendations (ship Wave 79+)

1. **Footer add `/beta-status` link** (sync GAP-539).
2. **Footer switch default → `support@kitehub.me`** (sync GAP-540).
3. **Plain-text fallback** (RFC 2049): tạo `beta-invite.txt` với:
   - Greeting + brand
   - 3-step instruction (text-only)
   - Invite URL (raw)
   - Claim code (text)
   - Expiry date
   - Disclaimer
   - Footer
4. **Cross-client render test:** Litmus với Gmail / Outlook desktop (CSS custom property fallback critical) / Outlook web / Apple Mail iOS.

---

## Related

- Parent: GAP-543
- Sync: GAP-539, GAP-540, GAP-527, GAP-388 (claim code security)
- Rule: `dev-readable-doc-language.md` §4, `business-logic-review.md` (disclaimer phrasing)
