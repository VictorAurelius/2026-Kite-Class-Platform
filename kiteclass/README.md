# KiteClass Platform

Multi-tenant educational management platform với microservices architecture.

## 📁 Project Structure

```
kiteclass-platform/
├── kiteclass/                  # Microservices
│   ├── kiteclass-core/         # Core Service (Business Logic)
│   ├── kiteclass-gateway/      # API Gateway (Auth, Routing)
│   └── kiteclass-frontend/     # Next.js Frontend
├── scripts/                    # Development & Build Scripts
├── nginx/                      # Nginx Configuration
├── documents/                  # Technical Documentation
├── .github/workflows/          # CI/CD Workflows
├── docker-compose.*.yml        # Docker Orchestration
└── *.md                        # Project Guides

```

## 🚀 Quick Start

### Development

```bash
# Start all services with Docker Compose
./scripts/dev-docker.sh up

# View logs
./scripts/dev-docker.sh logs

# Stop services
./scripts/dev-docker.sh down
```

### Testing

```bash
# Run tests locally (auto-cleanup)
./scripts/test-local.sh all

# Run specific service tests
./scripts/test-local.sh core
./scripts/test-local.sh gateway
```

### Docker Build

```bash
# Build images with version tracking
./scripts/docker-build.sh

# Check current version
./scripts/docker-version.sh
```

## 📚 Documentation

- **[DOCKER-BUILD-GUIDE.md](DOCKER-BUILD-GUIDE.md)** - Docker build and versioning
- **[TESTING-GUIDE.md](TESTING-GUIDE.md)** - Testing strategies
- **[CURRENT-WORK.md](CURRENT-WORK.md)** - Current work tracking
- **[documents/](documents/)** - Full technical documentation
  - [Architecture](documents/01-research/architecture/system-architecture-v4.md)
  - [Database Design](documents/03-planning/database/database-design.md)
  - [Implementation Plan](documents/03-planning/implementation/kiteclass-implementation-plan.md)
  - [PR Index](documents/03-planning/prs/00-master-pr-index.md)

## 🗂️ File Organization

Để xác định vị trí đúng cho file/folder mới, sử dụng:

```bash
/organize <filename> [type]
```

**Rules**:
- Scripts → `scripts/`
- Docker orchestration → root (docker-compose.*.yml)
- Service-specific → `kiteclass/[service]/`
- Documentation → `documents/[category]/`
- Nginx config → `nginx/`

**See**: `.claude/skills/organize.md` for detailed rules.

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.5.10, Spring Cloud 2025.0.0
- **Frontend**: Next.js 15, React 19, TypeScript
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Storage**: MinIO (S3-compatible)
- **Container**: Docker, Docker Compose

## 📊 Services

| Service | Port | Description |
|---------|------|-------------|
| **Frontend** | 3000 | Next.js UI |
| **Gateway** | 8090 | API Gateway (Auth, Routing) |
| **Core** | 8081 | Business Logic (Students, Teachers, Courses, etc.) |
| **PostgreSQL** | 5432 | Primary Database |
| **Redis** | 6379 | Cache & Sessions |

## 🔐 Development Credentials

**PostgreSQL**:
- Host: localhost:5432
- Database: kiteclass_dev
- User: kiteclass
- Password: kiteclass123

**Redis**:
- Host: localhost:6379

**Note**: Development credentials only - DO NOT use in production.

## 🧪 Testing

- **Unit Tests**: JUnit 5, Mockito, Vitest
- **Integration Tests**: Testcontainers, @SpringBootTest
- **E2E Tests**: Playwright
- **CI**: GitHub Actions with automated tests

## 📖 More Info

- **Frontend**: [kiteclass/kiteclass-frontend/README.md](kiteclass/kiteclass-frontend/README.md)
- **Gateway**: [kiteclass/kiteclass-gateway/README.md](kiteclass/kiteclass-gateway/README.md)
- **Core**: [kiteclass/kiteclass-core/README.md](kiteclass/kiteclass-core/README.md)
- **Scripts**: [scripts/README.md](scripts/README.md)

---

**Last Updated**: 2026-02-27
