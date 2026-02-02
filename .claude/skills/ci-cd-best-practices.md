# Skill: CI/CD Best Practices

**Version:** 1.0
**Last Updated:** 2026-02-02
**Purpose:** Ensure all PRs and commits maintain CI/CD pipeline health

---

## 📋 Overview

This skill ensures that code changes don't break the CI/CD pipeline and maintains high code quality standards.

---

## 🎯 When to Use This Skill

**Use BEFORE:**
- Creating a Pull Request
- Merging to main/develop
- Adding new tests
- Modifying build configuration

**Use DURING:**
- Fixing CI/CD pipeline failures
- Investigating test failures
- Optimizing build performance

---

## ✅ Pre-Commit Checklist

### 1. Local Test Verification

```bash
# Run ALL tests locally
cd kiteclass/kiteclass-gateway
./mvnw clean test

# Expected: All tests pass (or skip known failing tests)
```

**If tests fail:**
- ❌ DO NOT commit if NEW tests fail
- ✅ OK to commit if EXISTING tests fail with @Disabled
- 📝 Document reason in @Disabled annotation

---

### 2. Build Verification

```bash
# Verify clean build
./mvnw clean package

# Expected: BUILD SUCCESS
```

**If build fails:**
- Check for compilation errors
- Check for missing dependencies
- Check Maven plugin configurations

---

### 3. Coverage Check (Optional but Recommended)

```bash
# Generate coverage report
./mvnw clean test jacoco:report

# View report
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
```

**Coverage Targets:**
- Line coverage: ≥70% (minimum)
- Branch coverage: ≥65% (minimum)
- Target: 80%+

---

## 🔧 CI/CD Pipeline Health

### Pipeline Structure

```
┌─────────────────────────────────────────────────┐
│            GitHub Actions Pipeline               │
├─────────────────────────────────────────────────┤
│                                                  │
│  Test Job (CRITICAL - Must Pass)                │
│  ├── Setup services (PostgreSQL, Redis)         │
│  ├── Run tests                                   │
│  ├── Generate coverage                           │
│  └── Upload artifacts                            │
│                                                  │
│  Quality Job (INFORMATIONAL - Can Fail)          │
│  ├── Checkstyle (continue-on-error)             │
│  ├── Compilation warnings                        │
│  └── Dependency analysis (continue-on-error)    │
│                                                  │
│  Build Job (CRITICAL - Must Pass on main)        │
│  ├── Build JAR                                   │
│  └── Build Docker image                          │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Job Failure Handling

#### Test Job Fails ❌ BLOCK MERGE

**Common causes:**
1. **Tests fail in CI but pass locally**
   - Different environment (CI uses native Docker, local uses Docker Desktop)
   - Different database state
   - Timing issues in async tests

   **Solution:**
   - Check CI logs for specific failure
   - If flaky: Add @Disabled with TODO
   - If genuine bug: Fix before merging

2. **Docker service not starting**
   - PostgreSQL/Redis health check fails

   **Solution:**
   - Usually transient, retry workflow
   - If persistent: Check service configuration in workflow

3. **Coverage threshold not met**
   - JaCoCo check fails

   **Solution:**
   - Add more tests
   - Or adjust threshold in pom.xml (with justification)

#### Quality Job Fails ⚠️ REVIEW BUT DON'T BLOCK

**Common causes:**
1. **Checkstyle violations**
   - Code style doesn't match rules

   **Solution:**
   - Fix style issues
   - Or adjust checkstyle.xml
   - Job has continue-on-error, won't block

2. **Compilation warnings**
   - Deprecation warnings
   - Unchecked type conversions

   **Solution:**
   - Fix warnings
   - Or document why safe to ignore

#### Build Job Fails ❌ BLOCK MERGE

**Common causes:**
1. **JAR build fails**
   - Compilation error
   - Missing resources

   **Solution:**
   - Fix compilation issues
   - Ensure all resources in correct locations

2. **Docker build fails**
   - Dockerfile syntax error
   - Missing files in build context

   **Solution:**
   - Test Docker build locally
   - Check Dockerfile and .dockerignore

---

## 🚨 Handling Failing Tests

### Strategy 1: Fix Immediately (PREFERRED)

**When to use:**
- Bug in implementation
- Incorrect test setup
- Missing configuration

**Steps:**
```bash
1. Reproduce failure locally
2. Debug and fix issue
3. Verify fix: ./mvnw test
4. Commit fix with descriptive message
5. Push and verify CI passes
```

### Strategy 2: Disable with TODO (TEMPORARY)

**When to use:**
- Flaky test
- Known issue with workaround
- Blocking urgent fix/feature
- Test depends on external service

**Steps:**
```java
@Test
@Disabled("TODO: Fix flaky test - fails intermittently in CI (Issue #123)")
void flakyTest() {
    // Test code
}
```

**OR disable entire class:**
```java
@Disabled("TODO: Fix security config for public endpoints - expects 429 but gets 401")
class RateLimitSecurityTest {
    // Tests
}
```

**Requirements:**
- ✅ MUST include TODO with reason
- ✅ MUST include ticket number if available
- ✅ MUST plan to re-enable later
- ❌ DON'T disable without explanation
- ❌ DON'T leave disabled indefinitely

### Strategy 3: Skip in CI Only (RARE)

**When to use:**
- Test requires specific environment not available in CI
- Test is too slow for CI (>5 minutes)

**Steps:**
```java
@Test
@EnabledIfSystemProperty(named = "run.slow.tests", matches = "true")
void slowIntegrationTest() {
    // Test code
}
```

---

## 📊 Coverage Guidelines

### What to Test

**High Priority (Must have 80%+ coverage):**
- ✅ Business logic (Services)
- ✅ API endpoints (Controllers)
- ✅ Data access (Repositories)
- ✅ Security (Auth, Authorization)
- ✅ Error handling

**Medium Priority (Target 70%+ coverage):**
- ✅ DTOs with validation
- ✅ Mappers
- ✅ Filters
- ✅ Utilities

**Low Priority (Optional coverage):**
- ⚠️ Configuration classes
- ⚠️ Simple POJOs
- ⚠️ Main application class

### What NOT to Test

**Excluded from coverage:**
- ❌ Generated code (MapStruct implementations)
- ❌ Lombok-generated code
- ❌ Simple getters/setters
- ❌ Configuration beans

**Configured in pom.xml:**
```xml
<configuration>
  <excludes>
    <exclude>**/generated/**</exclude>
    <exclude>**/*MapperImpl.class</exclude>
    <exclude>**/dto/**</exclude>
    <exclude>**/entity/**</exclude>
    <exclude>**/config/**</exclude>
  </excludes>
</configuration>
```

---

## 🔍 Debugging CI Failures

### Step 1: Check Workflow Run

```
1. Go to: https://github.com/{repo}/actions
2. Click on failed workflow run
3. Check which job(s) failed
4. Click on failed job → View logs
```

### Step 2: Identify Failure Type

**Test Failure:**
```
Look for: "Tests run: X, Failures: Y, Errors: Z"
```

**Build Failure:**
```
Look for: "[ERROR] Failed to execute goal"
```

**Permission Failure:**
```
Look for: "HttpError: Resource not accessible"
```

### Step 3: Reproduce Locally

```bash
# For test failures
./mvnw test -Dtest=FailingTestClass

# For build failures
./mvnw clean package

# For Docker issues
docker-compose up -d
./mvnw test
docker-compose down
```

### Step 4: Fix and Verify

```bash
# After fix
git add .
git commit -m "fix(test): resolve flaky test issue"
git push

# Monitor: GitHub Actions will run automatically
```

---

## 📝 Commit Message Best Practices

### For Test Fixes

```bash
# Good examples
fix(test): resolve RateLimitSecurityTest authentication issue
test: disable AccountLockoutTest until referenceId setup fixed
refactor(test): improve test data setup for integration tests

# Bad examples
fix tests
update
WIP
```

### For CI/CD Fixes

```bash
# Good examples
fix(ci): upgrade actions/upload-artifact to v4
fix(ci): add permissions for test reporter
ci: make quality checks non-blocking

# Bad examples
fix ci
update workflow
fix
```

---

## 🎯 Success Criteria

### Before Merging PR

**Required (Must Pass):**
- [x] All tests pass in CI (or failing tests @Disabled with TODO)
- [x] Test job completes successfully
- [x] Build job completes successfully (on main/develop)
- [x] No new compilation errors
- [x] Coverage doesn't decrease significantly (>5%)

**Recommended (Should Pass):**
- [ ] Quality job passes (or issues documented)
- [ ] No new deprecation warnings
- [ ] Code follows style guidelines
- [ ] PR reviewed by team member

**Optional (Nice to Have):**
- [ ] Coverage increased
- [ ] Test reporter creates check runs
- [ ] PR comment shows coverage diff

---

## 🔄 Common Issues and Solutions

### Issue 1: "mvnw: Permission denied"

**Error:**
```
./mvnw: Permission denied
Exit code 126
```

**Solution:**
```yaml
# In workflow, add before running mvnw
- name: Make mvnw executable
  run: chmod +x mvnw
```

### Issue 2: "No plugin found for prefix 'lint'"

**Error:**
```
Error: No plugin found for prefix 'lint'
```

**Solution:**
```bash
# Wrong: Maven interprets -Xlint as plugin
./mvnw compile -Xlint:deprecation

# Right: Use plain compile
./mvnw compile

# Or configure in pom.xml
<compilerArgs>
  <arg>-Xlint:deprecation</arg>
</compilerArgs>
```

### Issue 3: "Resource not accessible by integration"

**Error:**
```
HttpError: Resource not accessible by integration
```

**Solution:**
```yaml
# Add to workflow root
permissions:
  contents: read
  checks: write
  pull-requests: write
```

### Issue 4: Flaky Tests

**Symptoms:**
- Test passes locally but fails in CI
- Test fails intermittently

**Solutions:**
1. **Add explicit waits:**
```java
// Bad
result = service.asyncOperation().block();

// Good
result = service.asyncOperation()
    .timeout(Duration.ofSeconds(5))
    .block();
```

2. **Disable temporarily:**
```java
@Disabled("TODO: Fix flaky async test (Issue #123)")
@Test
void flakyAsyncTest() { }
```

3. **Make deterministic:**
```java
// Bad: Depends on current time
if (now > deadline) { }

// Good: Inject time provider
if (timeProvider.now() > deadline) { }
```

---

## 📚 Related Documentation

- **Workflow file:** `.github/workflows/gateway-ci.yml`
- **CI/CD setup:** `kiteclass-gateway/docs/CI-CD-SETUP.md`
- **Development workflow:** `.claude/skills/development-workflow.md`
- **Testing guide:** `documents/03-planning/quality/code-review-pr-plan.md`

---

## 🎓 Learning Resources

### GitHub Actions
- Official docs: https://docs.github.com/en/actions
- Permissions: https://docs.github.com/en/actions/security-guides/automatic-token-authentication

### JaCoCo
- Maven plugin: https://www.jacoco.org/jacoco/trunk/doc/maven.html
- Coverage goals: https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html

### Testcontainers
- Java docs: https://java.testcontainers.org/
- Troubleshooting: https://java.testcontainers.org/on_failure.html

---

**Last Updated:** 2026-02-02
**Author:** KiteClass Team + Claude Sonnet 4.5
**Status:** ✅ Active
