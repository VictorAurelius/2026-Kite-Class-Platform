# Quick Start Prompts for New Claude Sessions

**Choose the prompt based on what you want to work on next.**

---

## 🚀 Option 1: PR 1.5 - Email Service

**Copy this prompt when context is cleared:**

```
I'm continuing development on KiteClass Gateway Service.

CURRENT STATE:
- Working directory: /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed: PR 1.4 (Auth Module) + PR 1.4.1 (Docker + Tests)
- Status: 77 tests, Docker setup complete, ready for next PR

DOCUMENTATION TO READ:
1. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md
2. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.4-SUMMARY.md
3. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.4.1-SUMMARY.md

MY REQUEST: Implement PR 1.5 - Email Service

REQUIREMENTS:
- Add email sending for password reset (forgot-password endpoint logs only)
- Create EmailService with Spring Boot Mail
- Email templates with Thymeleaf (password-reset, welcome, account-locked)
- SMTP configuration (Gmail for dev)
- Update AuthService to use EmailService
- Add unit + integration tests
- Update documentation

CONSTRAINTS:
- Follow reactive patterns (Mono/Flux)
- Use existing error handling (BusinessException)
- Use i18n from messages.properties
- Follow code style from PR 1.4
- Don't break existing 77 tests

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

Please read the docs first, then help me implement step by step.
```

---

## 🔗 Option 2: PR 2.1 - Core Service Integration

**Copy this prompt when context is cleared:**

```
I'm continuing development on KiteClass Gateway Service.

CURRENT STATE:
- Working directory: /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed: PR 1.4 (Auth Module) + PR 1.4.1 (Docker + Tests)
- Status: 77 tests, Docker setup complete, ready for next PR

DOCUMENTATION TO READ:
1. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md
2. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.4-SUMMARY.md
3. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/filter/AuthenticationFilter.java

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

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

Please read the docs first, then help me implement step by step.
```

---

## 🤖 Option 3: CI/CD Setup

**Copy this prompt when context is cleared:**

```
I'm continuing development on KiteClass Gateway Service.

CURRENT STATE:
- Working directory: /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed: PR 1.4 (Auth Module) + PR 1.4.1 (Docker + Tests)
- Status: 77 tests (37 unit + 40 integration with Docker), ready for CI/CD

DOCUMENTATION TO READ:
1. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md
2. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/guides/DOCKER-SETUP.md
3. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/guides/TESTING.md

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
- Generate and upload coverage reports

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com
- GitHub: (if needed)

Please read the docs first, then help me implement step by step.
```

---

## 📝 Option 4: General Development

**Copy this prompt for general development/debugging:**

```
I'm working on KiteClass Gateway Service and need help.

CONTEXT:
- Working directory: /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed PRs: 1.1, 1.2, 1.3 (User), 1.4 (Auth), 1.4.1 (Docker+Tests)

DOCUMENTATION:
Please read:
1. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md
2. /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.4.1-SUMMARY.md

MY ISSUE/REQUEST:
[Describe your issue or what you want to implement]

WHAT I'VE TRIED:
[What you've already attempted]

Please help me solve this or implement the feature.
```

---

## 🛠️ Quick Commands Reference

### Start Docker Services
```bash
cd /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
docker-compose up -d
```

### Run Tests
```bash
# Unit tests (no Docker)
./mvnw test -Dtest='JwtTokenProviderTest,AuthServiceTest,UserServiceTest'

# All tests (requires Docker)
./mvnw clean verify
```

### Git Workflow
```bash
# Create branch for next PR
git checkout -b feature/pr-1.5-email-service

# After work
git add -A
git commit -m "feat(gateway): your message"

# Merge to feature/gateway
git checkout feature/gateway
git merge feature/pr-1.5-email-service
```

---

## 📚 Important Files

- **Project Overview:** `README.md`
- **PR Summaries:** `docs/pr-summaries/PR-*.md`
- **Setup Guides:** `docs/guides/DOCKER-SETUP.md`, `docs/guides/TESTING.md`
- **Original Plan:** `/mnt/f/nam4/doan/2026-Kite-Class-Platform/documents/plans/kiteclass-gateway-plan.md`
- **Skills:** `/mnt/f/nam4/doan/2026-Kite-Class-Platform/.claude/skills/`

---

## ✅ Checklist Before Starting

- [ ] Docker Desktop is running
- [ ] Java 17 is installed (`java -version`)
- [ ] Maven wrapper is executable (`./mvnw --version`)
- [ ] Can access documentation files
- [ ] Git working tree is clean (`git status`)

---

**Last Updated:** 2026-01-26
**Quick Reference:** Copy the appropriate prompt above based on your needs
