# G11 — Theme Customization Live Preview

**Component gap:** G11 per `documents/02-architecture/design-system/dossier/04-component-gaps.md` §G11
**Flow ref:** `.claude/rules/ai-branding-guidelines.md` §4.2 (Preview before commit MANDATORY)
**Used by screens:** KH `/branding/wizard` step 5 (preview-approve before deploy)
**Persona:** P2 Center Owner

---

## Purpose

Wizard step 5 of AI Branding flow — owner sees the theme rendered LIVE before clicking DEPLOY. Per `ai-branding-guidelines.md` §4.2 this preview is MANDATORY (no auto-deploy after generate). Per §4.3 quality gate, must show contrast warning when picker yields fail (<4.5:1) and BLOCK deploy until fixed.

---

## States

| File | State | UI summary |
|------|-------|-----------|
| `states/default.html` | `default` | Generic KiteClass theme baseline + control panel (logo upload, color picker, radius, font select) |
| `states/brand-applied.html` | `brand-applied` | Side-by-side before/after + full preview + per-resource approve toggles (Logo / Bảng màu / Banner / Hero) |
| `states/dark-morph.html` | `dark-morph` | Light/dark side-by-side with auto-shifted brand color (#2563eb → #3b82f6 in dark) preserving family |
| `states/mobile-preview.html` | `mobile-preview` | iPhone 13 frame mockup (320×660 displayed) + mobile-first checks (tap target, safe-area, Zalo webview note) |
| `states/wcag-warning.html` | `wcag-warning` | Failing 3.2:1 yellow-on-yellow demo + 3 auto-suggested fixes (7.4:1 / 5.1:1 / 9.8:1) + deploy button DISABLED |

---

## Vietnamese UX

- All resource labels per `ai-branding-guidelines.md` §4.1: `Logo`, `Bảng màu`, `Banner trang chủ`, `Hero`
- Picker copy: `Kéo thả file SVG/PNG vào đây`
- WCAG fail copy: `Màu hiện tại không đạt WCAG AA — Contrast 3.2:1, cần ≥ 4.5:1`
- Quality gate copy: `Quality gate sẽ trả điểm 52/100 — FAIL (cần ≥ 70)`
- Deploy CTA disabled label: `Triển khai (chặn cho đến khi đạt AA)`
- Per-resource approve: toggle switch with success-color when checked

---

## Per-resource approve (per `ai-branding-guidelines.md` §4.2)

`brand-applied.html` shows the canonical pattern:
- 4 toggle switches, one per resource
- Each switch decoupled: owner can approve Logo + Colors but defer Hero
- Banner with "3/4 đã phê duyệt — Hero sẽ giữ template mặc định" feedback
- Visual: row swatch + name + sub-label + toggle (toggle uses semantic success color when on)

---

## WCAG warning (reflexive coverage — §4.3 + §5)

`wcag-warning.html` is the most important state:
- Demonstrates a CURRENT failing combo (#fbbf24 yellow on light yellow → 3.2:1)
- Auto-suggests 3 alternate hexes within same hue family but darker — all pass AA
- Each suggestion shows measured ratio (`7.4:1`, `5.1:1`, `9.8:1`) inline
- Deploy button is `disabled` until owner picks a passing color
- Component UI itself remains AA-compliant (meta-correct: shows the fail in a sandbox)

This pairs with `ai-branding-guidelines.md` §5 quality gate: WCAG ≥ 4.5:1 is one of 5 gate checks; if any fails the deploy is blocked and instance moves to FAILED state (see G9 `failed.html`).

---

## Accessibility

- Color picker has `aria-label="Chọn màu chính"` + adjacent text input with `aria-label="Mã màu HEX"` for keyboard users
- Per-resource toggles use `<input type="checkbox" class="sr-only">` + visual switch with proper `peer:checked` styling; each has `aria-label`
- Contrast pills use `font-mono` to make ratio scannable
- WCAG warning uses `role="alert"` + `aria-live="polite"`
- Phone frame mockup: descriptive labels for status bar elements (`aria-hidden="true"` on decorative emojis only)
- Dark/light toggle: `role="radiogroup"` with `aria-checked`
- All disabled buttons have `disabled` attr (NOT just visual styling)

---

## Reuse

- shadcn `Switch` for per-resource toggles (we hand-rolled here for HTML-only demo)
- shadcn `Tabs` for device toggle (Desktop/Tablet/Mobile)
- shadcn `Slider` for border-radius
- lucide icons: `arrow-left`, `palette`, `sun`, `moon`, `smartphone`, `alert-triangle`, `check-circle`
- Real impl: `BrandingProvider` injects CSS vars per `ai-branding-guidelines.md` §7.2; this component issues `branding.preview-changed` event (Outbox per §3.5 design-patterns rule)

---

## Self-score

| State | Score |
|-------|------:|
| `default.html` | 105/128 |
| `brand-applied.html` | 110/128 |
| `dark-morph.html` | 107/128 |
| `mobile-preview.html` | 106/128 |
| `wcag-warning.html` | 110/128 |
| `index.html` (showcase) | 109/128 |
| **Average** | **~108/128** |

All states ≥105/128.

---

## Acceptance criteria

- [x] All 5 states demonstrate distinct preview scenario
- [x] Brand-applied shows side-by-side before/after + per-resource approve (per §4.2)
- [x] Dark-morph shows automated brand-color shift preserving family
- [x] Mobile-preview shows phone frame + Zalo webview consideration
- [x] WCAG warning demonstrates 3.2:1 fail + 3 auto-suggestions + DEPLOY blocked
- [x] WCAG warning shows the failing combo INSIDE a sandbox while component UI remains AA
- [x] Vietnamese-only content; resource labels match §4.1
- [x] WCAG AA contrast measured + commented in every file
- [x] Per-resource approve toggle pattern matches §4.2
