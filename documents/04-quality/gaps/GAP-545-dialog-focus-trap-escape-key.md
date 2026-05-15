# GAP-545: Dialog focus-trap + Escape key handler — Wave 78 new modals (FeedbackWidget + OnboardingChecklist demo-confirm)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-05-14 (ui-review /128 post-Wave-78 audit)
**Affects:** `kitehub-frontend` — FeedbackWidget dialog (GAP-542) + OnboardingChecklist `IMPORT_DATA` demo-confirm modal (GAP-538). Beta tenant keyboard-only users / screen reader users.

## Problem

Wave 78 ship 2 dialogs mới với `role="dialog"` + `aria-modal="true"` đúng chuẩn ARIA primitives, NHƯNG thiếu 3 keyboard discipline items theo WAI-ARIA Authoring Practices Guide (APG) cho dialog pattern:

1. **Focus trap** — `Tab` từ last focusable element trong modal sẽ thoát ra background page (focus rò rỉ). User screen reader bị confused vì SR đọc background content trong khi modal vẫn visible.
2. **Escape key handler** — không có `onKeyDown` lắng nghe `Escape` để close. WCAG 2.1.1 (Keyboard) + WAI-ARIA APG dialog pattern mandate.
3. **Auto-focus on open** — không `useEffect` set focus tới first focusable input (Feedback widget: 5-star rating; demo-confirm: "Bật dữ liệu mẫu" CTA). User keyboard phải Tab nhiều lần mới tới control.

### Evidence

`grep -c 'focus-trap\|trapFocus\|onKeyDown.*Escape' kitehub/kitehub-frontend/src/components/feedback-widget/FeedbackWidget.tsx kitehub/kitehub-frontend/src/components/onboarding-checklist/OnboardingChecklist.tsx` → 0 hits both files.

Vi phạm:
- WCAG 2.1.1 (Keyboard) Level A
- WCAG 2.4.3 (Focus Order) Level A
- WAI-ARIA APG `dialog (modal)` pattern

## Root Cause

Wave 78 Bucket F (feedback widget) + Bucket B (onboarding) implemented modals ground-up không dựa trên primitive đã có focus-trap (vd `@radix-ui/react-dialog` đã có available qua `@/components/ui/button` cùng package). Author optimize cho "no portal — simpler test" (xem comment line 157 FeedbackWidget.tsx) — đánh đổi compliance.

## Proposed Fix

**Path A — Migrate sang Radix Dialog primitive** (preferred — battle-tested focus-trap):

```tsx
import * as Dialog from '@radix-ui/react-dialog';

<Dialog.Root open={open} onOpenChange={setOpen}>
  <Dialog.Portal>
    <Dialog.Overlay className="..." />
    <Dialog.Content
      onEscapeKeyDown={() => setOpen(false)}
      onPointerDownOutside={(e) => e.preventDefault()} // optional — disallow dismiss-on-outside
    >
      ...
    </Dialog.Content>
  </Dialog.Portal>
</Dialog.Root>
```

Radix tự lo focus-trap + Escape + auto-focus + scroll lock.

**Path B — Lightweight hook** (nếu muốn tránh Portal — match current "simpler test" philosophy):

```tsx
// src/hooks/useDialogA11y.ts
export function useDialogA11y(open: boolean, onClose: () => void, ref: RefObject<HTMLElement>) {
  useEffect(() => {
    if (!open) return;
    const previouslyFocused = document.activeElement as HTMLElement | null;
    const firstFocusable = ref.current?.querySelector<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    firstFocusable?.focus();

    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'Tab') {
        const focusables = ref.current?.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (!focusables || focusables.length === 0) return;
        const first = focusables[0];
        const last = focusables[focusables.length - 1];
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('keydown', handleKey);
      previouslyFocused?.focus();
    };
  }, [open, onClose, ref]);
}
```

Apply trong cả 2 dialogs. Bonus: restore focus tới previously-focused element khi close (WCAG 2.4.3).

**Test coverage:** thêm test "keyboard-only flow" trong `FeedbackWidget.test.tsx` + `OnboardingChecklist.test.tsx`:
- Open → expect first focusable focused
- Press `Escape` → expect modal closed
- Tab through last → expect wrap to first
- Shift-Tab from first → expect wrap to last

## Acceptance Criteria

- [ ] Path A (Radix) OR Path B (hook) implemented trong FeedbackWidget + OnboardingChecklist demo-confirm
- [ ] `Escape` key closes modal trong cả 2 dialogs (manual test + automated test)
- [ ] Tab trap: tabbing không thoát modal khi đang open
- [ ] Auto-focus first focusable element khi modal mở
- [ ] Restore focus tới trigger button khi modal close
- [ ] 4 new keyboard-flow tests pass (2 per dialog: escape + tab-wrap)
- [ ] Axe-core scan trên 2 dialogs report 0 WCAG 2.1.1 / 2.4.3 violations
- [ ] No regression trên existing widget tests (28 currently passing)

## Related

- Audit: [`documents/04-quality/audits/ui/2026-05-14-post-wave-78.md`](../audits/ui/2026-05-14-post-wave-78.md) §4 P1-A finding
- Parent gaps shipped: GAP-542 (feedback widget Bucket F), GAP-538 (onboarding Bucket B)
- Rubric: [`audit-skill-rubric-ui-review.md`](../../../.claude/rules/audit-skill-rubric-ui-review.md) §2.5 sub-check 5.5
- Rule: [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §3

## Log

- **2026-05-14:** DONE — Wave 79 Bucket D closure. Radix Dialog focus-trap migration applied to FeedbackWidget + OnboardingChecklist demo-confirm. WCAG 2.1.1 + 2.4.3 + 4.1.3 satisfied via radix-ui/react-dialog primitives (PR #1368).

- **2026-05-14:** Filed from Wave 78 post-wave ui-review audit. WCAG sub-check 5.5 (keyboard nav) failed on 2 sampled modals; P0 sub-check threshold not breached (other P0 a11y items PASS) → file as P1 follow-up per `audit-skill-rubric-ui-review.md` §4. Static-analysis evidence: 0 grep hits cho focus-trap / Escape handler trong 2 component files. Proposed paths A/B documented; Path A preferred (Radix battle-tested). Effort estimate ~2-3h cho Path A, ~3-4h cho Path B + tests.
