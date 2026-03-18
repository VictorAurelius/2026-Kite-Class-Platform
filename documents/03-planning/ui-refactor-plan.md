# KiteHub UI Refactor Plan

**Ngày tạo**: 2026-03-18
**Scope**: Toàn bộ KiteHub Frontend (Landing, Auth, Dashboard, Admin)
**Mục tiêu**: UI chuyên nghiệp ngang SaaS templates top Figma, giữ brand identity KiteHub

---

## 1. Đánh giá UI hiện tại

### Điểm mạnh (giữ lại)
- ✅ Tech stack tốt: Next.js 15 + shadcn/ui + Tailwind
- ✅ Brand colors đã define: Sky Blue (#00A3E0) + Orange (#FF8C00) + Teal
- ✅ Dark mode support (HSL variables)
- ✅ Card-based design pattern nhất quán
- ✅ Responsive breakpoints

### Điểm yếu (cần fix)
- ❌ Sidebar dùng emoji icons (📊, 💳, 🎨) → amateur
- ❌ Login/Register form dùng raw `<input>` thay vì shadcn components
- ❌ Không có custom font (dùng default system)
- ❌ Landing page thiếu social proof, trust signals
- ❌ Dashboard cards quá đơn giản, thiếu data visualization
- ❌ Mobile sidebar không collapse
- ❌ Spacing/typography không có hệ thống rõ ràng
- ❌ Không có illustration/graphics (chỉ có text + icons)
- ❌ Button variants không nhất quán

---

## 2. Design Direction: Modern Education SaaS

### Tham khảo SaaS Templates (Figma Community)

**Style phù hợp cho Education SaaS:**

| Trend | Áp dụng cho KiteHub |
|-------|---------------------|
| **Glassmorphism** | Header, modal overlays (đã có, tăng cường) |
| **Gradient accents** | Hero section, CTA buttons, section dividers |
| **Bento grid** | Dashboard cards, feature showcase |
| **Micro-interactions** | Button hover, card hover, page transitions |
| **Illustration** | Hero illustration, empty states, onboarding |
| **Data visualization** | Dashboard charts, admin analytics |
| **Rounded corners** | Tăng border-radius lên 12-16px cho cards |
| **Soft shadows** | Layered shadows thay vì flat borders |

### Brand Identity giữ lại

```
Primary:    Sky Blue (#00A3E0) → "Trust, Technology, Education"
Accent:     Orange (#FF8C00) → "Energy, Action, CTA"
Teal:       (#2DB39E) → "Growth, Success"
Background: Off-white (#FAFBFC) → Clean, spacious
```

### Typography upgrade

```
Headings:   Inter (Google Fonts) - Clean, professional, great Vietnamese support
Body:       Inter - Consistent
Mono:       JetBrains Mono - Code/data display
```

---

## 3. Refactor PRs (chia theo pages)

### PR-UI-1: Design System Foundation
**Priority**: P0 (làm trước tất cả UI PRs)
**Scope**:
- [ ] Add Inter font (Google Fonts hoặc next/font)
- [ ] Update tailwind.config: border-radius 12px, custom shadows, spacing scale
- [ ] Add semantic colors: success, warning, info (thay hardcoded green/yellow)
- [ ] Upgrade Button component: variants (primary, secondary, ghost, destructive, outline)
- [ ] Replace emoji sidebar icons → lucide-react icons
- [ ] Add collapsible mobile sidebar (Sheet component)
- [ ] Create GradientText, GradientButton components
- [ ] Create SectionTitle, PageHeader shared components
**Estimate**: 1 ngày

### PR-UI-2: Landing Page Redesign
**Priority**: P0
**Scope**:
- [ ] Hero: Gradient background + illustration + animated text
- [ ] Features: Bento grid layout (thay vì 3-column cards)
- [ ] Stats: Animated counters (instances served, students, etc.)
- [ ] Testimonials: Carousel/slider thay vì static cards
- [ ] Pricing preview: Highlighted popular plan + feature comparison
- [ ] Trust signals: "Trusted by X schools", partner logos
- [ ] CTA section: Gradient bg + compelling copy
- [ ] Footer: Richer layout (social links, newsletter, legal)
**Estimate**: 1-2 ngày

### PR-UI-3: Auth Pages Polish
**Priority**: P1
**Scope**:
- [ ] Login: Split layout (left: illustration/branding, right: form)
- [ ] Register: Multi-step wizard thay vì 1 form dài
- [ ] Use shadcn Input/Label components (thay raw `<input>`)
- [ ] Social login buttons (Google, Microsoft) - UI only, logic later
- [ ] Password strength indicator
- [ ] Animated transitions giữa login ↔ register
**Estimate**: 1 ngày

### PR-UI-4: Dashboard & Customer Portal
**Priority**: P1
**Scope**:
- [ ] Dashboard: Bento grid layout (stats cards + instance card + quick actions)
- [ ] Stats cards: Icon + number + trend indicator (↑↓)
- [ ] Instance cards: Richer layout (logo, subdomain, metrics, status badge)
- [ ] Welcome banner: "Good morning, [Name]" with gradient
- [ ] Quick actions: Create instance, View billing, AI branding
- [ ] Sidebar: Polish navigation (active indicator, hover effects, icons)
- [ ] Billing pages: Cleaner pricing cards, payment flow wizard
- [ ] Settings: Tab content improved, form validation UX
**Estimate**: 2 ngày

### PR-UI-5: Admin Portal
**Priority**: P2
**Scope**:
- [ ] Dashboard: Revenue chart (recharts), instance growth line chart
- [ ] Stats cards: Animated numbers, color-coded (green/red/blue/orange)
- [ ] Instance table: Better filters, bulk actions toolbar
- [ ] Payment table: Status pills, quick approve/reject
- [ ] Detail pages: Tabbed layout, action cards
**Estimate**: 1 ngày

### PR-UI-6: Animations & Polish
**Priority**: P2
**Scope**:
- [ ] Page transitions (framer-motion hoặc CSS)
- [ ] Skeleton loading states (thay vì spinner)
- [ ] Toast notifications style upgrade (sonner customization)
- [ ] Scroll-triggered animations cho landing page
- [ ] Dark mode polish (test tất cả pages)
- [ ] 404 page with illustration
- [ ] Favicon, meta images (OG image)
**Estimate**: 1 ngày

---

## 4. Execution Order

```
PR-UI-1 (Design System) ──→ PR-UI-2 (Landing) ──→ PR-UI-3 (Auth)
                               ↓
                          PR-UI-4 (Dashboard) ──→ PR-UI-5 (Admin)
                                                       ↓
                                                PR-UI-6 (Polish)
```

**Total estimate**: 7-8 ngày

---

## 5. Component Inventory (New/Upgraded)

### New Components
| Component | Purpose | Used In |
|-----------|---------|---------|
| `GradientButton` | CTA buttons with gradient | Landing, Auth |
| `GradientText` | Animated gradient headings | Landing, Hero |
| `BentoGrid` | Responsive bento layout | Dashboard, Features |
| `StatCard` | Animated stat display | Dashboard, Admin |
| `FeatureCard` | Rich feature showcase | Landing |
| `TestimonialCarousel` | Sliding testimonials | Landing |
| `PageHeader` | Consistent page title + description | All pages |
| `MobileSidebar` | Collapsible mobile nav (Sheet) | Dashboard, Admin |
| `SkeletonLoader` | Skeleton loading states | All data pages |
| `PasswordStrength` | Password strength meter | Register |

### Upgraded Components
| Component | Current | After |
|-----------|---------|-------|
| `Sidebar` | Emoji icons, no mobile | Lucide icons, collapsible |
| `Button` | Basic variants | + gradient, + icon, + loading |
| `Input` | Raw HTML in auth | shadcn Input everywhere |
| `Badge` | Basic | + animated, + color variants |
| `Card` | Flat border | Soft shadow, hover lift |

---

## 6. Không thay đổi

- ✅ Tech stack (Next.js 15, shadcn/ui, Tailwind, TypeScript)
- ✅ File structure (app router, component organization)
- ✅ API layer (axios, endpoints, hooks)
- ✅ State management (zustand, react-query)
- ✅ Brand colors (Sky Blue, Orange, Teal)
- ✅ All existing functionality (chỉ upgrade UI, không thay đổi logic)
