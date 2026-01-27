# Quick Start Prompts for New Claude Sessions

**Choose the prompt based on what you want to work on next.**

**IMPORTANT: All prompts are in English, but Claude should communicate in Vietnamese for easier user control.**

**📋 Full Implementation Plan:** See `/mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md` for the complete 27-PR roadmap with status tracking.

---

## 🎯 Current Status

- **Latest PR:** 2.2 (Core Common Components) ✅ COMPLETE
- **Branch:** feature/core
- **Tests:** 22 tests (22 unit, 0 integration)
- **Tech Stack:** Spring Boot 3.5.10, JPA, PostgreSQL, Redis, RabbitMQ
- **Server Port:** 8081
- **Next:** PR 2.3 (Student Module) - RECOMMENDED

---

## 📚 Completed PRs

- ✅ PR 2.1: Core Project Setup
- ✅ PR 2.2: Core Common Components

---

## 🚀 Available Options

Choose the prompt based on your next task:

### Option 1: PR 2.2 - Core Common Components
### Option 2: General Development/Debug

---

## 🚀 Option 1: PR 2.3 - Student Module

**Copy this prompt when context is cleared:**

```
I'm continuing development on KiteClass Core Service.

CURRENT STATE:
- Working directory: /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-core
- Branch: feature/core
- Completed: PR 2.1 (Core Project Setup), PR 2.2 (Core Common Components)
- Status: 22 tests passing, BaseEntity + DTOs + Exceptions + Enums + Configs ready

DOCUMENTATION TO READ:
1. /mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md
2. /mnt/e/person/2026-Kite-Class-Platform/documents/plans/kiteclass-core-service-plan.md

MY REQUEST: Implement PR 2.3 - Student Module

REQUIREMENTS:
- Create Student entity with JPA annotations (extends BaseEntity)
- Create StudentRepository with custom queries:
  - findByIdAndDeletedFalse
  - existsByEmailAndDeletedFalse
  - findBySearchCriteria (search, status, pageable)
- Create StudentMapper (MapStruct)
- Create StudentService interface and StudentServiceImpl:
  - createStudent
  - getStudentById
  - getStudents (paginated, searchable)
  - updateStudent
  - deleteStudent (soft delete)
- Create StudentController with REST endpoints

TESTS REQUIRED:
- StudentServiceTest (unit tests)
- StudentControllerTest (unit tests)
- StudentRepositoryTest (integration tests with Testcontainers)
- StudentMapperTest
- StudentTestDataBuilder (test utility)

FLYWAY MIGRATION:
- V2__create_student_tables.sql

CONSTRAINTS:
- Follow code-style.md skill (JavaDoc, @since 2.3.0)
- Follow api-design.md skill (RESTful conventions)
- Follow testing-guide.md skill (test patterns)

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

**NOTE: Please communicate in Vietnamese (Tiếng Việt) for easier control.**

Please read the docs first, then help me implement step by step.
```

---

## 📝 Option 2: General Development/Debug

**Copy this prompt for general development/debugging:**

```
I'm working on KiteClass Core Service and need help.

CONTEXT:
- Working directory: /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-core
- Branch: feature/core
- Completed PRs: 2.1 (Core Project Setup)
- Status: Spring Boot 3.5.10, JPA, PostgreSQL configured

DOCUMENTATION:
Please read:
1. /mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md
2. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-core/docs/QUICK-START.md

MY ISSUE/REQUEST:
[Describe your issue or what you want to implement]

WHAT I'VE TRIED:
[What you've already attempted]

**NOTE: Please communicate in Vietnamese (Tiếng Việt) for easier control.**

Please help me solve this or implement the feature.
```

---

## 🛠️ Quick Commands Reference

### Start Docker Services

```bash
# Start PostgreSQL, Redis, RabbitMQ (from gateway folder)
cd /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
docker-compose up -d
```

### Run Tests

```bash
cd /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-core

# Unit tests only
./mvnw test

# All tests
./mvnw clean verify
```

### Compile & Run

```bash
# Compile
./mvnw clean compile

# Run application
./mvnw spring-boot:run

# Run with profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Git Workflow

```bash
# Check current branch
git branch -vv

# Commit after PR complete
git add -A
git commit -m "feat(core): your message

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## 📚 Important Files

- **📋 Full Implementation Plan:** `/mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md`
- **Project Plan:** `documents/plans/kiteclass-core-service-plan.md`
- **Skills:** `/mnt/e/person/2026-Kite-Class-Platform/.claude/skills/`
- **Main Application:** `src/main/java/com/kiteclass/core/KiteclassCoreApplication.java`
- **Configuration:** `src/main/resources/application.yml`
- **POM:** `pom.xml`

---

## ✅ Checklist Before Starting

- [ ] Docker Desktop is running (for PostgreSQL, Redis, RabbitMQ)
- [ ] Java 17 is installed (`java -version`)
- [ ] Maven wrapper is executable (`./mvnw --version`)
- [ ] Can access documentation files
- [ ] Git working tree is clean (`git status`)

---

## 📊 Test Coverage Summary

| Module | Unit Tests | Integration Tests | Status |
|--------|-----------|------------------|--------|
| Common | 22 | 0 | ✅ Complete |
| **Total** | **22** | **0** | ✅ Common ready |

---

## 🗺️ Project Roadmap

### Phase 1: Core Backend Setup ✅
- [x] PR 2.1: Core Project Setup
- [x] PR 2.2: Core Common Components

### Phase 2: Domain Modules 🔄
- [ ] PR 2.3: Student Module (NEXT)
- [ ] PR 2.3: Student Module
- [ ] PR 2.4: Course Module
- [ ] PR 2.5: Class Module
- [ ] PR 2.6: Enrollment Module
- [ ] PR 2.7: Attendance Module
- [ ] PR 2.8: Invoice & Payment Module
- [ ] PR 2.9: Settings & Parent Module
- [ ] PR 2.10: Core Docker & Final Integration

---

## 💡 Tips

1. **Always read documentation first** before implementing
2. **Follow existing patterns** from Gateway service
3. **Run tests after changes**: `./mvnw test`
4. **Use Docker for database**: Gateway's docker-compose includes PostgreSQL
5. **Commit frequently** with good messages
6. **Ask questions in Vietnamese** for better communication

---

## 📞 Support

If you need help:
- Check skill files in `.claude/skills/`
- Review implementation plan in `documents/scripts/kiteclass-implementation-plan.md`
- Check Gateway implementation for patterns

---

**Last Updated:** 2026-01-27 (PR 2.2 Core Common Components)
**Quick Reference:** Copy the appropriate prompt above based on your needs
**Language:** English prompts, but **communicate in Vietnamese (Tiếng Việt)**
