---
name: continue
description: Xác định action ưu tiên nhất từ project plans và thực hiện theo Superpowers methodology
user-invocable: true
argument-hint: "[optional context]"
---

# /continue - Thực hiện Action Ưu Tiên Nhất

## Step 1: Xác định Priority Action

Đọc các plan documents theo thứ tự, tìm item ưu tiên cao nhất chưa hoàn thành:

1. `documents/03-planning/local-prod-separation-plan.md`
2. `documents/03-planning/local-e2e-roadmap.md`
3. `documents/03-planning/ui-refactor-plan.md`
4. `documents/03-planning/kiteclass-theme-system-design.md`
5. `documents/03-planning/ai-local-implementation-plan.md`

Thứ tự ưu tiên: 🔴 P0 > 🟠 P1 > 🟡 P2

Nếu tất cả plans hoàn thành:
- Check open PRs: `gh pr list --state open`
- Check CI: `gh run list --limit 5`
- Check stale branches: `git branch -r | grep -v main | wc -l`
- Run E2E verify: `./scripts/test-api-e2e.sh`

Context bổ sung từ user: $ARGUMENTS

## Step 2: Báo cáo trước khi làm

```
## Tiếp tục: [Tên PR/Task]
**Priority**: P0/P1/P2
**Plan**: [tên document]
**Scope**: [mô tả 1-2 câu]
```

## Step 3: Thực hiện theo Superpowers

### 3.1 Quick Brainstorm
- Phân tích scope, risks, edge cases
- Xác định dependencies

### 3.2 Task Breakdown
- Chia tasks cụ thể với estimate

### 3.3 TDD (cho code changes)
- Viết tests TRƯỚC code
- Commit tests (RED phase)

### 3.4 Implementation
- Implement theo breakdown
- Commit thường xuyên

### 3.5 Verify (BẮT BUỘC trước khi push)
- Rebuild Docker nếu cần: `cd kitehub && ./scripts/rebuild.sh <service>`
- API E2E: `./scripts/test-api-e2e.sh`
- Unit tests: `JAVA_HOME=/home/vkiet/jdk/jdk-21 ./mvnw clean test -pl <module> -am`
- PHẢI tất cả pass

### 3.6 Push & PR
- `git push origin <branch>`
- `gh pr create`
- `gh pr checks <number> --watch`
- KHÔNG merge - chờ user approve

## Step 4: Update Plan
Sau khi PR tạo xong, update plan document: đánh dấu ✅, thêm PR number.

## Rules
- LUÔN giao tiếp tiếng Việt
- LUÔN tạo feature branch
- LUÔN chạy tests sau code changes
- KHÔNG merge không có approval
- KHÔNG bỏ qua brainstorm/TDD
