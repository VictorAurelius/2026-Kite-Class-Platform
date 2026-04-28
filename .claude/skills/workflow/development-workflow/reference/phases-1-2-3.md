# Phases 1-2-3: Planning, Implementation, Testing

> Pointer: read this while coding — covers planning gates, code-style standards, and testing requirements before review. Parent skill: `../SKILL.md`.

## ✅ Phase 1: Planning & Design (Before Coding)

### Database Design Mapping

- [ ] Xác định tables liên quan trong `database-design.md`
- [ ] Review schema và columns cần thiết
- [ ] Xác định indexes cần thiết (performance)
- [ ] Xác định relationships (FK, constraints)
- [ ] Migration script đã được tạo (Flyway)

### API Design Mapping

- [ ] Xác định endpoints cần implement
- [ ] Xác định request/response DTOs
- [ ] Xác định error codes và messages
- [ ] OpenAPI spec đã được update

### UI Design Mapping (Frontend)

- [ ] Xác định components cần tạo
- [ ] Review Figma/wireframe
- [ ] Xác định state management

### Task Breakdown

- [ ] Chia nhỏ feature thành tasks cụ thể
- [ ] Estimate complexity (T-shirt sizing: S/M/L/XL)
- [ ] Identify blockers và dependencies

---

## 💻 Phase 2: Implementation (Coding)

### Backend Coding Standards (Java/Spring Boot)

**Package Structure:**
```
com.kiteclass.{service}
├── common/          (shared code)
├── config/          (Spring configuration)
└── module/
    └── {module}/    (e.g., student)
        ├── controller/
        ├── service/
        ├── repository/
        ├── dto/
        ├── entity/
        └── mapper/
```

**Naming Conventions:**
- Classes: `PascalCase` (e.g., `StudentService`)
- Methods/Variables: `camelCase` (e.g., `getStudentById`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_STUDENTS`)
- DTOs: Use Java Records
- Validation: `@Valid`, `@NotNull`, `@Size`
- Exceptions: Custom exceptions, `@RestControllerAdvice`

### Frontend Coding Standards (TypeScript/React)

**Component Naming:**
- Components: `PascalCase` (e.g., `StudentList`)
- Hooks: `use` prefix (e.g., `useAuth`, `useStudents`)
- Types/Interfaces: `PascalCase` (e.g., `Student`, `ApiResponse`)
- Files: `kebab-case` (e.g., `student-list.tsx`)

**State Management:**
- React Query: Server state
- Zustand: Client state
- Context API: Shared UI state

### Comment Guidelines

**When to Comment:**
- ✅ Complex business logic
- ✅ Algorithm explanation
- ✅ Workaround with reason
- ✅ TODO with ticket number

**When NOT to Comment:**
- ❌ Self-explanatory code
- ❌ Redundant comments
- ❌ Commented-out code (delete it)

**Examples:**

```java
// ✅ GOOD - Complex business logic
// Calculate attendance percentage excluding cancelled sessions
// Formula: (attended / total_active_sessions) * 100
double percentage = calculateAttendancePercentage(student, sessions);

// ❌ BAD - Obvious comment
// Get student by ID
Student student = studentRepository.findById(id);

// ✅ GOOD - Workaround with reason
// WORKAROUND: MapStruct cannot map BaseEntity fields via Builder
// See: https://github.com/mapstruct/mapstruct/issues/XXXX
Student student = mapper.toEntity(request);
student.setId(1L);

// ✅ GOOD - TODO with ticket
// TODO(KC-789): Add email notification when status changes to GRADUATED
```

### Design Patterns to Use

**Backend Patterns:**
- Repository Pattern: Data access abstraction
- DTO Pattern: Separate entity and transfer objects
- Service Layer: Business logic isolation
- Builder Pattern: Complex object creation
- MapStruct: Type-safe bean mapping

**Frontend Patterns:**
- Container/Presentational: Logic vs UI separation
- Custom Hooks: Reusable logic
- Composition: Component composition over inheritance
- Provider Pattern: Context API for shared state

---

## 🧪 Phase 3: Testing

### Unit Tests (REQUIRED)

**Coverage Requirements:**
- Minimum: 80% coverage
- Service layer: 90%+ coverage
- Controllers: Happy path + error cases
- Repositories: Basic CRUD operations

**Testing Patterns:**

```java
// Service Unit Test (Mockito)
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock
    private StudentRepository repository;

    @Mock
    private StudentMapper mapper;

    @InjectMocks
    private StudentServiceImpl service;

    @Test
    void createStudent_shouldCreateSuccessfully() {
        // Given
        CreateStudentRequest request = createDefaultRequest();
        Student entity = createDefaultStudent();
        when(repository.existsByEmailAndDeletedFalse(any())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        // When
        StudentResponse response = service.createStudent(request);

        // Then
        assertNotNull(response);
        verify(repository).save(entity);
    }
}
```

### Integration Tests

**What to Test:**
- API endpoint testing (MockMvc)
- Database interaction (Testcontainers)
- Authentication/Authorization flows
- External service integration

```java
// Integration Test with Testcontainers
@SpringBootTest
@Testcontainers
class StudentRepositoryTest extends IntegrationTestBase {
    @Autowired
    private StudentRepository repository;

    @Test
    void findByEmailAndDeletedFalse_shouldReturnStudent() {
        // Given
        Student student = createAndSaveStudent("test@example.com");

        // When
        Optional<Student> found = repository.findByEmailAndDeletedFalse("test@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }
}
```

### Test Checklist

- [ ] Unit tests cover happy path
- [ ] Unit tests cover edge cases
- [ ] Unit tests cover error scenarios
- [ ] Integration tests for API endpoints
- [ ] Integration tests for database operations
- [ ] All tests pass locally
- [ ] No test warnings or flaky tests
