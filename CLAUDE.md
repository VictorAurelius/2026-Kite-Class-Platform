# Claude Code Instructions

## CRITICAL: Communication Language

**ALWAYS communicate in Vietnamese (tiếng Việt)**
- All responses, explanations, and documentation should be in Vietnamese
- Code comments can be in English (standard practice)
- Commit messages should be in English (git convention)

## Project Overview

**Kite Platform** = 2 products sharing infrastructure:
- **KiteHub** — SaaS platform quản lý education instances (6 backend services + gateway + frontend)
- **KiteClass** — Multi-tenant education platform (core + gateway + frontend), mỗi tenant là 1 trường học

**Architecture:** KiteHub quản lý lifecycle (trial, subscription, billing, domain). KiteClass xử lý nghiệp vụ giáo dục (student, course, class, attendance, grade, payment).

**Shared infrastructure:** PostgreSQL, Redis, RabbitMQ, MinIO — tất cả dùng prefix `kite-` (KHÔNG phải `kitehub-`).

## CRITICAL: Superpowers Methodology for Every PR

**MẶC ĐỊNH: Mỗi PR PHẢI dùng Superpowers methodology**

### Quy trình bắt buộc:
1. **Quick Brainstorm** (5-10 phút)
   - Phân tích scope, risks, edge cases
   - Xác định dependencies và blockers
   - Tham khảo: `.claude/skills/core/brainstorming-methodology.md`

2. **Task Breakdown** (5-10 phút)
   - Chia nhỏ thành tasks cụ thể
   - Estimate effort cho mỗi task
   - Tham khảo: `.claude/skills/core/task-breakdown-guide.md`

3. **TDD - Test First** (cho code changes)
   - Viết tests TRƯỚC khi viết code
   - Red → Green → Refactor
   - Tham khảo: `.claude/skills/core/tdd-enforcement.md`

4. **Implementation**
   - Implement theo task breakdown
   - Commit thường xuyên

5. **Code Review** (self-review trước khi PR)
   - Tham khảo: `.claude/skills/core/two-stage-code-review.md`

### KHÔNG được bỏ qua:
- ❌ Nhảy thẳng vào code mà không brainstorm
- ❌ Viết code trước tests
- ❌ Commit mà không có tests đi kèm

## CRITICAL: Docker Scripts Required

**KHÔNG BAO GIỜ** chạy lệnh Docker trực tiếp. **LUÔN LUÔN** dùng scripts.

```bash
# ❌ WRONG
docker-compose -f docker-compose.kitehub.yml up -d

# ✅ CORRECT
./scripts/up.sh
```

**KiteHub scripts** (`kitehub/scripts/`):
- `up.sh` / `down.sh` - Start/stop stack
- `logs.sh` - View logs
- `build-all.sh` - Build all images
- `rebuild.sh` - Rebuild single service
- `status.sh` - Check status
- `exec.sh` - Run command in container
- `clean.sh` - Cleanup resources
- `help.sh` - Show all commands

Tham khảo: `.claude/skills/devops/devops-standards.md` (section Docker Scripts)

## Git Workflow

- **ALWAYS** create feature branch before changes
- **NEVER** commit directly to main
- **Branch naming:** `feature/PR-{number}-{description}`
- Test locally before pushing to CI
- Use `./scripts/test-local.sh` for testing

## CRITICAL: Wave Branch Strategy

**MỌI thay đổi PHẢI qua PR**, KHÔNG merge trực tiếp vào main.

```bash
# ✅ CORRECT workflow
git checkout -b wave/X          # hoặc feature/description
# ... làm việc ...
git push -u origin wave/X
gh pr create --base main        # Tạo PR
gh pr merge N --squash          # Squash merge sau review

# ❌ WRONG
git checkout main && git merge wave/X && git push  # KHÔNG!
```

**Wave strategy cho parallel work:**
1. Tạo `wave/X` branch từ main
2. Agents làm song song (worktree isolation)
3. Cherry-pick commits vào wave/X
4. Tạo PR: wave/X → main (squash merge)
5. Quality check TRƯỚC khi merge

## CRITICAL: Business Logic Documents — 3-Layer Structure

**Location:** `documents/01-business/` — SOURCE OF TRUTH cho business rules
**Skill chi tiết:** `.claude/skills/reference/business-docs-3-layer.md`

### Cấu trúc: mỗi domain = 1 folder, 3 files

```
documents/01-business/{project}/{domain}/
├── rules.md          # Layer 1: Business Rules (constraints, config keys)
├── use-cases.md      # Layer 2: Use Cases (actor, steps, errors, FE behavior)
└── api-contract.md   # Layer 3: API Contract (endpoint, request/response, error codes)
```

### Rules bắt buộc:
- 3 files per domain — pre-commit hook sẽ warn nếu thiếu
- Doc và code PHẢI cùng PR — đổi logic = đổi doc trong cùng commit
- KHÔNG hardcode business rules — luôn dùng config key từ rules.md
- TRƯỚC KHI code module mới → tạo 3 files TRƯỚC (`/pre-flight-check domain`)
- Verification chain: `BR-xxx → UC-xxx → endpoint → @Mapping → @Test`

## CRITICAL: Living Documents

Các docs sau PHẢI update liên tục theo mỗi PR/wave:

| Doc | Update khi |
|-----|-----------|
| `README.md` | Thêm/xóa folder, service, tech stack |
| `CLAUDE.md` | Thay đổi quy trình, convention, skill |
| `documents/01-business/*.md` | Thay đổi business logic, config |
| `documents/01-business/README.md` | Thêm/xóa business doc |
| `.claude/skills/_README-skills-index.md` | Thêm/xóa/rename skill |

**Rule:** Nếu PR thay đổi business logic nhưng KHÔNG update business doc → PR KHÔNG đạt quality check.

## Skills Reference

Index đầy đủ: `.claude/skills/_README-skills-index.md`

### Core Skills (Superpowers — dùng mỗi PR)
- `core/brainstorming-methodology.md` - Quick brainstorm process
- `core/task-breakdown-guide.md` - Task decomposition
- `core/tdd-enforcement.md` - Test-first development
- `core/two-stage-code-review.md` - Self-review checklist
- `core/systematic-debugging.md` - 4-phase debugging

### Check & Audit
- `pre-flight-check.md` - 3-layer check: PR / Domain / Project
- `business-gap-check.md` - Business logic gap analysis
- `quality-audit/SKILL.md` - Quality scoring (100 points)
- `wave-completion-check.md` - Wave completion verification

### Technical Standards (consolidated)
- `backend/backend-standards.md` - Code style, API, DB, enums, errors, Maven
- `frontend/frontend-standards.md` - TypeScript, React, Shadcn, theme, i18n
- `reference/ui-template-guide.md` - Page checklist, Figma workflow, FE anti-patterns, Gotchas
- `testing/testing-standards.md` - Spring Boot tests, frontend tests, E2E, security
- `devops/devops-standards.md` - Docker scripts, CI/CD, deployment, cloud

### Rules (conventions nội bộ)
- `.claude/rules/skill-conventions.md` - Cách viết skill đúng chuẩn (đọc khi tạo skill mới)

### Workflow
- `workflow/continue/` - /continue skill
- `workflow/check-pr/` - /check-pr skill
- `workflow/fix-pr/` - /fix-pr skill
- `workflow/start-pr/` - /start-pr skill

## Project Folder Structure

```
2026-Kite-Class-Platform/
├── .claude/               # Skills, scripts, hooks
├── .github/               # CI/CD workflows (8 files)
├── documents/             # Documentation
│   ├── 01-business/       # Business logic (SOURCE OF TRUTH)
│   ├── 02-architecture/   # Technical architecture
│   ├── 03-planning/       # Plans, PRs, strategies
│   ├── 04-quality/        # Audits, gap checks
│   ├── 05-guides/         # Deploy guides, operations
│   ├── 06-diagrams/       # PlantUML + rendered PNG
│   ├── 07-archived/       # Old docs, research
│   └── 08-thesis/         # Graduation project refs
├── infrastructure/        # DevOps
│   ├── helm/              # Kubernetes Helm charts
│   ├── k8s/               # K8s manifests
│   ├── terraform-aws/     # AWS infrastructure
│   └── terraform-oracle/  # Oracle Cloud
├── kiteclass/             # KiteClass (core + gateway + frontend)
├── kitehub/               # KiteHub (6 services + gateway + frontend)
└── scripts/               # Root CI/QA scripts
```

## Docker Naming Convention

| Prefix | Dùng cho | Ví dụ |
|--------|---------|-------|
| `kite-` | Shared infrastructure | `kite-postgres`, `kite-redis`, `kite-gateway` |
| `kitehub-` | KiteHub services | `kitehub-subscription`, `kitehub-branding` |
| `kiteclass-` | KiteClass services | `kiteclass-core`, `kiteclass-frontend` |

**Canonical compose file:** `kitehub/docker-compose.kitehub.yml`
