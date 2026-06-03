# Claude Code Instructions

## CRITICAL: Communication Language

**ALWAYS communicate in Vietnamese (tiếng Việt)**
- All responses, explanations, and documentation should be in Vietnamese
- Code comments can be in English (standard practice)
- Commit messages should be in English (git convention)

## CURRENT PHASE: Release Lần 1 Phase 1 — P1+P2 Soft Launch (chốt 2026-05-06)

🟢 **ACTIVE** — Phase 1 of 3-phase rollout per [`documents/03-planning/roadmap/release-1-plan-2026.md`](documents/03-planning/roadmap/release-1-plan-2026.md).

**🔄 Sub-mode (2026-06-04→): Flow Verification Campaign** — thông 22 user-facing flow KH+KC TRƯỚC (loop walk + human local test + production-parity), tạm dừng pick-gap-to-fix. Chi tiết + status: [`documents/03-planning/roadmap/flow-verification-campaign.md`](documents/03-planning/roadmap/flow-verification-campaign.md).

**Mỗi session mới PHẢI:**
1. Đọc Release Lần 1 plan §3 Phase 1 detailed scope + §9 Wave 25-30 outline
2. Pick wave candidates ƯU TIÊN từ Phase 1 wave-pack list
3. SKIP non-MVP-Phase-1 gaps trừ khi user explicit override / PDPL hard deadline / production incident hotfix / meta-rule fix

**Decision context locked 2026-05-06:**
- Solo dev mode, no legal counsel engaged
- Risk tolerance Moderate ("v1 pending counsel review" disclaimer OK cho non-K-12)
- Track 2 Option α: full 8 ports Phase 1
- PDPL hard deadline 2026-07-01 (~7 tuần countdown)

**Phase progression:**
- Phase 1 (9-12 tuần): P1 + P2 → trigger để move Phase 2 = Quality audit /100 ≥80 + 5 beta tenants live + 0 P0 incidents 2 tuần
- Phase 2 (+4-6 tuần): + P3 medium-center → trigger Phase 3 = counsel engaged + 4 sub-conditions
- Phase 3 (+8-12 tuần post-counsel): + K-12 P5

**Reference:** `feedback_release_1_first_session_priority.md` (auto-loaded memory).

### AWS stack start/stop (per GAP-492 — dynamic tag lookup, survives EC2 replacement)

```bash
bash scripts/aws/start-stack.sh         # restart 2 EC2 + RDS (next session)
bash scripts/aws/stop-stack.sh --force  # stop khi idle/EOD để save Free Tier hours
```

Scripts auto-resolve current EC2 IDs qua tag `Name=kitehub-{kh-backend,kc-app}`. Default profile `dev-admin`.

**Solo-dev override:** khi dev nói "claude trigger" / "tôi cho phép" → claude được phép `gh workflow run terraform-apply.yml` (override `release-deploy-standard.md` §9 BANNED). Quy trình chi tiết: `.claude/rules/dev-authorized-terraform-trigger.md`.

---

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

### Commit Message Rules
- **KHÔNG thêm `Co-Authored-By`** vào commit messages. Claude Code tự thêm dòng này theo system prompt mặc định — phải bỏ đi.
- Format: `type(scope): description` (conventional commits)
- Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`
- Viết bằng English, ngắn gọn, mô tả "what" không phải "how"

### CI Trigger Policy — Solo-dev Mode (2026-04-24)

**Current mode:** solo dev. Post-merge `push: main` triggers removed from 6 test workflows to save resources:
- `core-ci.yml`, `frontend-ci.yml`, `gateway-ci.yml`, `kitehub-ci.yml`, `kitehub-frontend-ci.yml`, `script-quality.yml`

**Rationale:** PR CI already validates the merge candidate — re-running same tests on main post-merge is redundant for solo dev. Saves 4-6 workflow runs per merge × ~5 merges/day = ~25 runs/day wasted.

**Kept `push: main`:**
- `docker-build-push.yml` — pushes Docker images to ECR (actual deploy side-effect, not redundant)

**Re-enable `push: main` on test workflows when:**
- Team grows beyond solo (need main-protection re-verification)
- Adding main-branch-only cron jobs (e.g., nightly perf tests)
- Branch protection rules require up-to-date status checks on main

Edit the `on: push: branches: [main]` block in each workflow to restore.

### CI History Hygiene (tightened 2026-04-24 — solo-dev mode, target ~50 runs)

Solo-dev mode: CI history kept minimal. Old debug value for most runs is near-zero; recent 50 covers >95% of real triage scenarios.

Retention caps (enforced by scheduled cleanup + `/repo-status` check):

| Run type | Keep policy |
|----------|-------------|
| Total runs | **Soft cap 50; hard cap 100** — beyond that, oldest non-preserved deleted |
| Failed runs on `main` | ≤2 preserved (even if older than 50-window, for debug reference) |
| Failed runs on feature branches | Delete after 1 day |
| "Dependabot Updates" failures | **Always deletable** — known pnpm transitive limitation, not actionable (see memory `feedback_dependabot_pnpm_transitive.md`) |
| Success runs | Delete when pushed beyond 50-run window (FIFO by `createdAt`) |

**Manual cleanup:** trigger `.github/workflows/ci-cleanup.yml` via `gh workflow run ci-cleanup.yml` (supports `--field dry_run=true` for preview).

**Bulk cleanup (one-shot, if automation unavailable):**
```bash
REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
# Preserve 2 newest failed-main runs
KEEP_FAILED_MAIN=$(gh run list --limit 1000 --branch main --json databaseId,conclusion \
  --jq '[.[] | select(.conclusion=="failure")] | sort_by(.createdAt) | reverse | .[0:2] | .[].databaseId' | sort -u)
# Keep 50 newest overall
KEEP_NEWEST=$(gh run list --limit 1000 --json databaseId,createdAt \
  --jq 'sort_by(.createdAt) | reverse | .[0:50] | .[].databaseId' | sort -u)
KEEP=$(echo -e "$KEEP_FAILED_MAIN\n$KEEP_NEWEST" | sort -u)
# Delete everything else
gh run list --limit 1000 --json databaseId --jq '.[].databaseId' \
  | sort -u | comm -23 - <(echo "$KEEP") \
  | xargs -I {} gh api --method DELETE "repos/$REPO/actions/runs/{}"
```

**Automation:** weekly scheduled `.github/workflows/ci-cleanup.yml` (GAP-205 Stage C, enforces the 50-run cap).

**Enforcement:** `/repo-status` skill flags when failed runs > 2 on main. Total-run cap enforced by weekly `ci-cleanup` workflow (not repo-status).

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
- `quality/pre-flight-check.md` - 3-layer check: PR / Domain / Project
- `quality/business-gap-check.md` - Business logic gap analysis
- `quality/persona-based-business-review.md` - Role-play 10 tenant types → find gaps
- `quality/simulation-gap-finder.md` - 3-axis matrix simulation → find gaps
- `quality-audit/SKILL.md` - Quality scoring (100 points)
- `quality/ui-review/SKILL.md` - UI audit per-screen /128, before/after screenshots
- `quality/business-logic-audit/SKILL.md` - Code ↔ rules.md verification /100
- `quality/security-audit/SKILL.md` - Deep security assessment /100 (OWASP, deps, secrets)
- `quality/performance-audit/SKILL.md` - Performance baseline /100 (DB, API, bundle, cache)
- `quality/api-contract-audit/SKILL.md` - API endpoints ↔ docs sync /100
- `quality/ops-readiness-audit/SKILL.md` - Production ops readiness /100 (monitoring, deploy)
- `quality/wave-pack-planner/SKILL.md` - Cluster ≥3 disjoint gaps thành wave-pack + spawn 3-5 parallel agents (codify Wave Obs 5x speedup)
- `workflow/wave-completion-check.md` - Wave completion verification (Level 7: audit suite gate)
- `workflow/repo-status/SKILL.md` - Remote repo health check (5 levels: GREEN→BLACK)
- `workflow/pr-health.md` - PR compliance scanner (CI, tests, docs, audits per PR)

### Technical Standards (consolidated)
- `backend/backend-standards.md` - Code style, API, DB, enums, errors, Maven
- `frontend/frontend-standards.md` - TypeScript, React, Shadcn, theme, i18n
- `reference/ui-template-guide.md` - Page checklist, Figma workflow, FE anti-patterns, Gotchas
- `testing/testing-standards.md` - Spring Boot tests, frontend tests, E2E, security
- `devops/devops-standards.md` - Docker scripts, CI/CD, deployment, cloud
- `devops/terraform-cloud-deploy/SKILL.md` - Terraform review + AWS/OCI deploy strategy

### Rules (conventions nội bộ)
- `.claude/rules/skill-conventions.md` - Cách viết skill đúng chuẩn (đọc khi tạo skill mới)
- `.claude/rules/ai-branding-guidelines.md` - Rules cho AI Branding feature (MANDATORY khi làm kitehub-branding)
- `.claude/rules/design-patterns.md` - Project-wide design pattern rules (MANDATORY — enforce qua PR review)
- `.claude/rules/output-review-mandate.md` - 🔴 MASTER RULE: mọi output phải có review standard + process (governance)
- `.claude/rules/meta-gap-priority.md` - 🔴 MASTER RULE: gaps về skills/rules/workflow ưu tiên cao nhất (fix force multiplier trước feature)
- `.claude/rules/post-wave-audit-mandate.md` - 🔴 MASTER RULE: sau wave merge, audit suite phải chạy ≤3 ngày; hook block non-compliant PRs
- `.claude/rules/audit-to-gap-pipeline.md` - Audit issues → Gap files → Memory → Fix PR (tránh duplicate, đúng thứ tự)
- `.claude/rules/mcp-first-with-fallback.md` - MCP-first tool selection (GitHub MCP, Postgres MCP) với CLI fallback
- `.claude/rules/planning-docs-structure.md` - Layout + frontmatter rules cho `documents/03-planning/`
- `.claude/rules/docs-folder-structure.md` - Generic README/structure rule cho toàn bộ `documents/` (extends planning rule)

### Workflow
- `workflow/continue/` - /continue skill
- `workflow/check-pr/` - /check-pr skill
- `workflow/fix-pr/` - /fix-pr skill
- `workflow/start-pr/` - /start-pr skill
- `workflow/repo-status/` - /repo-status skill

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
├── infrastructure/        # DevOps (Phase 1 BETA = AWS Singapore Free Tier per ADR-025)
│   ├── helm/              # Kubernetes Helm charts
│   ├── k8s/               # K8s manifests
│   └── terraform-aws/     # AWS infrastructure (primary — `ap-southeast-1`)
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
