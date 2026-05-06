# G11 Theme Customization Live Preview — Component Spec (production port)

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G11-theme-preview/README.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G11-theme-preview/README.md) + 5 state HTML files under `states/`.
**Component gap:** G11 per `dossier/04-component-gaps.md` §G11.
**Tracking gap:** [`GAP-273`](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md) — stays 🟡 PARTIAL after this port.
**Wave:** 29 Bucket C — final 4 G* of Track 2 Phase 3 (G1/G9/G11/G12).
**Used by screens:** KH `/branding/wizard` step 5 (preview-approve before deploy).
**Persona:** P2 Center Owner.

---

## What this PR ships

- `<ThemePreview>` React component covering the 5 spec'd states (`default`, `brand-applied`, `dark-morph`, `mobile-preview`, `wcag-warning`) condensed into one self-contained component (mode toggle + before/after panels + reflexive sandbox + warning + auto-fix CTA).
- WCAG 2.1 contrast utilities — `calculateContrast(fg, bg)` (relative-luminance per W3C spec, returns 1-21, order-independent) and `suggestFix({foreground, background})` (auto-darken / auto-lighten foreground until AA, fallback to invert pair).
- TypeScript types exported on the public `@kite/shared-ui` API: `ThemePreview`, `ThemePreviewProps`, `ThemeMode`, `BrandColors`, `ContrastWarning`, `WCAG_AA_NORMAL`, `calculateContrast`, `suggestFix`.
- Vitest coverage: 16 tests (10 component + 6 utils) covering all 5 states + light/dark toggle + reflexive auto-fix demonstration + accessibility roles + Vietnamese label parity.

## Reflexive coverage (per spec README §49-58)

The signature feature of G11: when the picker yields a failing pair, the component renders the failing pair INSIDE its own demo sandbox AND offers an auto-fix CTA. Clicking auto-fix calls `suggestFix`, applies the AA-compliant alternative to the active sandbox, and the warning disappears. The same component therefore SHOWS the violation it would catch — this is the meta-correct pattern called out by the spec README §49.

The reflexive RTL test asserts the full cycle:
1. `calculateContrast(failingFg, failingBg) < WCAG_AA_NORMAL` (pre-condition).
2. Warning panel + auto-fix CTA visible.
3. Click auto-fix.
4. Warning removed; AA pill visible; "Đã áp dụng cặp màu đạt AA" surfaced.

## State / mode mapping

`ThemeMode` (sandbox surface tone toggle, NOT the brand pair):

| Mode | Wrapper | When |
|------|---------|------|
| `light` | `bg-slate-50 text-slate-900` | Default initial mode. |
| `dark` | `bg-slate-900 text-slate-100` | Owner toggles to compare dark variant. |

`BrandColors` (the active brand pair under WCAG measurement):

| Field | Subject |
|-------|---------|
| `primary` | CTA / button background — informational swatch only |
| `secondary` | Card accent / icon — informational swatch only |
| `background` | Sandbox surface fill — the BG side of measurement |
| `foreground` | Sandbox text colour — the FG side of measurement |

The auto-fix engine treats `foreground` as the variable, `background` as the constant — preserving the brand surface while remediating the text contrast.

## WCAG calculation (per W3C 2.1)

Relative luminance:
- For each channel `c` ∈ [0, 255]: `c' = c/255; L_channel = c'/12.92 if c' ≤ 0.03928 else ((c'+0.055)/1.055)^2.4`
- `L = 0.2126*R + 0.7152*G + 0.0722*B`

Contrast ratio: `(L1 + 0.05) / (L2 + 0.05)` where `L1 ≥ L2`. Range [1, 21].

AA threshold: `WCAG_AA_NORMAL = 4.5` (normal-size text per `.claude/rules/ai-branding-guidelines.md` §5).

## Auto-fix algorithm

`suggestFix({foreground, background})`:
1. If `calculateContrast(fg, bg) ≥ 4.5` → return as-is with reason "đạt AA".
2. Determine direction by comparing relative luminance of fg vs bg:
   - `fg ≥ bg` → push fg toward white (lerp by `t = step/20`)
   - `fg < bg` → push fg toward black (scale by `1 - step/20`)
3. Iterate 20 steps; first iteration yielding `ratio ≥ 4.5` wins; return the new fg + original bg.
4. If iteration exhausts without success, swap the pair (`fg ↔ bg`) — guaranteed to flip into the passing region.
5. Final fallback: black-on-white (`#000000` on `#ffffff`, ratio = 21).

The algorithm is deterministic + idempotent — calling it on an already-passing pair returns the same pair.

## Vietnamese formatting

All labels Vietnamese-only per CLAUDE.md + spec README §28-37:

- Heading: `Xem trước giao diện`
- Mode toggle: `Sáng` / `Tối` (radiogroup label `Chọn chế độ xem`)
- Before/After: `Trước` / `Sau`
- Sandbox label: `Vùng xem trước trực tiếp`
- Warning title: `Cảnh báo độ tương phản`
- Warning body: `Tỉ lệ tương phản hiện tại là X.XX:1 — cần ≥ 4.5:1 để đạt WCAG AA.`
- Auto-fix CTA: `Tự động sửa`
- Auto-fix applied: `Đã áp dụng cặp màu đạt AA`
- AA pill: `Đạt AA (X.XX:1)`
- Swatch labels: `Màu chính` / `Màu phụ` / `Nền` / `Chữ`

## Accessibility (WCAG AA)

- Wrapper carries `lang="vi"` by default.
- Mode toggle group: `role="radiogroup"` + `aria-label="Chọn chế độ xem"`; each option `role="radio"` + `aria-checked={mode === m}`.
- Sandbox surface: `aria-label="Vùng xem trước trực tiếp"` so AT announces the preview region.
- Warning panel: `role="alert"` + `aria-live="polite"` so AT immediately announces contrast failures.
- Auto-fix button: real `<button>` with `aria-label="Tự động sửa"`.
- Decorative swatch chips marked `aria-hidden="true"`; meaning conveyed via text labels.
- Component UI itself uses Tailwind tokens (slate / emerald / destructive) that pass AA on both light and dark wrappers — the failing pair lives ONLY inside the sandbox so the chrome remains accessible.

## Cross-component re-use

None this port. `calculateContrast` + `suggestFix` are standalone WCAG utilities — no dependency on other G* components. The utilities ARE re-exported on the public `@kite/shared-ui` API so future components (e.g. AI Branding quality gate review surface) can re-use them without copy-paste.

## Files

- `ThemePreview.tsx` — main component (≈260 LOC).
- `types.ts` — `ThemeMode`, `BrandColors`, `ContrastWarning`, `ThemePreviewProps`.
- `utils.ts` — `calculateContrast`, `suggestFix`, `WCAG_AA_NORMAL`.
- `__tests__/ThemePreview.test.tsx` — 10 component tests.
- `__tests__/utils.test.ts` — 6 utility tests.
- `index.tsx` — public re-exports.

## Acceptance criteria

- [x] light + dark toggle with smooth transition (CSS class change on wrapper)
- [x] WCAG warning badge appears on contrast `<4.5:1`
- [x] auto-fix CTA produces AA-compliant alternative + applies on click
- [x] before/after preview side-by-side
- [x] Vietnamese labels: `Xem trước giao diện`, `Cảnh báo độ tương phản`, `Tự động sửa`
- [x] Reflexive: the component SHOWS its own contrast violations + fixes them on its own preview surface
- [x] `pnpm -F @kite/shared-ui type-check` passes
- [x] `pnpm -F @kite/shared-ui test -- ThemePreview` passes (≥10 tests)
