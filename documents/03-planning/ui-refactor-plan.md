# KiteHub UI Refactor Plan

**Ngày tạo**: 2026-03-18
**Cập nhật**: 2026-03-18 (v2 - thêm template references + user persona)
**Scope**: Toàn bộ KiteHub Frontend (Landing, Auth, Dashboard, Admin)

---

## Mục tiêu đỏ

1. **Landing page phải ĐẸP NHẤT có thể** - first impression quyết định conversion
2. **Dashboard/Portal phải PHÙ HỢP với tệp người dùng KiteClass** - chủ trung tâm giáo dục VN

---

## 1. Tệp người dùng KiteClass

### Persona chính: Chủ trung tâm ngoại ngữ / giáo dục tại Việt Nam

| Đặc điểm | Chi tiết |
|-----------|----------|
| **Tuổi** | 28-50 |
| **Giới tính** | Nữ chiếm 60-70% (ngành giáo dục VN) |
| **Trình độ IT** | Trung bình - thấp (dùng Facebook, Zalo thành thạo, ít dùng phần mềm quản lý) |
| **Thiết bị** | Laptop (70%) + Điện thoại (30%) |
| **Nỗi đau** | Quản lý bằng Excel/sổ tay, thiếu chuyên nghiệp, mất thời gian |
| **Mong muốn** | Phần mềm DỄ DÙNG, ĐẸP, TIẾNG VIỆT, giá hợp lý |
| **Tâm lý** | Ngại công nghệ, thích giao diện quen thuộc (giống app VN đã dùng) |

### UI principles cho tệp này

| Principle | Áp dụng |
|-----------|---------|
| **Đơn giản trước hết** | Ít options hiển thị, progressive disclosure |
| **Tiếng Việt 100%** | Không dùng thuật ngữ IT tiếng Anh trong UI |
| **Visual guidance** | Icons rõ ràng, labels cụ thể, tooltips khi cần |
| **Familiar patterns** | Giống Zalo, Facebook layout (sidebar left, content right) |
| **Trust & credibility** | Logo, testimonials, số liệu thực, chứng nhận |
| **Mobile-friendly** | Nhiều người check bằng điện thoại |
| **Warm colors** | Không quá corporate (cold blue), thêm warm tones |
| **Onboarding nhẹ** | Không overwhelm, hướng dẫn từng bước |

---

## 2. Template References

### Landing Page: [saas-landing-page-template](https://github.com/karthikmudunuri/saas-landing-page-template)
**Lấy:**
- ✅ Framer Motion animations (scroll-triggered)
- ✅ Dark gradient hero với animated elements
- ✅ Feature showcase layout
- ✅ Smooth section transitions

**Customize cho KiteHub:**
- Đổi dark theme → light/bright theme (education = friendly, approachable)
- Thêm education imagery (classroom, students, teachers)
- Tiếng Việt content với emotional copy
- Trust signals phù hợp VN (số trung tâm, số học viên)

### Dashboard: [next-shadcn-dashboard-starter](https://github.com/Kiranism/next-shadcn-dashboard-starter)
**Lấy:**
- ✅ Sidebar navigation pattern (collapsible, icons)
- ✅ Stats cards layout
- ✅ Table components
- ✅ Chart integration (recharts)

**Customize cho KiteHub:**
- Đơn giản hóa (ít metrics hơn, focus vào actions)
- Vietnamese labels
- Warm color palette
- Larger click targets (người dùng ít IT)

### Toàn bộ: [SaaS-Boilerplate](https://github.com/ixartz/SaaS-Boilerplate)
**Lấy:**
- ✅ Auth flow pattern
- ✅ Pricing page structure
- ✅ Multi-tenancy UI patterns

---

## 3. Đánh giá UI hiện tại

### Điểm mạnh (giữ lại)
- ✅ Tech stack: Next.js 15 + shadcn/ui + Tailwind
- ✅ Brand colors: Sky Blue (#00A3E0) + Orange (#FF8C00) + Teal
- ✅ Dark mode support
- ✅ Card-based design pattern
- ✅ Responsive breakpoints

### Điểm yếu (cần fix)
- ❌ Sidebar: emoji icons → amateur
- ❌ Auth forms: raw `<input>` → cần shadcn components
- ❌ Landing: thiếu animations, trust signals, emotional copy
- ❌ Dashboard: quá đơn giản, thiếu data visualization
- ❌ Mobile: sidebar không collapse
- ❌ Typography: không có custom font
- ❌ Illustrations: không có graphics
- ❌ Onboarding: người dùng mới không biết bắt đầu từ đâu

---

## 4. Brand Identity

### Giữ nguyên
```
Primary:    Sky Blue (#00A3E0) → Trust, Technology
Accent:     Orange (#FF8C00) → Energy, CTA
Teal:       (#2DB39E) → Growth, Success
```

### Thêm mới
```
Warm Beige:  (#FFF8F0) → Friendly background (thay pure white)
Soft Purple: (#8B5CF6) → Premium/AI features
Light Green: (#10B981) → Success states
```

### Typography
```
Headings:   Inter (next/font) - Clean, excellent Vietnamese
Body:       Inter
Mono:       JetBrains Mono (data/code)
```

### Tone
```
Landing:    Professional nhưng WARM - "Chúng tôi hiểu nỗi đau của bạn"
Dashboard:  Clean, calm, efficient - "Mọi thứ bạn cần, ở một nơi"
Admin:      Data-focused, actionable - "Kiểm soát toàn bộ"
```

---

## 5. Refactor PRs

### PR-UI-1: Design System Foundation
**Priority**: 🔴 P0
**Template ref**: [next-shadcn-dashboard-starter](https://github.com/Kiranism/next-shadcn-dashboard-starter) (component patterns)
**Scope**:
- [ ] Inter font via next/font/google
- [ ] tailwind.config: border-radius 12px, layered shadows, warm bg color
- [ ] Semantic colors: success (#10B981), warning (#F59E0B), info (#3B82F6)
- [ ] Button: gradient variant, icon variant, loading state
- [ ] Sidebar: lucide icons, collapsible mobile (Sheet), active indicator
- [ ] New: PageHeader, SectionTitle, GradientButton, GradientText
- [ ] Warm Beige background (#FFF8F0) cho light mode
**Estimate**: 1 ngày

### PR-UI-2: Landing Page - "Đẹp nhất có thể"
**Priority**: 🔴🔴 P0 (mục tiêu đỏ)
**Template ref**: [saas-landing-page-template](https://github.com/karthikmudunuri/saas-landing-page-template) (animations, layout)
**Scope**:
- [ ] **Hero**:
  - Gradient mesh background (warm tones, không dark)
  - Animated headline: "Quản lý trung tâm giáo dục thông minh"
  - Sub-headline emotional: "Dành thời gian cho việc giảng dạy, để KiteClass lo phần còn lại"
  - CTA gradient button: "Dùng thử miễn phí 14 ngày"
  - Hero illustration: classroom/education SVG hoặc Lottie animation
  - Floating UI mockup screenshots
- [ ] **Social proof bar**: "Được X+ trung tâm tin dùng"
- [ ] **Features**: Bento grid (6 features)
  - Quản lý học viên, Lịch học & điểm danh, Thanh toán & hóa đơn
  - AI Branding, Báo cáo & thống kê, Đa chi nhánh
  - Mỗi feature: icon + title + description + mini screenshot
- [ ] **How it works**: 3 steps (Đăng ký → Cấu hình → Sử dụng)
- [ ] **Stats**: Animated counters (scroll-triggered via Framer Motion)
- [ ] **Testimonials**: Carousel (chủ trung tâm VN, ảnh thật, quotes)
- [ ] **Pricing**: 4 tiers, highlighted PREMIUM, toggle tháng/năm
  - Giá VND (không USD)
  - Feature comparison table
- [ ] **FAQ**: Accordion (câu hỏi phổ biến)
- [ ] **CTA bottom**: "Bắt đầu miễn phí ngay hôm nay" + trust signals
- [ ] **Footer**: Social links (Facebook, Zalo), hotline, email, legal
- [ ] **Animations**: Framer Motion scroll-triggered, stagger children
**Estimate**: 2-3 ngày

### PR-UI-3: Auth Pages
**Priority**: P1
**Template ref**: [SaaS-Boilerplate](https://github.com/ixartz/SaaS-Boilerplate) (auth pattern)
**Scope**:
- [ ] Login: Split layout (left: education illustration + tagline, right: form)
- [ ] Register: 3-step wizard (1. Thông tin → 2. Tên miền → 3. Xác nhận)
- [ ] shadcn Input/Label thay raw HTML
- [ ] Password strength indicator
- [ ] Error states rõ ràng (tiếng Việt)
- [ ] Mobile: single column, illustration hidden
**Estimate**: 1 ngày

### PR-UI-4: Dashboard & Customer Portal
**Priority**: P1
**Template ref**: [next-shadcn-dashboard-starter](https://github.com/Kiranism/next-shadcn-dashboard-starter) (layout, charts)
**Người dùng**: Chủ trung tâm (IT thấp-trung bình)
**Scope**:
- [ ] **Welcome banner**: "Chào [Tên], chúc bạn một ngày hiệu quả!"
- [ ] **Dashboard**:
  - Bento grid: Instance card lớn (chính) + stats nhỏ (phụ)
  - Quick actions rõ ràng: "Truy cập KiteClass", "Nâng cấp", "Hỗ trợ"
  - Ít metrics (chỉ: học viên, lớp, doanh thu tháng)
- [ ] **Instance cards**:
  - Logo/avatar trung tâm
  - Tên + subdomain
  - Status badge lớn, rõ ràng
  - Trial countdown prominent
  - 1 CTA button rõ ràng: "Vào quản lý"
- [ ] **Sidebar**:
  - Icons + labels rõ ràng (tiếng Việt)
  - Active state nổi bật
  - Collapse trên mobile
  - 4 items: Tổng quan, Thanh toán, Thương hiệu, Cài đặt
- [ ] **Billing**: Pricing cards đơn giản, giá VND
- [ ] **Settings**: Forms với labels rõ, validation messages tiếng Việt
**Estimate**: 2 ngày

### PR-UI-5: Admin Portal
**Priority**: P2
**Template ref**: [next-shadcn-dashboard-starter](https://github.com/Kiranism/next-shadcn-dashboard-starter) (charts, tables)
**Scope**:
- [ ] Revenue chart: Line/Bar chart (recharts)
- [ ] Stats: Animated counters, color-coded cards
- [ ] Instance table: Search, filter, status pills
- [ ] Payment table: Quick approve/reject buttons
**Estimate**: 1 ngày

### PR-UI-6: Animations & Polish
**Priority**: P2
**Scope**:
- [ ] Framer Motion page transitions
- [ ] Skeleton loading (thay spinner)
- [ ] Scroll animations cho landing
- [ ] Dark mode polish
- [ ] 404 page
- [ ] OG image, favicon
- [ ] Performance: Lighthouse score > 90
**Estimate**: 1 ngày

---

## 6. Execution Order

```
PR-UI-1 (Design System) ──→ PR-UI-2 (Landing - MỤC TIÊU ĐỎ)
                                  ↓
                             PR-UI-3 (Auth) ──→ PR-UI-4 (Dashboard)
                                                      ↓
                                                PR-UI-5 (Admin) ──→ PR-UI-6 (Polish)
```

**Total estimate**: 8-10 ngày

---

## 7. Landing Page Wireframe (PR-UI-2)

```
┌─────────────────────────────────────────────────┐
│  HEADER: Logo | Tính năng | Bảng giá | Đăng nhập | [Dùng thử] │
├─────────────────────────────────────────────────┤
│                                                 │
│  HERO:                                          │
│  ┌─────────────────┐  ┌──────────────────┐     │
│  │ Quản lý trung   │  │  [UI Mockup      │     │
│  │ tâm giáo dục    │  │   Screenshot]    │     │
│  │ thông minh      │  │                  │     │
│  │                 │  │                  │     │
│  │ [Dùng thử 14   │  └──────────────────┘     │
│  │  ngày miễn phí] │                           │
│  └─────────────────┘                           │
│                                                 │
│  SOCIAL PROOF: "500+ trung tâm | 50,000+ học viên"│
│                                                 │
├─────────────────────────────────────────────────┤
│                                                 │
│  FEATURES (Bento Grid):                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ 👨‍🎓 Quản lý │ │ 📅 Lịch học │ │ 💳 Thanh  │       │
│  │ học viên  │ │ điểm danh │ │ toán     │       │
│  ├──────────┤ ├──────────┤ ├──────────┤       │
│  │ 🎨 AI    │ │ 📊 Báo cáo│ │ 🏢 Đa    │       │
│  │ Branding │ │ thống kê  │ │ chi nhánh│       │
│  └──────────┘ └──────────┘ └──────────┘       │
│                                                 │
├─────────────────────────────────────────────────┤
│                                                 │
│  HOW IT WORKS:                                  │
│  ① Đăng ký → ② Cấu hình → ③ Sử dụng          │
│                                                 │
├─────────────────────────────────────────────────┤
│                                                 │
│  TESTIMONIALS: [←] "Quote từ chủ TT" [→]       │
│                                                 │
├─────────────────────────────────────────────────┤
│                                                 │
│  PRICING: [Tháng/Năm]                          │
│  ┌────┐ ┌────┐ ┌─────────┐ ┌────┐             │
│  │FREE│ │BASIC│ │★PREMIUM★│ │ENTER│             │
│  │ 0đ │ │199k│ │  399k   │ │Liên │             │
│  │    │ │/th │ │  /tháng  │ │ hệ  │             │
│  └────┘ └────┘ └─────────┘ └────┘             │
│                                                 │
├─────────────────────────────────────────────────┤
│  FAQ: [Accordion questions]                     │
├─────────────────────────────────────────────────┤
│  CTA: "Bắt đầu miễn phí" + Hotline + Zalo     │
├─────────────────────────────────────────────────┤
│  FOOTER: Links | Social | Legal | © KiteClass   │
└─────────────────────────────────────────────────┘
```

---

## 8. Không thay đổi

- ✅ Tech stack (Next.js 15, shadcn/ui, Tailwind, TypeScript)
- ✅ File structure (app router, component organization)
- ✅ API layer (axios, endpoints, hooks)
- ✅ State management (zustand, react-query)
- ✅ Brand colors core (Sky Blue, Orange, Teal)
- ✅ All existing functionality (chỉ upgrade UI, không thay đổi logic)
- ✅ E2E tests (110 FE + 63 BE phải vẫn pass sau refactor)
