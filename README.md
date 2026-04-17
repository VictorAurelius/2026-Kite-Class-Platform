# Kite Platform

Multi-platform educational technology suite — SaaS management + multi-tenant education.

## Projects

- **[KiteHub](kitehub/)** — SaaS platform: tenant management, subscription, billing, branding, email, admin
  - 6 backend services + API gateway + frontend
  - Spring Boot 3.5.11, Next.js 15

- **[KiteClass](kiteclass/)** — Multi-tenant education platform: students, courses, classes, attendance, grades, payments
  - Core service + API gateway + frontend
  - Spring Boot 3.5.11, Next.js 15

## Getting Started

- [KiteHub Quick Start](kitehub/QUICK_START.md)
- [KiteClass Quick Start](kiteclass/QUICK_START.md)

## Repository Structure

```
2026-Kite-Class-Platform/
├── .claude/               # Skills, scripts, hooks
├── .github/               # CI/CD workflows (8 files)
├── documents/             # Documentation
│   ├── 01-business/       # Business logic (SOURCE OF TRUTH)
│   ├── 02-architecture/   # Technical architecture
│   ├── 03-planning/       # Plans, PRs, strategies
│   ├── 04-quality/        # Audits, gap checks
│   ├── 05-guides/         # Deploy guides, operations
│   ├── 06-diagrams/       # PlantUML + rendered PNG
│   ├── 07-archived/       # Old docs, research
│   └── 08-thesis/         # Graduation project refs
├── infrastructure/        # DevOps
│   ├── helm/              # Kubernetes Helm charts
│   ├── k8s/               # K8s manifests
│   ├── terraform-aws/     # AWS infrastructure
│   └── terraform-oracle/  # Oracle Cloud
├── kiteclass/             # KiteClass (core + gateway + frontend)
├── kitehub/               # KiteHub (6 services + gateway + frontend)
└── scripts/               # Root CI/QA scripts
```

## Tech Stack

### Shared
- **Database:** PostgreSQL 16
- **Cache:** Redis 7
- **Messaging:** RabbitMQ 3
- **Storage:** MinIO (S3-compatible)
- **AI:** Ollama (local LLM)

### Backend
- Spring Boot 3.5.11, Spring Cloud 2025.0.0
- Java 21, Maven
- Flyway migrations
- Micrometer + Prometheus metrics

### Frontend
- Next.js 15, React 19, TypeScript
- Tailwind CSS, Shadcn UI
- Playwright E2E tests

### Infrastructure
- Docker Compose (development)
- Kubernetes + Helm (production)
- Terraform (AWS + Oracle Cloud)
- GitHub Actions CI/CD

## Documentation

- [Business Logic](documents/01-business/) — Source of truth for all business rules
- [Architecture](documents/02-architecture/) — System design, Docker, domain management
- [Plans & Progress](documents/03-planning/) — Implementation plans, PR tracking
- [Quality Reports](documents/04-quality/) — Audits, gap checks
- [Guides](documents/05-guides/) — Deployment, operations, Vietnamese guides
- [Diagrams](documents/06-diagrams/) — 19 PlantUML diagrams with rendered PNGs
- [Thesis References](documents/08-thesis/) — Graduation project materials
- [Documentation Map](documents/README.md) — Complete navigation guide

## Quality

| Metric | KiteHub | KiteClass |
|--------|---------|-----------|
| Quality Score | 98/100 | 98/100 |
| Java Tests | 177 | 98 |
| Frontend Tests | 532 | 620 |
| CI Workflows | 6 | 3 |
| Business Docs | 7 | 9 |

## Development

```bash
# Start KiteHub full stack (14 containers)
cd kitehub/ && ./scripts/up.sh

# Run tests
cd kitehub/ && ./scripts/test-api-e2e.sh
cd kiteclass/ && ./scripts/test-api-e2e.sh
```

See [CLAUDE.md](CLAUDE.md) for development rules and methodology.

---

**Last Updated:** 2026-03-24
