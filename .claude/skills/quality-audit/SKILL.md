# /quality-audit - Đánh giá chất lượng toàn diện

**Usage:** `/quality-audit [kitehub|kiteclass|all]`

**Default:** `all` (đánh giá cả KiteHub + KiteClass)

---

## Instructions

Khi user invoke `/quality-audit`:

### Bước 1: Thu thập dữ liệu tự động

Chạy tất cả lệnh sau **song song** để thu thập metrics:

```bash
# 1. Git & PR stats
git log --oneline --since="30 days ago" | wc -l
gh pr list --state merged --limit 200 --json number --jq 'length'
gh pr list --state open --json number --jq 'length'
git branch -r | grep -v "main\|HEAD" | wc -l

# 2. CI status — PHẢI dùng script
#    Audit: dùng --status (đọc kết quả, không đợi)
#    Sau push: dùng không có flag (đợi hoàn thành)
scripts/check-ci.sh --status

# 3. Backend + Frontend tests — PHẢI dùng script
scripts/test-local.sh kiteclass all    # hoặc kitehub all
# KHÔNG chạy mvnw/vitest/eslint trực tiếp

# 4. E2E tests — dùng script có sẵn
kiteclass/scripts/test-api-e2e.sh

# 5. Docker status — dùng script có sẵn
kiteclass/scripts/dev-status.sh        # hoặc kitehub/scripts/status.sh

# 6. Code stats (grep/find OK vì chỉ đếm, không execute)
find kiteclass -name "*.java" -path "*/src/main/*" | wc -l
find kiteclass -name "*Test.java" | wc -l
find kiteclass/kiteclass-frontend/src -name "*.tsx" -o -name "*.ts" | wc -l

# 7. Security check (grep OK vì chỉ scan, không execute)
grep -r "TODO\|FIXME\|HACK\|XXX" kiteclass/*/src/main --include="*.java" | wc -l

# 8. Documentation
find documents -name "*.md" | wc -l

# 9. Monitoring
kiteclass/scripts/monitor.sh health    # hoặc kitehub/scripts/status.sh
```

**CRITICAL: KHÔNG chạy lệnh ad-hoc cho:**
- Tests → `scripts/test-local.sh`
- CI monitoring → `scripts/check-ci.sh`
- Docker → `*/scripts/dev-*.sh` hoặc `kitehub/scripts/*.sh`
- Monitoring → `*/scripts/monitor.sh`

### Bước 2: Chấm điểm 10 categories (100 điểm)

#### 1. E2E Functionality (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| E2E API tests pass 100% | 4 | `test-api-e2e.sh` results |
| E2E pass ngay lần đầu (no cold start issue) | 2 | Chạy 1 lần duy nhất |
| Critical flows hoạt động: Register→Login→Dashboard→Instance | 2 | Manual hoặc E2E |
| AI features hoạt động (không chỉ mock) | 2 | Check AI provider config |

#### 2. Security (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Authentication: JWT + email verify + captcha | 3 | Code review |
| Rate limiting hoạt động | 2 | Gateway config |
| Không có secrets hardcode trong code | 2 | `grep -r` sensitive patterns |
| CORS configured đúng | 1 | Gateway CORS config |
| Input validation trên tất cả endpoints | 2 | DTO annotations |

#### 3. Backend Tests (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả modules build + test pass (0 errors) | 4 | `mvnw test` |
| 0 skipped tests | 2 | Check skipped count |
| Test coverage >70% | 2 | Jacoco report nếu có |
| Integration tests cho critical paths | 2 | Check *IT.java files |

#### 4. Frontend Tests (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| KiteClass FE: vitest pass, <10% skipped | 3 | `vitest run` results |
| KiteHub FE: build pass (all pages) | 2 | `next build` |
| Component tests cho critical pages | 3 | Count test files |
| E2E browser tests (Playwright/Cypress) | 2 | Check e2e/ folder |

#### 5. CI/CD (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả CI workflows green trên main | 4 | `gh run list` |
| 0 stale branches | 2 | `git branch -r` count |
| 0 open PRs không hoạt động | 2 | `gh pr list --state open` |
| CI history sạch (không spam failed runs) | 2 | `gh run list` count |

#### 6. UI/UX (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả pages có consistent design system | 3 | Check gradient headers, shadow-soft |
| Theme system hoạt động (đổi màu visual) | 2 | Test ?primary=FF0000 URL |
| Responsive (mobile-friendly) | 2 | Check breakpoints |
| Onboarding/guidance cho new users | 2 | Wizard, checklist, tooltips |
| Accessibility (a11y basics) | 1 | aria-labels, semantic HTML |

#### 7. DevOps/Infrastructure (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả Docker containers healthy | 3 | `docker compose ps` |
| Production deployment plan có (Terraform) | 2 | Check terraform/ folder |
| Backup strategy documented | 2 | Check docs |
| Monitoring/alerting | 2 | Check dashboard/alerts |
| Secrets management documented | 1 | SECRET-MANAGEMENT.md |

#### 8. Documentation (10 điểm)

> **Business docs at `documents/01-business/` are SOURCE OF TRUTH. Score 0 for this category if no business docs exist for implemented domains.**

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Business docs exist for all implemented domains | 3 | Check `documents/01-business/` has doc for each service with business logic |
| Business docs match code - config keys, rules | 2 | Cross-check config keys in code vs business doc Config section |
| Architecture + guides + README up-to-date | 3 | Check planning/ docs, vietnamese/ folder, README, CLAUDE.md |
| Plans up to date with completion tracking | 2 | Check ✅/⬜ in plans |

#### 9. Code Quality (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| 0 TODO/FIXME/HACK trong production code | 2 | `grep -r` |
| 0 IDE warnings (TypeScript + Java) | 2 | User reports |
| Consistent coding style (ESLint, Checkstyle) | 2 | Pre-commit hooks |
| No dead code / unused imports | 2 | Linter results |
| Spring Boot latest patch version | 2 | Check pom.xml |

#### 10. Project Management (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả plans có completion status | 3 | Review plan docs |
| PRs follow Superpowers methodology | 3 | Check recent PR descriptions |
| Commit messages clean + meaningful | 2 | `git log` review |
| Issues/gaps tracked và prioritized | 2 | Check gap reports |

### Bước 3: Output Report

```markdown
# Quality Audit Report: [KiteHub/KiteClass/All]

**Ngày:** [date]
**Người đánh giá:** Claude Code
**Version:** [latest commit hash]

---

## Overall Score

| # | Category | Score | Max | Grade |
|---|----------|-------|-----|-------|
| 1 | E2E Functionality | X | 10 | ✅/⚠️/❌ |
| 2 | Security | X | 10 | ✅/⚠️/❌ |
| 3 | Backend Tests | X | 10 | ✅/⚠️/❌ |
| 4 | Frontend Tests | X | 10 | ✅/⚠️/❌ |
| 5 | CI/CD | X | 10 | ✅/⚠️/❌ |
| 6 | UI/UX | X | 10 | ✅/⚠️/❌ |
| 7 | DevOps/Infra | X | 10 | ✅/⚠️/❌ |
| 8 | Documentation | X | 10 | ✅/⚠️/❌ |
| 9 | Code Quality | X | 10 | ✅/⚠️/❌ |
| 10 | Project Management | X | 10 | ✅/⚠️/❌ |
| **Total** | | **X** | **100** | **Grade** |

### Grade Scale
- 95-100: A+ (Production Excellence)
- 90-94: A (Production Ready)
- 85-89: B+ (Near Production)
- 80-84: B (Good, needs polish)
- 70-79: C (Acceptable, significant gaps)
- <70: D (Major work needed)

---

## Detailed Findings

### ✅ Strengths (8+/10)
[List categories scoring 8+ with evidence]

### ⚠️ Needs Improvement (5-7/10)
[List categories with specific gaps]

### ❌ Critical Issues (<5/10)
[List categories with blockers]

---

## Improvement Roadmap

### Quick Wins (1-2 hours each)
[Items that give most score per effort]

### Medium Effort (0.5-1 day)
[Important improvements]

### Major Effort (2+ days)
[Strategic improvements]

---

## Comparison with Previous Audit

| Category | Previous | Current | Change |
|----------|----------|---------|--------|
| ... | ... | ... | +X/-X |

(Nếu có audit trước, so sánh. Nếu không, ghi "First audit")

---

## Action Items

| Priority | Item | Estimated Score | Effort |
|----------|------|-----------------|--------|
| 🔴 P0 | ... | +X | ... |
| 🟠 P1 | ... | +X | ... |
| 🟡 P2 | ... | +X | ... |

---

## Next Audit Recommended

Đề xuất chạy `/quality-audit` lại sau khi hoàn thành Phase [X] của improvement plan.
```

### Bước 4: Lưu kết quả

- Save report to `documents/04-quality/quality-audit-[date].md`
- Update `kitehub-quality-100-plan.md` nếu phát hiện gaps mới
- So sánh với audit trước nếu có

---

## Rules

- LUÔN chạy tests thật (không đoán)
- LUÔN giao tiếp tiếng Việt
- Chấm điểm dựa trên evidence (test output, code check), không dựa trên cảm tính
- Nếu E2E fail lần 1 do cold start, chạy lần 2 nhưng GHI NHẬN cold start issue (-2 điểm)
- Nếu không thể chạy test (Docker down, etc.), ghi 0 điểm cho category đó + note lý do
- So sánh với quality plan nếu có (`kitehub-quality-100-plan.md`)

### CRITICAL: CI phải hoàn thành trước khi chấm điểm

**KHÔNG BAO GIỜ** kết luận CI/CD score hoặc Backend Tests score khi CI còn đang chạy (`in_progress`).

**Quy trình bắt buộc:**
1. Thu thập data → kiểm tra `gh run list` status
2. Nếu có run `in_progress` liên quan đến target (branch/PR):
   - **PHẢI** dùng `scripts/check-ci.sh --status` để kiểm tra — KHÔNG dùng lệnh CI trực tiếp
   - Nếu có runs `in_progress` → dùng `scripts/check-ci.sh` (wait mode) để đợi
   - **PHẢI** báo user: "CI đang chạy, đợi kết quả trước khi chấm điểm CI/CD"
   - **KHÔNG ĐƯỢC** giả định pass/fail
3. Chỉ sau khi CI completed → mới chấm điểm categories: CI/CD, Backend Tests, E2E
4. Nếu user yêu cầu audit gấp → ghi rõ "CI/CD: PENDING (chưa có kết quả)" thay vì đoán điểm

**Lý do:** Audit trước đã kết luận CI pass trong khi thực tế CI đang chạy. Điều này làm sai lệch kết quả đánh giá và có thể dẫn đến merge PR lỗi.
