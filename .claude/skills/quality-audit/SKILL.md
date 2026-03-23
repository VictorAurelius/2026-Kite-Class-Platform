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

# 2. CI status
gh run list --limit 10 --json conclusion,name,headBranch --jq '.[] | "\(.conclusion) \(.name) (\(.headBranch))"'

# 3. Backend tests
cd kitehub && JAVA_HOME=/home/vkiet/jdk/jdk-21 ./mvnw test -q 2>&1 | grep "Tests run:"
# Nếu kiteclass: cd kiteclass/kiteclass-core && ./mvnw test

# 4. Frontend tests
cd kiteclass/kiteclass-frontend && npx vitest run 2>&1 | grep -E "Test Files|Tests "
cd kitehub/kitehub-frontend && npx next build 2>&1 | grep -E "pages|error"

# 5. E2E tests
cd kitehub && bash scripts/test-api-e2e.sh 2>&1 | grep "Results:"

# 6. Docker status
docker compose -f kitehub/docker-compose.kitehub.yml ps --format "table {{.Name}}\t{{.Status}}"

# 7. Code stats
find kitehub -name "*.java" | grep -v test | wc -l
find kitehub -name "*Test.java" -o -name "*IT.java" | wc -l
find kitehub/kitehub-frontend/src -name "*.tsx" -o -name "*.ts" | wc -l
find kiteclass/kiteclass-frontend/src -name "*.tsx" -o -name "*.ts" | wc -l

# 8. Security check
grep -r "TODO\|FIXME\|HACK\|XXX" kitehub/kitehub-*/src/main --include="*.java" | wc -l
grep -r "sk-mock\|password.*=.*['\"]" kitehub/.env 2>/dev/null | wc -l

# 9. Documentation
find documents -name "*.md" | wc -l
ls documents/03-planning/*.md | wc -l

# 10. IDE Warnings (if available)
# Check for common issues in staged/modified files
```

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

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Hướng dẫn deploy tiếng Việt | 2 | Check vietnamese/ folder |
| Architecture docs up-to-date | 2 | Check planning/ docs |
| API documentation (Swagger/OpenAPI) | 2 | Check /swagger-ui |
| Plan documents có completion tracking | 2 | Check ✅/⬜ in plans |
| README + CLAUDE.md up-to-date | 2 | Read and verify |

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
