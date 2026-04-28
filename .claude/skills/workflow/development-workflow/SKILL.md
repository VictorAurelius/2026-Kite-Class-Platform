---
name: development-workflow
description: Use when user says 'development workflow', 'PR workflow', 'branching strategy', 'how to ship', 'quy trình phát triển', 'cách làm PR', or before starting a new feature/PR. Comprehensive end-to-end workflow from planning through release for KiteClass Platform — covers branching, commits, PR template, 7 phases (planning → testing → docs → review → merge), release/hotfix, troubleshooting.
type: workflow
---

# Skill: Development Workflow

**Version:** 2.1 (folder split from 1221-line monolith — Wave 9 / GAP-251 follow-up)
**Last Updated:** 2026-04-28
**Purpose:** End-to-end development workflow — planning → branch → implement → test → review → merge → release

## Overview

Tổng hợp toàn bộ quy trình phát triển KiteClass Platform: Git workflow (branching, commits, PRs), 7-phase feature development checklist, release/hotfix processes, và troubleshooting fast-paths. Skill này là one-stop reference khi triển khai bất kỳ feature/bug-fix/refactor nào.

Content được tách thành 8 reference files để giữ SKILL.md gọn — đọc reference khi cần đến phần cụ thể, không phải đọc hết một lần.

## When to Use This Skill

- Trước khi tạo branch / PR mới (xem `branching-strategy.md` + `commit-messages.md`)
- Khi đang ở giữa phase coding/testing và cần checklist (xem `phases-1-2-3.md`)
- Trước khi merge — chạy review + merge criteria (xem `phases-4-5-6-7.md`)
- Khi cắt release tag hoặc xử lý hotfix (xem `release-and-hotfix.md`)

## Skill Contents

| Reference | When to read |
|-----------|--------------|
| `reference/branching-strategy.md` | When creating branch / unsure of base / git flow rules |
| `reference/commit-messages.md` | When writing commit / preparing PR title |
| `reference/pull-request-process.md` | When opening PR / filling PR template |
| `reference/phases-1-2-3.md` | While coding (planning, implementation, testing phases) |
| `reference/phases-4-5-6-7.md` | Pre-merge gates (docs update, review, merge criteria) |
| `reference/release-and-hotfix.md` | Cutting a release tag / handling production hotfix |
| `reference/troubleshooting.md` | When stuck — common issues, fixes, fast paths |
| `reference/checklist.md` | Final pre-merge sweep |

## Gotchas

- **KHÔNG bao giờ commit trực tiếp lên `main`** — luôn qua PR (per CLAUDE.md Git Workflow).
- **KHÔNG thêm `Co-Authored-By` trailer** vào commits — Claude Code tự thêm nhưng project rule bỏ đi (per CLAUDE.md Commit Message Rules). Note: một số example trong reference files vẫn còn `Co-Authored-By` từ legacy v2.0; tuân theo CLAUDE.md, không theo example.
- **AI KHÔNG push** — luôn dừng trước `git push` và để user confirm (per `branching-strategy.md` workflow rules).
- **Self-test trước push BẮT BUỘC** — dùng `scripts/test-local.sh`, không chạy lệnh test tự do (per `phases-1-2-3.md` Phase 3).
- Reference files vẫn dùng giả định `develop` branch + ticket prefix `KC-{id}` từ legacy plan; project hiện tại dùng wave/* + feature/PR-{N}-* — đối chiếu với CLAUDE.md Wave Branch Strategy khi áp dụng.

## Related Skills

- `workflow/priority-pr-planning.md` — chọn PR/gap nào làm trước (priority + sequencing)
- `workflow/pr-health.md` — PR compliance scanner per-PR (CI, tests, docs, audits)
- `workflow/gap-to-pr-converter.md` — chuyển gap file thành PR plan
- `core/two-stage-code-review.md` — self-review checklist trước khi mở PR
