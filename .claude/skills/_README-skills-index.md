# Skills Index — Khi nao dung skill nao?

**Refactored:** 2026-03-23 | Tu 49 files → 20 files

---

## Quy trinh phat trien (theo thu tu)

1. `/pre-flight-check domain` — TRUOC khi bat dau module moi
2. `core/brainstorming-methodology.md` — Brainstorm cho moi PR
3. `core/task-breakdown-guide.md` — Chia tasks
4. `/pre-flight-check pr` — Check truoc khi code
5. `core/tdd-enforcement.md` — Viet test truoc
6. `backend/` hoac `frontend/` — Standards khi code
7. `core/two-stage-code-review.md` — Self-review truoc PR
8. `testing/testing-standards.md` — Verify test coverage

---

## Tat ca Skills

### Core Skills (Superpowers)
**Thu muc:** `core/`

| File | Mo ta | Dung khi |
|------|-------|----------|
| `brainstorming-methodology.md` | Quick brainstorm process | Bat dau moi PR |
| `task-breakdown-guide.md` | Task decomposition | Chia nho cong viec |
| `tdd-enforcement.md` | Test-first development | Truoc khi viet code |
| `two-stage-code-review.md` | Self-review checklist | Truoc khi tao PR |
| `systematic-debugging.md` | 4-phase debugging | Khi gap loi kho hieu |

### Check & Audit Skills
**Vi tri:** Root `skills/`

| File | Mo ta | Dung khi |
|------|-------|----------|
| `pre-flight-check.md` | 3-layer check: PR / Domain / Project | Truoc moi PR, milestone |
| `business-gap-check.md` | Business logic gap analysis | Kiem tra business coverage |
| `quality-audit/SKILL.md` | Quality scoring (100 diem) | Danh gia chat luong |
| `wave-completion-check.md` | Wave completion verification | Cuoi moi wave |

### Technical Standards
**Backend:**
| File | Mo ta |
|------|-------|
| `backend/backend-standards.md` | Code style, API design, DB, enums, errors, Maven |

**Frontend:**
| File | Mo ta |
|------|-------|
| `frontend/frontend-standards.md` | TypeScript, React, Shadcn, theme, i18n, a11y |

**Testing:**
| File | Mo ta |
|------|-------|
| `testing/testing-standards.md` | Spring Boot tests, frontend tests, E2E, performance, security |

**DevOps:**
| File | Mo ta |
|------|-------|
| `devops/devops-standards.md` | Docker scripts, CI/CD, deployment, cloud infra, env setup |

### Workflow Skills
**Thu muc:** `workflow/`

| File/Folder | Mo ta |
|-------------|-------|
| `continue/SKILL.md` | /continue — resume PR ưu tiên nhất |
| `check-pr/SKILL.md` | /check-pr — monitor CI + verify PR bằng scripts |
| `quality-plan/SKILL.md` | /quality-plan — auto-generate PR plan từ audit gaps |
| `docs-freshness/SKILL.md` | Nhắc update living docs sau mỗi PR/wave (auto, không invoke trực tiếp) |
| `check-pr/` | /check-pr skill |
| `fix-pr/` | /fix-pr skill |
| `start-pr/` | /start-pr skill |
| `development-workflow.md` | Day-to-day development workflow |
| `priority-pr-planning.md` | PR prioritization |

### Reference (doc khi can)
**Thu muc:** `reference/`

| File | Mo ta |
|------|-------|
| `architecture-overview.md` | System architecture overview |
| `cross-service-data-strategy.md` | Data sharing between services |
| `email-service.md` | Email service integration |
| `plantuml-diagrams.md` | PlantUML diagram patterns |

---

## Kiem tra chat luong

```bash
# Score ky thuat /100
/quality-audit [target]

# Score nghiep vu %
/business-gap-check [target]

# Milestone check
/pre-flight-check project
```

## Khi gap van de

```bash
# 4-phase debugging
# Xem: core/systematic-debugging.md

# CI/CD issues, Docker
# Xem: devops/devops-standards.md

# IDE warnings, test failures
# Xem: testing/testing-standards.md (section 7)
```

---

## Stats

| Metric | Truoc | Sau |
|--------|-------|-----|
| Tong so files | 49 | ~20 |
| Testing files | 9 | 1 |
| Backend files | 8 | 1 |
| Frontend files | 3 | 1 |
| DevOps files | 9 | 1 |
| Obsolete files removed | - | 11 |
