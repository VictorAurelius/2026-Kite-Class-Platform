# GAP-667: Wave 98 UI hygiene cleanup — FeedbackForm semantic tokens + BetaDisclaimerBanner WCAG dismiss

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend (UI polish + WCAG compliance)
**Found:** 2026-05-19 (Wave 98 post-closure audit suite — GAP-661 UI /128 audit)
**Affects:** `kitehub-frontend/src/components/feedback/FeedbackForm.tsx` + `.../components/beta-disclaimer/BetaDisclaimerBanner.tsx`

## Problem

UI /128 audit sample identified 2 P2 hygiene issues — không P0 blockers nhưng accumulate technical debt:

### Issue 1: FeedbackForm success message raw color tokens

```tsx
// Current (FeedbackForm.tsx after submit success)
<div className="bg-green-50 text-green-700 ...">
  Cảm ơn bạn! Phản hồi đã được gửi.
</div>
```

Problems:
- Hardcoded `green-50` / `green-700` không through design system semantic tokens (`bg-success-subtle`, `text-success-emphasis` per design-system/tokens.css)
- Dark-mode inconsistency: green-50 stays light in dark theme → poor contrast
- Color contrast `text-green-700` on `bg-green-50` measures 4.4:1 (borderline WCAG AA — 4.5:1 required for normal text)

### Issue 2: BetaDisclaimerBanner dismiss button below WCAG touch target

```tsx
// Current (BetaDisclaimerBanner.tsx dismiss X)
<button className="p-1 ..." aria-label="Đóng banner">
  <X className="size-4" />  // 16×16 icon
</button>
```

Effective tap area:
- `p-1` = 4px padding × 4 sides
- `size-4` = 16×16px icon
- Total = ~24×24px (with padding ~28×28px)

WCAG 2.5.5 mandate ≥44×44px floor cho touch targets. Current 28px = below floor → mobile users hit miss-tap regularly, especially elderly P2 Center Owner persona (target audience). Per audit SupportMenu equivalent dismiss button = 56×56 (exemplary).

## Root Cause

- Issue 1: B5 agent prioritize functional submit + Radix Dialog wire (correct focus); color tokens last-mile polish skipped
- Issue 2: B3 agent ported existing banner from earlier wave; dismiss button inherited original sizing predating WCAG audit standard. SupportMenu (B0 fresh ship) demonstrates correct 56×56 — pattern available, just not applied to legacy banner

## Proposed Fix

### Step 1: FeedbackForm success message semantic tokens

```tsx
// FeedbackForm.tsx
<Alert variant="success" className="mt-4">
  <CheckCircle className="size-5" />
  <AlertDescription>
    Cảm ơn bạn! Phản hồi đã được gửi.
  </AlertDescription>
</Alert>
```

(Use shadcn `<Alert variant="success">` if exists; else add `success` variant to alert.tsx tokens.)

Verify dark-mode render via Tailwind `dark:` modifier.

### Step 2: BetaDisclaimerBanner dismiss button WCAG fix

```tsx
// BetaDisclaimerBanner.tsx
<button
  className="flex size-11 items-center justify-center rounded-md hover:bg-muted"  // 44×44 minimum
  aria-label="Đóng banner thông báo beta"
>
  <X className="size-5" />  // 20×20 icon centered trong 44×44 tap area
</button>
```

WCAG 2.5.5: 44×44 minimum touch target verified via DevTools Inspect → tap area assertion.

### Step 3: Audit other dismiss buttons trong codebase

`grep -rn "size-4\|size-3" kitehub-frontend/src/components --include="*.tsx" | grep -i "dismiss\|close\|cancel"` — find any other sub-44px tap targets. Batch fix if ≥3 found, else file follow-up.

## Acceptance Criteria

- [ ] FeedbackForm success message uses semantic `<Alert variant="success">` component
- [ ] Color contrast verified ≥4.5:1 light + dark mode
- [ ] BetaDisclaimerBanner dismiss button ≥44×44px tap area
- [ ] No regression on other component dismiss/close buttons (grep audit)
- [ ] UI /128 audit refresh next wave: FeedbackForm ≥118/128 A+ (vs 114) + BetaDisclaimerBanner ≥115/128 A (vs 110)

## Related

- **Parent audit:** `documents/04-quality/audits/ui/2026-05-19-wave-98-cluster-b-sample.md`
- **Reference exemplar:** SupportMenu floating button (B0 ship, 56×56 touch target — per audit "highest WCAG score")
- **Standard:** WCAG 2.5.5 (Target Size Level AAA) — 44×44 minimum
- **Standard:** WCAG 1.4.3 (Color Contrast Level AA) — 4.5:1 normal text
- **Design system:** `documents/02-architecture/design-system/tokens.css` semantic color tokens
