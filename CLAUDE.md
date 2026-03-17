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
   - Tham khảo: `.claude/skills/brainstorming-methodology.md`

2. **Task Breakdown** (5-10 phút)
   - Chia nhỏ thành tasks cụ thể
   - Estimate effort cho mỗi task
   - Tham khảo: `.claude/skills/task-breakdown-guide.md`

3. **TDD - Test First** (cho code changes)
   - Viết tests TRƯỚC khi viết code
   - Red → Green → Refactor
   - Tham khảo: `.claude/skills/tdd-enforcement.md`

4. **Implementation**
   - Implement theo task breakdown
   - Commit thường xuyên

5. **Code Review** (self-review trước khi PR)
   - Tham khảo: `.claude/skills/two-stage-code-review.md`

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

Tham khảo: `.claude/skills/docker-scripts-required.md`

## Git Workflow

- **ALWAYS** create feature branch before changes
- **NEVER** commit directly to main
- **Branch naming:** `feature/PR-{number}-{description}`
- Test locally before pushing to CI
- Use `./scripts/test-local.sh` for testing

## Skills Reference

All skills in `.claude/skills/`:
- `brainstorming-methodology.md` - Quick brainstorm process
- `task-breakdown-guide.md` - Task decomposition
- `tdd-enforcement.md` - Test-first development
- `two-stage-code-review.md` - Self-review checklist
- `systematic-debugging.md` - 4-phase debugging
