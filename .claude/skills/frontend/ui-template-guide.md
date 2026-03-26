# UI Template Guide — Code từ Design, không render tự do

## Nguyên tắc

**KHÔNG render UI tự do.** Mọi page/component PHẢI dựa trên:
1. Figma template (nếu có) → pixel-perfect
2. Page templates trong `frontend-standards.md` section 8 → copy-paste
3. Existing page trong codebase → follow pattern

**Thứ tự ưu tiên:** Figma > Page template > Copy existing page > Tự design (TRÁNH)

## Figma Workflow

### Setup

```
documents/06-diagrams/figma/
├── README.md           # Link Figma file + page index
├── exports/            # PNG exports per page (committed)
└── tokens/             # Design tokens export (nếu dùng Figma Tokens plugin)
```

### Quy trình

1. **Designer tạo/chọn Figma template** → share link vào `documents/06-diagrams/figma/README.md`
2. **Export PNG** cho mỗi page → `figma/exports/` (committed, để Claude AI có thể đọc)
3. **Developer code** theo export — Claude AI có thể nhìn PNG và generate code khớp
4. **Review** — so sánh code vs Figma export

### Không có Figma? Chọn template

Các nguồn template miễn phí chất lượng cao:

| Template | Stack | Link | Phù hợp cho |
|----------|-------|------|-------------|
| Shadcn Taxonomy | Next.js + Shadcn | github.com/shadcn-ui/taxonomy | SaaS dashboard |
| Shadcn Admin | Next.js + Shadcn | github.com/satnaing/shadcn-admin | Admin panel |
| Next SaaS Starter | Next.js + Shadcn | github.com/mickasmt/next-saas-stripe-starter | Landing + dashboard |
| Tremor Dashboard | React + Tremor | github.com/tremorlabs/tremor | Analytics dashboard |

**Workflow khi dùng template:**
1. Clone template repo
2. Screenshot các pages quan trọng → `figma/exports/`
3. Ghi lại design decisions trong `figma/README.md`
4. Code theo screenshots — giữ consistency

## Page Checklist (BẮT BUỘC)

Mỗi page/component mới PHẢI check trước khi commit:

### Layout
- [ ] Dùng đúng page template (list/detail/form/stats) từ `frontend-standards.md` section 8
- [ ] Header: `text-3xl font-bold` + subtitle `text-muted-foreground`
- [ ] Spacing: `space-y-6` wrapper, `gap-4` grids, `p-6` cards
- [ ] Responsive: `grid-cols-1 md:grid-cols-2` cho forms, `md:grid-cols-4` cho stats

### States (tất cả phải có)
- [ ] **Loading:** `LoadingSpinner` centered hoặc `Skeleton`
- [ ] **Error:** Error banner `border-destructive/50 bg-destructive/10`
- [ ] **Empty:** Message centered `text-muted-foreground` + CTA
- [ ] **Success:** Toast notification

### UX Patterns
- [ ] Delete/destructive → `ConfirmDialog` (KHÔNG `window.confirm`)
- [ ] CRUD success → Toast
- [ ] Form validation → Inline errors dưới field (Zod + react-hook-form)
- [ ] Navigation → `Link` (KHÔNG `router.push` cho regular nav)

### Visual
- [ ] Colors: chỉ dùng design tokens (`bg-card`, `text-muted-foreground`, etc.)
- [ ] KHÔNG hardcode hex colors (`bg-[#xxx]`)
- [ ] KHÔNG inline styles (trừ dynamic values như chart height, theme colors)
- [ ] Icons: Lucide React, `h-4 w-4` trong buttons, `h-8 w-8` trong stats

## Anti-patterns

```tsx
// ❌ BAD: Tự chọn spacing
<div className="mt-3 mb-7 px-5">

// ✅ GOOD: Dùng convention
<div className="space-y-6">  // page wrapper
<div className="p-6">        // card
<div className="gap-4">      // grid

// ❌ BAD: Hardcode color
<div className="bg-[#3B82F6] text-white">

// ✅ GOOD: Design token
<div className="bg-primary text-primary-foreground">

// ❌ BAD: window.confirm
if (window.confirm('Xóa?')) handleDelete();

// ✅ GOOD: ConfirmDialog
<ConfirmDialog onConfirm={handleDelete} variant="destructive" />

// ❌ BAD: Không có empty state
{data?.content?.map(item => <Card />)}

// ✅ GOOD: Có empty state
{data?.content?.length === 0 ? (
  <EmptyState message="Chưa có dữ liệu" />
) : (
  data.content.map(item => <Card />)
)}

// ❌ BAD: Không có error state
const { data } = useQuery(...);

// ✅ GOOD: Handle error
const { data, error, isLoading } = useQuery(...);
if (error) return <ErrorAlert />;
```

## Pre-commit FE Quality Check

Thêm vào CI hoặc pre-commit script:

```bash
# Check for window.confirm (should use ConfirmDialog)
grep -rn "window.confirm" src/ --include="*.tsx" && echo "❌ Use ConfirmDialog instead" && exit 1

# Check for hardcoded colors
grep -rn 'bg-\[#\|text-\[#\|border-\[#' src/ --include="*.tsx" && echo "❌ Use design tokens" && exit 1

# Check for missing error handling in pages
for page in $(find src/app -name "page.tsx"); do
  grep -q "error\|Error\|catch" "$page" || echo "⚠️  Missing error handling: $page"
done
```
