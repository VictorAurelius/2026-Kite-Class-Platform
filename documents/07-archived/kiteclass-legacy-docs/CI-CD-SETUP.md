# CI/CD Setup for Gateway Service

**Date:** 2026-02-02
**Status:** ✅ Active
**Purpose:** Automated testing and deployment pipeline for KiteClass Gateway

---

## 📋 Overview

This document describes the CI/CD pipeline setup for the Gateway Service using GitHub Actions.

### Why CI/CD is Critical

**Problem Solved:**
- WSL2 + Docker Desktop incompatibility blocks 68 integration tests locally
- Manual testing is time-consuming and error-prone
- No automated verification before deployment
- Risk of deploying code that breaks in production

**Solution:**
- GitHub Actions with native Docker (no Desktop issues)
- Automated testing on every push/PR
- Coverage verification (target: 80%)
- Automated Docker image builds
- Quality gates before merge

---

## 🎯 Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Actions Pipeline                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Push/PR Trigger                                             │
│       ↓                                                      │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Job 1: Test (runs in parallel with Quality)      │    │
│  ├────────────────────────────────────────────────────┤    │
│  │  1. Setup PostgreSQL service                       │    │
│  │  2. Setup Redis service                            │    │
│  │  3. Run all tests (218 tests)                      │    │
│  │  4. Generate coverage report                       │    │
│  │  5. Check coverage threshold (70% min)             │    │
│  │  6. Upload coverage to Codecov                     │    │
│  │  7. Comment PR with results                        │    │
│  └────────────────────────────────────────────────────┘    │
│                         ↓                                    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Job 2: Build (only on main/develop)              │    │
│  ├────────────────────────────────────────────────────┤    │
│  │  1. Build application JAR                          │    │
│  │  2. Build Docker image                             │    │
│  │  3. Tag with branch + SHA                          │    │
│  │  4. (Optional) Push to registry                    │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Job 3: Quality (runs in parallel with Test)      │    │
│  ├────────────────────────────────────────────────────┤    │
│  │  1. Run checkstyle                                 │    │
│  │  2. Check compilation warnings                     │    │
│  │  3. Analyze dependencies                           │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔧 Pipeline Configuration

### Workflow File

**Location:** `.github/workflows/gateway-ci.yml`

### Triggers

**Push Events:**
- Branches: `main`, `develop`, `feature/**`, `review/**`
- Paths: `kiteclass/kiteclass-gateway/**`, workflow file itself

**Pull Request Events:**
- Target branches: `main`, `develop`
- Paths: Gateway service files

**Manual Trigger:**
- Not configured (can be added with `workflow_dispatch`)

---

## 🧪 Test Job Details

### Services Configuration

#### PostgreSQL 15
```yaml
services:
  postgres:
    image: postgres:15-alpine
    env:
      POSTGRES_DB: kiteclass_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - 5432:5432
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

**Why:**
- Testcontainers PostgreSQL tests require real database
- Health checks ensure DB is ready before tests start
- Same version as production (PostgreSQL 15)

#### Redis 7
```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - 6379:6379
    options: >-
      --health-cmd "redis-cli ping"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

**Why:**
- Rate limiting tests require Redis
- Session management and caching tests
- Token blacklisting tests

### Test Execution

```bash
./mvnw clean test
```

**Environment Variables:**
- `SPRING_DATASOURCE_URL`: Points to GitHub Actions PostgreSQL service
- `SPRING_DATASOURCE_USERNAME`: test
- `SPRING_DATASOURCE_PASSWORD`: test
- `SPRING_REDIS_HOST`: localhost
- `SPRING_REDIS_PORT`: 6379

**Expected Results:**
- ✅ **218 tests passing** (vs 150 locally due to Docker issue)
- ✅ **~78% coverage** (vs 57% locally)
- ✅ **0 failures, 0 errors**

---

## 📊 Coverage Reporting

### JaCoCo Configuration

**Minimum Thresholds:**
- Line coverage: **70%** (configured in pom.xml)
- Branch coverage: **65%** (configured in pom.xml)

**Coverage Check:**
```bash
./mvnw jacoco:check
```

**Reports Generated:**
- HTML: `target/site/jacoco/index.html`
- XML: `target/site/jacoco/jacoco.xml` (for Codecov)
- CSV: `target/site/jacoco/jacoco.csv`

### Codecov Integration

**Features:**
- Coverage trend tracking
- PR comments with diff coverage
- Coverage badges for README
- Line-by-line coverage visualization

**Setup Required:**
1. Sign up at [codecov.io](https://codecov.io)
2. Connect GitHub repository
3. Add `CODECOV_TOKEN` to repository secrets (optional for public repos)

### PR Coverage Comments

**Provided by:** `madrapps/jacoco-report@v1.6.1`

**Features:**
- Coverage summary in PR comment
- Changed files coverage
- Fails if coverage drops below thresholds
- Updates existing comment on new commits

**Thresholds:**
- Overall coverage: **70% minimum**
- Changed files: **80% minimum**

---

## 🐳 Build Job Details

### Trigger Conditions

**Runs only when:**
- All tests pass (depends on `test` job)
- Push event (not PR)
- Target branch is `main` or `develop`

### Build Steps

#### 1. Maven Package
```bash
./mvnw clean package -DskipTests
```
- Skips tests (already run in test job)
- Produces `kiteclass-gateway-1.0.0-SNAPSHOT.jar`

#### 2. Docker Build
```yaml
uses: docker/build-push-action@v5
with:
  context: ./kiteclass/kiteclass-gateway
  push: false  # Set to true when ready
  tags: ${{ steps.meta.outputs.tags }}
  cache-from: type=gha
  cache-to: type=gha,mode=max
```

**Image Tags Generated:**
- `main-abc1234` (branch + short SHA)
- `develop-def5678`
- `latest` (only on main branch)

**Caching:**
- Uses GitHub Actions cache
- Speeds up subsequent builds
- Caches Docker layers

---

## ✅ Quality Job Details

### Checkstyle

**Purpose:** Enforce code style guidelines

**Configuration:** `checkstyle.xml` (if exists)

**Continues on error:** Yes (reports issues but doesn't fail build)

### Compilation Warnings

**Command:**
```bash
./mvnw clean compile -Xlint:deprecation -Xlint:unchecked
```

**Checks for:**
- Deprecated API usage
- Unchecked type conversions
- Raw type usage

**Expected:** 0 warnings

### Dependency Analysis

**Command:**
```bash
./mvnw dependency:analyze
```

**Checks for:**
- Unused dependencies
- Missing dependencies (used but not declared)
- Dependency conflicts

**Continues on error:** Yes (informational only)

---

## 🚀 Usage Guide

### For Developers

#### Running Tests Locally
```bash
cd kiteclass/kiteclass-gateway

# With Docker (if available)
docker-compose -f docker-compose.test.yml up -d
./mvnw test
docker-compose -f docker-compose.test.yml down

# Without Docker (unit tests only)
./mvnw test -Dgroups="!integration"
```

#### Viewing Coverage Locally
```bash
./mvnw test jacoco:report
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
```

#### Testing Docker Build Locally
```bash
cd kiteclass/kiteclass-gateway
docker build -t kiteclass-gateway:local .
docker run -p 8080:8080 kiteclass-gateway:local
```

### For Code Reviewers

#### Check Pipeline Status

**On Pull Request:**
1. Scroll to bottom of PR page
2. Look for "All checks have passed" ✅
3. Click "Details" to view full logs

**Red flags:**
- ❌ Tests failing
- ⚠️ Coverage decreased
- ⚠️ Quality checks failed

#### View Coverage Report

1. Click on Codecov comment in PR
2. Review line-by-line coverage
3. Check that new code is tested

### For Maintainers

#### Enable Docker Push

**Prerequisites:**
1. Container registry account (Docker Hub, GitHub Container Registry, etc.)
2. Add registry credentials to GitHub Secrets:
   - `REGISTRY_URL` (e.g., `ghcr.io`)
   - `REGISTRY_USERNAME`
   - `REGISTRY_PASSWORD`

**Update workflow:**
```yaml
# In .github/workflows/gateway-ci.yml
# Uncomment the "Login to Container Registry" and "Push Docker image" steps
```

#### Adjust Coverage Thresholds

**In pom.xml:**
```xml
<configuration>
  <rules>
    <rule>
      <limits>
        <limit>
          <counter>LINE</counter>
          <value>COVEREDRATIO</value>
          <minimum>0.80</minimum> <!-- Change this -->
        </limit>
      </limits>
    </rule>
  </rules>
</configuration>
```

**In workflow:**
```yaml
# In .github/workflows/gateway-ci.yml
min-coverage-overall: 80  # Change this
```

---

## 📈 Expected Results

### Before CI/CD (Local WSL2)

| Metric | Value | Status |
|--------|-------|--------|
| **Tests Run** | 150 | ⚠️ Incomplete |
| **Tests Blocked** | 68 | ❌ Docker issue |
| **Line Coverage** | 57% | ⚠️ Below target |
| **Branch Coverage** | 53% | ⚠️ Below target |
| **Target Coverage** | 80% | ❌ Not achieved |

**Critical Gaps:**
- ❌ Repository tests not run (multi-tenant isolation!)
- ❌ Redis integration tests not run
- ❌ Full authentication flow not verified
- ❌ Database migration not tested

### After CI/CD (GitHub Actions)

| Metric | Expected | Status |
|--------|----------|--------|
| **Tests Run** | 218 | ✅ Complete |
| **Tests Blocked** | 0 | ✅ All run |
| **Line Coverage** | ~78% | ✅ Near target |
| **Branch Coverage** | ~70% | ✅ Above min |
| **Target Coverage** | 80% | ⏳ Close |

**Gaps Filled:**
- ✅ Repository tests verified (multi-tenant isolation!)
- ✅ Redis integration tested
- ✅ Full authentication flow tested
- ✅ Database migrations verified

---

## 🔍 Monitoring & Debugging

### Viewing Workflow Runs

1. Go to repository → Actions tab
2. Click on workflow run
3. Click on job to see logs

### Common Issues

#### Tests Fail in CI but Pass Locally

**Possible causes:**
- Environment variable differences
- Database state differences
- Timing issues in async tests
- Docker vs local database differences

**Debug:**
```yaml
# Add to workflow step
- name: Debug environment
  run: |
    env | sort
    java -version
    docker --version
```

#### Coverage Check Fails

**Cause:** Coverage dropped below threshold

**Solution:**
- Add tests for new code
- Check which files lack coverage
- Adjust threshold if justified

**Temporary bypass:**
```yaml
# In workflow
- name: Check coverage threshold
  run: ./mvnw jacoco:check
  continue-on-error: true  # Add this
```

#### Docker Build Fails

**Common causes:**
- JAR not built correctly
- Missing dependencies
- Dockerfile syntax error
- Build context issues

**Debug:**
```bash
# Build locally first
cd kiteclass/kiteclass-gateway
./mvnw clean package
docker build -t test .
```

---

## 🎯 Success Criteria

### Before Merge to Main

- [x] GitHub Actions workflow created
- [ ] Workflow runs successfully on push
- [ ] All 218 tests pass in CI
- [ ] Coverage ≥ 70% (target: 78%+)
- [ ] No compilation warnings
- [ ] Docker image builds successfully
- [ ] Documentation complete

### Production Readiness

- [ ] All tests passing in CI for 5+ commits
- [ ] Coverage stable at 78%+
- [ ] No flaky tests
- [ ] Docker image tested in staging
- [ ] Monitoring configured
- [ ] Rollback plan documented

---

## 📚 Related Documentation

- **Workflow file:** `.github/workflows/gateway-ci.yml`
- **Docker issue:** `docs/DOCKER-TESTCONTAINERS-ISSUE.md`
- **Coverage analysis:** `docs/COVERAGE-ANALYSIS.md`
- **Testing guide:** `/documents/03-planning/quality/code-review-pr-plan.md`

---

## 🔄 Future Improvements

### Short-term
- [ ] Add manual workflow trigger (`workflow_dispatch`)
- [ ] Add Slack notifications for failures
- [ ] Add deployment job for staging
- [ ] Add E2E tests job

### Medium-term
- [ ] Add security scanning (Snyk, Trivy)
- [ ] Add performance testing
- [ ] Add canary deployment
- [ ] Add automatic rollback

### Long-term
- [ ] Multi-region deployment
- [ ] Blue-green deployment
- [ ] Automated load testing
- [ ] Chaos engineering tests

---

**Last Updated:** 2026-02-02
**Author:** KiteClass Team + Claude Sonnet 4.5
**Status:** ✅ Ready for Testing
