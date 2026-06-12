# Welcome email — content audit

**Template:** `kitehub/kitehub-email/src/main/resources/templates/emails/welcome.html`
**Audited:** 2026-05-14
**Auditor:** Wave 78 Bucket E (GAP-543)
**Verdict:** ✅ PARTIAL PASS (content tone OK; plain-text fallback thiếu)

---

## 7-dimension scoring

| # | Dimension | Score | Findings |
|---|-----------|-------|----------|
| 1 | Tone | **8/10** | Friendly + welcoming ("Chào Mừng Đến Với ... 🚀"). Phù hợp first impression post-onboarding step 1. Emoji 🚀 ổn cho tone Phase 1 BETA. |
| 2 | Subject line | **⚠️ 6/10** | Subject hiện tại = `\|Chào mừng đến với ${branding?.displayName ?: 'KiteClass'}\|`. Thiếu cụ thể về "tài khoản nào / dùng làm gì". Đề xuất: `Chào mừng — tài khoản KiteHub của bạn đã sẵn sàng`. ≤50 chars OK. |
| 3 | Body content | **9/10** | Vietnamese tone tự nhiên. Liệt kê 3 thông tin tài khoản (Tổ chức / Gói dùng thử / Hết hạn) rõ ràng. Cảm ơn + invite login. Không có machine-translation awkwardness. |
| 4 | CTA button | **9/10** | "Đăng Nhập Ngay" — Vietnamese, action-oriented. URL = `${loginUrl}` (Thymeleaf placeholder OK). |
| 5 | Footer | **⚠️ 5/10** | Có `support@kitehub.me` (default). Thiếu `/beta-status` link (GAP-539 sync). Thiếu mention `support@kitehub.me` (GAP-540 sync). Copyright OK. |
| 6 | HTML render | **⚠️ chưa test** | CSS inline trong `<style>` block — không guarantee cross-client. Gmail strip `<style>` ngoài head trong một số mode. Cần test Litmus / Email-on-Acid. |
| 7 | Plain-text fallback | **❌ 0/10** | KHÔNG có `welcome.txt`. Email client text-mode (some Outlook config, screen readers) render HTML raw. Vi phạm RFC 2049 best practice. |

---

## Subject line PII check

- ❌ Subject KHÔNG chứa user name / email / phone / org name (good).
- ✅ Variable `${branding?.displayName}` chỉ render tenant brand (vd "Trường THCS A") — không phải user PII.
- Verdict: **0 PII leak in subject**.

---

## Vietnamese tone correctness (per `dev-readable-doc-language.md` §4)

- ✅ Narrative Vietnamese: "Tài khoản của bạn đã được tạo thành công và bạn có thể bắt đầu sử dụng ngay."
- ✅ English technical token natural code-switch: không có (template thuần Việt).
- ✅ Brand placeholder English-friendly: `${branding?.displayName ?: 'KiteClass'}` — fallback English brand name OK cho international users (rare Phase 1 BETA, nhưng safe).
- Verdict: tone đúng chuẩn Phase 1 BETA Vietnamese-first.

---

## Recommendations (ship Wave 79+)

1. **Subject upgrade:** `Chào mừng — tài khoản KiteHub của bạn đã sẵn sàng` (≤50 chars, cụ thể hơn).
2. **Footer add `/beta-status` link** (sync GAP-539): `<p><a th:href="@{/beta-status}">Xem trạng thái beta</a></p>`.
3. **Footer add support@kitehub.me** (sync GAP-540): switch default email từ `support@kitehub.me` → `support@kitehub.me`.
4. **Plain-text fallback** (RFC 2049): tạo `welcome.txt` chứa cùng content text-only.
5. **Cross-client render test:** chạy Litmus / Email-on-Acid với Gmail web + Outlook web (preview screenshot lưu `documents/04-quality/audits/email-render/2026-XX-XX-welcome-render.md`).

---

## Related

- Parent: GAP-543
- Sync: GAP-539 (/beta-status link), GAP-540 (support@kitehub.me), GAP-527 (E2E send smoke)
- Rule: `dev-readable-doc-language.md` §4, `output-review-mandate.md` §3 row Email templates
