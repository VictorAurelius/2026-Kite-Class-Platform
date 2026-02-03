# Spring Boot 3.5.10 Upgrade Implementation Plan

## Executive Summary

**Goal**: Upgrade Spring Boot from 3.4.1 to 3.5.10 and fix all 56 test failures

**Critical Finding**: Spring Boot 3.5.10 requires Spring Cloud 2025.0.0+ (current: 2024.0.1)

**Root Causes of 56 Test Failures**:
1. **Testcontainers Configuration Conflicts** - 5 tests using manual `@DynamicPropertySource` pattern conflict with Spring Boot 3.5.x auto-configuration
2. **Security DSL Deprecation** - Method reference style (`::disable`) deprecated in favor of lambda DSL
3. **Mixed Test Configuration Strategies** - Some tests use `TestContainersConfiguration` (works), others use manual setup (broken)

---

## 🚨 Critical Decision Point

**Spring Cloud Compatibility Blocker**: Spring Cloud 2024.0.1 DOES NOT support Spring Boot 3.5.x

You must choose ONE of these options:

### Option A: Upgrade to Spring Boot 3.5.10 + Spring Cloud 2025.0.0 (USER CHOSE THIS)
- **Pros**: Latest features, extended support until 2027-05-31, future-proof
- **Cons**: Higher risk (2 major dependency changes), requires Spring Cloud migration
- **Effort**: ~3-4 hours implementation + testing
- **Risk**: Medium-High (Spring Cloud 2025.0.0 may introduce breaking changes in Gateway/Feign/Circuit Breaker)

### Option B: Stay on Spring Boot 3.4.x (SAFER ALTERNATIVE)
- **Pros**: Low risk, proven compatibility, Spring Boot 3.4.1 supported until 2026-12-31 (11 months)
- **Cons**: No new features from 3.5.x
- **Effort**: 0 hours (current state is stable, all tests pass)
- **Risk**: None

**User Decision**: Proceed with **Option A**

---

## Implementation Plan

### Phase 1: Dependency Upgrades

#### 1.1 Update pom.xml - Spring Boot + Spring Cloud Versions

**File**: `kiteclass/kiteclass-gateway/pom.xml`

Changes:
1. Line 19: `<version>3.4.1</version>` → `<version>3.5.10</version>`
2. Line 30: `<spring-cloud.version>2024.0.1</spring-cloud.version>` → `<spring-cloud.version>2025.0.0</spring-cloud.version>`
3. Lines 11-18: Update comment explaining upgrade

**Verify**:
```bash
cd kiteclass/kiteclass-gateway
mvn clean compile
mvn dependency:tree | grep -E "spring-boot|spring-cloud"
```

Expected: Spring Boot 3.5.10, Spring Cloud 2025.0.0, Spring Security 6.5.x

---

### Phase 2: Security Configuration Updates

#### 2.1 Update SecurityConfig.java - Lambda DSL Migration

**File**: `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/config/SecurityConfig.java`

Change lines 59-61 from method references to lambda DSL:

**Before**:
```java
.csrf(ServerHttpSecurity.CsrfSpec::disable)
.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
```

**After**:
```java
.csrf(csrf -> csrf.disable())
.formLogin(formLogin -> formLogin.disable())
.httpBasic(httpBasic -> httpBasic.disable())
```

#### 2.2 Update TestSecurityConfig.java

**File**: `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/module/user/controller/TestSecurityConfig.java`

Change line 22: `.csrf(ServerHttpSecurity.CsrfSpec::disable)` → `.csrf(csrf -> csrf.disable())`

**Verify**: `mvn compile` should succeed with no deprecation warnings

---

### Phase 3: Testcontainers Test Migration (4 files)

**Pattern**: Replace manual `@Container` + `@DynamicPropertySource` with `@Import(TestContainersConfiguration.class)`

#### 3.1 Migrate AccountLockoutTest.java

**File**: `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/module/auth/AccountLockoutTest.java`

1. Update class declaration (lines 40-44):
   - Remove: `@Testcontainers`, `@Disabled`
   - Change: `@SpringBootTest` → `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
   - Add: `@Import(TestContainersConfiguration.class)`

2. Remove lines 46-62 (entire `@Container` and `@DynamicPropertySource` block)

3. Remove imports: `DynamicPropertyRegistry`, `DynamicPropertySource`, `PostgreSQLContainer`, `Container`, `Testcontainers`

4. Add imports: `TestContainersConfiguration`, `Import`

#### 3.2 Migrate UserSecurityTest.java

**File**: `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/module/user/UserSecurityTest.java`

Apply same changes as 3.1

#### 3.3 Migrate RateLimitSecurityTest.java

**File**: `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/filter/RateLimitSecurityTest.java`

Same as 3.1, plus:
- Add `@TestPropertySource(properties = {"rate-limit.ip.capacity=10", "rate-limit.ip.refill-rate=10"})`
- Remove rate limit properties from `@DynamicPropertySource`

#### 3.4 Migrate PasswordPolicyTest.java

**File**: `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/module/auth/PasswordPolicyTest.java`

Apply same changes as 3.1

**Note**: May still have assertion failures (separate issue - fix test logic later)

#### 3.5 UserRepositoryTest.java - NO CHANGES NEEDED

**Rationale**: Uses `@DataR2dbcTest` (slice test) - manual Testcontainers is correct pattern for slice tests

---

### Phase 4: Verification & Testing

#### 4.1 After Phase 1 (Dependencies):
```bash
mvn clean compile  # Must succeed
```

#### 4.2 After Phase 2 (Security):
```bash
mvn test -Dtest=JwtAuthenticationIntegrationTest,AuthControllerIntegrationTest
```

#### 4.3 After Each Test Migration:
```bash
mvn test -Dtest=AccountLockoutTest  # After 3.1
mvn test -Dtest=UserSecurityTest    # After 3.2
mvn test -Dtest=RateLimitSecurityTest  # After 3.3
mvn test -Dtest=PasswordPolicyTest  # After 3.4
```

#### 4.4 Full Test Suite:
```bash
mvn clean test
```

**Expected**: 150+ passing, ~15 skipped, 0 failures

---

### Phase 5: Rollback Strategy

If tests fail:

**Option 1 - Full Rollback**:
```bash
git checkout kiteclass/kiteclass-gateway/pom.xml
git checkout kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/config/SecurityConfig.java
git checkout kiteclass/kiteclass-gateway/src/test/
mvn clean test
```

**Option 2 - Partial Rollback (Keep Security DSL)**:
```bash
git checkout kiteclass/kiteclass-gateway/pom.xml  # Revert only pom.xml
mvn clean test
```

Lambda DSL changes are backward compatible with Spring Boot 3.4.1

---

## Critical Files to Modify (7 files)

### Configuration Files (2 files):
1. ✅ `kiteclass/kiteclass-gateway/pom.xml`
   - Line 19: Spring Boot 3.4.1 → 3.5.10
   - Line 30: Spring Cloud 2024.0.1 → 2025.0.0
   - Lines 11-18: Update comment

2. ✅ `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/config/SecurityConfig.java`
   - Lines 59-61: Method references → Lambda DSL

### Test Configuration (1 file):
3. ✅ `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/module/user/controller/TestSecurityConfig.java`
   - Line 22: Method reference → Lambda DSL

### Test Files (4 files):
4. ✅ `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/module/auth/AccountLockoutTest.java`
   - Remove `@Testcontainers`, `@Disabled`, `@Container`, `@DynamicPropertySource`
   - Add `@Import(TestContainersConfiguration.class)`

5. ✅ `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/module/user/UserSecurityTest.java`
   - Same changes as #4

6. ✅ `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/filter/RateLimitSecurityTest.java`
   - Same changes as #4 + add `@TestPropertySource` for rate limits

7. ✅ `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/module/auth/PasswordPolicyTest.java`
   - Same changes as #4

**Total**: 7 files to modify

---

## Risk Assessment

| Risk Level | Area | Mitigation |
|------------|------|------------|
| **HIGH** | Spring Cloud 2024.0.1 → 2025.0.0 | Test Gateway, OpenFeign, Circuit Breaker thoroughly |
| **MEDIUM** | Spring Boot 3.4.1 → 3.5.10 | Follow incremental verification steps |
| **LOW** | Security DSL changes | Tested pattern, backward compatible |
| **LOW** | Testcontainers migration | Well-understood pattern, 4 tests only |

---

## Timeline Estimate

| Phase | Duration | Notes |
|-------|----------|-------|
| Phase 0: Research | 30 min | ✅ Completed |
| Phase 1: Dependencies | 15 min | Update pom.xml, verify compilation |
| Phase 2: Security Config | 20 min | Update 2 files, verify syntax |
| Phase 3: Test Migration | 60 min | Migrate 4 test files, update imports |
| Phase 4: Verification | 45 min | Incremental + full test suite |
| Phase 5: Rollback | N/A | Only if tests fail |
| **TOTAL** | **~3 hours** | Assuming no rollback needed |

---

## Success Criteria

✅ All 150 enabled tests pass (0 failures)
✅ Spring Boot version: 3.5.10
✅ Spring Cloud version: 2025.0.0
✅ No deprecated security DSL usage
✅ All `@SpringBootTest` tests use `TestContainersConfiguration`
✅ No manual `@Container` in full integration tests
✅ `mvn clean install` completes successfully
✅ Application starts up without errors

---

## Implementation Notes

### Test Pattern Guidelines
1. **Full Integration Tests** (`@SpringBootTest`): Use `@Import(TestContainersConfiguration.class)`
2. **Repository Slice Tests** (`@DataR2dbcTest`): Use manual `@Container` + `@DynamicPropertySource`
3. **Controller Tests** (`@WebFluxTest`): Use `@Import(TestSecurityConfig.class)`
4. **NEVER** mix both patterns in the same test

### Known Issues to Fix Separately
- PasswordPolicyTest.java: Assertion logic for password validation error messages
- RateLimitSecurityTest.java: Security config for public endpoints (expects 429, gets 401)

---

## References

- [Spring Boot 3.5.10 Release Notes](https://github.com/spring-projects/spring-boot/releases/tag/v3.5.10)
- [Spring Cloud 2025.0.0 Release](https://spring.io/blog/2025/05/29/spring-cloud-2025-0-0-is-available)
- [Spring Security 6.5 Lambda DSL](https://docs.spring.io/spring-security/reference/reactive/configuration/webflux.html)
- [Testcontainers Spring Boot](https://java.testcontainers.org/modules/databases/postgres/)
