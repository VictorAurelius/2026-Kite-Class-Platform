# Kite Platform

Multi-platform educational technology suite — SaaS management + multi-tenant education.

## Projects

- **[KiteHub](kitehub/)** — SaaS platform: tenant management, subscription, billing, branding, email, admin
  - 6 backend services + API gateway + frontend
  - Spring Boot 3.5.13, Next.js 15

- **[KiteClass](kiteclass/)** — Multi-tenant education platform: students, courses, classes, attendance, grades, payments
  - Core service + API gateway + frontend
  - Spring Boot 3.5.13, Next.js 15

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
- Spring Boot 3.5.13, Spring Cloud 2025.0.0
- Java 17 (LTS), Maven
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
- [Diagrams](documents/06-diagrams/) — 25 PlantUML diagrams with rendered PNGs
- [Architectural Decisions](documents/02-architecture/adr/) — 15 ADRs (Michael Nygard format)
- [Deployment Strategy](documents/02-architecture/deployment-strategy.md) — 5 philosophy principles + environment matrix
- [Governance Rules](.claude/rules/) — `docs-folder-structure.md`, `planning-docs-structure.md`, `audit-to-gap-pipeline.md`, etc.
- [Thesis References](documents/08-thesis/) — Graduation project materials
- [Documentation Map](documents/README.md) — Complete navigation guide
- [Master Plan 2026-04-20](documents/03-planning/roadmap/master-plan-all-gaps-2026-04-20.md) — 12-wave roadmap to close all open gaps in ~2-3 months
- [Part C Score Recovery Plan](documents/03-planning/waves/wave-audit-part-c-score-recovery.md) — 4-sprint plan to raise quality 77 → ~88 A−
- [Wave 5 Document Generation](documents/03-planning/waves/wave-05-document-generation.md) — GAP-047 adoption plan

## Quality

**Calibrated audit scores (2026-04-20 baseline)** — specialist audits provide honest scores vs prior 95/100 self-estimate:

| Audit | Score | Grade | Trend vs baseline |
|-------|:-----:|:-----:|:-----------------:|
| Quality Audit (10 categories) | 77/100 | C+ | first honest baseline 2026-04-19 |
| Business Logic | 72/100 | C | 65 → 72 (+7, after Part B) |
| Performance | 64/100 | D | 58 → 64 (+6, after Part B) |
| UI Review KiteClass | 81/128 | — | +1 since 2026-04-11 |
| UI Review KiteHub | 59/128 | — | +1 since 2026-04-11 |
| Ops Readiness | 49/100 | F | first-ever baseline 2026-04-19 |
| Security | 77/100 | B+ | 2026-04-17 |
| API Contract | 85/100 | A− | 2026-04-17 |

**Backlog status:** 155 gaps tracked (59 closed, 93 open). Master plan shipped to close all 93 in 12 waves over ~2-3 months — see [Master Plan](documents/03-planning/roadmap/master-plan-all-gaps-2026-04-20.md).

**Gap governance (all active):** `audit-gate.py` hook blocks non-docs-only PRs without fresh audits; post-wave audit mandate enforces 3-day freshness window.

| Metric | KiteHub | KiteClass |
|--------|---------|-----------|
| Business Domains (3-layer) | 7 | 13 |
| CI Workflows | 6 | 3 |
| ADRs | 15 | (shared) |

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

**Last Updated:** 2026-04-20 (Part A + Part B + Re-audit complete; master plan for 93 open gaps merged PR #382)
