# Kite Platform Monorepo

Multi-platform educational technology suite.

## 🎯 Projects

- **[KiteClass](kiteclass/)** - Educational management platform (multi-tenant SaaS)
- **KiteHub** _(Coming soon)_ - SaaS platform for education ecosystem

## 🚀 Getting Started

Each project has its own README with detailed setup instructions:

- [KiteClass Setup Guide](kiteclass/README.md)
- KiteHub Setup Guide _(future)_

## 📁 Repository Structure

```
2026-Kite-Class-Platform/
├── kiteclass/                 # KiteClass Platform
│   ├── kiteclass-core/        # Spring Boot business logic
│   ├── kiteclass-gateway/     # Spring Boot API Gateway
│   ├── kiteclass-frontend/    # Next.js frontend
│   ├── scripts/               # KiteClass-specific scripts
│   ├── nginx/                 # KiteClass nginx config
│   ├── docker-compose.dev.yml # KiteClass stack orchestration
│   └── README.md              # KiteClass documentation
│
├── kitehub/                   # KiteHub Platform (future)
│   └── ...
│
├── documents/                 # Shared documentation (monorepo-level)
│   ├── 01-research/
│   ├── 02-academic/
│   ├── 03-planning/
│   └── ...
│
├── scripts/                   # Shared monorepo utilities
│   ├── test-local.sh          # Generic test runner
│   ├── dev-docker.sh          # Generic docker wrapper
│   └── cleanup-testcontainers.sh
│
└── .github/workflows/         # CI/CD for all projects
    ├── kiteclass-*.yml
    └── kitehub-*.yml (future)
```

## 🛠️ Development Workflow

### Quick Commands

```bash
# KiteClass development
cd kiteclass/
./scripts/dev-docker.sh up

# Run tests
./scripts/test-local.sh all

# Check status
./scripts/dev-status.sh
```

### Shared Scripts

```bash
# Generic test runner
./scripts/test-local.sh <project> <service>

# Generic docker wrapper
./scripts/dev-docker.sh <compose-file> <command>

# Cleanup testcontainers
./scripts/cleanup-testcontainers.sh
```

See [scripts/README.md](scripts/README.md) for details.

## 📚 Documentation

- **[documents/](documents/)** - Shared technical documentation
- **[KiteClass Docs](kiteclass/)** - KiteClass-specific guides
- **[Scripts Guide](scripts/README.md)** - Shared utilities documentation

## 🧰 Tech Stack Overview

### KiteClass
- **Backend**: Spring Boot 3.5.10, Spring Cloud 2025.0.0
- **Frontend**: Next.js 15, React 19, TypeScript
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Storage**: MinIO (S3-compatible)

### KiteHub _(future)_
- TBD

## 🤝 Contributing

Each project has its own contributing guidelines:
- [KiteClass Contributing](kiteclass/README.md)
- KiteHub Contributing _(future)_

## 📖 More Information

For detailed project documentation:
- [KiteClass Platform](kiteclass/README.md)
- [Implementation Plan](documents/03-planning/implementation/kiteclass-implementation-plan.md)
- [Architecture Overview](documents/01-research/architecture/system-architecture-v4.md)

---

**Last Updated**: 2026-02-27
