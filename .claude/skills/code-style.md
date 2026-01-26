# Skill: Code Style & Conventions

Quy chuẩn code style cho KiteClass Platform - Java/Spring Boot & TypeScript/React.

## Mô tả

Tài liệu quy định chi tiết về:
- Naming conventions
- Code formatting
- JavaDoc / TSDoc requirements
- Import ordering
- Best practices
- Anti-patterns to avoid

## Trigger phrases

- "code style"
- "coding convention"
- "javadoc"
- "naming convention"
- "format code"

---

## Java/Spring Boot Code Style

### Package Structure

```
com.kiteclass.{service}/
├── config/           # Configuration classes
├── controller/       # REST controllers
├── service/          # Business logic
│   └── impl/         # Service implementations
├── repository/       # Data access
├── entity/           # JPA entities
├── dto/              # Data transfer objects
│   ├── request/      # Request DTOs
│   └── response/     # Response DTOs
├── mapper/           # Entity-DTO mappers
├── exception/        # Custom exceptions
├── util/             # Utility classes
└── constant/         # Constants & enums
```

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Package | lowercase, no underscores | `com.kiteclass.core` |
| Class | PascalCase, noun | `StudentService`, `InvoiceDTO` |
| Interface | PascalCase, adjective/noun | `Payable`, `StudentRepository` |
| Method | camelCase, verb | `findById()`, `calculateTotal()` |
| Variable | camelCase | `studentName`, `totalAmount` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| Enum | PascalCase, UPPER values | `PaymentStatus.PAID` |

### Class Naming Patterns

```java
// Controllers
@RestController
public class StudentController { }      // ✅ {Entity}Controller

// Services
public interface StudentService { }     // ✅ {Entity}Service
public class StudentServiceImpl { }     // ✅ {Entity}ServiceImpl

// Repositories
public interface StudentRepository { }  // ✅ {Entity}Repository

// DTOs
public record CreateStudentRequest() { }    // ✅ {Action}{Entity}Request
public record StudentResponse() { }         // ✅ {Entity}Response
public record StudentListResponse() { }     // ✅ {Entity}ListResponse

// Exceptions
public class StudentNotFoundException { }   // ✅ {Entity}NotFoundException
```

### Method Naming

```java
// Repository methods
findById(Long id)                    // ✅ Single entity
findAllByClassId(Long classId)       // ✅ Collection with filter
existsByEmail(String email)          // ✅ Boolean check
countByStatus(Status status)         // ✅ Count query
deleteByIdAndTenantId(...)           // ✅ Delete with conditions

// Service methods
createStudent(CreateStudentRequest)  // ✅ create{Entity}
updateStudent(Long id, UpdateReq)    // ✅ update{Entity}
deleteStudent(Long id)               // ✅ delete{Entity}
getStudentById(Long id)              // ✅ get{Entity}ById
listStudents(Pageable)               // ✅ list{Entity}s
enrollStudent(Long id, EnrollReq)    // ✅ {action}{Entity}

// Private helper methods
private void validateStudent(...)     // ✅ validate{Entity}
private BigDecimal calculateFee(...)  // ✅ calculate{Something}
private boolean isEligible(...)       // ✅ is{Condition}
private boolean hasPermission(...)    // ✅ has{Something}
```

---

## JavaDoc Requirements

### Class-Level JavaDoc (Bắt buộc)

```java
/**
 * Service xử lý nghiệp vụ quản lý học viên.
 *
 * <p>Bao gồm các chức năng:
 * <ul>
 *   <li>CRUD học viên</li>
 *   <li>Đăng ký học viên vào lớp</li>
 *   <li>Tính toán điểm danh</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 * @see StudentRepository
 * @see EnrollmentService
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
```

### Method-Level JavaDoc

```java
/**
 * Đăng ký học viên vào lớp học.
 *
 * <p>Quy trình xử lý:
 * <ol>
 *   <li>Kiểm tra học viên tồn tại</li>
 *   <li>Kiểm tra lớp còn chỗ trống</li>
 *   <li>Tạo enrollment record</li>
 *   <li>Tạo invoice cho học phí</li>
 * </ol>
 *
 * @param studentId ID của học viên
 * @param request   Thông tin đăng ký (classId, startDate, discount)
 * @return EnrollmentResponse chứa thông tin đăng ký
 * @throws StudentNotFoundException nếu không tìm thấy học viên
 * @throws ClassFullException nếu lớp đã đủ học viên
 * @throws DuplicateEnrollmentException nếu học viên đã đăng ký lớp này
 * @since 1.0.0
 */
@Override
@Transactional
public EnrollmentResponse enrollStudent(Long studentId, EnrollRequest request) {
```

### Khi Nào Cần JavaDoc

| Element | Required | Notes |
|---------|----------|-------|
| Public class | ✅ Bắt buộc | Mô tả mục đích, @author, @since |
| Public method | ✅ Bắt buộc | @param, @return, @throws |
| Private method | ❌ Optional | Chỉ khi logic phức tạp |
| Field | ❌ Optional | Chỉ khi không self-explanatory |
| Constant | ✅ Bắt buộc | Giải thích ý nghĩa giá trị |

### JavaDoc Don'ts

```java
// ❌ Redundant - không cần thiết
/**
 * Gets the name.
 * @return the name
 */
public String getName() { return name; }

// ❌ Meaningless
/**
 * This method does something.
 */
public void process() { }

// ❌ Out of date - comment không match code
/**
 * Deletes the student.  // Nhưng method là soft delete!
 */
public void deleteStudent(Long id) {
    student.setDeleted(true);  // Soft delete
}
```

---

## Code Formatting

### Indentation & Spacing

```java
// ✅ 4 spaces indentation (không dùng tabs)
public class StudentService {
    private final StudentRepository repository;

    public Student findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
    }
}

// ✅ Blank lines giữa methods
public void methodOne() {
    // ...
}
                                    // 1 blank line
public void methodTwo() {
    // ...
}

// ✅ No blank line sau opening brace
public void method() {
    doSomething();                  // ✅ Không có blank line đầu
}

// ❌ Avoid
public void method() {
                                    // ❌ Blank line thừa
    doSomething();
}
```

### Line Length & Wrapping

```java
// ✅ Max 120 characters per line
// ✅ Wrap method parameters
public StudentResponse createStudent(
        CreateStudentRequest request,
        Long tenantId,
        Long createdBy) {
    // ...
}

// ✅ Wrap chained calls
return students.stream()
        .filter(s -> s.isActive())
        .map(StudentMapper::toResponse)
        .sorted(Comparator.comparing(StudentResponse::name))
        .collect(Collectors.toList());

// ✅ Wrap annotations
@GetMapping("/{id}")
@PreAuthorize("hasPermission('STUDENT_VIEW')")
@Operation(summary = "Get student by ID")
public ResponseEntity<StudentResponse> getStudent(@PathVariable Long id) {
```

### Braces Style

```java
// ✅ K&R style (opening brace same line)
public void method() {
    if (condition) {
        doSomething();
    } else {
        doOther();
    }
}

// ✅ Always use braces (even single line)
if (condition) {
    return early;
}

// ❌ Avoid
if (condition) return early;      // ❌ No braces
if (condition)
    return early;                  // ❌ No braces
```

---

## Import Ordering

```java
// 1. Java standard library
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 2. Third-party libraries
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 3. Project imports
import com.kiteclass.core.dto.request.CreateStudentRequest;
import com.kiteclass.core.dto.response.StudentResponse;
import com.kiteclass.core.entity.Student;
import com.kiteclass.core.repository.StudentRepository;

// Rules:
// - Alphabetical within each group
// - Blank line between groups
// - No wildcard imports (import java.util.*)
// - Remove unused imports
```

---

## Annotation Ordering

```java
// Class annotations (theo thứ tự)
@Slf4j                              // 1. Lombok logging
@RequiredArgsConstructor            // 2. Lombok constructors
@Service                            // 3. Spring stereotype
@Transactional(readOnly = true)     // 4. Transaction
public class StudentServiceImpl {

// Method annotations
@Override                           // 1. Override
@Transactional                      // 2. Transaction
@PreAuthorize("...")                // 3. Security
@Operation(summary = "...")         // 4. OpenAPI
@GetMapping("/{id}")                // 5. Request mapping
public ResponseEntity<T> method() {

// Field annotations
@Id                                 // 1. JPA ID
@GeneratedValue(strategy = ...)     // 2. Generation strategy
@Column(name = "...", nullable = false)  // 3. Column definition
private Long id;
```

---

## Best Practices

### Constructor Injection (Luôn dùng)

```java
// ✅ Constructor injection with Lombok
@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentMapper mapper;
}

// ❌ Avoid field injection
@Service
public class StudentService {
    @Autowired  // ❌ Field injection
    private StudentRepository repository;
}
```

### Optional Handling

```java
// ✅ orElseThrow for required entities
Student student = repository.findById(id)
        .orElseThrow(() -> new StudentNotFoundException(id));

// ✅ orElse for optional with default
String nickname = student.getNickname().orElse("Unknown");

// ✅ ifPresent for optional action
repository.findById(id)
        .ifPresent(s -> notificationService.send(s));

// ❌ Avoid
if (optional.isPresent()) {         // ❌ Anti-pattern
    Student s = optional.get();
}

optional.get();                      // ❌ NoSuchElementException risk
```

### Stream Best Practices

```java
// ✅ Readable stream pipeline
List<StudentResponse> responses = students.stream()
        .filter(Student::isActive)
        .filter(s -> s.getEnrollments().size() > 0)
        .map(mapper::toResponse)
        .sorted(comparing(StudentResponse::name))
        .toList();

// ✅ Early termination
boolean hasOverdue = invoices.stream()
        .anyMatch(Invoice::isOverdue);

// ❌ Avoid side effects in streams
students.stream()
        .forEach(s -> s.setStatus(INACTIVE));  // ❌ Mutation in stream

// ✅ Better approach
students.forEach(s -> s.setStatus(INACTIVE));
repository.saveAll(students);
```

### Null Safety

```java
// ✅ Validate early with Objects.requireNonNull
public void updateStudent(Long id, UpdateRequest request) {
    Objects.requireNonNull(request, "Request cannot be null");
    // ...
}

// ✅ Return empty collection, not null
public List<Student> findByClassId(Long classId) {
    if (classId == null) {
        return Collections.emptyList();  // ✅ Not null
    }
    return repository.findByClassId(classId);
}

// ❌ Avoid returning null for collections
public List<Student> findAll() {
    return null;  // ❌ Never do this
}
```

#### ⚠️ Cảnh báo - Tránh vòng lặp Eclipse Null Analysis

**Vấn đề đã gặp:** Eclipse null analysis quá strict, không tương thích với Java/Spring/Lombok conventions.

**Giải pháp SAI đã thử:**
1. Thêm `@NonNull` annotations → Lombok không hỗ trợ trên generated code
2. Xóa Lombok, viết manual getters → Builder.build() không có `@NonNull`
3. Tạo NullSafetyUtils với `@NonNull` return → Vẫn warnings

**Giải pháp ĐÚNG:**
- **KHÔNG cố gắng fix Eclipse null analysis warnings**
- Disable trong `.vscode/settings.json`: `"java.compile.nullAnalysis.mode": "disabled"`
- Dùng `Objects.requireNonNull()` cho runtime checks
- Dùng annotations như documentation, không để satisfy analyzer

---

## TypeScript/React Code Style

### File Naming

```
src/
├── components/
│   ├── ui/                    # Shared UI components
│   │   ├── button.tsx         # kebab-case for files
│   │   ├── data-table.tsx
│   │   └── index.ts           # barrel export
│   └── students/
│       ├── student-form.tsx
│       ├── student-list.tsx
│       └── use-students.ts    # Custom hooks
├── hooks/
│   ├── use-auth.ts
│   └── use-debounce.ts
├── lib/
│   ├── api-client.ts
│   └── utils.ts
├── types/
│   ├── student.ts
│   └── api.ts
└── app/                       # Next.js App Router
    └── (dashboard)/
        └── students/
            └── page.tsx
```

### Component Structure

```tsx
// ✅ Recommended component structure
'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useCreateStudent } from '@/hooks/use-students';
import { createStudentSchema, type CreateStudentInput } from '@/types/student';

// Types at top (after imports)
interface StudentFormProps {
  onSuccess?: () => void;
  defaultValues?: Partial<CreateStudentInput>;
}

// Main component
export function StudentForm({ onSuccess, defaultValues }: StudentFormProps) {
  // 1. Hooks first
  const [isLoading, setIsLoading] = useState(false);
  const { mutate: createStudent } = useCreateStudent();

  const form = useForm<CreateStudentInput>({
    resolver: zodResolver(createStudentSchema),
    defaultValues,
  });

  // 2. Handlers
  const onSubmit = async (data: CreateStudentInput) => {
    setIsLoading(true);
    try {
      await createStudent(data);
      onSuccess?.();
    } finally {
      setIsLoading(false);
    }
  };

  // 3. Render
  return (
    <form onSubmit={form.handleSubmit(onSubmit)}>
      {/* ... */}
    </form>
  );
}

// Named export (not default)
```

### TypeScript Types

```typescript
// ✅ Use interface for object shapes
interface Student {
  id: number;
  name: string;
  email: string;
  status: StudentStatus;
  enrollments: Enrollment[];
}

// ✅ Use type for unions, intersections, utilities
type StudentStatus = 'active' | 'inactive' | 'graduated';
type CreateStudentInput = Omit<Student, 'id' | 'enrollments'>;
type StudentWithClass = Student & { className: string };

// ✅ Zod schemas for validation
const createStudentSchema = z.object({
  name: z.string().min(2, 'Tên phải có ít nhất 2 ký tự'),
  email: z.string().email('Email không hợp lệ'),
  phone: z.string().regex(/^0\d{9}$/, 'Số điện thoại không hợp lệ'),
});

type CreateStudentInput = z.infer<typeof createStudentSchema>;
```

### TSDoc Comments

```typescript
/**
 * Hook quản lý state và actions cho students.
 *
 * @example
 * ```tsx
 * const { students, isLoading, createStudent } = useStudents();
 * ```
 *
 * @returns Object chứa students data và mutation functions
 */
export function useStudents() {
  // ...
}

/**
 * Tính tổng học phí sau khi áp dụng giảm giá.
 *
 * @param baseAmount - Học phí gốc (VND)
 * @param discountPercent - Phần trăm giảm giá (0-100)
 * @returns Số tiền sau giảm giá
 *
 * @example
 * ```ts
 * calculateFee(2000000, 10); // Returns 1800000
 * ```
 */
export function calculateFee(baseAmount: number, discountPercent: number): number {
  return baseAmount * (1 - discountPercent / 100);
}
```

### Import Ordering (TypeScript)

```typescript
// 1. React/Next.js
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

// 2. Third-party libraries
import { useQuery, useMutation } from '@tanstack/react-query';
import { toast } from 'sonner';
import { z } from 'zod';

// 3. Internal aliases (@/)
import { Button } from '@/components/ui/button';
import { useAuth } from '@/hooks/use-auth';
import { apiClient } from '@/lib/api-client';
import type { Student } from '@/types/student';

// 4. Relative imports
import { StudentCard } from './student-card';
import { useStudentActions } from './use-student-actions';

// 5. Styles (if any)
import './styles.css';
```

---

## Anti-Patterns to Avoid

### Java Anti-Patterns

```java
// ❌ God class - quá nhiều responsibilities
public class StudentService {
    // 50+ methods covering students, classes, invoices, notifications...
}

// ❌ Magic numbers
if (retryCount > 3) { }           // ❌ What is 3?
if (retryCount > MAX_RETRIES) { } // ✅ Named constant

// ❌ Catching generic Exception
try {
    process();
} catch (Exception e) { }          // ❌ Too broad

// ✅ Catch specific exceptions
try {
    process();
} catch (StudentNotFoundException e) {
    // Handle not found
} catch (ValidationException e) {
    // Handle validation
}

// ❌ Boolean parameters
createStudent(student, true, false);  // ❌ What do these mean?

// ✅ Use builder or separate methods
createStudent(student);
createStudentWithNotification(student);
```

### TypeScript Anti-Patterns

```typescript
// ❌ Using 'any'
const data: any = fetchData();      // ❌ Loses type safety

// ✅ Use proper types or unknown
const data: Student[] = fetchData();
const data: unknown = fetchData();  // If truly unknown

// ❌ Mutating props
function StudentCard({ student }: Props) {
  student.name = 'New Name';        // ❌ Never mutate props
}

// ❌ Inline objects in dependencies
useEffect(() => {}, [{ id: 1 }]);   // ❌ New object every render

// ✅ Use primitive or useMemo
useEffect(() => {}, [student.id]);  // ✅ Primitive value

// ❌ Excessive useEffect
useEffect(() => {
  setFilteredData(data.filter(...));
}, [data, filter]);

// ✅ Use useMemo for derived state
const filteredData = useMemo(
  () => data.filter(...),
  [data, filter]
);
```

---

## IDE/Tool Configuration

### IntelliJ IDEA

```xml
<!-- .idea/codeStyles/Project.xml -->
<code_scheme name="KiteClass" version="173">
  <option name="RIGHT_MARGIN" value="120" />
  <option name="WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN" value="true" />
  <JavaCodeStyleSettings>
    <option name="CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND" value="999" />
    <option name="NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND" value="999" />
  </JavaCodeStyleSettings>
</code_scheme>
```

### ESLint + Prettier (Frontend)

```json
// .eslintrc.json
{
  "extends": [
    "next/core-web-vitals",
    "plugin:@typescript-eslint/recommended",
    "prettier"
  ],
  "rules": {
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/no-explicit-any": "error",
    "prefer-const": "error",
    "no-console": "warn"
  }
}
```

```json
// .prettierrc
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100
}
```

### Checkstyle (Backend)

```xml
<!-- checkstyle.xml (simplified) -->
<module name="Checker">
  <module name="TreeWalker">
    <module name="JavadocMethod"/>
    <module name="JavadocType"/>
    <module name="ConstantName"/>
    <module name="LocalVariableName"/>
    <module name="MethodName"/>
    <module name="PackageName"/>
    <module name="TypeName"/>
    <module name="AvoidStarImport"/>
    <module name="LineLength">
      <property name="max" value="120"/>
    </module>
  </module>
</module>
```

## Actions

### Format code trước khi commit
```bash
# Backend
./mvnw checkstyle:check

# Frontend
pnpm lint
pnpm format
```

### Verify JavaDoc
```bash
./mvnw javadoc:javadoc
```
