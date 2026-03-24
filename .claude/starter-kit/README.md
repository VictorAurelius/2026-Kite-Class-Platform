# Claude Code Starter Kit

Bộ skills, scripts, templates rút ra từ kinh nghiệm phát triển dự án thực tế (~200+ PRs, 10+ waves, nhiều lần fix workflow).

## Dùng khi nào?

Khi bắt đầu dự án mới với Claude Code và muốn:
- Workflow chuẩn hóa từ ngày đầu (không mất 3 tháng tự khám phá)
- Tránh lặp lại sai lầm đã gặp (CI monitoring, business docs, testing)
- Quality framework có sẵn (audit scoring, gap check)

## Setup

```bash
# Copy starter kit vào dự án mới
./init-project.sh /path/to/new-project

# Hoặc dự án hiện tại
./init-project.sh .
```

Script tự động:
- Copy skills → `.claude/skills/`
- Copy scripts → `scripts/`
- Tạo CLAUDE.md + README.md từ template
- Seed memories (lessons learned) → `~/.claude/projects/`
- Link pre-commit hook

## Cấu trúc

```
starter-kit/
├── README.md              ← File này
├── init-project.sh        ← Setup script
├── skills/
│   ├── core/              ← 5 files: brainstorm, TDD, review, debug, breakdown
│   ├── workflow/          ← 1 file: git + PR + CI process
│   ├── quality/           ← 1 file: 10-category audit framework
│   └── reference/         ← 2 files: business docs 3-layer, service docs
├── scripts/
│   ├── check-ci.sh        ← CI monitoring (--status mode)
│   ├── test-local.sh      ← Local test runner (auto-detect, quick mode)
│   └── pre-commit-check.sh ← Commit checks (extensible)
├── templates/
│   ├── CLAUDE.md.template ← Project instructions template
│   └── README.md.template ← README template
└── memory/
    ├── feedback_scripts_not_adhoc.md
    ├── feedback_ci_before_scoring.md
    ├── feedback_self_test_before_push.md
    └── feedback_business_design_first.md
```

## Sau khi setup

1. **Edit `CLAUDE.md`** — thay `{placeholders}` bằng thông tin dự án
2. **Edit `scripts/test-local.sh`** — cấu hình `PROJECT_DIRS` cho project
3. **Customize skills** — thêm project-specific checks vào `pre-commit-check.sh`

## Lessons Learned (seed memories)

| Rule | Lý do |
|------|-------|
| Scripts, không lệnh ad-hoc | Vi phạm 4+ lần → mỗi lần mất thời gian fix |
| CI phải complete trước scoring | Kết luận sai khi CI còn chạy |
| Test local trước push | CI fail 3 lần vì Checkstyle — 5s local vs 9min CI |
| Business docs trước code | 188 PRs → 22 gaps → 39 PRs fix |

## Workflow tổng quát

```
Code → Commit → test-local.sh → Push → check-ci.sh → PR → Review → Merge
         ↑                                                      ↓
    TDD (Red→Green→Refactor)                          Quality Audit
```
