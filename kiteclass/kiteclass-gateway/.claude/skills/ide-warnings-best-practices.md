# IDE Warnings Best Practices

Guidelines for handling IDE warnings in Java/Spring Boot projects to keep codebase clean.

## Common IDE Warnings & Solutions

### 1. Unknown Spring Boot Properties

**Warning:** "Unknown property 'custom.property.name'"

**Solution:** Create Spring configuration metadata

```bash
# Location: src/main/resources/META-INF/additional-spring-configuration-metadata.json
{
  "properties": [
    {
      "name": "custom.property.name",
      "type": "java.lang.String",
      "description": "Description of the property"
    }
  ]
}
```

**Benefits:**
- Eliminates IDE warnings
- Provides autocomplete in application.yml/properties
- Documents custom properties

### 2. Resource Leak Warnings

**Warning:** "Resource leak: '<unassigned Closeable value>' is never closed"

**Solutions:**

#### Option A: Field-level Suppression (Preferred for managed resources)
```java
@SuppressWarnings("resource") // Closed in @PreDestroy cleanup()
private RedisServer redisServer;
```

#### Option B: Method-level Suppression
```java
@Bean
@SuppressWarnings("resource") // Managed by Spring lifecycle
public DataSource dataSource() {
    return new HikariDataSource(config);
}
```

#### Option C: Try-with-resources (For local resources)
```java
try (InputStream is = new FileInputStream(file)) {
    // Use resource
}
```

**When to suppress:**
- Resources closed in @PreDestroy/@AfterEach
- Spring-managed beans (@Bean, @Component)
- Resources managed by framework (Testcontainers, etc.)

### 3. Unused Fields/Imports

**Warning:** "The value of field X is not used"

**Solution:** Remove unused code
```java
// BAD
@Autowired
private PasswordEncoder passwordEncoder; // Unused!

// GOOD - Remove it
// (Field removed)
```

**Best Practice:**
- Remove unused imports and fields immediately
- Don't suppress these warnings - fix them
- Use IDE's "Optimize Imports" feature regularly

### 4. Unsupported Configuration Properties

**Warning:** "Unknown property 'resilience4j.retry.enabled'"

**Solution:** Remove properties not supported by the library version
```yaml
# BAD - Property not recognized
resilience4j:
  retry:
    enabled: false

# GOOD - Remove unsupported properties
# (Section removed or use supported properties only)
```

**Best Practice:**
- Check library documentation for supported properties
- Remove deprecated/unsupported properties
- Use IDE validation to catch these early

### 6. Outdated Dependency Versions

**Warning:** "Newer minor version of Spring Boot available: 3.5.10"

**Solution:** Evaluate carefully - don't blindly upgrade

#### Option A: Stay on Current Version (Recommended if working)
```xml
<!-- KEEP current version if tests pass and OSS support not ended -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.1</version> <!-- Still supported until 2026-12-31 -->
</parent>
```

**Why stay:**
- Current version works perfectly (all tests pass)
- Still has OSS support
- Upgrading may introduce breaking changes
- Warning is informational, not critical

#### Option B: Upgrade (Only if necessary)
```xml
<!-- UPGRADE only if needed for security patches or new features -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.10</version>
</parent>
```

**CRITICAL - Before upgrading:**
1. **Read release notes** - Check for breaking changes
2. **Test on separate branch** - Never upgrade directly on main
3. **Run full test suite** - Ensure 100% pass rate
4. **Check dependencies** - Verify compatibility
5. **Have rollback plan** - Be ready to revert

**Real Example:**
- Spring Boot 3.4.1 → 3.5.10: Broke 56 tests (ApplicationContext failures)
- Solution: Reverted to 3.4.1 (still supported, all tests pass)

**Best Practice:**
- **Stability > Latest version** - If it works, don't break it
- Upgrade only when:
  - Security vulnerabilities in current version
  - OSS support ending soon (< 3 months)
  - New features required
- **Never upgrade** just to silence IDE warning
- Accept version warnings if current version is stable and supported

**Suppress Version Warning in IDE:**

If the warning bothers you but version is stable:

1. **IntelliJ IDEA:**
   - Settings → Editor → Inspections → Maven → "Newer version available"
   - Uncheck or set to "Weak Warning"

2. **VS Code:**
   - Add to `.vscode/settings.json`:
   ```json
   {
     "maven.showUpdateNotification": false
   }
   ```

3. **Accept it:**
   - Version warnings are informational
   - Safe to ignore if version has active support
   - Focus on actual errors, not warnings

### 5. Special Characters in YAML Keys

**Warning:** "This key contains special characters. Escape with '[]'"

**Solution:** Escape keys containing dots (.) with square brackets

```yaml
# BAD - Dots in keys trigger warning
logging:
  level:
    com.kiteclass: DEBUG
    org.springframework.mail: DEBUG

# GOOD - Escape with brackets
logging:
  level:
    '[com.kiteclass]': DEBUG
    '[org.springframework.mail]': DEBUG
```

**When to escape:**
- Keys with dots: `com.example`, `org.springframework`
- Keys with spaces: `my key`, `some value`
- Keys with special characters: `my-key`, `key@name`

**When NOT to escape:**
```yaml
# This is CORRECT - no dots in the keys themselves
spring:
  mail:
    properties:
      mail:
        smtp:
          connectiontimeout: 1000
```

**Rule of thumb:** If the IDE warns about special characters, escape with `[key]`

## Suppression Best Practices

### ✅ Good Suppressions

1. **Framework-managed resources:**
```java
@Bean
@SuppressWarnings("resource") // Closed by Spring on shutdown
public RedisServer redisServer() { ... }
```

2. **Test cleanup in @PreDestroy/@AfterEach:**
```java
@SuppressWarnings("resource") // Cleaned up in tearDown()
private TestResource resource;
```

3. **Intentional design:**
```java
@SuppressWarnings("unchecked") // Safe: verified type at runtime
List<String> list = (List<String>) obj;
```

### ❌ Bad Suppressions

1. **Actual resource leaks:**
```java
// BAD - Don't suppress real leaks!
@SuppressWarnings("resource")
public void badMethod() {
    new FileInputStream("file.txt"); // Actually leaked!
}
```

2. **Lazy coding:**
```java
// BAD - Fix the actual issue!
@SuppressWarnings("all") // Never use "all"
public void messyMethod() { ... }
```

## IDE-Specific Configuration

### IntelliJ IDEA

Suppress inspection for entire class:
```java
@SuppressWarnings("resource")
public class TestConfiguration { ... }
```

Suppress inspection for file:
```java
//noinspection resource
private RedisServer server;
```

### VS Code / Eclipse

Use standard Java annotations:
```java
@SuppressWarnings({"resource", "unused"})
```

## Verification Commands

### Check for real Maven warnings:
```bash
./mvnw clean compile 2>&1 | grep -i warning
```

### Check for real test warnings:
```bash
./mvnw clean test 2>&1 | grep -i warning
```

### IDE warnings vs. Real warnings:
- **IDE warnings:** Show in editor, may be overly cautious
- **Maven warnings:** Show in build output, always investigate
- **Priority:** Fix Maven warnings first, then IDE warnings for cleanliness

## Summary Checklist

Before committing code:
- [ ] No unused imports/fields
- [ ] All custom properties have metadata in META-INF
- [ ] Resource leaks properly suppressed with comments
- [ ] YAML keys with dots escaped with brackets: `'[com.example]'`
- [ ] Dependencies up-to-date (especially before OSS support ends)
- [ ] Maven build shows 0 warnings
- [ ] IDE shows 0 warnings
- [ ] Suppressions include explanatory comments
- [ ] No @SuppressWarnings("all") used

## References

- [Spring Boot Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)
- [Java @SuppressWarnings](https://docs.oracle.com/javase/8/docs/api/java/lang/SuppressWarnings.html)
- [Try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)
