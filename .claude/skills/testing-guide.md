# Skill: Testing Guide

Hướng dẫn viết test cho KiteClass Platform - Java/Spring Boot & TypeScript/React.

## Mô tả

Tài liệu hướng dẫn chi tiết về:
- Unit Testing (JUnit 5, Vitest)
- Integration Testing (Spring Boot Test, Playwright)
- Mocking strategies (Mockito, MSW)
- Test fixtures và data builders
- Coverage requirements

## Trigger phrases

- "viết test"
- "unit test"
- "integration test"
- "test coverage"
- "testing guide"

---

## Testing Stack

### Backend (Java/Spring Boot)

| Library | Purpose | Version |
|---------|---------|---------|
| JUnit 5 | Test framework | 5.10+ |
| Mockito | Mocking | 5.x |
| AssertJ | Fluent assertions | 3.24+ |
| Testcontainers | Database testing | 1.19+ |
| Spring Boot Test | Integration testing | 3.2+ |
| JaCoCo | Coverage reporting | 0.8.11+ |

### Frontend (TypeScript/React)

| Library | Purpose |
|---------|---------|
| Vitest | Test runner |
| React Testing Library | Component testing |
| MSW (Mock Service Worker) | API mocking |
| Playwright | E2E testing |

---

## Test File Naming & Structure

### Backend

```
src/
├── main/java/com/kiteclass/core/
│   └── service/
│       └── StudentService.java
└── test/java/com/kiteclass/core/
    ├── service/
    │   ├── StudentServiceTest.java          # Unit tests
    │   └── StudentServiceIntegrationTest.java  # Integration tests
    ├── controller/
    │   └── StudentControllerTest.java       # API tests
    ├── repository/
    │   └── StudentRepositoryTest.java       # Repository tests
    └── testutil/
        ├── TestDataBuilder.java             # Test data factories
        └── IntegrationTestBase.java         # Base class for integration tests
```

### Frontend

```
src/
├── components/
│   └── students/
│       ├── student-form.tsx
│       ├── student-form.test.tsx            # Component test
│       └── __snapshots__/                   # Snapshot tests
├── hooks/
│   ├── use-students.ts
│   └── use-students.test.ts                 # Hook test
├── lib/
│   ├── utils.ts
│   └── utils.test.ts                        # Utility tests
└── __tests__/
    ├── setup.ts                             # Test setup
    └── mocks/
        └── handlers.ts                      # MSW handlers
```

---

## Java Unit Testing

### Service Layer Test

```java
package com.kiteclass.core.service;

import com.kiteclass.core.dto.request.CreateStudentRequest;
import com.kiteclass.core.dto.response.StudentResponse;
import com.kiteclass.core.entity.Student;
import com.kiteclass.core.exception.StudentNotFoundException;
import com.kiteclass.core.mapper.StudentMapper;
import com.kiteclass.core.repository.StudentRepository;
import com.kiteclass.core.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link StudentServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Unit Tests")
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    // Test fixtures
    private Student student;
    private CreateStudentRequest createRequest;
    private StudentResponse studentResponse;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .name("Nguyễn Văn A")
                .email("student@example.com")
                .phone("0901234567")
                .build();

        createRequest = new CreateStudentRequest(
                "Nguyễn Văn A",
                "student@example.com",
                "0901234567",
                null,
                null
        );

        studentResponse = new StudentResponse(
                1L,
                "Nguyễn Văn A",
                "student@example.com",
                "0901234567",
                StudentStatus.ACTIVE
        );
    }

    // ==================== CREATE ====================

    @Nested
    @DisplayName("createStudent")
    class CreateStudent {

        @Test
        @DisplayName("should create student successfully")
        void shouldCreateStudentSuccessfully() {
            // Given
            given(studentMapper.toEntity(createRequest)).willReturn(student);
            given(studentRepository.save(any(Student.class))).willReturn(student);
            given(studentMapper.toResponse(student)).willReturn(studentResponse);

            // When
            StudentResponse result = studentService.createStudent(createRequest);

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(r -> {
                        assertThat(r.id()).isEqualTo(1L);
                        assertThat(r.name()).isEqualTo("Nguyễn Văn A");
                        assertThat(r.email()).isEqualTo("student@example.com");
                    });

            then(studentRepository).should(times(1)).save(any(Student.class));
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            given(studentRepository.existsByEmail(createRequest.email())).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> studentService.createStudent(createRequest))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("Email đã tồn tại");

            then(studentRepository).should(never()).save(any());
        }
    }

    // ==================== GET BY ID ====================

    @Nested
    @DisplayName("getStudentById")
    class GetStudentById {

        @Test
        @DisplayName("should return student when found")
        void shouldReturnStudentWhenFound() {
            // Given
            given(studentRepository.findById(1L)).willReturn(Optional.of(student));
            given(studentMapper.toResponse(student)).willReturn(studentResponse);

            // When
            StudentResponse result = studentService.getStudentById(1L);

            // Then
            assertThat(result)
                    .isNotNull()
                    .extracting(StudentResponse::id, StudentResponse::name)
                    .containsExactly(1L, "Nguyễn Văn A");
        }

        @Test
        @DisplayName("should throw exception when student not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            given(studentRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> studentService.getStudentById(999L))
                    .isInstanceOf(StudentNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ==================== DELETE ====================

    @Nested
    @DisplayName("deleteStudent")
    class DeleteStudent {

        @Test
        @DisplayName("should soft delete student successfully")
        void shouldSoftDeleteSuccessfully() {
            // Given
            given(studentRepository.findById(1L)).willReturn(Optional.of(student));

            // When
            studentService.deleteStudent(1L);

            // Then
            assertThat(student.isDeleted()).isTrue();
            then(studentRepository).should().save(student);
        }

        @Test
        @DisplayName("should throw exception when student has active enrollments")
        void shouldThrowWhenHasActiveEnrollments() {
            // Given
            student.setEnrollments(List.of(new Enrollment()));  // Has enrollments
            given(studentRepository.findById(1L)).willReturn(Optional.of(student));

            // When & Then
            assertThatThrownBy(() -> studentService.deleteStudent(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("không thể xóa");
        }
    }
}
```

### Test Data Builder Pattern

```java
package com.kiteclass.core.testutil;

import com.kiteclass.core.entity.*;
import com.kiteclass.core.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Builder for creating test data with sensible defaults.
 */
public class TestDataBuilder {

    // ==================== STUDENT ====================

    public static StudentBuilder aStudent() {
        return new StudentBuilder();
    }

    public static class StudentBuilder {
        private Long id = 1L;
        private String name = "Test Student";
        private String email = "test@example.com";
        private String phone = "0901234567";
        private StudentStatus status = StudentStatus.ACTIVE;
        private LocalDate dateOfBirth = LocalDate.of(2010, 1, 1);

        public StudentBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public StudentBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public StudentBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public StudentBuilder withStatus(StudentStatus status) {
            this.status = status;
            return this;
        }

        public StudentBuilder inactive() {
            this.status = StudentStatus.INACTIVE;
            return this;
        }

        public Student build() {
            return Student.builder()
                    .id(id)
                    .name(name)
                    .email(email)
                    .phone(phone)
                    .status(status)
                    .dateOfBirth(dateOfBirth)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    // ==================== CLASS ====================

    public static ClassBuilder aClass() {
        return new ClassBuilder();
    }

    public static class ClassBuilder {
        private Long id = 1L;
        private String name = "Test Class";
        private int maxStudents = 25;
        private ClassStatus status = ClassStatus.SCHEDULED;

        public ClassBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public ClassBuilder withMaxStudents(int max) {
            this.maxStudents = max;
            return this;
        }

        public ClassBuilder inProgress() {
            this.status = ClassStatus.IN_PROGRESS;
            return this;
        }

        public ClassEntity build() {
            return ClassEntity.builder()
                    .id(id)
                    .name(name)
                    .maxStudents(maxStudents)
                    .status(status)
                    .build();
        }
    }

    // ==================== INVOICE ====================

    public static InvoiceBuilder anInvoice() {
        return new InvoiceBuilder();
    }

    public static class InvoiceBuilder {
        private Long id = 1L;
        private String invoiceNo = "INV-2025-0001";
        private BigDecimal amount = new BigDecimal("2500000");
        private InvoiceStatus status = InvoiceStatus.DRAFT;

        public InvoiceBuilder paid() {
            this.status = InvoiceStatus.PAID;
            return this;
        }

        public InvoiceBuilder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Invoice build() {
            return Invoice.builder()
                    .id(id)
                    .invoiceNo(invoiceNo)
                    .totalAmount(amount)
                    .status(status)
                    .build();
        }
    }
}
```

### Using Test Data Builder

```java
@Test
void shouldCalculateCorrectBalance() {
    // Using builder - clean and readable
    Student student = aStudent()
            .withName("Nguyễn Văn A")
            .withEmail("nguyen@example.com")
            .build();

    Invoice invoice = anInvoice()
            .withAmount(new BigDecimal("2000000"))
            .build();

    // ... test logic
}
```

---

## Java Integration Testing

### Repository Integration Test

```java
package com.kiteclass.core.repository;

import com.kiteclass.core.entity.Student;
import com.kiteclass.core.enums.StudentStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link StudentRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("StudentRepository Integration Tests")
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void setUp() {
        studentRepository.deleteAll();
    }

    @Test
    @DisplayName("should find all active students")
    void shouldFindAllActiveStudents() {
        // Given
        studentRepository.saveAll(List.of(
                createStudent("Active 1", StudentStatus.ACTIVE),
                createStudent("Active 2", StudentStatus.ACTIVE),
                createStudent("Inactive", StudentStatus.INACTIVE)
        ));

        // When
        List<Student> activeStudents = studentRepository.findByStatus(StudentStatus.ACTIVE);

        // Then
        assertThat(activeStudents)
                .hasSize(2)
                .extracting(Student::getName)
                .containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    @DisplayName("should check email exists")
    void shouldCheckEmailExists() {
        // Given
        studentRepository.save(createStudent("Test", "existing@example.com"));

        // When & Then
        assertThat(studentRepository.existsByEmail("existing@example.com")).isTrue();
        assertThat(studentRepository.existsByEmail("new@example.com")).isFalse();
    }

    private Student createStudent(String name, StudentStatus status) {
        return Student.builder()
                .name(name)
                .email(name.toLowerCase().replace(" ", "") + "@test.com")
                .status(status)
                .build();
    }

    private Student createStudent(String name, String email) {
        return Student.builder()
                .name(name)
                .email(email)
                .status(StudentStatus.ACTIVE)
                .build();
    }
}
```

### Controller Integration Test

```java
package com.kiteclass.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.dto.request.CreateStudentRequest;
import com.kiteclass.core.entity.Student;
import com.kiteclass.core.repository.StudentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link StudentController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("StudentController Integration Tests")
class StudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    void setUp() {
        studentRepository.deleteAll();
    }

    // ==================== GET /api/v1/students ====================

    @Nested
    @DisplayName("GET /api/v1/students")
    class GetStudents {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return paginated students")
        void shouldReturnPaginatedStudents() throws Exception {
            // Given
            studentRepository.saveAll(List.of(
                    createStudent("Student A"),
                    createStudent("Student B")
            ));

            // When & Then
            mockMvc.perform(get("/api/v1/students")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].name").exists());
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/students"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== POST /api/v1/students ====================

    @Nested
    @DisplayName("POST /api/v1/students")
    class CreateStudent {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should create student successfully")
        void shouldCreateStudentSuccessfully() throws Exception {
            // Given
            CreateStudentRequest request = new CreateStudentRequest(
                    "Nguyễn Văn A",
                    "nguyen@example.com",
                    "0901234567",
                    null,
                    null
            );

            // When & Then
            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.name").value("Nguyễn Văn A"))
                    .andExpect(jsonPath("$.data.email").value("nguyen@example.com"));

            // Verify in database
            assertThat(studentRepository.findByEmail("nguyen@example.com")).isPresent();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 when validation fails")
        void shouldReturn400WhenValidationFails() throws Exception {
            // Given - invalid request (empty name)
            CreateStudentRequest request = new CreateStudentRequest(
                    "",  // Invalid
                    "invalid-email",  // Invalid
                    "123",  // Invalid phone
                    null,
                    null
            );

            // When & Then
            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.details.name").exists())
                    .andExpect(jsonPath("$.error.details.email").exists());
        }

        @Test
        @WithMockUser(roles = "TEACHER")
        @DisplayName("should return 403 when insufficient permissions")
        void shouldReturn403WhenInsufficientPermissions() throws Exception {
            // Given
            CreateStudentRequest request = new CreateStudentRequest(
                    "Test", "test@example.com", "0901234567", null, null
            );

            // When & Then
            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    private Student createStudent(String name) {
        return Student.builder()
                .name(name)
                .email(name.toLowerCase().replace(" ", "") + "@test.com")
                .phone("0901234567")
                .build();
    }
}
```

### Testcontainers for Database

```java
package com.kiteclass.core;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using Testcontainers.
 */
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("kiteclass_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## TypeScript/React Testing

### Component Test

```typescript
// components/students/student-form.test.tsx
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StudentForm } from './student-form';

// Mock the hook
vi.mock('@/hooks/use-students', () => ({
  useCreateStudent: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));

describe('StudentForm', () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render all form fields', () => {
    render(<StudentForm />, { wrapper });

    expect(screen.getByLabelText(/họ và tên/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/số điện thoại/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /lưu/i })).toBeInTheDocument();
  });

  it('should show validation errors for empty required fields', async () => {
    const user = userEvent.setup();
    render(<StudentForm />, { wrapper });

    // Submit without filling
    await user.click(screen.getByRole('button', { name: /lưu/i }));

    await waitFor(() => {
      expect(screen.getByText(/tên là bắt buộc/i)).toBeInTheDocument();
      expect(screen.getByText(/email là bắt buộc/i)).toBeInTheDocument();
    });
  });

  it('should show error for invalid email', async () => {
    const user = userEvent.setup();
    render(<StudentForm />, { wrapper });

    await user.type(screen.getByLabelText(/email/i), 'invalid-email');
    await user.click(screen.getByRole('button', { name: /lưu/i }));

    await waitFor(() => {
      expect(screen.getByText(/email không hợp lệ/i)).toBeInTheDocument();
    });
  });

  it('should show error for invalid phone', async () => {
    const user = userEvent.setup();
    render(<StudentForm />, { wrapper });

    await user.type(screen.getByLabelText(/số điện thoại/i), '123');
    await user.click(screen.getByRole('button', { name: /lưu/i }));

    await waitFor(() => {
      expect(screen.getByText(/số điện thoại không hợp lệ/i)).toBeInTheDocument();
    });
  });

  it('should call onSuccess when form submitted successfully', async () => {
    const { useCreateStudent } = await import('@/hooks/use-students');
    const mockMutate = vi.fn((data, { onSuccess }) => onSuccess?.());
    vi.mocked(useCreateStudent).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as any);

    const onSuccess = vi.fn();
    const user = userEvent.setup();
    render(<StudentForm onSuccess={onSuccess} />, { wrapper });

    // Fill valid data
    await user.type(screen.getByLabelText(/họ và tên/i), 'Nguyễn Văn A');
    await user.type(screen.getByLabelText(/email/i), 'nguyen@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0901234567');

    // Submit
    await user.click(screen.getByRole('button', { name: /lưu/i }));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Nguyễn Văn A',
          email: 'nguyen@example.com',
          phone: '0901234567',
        }),
        expect.any(Object)
      );
      expect(onSuccess).toHaveBeenCalled();
    });
  });

  it('should disable submit button while loading', () => {
    const { useCreateStudent } = require('@/hooks/use-students');
    vi.mocked(useCreateStudent).mockReturnValue({
      mutate: vi.fn(),
      isPending: true,
    } as any);

    render(<StudentForm />, { wrapper });

    expect(screen.getByRole('button', { name: /đang lưu/i })).toBeDisabled();
  });
});
```

### Hook Test

```typescript
// hooks/use-students.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useStudents, useCreateStudent } from './use-students';
import { apiClient } from '@/lib/api-client';

vi.mock('@/lib/api-client');

describe('useStudents', () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
  });

  describe('useStudents', () => {
    it('should fetch students successfully', async () => {
      const mockStudents = [
        { id: 1, name: 'Student A' },
        { id: 2, name: 'Student B' },
      ];

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: mockStudents, totalElements: 2 },
      });

      const { result } = renderHook(() => useStudents(), { wrapper });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(result.current.data?.content).toEqual(mockStudents);
      expect(apiClient.get).toHaveBeenCalledWith('/students', expect.any(Object));
    });

    it('should handle error', async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useStudents(), { wrapper });

      await waitFor(() => {
        expect(result.current.isError).toBe(true);
      });

      expect(result.current.error?.message).toBe('Network error');
    });
  });

  describe('useCreateStudent', () => {
    it('should create student and invalidate cache', async () => {
      const newStudent = { id: 1, name: 'New Student' };
      vi.mocked(apiClient.post).mockResolvedValue({ data: newStudent });

      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const { result } = renderHook(() => useCreateStudent(), { wrapper });

      result.current.mutate({
        name: 'New Student',
        email: 'new@example.com',
        phone: '0901234567',
      });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      });

      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['students'] });
    });
  });
});
```

### MSW (Mock Service Worker) Setup

```typescript
// __tests__/mocks/handlers.ts
import { http, HttpResponse } from 'msw';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL;

export const handlers = [
  // GET /students
  http.get(`${BASE_URL}/students`, ({ request }) => {
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page')) || 0;

    return HttpResponse.json({
      content: [
        { id: 1, name: 'Nguyễn Văn A', email: 'a@example.com', status: 'ACTIVE' },
        { id: 2, name: 'Trần Thị B', email: 'b@example.com', status: 'ACTIVE' },
      ],
      page,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });
  }),

  // POST /students
  http.post(`${BASE_URL}/students`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json(
      {
        data: {
          id: 3,
          ...body,
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
        },
      },
      { status: 201 }
    );
  }),

  // GET /students/:id
  http.get(`${BASE_URL}/students/:id`, ({ params }) => {
    const { id } = params;

    if (id === '999') {
      return HttpResponse.json(
        { error: { code: 'STUDENT_NOT_FOUND', message: 'Không tìm thấy học viên' } },
        { status: 404 }
      );
    }

    return HttpResponse.json({
      data: {
        id: Number(id),
        name: 'Test Student',
        email: 'test@example.com',
        status: 'ACTIVE',
      },
    });
  }),
];

// Error handlers for testing error scenarios
export const errorHandlers = [
  http.get(`${BASE_URL}/students`, () => {
    return HttpResponse.json(
      { error: { code: 'SYSTEM_ERROR', message: 'Internal server error' } },
      { status: 500 }
    );
  }),
];
```

```typescript
// __tests__/setup.ts
import { beforeAll, afterEach, afterAll } from 'vitest';
import { setupServer } from 'msw/node';
import { handlers } from './mocks/handlers';

export const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

### Utility Function Test

```typescript
// lib/utils.test.ts
import { describe, it, expect } from 'vitest';
import {
  formatCurrency,
  formatDate,
  formatPhone,
  calculateAge,
  cn,
} from './utils';

describe('formatCurrency', () => {
  it('should format VND correctly', () => {
    expect(formatCurrency(1000000)).toBe('1.000.000 ₫');
    expect(formatCurrency(2500000)).toBe('2.500.000 ₫');
    expect(formatCurrency(0)).toBe('0 ₫');
  });

  it('should handle negative numbers', () => {
    expect(formatCurrency(-500000)).toBe('-500.000 ₫');
  });
});

describe('formatDate', () => {
  it('should format date in Vietnamese format', () => {
    const date = new Date('2025-02-15');
    expect(formatDate(date)).toBe('15/02/2025');
  });

  it('should return empty string for null/undefined', () => {
    expect(formatDate(null)).toBe('');
    expect(formatDate(undefined)).toBe('');
  });
});

describe('formatPhone', () => {
  it('should format phone number with spaces', () => {
    expect(formatPhone('0901234567')).toBe('090 123 4567');
  });

  it('should handle invalid phone gracefully', () => {
    expect(formatPhone('')).toBe('');
    expect(formatPhone('123')).toBe('123');
  });
});

describe('calculateAge', () => {
  it('should calculate age correctly', () => {
    const birthDate = new Date('2010-01-15');
    const today = new Date('2025-02-15');
    expect(calculateAge(birthDate, today)).toBe(15);
  });

  it('should handle birthday not yet occurred this year', () => {
    const birthDate = new Date('2010-06-15');
    const today = new Date('2025-02-15');
    expect(calculateAge(birthDate, today)).toBe(14);
  });
});

describe('cn (classnames utility)', () => {
  it('should merge class names', () => {
    expect(cn('foo', 'bar')).toBe('foo bar');
  });

  it('should handle conditional classes', () => {
    expect(cn('base', true && 'active', false && 'hidden')).toBe('base active');
  });

  it('should handle Tailwind merge conflicts', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4');
    expect(cn('text-red-500', 'text-blue-500')).toBe('text-blue-500');
  });
});
```

---

## E2E Testing (Playwright)

```typescript
// e2e/students.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Student Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('[name="email"]', 'admin@example.com');
    await page.fill('[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });

  test('should display student list', async ({ page }) => {
    await page.goto('/students');

    // Wait for table to load
    await expect(page.locator('table')).toBeVisible();

    // Check table headers
    await expect(page.locator('th:has-text("Tên")')).toBeVisible();
    await expect(page.locator('th:has-text("Email")')).toBeVisible();
    await expect(page.locator('th:has-text("Trạng thái")')).toBeVisible();
  });

  test('should create new student', async ({ page }) => {
    await page.goto('/students');

    // Click add button
    await page.click('button:has-text("Thêm học viên")');

    // Fill form
    await page.fill('[name="name"]', 'Nguyễn Văn Test');
    await page.fill('[name="email"]', 'test@example.com');
    await page.fill('[name="phone"]', '0901234567');

    // Submit
    await page.click('button:has-text("Lưu")');

    // Verify success toast
    await expect(page.locator('text=Thêm học viên thành công')).toBeVisible();

    // Verify student appears in list
    await expect(page.locator('text=Nguyễn Văn Test')).toBeVisible();
  });

  test('should show validation errors', async ({ page }) => {
    await page.goto('/students');
    await page.click('button:has-text("Thêm học viên")');

    // Submit empty form
    await page.click('button:has-text("Lưu")');

    // Check validation messages
    await expect(page.locator('text=Tên là bắt buộc')).toBeVisible();
    await expect(page.locator('text=Email là bắt buộc')).toBeVisible();
  });

  test('should search students', async ({ page }) => {
    await page.goto('/students');

    // Type in search
    await page.fill('[placeholder="Tìm kiếm..."]', 'Nguyễn');

    // Wait for filtered results
    await page.waitForResponse(resp =>
      resp.url().includes('/students') && resp.url().includes('search=Nguyễn')
    );

    // Verify results contain search term
    const rows = page.locator('tbody tr');
    for (const row of await rows.all()) {
      await expect(row).toContainText(/nguyễn/i);
    }
  });
});
```

---

## Coverage Requirements

### Minimum Coverage

| Type | Minimum | Target |
|------|---------|--------|
| Line Coverage | 80% | 90% |
| Branch Coverage | 75% | 85% |
| Method Coverage | 85% | 95% |

### What to Test

| Layer | What to Test | Priority |
|-------|--------------|----------|
| Service | Business logic, validation, edge cases | High |
| Controller | Request/response mapping, auth, error handling | High |
| Repository | Custom queries, complex JPA queries | Medium |
| Utility | Pure functions, formatters, helpers | High |
| Components | User interactions, form validation | High |
| Hooks | State management, API calls | Medium |

### What NOT to Test

- Simple getters/setters
- Framework code (Spring, React)
- Third-party libraries
- Configuration classes (unless complex)
- DTOs/Entities (unless have logic)

## Actions

### Run tests
```bash
# Backend
./mvnw test                          # Unit tests
./mvnw verify                        # All tests + coverage

# Frontend
pnpm test                            # Vitest
pnpm test:coverage                   # With coverage
pnpm test:e2e                        # Playwright
```

### Generate coverage report
```bash
# Backend - JaCoCo
./mvnw jacoco:report
# Open target/site/jacoco/index.html

# Frontend - Vitest
pnpm test:coverage
# Open coverage/index.html
```
