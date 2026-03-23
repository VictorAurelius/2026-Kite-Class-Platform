# Claude Code Instructions

## CRITICAL: Communication Language

**ALWAYS communicate in Vietnamese (tiếng Việt)**
- All responses, explanations, and documentation should be in Vietnamese
- Code comments can be in English (standard practice)
- Commit messages should be in English (git convention)

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

## CRITICAL: Business Logic Documents

**Location:** `documents/01-business/` — SOURCE OF TRUTH cho business rules
**Quy tắc:** Xem `documents/01-business/README.md`

- Mỗi domain 1 file, ~100-150 dòng (4 sections: Rules, Flow, Emails, Config)
- Doc và code PHẢI cùng PR — đổi logic = đổi doc trong cùng commit
- KHÔNG hardcode business rules — luôn dùng config key từ doc
- TRƯỚC KHI code module mới → tạo business doc TRƯỚC (`/pre-flight-check domain`)

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
- `testing/testing-standards.md` - Spring Boot tests, frontend tests, E2E, security
- `devops/devops-standards.md` - Docker scripts, CI/CD, deployment, cloud

### Workflow
- `workflow/continue/` - /continue skill
- `workflow/check-pr/` - /check-pr skill
- `workflow/fix-pr/` - /fix-pr skill
- `workflow/start-pr/` - /start-pr skill
