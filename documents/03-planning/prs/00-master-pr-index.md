# KiteClass Master PR Index

**Mục đích**: Tổng quan tất cả PRs, tracking progress, và links đến chi tiết từng service.

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
- `4.x` = KiteHub Platform Service PRs
- `5.x` = Expand Services PRs (future)

**Examples:**
- ✅ PR 1.4: JWT Authentication (main feature)
- ✅ PR 1.4.1: Docker Compose for Auth (enhancement to 1.4)
- ✅ PR 2.3: Student Module (main feature)
- ✅ PR 2.3.1: Teacher Module (closely related to 2.3)
- ✅ PR 2.8: Invoice Module (main feature)
- ✅ PR 2.8.1: VietQR Payment Integration (enhancement to 2.8)

---

## 📊 Overall Progress

| Service | Completed | Total | Progress | Status |
|---------|-----------|-------|----------|--------|
| **Gateway** | 10 | 10 | **100%** | 🎉 **COMPLETE** ⭐ |
| **Core** | 17 | 17 | **100%** | 🎉 **COMPLETE** ⭐ |
| **Frontend** | 14 | 14 | **100%** | 🎉 **COMPLETE** ⭐ |
| **KiteHub** | 4 | 15 | **27%** | 🚀 **Platform Service - IN PROGRESS** |
| **Total** | 45 | 56 | **80%** | 🎉 **Core platform complete!** |

**Major Update (March 2026)**: 🎉 **CORE PLATFORM 100% COMPLETE!**
- **Gateway**: 10/10 PRs (100%) - Authentication, user management, rate limiting ✅
- **Core Service**: 17/17 PRs (100%) - All business modules complete ✅
- **Frontend**: 14/14 PRs (100%) - Admin + guest pages complete ✅
- **KiteHub**: NEW platform service with 15 PRs planned (AI Branding, Multi-tenant SaaS)
- **Landing Pages**: 100% complete with seed data (8 courses + LMS modules)

**Previous Milestones**:
- 11 PRs completed in one week (Core: +7, Frontend: +4)
- V4.1 Bundled Model implemented (LMS + Marketing merged into Core)

**Last Updated**: 2026-03-10

---

## 🎯 Priority PRs (Next Actions)

### 🎉 Core Platform: COMPLETE (41/41 PRs done!)
- ✅ Gateway: 10/10 PRs (100%)
- ✅ Core Service: 17/17 PRs (100%)
- ✅ Frontend: 14/14 PRs (100%)

All core functionality is production-ready! 🚀

### KiteHub Platform - HIGH PRIORITY 🚀
**Completed (4/15 - 27%):**
1. ✅ **PR 4.1**: Platform Core Setup & Instance Management
2. ✅ **PR 4.2**: Database Provisioning Service
3. ✅ **PR 4.3**: Trial Tracking & Expiration
4. ✅ **PR 4.4**: Subscription CRUD & Tier Management (#42)

**In Progress:**
5. 🎯 **PR 4.5**: Subscription Expiration & Auto-Renewal (2 days) ← **CURRENT**

**Critical Path (AI Branding):**
4. **PR 4.8**: KiteHub - OpenAI Integration (3 days) ⭐ **CORE VALUE**
5. **PR 4.9**: KiteHub - AI Branding Job Queue (3 days)
6. **PR 4.10**: KiteHub - Asset Storage & CDN (2 days)

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

### KiteHub Platform (0/15 completed - 0%) 🚀 NEW

**Detail file**: [`04-kitehub-prs.md`](./04-kitehub-prs.md)

**Status**: 🚀 **PLANNING PHASE - Platform-level SaaS service**

**Architecture**:
- Database per tenant (complete isolation)
- Multi-tenant platform managing KiteClass instances
- AI-powered branding (OpenAI GPT-4 Vision + DALL-E 3)
- Subscription management with VietQR payments
- RabbitMQ async job processing

**Phases** (7-8 weeks timeline):
1. ⏳ **PR 4.1-4.3**: Multi-Tenant Infrastructure (3 PRs)
2. ⏳ **PR 4.4-4.5**: Subscription Management (2 PRs)
3. ⏳ **PR 4.6-4.7**: Payment Integration (2 PRs)
4. ⏳ **PR 4.8-4.11**: AI Branding Service (4 PRs) ⭐ **CORE VALUE**
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

**Document Version**: 3.1 🎉 **CORE PLATFORM COMPLETE**
**Created**: 2026-02-26
**Last Updated**: 2026-03-10
**Major Milestones**:
- 🎉 2026-03-09: **CORE PLATFORM 100% COMPLETE!** (Gateway 10/10, Core 17/17, Frontend 14/14)
- 🚀 2026-03-09: KiteHub Platform added (15 PRs), Landing Pages 100% complete
- 🔥 2026-03-06: 11 PRs merged in one day (biggest merge day)
- Overall progress: 51% → 62% → **73%** (41/56 PRs)
- Gateway: **100%** ✅
- Core Service: 42% → **100%** (+58%)
- Frontend: 41% → **100%** (+59%)
