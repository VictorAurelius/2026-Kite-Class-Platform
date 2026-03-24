# Continue Development Prompt - KiteClass Gateway

**Use this prompt when starting a new Claude session to continue development.**

---

## 📋 Prompt for New Claude Session

```
I'm continuing development on KiteClass Gateway Service (microservices platform for online education).

PROJECT CONTEXT:
- Working directory: /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Current branch: feature/gateway
- Git repo: Yes
- Platform: WSL2 Ubuntu on Windows

COMPLETED WORK:
✅ PR 1.1: Project Setup (Spring Boot 3.5.10, WebFlux, R2DBC)
✅ PR 1.2: Common Components (DTOs, Exceptions, MessageService)
✅ PR 1.3: User Module (CRUD, RBAC, soft delete)
✅ PR 1.4: Auth Module (JWT, refresh tokens, account locking)
✅ PR 1.4.1: Docker Setup + Integration Tests (77 tests total)

CURRENT STATUS:
- Total commits: 18 commits on feature/gateway
- All code committed and clean working tree
- 37/37 core unit tests passing (100%)
- 40 integration tests created (require Docker)
- Docker Compose setup complete (PostgreSQL + Redis + Gateway)

DOCUMENTATION TO READ:
Please read these files to understand the project:

1. PROJECT OVERVIEW:
   /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/README.md

2. PR SUMMARIES (understand what's been built):
   /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.3-SUMMARY.md
   /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.4-SUMMARY.md
   /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/pr-summaries/PR-1.4.1-SUMMARY.md

3. DOCKER SETUP:
   /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/guides/DOCKER-SETUP.md

4. ORIGINAL PLAN:
   /mnt/f/nam4/doan/2026-Kite-Class-Platform/documents/plans/kiteclass-gateway-plan.md

NEXT STEPS OPTIONS:

Option A - PR 1.5: Email Service (Optional)
- Implement email sending for password reset
- Email templates with Thymeleaf
- Email verification for new users
- SMTP configuration

Option B - PR 2.1: Core Service Integration
- Test Gateway filter with real Core Service
- Verify X-User-Id and X-User-Roles headers work
- End-to-end authentication testing
- Service discovery integration

Option C - Setup CI/CD
- GitHub Actions workflow
- Automated testing with Docker
- Code coverage reports
- Automated deployment

MY REQUEST:
I want to work on: [USER WILL SPECIFY: PR 1.5 / PR 2.1 / CI/CD / Other]

Please:
1. Read the documentation files listed above
2. Understand current project state
3. Help me plan and implement the selected option
4. Follow existing code style and patterns from previous PRs
5. Create comprehensive tests (unit + integration)
6. Update documentation

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com
- WSL root password: vkiet432

Ready to start!
```

---

## 🎯 Specific Prompts for Each Option

### Option A: PR 1.5 - Email Service

```
I want to implement PR 1.5: Email Service for KiteClass Gateway.

CONTEXT: Already read the project documentation. Gateway has auth module with forgot-password and reset-password endpoints that currently only log messages. Need to implement actual email sending.

REQUIREMENTS:
1. Add Spring Boot Mail Starter dependency
2. Create EmailService with methods:
   - sendPasswordResetEmail(email, resetToken, resetUrl)
   - sendWelcomeEmail(email, name)
   - sendAccountLockedEmail(email, lockedUntil)
3. Create email templates with Thymeleaf:
   - password-reset.html
   - welcome.html
   - account-locked.html
4. Configure SMTP (Gmail for dev, configurable for prod)
5. Add email templates with nice styling
6. Update AuthService to use EmailService
7. Add unit tests for EmailService
8. Add integration tests
9. Update documentation

CONSTRAINTS:
- Follow reactive patterns (Mono/Flux)
- Use existing error handling patterns
- Use i18n messages from messages.properties
- Follow code style from previous PRs
- Don't break existing tests

Please help me implement this step by step.
```

### Option B: PR 2.1 - Core Service Integration

```
I want to implement PR 2.1: Core Service Integration for KiteClass Gateway.

CONTEXT: Already read the project documentation. Gateway has AuthenticationFilter that adds X-User-Id and X-User-Roles headers for downstream services. Need to test this with real Core Service.

REQUIREMENTS:
1. Review AuthenticationFilter implementation
2. Create mock Core Service for testing (simple Spring Boot app)
3. Configure Gateway routes to Core Service
4. Test header propagation:
   - X-User-Id header present
   - X-User-Roles header present
   - Authentication works end-to-end
5. Add integration tests for service-to-service calls
6. Document the integration pattern
7. Add troubleshooting guide

APPROACH:
- Create simple kiteclass-core-service-mock module
- Configure Spring Cloud Gateway routes
- Test with real HTTP calls
- Verify JWT validation and header propagation

Please help me implement this step by step.
```

### Option C: CI/CD Setup

```
I want to setup CI/CD with GitHub Actions for KiteClass Gateway.

CONTEXT: Already read the project documentation. Project has 77 tests (37 unit + 40 integration). Integration tests require Docker (Testcontainers). Need automated testing on every PR.

REQUIREMENTS:
1. Create .github/workflows/test.yml:
   - Trigger on push and pull_request
   - Setup Java 17
   - Start Docker containers (postgres, redis)
   - Run all tests (./mvnw clean verify)
   - Upload coverage reports
   - Cache Maven dependencies
2. Create .github/workflows/build.yml:
   - Build Docker image
   - Push to registry (optional)
3. Add status badges to README.md
4. Document CI/CD setup

CONSTRAINTS:
- Must work with Docker (Testcontainers)
- Fast feedback (< 10 minutes)
- Fail on test failures
- Generate coverage reports

Please help me implement this step by step.
```

---

## 📚 Key Files Reference

### Project Structure
```
kiteclass-gateway/
├── src/main/java/com/kiteclass/gateway/
│   ├── common/          # DTOs, Exceptions, Constants
│   ├── config/          # SecurityConfig, R2dbcConfig, MessageConfig
│   ├── security/        # JWT, UserPrincipal, SecurityContext
│   ├── filter/          # AuthenticationFilter
│   └── module/
│       ├── auth/        # Login, refresh, logout, password reset
│       └── user/        # CRUD, roles, permissions
├── src/main/resources/
│   ├── application.yml
│   ├── messages.properties
│   └── db/migration/    # V1-V4 SQL migrations
├── src/test/java/
│   └── com/kiteclass/gateway/
│       ├── module/      # Unit tests
│       └── integration/ # Integration tests
├── docs/
│   ├── pr-summaries/    # PR documentation
│   ├── guides/          # Setup guides
│   └── implementation/  # Implementation reports
├── docker-compose.yml   # PostgreSQL + Redis + Gateway
├── Dockerfile           # Multi-stage build
└── README.md
```

### Key Technologies
- Spring Boot 3.5.10 (WebFlux - reactive)
- Spring Security (JWT authentication)
- R2DBC (reactive database)
- PostgreSQL 15
- Redis 7
- Flyway (migrations)
- Testcontainers (integration tests)
- JJWT 0.12.6 (JWT library)

### Default Credentials
```
Email: owner@kiteclass.local
Password: Admin@123
Roles: OWNER (full permissions)
```

### Environment Variables
See `.env.example` for full list:
- DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- REDIS_HOST, REDIS_PORT
- JWT_SECRET (512-bit for HS512)
- JWT_ACCESS_TOKEN_EXPIRATION (default: 1 hour)
- JWT_REFRESH_TOKEN_EXPIRATION (default: 7 days)

---

## 🔧 Quick Commands

### Start Development Environment
```bash
# Start Docker services
docker-compose up -d

# Check status
docker-compose ps
curl http://localhost:8080/actuator/health

# View logs
docker-compose logs -f gateway
```

### Run Tests
```bash
# Unit tests only (no Docker)
./mvnw test -Dtest='JwtTokenProviderTest,AuthServiceTest,UserServiceTest'

# All tests (requires Docker)
docker-compose up -d postgres redis
./mvnw clean verify
```

### Manual API Testing
```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@kiteclass.local","password":"Admin@123"}'

# Get users (requires token)
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Git Workflow
```bash
# Create new branch for next PR
git checkout -b feature/pr-1.5-email-service

# After implementation
git add -A
git commit -m "feat(gateway): implement email service"
git push origin feature/pr-1.5-email-service

# Merge back to feature/gateway
git checkout feature/gateway
git merge feature/pr-1.5-email-service
```

---

## ⚠️ Important Notes

1. **Always read documentation first** before asking questions
2. **Follow existing patterns** from PR 1.3 and PR 1.4
3. **Write tests** (unit + integration) for all new code
4. **Update documentation** after implementation
5. **Use git commit conventions** from .claude/skills/git-workflow.md
6. **Don't break existing tests** (37/37 must still pass)
7. **Integration tests require Docker** running

---

## 📞 If You Get Stuck

Check these resources:
1. `/mnt/f/nam4/doan/2026-Kite-Class-Platform/.claude/skills/` - All project skills
2. `docs/pr-summaries/` - Detailed PR documentation
3. `docs/guides/TESTING.md` - Testing guide
4. `docs/guides/DOCKER-SETUP.md` - Docker troubleshooting

---

**Last Updated:** 2026-01-26
**Current Branch:** feature/gateway
**Total Commits:** 18 commits
**Total Tests:** 77 tests (37 unit + 40 integration)
**Status:** ✅ Ready for next PR

**Next PR Options:**
- [ ] PR 1.5: Email Service
- [ ] PR 2.1: Core Service Integration
- [ ] CI/CD Setup
- [ ] Other (specify)
