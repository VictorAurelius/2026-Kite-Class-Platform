# Project Structure — Best Practice

## Nguyên tắc

1. **Flat at root, deep in services** — root chỉ có top-level folders, chi tiết nằm trong service
2. **Tách rõ concerns** — docs, infra, code, scripts không trộn lẫn
3. **Convention over configuration** — tên folder nói lên mục đích, không cần giải thích
4. **Mỗi service tự chứa** — README + Dockerfile + tests + configs riêng

## Cấu trúc chuẩn

```
{project}/
├── .claude/                    # Claude Code configuration
│   ├── skills/                 # AI skills (core, workflow, quality, reference)
│   ├── scripts/                # Pre-commit hooks, automation
│   └── starter-kit/            # Portable kit (nếu là source project)
│
├── .github/                    # CI/CD workflows
│   └── workflows/
│
├── .vscode/                    # IDE settings (shared, committed)
│   └── settings.json
│
├── documents/                  # ALL documentation (SOURCE OF TRUTH)
│   ├── 01-business/            # Business logic (3-layer per domain)
│   │   ├── {project-a}/
│   │   │   └── {domain}/       # rules.md + use-cases.md + api-contract.md
│   │   └── {project-b}/
│   ├── 02-architecture/        # Technical architecture docs
│   ├── 03-planning/            # Plans, roadmaps, PRs
│   ├── 04-quality/             # Audit reports, gap checks
│   ├── 05-guides/              # Deploy guides, operations
│   ├── 06-diagrams/            # PlantUML, rendered PNGs
│   └── 07-archived/            # Old/deprecated docs
│
├── infrastructure/             # DevOps (KHÔNG để ở root)
│   ├── helm/                   # Helm charts
│   ├── k8s/                    # Kubernetes manifests
│   └── terraform-{provider}/   # IaC per provider
│
├── {service-a}/                # Microservice hoặc sub-project
│   ├── {service-a}-core/       # Backend service
│   │   ├── src/main/
│   │   ├── src/test/
│   │   ├── Dockerfile
│   │   └── pom.xml / build.gradle
│   ├── {service-a}-gateway/    # API gateway
│   ├── {service-a}-frontend/   # Frontend app
│   ├── docker-compose.dev.yml  # Local dev stack
│   ├── .env.example            # Environment template
│   └── scripts/                # Service-specific scripts
│
├── scripts/                    # Root-level scripts (cross-project)
│   ├── check-ci.sh
│   ├── test-local.sh
│   └── verify-*.sh
│
├── CLAUDE.md                   # Claude Code instructions
├── README.md                   # Project overview
└── .gitignore
```

## Anti-patterns (TRÁNH)

| Anti-pattern | Vấn đề | Fix |
|-------------|--------|-----|
| `helm/`, `k8s/`, `terraform/` ở root | Loạn root directory | → `infrastructure/` |
| `docs/` trong mỗi service + root | Docs phân tán, khó tìm | → `documents/` tập trung |
| Business logic trong code comments | Không searchable, không reviewable | → `documents/01-business/` |
| `.env` committed | Security risk | → `.env.example` + .gitignore |
| Test data trong `src/main/` | Production bloat | → `src/test/resources/` |
| Script trong nhiều folder | Không biết dùng script nào | → `scripts/` tập trung + service scripts |

## Checklist khi tạo dự án mới

- [ ] `documents/01-business/` có ít nhất 1 domain với 3-layer docs
- [ ] `infrastructure/` chứa tất cả IaC (không để root)
- [ ] Mỗi service có: README.md, Dockerfile, .env.example
- [ ] `scripts/` có: test-local.sh, check-ci.sh
- [ ] `.vscode/settings.json` committed (shared IDE config)
- [ ] `.gitignore` cover: .env, node_modules, target/, .next/

## Khi nào refactor folder?

**Signal:** Nếu trả lời "có" cho bất kỳ câu nào:
- Có >5 folders ở root không phải service?
- Có docs nằm trong >3 locations khác nhau?
- Có scripts phân tán ở >2 locations?
- Có infra files (helm, k8s, terraform) ở root?
- Developer mới không biết tìm file ở đâu?

**Process:** Tạo PR riêng cho refactor, không gộp với feature. Update tất cả references (CI paths, imports, scripts).
