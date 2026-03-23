# Documents & Skills Refactor Plan

**Ngày:** 2026-03-23
**Vấn đề:** 180 docs (đánh số trùng), 49 skills (35K dòng, nhiều overlap)
**Mục tiêu:** Cấu trúc gọn, rõ, tham chiếu dễ

---

## A. Documents Refactor

### Hiện trạng — Đánh số trùng

```
01-business      ← MỚI tạo
01-research      ← TRÙNG số 01
02-academic      ← TRÙNG số 02
02-architecture  ← TRÙNG số 02
04-implementation ← TRÙNG số 04
04-operations    ← TRÙNG số 04
06-compliance    ← TRÙNG số 06
06-diagrams      ← TRÙNG số 06
06-logs          ← TRÙNG số 06
07-archived      ← TRÙNG số 07
07-guides        ← TRÙNG số 07
```

### Cấu trúc mới

```
documents/
├── README.md                    ← Index + hướng dẫn
├── 01-business/                 ← Business logic (SOURCE OF TRUTH)
│   ├── kitehub/
│   └── kiteclass/
├── 02-architecture/             ← Technical architecture
├── 03-planning/                 ← Plans, PRs, roadmaps
├── 04-quality/                  ← Audits, gap checks (gộp 05-qa)
├── 05-guides/                   ← Hướng dẫn tiếng Việt, deploy
├── 06-diagrams/                 ← PlantUML, rendered images
├── 07-archived/                 ← Cũ, outdated, reference only
│   ├── research/                ← Từ 01-research
│   ├── academic/                ← Từ 02-academic
│   ├── implementation/          ← Từ 04-implementation
│   ├── old-plans/
│   └── logs/                    ← Từ 06-logs
└── action-1.md                  ← Scratch notes
```

### Migration mapping

| Cũ | Mới | Action |
|----|-----|--------|
| `01-research/` | `07-archived/research/` | Move (reference, ít dùng) |
| `02-academic/` | `07-archived/academic/` | Move (báo cáo thực tập, không code-related) |
| `02-architecture/` | `02-architecture/` | Giữ nguyên |
| `04-implementation/` | `07-archived/implementation/` | Move (code-analysis, pr-reviews) |
| `04-operations/` | `05-guides/operations/` | Move |
| `05-qa-and-best-practices/` | `04-quality/` | Rename |
| `06-compliance/` | `07-archived/compliance/` | Move (ít dùng) |
| `06-diagrams/` | `06-diagrams/` | Giữ nguyên |
| `06-logs/` | `07-archived/logs/` | Move |
| `07-guides/` | `05-guides/` | Move |

### Migrate kiteclass-core/docs → documents/01-business/

| Cũ | Mới |
|----|-----|
| `kiteclass-core/docs/modules/course-module-business-logic.md` (1015 dòng) | `documents/01-business/kiteclass/course.md` (~120 dòng, rewrite compact) |
| `kiteclass-core/docs/modules/student-module-business-logic.md` | `documents/01-business/kiteclass/student-enrollment.md` |
| ... (11 module docs) | Gộp thành ~8 compact docs |

**Cũ giữ lại** trong `kiteclass-core/docs/` nhưng thêm note:
```
> ⚠️ DEPRECATED: Xem `documents/01-business/kiteclass/` cho phiên bản cập nhật.
```

---

## B. Skills Refactor

### Hiện trạng — 49 files, 35K dòng

**Vấn đề:**
- 10 testing skills (trùng nhau 60%+)
- 5 CI/CD skills (trùng nhau)
- 8 IDE/Quality skills (trùng nhau)
- 4 frontend skills (trùng nhau)
- Không có index, không biết skill nào dùng khi nào
- Nhiều skill >1000 dòng (quá dài để đọc)

### Cấu trúc mới — Gộp thành 3 tầng

```
.claude/skills/
├── README.md                        ← INDEX: mô tả mỗi skill 1 dòng + khi nào dùng
│
├── core/                            ← 5 core skills (Superpowers)
│   ├── brainstorming.md             ← Gộp brainstorming-methodology
│   ├── task-breakdown.md            ← Gộp task-breakdown-guide
│   ├── tdd.md                       ← Gộp tdd-enforcement
│   ├── code-review.md               ← Gộp two-stage-code-review
│   └── debugging.md                 ← Gộp systematic-debugging
│
├── check/                           ← 4 check/audit skills
│   ├── pre-flight-check.md          ← 3-layer check (giữ nguyên)
│   ├── business-gap-check.md        ← Gap analysis (giữ nguyên)
│   ├── quality-audit/SKILL.md       ← Quality scoring (giữ nguyên)
│   └── ide-problem-check.md         ← IDE warnings (giữ nguyên)
│
├── backend/                         ← 1 file gộp (thay 10+ files)
│   └── backend-standards.md         ← Gộp: code-style + error-logging
│                                       + enums-constants + api-design
│                                       + spring-boot-testing + database-design
│                                       + maven-dependencies
│
├── frontend/                        ← 1 file gộp (thay 4 files)
│   └── frontend-standards.md        ← Gộp: frontend-development
│                                       + frontend-code-quality
│                                       + frontend-testing-requirements
│
├── testing/                         ← 1 file gộp (thay 10 files)
│   └── testing-standards.md         ← Gộp: testing-guide + e2e-testing
│                                       + security-testing + performance-testing
│                                       + kiteclass-backend-testing-patterns
│                                       + kiteclass-frontend-testing-patterns
│                                       + spring-boot-testing-quality
│                                       + ide-testcontainers-warnings
│
├── devops/                          ← 1 file gộp (thay 5 files)
│   └── devops-standards.md          ← Gộp: ci-cd-best-practices
│                                       + ci-cd-quality-enforcement
│                                       + ci-cleanup-workflow
│                                       + deployment-quality-standards
│                                       + docker-scripts-required
│                                       + cloud-infrastructure
│
├── workflow/                        ← Workflow skills
│   ├── continue/SKILL.md            ← /continue skill
│   ├── development-workflow.md      ← Giữ (trim to <300 dòng)
│   └── priority-pr-planning.md      ← Giữ
│
└── reference/                       ← Ít dùng, tra cứu khi cần
    ├── architecture-overview.md
    ├── cross-service-data-strategy.md
    ├── email-service.md
    ├── environment-setup.md
    └── plantuml-diagrams.md
```

### Skills reduction

| Category | Cũ | Mới | Giảm |
|----------|-----|-----|------|
| Core (Superpowers) | 5 | 5 | 0 |
| Check/Audit | 4 | 4 | 0 |
| Backend | 7+ | 1 | -6 |
| Frontend | 4 | 1 | -3 |
| Testing | 10 | 1 | -9 |
| DevOps | 5+ | 1 | -4 |
| Workflow | 3 | 3 | 0 |
| Reference | 5+ | 5 | 0 |
| Removed (obsolete) | 10+ | 0 | -10 |
| **Total** | **49** | **~20** | **-29** |

### Skills to REMOVE (obsolete/duplicate)

| Skill | Reason |
|-------|--------|
| `_README-quality-docs-status.md` | Outdated status file |
| `documentation-structure.md` | Replaced by documents/README.md |
| `log-management.md` | Merged into devops-standards |
| `organize.md` | Vague, not actionable |
| `project-schedule.md` | Outdated |
| `required-knowledge.md` | Reference, not skill |
| `setup-github-cli.md` | One-time setup, not recurring |
| `skills-compliance-checklist.md` | Merged into pre-commit hook |
| `spring-boot-upgrade-checklist.md` | One-time, move to reference |
| `troubleshooting.md` | Merged into debugging.md |
| `ide-warnings-best-practices.md` | Merged into ide-problem-check |

### README.md cho skills (INDEX)

```markdown
# Skills — Khi nào dùng skill nào?

## Quy trình phát triển (theo thứ tự)
1. `/pre-flight-check domain` — TRƯỚC khi bắt đầu module mới
2. `core/brainstorming.md` — Brainstorm cho mỗi PR
3. `core/task-breakdown.md` — Chia tasks
4. `/pre-flight-check pr` — Check trước khi code
5. `core/tdd.md` — Viết test trước
6. `backend/` hoặc `frontend/` — Standards khi code
7. `core/code-review.md` — Self-review trước PR
8. `testing/testing-standards.md` — Verify test coverage

## Kiểm tra chất lượng
- `/quality-audit [target]` — Score kỹ thuật /100
- `/business-gap-check [target]` — Score nghiệp vụ %
- `/pre-flight-check project` — Milestone check

## Khi gặp vấn đề
- `core/debugging.md` — 4-phase debugging
- `check/ide-problem-check.md` — IDE warnings
- `devops/devops-standards.md` — CI/CD, Docker

## Tra cứu
- `reference/` — Architecture, email, infra (đọc khi cần)
```

---

## C. Execution

### PR-REFACTOR-1: Documents restructure

**Estimate:** 1 giờ
**Scope:**
- [ ] Rename/move folders theo mapping
- [ ] Tạo `documents/README.md` index
- [ ] Add deprecation note vào `kiteclass-core/docs/`
- [ ] Verify: không có broken references

### PR-REFACTOR-2: Skills consolidation

**Estimate:** 2 giờ
**Scope:**
- [ ] Tạo folder structure mới
- [ ] Gộp 10 testing files → 1 `testing-standards.md`
- [ ] Gộp 7 backend files → 1 `backend-standards.md`
- [ ] Gộp 4 frontend files → 1 `frontend-standards.md`
- [ ] Gộp 5 devops files → 1 `devops-standards.md`
- [ ] Move core skills vào `core/`
- [ ] Xóa obsolete skills (11 files)
- [ ] Tạo `README.md` index
- [ ] Update CLAUDE.md references

### PR-REFACTOR-3: Business docs migration

**Estimate:** 2 giờ
**Scope:**
- [ ] Rewrite 11 kiteclass module docs → 8 compact docs (100-150 dòng each)
- [ ] Tạo KiteHub business docs (trial, subscription, email, domain, retention)
- [ ] Update index

**Tổng: 3 PRs, ~5 giờ**
