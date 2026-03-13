# Skill: Fix IDE Testcontainers Resource Leak Warnings

**Version:** 1.0
**Last Updated:** 2026-03-13
**Purpose:** Fix IDE warning code 1102 "compiler option being ignored" for Testcontainers

---

## 📋 Problem

VSCode Java extension (Eclipse JDT compiler) reports warning code 1102 on Testcontainers fields:

```
At least one of the problems in category 'resource' is not analysed due to a compiler option being ignored
```

**Root Cause:** IDE compiler doesn't recognize that Testcontainers framework manages container lifecycle.

---

## ✅ Solution: Use @Container Annotation

Replace `@SuppressWarnings("resource")` with `@Container` annotation to signal proper lifecycle management.

### Pattern 1: Static Fields with @Container

**For test configuration classes or base test classes:**

```java
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers  // Required for @Container to work
@TestConfiguration
public class TestContainersConfiguration {

    @Container  // ✅ Replaces @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static final GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
}
```

**Before (IDE warning):**
```java
@SuppressWarnings("resource") // ⚠️ IDE still warns
private static final PostgreSQLContainer<?> postgres = ...;
```

**After (No warning):**
```java
@Container  // ✅ IDE understands lifecycle
static final PostgreSQLContainer<?> postgres = ...;
```

### Pattern 2: Test Class with @Testcontainers

**For JUnit 5 test classes:**

```java
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers  // Required class annotation
class MyIntegrationTest {

    @Container  // ✅ No @SuppressWarnings needed
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void testSomething() {
        // Test uses postgres container
    }
}
```

---

## 🚨 When @Container Doesn't Work

### Case 1: @Bean Methods in @TestConfiguration

**Problem:** Cannot annotate @Bean method returns with @Container

```java
@TestConfiguration
public class TestContainersConfiguration {

    @Bean
    @SuppressWarnings("resource")  // ⚠️ Cannot use @Container here
    PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15");
        container.start();
        return container;
    }
}
```

**Solution:** Refactor to static field + @Container:

```java
@TestConfiguration
@Testcontainers
public class TestContainersConfiguration {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15");

    static {
        postgres.start();
    }

    @Bean
    PostgreSQLContainer<?> postgresContainer() {
        return postgres;  // ✅ Return static field
    }
}
```

### Case 2: Non-Testcontainers Resources

**For resources not from Testcontainers (e.g., RedisServer, HikariCP):**

```java
@SuppressWarnings("resource")  // ✅ Keep this - not a Testcontainer
private RedisServer redisServer;

@PreDestroy
void cleanup() {
    redisServer.stop();
}
```

**Why:** `@Container` only works with Testcontainers types. Other resources need `@SuppressWarnings` + proper cleanup.

---

## 📝 Eclipse JDT Settings (Complementary)

Create `.settings/org.eclipse.jdt.core.prefs` for project-wide compiler settings:

```properties
eclipse.preferences.version=1
org.eclipse.jdt.core.compiler.codegen.targetPlatform=17
org.eclipse.jdt.core.compiler.compliance=17
org.eclipse.jdt.core.compiler.source=17

# Resource leak detection - ignore for Testcontainers
org.eclipse.jdt.core.compiler.problem.potentiallyUnclosedCloseable=ignore
org.eclipse.jdt.core.compiler.problem.unclosedCloseable=ignore
org.eclipse.jdt.core.compiler.problem.explicitlyClosedAutoCloseable=ignore

# Annotation processing
org.eclipse.jdt.core.compiler.processAnnotations=enabled
```

**Note:** `.settings/` approach is fallback. `@Container` annotation is preferred.

---

## 🔧 Implementation Checklist

- [ ] Add `@Container` annotation to static Testcontainers fields
- [ ] Add `@Testcontainers` class annotation if missing
- [ ] Remove `@SuppressWarnings("resource")` from container fields
- [ ] Keep `@SuppressWarnings` for non-Testcontainers resources
- [ ] Create `.settings/org.eclipse.jdt.core.prefs` for each module
- [ ] Reload VSCode window (`F1` → "Reload Window")
- [ ] Verify warnings cleared in IDE

---

## 📚 Examples

### ✅ Core TestContainersConfiguration

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    @Container
    private static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    @Container
    private static final GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    private static final MinIOContainer minio =
        new MinIOContainer(DockerImageName.parse("minio/minio:latest"));

    static {
        postgres.start();
        redis.start();
        minio.start();
    }

    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return postgres;
    }
}
```

### ✅ IntegrationTestBase

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    protected static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("kiteclass_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
}
```

---

## 🔧 Maven m2e Lifecycle Mapping Warning

**Problem:** IDE shows warning on checkstyle plugin execution:
```
Plugin execution not covered by lifecycle configuration:
org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check
```

**Solution:** Add m2e lifecycle-mapping to `pom.xml`:

```xml
<build>
    <pluginManagement>
        <plugins>
            <!-- Suppress m2e warning for checkstyle -->
            <plugin>
                <groupId>org.eclipse.m2e</groupId>
                <artifactId>lifecycle-mapping</artifactId>
                <version>1.0.0</version>
                <configuration>
                    <lifecycleMappingMetadata>
                        <pluginExecutions>
                            <pluginExecution>
                                <pluginExecutionFilter>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-checkstyle-plugin</artifactId>
                                    <versionRange>[3.0,)</versionRange>
                                    <goals>
                                        <goal>check</goal>
                                    </goals>
                                </pluginExecutionFilter>
                                <action>
                                    <ignore/>
                                </action>
                            </pluginExecution>
                        </pluginExecutions>
                    </lifecycleMappingMetadata>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
    <plugins>
        <!-- Existing plugins here -->
    </plugins>
</build>
```

**Note:** This only affects IDE behavior, not actual `mvn build` execution.

---

## ⚠️ Common Issues

### Issue 1: "Cannot resolve @Container"

**Fix:** Add Testcontainers JUnit 5 dependency:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Issue 2: Container not starting

**Cause:** Missing `@Testcontainers` class annotation

**Fix:** Add `@Testcontainers` to test class

### Issue 3: Warnings persist after fix

**Fix:**
1. Clean Java workspace: `Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace"
2. Reload VSCode window
3. Check `.settings/org.eclipse.jdt.core.prefs` exists

---

## 📊 Benefits

✅ **Proper lifecycle signaling** - IDE understands Testcontainers manages cleanup
✅ **No false positives** - Warnings only for real resource leaks
✅ **Cleaner code** - No `@SuppressWarnings` clutter
✅ **Framework standard** - Follows Testcontainers best practices

---

**Last Updated:** 2026-03-13
**Author:** KiteClass Team
**Status:** ✅ Active
**Related:** `code-style.md`, `testing-best-practices.md`
