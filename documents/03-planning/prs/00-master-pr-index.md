# KiteClass Master PR Index

**Mục đích**: Tổng quan tất cả PRs, tracking progress, và links đến chi tiết từng service.

> ⚠️ **Index split:** Delivery since 2026-04-14 is **gap-driven + wave-driven**, not X.Y service numbering. See [Post-V4.1 Delivery Phase](#-post-v41-delivery-phase-2026-04-14-onwards) section below for current PRs. Legacy X.Y sections below remain accurate for the **V4.1 Core Platform phase (ended 2026-03-10)**.

**Cấu trúc thư mục:**
```
documents/03-planning/
├── implementation/          # Technical plans (HOW to build)
│   ├── core-service-implementation.md
│   ├── frontend-plan.md
│   ├── gateway-implementation-plan.md
│   └── kitehub-implementation-plan.md
└── prs/                     # PR lists (WHAT to build)
    ├── 00-master-pr-index.md        # ← You are here
    ├── 01-gateway-prs.md
    ├── 02-core-prs.md
    ├── 03-frontend-prs.md
    └── 04-kitehub-prs.md
```

**Gap roadmap (primary source of truth for remaining work):** `documents/04-quality/gaps/ROADMAP.md`

---

## 🚀 Post-V4.1 Delivery Phase (2026-04-14 onwards)

**Approach change:** After V4.1 Core Platform completion (2026-03-10), KiteHub multi-tenant lifecycle + AI Branding work moved to **gap-driven delivery** (GAP-XXX) organized into **waves**. Progress primarily tracked in `documents/04-quality/gaps/ROADMAP.md`. This index mirrors the wave + PR structure for traceability.

### Waves Shipped

| Wave | Main PR(s) | Gaps Closed | Date | Status |
|------|-----------|-------------|------|:------:|
| Wave 2 — Data model foundation (7 sub-PRs) | #271-277 | GAP-009, 053, 054, 058, 007 (partial) | 2026-04-14 | ✅ |
| Wave 3 — AI Branding core pipeline (8 sub-PRs) | #284-290 | GAP-007, 008, 010, 013, 015, 031, 069, 070 | 2026-04-14 | ✅ |
| Wave 4 — Security & compliance (6 sub-PRs, parallel-agent) | #294-299 | GAP-012, 018, 041, 042, 073 | 2026-04-14 | ✅ |
| Wave 1 (post-cleanup) — Bulk import MVP | #332 | GAP-051 | 2026-04-17 | ✅ |
| Wave 2b — Parent portal identity + invitation MVP | #337 | GAP-052a | 2026-04-17 | ✅ |
| Wave 3b — AI async pipeline + fair queue Phase 1 | #341 | GAP-002, GAP-005a | 2026-04-18 | ✅ |
| Wave 4b — Branding propagation cluster | #343 | GAP-021, 032, 033p, 037 | 2026-04-18 | ✅ |

### Recent Feature PRs (standalone, not part of a wave)

| PR | Gap | Title | Date |
|----|-----|-------|------|
| #338 | — | fix(bulk-import): detect in-file duplicates | 2026-04-18 |
| #353 | GAP-100 | Lunar calendar CSV for VN holidays | 2026-04-18 |
| #354 | GAP-098 | Notification preferences persistence API | 2026-04-18 |
| #355 | GAP-099 | ClassScheduleSlot entity foundation (Phase 1) | 2026-04-18 |

### Governance / Rules / Skills PRs (meta)

Per `.claude/rules/meta-gap-priority.md`, these PRs have priority over feature PRs because they affect output quality of all future work.

| PR | Scope | Date |
|----|-------|------|
| #327 | fix(hooks): auto-stage PR log files after creation | 2026-04-17 |
| #340 | docs(guides): GitHub MCP playbook | 2026-04-18 |
| #342 | docs(rules): mcp-first-with-fallback | 2026-04-18 |
| #345 | docs(planning): 03-planning restructure + planning-docs-structure rule | 2026-04-18 |
| #346 | docs(gaps): GAP-101/102/103 docs folder governance | 2026-04-18 |
| #349 | docs(readmes): GAP-101 — README cho 4 folders thiếu | 2026-04-18 |
| #350 | docs(adr): GAP-102 Part 2 — ADR-014 async jobs queue | 2026-04-18 |
| #351 | docs(deploy): GAP-103 — deployment-strategy + ADR-015 | 2026-04-18 |
| #352 | docs(guides): GAP-102 Part 1 P2 — 3 operational guides | 2026-04-18 |
| #356 | docs(readme): refresh tech stack + docs navigation | 2026-04-18 |
| #357 | docs(roadmap): refresh gap statuses (48/103 DONE) | 2026-04-18 |
| #358 | docs(rules): meta-gap-priority MASTER RULE | 2026-04-18 |
| #359 | chore(pr-logs): backfill 18 missing PR logs + script | 2026-04-18 |

### Remaining GA Blockers (6)

Ordered per `meta-gap-priority.md` (meta-gaps first within P0):

| # | Gap | Type | Title |
|:-:|-----|:----:|-------|
| 1 | **GAP-047** | 🔴 Meta (skills) | Document generation skills (Excel/Word/PDF/PPT) |
| 2 | **GAP-046** | 🔴 Meta (rules) | Design patterns applied systematically |
| 3 | **GAP-016** | 🔴 Meta (docs) | Living docs impact scope |
| 4 | GAP-011 | Feature | Template library curation |
| 5 | GAP-014 | Feature | Wave mock plan include AI branding |
| 6 | GAP-005 | Feature | AI queue fair scheduling (Phase 2) |

**Full gap roadmap:** `documents/04-quality/gaps/ROADMAP.md` (48/103 CLOSED, Waves 1-4 shipped, Epics 5/11/12 fully closed).

---

## 📝 PR Naming Convention

**Format:**
- **Main PRs**: `X.Y` (sequential numbering)
  - Example: 1.1, 1.2, 1.3, 2.1, 2.2, 3.1, 3.2
  - Used for: Core features, main functionality

- **Enhancement PRs**: `X.Y.Z` (sub-version)
  - Example: 1.4.1 (Docker for PR 1.4 Auth), 2.8.1 (Payment enhancement to 2.8 Invoice)
  - Used for: Add-ons, improvements, extensions to existing PRs

- **Renumbered PRs**: Use next available number
  - Example: 3.14.1 (was going to be 3.13, but 3.13 moved to different service)

**Prefix by Service:**
- `1.x` = Gateway Service PRs
- `2.x` = Core Service PRs
- `3.x` = Frontend PRs
- `4.x` = KiteHub Backend Service PRs
- `5.x` = KiteHub Frontend PRs
- `6.x` = Expand Services PRs (future)

**Examples:**
- ✅ PR 1.4: JWT Authentication (main feature)
- ✅ PR 1.4.1: Docker Compose for Auth (enhancement to 1.4)
- ✅ PR 2.3: Student Module (main feature)
- ✅ PR 2.3.1: Teacher Module (closely related to 2.3)
- ✅ PR 2.8: Invoice Module (main feature)
- ✅ PR 2.8.1: VietQR Payment Integration (enhancement to 2.8)

---

## 📊 V4.1 Core Platform Progress (frozen 2026-03-10)

> **Scope:** X.Y-numbered PRs from the V4.1 Bundled Core Platform phase. Post-V4.1 delivery (2026-04-14+) is tracked in the **Post-V4.1 Delivery Phase** section above. KiteHub counts below reflect the March 2026 planning — actual KiteHub work since has been gap-driven (Waves 2-4), not X.Y-numbered.

| Service | Completed | Total | Progress | Status |
|---------|-----------|-------|----------|--------|
| **Gateway** | 10 | 10 | **100%** | 🎉 **COMPLETE** ⭐ |
| **Core** | 17 | 17 | **100%** | 🎉 **COMPLETE** ⭐ |
| **Frontend** | 14 | 14 | **100%** | 🎉 **COMPLETE** ⭐ |
| **KiteHub Backend** | 11 | 15 | **73.3%** | 📧 V4.1 plan — now tracked via gaps |
| **KiteHub Frontend** | 0 | 10 | **0%** | 📋 V4.1 plan — deferred |
| **Total (V4.1 scope)** | 52 | 66 | **78.8%** | 🎉 Core complete; KiteHub moved to gap-driven delivery |

**Major Update (March 2026)**: 🎉 **V4.1 CORE PLATFORM 100% COMPLETE!**
- **Gateway**: 10/10 PRs (100%) - Authentication, user management, rate limiting ✅
- **Core Service**: 17/17 PRs (100%) - All business modules complete ✅
- **Frontend**: 14/14 PRs (100%) - Admin + guest pages complete ✅
- **KiteHub**: NEW platform service (15 PRs planned in V4.1) — implementation continues via Waves 2-4 (gap-driven)
- **Landing Pages**: 100% complete with seed data (8 courses + LMS modules)

**Previous Milestones**:
- 11 PRs completed in one week (Core: +7, Frontend: +4)
- V4.1 Bundled Model implemented (LMS + Marketing merged into Core)

**V4.1 Phase Last Updated**: 2026-03-10. **Post-V4.1 refresh:** 2026-04-18 (see top section).

---

## 🎯 Priority PRs (Next Actions)

### 🎉 Core Platform: COMPLETE (41/41 PRs done!)
- ✅ Gateway: 10/10 PRs (100%)
- ✅ Core Service: 17/17 PRs (100%)
- ✅ Frontend: 14/14 PRs (100%)

All core functionality is production-ready! 🚀

### KiteHub Platform - HIGH PRIORITY 🚀
**Completed (11/15 - 73.3%):**
1. ✅ **PR 4.1**: Platform Core Setup & Instance Management
2. ✅ **PR 4.2**: Database Provisioning Service
3. ✅ **PR 4.3**: Trial Tracking & Expiration
4. ✅ **PR 4.4**: Subscription CRUD & Tier Management (#42)
5. ✅ **PR 4.5**: Subscription Expiration & Auto-Renewal (#43)
6. ✅ **PR 4.6**: VietQR Payment Integration (#44)
7. ✅ **PR 4.7**: Subscription Activation Hook (#45)
8. ✅ **PR 4.8**: OpenAI Integration (GPT-4 + DALL-E) (#46)
9. ✅ **PR 4.10**: Asset Storage & CDN Integration (#47)
10. ✅ **PR 4.11**: Landing Page Content Generation (#48)
11. ✅ **PR 4.12**: Email Service (AWS SES) (#49)

**Remaining (4 PRs):**
- ⏳ **PR 4.9**: AI Branding Job Queue (async, RabbitMQ, WebSocket) - COMPLEX, skip cho MVP
- ⏳ **PR 4.13**: Admin Portal API
- ⏳ **PR 4.14**: API Gateway
- ⏳ **PR 4.15**: Infrastructure & Docker

### KiteHub Frontend - NEW 🆕
**Planned (0/10 - 0%):**
- ⏳ **PR 5.1**: Project Setup & Shared Infrastructure
- ⏳ **PR 5.2**: Marketing Pages (Landing + Pricing + Auth)
- ⏳ **PR 5.3**: Customer Dashboard & Instance Management
- ⏳ **PR 5.4**: Subscription & Billing Management
- ⏳ **PR 5.5**: AI Branding Portal
- ⏳ **PR 5.6**: Customer Settings & Profile
- ⏳ **PR 5.7**: Admin Instance & Payment Management (requires Backend PR 4.13)
- ⏳ **PR 5.8**: Admin Dashboard & Analytics (requires Backend PR 4.13)
- ⏳ **PR 5.9**: Testing Suite (Unit + Component + E2E)
- ⏳ **PR 5.10**: Docker & CI/CD Integration

### Expand Service - FUTURE (Moved from Core)
**Optional enhancements (defer to Phase 2):**
- Parent Portal (3.13 renamed to EXP-2)
- Trial Learning System (1.13, 2.13-2.14, 3.14.1)
- Assignment Module (2.7.1)

---

## 📚 Service-Specific PR Lists

### Gateway Service (10/10 completed - 100%) 🎉

**Detail file**: [`01-gateway-prs.md`](./01-gateway-prs.md)

**Status**: ✅ **COMPLETE - All core features done!**
- ✅ PR 1.1-1.7: Setup, Auth, User, Email, Internal API Security
- ✅ PR 1.8-1.10: User management, security enhancements
- ✅ PR 1.12: Spring Boot 3.5.10 Upgrade
- 📝 **Note**: PR 1.13 (Trial User Auth) moved to Expand Service plan

**Key Milestones** 🎉:
- 🎯 **ALL Gateway PRs: 10/10 (100%) COMPLETE!**
- Auth system complete with JWT refresh tokens
- Email service with Thymeleaf templates
- Rate limiting (Bucket4j): 100 req/min IP, 1000 req/min user
- Internal API security with HMAC-SHA256
- Spring Boot 3.5.10 + Spring Cloud 2025.0.0
- **Tests**: 179 passing (149 unit + 30 integration), 32 skipped

**New V4.1 Phase 2**:
- TRIAL_USER role support (Migration V12)
- Magic link authentication (passwordless)
- Stricter rate limiting for trial users (30 req/min)

---

### Core Service (17/17 completed - 100%) 🎉

**Detail file**: [`02-core-prs.md`](./02-core-prs.md)

**Status**: ✅ **COMPLETE - All core features done!**
- ✅ PR 2.1-2.5: Setup, Student, Teacher, Course, Class
- ✅ PR 2.6: Enrollment Module (#15)
- ✅ PR 2.7: Attendance Module (#22)
- ✅ PR 2.7.2: Grade Module (#24)
- ✅ PR 2.8: Invoice Module (#19)
- ✅ PR 2.8.1: Payment Module (#21)
- ✅ PR 2.9: LMS Module (#23) ⭐ V4.1
- ✅ PR 2.10: Marketing Module (#29) ⭐ V4.1
- ✅ PR 2.10.1: Storage Service (#14) ⭐ V4.1
- ✅ PR 2.11-2.12: Internal APIs, Spring Boot 3.5.10
- ✅ PR 2.15-2.16: Settings, Docker Integration
- 📝 **Note**: PR 2.13-2.14 (Trial Learning) moved to Expand Service plan

**Recent Achievements (March 2026)** 🎉:
- 🎯 **ALL Core Service PRs: 17/17 (100%) COMPLETE!**
- 🎯 Core business modules: Student, Teacher, Course, Class, Enrollment, Attendance, Grade, Invoice, Payment
- 🎯 V4.1 features: LMS, Marketing, Storage - all 100% complete
- 🎯 Infrastructure: Docker, Settings, Internal APIs - all 100% complete
- 📊 Test coverage: **527+ tests passing**, 59 skipped (by design)
- 🚀 Spring Boot 3.5.10, Java 17, PostgreSQL 15

**V4.1 Features Delivered**:
- ✅ LMS Module: 3-tier structure (Course → Module → Lesson), trial access, progress tracking
- ✅ Marketing Module: Landing pages, lead management, contact forms
- ✅ Storage Service: S3/MinIO, presigned URLs, quota enforcement
- ✅ Settings Module: System settings, user preferences, feature flags

---

### Frontend (14/14 completed - 100%) 🎉

**Detail file**: [`03-frontend-prs.md`](./03-frontend-prs.md)

**Status**: ✅ **COMPLETE - All core features done!**
- ✅ PR 3.1-3.3: Infrastructure, Shared Components, Auth pages
- ✅ PR 3.4: Public Routes & Landing Pages (#30)
- ✅ PR 3.5-3.7: Student, Teacher, Course, Class pages
- ✅ PR 3.8: Frontend Testing - 164 tests, 83% coverage (#7)
- ✅ PR 3.9: Attendance Management UI (#36)
- ✅ PR 3.10: Billing & Payment System (#31)
- ✅ PR 3.11: Settings & Profile Pages (#32)
- ✅ PR 3.12: Marketing Website (#33) ⭐ V4.1
- ✅ PR 3.14: Dashboard Enhancement (#34)
- ✅ PR 3.15: E2E Tests & Polish (#36)
- 📝 **Note**: PR 3.13 (Parent Portal) moved to Expand Service plan

**Recent Achievements (March 2026)** 🎉:
- 🎯 **ALL Frontend PRs: 14/14 (100%) COMPLETE!**
- 🎯 Admin pages: Student, Teacher, Course, Class, Attendance, Billing, Settings - all done
- 🎯 Guest/Marketing pages: Landing pages, catalog, course detail, about, contact - all done
- 🎯 Infrastructure: Complete with comprehensive testing
- 📊 Test coverage: **236+ tests passing**, 58 skipped (294 total)
- 🚀 Next.js 14, TypeScript strict mode, Tailwind CSS

**V4.1 Features Delivered**:
- ✅ Public landing pages with multi-tenant routing
- ✅ Marketing website with course catalog
- ✅ Settings & profile with avatar upload (Storage integration)
- ✅ Billing system with payment workflows
- ✅ Enhanced dashboard with real data

**Pending V4.1 Phase 2 (Trial Learning)**:
- Trial dashboard with quota display
- Trial lesson viewer with restricted features
- Trial registration with magic link
- Payment/conversion flow

---

### KiteHub Frontend (0/10 planned - 0%) 📋 NEW

**Detail file**: [`05-kitehub-frontend-prs.md`](./05-kitehub-frontend-prs.md)

**Status**: 📋 **PLANNED — Customer & Admin Portal cho KiteHub**

**Mục đích:** Giao diện web để:
1. **Khách hàng B2B** — Landing page, pricing, đăng ký trial, quản lý subscription, AI branding
2. **Admin nền tảng** — Dashboard analytics, quản lý instances, xác nhận thanh toán

**Phases** (5-6 weeks):
1. ⏳ **PR 5.1-5.2**: Infrastructure + Marketing Pages (Landing, Pricing, Auth)
2. ⏳ **PR 5.3-5.6**: Customer Portal (Dashboard, Billing, Branding, Settings)
3. ⏳ **PR 5.7-5.8**: Admin Portal (Instances, Payments, Dashboard, Revenue)
4. ⏳ **PR 5.9-5.10**: Testing + Docker/CI

**Tech Stack**: Next.js 15, TypeScript, Tailwind CSS, Shadcn/UI, React Query, Recharts
**Estimated effort**: 38-47 giờ

---

### KiteHub Backend (11/15 completed - 73.3%) 🚀 IN PROGRESS

**Detail file**: [`04-kitehub-prs.md`](./04-kitehub-prs.md)

**Status**: 🚧 **ACTIVE DEVELOPMENT - Foundation complete, building AI features**

**Architecture**:
- Database per tenant (complete isolation)
- Multi-tenant platform managing KiteClass instances
- AI-powered branding (OpenAI GPT-4 Vision + DALL-E 3)
- Subscription management with VietQR payments
- RabbitMQ async job processing

**Phases** (7-8 weeks timeline):
1. ✅ **PR 4.1-4.3**: Multi-Tenant Infrastructure (3 PRs) - COMPLETE
2. ✅ **PR 4.4-4.5**: Subscription Management (2 PRs) - COMPLETE
3. ✅ **PR 4.6-4.7**: Payment Integration (2 PRs) - COMPLETE
4. ⏳ **PR 4.8-4.11**: AI Branding Service (4 PRs) ⭐ **CORE VALUE** - NEXT
5. ⏳ **PR 4.12**: Email Service (1 PR)
6. ⏳ **PR 4.13**: Admin Portal (1 PR)
7. ⏳ **PR 4.14**: API Gateway (1 PR)
8. ⏳ **PR 4.15**: Infrastructure (1 PR)

**Critical Path**: PR 4.1 → 4.2 → 4.8 → 4.9 → 4.10 (Multi-tenant + AI Branding)

**Key Features**:
- AI Branding: Auto-generate logo, colors, marketing copy from teacher input
- Trial Management: 14-day trial with quota tracking
- Subscription Tiers: FREE, BASIC (500k VNĐ/month), PREMIUM (1.5M), ENTERPRISE (custom)
- Payment Gateway: VietQR integration for subscription payments
- Instance Provisioning: Auto-create databases and deploy KiteClass instances
- Admin Portal: Manage tenants, subscriptions, usage analytics

**Tech Stack**:
- Spring Boot 3.5.10 (Core Service)
- Spring Cloud Gateway (API Gateway)
- PostgreSQL (multi-tenant metadata)
- Redis (caching, rate limiting)
- RabbitMQ (async job queue)
- OpenAI API (GPT-4 Vision, DALL-E 3)
- MinIO/S3 (asset storage)
- Next.js 14 (Admin Portal)

**Value Proposition**:
- Turn KiteClass from single-tenant to multi-tenant SaaS platform
- Enable teachers to create branded landing pages in minutes
- Monetization via subscription model
- Scale to hundreds of language centers

---

## 🎯 Architecture Version: V4.1 (Bundled Model)

**Key Changes from V4.0**:
- Core Service extended: ~650MB → ~900MB
- LMS Module merged into Core (guest learning features)
- Marketing Module merged into Core (landing page, leads)
- Bundled pricing: ₫299k all-inclusive (admin + guest features)

**Benefits**:
- Simpler architecture (3 services vs 5-7)
- Faster time to market (no LMS Service separation)
- Better customer value (more features, same price)

---

## 📖 Reference Documents

**Architecture**:
- [`system-architecture-v4.md`](../../01-research/architecture/system-architecture-v4.md) - V4.1 Bundled Model
- [`service-use-cases-v3.md`](../../01-research/services/service-use-cases-v3.md) - Use cases with LMS + Marketing

**Technical Plans**:
- [`core-service-implementation.md`](../implementation/core-service-implementation.md) - Core technical design
- [`frontend-plan.md`](../implementation/frontend-plan.md) - Frontend technical design
- [`gateway-implementation-plan.md`](../implementation/gateway-implementation-plan.md) - Gateway technical design
- [`kitehub-implementation-plan.md`](../implementation/kitehub-implementation-plan.md) - KiteHub platform technical design

**Skills**:
- `.claude/skills/` - All development guidelines and best practices

---

## 🔄 Git Workflow

**Branch Strategy**:
- `main` - Production-ready code
- `feature/gateway` - Gateway service work (MERGED)
- `feature/core` - Core service work (MERGED)
- `feature/frontend` - Frontend work (ACTIVE)
- `feature/PR-X.X-description` - Individual PR branches

**Commit Format**:
```
type(scope): description (25-50 chars)

Changes:
- Detail 1
- Detail 2
```

**Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

---

## 📝 Notes

**When starting a new PR**:
1. Check detail file (01-gateway-prs.md, 02-core-prs.md, or 03-frontend-prs.md)
2. Review dependencies (are prerequisite PRs completed?)
3. Consult technical plan in `implementation/` folder
4. Follow skills in `.claude/skills/`
5. Run tests locally before pushing to CI
6. Update progress in this index after completion

**Quality Gates** (all PRs must pass):
- ✅ 80%+ code coverage
- ✅ Zero compiler warnings
- ✅ All tests passing (local + CI)
- ✅ JavaDoc on public methods (backend)
- ✅ TypeScript strict mode (frontend)
- ✅ Git hooks passing

---

**Document Version**: 4.0 — Post-V4.1 section added, gap-driven delivery acknowledged
**Created**: 2026-02-26
**Last Updated**: 2026-04-18 (post-V4.1 refresh — waves 1-4 + meta-governance PRs indexed)

**V4.1 Phase Milestones**:
- 🎉 2026-03-09: **CORE PLATFORM 100% COMPLETE!** (Gateway 10/10, Core 17/17, Frontend 14/14)
- 🚀 2026-03-09: KiteHub Platform added (15 PRs), Landing Pages 100% complete
- 🔥 2026-03-06: 11 PRs merged in one day (biggest merge day)

**Post-V4.1 Phase Milestones (2026-04-14+)**:
- 🎯 2026-04-14: Wave 2 (data model), Wave 3 (AI branding core), Wave 4 (security/compliance) shipped — 22 sub-PRs, 18 gaps closed
- 🎯 2026-04-17: Wave 1 bulk import (#332), Wave 2b parent portal (#337), IDE warnings audit (#323)
- 🎯 2026-04-18: Wave 3b async + Phase 1 AI queue (#341), Wave 4b branding propagation (#343), docs governance burst (GAP-101/102/103), meta-gap-priority MASTER RULE (#358)
- 📊 Overall: 48/103 gaps CLOSED (47%); 6 GA blockers remaining (GAP-047 → 046 → 016 → 011 → 014 → 005)
