# Wave 10 — KiteClass 82→100 + 3-Layer Business Docs

**Date:** 2026-03-24
**Baseline:** KiteClass 82/100 (B), Business Gap 82% (37/45)
**Target:** KiteClass 100/100 (A+), Business Gap 100%, 3-Layer docs complete

---

## Business Docs Restructure

### Cấu trúc mới: mỗi domain = 1 folder, mỗi layer = 1 file

```
documents/01-business/kiteclass/{domain}/
├── rules.md          # Layer 1: Business Rules (constraints, validation, lifecycle)
├── use-cases.md      # Layer 2: Use Cases (actor, steps, errors, FE behavior)
├── api-contract.md   # Layer 3: API endpoints, request/response, error codes
└── changelog.md      # Lịch sử thay đổi (optional, tạo khi cần)
```

### 9 domains KiteClass cần chuyển + bổ sung:

| Domain | rules.md | use-cases.md | api-contract.md |
|--------|----------|-------------|-----------------|
| student-enrollment | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| course-class | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| teacher | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| attendance | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| grade-assignment | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| payment-invoice | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| gamification-points | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| notification-email | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| tenant-settings | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |

**Tổng:** 9 rules.md (migrate) + 9 use-cases.md (mới) + 9 api-contract.md (mới) = 27 files

### Layer standards:

**use-cases.md (~80-120 lines):**
```markdown
### UC-{DOMAIN}-{NN}: {Action}
- **Actor:** Teacher / Admin / Student
- **Precondition:** ...
- **Steps:**
  1. FE: hiển thị gì, filter gì
  2. User: action gì
  3. System: validate gì (reference BR-xxx)
  4. System: side effect gì
- **Postcondition:** ...
- **Errors:**
  - 400: validation fail → message
  - 403: permission denied → message
  - 409: conflict → message
- **FE Behavior:** component, UX flow, confirm dialog
```

**api-contract.md (~60-100 lines):**
```markdown
### {METHOD} /api/{resource}
**Use case:** UC-{DOMAIN}-{NN}
**Auth:** Bearer token, role required
**Request:**
```json
{ ... }
```
**Response 2xx:**
```json
{ ... }
```
**Errors:** 400/403/409 with message
```

---

## PR List

### PR-1: Restructure business docs (9 domains → folders) [CRITICAL]

**Scope:**
- [ ] Migrate 9 single-file docs → folder structure
  - `course-class.md` → `course-class/rules.md`
  - (same for all 9 domains)
- [ ] Update `documents/01-business/README.md` index
- [ ] Update cross-references trong service READMEs

### PR-2: Layer 2 — Use Cases cho 9 domains [CRITICAL]

**Yêu cầu:** Extract use cases từ actual code (Controllers, Services)

**Scope:**
- [ ] `student-enrollment/use-cases.md` — CRUD student, enroll/unenroll, transfer
- [ ] `course-class/use-cases.md` — create/edit/publish course, create class, change teacher, manage sessions
- [ ] `teacher/use-cases.md` — create teacher, assign to course/class, change status, permission checks
- [ ] `attendance/use-cases.md` — mark attendance, bulk mark, edit, view stats
- [ ] `grade-assignment/use-cases.md` — create assignment, submit, grade, calculate final
- [ ] `payment-invoice/use-cases.md` — create invoice, process payment, installment, refund
- [ ] `gamification-points/use-cases.md` — award/deduct points, leaderboard
- [ ] `notification-email/use-cases.md` — send email, contact form, admin notification
- [ ] `tenant-settings/use-cases.md` — upload logo, change theme, update branding

### PR-3: Layer 3 — API Contracts cho 9 domains [CRITICAL]

**Yêu cầu:** Extract từ actual Controllers (`@GetMapping`, `@PostMapping`, etc.)

**Scope:**
- [ ] 9 api-contract.md files — mỗi file list tất cả endpoints của domain đó
- [ ] Cross-reference UC-IDs
- [ ] Request/response JSON examples (từ actual DTOs)

### PR-4: Monitoring Stack [DevOps +5]

- [ ] `kiteclass/docker/prometheus/prometheus.yml`
- [ ] `kiteclass/docker/prometheus/alert-rules.yml` (5 rules)
- [ ] `kiteclass/docker/grafana/provisioning/datasources/prometheus.yml`
- [ ] Update `docker-compose.dev.yml` — thêm monitoring profile
- [ ] `kiteclass/scripts/monitor.sh`

### PR-5: Security Hardening [Security +3]

- [ ] Remove default passwords từ application.yml → env var fail-fast
- [ ] Tạo `kiteclass/.env.example`
- [ ] `@PostConstruct` validation cho critical configs

### PR-6: SEO + UI/UX [UI/UX +3]

- [ ] JsonLd component + tests
- [ ] OpenGraph + Twitter metadata
- [ ] Structured data cho education platform

### PR-7: CI/CD + E2E + Docs + Project Mgmt [+7 total]

- [ ] KC deploy workflow hoặc shared deploy doc (+2 CI/CD)
- [ ] Remove placeholder E2E spec (+1 E2E)
- [ ] KC architecture doc — multi-tenant isolation (+1 Documentation)
- [ ] KC phase tracking + completion checks (+3 Project Mgmt)

### PR-8: Close Business Gaps [Business Gap +8]

- [ ] Invoice overdue scheduler + test
- [ ] PointServiceTest + LeaderboardTest
- [ ] InstallmentPlanServiceTest
- [ ] SmtpEmailService stub
- [ ] Update rules.md nếu code khác docs

---

## Execution

| Agent | PRs | Scope |
|-------|-----|-------|
| 1 | PR-1 + PR-2 | Restructure + Use Cases (documents/ only) |
| 2 | PR-3 | API Contracts (documents/ + read Controllers) |
| 3 | PR-4 + PR-5 | Monitoring + Security (kiteclass/ config) |
| 4 | PR-6 + PR-7 | SEO + CI + Docs + PM |

**PR-8** thực hiện sau vì cần code changes + TDD.

---

## Score Projection

| After | Quality | Business Gap |
|-------|---------|-------------|
| Baseline | 82/100 | 82% |
| +PR-1,2,3 (3-layer docs) | 83 | 90% |
| +PR-4 (Monitoring) | 88 | 90% |
| +PR-5 (Security) | 91 | 92% |
| +PR-6 (SEO) | 94 | 92% |
| +PR-7 (CI+E2E+Docs+PM) | 100 | 95% |
| +PR-8 (Business gaps) | 100 | 100% |
