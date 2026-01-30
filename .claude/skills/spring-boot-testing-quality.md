# Skill: Spring Boot Testing Quality & Best Practices

Hướng dẫn fix warnings, deprecated APIs, và best practices cho Spring Boot 3.4+ testing.

## Mô tả

Tài liệu tổng hợp toàn bộ kinh nghiệm fix code quality warnings trong tests:
- Spring Boot 3.4.0+ deprecations (@MockBean → @TestConfiguration)
- Mockito best practices (explicit imports, argThat, no lenient)
- AssertJ patterns (assertThatCode, null safety)
- Testcontainers fixes (resource leak suppression, reuse)
- Deprecated APIs (Bandwidth.classic, Arrays.asList)
- Complete test templates và quick reference

**Lessons learned from:** Gateway Service & Core Service quality improvements (Jan 2026)

## Trigger phrases

- "fix warnings"
- "fix test warnings"
- "deprecated mockbean"
- "testcontainers resource leak"
- "code quality"
- "spring boot 3.4"
- "mockito best practices"
- "assertj patterns"
- "@MockBean deprecated"
- "unmapped target properties"
- "mapstruct warnings"

---

## 1. Spring Boot Testing

### 1.1 @MockBean Deprecation (Spring Boot 3.4.0+)

**❌ DEPRECATED:**
```java
@MockBean
private AuthService authService;
```

**✅ CORRECT - Use @TestConfiguration:**
```java
@WebFluxTest(AuthController.class)
@Import({AuthControllerTest.MockConfig.class, TestSecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private AuthService authService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(authService);
    }
}
```

**Key Points:**
- Use `@TestConfiguration` with inner static class
- Add `@Primary` to override existing beans
- Remember `Mockito.reset()` in `@BeforeEach`
- Update `@Import` to include MockConfig

**For Spring MVC (non-reactive):**
```java
@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc
@Import({StudentControllerTest.MockConfig.class, TestSecurityConfig.class})
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentService studentService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public StudentService studentService() {
            return Mockito.mock(StudentService.class);
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(studentService);
    }
}
```

---

### 1.2 Integration Test Best Practices

**Template for WebFlux Integration Tests:**
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class IntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", postgres::getJdbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }
}
```

**Template for Spring MVC Integration Tests:**
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class IntegrationTest {

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers framework
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

## 2. Mockito Best Practices

### 2.1 Complete ArgumentMatchers Import List

**✅ ALWAYS import explicitly (NEVER use wildcard):**
```java
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.isNotNull;

// Mockito methods
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
```

**❌ NEVER:**
```java
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
```

---

### 2.2 Custom Argument Matchers with argThat()

**✅ Use argThat() for complex verification:**
```java
verify(repository).save(argThat(user ->
    user.getEmail().equals("test@example.com") &&
    user.getStatus() == UserStatus.ACTIVE &&
    user.getRoles().contains("ADMIN")
));

verify(publisher).publishAll(argThat(events ->
    events.size() == 3 &&
    events.stream().allMatch(e -> e.getType() == EventType.USER_UPDATED)
));
```

**Common Error: argThat() undefined**
```
Fix: Add import static org.mockito.ArgumentMatchers.argThat;
```

---

### 2.3 Lenient Mocking (Deprecated)

**❌ DEPRECATED:**
```java
@Mock(lenient = true)
private EmailProperties emailProperties;
```

**✅ CORRECT - Don't use lenient:**
```java
@Mock
private EmailProperties emailProperties;

// If absolutely needed, use class-level:
@MockitoSettings(strictness = Strictness.LENIENT)
class MyTest {
    // ...
}
```

**Why avoid lenient:**
- Hides unused stubbings (test smells)
- Makes tests less maintainable
- Mockito strict mode catches real issues

---

### 2.4 MockitoAnnotations.openMocks() vs @ExtendWith

**❌ OLD (verbose):**
```java
class GlobalExceptionHandlerTest {
    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ...
    }
}
```

**✅ NEW (cleaner):**
```java
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        // No need for MockitoAnnotations.openMocks()
        // ...
    }
}
```

---

## 3. AssertJ Assertions

### 3.1 Complete Imports

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

// ❌ NEVER use wildcard
import static org.assertj.core.api.Assertions.*;
```

---

### 3.2 Testing No Exception

**✅ Use assertThatCode() (compatible with older AssertJ):**
```java
assertThatCode(() -> someMethod())
    .doesNotThrowAnyException();
```

**⚠️ assertThatNoException() requires AssertJ 3.23.0+:**
```java
// Only if you have AssertJ 3.23.0+
assertThatNoException()
    .isThrownBy(() -> someMethod());
```

**Common Error:**
```
The method assertThatNoException() is undefined
Fix: Use assertThatCode().doesNotThrowAnyException() instead
```

---

### 3.3 Replace Native assert with AssertJ

**❌ BAD - Java assert (disabled by default):**
```java
assert user != null;
assert user.getId() == 1;
```

**✅ GOOD - AssertJ:**
```java
assertThat(user).isNotNull();
assertThat(user.getId()).isEqualTo(1);
```

---

### 3.4 Null Safety Patterns

**✅ ALWAYS check null after .block():**
```java
// ❌ BAD
User user = userRepository.findById(1L).block();
user.getName(); // NPE if null!

// ✅ GOOD - With assertion
User user = userRepository.findById(1L).block();
assertThat(user).isNotNull();
assertThat(user.getName()).isEqualTo("John");

// ✅ BETTER - Use StepVerifier for reactive
StepVerifier.create(userRepository.findById(1L))
    .assertNext(user -> assertThat(user.getName()).isEqualTo("John"))
    .verifyComplete();
```

---

## 4. Testcontainers

### 4.1 Resource Leak Warning

**Problem:** `PostgreSQLContainer` implements `AutoCloseable`, static field triggers warning.

**✅ SOLUTION - @SuppressWarnings("resource"):**
```java
@Container
@SuppressWarnings("resource") // Managed by Testcontainers framework
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);
```

**Why this is correct:**
- Testcontainers manages lifecycle automatically
- Container cleaned up on JVM shutdown
- This is a false positive warning
- Recommended by Testcontainers documentation

**Common Error:**
```
Resource leak: '<unassigned Closeable value>' is never closed
Fix: Add @SuppressWarnings("resource") with explanatory comment
```

---

### 4.2 Container Reuse

**✅ ALWAYS add .withReuse(true):**
```java
.withReuse(true) // Performance optimization
```

**Benefits:**
- Faster test execution
- Reuses container across test runs
- Reduces Docker overhead

---

## 5. Deprecated APIs

### 5.1 Bandwidth.classic() - Bucket4j

**❌ DEPRECATED:**
```java
Bandwidth limit = Bandwidth.classic(
    capacity,
    Refill.intervally(capacity, duration)
);
```

**✅ CORRECT - Use builder:**
```java
Bandwidth limit = Bandwidth.builder()
    .capacity(capacity)
    .refillIntervally(capacity, duration)
    .build();
```

---

### 5.2 Arrays.asList() → List.of()

**❌ OLD (works but not modern):**
```java
List<String> roles = Arrays.asList("OWNER", "ADMIN");
```

**✅ NEW (Java 9+):**
```java
List<String> roles = List.of("OWNER", "ADMIN");
```

**Benefits:**
- More concise
- Immutable by default
- Better performance

---

### 5.3 Wildcard Imports (Lombok)

**❌ BAD:**
```java
import lombok.*;
```

**✅ GOOD:**
```java
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
```

---

## 6. MapStruct Warnings

### 6.1 Unmapped Target Properties

**Problem:** MapStruct warns about fields not mapped.

**Example Error:**
```
Unmapped target properties: "avatarUrl, status"
Unmapped target properties: "id, createdAt, updatedAt, createdBy, updatedBy, deleted, version, avatarUrl"
```

**✅ SOLUTION - Add @Mapping(ignore = true):**

```java
@Mapper(componentModel = "spring")
public interface StudentMapper {

    StudentResponse toResponse(Student student);

    // Fix 1: Ignore fields not in CreateStudentRequest
    @Mapping(target = "avatarUrl", ignore = true)  // Not set during creation
    @Mapping(target = "status", ignore = true)     // Uses @Builder.Default
    Student toEntity(CreateStudentRequest request);

    // Fix 2: Ignore BaseEntity audit fields
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)  // Updated via separate endpoint
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Student student, UpdateStudentRequest request);
}
```

**Key Points:**
- Always add comments explaining why fields are ignored
- Common ignored fields: audit fields (id, createdAt, updatedAt, etc.)
- Fields set elsewhere: avatarUrl, status with defaults

---

## 7. Code Quality Checklist

### 7.1 Before Committing Test Code

- [ ] No wildcard imports (`import static org.mockito.Mockito.*;`)
- [ ] All ArgumentMatchers explicitly imported
- [ ] AssertJ assertions instead of `assert` keyword
- [ ] @MockBean replaced with @TestConfiguration (Spring Boot 3.4.0+)
- [ ] Mockito.reset() in @BeforeEach when using @TestConfiguration
- [ ] @SuppressWarnings("resource") on Testcontainers
- [ ] .withReuse(true) on all containers
- [ ] No deprecated APIs without good reason
- [ ] Use List.of() instead of Arrays.asList()
- [ ] Use assertThatCode() instead of assertThatNoException()
- [ ] MapStruct unmapped properties handled with @Mapping(ignore = true)

---

### 7.2 Common Warning Fixes

| Warning | Fix |
|---------|-----|
| Resource leak (Testcontainers) | `@SuppressWarnings("resource")` |
| @MockBean deprecated | Migrate to @TestConfiguration |
| Unused ObjectMapper | Remove if truly unused |
| Deprecated Bandwidth.classic() | Use Bandwidth.builder() |
| Arrays.asList() | Replace with List.of() |
| argThat() undefined | Add import: ArgumentMatchers.argThat |
| anyList() undefined | Add import: ArgumentMatchers.anyList |
| anyInt() undefined | Add import: ArgumentMatchers.anyInt |
| assertThatNoException() undefined | Use assertThatCode().doesNotThrowAnyException() |
| MockitoAnnotations.openMocks() | Use @ExtendWith(MockitoExtension.class) |
| Wildcard import lombok.* | Use explicit imports |
| MapStruct unmapped properties | Add @Mapping(target = "...", ignore = true) |

---

## 8. Standard Test File Templates

### 8.1 Controller Unit Test (WebFluxTest - Reactive)

```java
package com.kiteclass.gateway.controller;

import com.kiteclass.gateway.service.YourService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(YourController.class)
@Import({YourControllerTest.MockConfig.class, TestSecurityConfig.class})
@DisplayName("YourController Tests")
class YourControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private YourService yourService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public YourService yourService() {
            return Mockito.mock(YourService.class);
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(yourService);
    }

    @Test
    @DisplayName("Test description")
    void testMethod() {
        // Given
        when(yourService.method(any())).thenReturn(Mono.just(result));

        // When/Then
        webTestClient.get()
                .uri("/api/endpoint")
                .exchange()
                .expectStatus().isOk();
    }
}
```

---

### 8.2 Controller Unit Test (WebMvcTest - Spring MVC)

```java
package com.kiteclass.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc
@Import({StudentControllerTest.TestSecurityConfig.class, StudentControllerTest.MockConfig.class})
class StudentControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public StudentService studentService() {
            return Mockito.mock(StudentService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentService studentService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(studentService);
    }

    @Test
    void createStudent_shouldReturn201() throws Exception {
        // Given
        when(studentService.createStudent(any())).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

---

### 8.3 Service Unit Test

```java
package com.kiteclass.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("YourService Tests")
class YourServiceTest {

    @Mock
    private YourRepository repository;

    @InjectMocks
    private YourServiceImpl service;

    @Test
    @DisplayName("Test description")
    void testMethod() {
        // Given
        when(repository.findById(anyLong())).thenReturn(Mono.just(entity));

        // When
        Mono<Entity> result = service.findById(1L);

        // Then
        StepVerifier.create(result)
                .assertNext(e -> assertThat(e.getId()).isEqualTo(1L))
                .verifyComplete();

        verify(repository).findById(1L);
    }
}
```

---

### 8.4 Integration Test (Testcontainers - Reactive)

```java
package com.kiteclass.gateway.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Integration Tests")
class IntegrationTest {

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers framework
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", postgres::getJdbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Test description")
    void testMethod() {
        webTestClient.get()
                .uri("/api/endpoint")
                .exchange()
                .expectStatus().isOk();
    }
}
```

---

### 8.5 Integration Test (Testcontainers - Spring MVC/JPA)

```java
package com.kiteclass.core.testutil;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers framework
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

        // Disable Redis and RabbitMQ for integration tests
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "63790");
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> "56720");
        registry.add("spring.cache.type", () -> "simple");
    }
}
```

---

## 9. Quick Reference

### 9.1 Import Cheat Sheet

```java
// === JUnit 5 ===
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

// === Mockito ===
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
// NEVER: import static org.mockito.ArgumentMatchers.*;
// NEVER: import static org.mockito.Mockito.*;

// === AssertJ ===
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// === Spring Test ===
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;

// === Reactor Test ===
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

// === Testcontainers ===
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// === MapStruct ===
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.BeanMapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
```

---

## 10. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-28 | Initial guide from Gateway & Core Service quality improvements |

---

## 11. References

- Spring Boot Testing Documentation: https://docs.spring.io/spring-boot/reference/testing/index.html
- Mockito Documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- AssertJ Documentation: https://assertj.github.io/doc/
- Testcontainers Documentation: https://testcontainers.com/
- MapStruct Documentation: https://mapstruct.org/
- Gateway Service PRs: 7db6961 → 5b9fa9f
- Core Service PRs: 55eeba5, 227ae74

---

**Maintained by:** KiteClass Team
**Questions?** Refer to this skill first, then ask the team.
