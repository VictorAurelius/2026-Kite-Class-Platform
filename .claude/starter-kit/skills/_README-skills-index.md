# Skills Index — Khi nào dùng skill nào?

## Quy trình phát triển (theo thứ tự)

1. **Brainstorm** → `core/brainstorming-methodology.md`
2. **Task Breakdown** → `core/task-breakdown-guide.md`
3. **TDD** → `core/tdd-enforcement.md`
4. **Implementation** → (project-specific standards)
5. **Code Review** → `core/two-stage-code-review.md`
6. **Push** → `scripts/test-local.sh` → `scripts/check-ci.sh`

## Tất cả Skills

### Core (mỗi PR)
| File | Dùng khi |
|------|----------|
| `core/brainstorming-methodology.md` | Bắt đầu mỗi PR — phân tích scope, risks |
| `core/task-breakdown-guide.md` | Chia nhỏ công việc |
| `core/tdd-enforcement.md` | Trước khi viết code (Red→Green→Refactor) |
| `core/two-stage-code-review.md` | Self-review trước khi tạo PR |
| `core/systematic-debugging.md` | Gặp lỗi khó hiểu — 4-phase debugging |

### Workflow
| File | Dùng khi |
|------|----------|
| `workflow/development-workflow.md` | Git, PR, commit, self-test, CI monitoring |

### Quality
| File | Dùng khi |
|------|----------|
| `quality/quality-audit.md` | Đánh giá chất lượng /100 điểm |

### Reference
| File | Dùng khi |
|------|----------|
| `reference/business-docs-3-layer.md` | Thiết kế business docs (rules + use-cases + api-contract) |
| `reference/service-docs-standard.md` | Chuẩn README + QUICK-START cho mỗi service |
| `reference/project-structure.md` | Cấu trúc folder best practice, khi nào refactor |
| `reference/ide-setup.md` | VS Code settings, test runner, tắt MD warnings, Claude permissions |

## Scripts (PHẢI dùng, KHÔNG lệnh ad-hoc)

| Script | Dùng khi |
|--------|----------|
| `scripts/test-local.sh` | Trước push — test local |
| `scripts/test-local.sh --quick` | Quick check (compile/lint only) |
| `scripts/check-ci.sh` | Sau push — đợi CI |
| `scripts/check-ci.sh --status` | Quick CI status (audit, review) |
