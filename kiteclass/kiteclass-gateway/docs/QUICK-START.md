# Quick Start Prompts for New Claude Sessions

**Choose the prompt based on what you want to work on next.**

**IMPORTANT: All prompts are in English, but Claude should communicate in Vietnamese for easier user control.**

**📋 Full Implementation Plan:** See `/mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md` for the complete 27-PR roadmap with status tracking.

---

## 🎯 Current Status

- **Latest PR:** 1.6 (Gateway Configuration) ✅ COMPLETE
- **Branch:** feature/gateway
- **Gateway Service:** ✅ 100% COMPLETE (7/7 PRs)
- **Tests:** 95 tests passing (55 unit + 40 integration)
- **Docker:** Complete with PostgreSQL, Redis
- **Features:** Auth, Email, Rate Limiting, Logging
- **Next:** PR 2.1 (Core Service Integration) - RECOMMENDED

---

## 📚 Completed PRs

- ✅ PR 1.1: Project Setup
- ✅ PR 1.2: Common Components
- ✅ PR 1.3: User Module
- ✅ PR 1.4: Auth Module
- ✅ PR 1.4.1: Docker Setup & Integration Tests
- ✅ PR 1.5: Email Service
- ✅ PR 1.6: Gateway Configuration (Rate Limiting + Logging)

---

## 🚀 Available Options

Choose the prompt based on your next task:

### Option 1: PR 2.1 - Core Service Integration
### Option 2: CI/CD Setup (GitHub Actions)
### Option 3: PR 1.6 - Email Verification
### Option 4: General Development/Debug

---

## 🚀 Option 1: PR 2.1 - Core Service Integration

**Copy this prompt when context is cleared:**

```
I'm continuing development on KiteClass Gateway Service.

CURRENT STATE:
- Working directory: /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed: PR 1.5 (Email Service)
- Status: 42 tests passing, Docker setup complete, email service integrated

DOCUMENTATION TO READ:
1. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md
2. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.5-SUMMARY.md
3. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/filter/AuthenticationFilter.java

MY REQUEST: Implement PR 2.1 - Core Service Integration

REQUIREMENTS:
- Create mock kiteclass-core-service (simple Spring Boot app)
- Configure Gateway routes to Core Service
- Test AuthenticationFilter with real downstream service:
  - Verify X-User-Id header propagation
  - Verify X-User-Roles header propagation
  - Test JWT validation works end-to-end
- Add integration tests for service-to-service calls
- Document integration pattern
- Add troubleshooting guide

APPROACH:
- Create kiteclass-core-service-mock module in parent folder
- Simple REST API with /api/test endpoint
- Configure Gateway to route /api/core/** to Core Service
- Test with curl and integration tests

CONSTRAINTS:
- Follow reactive patterns
- Use existing AuthenticationFilter
- Don't break existing 42 tests

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

**NOTE: Please communicate in Vietnamese (Tiếng Việt) for easier control.**

Please read the docs first, then help me implement step by step.
```

---

## 🤖 Option 2: CI/CD Setup (GitHub Actions)

**Copy this prompt when context is cleared:**

```
I'm continuing development on KiteClass Gateway Service.

CURRENT STATE:
- Working directory: /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed: PR 1.5 (Email Service)
- Status: 42 tests passing (37 unit + 5 email), Docker setup complete

DOCUMENTATION TO READ:
1. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md
2. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/guides/DOCKER-SETUP.md
3. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/guides/TESTING.md

MY REQUEST: Setup CI/CD with GitHub Actions

REQUIREMENTS:
- Create .github/workflows/test.yml:
  - Run on push and PR to main/feature branches
  - Setup Java 17
  - Start Docker services (postgres, redis)
  - Run all tests: ./mvnw clean verify
  - Upload coverage reports (Codecov)
  - Cache Maven dependencies
  - Timeout: 15 minutes
- Create .github/workflows/build.yml:
  - Build Docker image
  - Tag with commit SHA and branch name
  - Push to registry (optional)
- Add status badges to README.md
- Document CI/CD workflow

CONSTRAINTS:
- Must work with Testcontainers (Docker-in-Docker)
- Fast feedback (< 10 minutes ideally)
- Fail pipeline if any test fails

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

**NOTE: Please communicate in Vietnamese (Tiếng Việt) for easier control.**

Please read the docs first, then help me implement step by step.
```

---

## 📧 Option 3: PR 1.6 - Email Verification

**Copy this prompt when context is cleared:**

```
I'm continuing development on KiteClass Gateway Service.

CURRENT STATE:
- Working directory: /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed: PR 1.5 (Email Service with password reset)
- Status: 42 tests passing, email service integrated

DOCUMENTATION TO READ:
1. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md
2. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.5-SUMMARY.md
3. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/service/EmailService.java

MY REQUEST: Implement PR 1.6 - Email Verification

REQUIREMENTS:
- Add email verification for new users
- Create email_verification_tokens table
- Send verification email on user creation
- Add verify-email endpoint (GET with token)
- Prevent login if email not verified
- Add resend verification email endpoint
- Update user creation to send welcome + verification email
- Add unit + integration tests

CONSTRAINTS:
- Follow PR 1.5 email pattern
- Use existing EmailService
- Follow reactive patterns
- Don't break existing 42 tests

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

**NOTE: Please communicate in Vietnamese (Tiếng Việt) for easier control.**

Please read the docs first, then help me implement step by step.
```

---

## 📝 Option 4: General Development/Debug

**Copy this prompt for general development/debugging:**

```
I'm working on KiteClass Gateway Service and need help.

CONTEXT:
- Working directory: /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed PRs: 1.1, 1.2, 1.3 (User), 1.4 (Auth), 1.4.1 (Docker+Tests), 1.5 (Email)
- Status: 42 tests passing

DOCUMENTATION:
Please read:
1. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md
2. /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.5-SUMMARY.md

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
cd /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
docker-compose up -d
```

### Run Tests
```bash
# Unit tests only (no Docker)
./mvnw test -Dtest='*Test'

# All tests (requires Docker)
./mvnw clean verify
```

### Check Email Service
```bash
# Request password reset
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@kiteclass.local"}'
```

### Git Workflow
```bash
# Create branch for next PR
git checkout -b feature/pr-2.1-core-integration

# After work
git add -A
git commit -m "feat(gateway): your message

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

# Merge to feature/gateway
git checkout feature/gateway
git merge feature/pr-2.1-core-integration
```

---

## 📚 Important Files

- **📋 Full Implementation Plan:** `/mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md`
- **Project Overview:** `README.md`
- **PR Summaries:** `docs/pr-summaries/PR-*.md`
- **Setup Guides:** `docs/guides/DOCKER-SETUP.md`, `docs/guides/TESTING.md`
- **Email Service:** `src/main/java/com/kiteclass/gateway/service/EmailService.java`
- **Auth Service:** `src/main/java/com/kiteclass/gateway/module/auth/service/impl/AuthServiceImpl.java`
- **Skills:** `/mnt/e/person/2026-Kite-Class-Platform/.claude/skills/`

---

## ✅ Checklist Before Starting

- [ ] Docker Desktop is running
- [ ] Java 17 is installed (`java -version`)
- [ ] Maven wrapper is executable (`./mvnw --version`)
- [ ] Can access documentation files
- [ ] Git working tree is clean (`git status`)

---

## 📊 Test Coverage Summary

| Module | Unit Tests | Integration Tests | Status |
|--------|-----------|------------------|--------|
| Common | 10 | 0 | ✅ 100% |
| User | 8 | 13 | ✅ 100% |
| Auth | 9 | 9 | ✅ 100% |
| JWT | 10 | 10 | ✅ 100% |
| Email | 5 | 8 | ✅ 100% |
| Filters | 13 | 0 | ✅ 100% |
| **Total** | **55** | **40** | **✅ 95 tests** |

---

## 🗺️ Project Roadmap

### Phase 1: Core Backend (Gateway + Auth) ✅ COMPLETE
- [x] PR 1.1: Project Setup
- [x] PR 1.2: Common Components
- [x] PR 1.3: User Module
- [x] PR 1.4: Auth Module
- [x] PR 1.4.1: Docker Setup & Integration Tests
- [x] PR 1.5: Email Service
- [x] PR 1.6: Gateway Configuration (Rate Limiting + Logging)

### Phase 2: Service Integration 🔄
- [ ] PR 2.1: Core Service Integration
- [ ] PR 2.2: Advanced Security (rate limiting)
- [ ] PR 2.3: Email Verification
- [ ] PR 2.4: Notification Service

### Phase 3: Operations 📋
- [ ] CI/CD Setup (GitHub Actions)
- [ ] Monitoring & Logging
- [ ] Production Deployment

---

## 💡 Tips

1. **Always read documentation first** before implementing
2. **Follow existing patterns** from completed PRs
3. **Run tests after changes**: `./mvnw test`
4. **Use Docker for integration tests**: `docker-compose up -d`
5. **Commit frequently** with good messages
6. **Ask questions in Vietnamese** for better communication

---

## 📞 Support

If you need help:
- Check skill files in `.claude/skills/`
- Read PR summaries in `docs/pr-summaries/`
- Review guides in `docs/guides/`

---

**Last Updated:** 2026-01-27 (PR 1.6 Gateway Configuration - Gateway Service COMPLETE)
**Quick Reference:** Copy the appropriate prompt above based on your needs
**Language:** English prompts, but **communicate in Vietnamese (Tiếng Việt)**
