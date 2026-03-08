# KiteClass Master PR Index

**Mục đích**: Tổng quan tất cả PRs, tracking progress, và links đến chi tiết từng service.

**Cấu trúc thư mục:**
```
documents/03-planning/
├── implementation/          # Technical plans (HOW to build)
│   ├── core-service-implementation.md
│   ├── frontend-plan.md
│   └── gateway-implementation-plan.md
└── prs/                     # PR lists (WHAT to build)
    ├── 00-master-pr-index.md        # ← You are here
    ├── 01-gateway-prs.md
    ├── 02-core-prs.md
    └── 03-frontend-prs.md
```

---

## 📊 Overall Progress

| Service | Completed | Planned | Total | Progress | Status |
|---------|-----------|---------|-------|----------|--------|
| **Gateway** | 9 | 1 | 11 | 82% | ✅ Near complete |
| **Core** | 15 | 2 | 20 | 75% | 🎉 **Near complete** ⭐ |
| **Frontend** | 11 | 2 | ~20 | 65% | 🎉 **Accelerated** ⭐ |
| **Total** | 35 | 5 | ~51 | **69%** | 🚀 **Major milestone** |

**Major Update (March 2026)**: 11 PRs completed in one week!
- Core: +7 PRs (Enrollment, Attendance, Grade, Invoice, Payment, LMS, Marketing, Storage, Settings, Docker)
- Frontend: +4 PRs (Public Routes, Testing, Billing, Settings, Marketing, Dashboard)

**New in V4.1 Phase 2 (Trial Learning)**: 5 PRs added
- Gateway PR 1.13: Trial User Authentication
- Core PR 2.13-2.14: Trial Registration & Conversion
- Frontend PR 3.13-3.14.1: Trial UI & Payment Flow

**Last Updated**: 2026-03-08

---

## 🎯 Priority PRs (Next Actions)

### Immediate - Can Start Now
1. **PR 3.8.1**: Frontend - Attendance Management UI (backend ready ✅)
2. **PR 2.7.1**: Core - Assignment Module (optional, can defer)

### V4.1 Phase 2 - Trial Learning (Blocked, waiting for planning)
3. **PR 1.13**: Gateway - Trial User Authentication
4. **PR 2.13**: Core - Trial Registration & Quota Management
5. **PR 3.13**: Frontend - Trial Learning UI
6. **PR 2.14**: Core - Lead to Student Conversion
7. **PR 3.14.1**: Frontend - Lead Conversion Flow

### Polish & Finalization
8. **PR 3.16**: Frontend - E2E Tests & Polish
9. **PR 3.17**: Frontend - Deployment & DevOps

---

## 📚 Service-Specific PR Lists

### Gateway Service (9/11 completed - 82%)

**Detail file**: [`01-gateway-prs.md`](./01-gateway-prs.md)

**Status**:
- ✅ PR 1.1-1.7: Setup, Auth, User, Email, Internal API Security
- ✅ PR 1.12: Spring Boot 3.5.10 Upgrade
- ⏳ PR 1.8: UserType + ReferenceId Pattern (BLOCKED - need finalize)
- ⭐ **PR 1.13**: Trial User Authentication Support (PLANNED - V4.1 Phase 2)

**Key Milestones**:
- Auth system complete with JWT refresh tokens
- Email service with Thymeleaf templates
- Rate limiting (Bucket4j): 100 req/min IP, 1000 req/min user
- Internal API security with HMAC-SHA256
- Spring Boot 3.5.10 + Spring Cloud 2025.0.0

**New V4.1 Phase 2**:
- TRIAL_USER role support (Migration V12)
- Magic link authentication (passwordless)
- Stricter rate limiting for trial users (30 req/min)

---

### Core Service (15/20 completed - 75%) 🎉

**Detail file**: [`02-core-prs.md`](./02-core-prs.md)

**Status**: ✅ **NEAR COMPLETE - Core features done!**
- ✅ PR 2.1-2.5: Setup, Student, Teacher, Course, Class
- ✅ PR 2.6: Enrollment Module (#15)
- ✅ PR 2.7: Attendance Module (#22)
- ✅ PR 2.7.2: Grade Module (#24)
- ✅ PR 2.8: Invoice Module (#19)
- ✅ PR 2.8.1: Payment Module (#21)
- ✅ PR 2.9: LMS Module (#23) ⭐ V4.1
- ✅ PR 2.10: Marketing Module (#29) ⭐ V4.1
- ✅ PR 2.10.1: Storage Service (#14) ⭐ V4.1
- ✅ PR 2.11: Internal APIs
- ✅ PR 2.12: Spring Boot 3.5.10 Upgrade
- ✅ PR 2.15: Settings & Preferences (#26)
- ✅ PR 2.16: Docker Integration (#25)
- ⏳ PR 2.7.1: Assignment Module (optional)
- ⭐ PR 2.13-2.14: Trial Learning (PLANNED - V4.1 Phase 2)

**Recent Achievements (March 2026)**:
- 🎯 Core business modules: 100% complete
- 🎯 V4.1 features (LMS, Marketing, Storage): 100% complete
- 🎯 Infrastructure (Docker, Settings): 100% complete
- 📊 Test coverage: 400+ tests (estimated)
- 🚀 Spring Boot 3.5.11, Java 17, PostgreSQL 15

**V4.1 Features Delivered**:
- ✅ LMS Module: 3-tier structure (Course → Module → Lesson), trial access, progress tracking
- ✅ Marketing Module: Landing pages, lead management, contact forms
- ✅ Storage Service: S3/MinIO, presigned URLs, quota enforcement
- ✅ Settings Module: System settings, user preferences, feature flags

---

### Frontend (11/~20 completed - 65%) 🎉

**Detail file**: [`03-frontend-prs.md`](./03-frontend-prs.md)

**Status**: 🚀 **ACCELERATED - Major features complete!**
- ✅ PR 3.1-3.3: Infrastructure, Shared Components, Auth pages
- ✅ PR 3.4: Public Routes & Landing Pages (#30)
- ✅ PR 3.5-3.7: Teacher, Course, Class pages
- ✅ PR 3.8: Frontend Testing - 164 tests, 83% coverage (#7)
- ✅ PR 3.10: Billing & Payment System (#31)
- ✅ PR 3.11: Settings & Profile Pages (#32)
- ✅ PR 3.12: Marketing Website (#33) ⭐ V4.1
- ✅ PR 3.14: Dashboard Enhancement (#34)
- ⏳ PR 3.8.1: Attendance Management UI (backend ready)
- ⏳ PR 3.9: Assignment & Grade pages
- ⏳ PR 3.11.1: Parent Portal
- ⭐ PR 3.13-3.14.1: Trial Learning UI (PLANNED - V4.1 Phase 2)

**Recent Achievements (March 2026)**:
- 🎯 Admin pages: 88% complete (7/8)
- 🎯 Guest/Marketing pages: Complete
- 🎯 Infrastructure: Complete with comprehensive testing
- 📊 Test coverage: 164 tests, 83% coverage
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

**Document Version**: 2.0 ⭐ **Major Update**
**Created**: 2026-02-26
**Last Updated**: 2026-03-08
**Major Milestones**:
- 2026-03-06: 11 PRs merged in one day (biggest merge day)
- Overall progress: 51% → **69%** (+18%)
- Core Service: 42% → **75%** (+33%)
- Frontend: 41% → **65%** (+24%)
