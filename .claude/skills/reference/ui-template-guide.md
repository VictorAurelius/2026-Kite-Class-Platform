# UI Template Guide — Code from Design, Not Freeform

## Principle

**KHÔNG BAO GIỜ render UI freeform.** Mọi page/component PHẢI dựa trên:
1. Figma template (nếu có) → pixel-perfect
2. Page templates từ `frontend-standards.md` section 8 → copy-paste
3. Existing page trong codebase → follow pattern

**Priority:** Figma > Page template > Copy existing > Freeform (TRÁNH)

## KiteClass Frontend Stack

- **Framework:** Next.js 14 (App Router)
- **UI Library:** Shadcn/ui + Radix primitives
- **Styling:** Tailwind CSS với design tokens
- **State:** React Query (server state) + Zustand (client state)
- **Forms:** React Hook Form + Zod
- **i18n:** next-intl

## Page Templates (Copy-Paste)

Xem chi tiết tại `frontend-standards.md` → **Section 8: Page Templates**.

| Template | Dùng khi |
|----------|---------|
| List Page | Danh sách resources (students, courses, classes) |
| Detail/Edit Page | Xem + sửa 1 record |
| Dashboard Page | Overview + stats cards |
| Form Page | Create/wizard flow |
| Settings Page | Config groups |

## Page Checklist (BẮT BUỘC trước commit)

### States — TẤT CẢ required

- [ ] **Loading:** `<Skeleton />` hoặc spinner
- [ ] **Error:** `<Alert variant="destructive">` với message
- [ ] **Empty:** Centered message + CTA button
- [ ] **Success:** `<toast>` notification (sonner)

### UX Patterns

- [ ] Delete/destructive → `<ConfirmDialog />` (KHÔNG dùng `window.confirm`)
- [ ] CRUD success → toast (sonner)
- [ ] Form validation → inline errors dưới field (`<FormMessage />`)
- [ ] Navigation → `<Link>` component (KHÔNG dùng `router.push` cho nav thường)

### Visual Consistency

- [ ] Colors: design tokens only — `bg-primary`, `text-muted-foreground` (KHÔNG hardcode hex)
- [ ] KHÔNG inline styles (trừ dynamic values như `style={{ width: percent + '%' }}`)
- [ ] Icons: `lucide-react` nhất quán + cùng kích thước trong 1 page
- [ ] Spacing: `space-y-4`, `gap-6` (KHÔNG dùng `mt-3 mb-7` lộn xộn)

## KiteClass-Specific Anti-patterns

```tsx
// ❌ Hardcoded color
<div className="bg-[#2563EB]">
// ✅ Design token (thay đổi theo theme)
<div className="bg-primary">

// ❌ window.confirm
if (window.confirm('Xóa học sinh?')) handleDelete();
// ✅ ConfirmDialog
<ConfirmDialog
  title="Xóa học sinh?"
  description="Hành động này không thể hoàn tác."
  onConfirm={handleDelete}
/>

// ❌ Không có empty state
{students?.map(s => <StudentCard key={s.id} student={s} />)}
// ✅ Đầy đủ states
{students?.length === 0
  ? <EmptyState icon={Users} message="Chưa có học sinh" action={<AddStudentButton />} />
  : students.map(s => <StudentCard key={s.id} student={s} />)}

// ❌ Freeform spacing
<div className="mt-3 mb-7 px-5">
// ✅ Convention
<div className="space-y-6 px-4">
```

## Figma Workflow (nếu có designer)

```
documents/06-diagrams/figma/
├── README.md           # Link Figma + page index
├── exports/            # PNG exports mỗi page (committed)
└── tokens/             # Design tokens export (optional)
```

1. Designer tạo/chọn Figma template → share link trong README.md
2. Export PNGs → `figma/exports/` (commit để Claude AI đọc được)
3. Code từ exports — Claude có thể đọc PNG và generate matching code
4. Review — so sánh code vs Figma export

## Pre-commit Quality Check

```bash
# Hardcoded colors (should be 0)
grep -rn 'bg-\[#\|text-\[#\|border-\[#' kiteclass/kiteclass-frontend/src --include="*.tsx"

# window.confirm (should be 0)
grep -rn "window\.confirm" kiteclass/kiteclass-frontend/src --include="*.tsx"

# Missing error/empty state (review manually)
find kiteclass/kiteclass-frontend/src/app -name "page.tsx" | while read f; do
  grep -qE "error|Error|empty|Empty" "$f" || echo "Review: $f"
done
```

## Gotchas

1. **Theme tokens không apply ngay** — cần restart dev server sau khi đổi CSS variables trong `globals.css`
2. **Shadcn component variants** — luôn dùng `variant=` prop thay vì override className, để theme work
3. **next-intl trong Server Component** — dùng `getTranslations()` (async), KHÔNG dùng `useTranslations()` hook
4. **KiteClass tenant theme** — màu primary load từ API theo `?primary=HEX` query param, test bằng URL trực tiếp
