# Documents Structure v2 — Clean + Thesis Ready

**Ngày:** 2026-03-24
**Mục tiêu:** Clean 03-planning, fix vietnamese, thêm diagrams, thesis reference

---

## 1. Fix 03-planning (68 files → organized)

### Hiện trạng: 19 files ở root + 8 subdirs

Move root files vào subdirs phù hợp:

| File ở root | Move to |
|------------|---------|
| `ai-local-implementation-plan.md` | `implementation/` |
| `bao-cao-chong-spam-dang-ky.md` | `quality/` (báo cáo) |
| `docs-and-skills-refactor-plan.md` | `quality/` ✅ DONE |
| `kiteclass-quality-improvement-plan.md` | `quality/` |
| `kiteclass-theme-system-design.md` | `implementation/` |
| `kitehub-onboarding-security-gaps.md` | `quality/` |
| `kitehub-onboarding-security-plan.md` | `implementation/` |
| `kitehub-quality-100-plan.md` | `quality/` ✅ DONE |
| `kitehub-quality-improvement-plan-v2.md` | `quality/` ✅ DONE |
| `kitehub-quality-improvement-plan-v3.md` | `quality/` ✅ DONE |
| `kitehub-saas-best-practices-analysis.md` | GIỮ root (cross-cutting) |
| `kitehub-saas-implementation-plan.md` | GIỮ root (master plan) |
| `local-e2e-roadmap.md` | `testing/` |
| `local-prod-audit-report.md` | `quality/` |
| `local-prod-separation-plan.md` | `infrastructure/` |
| `parallel-execution-strategy.md` | GIỮ root (active reference) |
| `quality-plan-v4-final-push.md` | `quality/` |
| `superpowers-integration-implementation-plan.md` | `07-archived/old-plans/` |
| `ui-refactor-plan.md` | `implementation/` |
| `docs-structure-v2-plan.md` | GIỮ root (this file) |

**Kết quả root:** ~4 files (master plan, parallel strategy, SaaS analysis, this)

---

## 2. Fix 05-guides/vietnamese duplicate

```
Hiện tại:
05-guides/vietnamese/ENVIRONMENT-MATRIX.md
05-guides/vietnamese/LOCAL-DEV.md
05-guides/vietnamese/PRODUCTION-DEPLOY.md
05-guides/vietnamese/SECRET-MANAGEMENT.md
05-guides/vietnamese/vietnamese/           ← DUPLICATE NESTED
  huong-dan-deploy-oracle-cloud.md
  huong-dan-testing-100-percent.md
  huong-dan-trien-khai-production.md

Fix: Flatten
05-guides/vietnamese/
  ENVIRONMENT-MATRIX.md
  LOCAL-DEV.md
  PRODUCTION-DEPLOY.md
  SECRET-MANAGEMENT.md
  huong-dan-deploy-oracle-cloud.md        ← move up
  huong-dan-testing-100-percent.md        ← move up
  huong-dan-trien-khai-production.md      ← move up
```

---

## 3. Thêm Diagrams

### Diagrams hiện có (11 PlantUML)
- Architecture: 01, 04, 05, 08
- ERD: 03, 07
- Business flow: 02, 06
- Provisioning: 09

### Diagrams CẦN thêm cho đồ án

| # | Diagram | Type | Mục đích |
|---|---------|------|----------|
| 10 | SaaS Multi-tenant Architecture | Component | Tổng quan KiteHub + KiteClass |
| 11 | Email Lifecycle Flow | Sequence | 13 emails trigger timeline |
| 12 | Trial → Payment → Retention Flow | Activity | Full customer lifecycle |
| 13 | Domain Resolution Flow | Sequence | Subdomain + custom domain |
| 14 | AI Branding Pipeline | Activity | Template → AI → Theme |
| 15 | CI/CD Pipeline | Deployment | GitHub Actions → Docker → Deploy |
| 16 | Database Schema (full) | ERD | Tất cả entities KiteHub + KiteClass |
| 17 | Wave Execution Process | Activity | Pre-check → Agents → Merge → Check |
| 18 | Class Diagram — Core Modules | Class | 15 KiteClass modules relationship |
| 19 | Use Case Diagram | Use Case | Actor: Student, Teacher, Admin, Owner |

**Format:** PlantUML (.puml) → render PNG vào `06-diagrams/rendered/`

---

## 4. Thesis Reference — Đồ Án Tốt Nghiệp

### Tạo section `08-thesis/`

```
documents/08-thesis/
├── README.md                    ← Index + mapping
├── chapter-mapping.md           ← Map docs → thesis chapters
├── references/                  ← Tham chiếu
│   ├── technology-stack.md      ← Danh sách công nghệ + lý do chọn
│   ├── methodology.md           ← Superpowers, Agile, TDD
│   ├── testing-results.md       ← Tổng hợp test results
│   ├── quality-metrics.md       ← Quality audit summary
│   └── deployment-guide.md      ← Hướng dẫn triển khai
└── figures/                     ← Diagrams cho thesis
    └── (symlink to 06-diagrams/rendered/)
```

### Chapter Mapping (đề cương → documents)

| Chương đồ án | Documents tham chiếu |
|-------------|---------------------|
| Chương 1: Giới thiệu | `01-business/`, `07-archived/research/competitive/` |
| Chương 2: Cơ sở lý thuyết | `07-archived/research/technology/`, `08-thesis/references/technology-stack.md` |
| Chương 3: Phân tích yêu cầu | `01-business/`, `06-diagrams/` (use case, ERD) |
| Chương 4: Thiết kế hệ thống | `02-architecture/`, `06-diagrams/` (architecture, class) |
| Chương 5: Triển khai | `05-guides/`, `03-planning/kitehub-saas-implementation-plan.md` |
| Chương 6: Kiểm thử | `04-quality/`, `08-thesis/references/testing-results.md` |
| Chương 7: Kết luận | `04-quality/quality-audit-2026-03-24-*.md` |

### Thesis References cần tạo

| Doc | Nội dung | Từ đâu |
|-----|----------|--------|
| `technology-stack.md` | Spring Boot 3.5, Next.js 15, PostgreSQL, Redis, Docker, Ollama | Extract từ pom.xml + package.json |
| `methodology.md` | Superpowers, 3-layer pre-flight, wave execution, TDD | Extract từ skills |
| `testing-results.md` | KH 48 tests, KC 98 tests, 5 waves, CI metrics | Extract từ wave reports |
| `quality-metrics.md` | Score timeline: 77→91→96, 78→93 | Extract từ audits |
| `deployment-guide.md` | Oracle Cloud, Docker Compose, Terraform | Extract từ infrastructure docs |

---

## Execution

### PR-DOCS-1: Clean 03-planning + fix vietnamese

**Estimate:** 1 giờ
**Scope:**
- [ ] Move 15 files từ 03-planning root → subdirs
- [ ] Flatten 05-guides/vietnamese/vietnamese/
- [ ] Update cross-references

### PR-DOCS-2: Add 10 diagrams

**Estimate:** 3 giờ
**Scope:**
- [ ] Tạo 10 PlantUML files (#10-#19)
- [ ] Render PNG (nếu có PlantUML CLI)
- [ ] Link vào architecture docs

### PR-DOCS-3: Thesis reference section

**Estimate:** 2 giờ
**Scope:**
- [ ] Tạo 08-thesis/ structure
- [ ] Tạo chapter-mapping.md
- [ ] Tạo 5 reference docs
- [ ] Link figures to diagrams

## Completion Status

| PR | Status |
|----|--------|
| PR-DOCS-1 Clean planning + vietnamese | ⬜ TODO |
| PR-DOCS-2 Add diagrams | ⬜ TODO |
| PR-DOCS-3 Thesis references | ⬜ TODO |
| **Total** | **0/3** |
