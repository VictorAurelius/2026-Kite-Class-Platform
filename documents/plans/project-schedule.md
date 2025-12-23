# LỊCH TRÌNH TRIỂN KHAI DỰ ÁN
## KiteClass Platform V3.1

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Dự án** | KiteClass Platform V3.1 |
| **Loại tài liệu** | Project Schedule |
| **Ngày tạo** | 23/12/2025 |
| **Phương pháp** | Agile Scrum (2-week sprints) |

---

# MỤC LỤC

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Phases và Milestones](#2-phases-và-milestones)
3. [Chi tiết Sprint](#3-chi-tiết-sprint)
4. [Gantt Chart](#4-gantt-chart)
5. [Team Structure](#5-team-structure)
6. [Risk Management](#6-risk-management)
7. [Deliverables](#7-deliverables)

---

# 1. TỔNG QUAN DỰ ÁN

## 1.1. Project Timeline

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         PROJECT TIMELINE OVERVIEW                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  PHASE 0          PHASE 1          PHASE 2          PHASE 3          PHASE 4   │
│  Discovery        Foundation       Core Features    Advanced         Launch     │
│  ────────         ──────────       ─────────────    ────────         ──────     │
│                                                                                  │
│  ┌──────┐        ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────┐   │
│  │ W1-4 │───────►│  W5-12   │────►│  W13-24  │────►│  W25-32  │────►│W33-36│   │
│  │      │        │          │     │          │     │          │     │      │   │
│  │ 1    │        │    2     │     │    3     │     │    2     │     │  1   │   │
│  │month │        │  months  │     │  months  │     │  months  │     │month │   │
│  └──────┘        └──────────┘     └──────────┘     └──────────┘     └──────┘   │
│                                                                                  │
│  TOTAL: 9 MONTHS (36 Weeks)                                                      │
│                                                                                  │
│  Key Milestones:                                                                 │
│  ★ W4:  Requirements Complete                                                    │
│  ★ W12: MVP Backend Ready                                                        │
│  ★ W18: Beta Release (Internal)                                                  │
│  ★ W24: Core Features Complete                                                   │
│  ★ W32: All Features Complete                                                    │
│  ★ W36: Production Launch                                                        │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 1.2. Summary

| Metric | Value |
|--------|-------|
| **Tổng thời gian** | 9 tháng (36 tuần) |
| **Số Sprint** | 18 sprints (2 tuần/sprint) |
| **Số Phases** | 5 phases |
| **Team Size** | 5-7 người |
| **Methodology** | Agile Scrum |

---

# 2. PHASES VÀ MILESTONES

## 2.1. Phase 0: Discovery & Planning (4 tuần)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PHASE 0: DISCOVERY & PLANNING                                 │
│                           Week 1-4 (1 tháng)                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  SPRINT 0.1 (W1-W2): Research & Survey                                           │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Hoàn thành khảo sát online (200+ responses)                                   │
│  □ Phân tích đối thủ (BeeClass, VnEdu, Azota...)                                │
│  □ Benchmark về pricing                                                          │
│  □ Xác định tech stack final                                                     │
│                                                                                  │
│  SPRINT 0.2 (W3-W4): Planning & Design                                           │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Phỏng vấn sâu 15-20 stakeholders                                              │
│  □ Tổng hợp requirements                                                         │
│  □ Thiết kế database schema                                                      │
│  □ Thiết kế API specs (OpenAPI)                                                  │
│  □ UI/UX wireframes                                                              │
│  □ Setup project structure                                                       │
│                                                                                  │
│  DELIVERABLES:                                                                   │
│  ────────────────────────────────────────────────────────────────────────────── │
│  ✓ Survey Report                                                                 │
│  ✓ Requirements Document (PRD)                                                   │
│  ✓ Database Design Document                                                      │
│  ✓ API Specification                                                             │
│  ✓ UI/UX Wireframes                                                              │
│  ✓ Project Repository Setup                                                      │
│                                                                                  │
│  MILESTONE: Requirements Complete ★                                              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.2. Phase 1: Foundation (8 tuần)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        PHASE 1: FOUNDATION                                       │
│                         Week 5-12 (2 tháng)                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  SPRINT 1.1-1.2 (W5-W8): Infrastructure & Auth                                   │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Setup CI/CD pipeline (GitHub Actions)                                         │
│  □ Setup Docker development environment                                          │
│  □ Setup PostgreSQL, Redis, RabbitMQ                                             │
│  □ Implement User Service (Auth, JWT, RBAC)                                      │
│  □ Implement Gateway layer (embedded)                                            │
│  □ Setup monitoring (Prometheus, Grafana)                                        │
│                                                                                  │
│  SPRINT 1.3-1.4 (W9-W12): Core Service Foundation                                │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Core Service: Entity models, Repositories                                     │
│  □ Core Service: Class Module (CRUD)                                             │
│  □ Core Service: Student Module (CRUD)                                           │
│  □ Core Service: Schedule Module                                                 │
│  □ Frontend: Setup Next.js project                                               │
│  □ Frontend: Auth pages (Login, Register)                                        │
│  □ Frontend: Basic dashboard layout                                              │
│                                                                                  │
│  DELIVERABLES:                                                                   │
│  ────────────────────────────────────────────────────────────────────────────── │
│  ✓ User+Gateway Service deployed                                                 │
│  ✓ Core Service with basic modules                                               │
│  ✓ CI/CD pipeline working                                                        │
│  ✓ Development environment documented                                            │
│  ✓ Basic frontend with auth                                                      │
│                                                                                  │
│  MILESTONE: MVP Backend Ready ★                                                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.3. Phase 2: Core Features (12 tuần)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                       PHASE 2: CORE FEATURES                                     │
│                        Week 13-24 (3 tháng)                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  SPRINT 2.1-2.2 (W13-W16): Learning Module                                       │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Attendance Module (Backend + Frontend)                                        │
│  □ Grades Module (Backend + Frontend)                                            │
│  □ Assignments Module (Backend + Frontend)                                       │
│  □ Teacher Dashboard                                                             │
│  □ Student Dashboard                                                             │
│                                                                                  │
│  SPRINT 2.3-2.4 (W17-W20): Billing Module                                        │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Invoice generation                                                            │
│  □ Payment tracking                                                              │
│  □ Debt management                                                               │
│  □ QR Code payment (VietQR)                                                      │
│  □ Admin billing dashboard                                                       │
│  □ Payment notifications                                                         │
│                                                                                  │
│  ★ W18: BETA RELEASE (Internal Testing)                                          │
│                                                                                  │
│  SPRINT 2.5-2.6 (W21-W24): Parent Portal & Notifications                         │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Parent registration (Zalo OTP)                                                │
│  □ Parent dashboard                                                              │
│  □ Child linking                                                                 │
│  □ Notification system (Zalo, Email)                                             │
│  □ Attendance notifications                                                      │
│  □ Payment reminders                                                             │
│                                                                                  │
│  DELIVERABLES:                                                                   │
│  ────────────────────────────────────────────────────────────────────────────── │
│  ✓ Complete Learning Module                                                      │
│  ✓ Complete Billing Module with QR                                               │
│  ✓ Parent Portal (basic)                                                         │
│  ✓ Notification System                                                           │
│  ✓ All dashboards (Admin, Teacher, Student, Parent)                              │
│                                                                                  │
│  MILESTONE: Core Features Complete ★                                             │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.4. Phase 3: Advanced Features (8 tuần)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      PHASE 3: ADVANCED FEATURES                                  │
│                        Week 25-32 (2 tháng)                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  SPRINT 3.1-3.2 (W25-W28): Engagement Service                                    │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Gamification Module                                                           │
│     - Points system                                                              │
│     - Badges & achievements                                                      │
│     - Leaderboard                                                                │
│     - Rewards redemption                                                         │
│  □ Forum Module                                                                  │
│     - Q&A forum                                                                  │
│     - Comments                                                                   │
│     - Moderation                                                                 │
│  □ Engagement Service deployment (optional)                                      │
│                                                                                  │
│  SPRINT 3.3-3.4 (W29-W32): Media Service & KiteHub                               │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Media Service                                                                 │
│     - Video upload                                                               │
│     - Transcoding (FFmpeg)                                                       │
│     - Streaming (HLS)                                                            │
│  □ KiteHub Backend                                                               │
│     - Sale Module                                                                │
│     - Provisioning Module                                                        │
│     - AI Marketing Agent                                                         │
│  □ KiteHub Frontend                                                              │
│     - Landing page                                                               │
│     - Admin dashboard                                                            │
│     - Customer portal                                                            │
│                                                                                  │
│  DELIVERABLES:                                                                   │
│  ────────────────────────────────────────────────────────────────────────────── │
│  ✓ Engagement Service complete                                                   │
│  ✓ Media Service complete                                                        │
│  ✓ KiteHub MVP                                                                   │
│  ✓ AI Marketing Agent                                                            │
│                                                                                  │
│  MILESTONE: All Features Complete ★                                              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.5. Phase 4: Launch Preparation (4 tuần)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     PHASE 4: LAUNCH PREPARATION                                  │
│                        Week 33-36 (1 tháng)                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  SPRINT 4.1 (W33-W34): Testing & Hardening                                       │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ End-to-end testing                                                            │
│  □ Performance testing                                                           │
│  □ Security audit                                                                │
│  □ Bug fixes                                                                     │
│  □ Documentation                                                                 │
│  □ User guides                                                                   │
│                                                                                  │
│  SPRINT 4.2 (W35-W36): Production Deployment                                     │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Production infrastructure setup                                               │
│  □ Data migration (if any)                                                       │
│  □ Staging deployment                                                            │
│  □ Production deployment                                                         │
│  □ Monitoring setup                                                              │
│  □ Backup & DR procedures                                                        │
│  □ Training materials                                                            │
│  □ Support processes                                                             │
│                                                                                  │
│  DELIVERABLES:                                                                   │
│  ────────────────────────────────────────────────────────────────────────────── │
│  ✓ All tests passing                                                             │
│  ✓ Security audit report                                                         │
│  ✓ Production environment ready                                                  │
│  ✓ Documentation complete                                                        │
│  ✓ Training materials                                                            │
│                                                                                  │
│  MILESTONE: Production Launch ★                                                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# 3. CHI TIẾT SPRINT

## 3.1. Sprint Planning Template

| Sprint | Dates | Goals | Story Points |
|--------|-------|-------|--------------|
| 0.1 | W1-W2 | Research & Survey | 20 |
| 0.2 | W3-W4 | Planning & Design | 25 |
| 1.1 | W5-W6 | Infrastructure Setup | 30 |
| 1.2 | W7-W8 | Auth & Gateway | 35 |
| 1.3 | W9-W10 | Core Service Foundation | 40 |
| 1.4 | W11-W12 | Frontend Foundation | 35 |
| 2.1 | W13-W14 | Attendance Module | 40 |
| 2.2 | W15-W16 | Grades & Assignments | 40 |
| 2.3 | W17-W18 | Billing Module | 45 |
| 2.4 | W19-W20 | Payment & QR | 40 |
| 2.5 | W21-W22 | Parent Portal | 40 |
| 2.6 | W23-W24 | Notifications | 35 |
| 3.1 | W25-W26 | Gamification | 40 |
| 3.2 | W27-W28 | Forum | 35 |
| 3.3 | W29-W30 | Media Service | 45 |
| 3.4 | W31-W32 | KiteHub | 45 |
| 4.1 | W33-W34 | Testing & Hardening | 30 |
| 4.2 | W35-W36 | Production Launch | 25 |

## 3.2. Sprint Detail: Sprint 2.3 (Billing Module)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SPRINT 2.3: BILLING MODULE                                    │
│                        Week 17-18 (2 weeks)                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  SPRINT GOAL: Complete invoice generation and payment tracking                   │
│                                                                                  │
│  USER STORIES:                                                                   │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  [BILL-01] As an Admin, I want to generate invoices automatically                │
│            so that I don't have to create them manually.                         │
│            Story Points: 8                                                       │
│            Tasks:                                                                │
│            - Design invoice entity and DTOs (2h)                                 │
│            - Implement invoice generation service (4h)                           │
│            - Create invoice scheduler (2h)                                       │
│            - Unit tests (2h)                                                     │
│            - Integration tests (2h)                                              │
│                                                                                  │
│  [BILL-02] As an Admin, I want to view all invoices with filters                 │
│            so that I can track payments easily.                                  │
│            Story Points: 5                                                       │
│            Tasks:                                                                │
│            - Invoice list API with pagination (3h)                               │
│            - Filter by status, date, student (2h)                                │
│            - Frontend: Invoice list page (4h)                                    │
│            - Frontend: Filter components (2h)                                    │
│                                                                                  │
│  [BILL-03] As an Admin, I want to record payments                                │
│            so that I can update invoice status.                                  │
│            Story Points: 5                                                       │
│            Tasks:                                                                │
│            - Payment recording API (2h)                                          │
│            - Partial payment support (2h)                                        │
│            - Frontend: Payment form (3h)                                         │
│            - Payment history (2h)                                                │
│                                                                                  │
│  [BILL-04] As an Admin, I want to track student debts                            │
│            so that I can follow up on unpaid invoices.                           │
│            Story Points: 5                                                       │
│            Tasks:                                                                │
│            - Debt calculation service (3h)                                       │
│            - Debt report API (2h)                                                │
│            - Frontend: Debt dashboard (4h)                                       │
│            - Export to Excel (2h)                                                │
│                                                                                  │
│  [BILL-05] As an Admin, I want to configure billing settings                     │
│            so that I can customize invoice generation.                           │
│            Story Points: 3                                                       │
│            Tasks:                                                                │
│            - Billing config entity (1h)                                          │
│            - Config API (2h)                                                     │
│            - Frontend: Settings page (3h)                                        │
│                                                                                  │
│  TOTAL STORY POINTS: 26                                                          │
│                                                                                  │
│  SPRINT CEREMONIES:                                                              │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  • Sprint Planning: Monday W17, 9:00 AM (2h)                                     │
│  • Daily Standup: Every day 9:00 AM (15 min)                                     │
│  • Sprint Review: Friday W18, 2:00 PM (1h)                                       │
│  • Sprint Retrospective: Friday W18, 3:30 PM (1h)                                │
│                                                                                  │
│  ACCEPTANCE CRITERIA:                                                            │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Invoices are auto-generated on the 1st of each month                          │
│  □ Admin can view, filter, and export invoices                                   │
│  □ Admin can record full/partial payments                                        │
│  □ Debt report shows accurate data                                               │
│  □ All APIs have unit tests (>80% coverage)                                      │
│  □ API documentation updated                                                     │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# 4. GANTT CHART

## 4.1. High-Level Gantt

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            PROJECT GANTT CHART                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Month        │ M1   │ M2   │ M3   │ M4   │ M5   │ M6   │ M7   │ M8   │ M9   │ │
│  Week         │1-4   │5-8   │9-12  │13-16 │17-20 │21-24 │25-28 │29-32 │33-36 │ │
│               │      │      │      │      │      │      │      │      │      │ │
│  ─────────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤ │
│               │      │      │      │      │      │      │      │      │      │ │
│  PHASE 0      │████  │      │      │      │      │      │      │      │      │ │
│  Discovery    │      │      │      │      │      │      │      │      │      │ │
│               │      │      │      │      │      │      │      │      │      │ │
│  PHASE 1      │      │██████│██████│      │      │      │      │      │      │ │
│  Foundation   │      │      │      │      │      │      │      │      │      │ │
│               │      │      │      │      │      │      │      │      │      │ │
│  PHASE 2      │      │      │      │██████│██████│██████│      │      │      │ │
│  Core         │      │      │      │      │      │      │      │      │      │ │
│               │      │      │      │      │      │      │      │      │      │ │
│  PHASE 3      │      │      │      │      │      │      │██████│██████│      │ │
│  Advanced     │      │      │      │      │      │      │      │      │      │ │
│               │      │      │      │      │      │      │      │      │      │ │
│  PHASE 4      │      │      │      │      │      │      │      │      │████  │ │
│  Launch       │      │      │      │      │      │      │      │      │      │ │
│               │      │      │      │      │      │      │      │      │      │ │
│  ─────────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤ │
│               │      │      │      │      │      │      │      │      │      │ │
│  MILESTONES   │  ★   │      │  ★   │      │  ★   │  ★   │      │  ★   │  ★   │ │
│               │ REQ  │      │ MVP  │      │BETA  │CORE  │      │ ALL  │LAUNCH│ │
│               │      │      │      │      │      │      │      │      │      │ │
└─────────────────────────────────────────────────────────────────────────────────┘

Legend:
████ = Active development
★    = Milestone
```

## 4.2. Detailed Module Timeline

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         MODULE DEVELOPMENT TIMELINE                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Module              │W1-4│W5-8│W9-12│W13-16│W17-20│W21-24│W25-28│W29-32│W33-36│
│  ────────────────────┼────┼────┼─────┼──────┼──────┼──────┼──────┼──────┼──────│
│                      │    │    │     │      │      │      │      │      │      │
│  KITEHUB             │    │    │     │      │      │      │      │      │      │
│  ──────              │    │    │     │      │      │      │      │      │      │
│  - Sale Module       │    │    │     │      │      │      │      │██████│      │
│  - AI Marketing      │    │    │     │      │      │      │      │██████│      │
│  - Provisioning      │    │    │     │      │      │      │      │██████│      │
│  - KiteHub Frontend  │    │    │     │      │      │      │      │██████│      │
│                      │    │    │     │      │      │      │      │      │      │
│  KITECLASS INSTANCE  │    │    │     │      │      │      │      │      │      │
│  ──────────────────  │    │    │     │      │      │      │      │      │      │
│  - User+GW Service   │    │████│█████│      │      │      │      │      │      │
│  - Core: Class       │    │    │█████│██████│      │      │      │      │      │
│  - Core: Learning    │    │    │     │██████│      │      │      │      │      │
│  - Core: Billing     │    │    │     │      │██████│      │      │      │      │
│  - Parent Portal     │    │    │     │      │      │██████│      │      │      │
│  - Notifications     │    │    │     │      │      │██████│      │      │      │
│  - Gamification      │    │    │     │      │      │      │██████│      │      │
│  - Forum             │    │    │     │      │      │      │██████│      │      │
│  - Media Service     │    │    │     │      │      │      │      │██████│      │
│  - Frontend          │    │    │█████│██████│██████│██████│██████│██████│      │
│                      │    │    │     │      │      │      │      │      │      │
│  INFRASTRUCTURE      │    │    │     │      │      │      │      │      │      │
│  ──────────────      │    │    │     │      │      │      │      │      │      │
│  - CI/CD             │    │████│     │      │      │      │      │      │      │
│  - Docker            │    │████│     │      │      │      │      │      │      │
│  - Kubernetes        │    │    │     │      │      │      │      │      │██████│
│  - Monitoring        │    │████│     │      │      │      │      │      │██████│
│                      │    │    │     │      │      │      │      │      │      │
│  QA & TESTING        │    │    │     │      │      │      │      │      │      │
│  ───────────         │    │    │     │      │      │      │      │      │      │
│  - Unit Tests        │    │░░░░│░░░░░│░░░░░░│░░░░░░│░░░░░░│░░░░░░│░░░░░░│      │
│  - Integration Tests │    │    │░░░░░│░░░░░░│░░░░░░│░░░░░░│░░░░░░│░░░░░░│      │
│  - E2E Tests         │    │    │     │      │      │      │      │      │██████│
│  - Security Audit    │    │    │     │      │      │      │      │      │██████│
│                      │    │    │     │      │      │      │      │      │      │
└─────────────────────────────────────────────────────────────────────────────────┘

Legend:
████ = Primary development
░░░░ = Ongoing/parallel
```

---

# 5. TEAM STRUCTURE

## 5.1. Team Composition

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           TEAM STRUCTURE                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                        ┌─────────────────────┐                                  │
│                        │   Product Owner     │                                  │
│                        │   (1 person)        │                                  │
│                        └──────────┬──────────┘                                  │
│                                   │                                              │
│                        ┌──────────┴──────────┐                                  │
│                        │    Tech Lead /      │                                  │
│                        │    Scrum Master     │                                  │
│                        │    (1 person)       │                                  │
│                        └──────────┬──────────┘                                  │
│                                   │                                              │
│           ┌───────────────────────┼───────────────────────┐                     │
│           │                       │                       │                     │
│  ┌────────┴────────┐    ┌────────┴────────┐    ┌────────┴────────┐            │
│  │ Backend Team    │    │ Frontend Team   │    │ DevOps          │            │
│  │ (2-3 people)    │    │ (2 people)      │    │ (1 person)      │            │
│  │                 │    │                 │    │                 │            │
│  │ • Senior BE     │    │ • Senior FE     │    │ • DevOps Eng    │            │
│  │ • Mid BE        │    │ • Mid FE        │    │                 │            │
│  │ • Junior BE     │    │                 │    │                 │            │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘            │
│                                                                                  │
│  TOTAL: 6-8 PEOPLE                                                               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.2. Role Responsibilities

| Role | Responsibilities | Time Allocation |
|------|------------------|-----------------|
| **Product Owner** | Requirements, Prioritization, Stakeholder communication | Part-time |
| **Tech Lead** | Architecture, Code review, Technical decisions | Full-time |
| **Senior Backend** | Core services, Complex features, Mentoring | Full-time |
| **Mid Backend** | Feature development, API implementation | Full-time |
| **Junior Backend** | Bug fixes, Testing, Documentation | Full-time |
| **Senior Frontend** | Architecture, Complex UI, Performance | Full-time |
| **Mid Frontend** | Feature implementation, UI components | Full-time |
| **DevOps** | CI/CD, Infrastructure, Monitoring | Full-time |

## 5.3. RACI Matrix

| Activity | PO | TL | BE Team | FE Team | DevOps |
|----------|:--:|:--:|:-------:|:-------:|:------:|
| Requirements | A | C | I | I | I |
| Architecture | C | A | R | C | C |
| Backend Dev | I | C | A/R | I | I |
| Frontend Dev | I | C | I | A/R | I |
| Code Review | I | A | R | R | C |
| Testing | C | A | R | R | I |
| Deployment | I | A | C | C | R |
| Monitoring | I | C | I | I | A/R |

*R = Responsible, A = Accountable, C = Consulted, I = Informed*

---

# 6. RISK MANAGEMENT

## 6.1. Risk Register

| ID | Risk | Probability | Impact | Score | Mitigation |
|----|------|:-----------:|:------:|:-----:|------------|
| R1 | Key developer leaves | Medium | High | 6 | Documentation, Knowledge sharing |
| R2 | Scope creep | High | Medium | 6 | Strict change control |
| R3 | Technical debt | High | Medium | 6 | Code review, Refactoring sprints |
| R4 | Integration issues | Medium | Medium | 4 | Early integration testing |
| R5 | Performance issues | Medium | High | 6 | Performance testing from Phase 2 |
| R6 | Security vulnerabilities | Low | High | 3 | Security audit, OWASP practices |
| R7 | Deployment failures | Medium | High | 6 | Staging environment, Rollback plan |
| R8 | Underestimation | High | Medium | 6 | Buffer time, Regular estimation review |

## 6.2. Risk Matrix

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              RISK MATRIX                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  IMPACT                                                                          │
│    ▲                                                                             │
│    │                                                                             │
│  H │     R6          R1, R5, R7                                                 │
│  I │                                                                             │
│  G │                                                                             │
│  H ├─────────────────────────────────────────                                   │
│    │                                                                             │
│  M │     R4          R2, R3, R8                                                 │
│  E │                                                                             │
│  D │                                                                             │
│    ├─────────────────────────────────────────                                   │
│    │                                                                             │
│  L │                                                                             │
│  O │                                                                             │
│  W │                                                                             │
│    └─────────────────────────────────────────────────────────────────────►      │
│         LOW              MEDIUM              HIGH                                │
│                       PROBABILITY                                                │
│                                                                                  │
│  Action:                                                                         │
│  • High-High (Red): Immediate action required                                    │
│  • High-Med / Med-High (Orange): Active monitoring                               │
│  • Med-Med (Yellow): Regular review                                              │
│  • Low (Green): Accept and monitor                                               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 6.3. Contingency Plans

| Risk | Trigger | Contingency Plan |
|------|---------|------------------|
| **R1: Developer leaves** | Resignation notice | Cross-training, Contractor backup |
| **R5: Performance issues** | Response time > 2s | Optimize queries, Add caching, Scale infrastructure |
| **R7: Deployment failure** | Failed deployment | Rollback to previous version within 15 minutes |
| **R8: Underestimation** | Sprint velocity < 70% | Reduce scope, Add resources, Extend timeline |

---

# 7. DELIVERABLES

## 7.1. Deliverables by Phase

| Phase | Deliverable | Type | Owner |
|-------|-------------|------|-------|
| **Phase 0** | Survey Report | Document | PO |
| | Requirements Document | Document | PO |
| | Database Design | Document | TL |
| | API Specification | Document | TL |
| | UI Wireframes | Design | FE Lead |
| **Phase 1** | User+Gateway Service | Software | BE Team |
| | Core Service (basic) | Software | BE Team |
| | Frontend (auth) | Software | FE Team |
| | CI/CD Pipeline | Infrastructure | DevOps |
| | Dev Environment | Infrastructure | DevOps |
| **Phase 2** | Learning Module | Software | BE + FE |
| | Billing Module | Software | BE + FE |
| | Parent Portal | Software | BE + FE |
| | Notification System | Software | BE Team |
| **Phase 3** | Engagement Service | Software | BE + FE |
| | Media Service | Software | BE Team |
| | KiteHub (MVP) | Software | BE + FE |
| **Phase 4** | Test Reports | Document | QA |
| | Security Audit | Document | External |
| | User Documentation | Document | TL |
| | Production System | Infrastructure | DevOps |

## 7.2. Definition of Done

### For User Stories

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         DEFINITION OF DONE                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  CODE QUALITY:                                                                   │
│  □ Code compiles without errors                                                  │
│  □ All unit tests pass                                                           │
│  □ Code coverage >= 80%                                                          │
│  □ Code review approved by at least 1 reviewer                                   │
│  □ No critical/high SonarQube issues                                             │
│                                                                                  │
│  FUNCTIONALITY:                                                                  │
│  □ All acceptance criteria met                                                   │
│  □ Integration tests pass                                                        │
│  □ Deployed to staging environment                                               │
│  □ Tested on staging by QA                                                       │
│                                                                                  │
│  DOCUMENTATION:                                                                  │
│  □ API documentation updated (OpenAPI)                                           │
│  □ Code comments for complex logic                                               │
│  □ README updated if needed                                                      │
│                                                                                  │
│  DEMO:                                                                           │
│  □ Demo-able in Sprint Review                                                    │
│  □ Product Owner accepted                                                        │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 7.3. Quality Gates

| Gate | Phase | Criteria |
|------|-------|----------|
| **G1: Architecture Review** | End Phase 0 | Architecture approved by stakeholders |
| **G2: MVP Ready** | End Phase 1 | Core services deployable, Auth working |
| **G3: Beta Ready** | Mid Phase 2 | Internal testing possible |
| **G4: Feature Complete** | End Phase 3 | All features implemented |
| **G5: Release Ready** | End Phase 4 | All tests pass, Security audit complete |

---

## 7.4. Communication Plan

| Meeting | Frequency | Participants | Purpose |
|---------|-----------|--------------|---------|
| **Daily Standup** | Daily 9:00 AM | Dev Team | Progress, Blockers |
| **Sprint Planning** | Bi-weekly Monday | All | Plan sprint |
| **Sprint Review** | Bi-weekly Friday | All + Stakeholders | Demo |
| **Retrospective** | Bi-weekly Friday | Dev Team | Process improvement |
| **Stakeholder Update** | Monthly | PO + Stakeholders | Project status |
| **Tech Sync** | Weekly | TL + Seniors | Technical decisions |

---

*Tài liệu được tạo bởi: Claude Assistant*
*Ngày: 23/12/2025*
