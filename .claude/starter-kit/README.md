# Claude Code Starter Kit

**Version:** 2.3.0 (xem CHANGELOG.md)

Bộ skills, scripts, templates rút ra từ kinh nghiệm phát triển dự án thực tế (~200+ PRs, 10+ waves). Được quản lý như internal package với versioning và review process.

## Dùng khi nào?

- Bắt đầu dự án mới → workflow chuẩn từ ngày đầu
- Dự án đã có → tiếp nhận best practices không overwrite customizations
- Dự án phát hiện cải tiến → đóng góp ngược vào kit cho các dự án khác

## 3 Scenarios

### 1. Dự án MỚI

```bash
./bin/init-project.sh /path/to/new-project
```

### 2. Dự án ĐÃ CÓ skills/workflows

```bash
./bin/upgrade-project.sh /path/to/project --dry-run    # Preview
./bin/upgrade-project.sh /path/to/project               # Interactive
./bin/upgrade-project.sh /path/to/project --scripts     # Chỉ scripts
```

### 3. Đóng góp cải tiến từ dự án → kit

```bash
./bin/contribute.sh /path/to/project "Cải thiện TDD skill sau incident X"
./bin/contribute.sh --list
./bin/contribute.sh --apply <proposal-id>
```

Xem chi tiết: [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)

## Cấu trúc

```
starter-kit/
├── README.md              ← File này
├── VERSION                ← Semantic versioning
├── CHANGELOG.md           ← Lịch sử thay đổi
├── kit-manifest.yml       ← File classification for upgrades
│
├── bin/                   ← Kit management scripts
│   ├── init-project.sh        ← Setup dự án mới
│   ├── upgrade-project.sh     ← Import vào dự án đã có
│   ├── install-remote.sh      ← Install từ remote repo
│   ├── contribute.sh          ← Đề xuất cải tiến
│   ├── publish.sh             ← Publish new version
│   └── test-kit.sh            ← Test kit functionality
│
├── docs/                  ← Kit documentation
│   ├── INSTALL.md             ← Installation guide
│   ├── GETTING-STARTED.md     ← First-time setup guide
│   ├── CONTRIBUTING.md        ← Contribution governance
│   └── EXTRACTION-GUIDE.md    ← Extract kit to standalone repo
│
├── rules/                 ← Conventions & standards
│   └── skill-conventions.md   ← How to write skills (Anthropic best practices)
│
├── skills/                ← Development methodology (gotchas, not generic)
│   ├── core/              ← Project-specific gotchas templates
│   ├── workflow/          ← Git, PR, CI process
│   ├── quality/           ← Audit framework 100 điểm
│   └── reference/         ← Business docs, UI templates, diagrams
│
├── scripts/               ← Reusable project automation
│   ├── check-ci.sh        ← CI monitoring
│   ├── test-local.sh      ← Local test runner
│   └── pre-commit-check.sh
│
├── templates/             ← Ready-to-use templates
│   ├── CLAUDE.md.template
│   ├── README.md.template
│   ├── skill-folder/      ← Template for new skills
│   └── *.template
│
├── memory/                ← Seed memories (lessons learned)
└── .claude-plugin/        ← Plugin registry metadata
```

## Versioning

| Type | Khi nào | Ví dụ |
|------|---------|-------|
| MAJOR (x.0.0) | Breaking changes (đổi cấu trúc, bỏ file) | 2.0.0 |
| MINOR (1.x.0) | Thêm skill/script/rule mới | 1.5.0 |
| PATCH (1.0.x) | Fix bug, cải thiện nội dung | 1.5.1 |

Xem [rules/skill-conventions.md](rules/skill-conventions.md) cho version management checklist.
