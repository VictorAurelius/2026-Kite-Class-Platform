# UI Review — KiteClass Frontend

**Ngày:** 2026-04-03
**Phiên bản:** main @ `3503c024` (sau PR #252)
**Phương pháp:** Screenshot thực tế (Playwright) — 36 PNGs, 9 trang × 2 themes × 2 viewports
**Screenshots:** `documents/screenshots/audit-2026-04-03/`
**Skill:** `.claude/skills/quality/ui-review/SKILL.md`
**Next review:** Sau khi fix dark mode + i18n auth pages

---

## Tổng quan Issues

| Priority | Issue | Pages affected |
|----------|-------|----------------|
| 🔴 P0 | **Dark mode không hoạt động** — light/dark screenshots giống hệt nhau | Tất cả (landing, about, catalog, contact) |
| 🔴 P1 | **i18n gap auth pages** — toàn bộ text English | login, forgot-password, reset-password, auth sidebar |
| 🟡 P2 | **Catalog loading spinner vô hạn** — không có backend → spinner mãi không dừng | catalog |
| 🟡 P2 | **Date input format** `mm/dd/yyyy` — English locale, VN users expect `dd/mm/yyyy` | register/student |
| 🟡 P3 | **Placeholder data** — "1900 xxxx", empty sections (Đội ngũ, Chứng chỉ, Bảng giá) | landing |
| 🟢 P4 | **Floating emoji icon** — bottom-right corner, browser extension artifact | tất cả |

---

## Scores per Screen (/128)

> Dựa trên screenshots thực tế. Rubric: 2/4 = "có feature", 3/4 = "tốt nhất quán mọi screen", 4/4 = "genuinely excellent".

### Dimensions
- **Technical /20** — responsive, theming, anti-patterns
- **Design Heuristics /40** — Nielsen's 10 heuristics (0–4 mỗi cái)
- **Visual Aesthetics /28** — màu, typography, spacing, hierarchy, polish
- **User Friendliness /20** — first impression, navigation, action clarity
- **WCAG /20** — contrast, touch targets, labels, keyboard

---

### Landing `/`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 13/20 | Dark mode broken (-4): light=dark hoàn toàn. SSR ✓, responsive ✓, fallback data ✓ |
| Design Heuristics | 20/40 | Visibility 2, Real world 3, Control 2, Consistency 2, Error prev 2, Recognition 2, Flex 1, Aesthetic 2, Error rec 2, Help 2 |
| Visual Aesthetics | 18/28 | Brand blue nhất quán ✓, typography hierarchy ✓, nhưng nhiều sections rỗng (Đội ngũ/Chứng chỉ/Bảng giá chỉ có heading) |
| User Friendliness | 13/20 | Ấn tượng tốt: Vietnamese, hero CTA rõ. Nhưng empty sections phá trust |
| WCAG | 12/20 | Contrast OK (blue/white). Screen reader/keyboard chưa verify |
| **Total** | **76/128** | |

**Issues:**
- ❌ Dark mode không hoạt động — `kiteclass_theme` localStorage key riêng biệt, cần verify setup
- ⚠️ Empty sections: "Đội ngũ giáo viên", "Chứng chỉ", "Tuyển sinh", "Bảng giá" chỉ có heading không có data
- ⚠️ Fallback placeholder: "1900 xxxx", "support@kiteclass.com" vẫn là mẫu

---

### Login `/login`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 12/20 | Dark mode broken (-4). Responsive ✓ (mobile ẩn sidebar). Validation ✓ (Zod) |
| Design Heuristics | 26/40 | Split layout ✓, error states ✓, forgot pwd ✓, remember me ✓, nhưng English text cho VN users |
| Visual Aesthetics | 20/28 | Clean split layout: blue sidebar + white form. Good spacing. Whitespace trên form hơi nhiều |
| User Friendliness | 12/20 | Flow rõ ràng ✓, nhưng **toàn bộ text English**: "Welcome back", "Sign in", "Forgot password?" |
| WCAG | 12/20 | Input labels ✓, button contrast ✓, ARIA live region errors chưa có |
| **Total** | **82/128** | |

**Issues:**
- ❌ 100% English: "Welcome back", "Sign in to your account to continue", "Email", "Password", "Remember me", "Forgot password?", "Sign in", "Don't have an account? Sign up"
- ❌ Dark mode không hoạt động
- Mobile: sidebar ẩn đúng, nhưng mất branding context

---

### Register `/register`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 12/20 | Dark mode broken (-4). Responsive ✓ |
| Design Heuristics | 26/40 | Account type selection UX tốt ✓. "Trung tâm" disabled = Coming soon rõ ràng ✓ |
| Visual Aesthetics | 20/28 | Card selection layout đẹp. 2 cards: Học viên (active) + Trung tâm (disabled/muted) |
| User Friendliness | 14/20 | Form area Vietnamese ✓ ("Tạo tài khoản"). Sidebar English ⚠️ |
| WCAG | 12/20 | |
| **Total** | **84/128** | |

**Issues:**
- ⚠️ Auth sidebar (left panel) text vẫn English: "Manage your education center with ease"
- ✓ Form area Vietnamese: "Tạo tài khoản", "Chọn loại tài khoản", "Đăng ký học viên"

---

### Register Student `/register/student`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 12/20 | Dark mode broken (-4). Required field markers ✓ |
| Design Heuristics | 26/40 | Full form với all fields. Password hint rõ ràng (tiếng Việt) ✓ |
| Visual Aesthetics | 19/28 | Long form, overflow scroll. Red * required markers rõ. Date input browser-native style inconsistent |
| User Friendliness | 14/20 | Toàn Vietnamese ✓. Nhưng date picker: `mm/dd/yyyy` format (English locale) |
| WCAG | 13/20 | Required markers visible ✓, labels rõ ✓ |
| **Total** | **84/128** | |

**Issues:**
- ⚠️ Date input hiển thị `mm/dd/yyyy` — cần `dd/mm/yyyy` cho người dùng Việt Nam
- ✓ Password hint: "Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt" (Vietnamese, đầy đủ)
- ✓ Confirm password field có

---

### Forgot Password `/forgot-password`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 12/20 | Dark mode broken (-4). Simple page, ít risk |
| Design Heuristics | 22/40 | Simple ✓, back to login ✓. Nhưng rất minimal — flexibility 1/4, help 1/4 |
| Visual Aesthetics | 18/28 | Very clean but sparse. Large empty whitespace. Split layout consistent |
| User Friendliness | 12/20 | English text toàn bộ: "Forgot password?", "No worries, we'll send you reset instructions" |
| WCAG | 13/20 | Simple form, few elements — easier to get right |
| **Total** | **77/128** | |

**Issues:**
- ❌ 100% English: "Forgot password?", "No worries, we'll send you reset instructions.", "Send reset instructions", "Back to login"

---

### Reset Password `/reset-password`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 14/20 | Error state handled correctly ✓ — "Invalid reset link" khi không có token |
| Design Heuristics | 24/40 | Error message rõ ✓, action button ✓ ("Request new link") |
| Visual Aesthetics | 18/28 | Clean error state. Red error box nổi bật ✓ |
| User Friendliness | 13/20 | Error clear, action clear. Nhưng English |
| WCAG | 12/20 | |
| **Total** | **81/128** | |

**Issues:**
- ❌ English: "Invalid reset link", "This password reset link is invalid or has expired.", "Please request a new password reset link.", "Request new link"
- ✓ Error handling correct — hiển thị state phù hợp khi không có valid token

---

### About `/about`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 13/20 | Dark mode broken (-4). Rich content page |
| Design Heuristics | 24/40 | Good storytelling, stats section, timeline ✓. Không có interactivity |
| Visual Aesthetics | 20/28 | Well structured sections: mission/vision, stats, values, why us, timeline, CTA. Color usage good |
| User Friendliness | 15/20 | Toàn Vietnamese ✓. Timeline rõ ràng. CTA "Dùng thử miễn phí" prominent |
| WCAG | 12/20 | |
| **Total** | **84/128** | |

**Notes:**
- ✓ Tốt nhất về content quality — hoàn toàn Vietnamese, rich storytelling
- Stats: 100+ trung tâm, 10,000+ học viên — likely placeholder

---

### Catalog `/catalog`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 12/20 | Dark mode broken (-4). Loading spinner không resolve (no backend) |
| Design Heuristics | 20/40 | Loading state visible ✓, nhưng spinner vô hạn = bad visibility. Empty state section ✓ |
| Visual Aesthetics | 17/28 | Top half = search/filter bar (clean). Middle = spinner only. Bottom = empty state CTA |
| User Friendliness | 11/20 | Spinner chạy mãi = poor UX. Empty state + CTA bên dưới cứu vãn được |
| WCAG | 12/20 | |
| **Total** | **72/128** | **LOWEST SCREEN** |

**Issues:**
- ❌ Loading spinner không dừng — cần timeout + empty state khi API fail (như landing page đã làm)
- ✓ Empty state section "Không tìm thấy khóa học phù hợp?" với CTA buttons
- ✓ Search bar + filter dropdowns UI sạch

---

### Contact `/contact`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 13/20 | Dark mode broken (-4). Form + info cards |
| Design Heuristics | 26/40 | Two-column layout ✓, contact info rõ ✓, form labels ✓ |
| Visual Aesthetics | 20/28 | Clean professional: form card (left) + info cards (right). Blue CTA button |
| User Friendliness | 14/20 | Toàn Vietnamese ✓. Thông tin liên hệ visible ngay (email, hotline, địa chỉ) |
| WCAG | 13/20 | Required field markers ✓, labels ✓ |
| **Total** | **86/128** | **HIGHEST SCREEN** |

**Issues:**
- ⚠️ Placeholder: "1900 xxxx", "Hà Nội, Việt Nam" (generic)

---

## Tổng kết

| Screen | Score | Lowest dim |
|--------|-------|-----------|
| Contact | 86/128 (67%) | |
| Login | 82/128 (64%) | |
| Reset Password | 81/128 (63%) | |
| About | 84/128 (66%) | |
| Register | 84/128 (66%) | |
| Register Student | 84/128 (66%) | |
| Landing | 76/128 (59%) | Design Heuristics 20/40 |
| Forgot Password | 77/128 (60%) | |
| **Catalog** | **72/128 (56%)** | **Quality bar — LOWEST** |

**Overall: 82/128 average (64%)** — C+. Functional nhưng còn nhiều vấn đề cơ bản.

---

## Action Items

| Priority | Action | Impact | Pages |
|----------|--------|--------|-------|
| 🔴 P0 | Fix dark mode — verify `kiteclass_theme` localStorage key | +4pts Technical mọi trang | Tất cả |
| 🔴 P1 | i18n auth pages: login, forgot-password, reset-password, auth sidebar | +3-4pts UF | auth pages |
| 🟡 P2 | Catalog: add timeout + empty state khi API fail (pattern như landing) | +5pts UF catalog | catalog |
| 🟡 P2 | Date input locale: `dd/mm/yyyy` cho Vietnamese users | +1pt UF | register/student |
| 🟡 P3 | Điền thực data: đội ngũ, chứng chỉ, bảng giá sections trên landing | +2pts VA | landing |
| 🟢 P4 | ARIA live regions cho error messages | +1pt WCAG | auth pages |

---

## Environment Notes

| Issue | Status |
|-------|--------|
| pnpm + WSL2 + NTFS symlink | npm install thay thế — hoạt động ✓ |
| autoprefixer.js missing | Đã resolve sau npm install ✓ |
| Dev server startup | `node_modules/.bin/next dev --port 3000` ✓ |
| Playwright chromium | v1217 installed ✓ |
| Screenshots captured | 36 PNGs ✓ (audit-2026-04-03/) |
