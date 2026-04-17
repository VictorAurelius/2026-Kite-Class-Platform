> **Business Logic Documentation:** See [documents/01-business/kiteclass/](../../documents/01-business/kiteclass/) — Source of Truth

# KiteClass Core Service

KiteClass Core is the main backend service for the KiteClass education management platform.
It provides multi-tenant APIs for managing students, teachers, courses, classes, and more.

## Architecture

- **Multi-tenant isolation** via Hibernate filter on `instance_id` (UUID per tenant)
- **Soft delete** on all entities (`deleted` flag + `markAsDeleted()`)
- **Caching** with Redis (multi-tenant key generator)
- **Event-driven** with Spring ApplicationEvent (e.g., EnrollmentCreatedEvent -> Invoice)
- **MapStruct** for DTO mapping
- Runs as part of the **KiteHub Docker stack** (not standalone)

## Modules (15)

| Module | Description |
|--------|-------------|
| `student` | Student CRUD, search, status management (PENDING/ACTIVE/INACTIVE/GRADUATED/DROPPED) |
| `teacher` | Teacher profiles, assignments to classes |
| `course` | Course catalog, pricing, curriculum |
| `clazz` | Class instances, scheduling, capacity management |
| `attendance` | Attendance tracking per class session |
| `enrollment` | Student-class enrollment, tuition/discount calculation |
| `grade` | Grading and score management |
| `invoice` | Invoice generation from enrollments |
| `payment` | Payment tracking and reconciliation |
| `assignment` | Homework and assignment management |
| `lms` | Learning management system features |
| `gamification` | Points, badges, leaderboards |
| `marketing` | Campaigns, promotions |
| `settings` | Tenant-level configuration |
| `storage` | File upload and management |

## Package Structure

```
com.kiteclass.core.module.{module}/
  controller/     # REST controllers
  dto/            # Request/Response DTOs (Java records)
  entity/         # JPA entities (extend BaseEntity)
  mapper/         # MapStruct mappers
  repository/     # Spring Data JPA repositories
  service/        # Service interfaces
  service/impl/   # Service implementations
  event/          # Domain events (optional)
```

## API Overview

All APIs are accessed through the KiteHub Gateway at port `9000`.
Each module exposes standard REST endpoints:

```
GET    /api/v1/{module}          # List with pagination + search
GET    /api/v1/{module}/{id}     # Get by ID
POST   /api/v1/{module}          # Create
PUT    /api/v1/{module}/{id}     # Update
DELETE /api/v1/{module}/{id}     # Soft delete
```

Module-specific endpoints are documented in each controller.

## Local Development

KiteClass Core runs inside the KiteHub Docker stack:

```bash
# Start the full stack (includes kiteclass-core)
cd kitehub && ./scripts/up.sh

# View kiteclass-core logs
cd kitehub && ./scripts/logs.sh kiteclass-core

# Rebuild after code changes
cd kitehub && ./scripts/rebuild.sh kiteclass-core
```

### Prerequisites
- Java 21+
- Docker & Docker Compose
- KiteHub stack running (provides PostgreSQL, Redis, Gateway)

## Testing

```bash
# Run all tests
cd kiteclass/kiteclass-core && ./mvnw test

# Run specific module tests
cd kiteclass/kiteclass-core && ./mvnw test -pl . -Dtest="StudentServiceImplTest"

# Run with coverage
cd kiteclass/kiteclass-core && ./mvnw test jacoco:report
```

### Test types:
- **Unit tests**: Service layer with mocked repositories
- **Integration tests**: Full Spring context with Testcontainers
- **Tenant isolation tests**: Verify multi-tenant data separation

## Business Rules

Business logic documentation lives in:
- `documents/01-business/kiteclass/` (source of truth)

Each domain has a dedicated file with 4 sections: Rules, Flow, Emails, Config.

## Related

- [KiteClass QUICK_START](../QUICK_START.md)
- [KiteHub QUICK_START](../../kitehub/QUICK_START.md)
- [Business Rules](../../documents/01-business/kiteclass/)
