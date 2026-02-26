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

| Service | Completed | Total | Progress | Status |
|---------|-----------|-------|----------|--------|
| **Gateway** | 9 | 10 | 90% | ✅ Near complete |
| **Core** | 8 | 17 | 47% | 🔄 Active development |
| **Frontend** | 7 | 15 | 47% | 🔄 Active development |
| **Total** | 24 | 42 | 57% | 🔄 On track |

**Last Updated**: 2026-02-26

---

## 🎯 Priority PRs (Next 3)

1. **PR 2.6**: Core - Enrollment Module (dependencies met: Student ✅, Class ✅)
2. **PR 2.7**: Core - Attendance Module
3. **PR 3.8**: Frontend - Attendance Management Pages

---

## 📚 Service-Specific PR Lists

### Gateway Service (9/10 completed - 90%)

**Detail file**: [`01-gateway-prs.md`](./01-gateway-prs.md)

**Status**:
- ✅ PR 1.1-1.7: Setup, Auth, User, Email, Internal API Security
- ✅ PR 1.12: Spring Boot 3.5.10 Upgrade
- ⏳ PR 1.8: UserType + ReferenceId Pattern (BLOCKED - need finalize)

**Key Milestones**:
- Auth system complete with JWT refresh tokens
- Email service with Thymeleaf templates
- Rate limiting (Bucket4j): 100 req/min IP, 1000 req/min user
- Internal API security with HMAC-SHA256
- Spring Boot 3.5.10 + Spring Cloud 2025.0.0

---

### Core Service (8/17 completed - 47%)

**Detail file**: [`02-core-prs.md`](./02-core-prs.md)

**Status**:
- ✅ PR 2.1-2.5: Setup, Student, Teacher, Course, Class
- ✅ PR 2.11: Internal APIs for Gateway
- ✅ PR 2.12: Spring Boot 3.5.10 Upgrade
- ⏳ PR 2.6: Enrollment Module (NEXT PRIORITY)
- ⏳ PR 2.7-2.10: Attendance, Assignment, Grade, Billing
- ⭐ **PR 2.9**: LMS Module (NEW - V4.1)
- ⭐ **PR 2.10**: Marketing Module (NEW - V4.1)

**Key Milestones**:
- Multi-tenant architecture with Hibernate filters
- Soft delete pattern across all entities
- 292 tests passing (260 unit + 32 integration)
- Spring Boot 3.5.11

**New V4.1 Features**:
- LMS Module: Course structure (Modules → Lessons), trial access, progress tracking
- Marketing Module: Landing page API, Lead management, Contact forms

---

### Frontend (7/15 completed - 47%)

**Detail file**: [`03-frontend-prs.md`](./03-frontend-prs.md)

**Status**:
- ✅ PR 3.1-3.7: Infrastructure, Auth, Student, Teacher, Course, Class pages
- ⏳ PR 3.8-3.11: Attendance, Billing, Settings, Parent portal
- ⭐ **PR 3.12**: Guest Pages (NEW - V4.1)
- ⭐ **PR 3.13**: AI Branding System (NEW - V4.1)
- ⏳ PR 3.14: E2E Tests & Polish

**Key Milestones**:
- TypeScript strict mode, no `any` types
- React Query for data fetching
- Zustand for global state
- Radix UI + Tailwind CSS
- Feature gates for tier-based features

**New V4.1 Features**:
- Guest Pages: Landing page, Course catalog, Trial viewer, Contact form
- AI Branding: Logo/tagline generator, color scheme suggestions

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

**Document Version**: 1.0
**Created**: 2026-02-26
**Last Updated**: 2026-02-26
