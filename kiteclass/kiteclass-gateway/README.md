# KiteClass Gateway Service

API Gateway + User Management Service for KiteClass Platform.

## 🚀 Features

- **JWT Authentication** (Access + Refresh tokens)
- **User Management** (CRUD, roles, permissions)
- **API Gateway** (Route to downstream services)
- **Role-Based Access Control** (RBAC)
- **Account Security** (Login tracking, account locking)
- **Rate Limiting** (Bucket4j - IP & User based)
- **CORS Support** (Multi-origin)
- **Email Service** (Password reset, welcome emails)
- **Cross-Service Integration** (Profile fetching from Core Service via Feign)
- **Graceful Degradation** (Handles Core service unavailability)

## 📚 Documentation

### Pull Request Summaries
- [PR 1.3: User Module](docs/pr-summaries/PR-1.3-SUMMARY.md)
- [PR 1.4: Auth Module](docs/pr-summaries/PR-1.4-SUMMARY.md)
- [PR 1.4.1: Docker + Tests](docs/pr-summaries/PR-1.4.1-SUMMARY.md)
- [PR 1.5: Email Service](docs/pr-summaries/PR-1.5-SUMMARY.md)
- [PR 1.8: Cross-Service Integration](docs/pr-summaries/PR-1.8-SUMMARY.md) ⚠️ **Latest - Partial**

### Test Reports
- [Test Results (Final)](docs/test-reports/TEST-RESULTS-FINAL.md)
- [Testing Complete](docs/test-reports/TESTING-COMPLETE.md)

### Implementation Reports
- [PR 1.4 Implementation Complete](docs/implementation/IMPLEMENTATION-COMPLETE-PR-1.4.md)
- [PR 1.4 Commit History](docs/implementation/COMMIT-HISTORY-PR-1.4.md)

### Guides
- [Testing Guide](docs/guides/TESTING.md) - How to run tests
- [Docker Setup Guide](docs/guides/DOCKER-SETUP.md) - Docker setup and deployment

## 🛠️ Quick Start

Choose your setup method:

### Option 1: Docker Setup (Recommended) 🐳

**Easiest way to get started - no installation needed!**

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Start all services (PostgreSQL, Redis, Gateway)
docker-compose up -d

# 3. Check status
docker-compose ps

# 4. View logs
docker-compose logs -f gateway

# 5. Test API
curl http://localhost:8080/actuator/health
```

**Done!** Gateway running at http://localhost:8080

See [Docker Setup Guide](docs/guides/DOCKER-SETUP.md) for details.

---

### Option 2: Local Setup (Manual)

**Prerequisites:**
- Java 17+
- PostgreSQL 15+
- Redis 7+
- Maven 3.9+

**Setup:**

### Setup

```bash
# 1. Setup Java (first time only)
scripts/setup/setup-java.sh
source ~/.bashrc

# 2. Configure database
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=kiteclass_dev
export DB_USERNAME=kiteclass
export DB_PASSWORD=kiteclass123

# 3. Configure JWT (REQUIRED for production)
export JWT_SECRET="your-production-secret-min-512-bits"

# 4. Run application
./mvnw spring-boot:run
```

### Run Tests

```bash
# All tests
./mvnw test

# Specific test suite
./mvnw test -Dtest=JwtTokenProviderTest
./mvnw test -Dtest=AuthServiceTest

# Using test script
scripts/test/run-tests.sh
```

### Manual API Testing

```bash
# Start server
./mvnw spring-boot:run

# Run automated auth flow tests
scripts/test/test-auth-flow.sh

# Or test manually
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@kiteclass.local","password":"Admin@123"}'
```

## 📦 Project Structure

```
kiteclass-gateway/
├── docs/                           # All documentation
│   ├── pr-summaries/               # PR summaries
│   ├── test-reports/               # Test reports
│   ├── implementation/             # Implementation reports
│   └── guides/                     # User/developer guides
│
├── scripts/                        # Utility scripts
│   ├── setup/                      # Setup scripts
│   └── test/                       # Test scripts
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/kiteclass/gateway/
│   │   │       ├── common/         # Shared components
│   │   │       ├── config/         # Configuration
│   │   │       ├── security/       # JWT, UserPrincipal
│   │   │       ├── filter/         # Gateway filters
│   │   │       └── module/         # Business modules
│   │   │           ├── auth/       # Auth module (PR 1.4)
│   │   │           └── user/       # User module (PR 1.3)
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── messages.properties
│   │       └── db/migration/       # Flyway migrations
│   └── test/                       # Tests
│
├── pom.xml
└── README.md
```

## 🔑 Default Credentials

```
Email: owner@kiteclass.local
Password: Admin@123
Roles: OWNER (full permissions)
```

## 🔧 Configuration

### JWT Settings

```yaml
jwt:
  secret: ${JWT_SECRET:development-only-secret}
  access-token-expiration: 3600000      # 1 hour
  refresh-token-expiration: 604800000   # 7 days
```

⚠️ **Production:** MUST set `JWT_SECRET` environment variable!

### Database

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### Redis

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

## 🧪 Testing

**Test Results (PR 1.8):**
- ✅ Unit tests: 86/86 (100%)
- ✅ Integration tests: 32 tests (require Docker)
- Total: 118 tests

**With Docker (Recommended):**
```bash
docker-compose up -d postgres redis
./mvnw clean verify
```

**Without Docker (Unit tests only):**
```bash
./mvnw test -Dtest='JwtTokenProviderTest,AuthServiceTest,UserServiceTest'
```

See [Testing Guide](docs/guides/TESTING.md) and [Docker Setup](docs/guides/DOCKER-SETUP.md) for details.

## 🚀 Deployment

Coming soon in future PRs.

## 📖 API Documentation

### Auth Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Login with email/password |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout (invalidate token) |
| POST | `/api/v1/auth/forgot-password` | Request password reset |
| POST | `/api/v1/auth/reset-password` | Reset password |

### User Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/v1/users` | List all users | ADMIN, OWNER, STAFF |
| GET | `/api/v1/users/{id}` | Get user by ID | ADMIN, OWNER, STAFF |
| POST | `/api/v1/users` | Create user | ADMIN, OWNER |
| PUT | `/api/v1/users/{id}` | Update user | ADMIN, OWNER |
| DELETE | `/api/v1/users/{id}` | Delete user | OWNER |

## 🛣️ Roadmap

- [x] PR 1.1: Project Setup
- [x] PR 1.2: Common Components
- [x] PR 1.3: User Module
- [x] PR 1.4: Auth Module
- [x] PR 1.4.1: Docker Setup & Integration Tests
- [x] PR 1.5: Email Service
- [x] PR 1.6: Gateway Configuration
- [x] PR 1.8: Cross-Service Integration ⚠️ **Partial - Current** (STUDENT ✅, TEACHER/PARENT ⏳)
- [ ] Core Teacher/Parent Modules (blocking PR 1.8 completion)
- [ ] PR 1.8.1: Complete TEACHER/PARENT Profile Fetching

## 👥 Contributors

- VictorAurelius (vankiet14491@gmail.com)
- Claude Sonnet 4.5 (AI Assistant)

## 📄 License

Internal Project - KiteClass Platform V3.1

---

**Version:** 1.0.0-SNAPSHOT
**Last Updated:** 2026-04-29 (freshness re-confirmed; content reflects current state of PR 1.8 — no drift since 2026-01-28)
**Status:** ✅ Active Development - 7/8 PRs Complete (87.5%)
