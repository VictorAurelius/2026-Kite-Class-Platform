# Claude Code Starter Kit

Bộ skills, scripts, templates rút ra từ kinh nghiệm phát triển dự án thực tế (~200+ PRs, 10+ waves, nhiều lần fix workflow).

## Dùng khi nào?

Khi bắt đầu dự án mới với Claude Code và muốn:
- Workflow chuẩn hóa từ ngày đầu (không mất 3 tháng tự khám phá)
- Tránh lặp lại sai lầm đã gặp (CI monitoring, business docs, testing)
- Quality framework có sẵn (audit scoring, gap check)

## Setup

### Dự án MỚI (chưa có .claude/)

```bash
./init-project.sh /path/to/new-project
```

Tự động: copy skills + scripts + templates + seed memories + link git hooks.

### Dự án ĐÃ CÓ skills/workflows

```bash
# Preview — xem sẽ thay đổi gì (không sửa file)
./upgrade-project.sh /path/to/existing-project --dry-run

# Interactive — hỏi trước mỗi conflict
./upgrade-project.sh /path/to/existing-project

# Chỉ import scripts (không đụng skills)
./upgrade-project.sh /path/to/project --scripts

# Chỉ import skills
./upgrade-project.sh /path/to/project --skills

# Chỉ seed memories
./upgrade-project.sh /path/to/project --memory
```

Khi file đã tồn tại và khác kit version → 3 lựa chọn:
- **[k] Keep** existing (skip)
- **[u] Use** kit version (overwrite)
- **[m] Merge** manually (save as `.kit-new` để review)

### Cập nhật starter-kit từ dự án gốc

```bash
# Khi dự án gốc update skills/scripts → sync vào kit
./sync-to-kit.sh              # Interactive
./sync-to-kit.sh --dry-run    # Preview only
./sync-to-kit.sh --auto       # Auto-sync
```

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
