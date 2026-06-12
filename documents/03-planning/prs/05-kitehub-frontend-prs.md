# KITEHUB FRONTEND PR PLAN - Customer & Admin Portal

**Service:** KiteHub Frontend
**Architecture Version:** V4.1 (Bundled Model)
**Effective Date:** 2026-03-15
**Repository:** `kitehub/kitehub-frontend/` (new Next.js project)
**Total PRs:** 10 PRs
**Timeline:** 5-6 tuần
**Numbering:** 5.x (KiteHub Frontend series)

**References:**
- Backend APIs: `04-kitehub-prs.md` (11/15 PRs done)
- KiteClass Frontend patterns: `03-frontend-prs.md`, `frontend-plan.md`
- Architecture: `system-architecture-v4.md`
- Backend implementation: `kitehub-implementation-plan.md`

---

## OVERVIEW

KiteHub Frontend là giao diện web phục vụ 2 nhóm người dùng:

1. **Khách hàng (Customer Portal)** — Trung tâm/trường học muốn mua và sử dụng KiteClass
2. **Admin nền tảng (Admin Portal)** — Đội ngũ KiteHub quản lý toàn bộ hệ thống

**Kiến trúc:**
```
┌──────────────── KITEHUB FRONTEND ────────────────┐
│                                                    │
│  (public)          Marketing & Onboarding          │
│  ├── /              Landing page (giới thiệu)     │
│  ├── /pricing       Bảng giá & so sánh gói        │
│  ├── /register      Đăng ký trial 14 ngày         │
│  └── /login         Đăng nhập                     │
│                                                    │
│  (customer)         Customer Dashboard             │
│  ├── /dashboard     Tổng quan instance             │
│  ├── /instances     Quản lý instances              │
│  ├── /billing       Subscription & thanh toán      │
│  ├── /branding      AI Branding (logo → assets)   │
│  └── /settings      Cài đặt tài khoản             │
│                                                    │
│  (admin)            Platform Admin                 │
│  ├── /admin         Dashboard analytics            │
│  ├── /admin/instances   Quản lý tất cả instances  │
│  ├── /admin/payments    Xác nhận thanh toán        │
│  └── /admin/revenue     Báo cáo doanh thu         │
│                                                    │
└────────────────────────────────────────────────────┘
```

**Tech Stack** (đồng bộ với KiteClass Frontend):
- Next.js 15 (App Router, React 19)
- TypeScript 5.x (strict mode)
- Tailwind CSS 3.x + Shadcn/UI
- React Query (TanStack) 5.x
- Zustand (auth store)
- React Hook Form + Zod (validation)
- Axios (API client)
- Recharts (dashboard charts)
- Vitest + Testing Library (unit/component tests)
- Playwright (E2E tests)

---

## BACKEND API AVAILABILITY

| API Group | Base Path | Backend PR | Status |
|-----------|-----------|------------|--------|
| Instance CRUD | `/api/platform/instances` | PR 4.1 | ✅ Done |
| Trial Management | `/api/platform/instances/{id}/trial-status` | PR 4.3 | ✅ Done |
| Subscription CRUD | `/api/platform/subscriptions` | PR 4.4-4.5 | ✅ Done |
| Payment + VietQR | `/api/platform/payments` | PR 4.6, 4.20 | ✅ Done |
| Payment Webhook | `/api/platform/webhooks/payment` | PR 4.7 | ✅ Done |
| AI Branding | `/api/platform/branding/ai/*` | PR 4.8 | ✅ Done |
| Asset Storage | `/api/platform/branding/assets/*` | PR 4.10 | ✅ Done |
| Content Generation | `/api/platform/branding/content/*` | PR 4.11 | ✅ Done |
| Email Service | `/api/platform/emails/send` | PR 4.12 | ✅ Done |
| Admin Dashboard | `/api/platform/admin/dashboard` | PR 4.13 | ⏳ Pending |
| Admin Revenue | `/api/platform/admin/revenue` | PR 4.13 | ⏳ Pending |
| API Gateway | Gateway routing | PR 4.14 | ⏳ Pending |

**Lưu ý:** PR 4.13 (Admin Portal API) cần hoàn thành trước khi bắt đầu PR 5.8 (Admin Dashboard Frontend).

---

## PHASE 1: INFRASTRUCTURE & MARKETING (2 PRs)

### PR 5.1: Project Setup & Shared Infrastructure
**Branch:** `feature/KC-5.1-kitehub-fe-setup`
**Estimate:** 3-4 giờ
**Dependencies:** Không

**Scope:**
- Khởi tạo Next.js 15 project (`kitehub/kitehub-frontend/`)
- Cấu hình TypeScript, Tailwind CSS, Shadcn/UI
- API client (Axios instance) kết nối KiteHub Gateway (`localhost:9000`)
- Auth store (Zustand) — `accessToken`, `user`, `logout()`
- React Query provider
- Theme provider (dark/light mode)
- Layout components: `PublicLayout`, `DashboardLayout`, `AdminLayout`
- Sidebar navigation component
- Shared UI components: `StatusBadge`, `LoadingSpinner`, `ErrorAlert`, `EmptyState`
- Environment config (`.env.example`)

**Cấu trúc thư mục:**
```
kitehub-frontend/
├── src/
│   ├── app/
│   │   ├── (public)/           # Marketing pages
│   │   ├── (auth)/             # Login, Register
│   │   ├── (customer)/         # Customer dashboard
│   │   ├── (admin)/            # Admin portal
│   │   ├── layout.tsx
│   │   └── globals.css
│   ├── components/
│   │   ├── ui/                 # Shadcn components
│   │   ├── layout/             # Sidebar, Header, Footer
│   │   └── common/             # StatusBadge, LoadingSpinner
│   ├── hooks/
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts       # Axios instance
│   │   │   └── endpoints.ts    # API endpoint constants
│   │   ├── validations/        # Zod schemas
│   │   ├── utils.ts
│   │   └── format.ts           # VNĐ formatter, date formatter
│   ├── providers/
│   ├── stores/
│   │   └── auth-store.ts
│   └── types/
│       ├── instance.ts
│       ├── subscription.ts
│       ├── payment.ts
│       ├── branding.ts
│       └── api.ts
├── public/
├── next.config.js
├── tailwind.config.ts
├── package.json
└── Dockerfile
```

**Verification:**
- `pnpm dev` chạy thành công trên `localhost:3001`
- Shadcn components render đúng
- API client kết nối được backend (health check)

---

### PR 5.2: Marketing Pages (Landing + Pricing + Auth)
**Branch:** `feature/KC-5.2-marketing-pages`
**Estimate:** 4-5 giờ
**Dependencies:** PR 5.1

**Scope:**

**Landing Page (`/`):**
- Hero section: Tiêu đề, mô tả, CTA "Dùng thử miễn phí 14 ngày"
- Feature highlights: 6 tính năng chính của KiteClass
  - Quản lý học viên & giảng viên
  - Quản lý khóa học & lớp học
  - Điểm danh tự động
  - Thanh toán & hóa đơn
  - Landing page AI tự động
  - Báo cáo & phân tích
- Social proof: Số liệu (instances, students, courses)
- Testimonials section (placeholder)
- Footer: Links, contact info

**Pricing Page (`/pricing`):**
- Bảng so sánh 4 gói:

| Tính năng | FREE | BASIC (₫500k/th) | PREMIUM (₫1.5M/th) | ENTERPRISE |
|-----------|------|-------------------|---------------------|------------|
| Học viên | 10 | 50 | 200 | Unlimited |
| Giảng viên | 1 | 5 | 20 | Unlimited |
| Lưu trữ | 500MB | 2GB | 10GB | Unlimited |
| Custom domain | ❌ | ❌ | ✅ | ✅ |
| AI Branding | ❌ | ✅ | ✅ | ✅ |
| Hỗ trợ | Community | Email | Priority | Dedicated |

- Toggle Monthly/Annually (giảm 10% khi thanh toán năm)
- CTA buttons → `/register`
- FAQ section

**Auth Pages:**
- Login (`/login`): Email + password form
- Register (`/register`): Đăng ký trial
  - Fields: Organization name, subdomain (auto-suggest), owner email, password
  - Subdomain preview: `{subdomain}.kitehub.me`
  - Validation: Subdomain availability check (realtime)
  - Submit → POST `/api/platform/instances` (tạo trial instance)
  - Success → Redirect to `/dashboard`
- Forgot Password (`/forgot-password`): Email form (placeholder)

**Verification:**
- Landing page responsive (mobile, tablet, desktop)
- Pricing toggle hoạt động
- Register form validation đúng
- Đăng ký trial thành công → redirect

---

## PHASE 2: CUSTOMER PORTAL (4 PRs)

### PR 5.3: Customer Dashboard & Instance Management
**Branch:** `feature/KC-5.3-customer-dashboard`
**Estimate:** 4-5 giờ
**Dependencies:** PR 5.2

**Scope:**

**Dashboard (`/dashboard`):**
- Welcome banner: "Chào mừng {organizationName}"
- Instance overview cards:
  - Status badge (TRIAL / ACTIVE / SUSPENDED)
  - Trial countdown: "Còn {n} ngày trial" (progress bar)
  - Current tier + upgrade CTA
  - Quick link: "Truy cập KiteClass →" (`{subdomain}.kitehub.me`)
- Quick stats: Students, Teachers, Courses (từ instance)

**Instance Detail (`/instances/{id}`):**
- Instance info: Subdomain, organization name, status, tier
- Trial status card: Days remaining, warning levels (MEDIUM/HIGH/EXPIRED)
- Database status: Connection health
- Action buttons:
  - "Truy cập KiteClass" → external link
  - "Nâng cấp gói" → `/billing`
  - "AI Branding" → `/branding`

**APIs consumed:**
- `GET /api/platform/instances/owner/{ownerId}` — List owner's instances
- `GET /api/platform/instances/{id}` — Instance detail
- `GET /api/platform/instances/{id}/trial-status` — Trial info

**Verification:**
- Dashboard hiển thị đúng instance data
- Trial countdown chính xác
- Status badges đúng màu (TRIAL=blue, ACTIVE=green, SUSPENDED=red)

---

### PR 5.4: Subscription & Billing Management
**Branch:** `feature/KC-5.4-billing`
**Estimate:** 5-6 giờ
**Dependencies:** PR 5.3

**Scope:**

**Billing Overview (`/billing`):**
- Current plan card:
  - Tier name, price, billing cycle
  - Next renewal date
  - Auto-renew toggle
- Plan comparison (upgrade/downgrade options)
- "Nâng cấp" / "Hạ gói" buttons

**Upgrade Flow (`/billing/upgrade`):**
- Step 1: Chọn gói mới (highlight current vs new)
- Step 2: Xác nhận thay đổi
  - Upgrade: Hiển thị prorated charge
  - Downgrade: "Sẽ áp dụng cuối kỳ billing"
- Step 3: Thanh toán (redirect to payment)

**Payment Page (`/billing/payment/{paymentId}`):**
- VietQR code display (từ API `GET /payments/{id}/qr-code`)
- Payment info: Số tiền, nội dung CK, ngân hàng
- Countdown timer (QR expiry)
- Auto-refresh trạng thái (polling mỗi 5s)
- States:
  - PENDING: Hiển thị QR + "Đang chờ thanh toán..."
  - COMPLETED: ✅ "Thanh toán thành công!" → redirect
  - FAILED: ❌ "Thanh toán thất bại" → retry

**Payment History (`/billing/history`):**
- DataTable: Date, Amount, Method, Status, Invoice
- Filters: Status (COMPLETED, PENDING, FAILED), date range
- Download invoice (PDF — future)

**APIs consumed:**
- `GET /api/platform/subscriptions/instance/{id}/active` — Current subscription
- `GET /api/platform/subscriptions/instance/{id}` — Subscription history
- `PATCH /api/platform/subscriptions/{id}/upgrade` — Upgrade tier
- `PATCH /api/platform/subscriptions/{id}/downgrade` — Downgrade tier
- `POST /api/platform/payments` — Create payment
- `GET /api/platform/payments/{id}` — Payment status
- `GET /api/platform/payments/{id}/qr-code` — QR code URL
- `GET /api/platform/payments/subscription/{id}` — Payment history

**Verification:**
- Upgrade flow end-to-end
- QR code hiển thị đúng
- Payment status polling hoạt động
- History table có pagination + filters

---

### PR 5.5: AI Branding Portal
**Branch:** `feature/KC-5.5-branding`
**Estimate:** 5-6 giờ
**Dependencies:** PR 5.3

**Scope:**

**Branding Dashboard (`/branding`):**
- Current branding status card
- Generated assets gallery (nếu đã có)
- "Tạo Branding mới" CTA

**Branding Wizard (`/branding/create`):**
- Step 1: Upload Logo
  - Drag & drop / click to upload
  - Preview logo uploaded
  - Upload to S3 via `POST /branding/assets/{instanceId}/profile`
- Step 2: AI Analysis (auto)
  - Call `POST /branding/ai/analyze-logo`
  - Hiển thị kết quả: Primary colors, theme, brand personality
  - Cho phép chỉnh sửa (override colors, theme)
- Step 3: Generate Assets
  - Create branding job: `POST /branding/jobs`
  - Progress bar realtime (polling `GET /branding/jobs/{id}`)
  - Steps: Analyzing → Generating profiles → Generating heroes → Generating logos → Generating banners → Complete
- Step 4: Preview & Publish
  - Grid gallery: Profile images, Hero variants, Logos, Banners, OG image
  - Landing page preview (iframe hoặc mock)
  - Marketing copy preview (title, subtitle, tagline, about us)
  - "Xuất bản" button → Apply to instance landing page

**Asset Management (`/branding/assets`):**
- Grid view: All generated assets organized by type
- Download individual assets
- Delete & regenerate
- View S3/CDN URLs

**APIs consumed:**
- `POST /api/platform/branding/assets/{instanceId}/{type}` — Upload asset
- `POST /api/platform/branding/ai/analyze-logo` — Analyze logo
- `POST /api/platform/branding/jobs` — Create job
- `GET /api/platform/branding/jobs/{id}` — Job status/progress
- `GET /api/platform/branding/jobs/{id}/assets` — Generated assets
- `POST /api/platform/branding/content/generate` — Generate copy
- `GET /api/platform/branding/assets/{instanceId}` — List all assets

**Verification:**
- Logo upload + preview hoạt động
- AI analysis trả về kết quả hợp lệ
- Progress bar cập nhật realtime
- Asset gallery hiển thị đúng
- Landing page preview render đúng content

---

### PR 5.6: Customer Settings & Profile
**Branch:** `feature/KC-5.6-settings`
**Estimate:** 2-3 giờ
**Dependencies:** PR 5.3

**Scope:**

**Account Settings (`/settings`):**
- Profile: Name, email, phone (read-only hoặc editable)
- Organization info: Name, logo, contact info
- Password change form

**Instance Settings (`/settings/instance`):**
- Subdomain display (read-only after creation)
- Custom domain config (PREMIUM only):
  - Input custom domain
  - DNS instructions: "Tạo CNAME record trỏ đến `{subdomain}.kitehub.me`"
  - Verification status
- Notification preferences:
  - Email notifications on/off
  - Trial reminders on/off

**Danger Zone:**
- Cancel subscription button (with confirmation modal)
- Delete instance (with double confirmation: type instance name)

**APIs consumed:**
- `PATCH /api/platform/instances/{id}` — Update instance
- `DELETE /api/platform/subscriptions/{id}` — Cancel subscription
- `DELETE /api/platform/instances/{id}` — Delete instance

**Verification:**
- Custom domain flow hiển thị đúng cho PREMIUM
- Cancel subscription có confirmation dialog
- Delete instance yêu cầu nhập tên

---

## PHASE 3: ADMIN PORTAL (2 PRs)

### PR 5.7: Admin Instance & Payment Management
**Branch:** `feature/KC-5.7-admin-instances`
**Estimate:** 4-5 giờ
**Dependencies:** PR 5.1, Backend PR 4.13

**Scope:**

**Admin Instance List (`/admin/instances`):**
- DataTable with columns:
  - Organization name, Subdomain, Status, Tier, Trial end, Created at
- Filters: Status (TRIAL/ACTIVE/SUSPENDED), Tier, search by name
- Row actions:
  - View detail
  - Suspend / Activate
  - Extend trial (admin only)

**Admin Instance Detail (`/admin/instances/{id}`):**
- Full instance info
- Subscription history
- Payment history
- Database connection status
- Admin actions: Suspend, Activate, Extend trial, Force delete

**Payment Verification (`/admin/payments`):**
- Pending payments table
- Columns: Instance, Amount, Method, Created at, QR code
- Actions: Confirm ✅ / Reject ❌
- Confirm flow: Modal với transaction ID input → POST confirm
- Bulk actions: Confirm multiple payments

**APIs consumed:**
- `GET /api/platform/admin/instances` — All instances
- `PATCH /api/platform/admin/instances/{id}/suspend` — Suspend
- `PATCH /api/platform/admin/instances/{id}/activate` — Activate
- `POST /api/platform/instances/{id}/extend-trial` — Extend trial
- `GET /api/platform/admin/payments/pending` — Pending payments
- `POST /api/platform/admin/payments/{id}/confirm` — Confirm payment
- `POST /api/platform/admin/payments/{id}/reject` — Reject payment

**Verification:**
- Instance table có search + filter hoạt động
- Suspend/Activate thay đổi status đúng
- Payment confirm flow end-to-end
- Bulk confirm hoạt động

---

### PR 5.8: Admin Dashboard & Analytics
**Branch:** `feature/KC-5.8-admin-dashboard`
**Estimate:** 4-5 giờ
**Dependencies:** PR 5.7, Backend PR 4.13

**Scope:**

**Dashboard (`/admin`):**
- KPI Cards (top row):
  - Total Instances (by status breakdown)
  - MRR (Monthly Recurring Revenue) — ₫ format
  - Trial → Paid Conversion Rate (%)
  - Churn Rate (%)

- Charts:
  - Revenue trend (Recharts AreaChart — 30 ngày)
  - Instances by tier (PieChart)
  - New signups (BarChart — 30 ngày)
  - Revenue by tier breakdown (BarChart)

- Recent Activity:
  - New signups (last 7 days)
  - Pending payments
  - Expiring trials
  - Expiring subscriptions

**Revenue Report (`/admin/revenue`):**
- Date range picker (start/end)
- Period selector: Daily / Monthly / Yearly
- Revenue chart (AreaChart with daily breakdown)
- Revenue by tier table
- Export CSV button
- Summary: Total revenue, MRR, Projected ARR, Churn impact

**APIs consumed:**
- `GET /api/platform/admin/dashboard` — Dashboard stats
- `GET /api/platform/admin/revenue?period=MONTHLY&startDate=...&endDate=...` — Revenue report
- `GET /api/platform/subscriptions/expiring` — Expiring subscriptions

**Verification:**
- KPI cards hiển thị đúng số liệu
- Charts render responsive
- Revenue report filter đúng theo date range
- CSV export hoạt động

---

## PHASE 4: TESTING & DEPLOYMENT (2 PRs)

### PR 5.9: Testing Suite (Unit + Component + E2E)
**Branch:** `feature/KC-5.9-testing`
**Estimate:** 4-5 giờ
**Dependencies:** PR 5.1-5.8

**Scope:**

**Unit Tests (Vitest):**
- API client functions
- Utility functions (VNĐ formatter, date helpers)
- Zod validation schemas
- Zustand auth store

**Component Tests (Testing Library):**
- Marketing pages render correctly
- Form validation (register, login, upgrade)
- StatusBadge renders correct colors
- Trial countdown displays correctly
- Payment QR code component
- DataTable with mock data

**E2E Tests (Playwright):**
- Registration flow: Landing → Register → Dashboard
- Login/Logout flow
- Upgrade subscription flow
- Payment QR display
- Admin: Instance list → Suspend → Activate
- Admin: Payment confirm flow
- AI Branding: Upload → Analyze → Generate → Preview

**Coverage target:** ≥80%

**Verification:**
- All tests pass
- Coverage ≥80%
- E2E tests pass on CI

---

### PR 5.10: Docker & CI/CD Integration
**Branch:** `feature/KC-5.10-docker-ci`
**Estimate:** 2-3 giờ
**Dependencies:** PR 5.9

**Scope:**

**Dockerfile:**
- Multi-stage build (build + production)
- `next build` → `next start` (standalone output)
- Health check endpoint

**Docker Compose update:**
- Add `kitehub-frontend` service to `docker-compose.kitehub.yml`
- Port: 3001 (avoid conflict with KiteClass frontend 3000)
- Environment variables: `NEXT_PUBLIC_API_URL`

**CI/CD:**
- GitHub Actions workflow: `kitehub-frontend-ci.yml`
  - Lint + Type check
  - Unit tests (Vitest)
  - Build verification
  - E2E tests (Playwright — on PR to main)
  - Docker build test

**Verification:**
- `docker compose up kitehub-frontend` chạy thành công
- CI pipeline pass
- Production build size reasonable (<100MB)

---

## DEPENDENCY GRAPH

```
PR 5.1 (Setup)
├── PR 5.2 (Marketing + Auth)
│   └── PR 5.3 (Customer Dashboard)
│       ├── PR 5.4 (Billing)
│       ├── PR 5.5 (AI Branding)
│       └── PR 5.6 (Settings)
├── PR 5.7 (Admin Instances) ← requires Backend PR 4.13
│   └── PR 5.8 (Admin Dashboard) ← requires Backend PR 4.13
└── PR 5.9 (Testing) ← after all feature PRs
    └── PR 5.10 (Docker + CI)
```

**Parallel tracks:**
- Customer Portal (5.3 → 5.4, 5.5, 5.6) có thể phát triển song song
- Admin Portal (5.7, 5.8) phụ thuộc Backend PR 4.13
- Testing (5.9) chạy sau khi feature hoàn thành

---

## TIMELINE

| Tuần | PRs | Mô tả |
|------|-----|-------|
| 1 | 5.1, 5.2 | Setup + Marketing pages |
| 2 | 5.3, 5.4 | Customer Dashboard + Billing |
| 3 | 5.5, 5.6 | AI Branding + Settings |
| 4 | 5.7, 5.8 | Admin Portal (cần Backend PR 4.13) |
| 5 | 5.9, 5.10 | Testing + Docker/CI |

**Tổng:** 10 PRs, ~38-47 giờ, 5-6 tuần

---

## NOTES

### Khác biệt với KiteClass Frontend
- KiteHub FE phục vụ **platform admin + khách hàng B2B** (không phải học viên)
- Có **marketing/sales pages** (landing, pricing) — KiteClass FE không có
- Có **payment UI với VietQR** — tích hợp QR code realtime
- Có **AI Branding wizard** — multi-step với progress tracking
- **Không có** student/teacher/class/attendance pages

### Quy ước chung
- Branch: `feature/KC-5.X-description`
- Commit: `feat(kitehub-fe): short description`
- PR title: Max 70 chars, prefix với PR number
- Squash merge + delete branch
- Tất cả tests pass trước khi merge

### Backend PRs cần hoàn thành trước
- **PR 4.13** (Admin Portal API) — Cần cho PR 5.7, 5.8
- **PR 4.14** (API Gateway) — Cần cho routing qua gateway
- **PR 4.15** (Docker) — Cần cho PR 5.10

---

**Ngày tạo:** 2026-03-15
**Tác giả:** KiteClass Team
