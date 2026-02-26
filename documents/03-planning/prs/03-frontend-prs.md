# Frontend - PR Implementation List

**Project**: kiteclass-frontend
**Version**: V4.1 (Bundled Model)
**Tech Stack**: Next.js 14, TypeScript, Tailwind CSS, Shadcn/UI
**Total PRs**: 15 (13 original + 2 V4.1)
**Completed**: 7 (47%)
**Status**: 🔄 Active development
**Last Updated**: 2026-02-26

**Reference**:
- Technical plan: [`frontend-plan.md`](../implementation/frontend-plan.md)
- Master index: [`00-master-pr-index.md`](./00-master-pr-index.md)
- Backend PRs: [`02-core-prs.md`](./02-core-prs.md) (PR 2.9 LMS, PR 2.10 Marketing)

---

## ✅ Phase 1: Infrastructure (COMPLETE)

### PR 3.1: Project Setup & Core Infrastructure ✅
**Status**: Complete (PR-REVIEW-3.1)
**Description**: Initialize Next.js 14 with App Router

**Tasks**:
- Next.js 14 project with TypeScript
- Tailwind CSS + Shadcn/UI components
- TypeScript types (auth, student, teacher, course)
- API client with Axios interceptors
- React Query provider
- Feature detection hook (`useFeatureDetection`)
- FeatureGate component for tier-based features
- Environment setup

**Testing**: Build passes, TypeScript strict mode

---

### PR 3.2: Shared Components & Layout System ✅
**Status**: Complete (Merged #2)
**Description**: Reusable UI components and layouts

**Components**:
- **Layouts**: DashboardLayout, AuthLayout, Sidebar, Header, Footer
- **Common UI**: DataTable, SearchInput, StatusBadge, LoadingSpinner, ErrorAlert
- **Forms**: FormInput, FormSelect, FormTextarea
- **CI**: Frontend CI workflow (TypeScript, ESLint, tests, build)

**Testing**: Component unit tests, Storybook (optional)

---

### PR 3.3: Authentication Pages ✅
**Status**: Complete (Merged #3)
**Description**: Login, forgot password, reset password

**Features**:
- Auth store with Zustand persist
- Auth API functions (login, logout, refresh, forgot/reset)
- useAuth hook with React Query mutations
- Pages: Login, Forgot Password, Reset Password
- Dynamic rendering for auth pages
- Suspense boundary for useSearchParams

**Testing**: Auth flow tests, form validation

---

## ✅ Phase 2: Admin Pages (7/7 COMPLETE)

### PR 3.4: Student Management Pages ✅
**Status**: Complete (Merged #4)
**Description**: Full CRUD for students

**Pages**:
- `/admin/students` - List with search & pagination
- `/admin/students/new` - Create student form
- `/admin/students/[id]` - Student detail
- `/admin/students/[id]/edit` - Edit student

**Features**:
- StudentForm with Zod validation
- Student table with status badges
- Soft delete confirmation
- Vietnamese UI labels and error messages
- Search by name/email
- Filter by status

**Testing**: Component tests, form validation tests

---

### PR 3.5: Teacher Management Pages ✅
**Status**: Complete (Merged 2026-02-22)
**Description**: Full CRUD for teachers

**Pages**:
- `/admin/teachers` - List with filters
- `/admin/teachers/new` - Create teacher
- `/admin/teachers/[id]` - Teacher detail
- `/admin/teachers/[id]/edit` - Edit teacher

**Features**:
- TeacherForm with specialization, experience
- Status management (ACTIVE, ON_LEAVE, TERMINATED)
- Multi-tenant isolation

**Testing**: Component tests, API integration tests

---

### PR 3.6: Course Management Pages ✅
**Status**: Complete (Merged 2026-02-22)
**Description**: Course lifecycle management

**Pages**:
- `/admin/courses` - List with status filter
- `/admin/courses/new` - Create course (DRAFT)
- `/admin/courses/[id]` - Course detail with syllabus
- `/admin/courses/[id]/edit` - Edit course

**Features**:
- Lifecycle actions: Publish (DRAFT → PUBLISHED), Archive
- Read-only mode for ARCHIVED courses
- Syllabus editor (rich text)
- Cannot delete PUBLISHED courses

**Testing**: Lifecycle transition tests, validation tests

---

### PR 3.7: Class Management Pages ✅
**Status**: Complete (Merged 2026-02-24)
**Description**: Class scheduling and management

**Pages**:
- `/admin/classes` - List with filters (status, teacher, course)
- `/admin/classes/new` - Create class from course template
- `/admin/classes/[id]` - Class detail (info, schedules, students)
- `/admin/classes/[id]/edit` - Edit class

**Features**:
- Class lifecycle: Start, Complete, Cancel
- Schedule recurring patterns (weekly, custom)
- Session auto-generation
- Student enrollment list
- Class code display

**Testing**: Schedule creation tests, lifecycle tests

---

### PR 3.8: Attendance Management Pages ⏳
**Status**: Pending
**Dependencies**: PR 2.7 Attendance Module (backend)
**Estimated**: 1-2 weeks

**Pages**:
- `/teacher/attendance` - Today's classes for teacher
- `/teacher/classes/[id]/attendance` - Mark attendance for session
- `/admin/classes/[id]/attendance/stats` - Attendance statistics
- `/students/[id]/attendance` - Student attendance history

**Features**:
- Bulk attendance marking (checkboxes)
- Status: PRESENT, ABSENT, LATE, EXCUSED
- Attendance calendar view
- Statistics: attendance rate, late count
- Parent notifications for absences

**Components**:
- AttendanceMarker (bulk checkbox grid)
- AttendanceCalendar (calendar view)
- AttendanceStats (charts)

**Testing**: Bulk update tests, date picker tests

---

### PR 3.9: Billing Pages ⏳
**Status**: Pending
**Dependencies**: PR 2.8 Invoice, PR 2.8.1 Payment (backend)
**Estimated**: 2-3 weeks

**Pages**:
- `/admin/billing/invoices` - List invoices (filter by status, student)
- `/admin/billing/invoices/[id]` - Invoice detail with items
- `/admin/billing/payments` - Payment history
- `/parent/invoices` - Parent view invoices
- `/parent/invoices/[id]/pay` - Payment page (QR code)

**Features**:
- Invoice generation from enrollment
- Payment recording (cash, bank transfer, e-wallet)
- Invoice status workflow: DRAFT → SENT → PAID/PARTIAL/OVERDUE
- QR code payment (VNPay, MoMo integration)
- Receipt generation (PDF)

**Components**:
- InvoiceForm (create/edit invoice)
- PaymentRecorder (record payment)
- QRCodePayment (display QR)
- InvoicePreview (print view)

**Testing**: Invoice workflow tests, payment tests

---

### PR 3.10: Settings & Profile Pages ⏳
**Status**: Pending
**Dependencies**: PR 2.13 Settings Module (backend)
**Estimated**: 1 week

**Pages**:
- `/admin/settings` - System settings
- `/admin/settings/users` - User management
- `/profile` - User profile & preferences
- `/profile/change-password` - Change password

**Features**:
- System settings (date format, timezone, language)
- User preferences (notifications, theme)
- Email templates customization
- Profile picture upload

**Components**:
- SettingsForm (system settings)
- ProfileForm (user profile)
- ChangePasswordForm

**Testing**: Settings update tests, profile tests

---

### PR 3.11: Parent Portal ⏳
**Status**: Pending
**Dependencies**: PR 2.9 Settings (Parent features)
**Estimated**: 2 weeks

**Pages**:
- `/parent/dashboard` - Parent dashboard
- `/parent/children` - List of children
- `/parent/children/[id]` - Child detail (attendance, grades)
- `/parent/invoices` - Invoices & payments

**Features**:
- Zalo OTP registration for parents
- Track multiple children
- View attendance & grades
- Pay invoices online
- Receive notifications

**Components**:
- ParentDashboard (overview)
- ChildCard (child summary)
- AttendanceTimeline (visual timeline)
- GradeReport (grade summary)

**Testing**: Multi-child tests, notification tests

---

## ⭐ Phase 3: V4.1 Guest-Facing Pages (NEW)

### PR 3.12: Guest Pages (Landing, Catalog, Trial) ⭐ NEW
**Status**: Pending
**Dependencies**: PR 2.9 LMS, PR 2.10 Marketing (backend)
**Estimated**: 2-3 weeks
**Priority**: 🔥 High (public-facing features)

**Pages**:
- `/[tenant]` - Landing page (dynamic per tenant)
- `/[tenant]/courses` - Course catalog
- `/[tenant]/courses/[id]` - Course detail (syllabus, teacher, CTA)
- `/[tenant]/trial/[lessonId]` - Trial lesson viewer
- `/[tenant]/contact` - Contact form

**Landing Page Features**:
- Hero section (logo, tagline, hero image)
- About Teacher section (bio, photo, achievements)
- Course Highlights (3-5 featured courses)
- Social Proof (testimonials, stats)
- Contact form inline
- CTAs: "Xem khóa học", "Học thử miễn phí"

**Course Catalog Features**:
- Browse all published courses
- Filter by category, price
- Search by name
- Sort by popularity, date
- "Học thử" badge for courses with trial lessons

**Course Detail Features**:
- Course overview (objectives, audience)
- Teacher bio with photo
- Syllabus (modules + lessons):
  - FREE TRIAL lessons marked with 🎁
  - PAID lessons marked with 🔒
- Pricing information
- CTA: "Đăng ký học thử" (prominent button)

**Trial Viewer Features**:
- Guest registers with email
- Access trial lessons (isTrial=true)
- Video player (integrate with Media Service if available)
- Lesson content (markdown/HTML rendering)
- Learning resources (PDF, slides download)
- Progress tracker (how many trial lessons completed)
- Upsell: "Đăng ký học chính thức" CTA

**Contact Form Features**:
- Name, email, phone, message
- Sends to teacher via email
- Creates Lead record in backend
- Success message: "Chúng tôi sẽ liên hệ trong 24h"

**Components**:
- LandingPageHero
- TeacherBio
- CourseHighlightCard
- TestimonialCarousel
- CourseCatalog (grid view)
- CourseCard (with trial badge)
- CourseDetailSyllabus (collapsible modules)
- TrialLessonViewer (video + content)
- ContactForm
- TrialRegistrationForm

**Testing**:
- Multi-tenant routing tests
- Trial access control tests
- Contact form submission tests
- Lead creation tests

**SEO**:
- Dynamic meta tags per tenant
- Open Graph tags for social sharing
- Structured data (Course schema)

**Reference**: UC-MKT-01, UC-MKT-02, UC-LMS-01, UC-LMS-02 in service-use-cases-v3.md

---

### PR 3.13: AI Branding System ⭐ NEW (Phase 2)
**Status**: Pending (Future Enhancement)
**Dependencies**: PR 3.12 Guest Pages, AI Service integration
**Estimated**: 2-3 weeks

**Pages**:
- `/admin/landing-page` - Landing page editor
- `/admin/landing-page/branding` - AI Branding generator

**AI Branding Features**:
- **Logo Generation**:
  - Teacher inputs: name, keywords (e.g., "Java programming")
  - AI (DALL-E) generates: 3-5 logo options
  - Teacher reviews and selects
  - Auto-crop and optimize for web

- **Tagline Generation**:
  - AI (GPT-4) generates: 5-10 tagline options
  - Based on: teacher name, specialization, target audience
  - Examples: "Học lập trình cùng Thầy Kiệt", "Master Java in 3 months"

- **Color Scheme Suggestions**:
  - AI suggests: primary, secondary, accent colors
  - Based on: education industry best practices, psychology
  - Preview: Apply colors to landing page in real-time

**Landing Page Editor Features**:
- Drag-drop section reordering
- Rich text editor for teacher bio
- Image upload (teacher photo, hero background)
- Course selection (which courses to feature)
- Live preview (side-by-side)
- Publish button (save to database)

**Components**:
- AIBrandingGenerator (wizard interface)
- LogoPicker (grid of AI-generated logos)
- TaglinePicker (list with ratings)
- ColorSchemePicker (color palette selector)
- LandingPageEditor (WYSIWYG editor)
- LivePreview (iframe with hot reload)

**API Integration**:
- POST /api/v1/ai/generate-logo
- POST /api/v1/ai/generate-taglines
- POST /api/v1/ai/suggest-colors
- PUT /api/v1/tenants/{id}/landing

**Testing**:
- AI generation mock tests
- Editor state management tests
- Live preview sync tests

**Reference**: UC-MKT-04 in service-use-cases-v3.md

---

## Phase 4: Final Polish

### PR 3.14: E2E Tests & Polish ⏳
**Status**: Pending
**Dependencies**: All features complete
**Estimated**: 2-3 weeks

**Tasks**:
- Playwright E2E tests for critical flows:
  - Student registration → Enrollment → Payment
  - Teacher marks attendance → Parent sees notification
  - Guest trial → Contact form → Lead created
- Performance optimization (lazy loading, code splitting)
- Accessibility audit (WCAG 2.1 AA)
- Mobile responsiveness polish
- Error handling improvements
- Loading states polish
- Documentation (user guide, admin guide)

**E2E Test Scenarios**:
- Happy path: Complete enrollment flow
- Error path: Invalid payment, duplicate enrollment
- Guest path: Trial lesson → Contact → Conversion
- Parent path: View child attendance, pay invoice

---

### PR 3.15: Deployment & DevOps ⏳
**Status**: Pending
**Estimated**: 1 week

**Tasks**:
- Vercel deployment configuration
- Environment variable management
- CI/CD pipeline optimization
- Performance monitoring (Vercel Analytics)
- Error tracking (Sentry integration)
- SEO optimization (sitemap, robots.txt)
- CDN configuration for static assets

---

## 📊 Summary

**Total PRs**: 15
**Completed**: 7 (47%)
**In Progress**: 0
**Pending**: 8

**By Phase**:
- Phase 1 (Infrastructure): 3/3 ✅
- Phase 2 (Admin Pages): 4/8 (50%)
- Phase 3 (Guest Pages): 0/2 (0%)
- Phase 4 (Polish): 0/2 (0%)

**Next 3 PRs**:
1. PR 3.8: Attendance Management (waiting for backend PR 2.7)
2. PR 3.12: Guest Pages (waiting for backend PR 2.9, 2.10)
3. PR 3.9: Billing Pages (waiting for backend PR 2.8, 2.8.1)

**Paired Development Status**:
- Student pages ✅ + Backend Student module ✅
- Teacher pages ✅ + Backend Teacher module ✅
- Course pages ✅ + Backend Course module ✅
- Class pages ✅ + Backend Class module ✅
- Enrollment pages ⏳ (backend in progress)

---

**Last Updated**: 2026-02-26
