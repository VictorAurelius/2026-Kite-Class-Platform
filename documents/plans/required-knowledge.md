# KIẾN THỨC CẦN NẮM VỮNG
## Phát triển và Triển khai KiteClass Platform V3.1

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Dự án** | KiteClass Platform V3.1 |
| **Loại tài liệu** | Technical Knowledge Requirements |
| **Ngày tạo** | 23/12/2025 |
| **Đối tượng** | Development Team |

---

# MỤC LỤC

1. [Tổng quan Tech Stack](#1-tổng-quan-tech-stack)
2. [Backend Development](#2-backend-development)
3. [Frontend Development](#3-frontend-development)
4. [Database & Data](#4-database--data)
5. [DevOps & Deployment](#5-devops--deployment)
6. [Security](#6-security)
7. [Testing](#7-testing)
8. [Soft Skills & Processes](#8-soft-skills--processes)
9. [Learning Roadmap](#9-learning-roadmap)
10. [Tài liệu tham khảo](#10-tài-liệu-tham-khảo)

---

# 1. TỔNG QUAN TECH STACK

## 1.1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         KITECLASS TECH STACK V3.1                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                            KITEHUB                                       │   │
│  │                                                                          │   │
│  │  Backend: Java Spring Boot (Modular Monolith)                            │   │
│  │  Frontend: Next.js 14 + TypeScript + TailwindCSS                         │   │
│  │  Database: PostgreSQL                                                    │   │
│  │  Cache: Redis                                                            │   │
│  │  Queue: RabbitMQ                                                         │   │
│  │  AI: OpenAI GPT-4, Stability AI SDXL                                     │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                         KITECLASS INSTANCE                               │   │
│  │                                                                          │   │
│  │  User+Gateway Service: Java Spring Boot + Spring Cloud Gateway           │   │
│  │  Core Service: Java Spring Boot                                          │   │
│  │  Engagement Service: Java Spring Boot (Optional)                         │   │
│  │  Media Service: Node.js + FFmpeg (Optional)                              │   │
│  │  Frontend: Next.js 14 + TypeScript                                       │   │
│  │  Database: PostgreSQL (per instance)                                     │   │
│  │  Cache: Redis                                                            │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                          INFRASTRUCTURE                                  │   │
│  │                                                                          │   │
│  │  Container: Docker + Docker Compose                                      │   │
│  │  Orchestration: Kubernetes (Production)                                  │   │
│  │  CI/CD: GitHub Actions                                                   │   │
│  │  Cloud: AWS / DigitalOcean / Vultr                                       │   │
│  │  CDN: CloudFlare                                                         │   │
│  │  Storage: S3 / CloudFlare R2                                             │   │
│  │  Monitoring: Prometheus + Grafana                                        │   │
│  │  Logging: ELK Stack / Loki                                               │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 1.2. Skill Matrix theo Role

| Role | Primary Skills | Secondary Skills | Level Required |
|------|----------------|------------------|----------------|
| **Backend Dev** | Java, Spring Boot, PostgreSQL | Redis, RabbitMQ, Docker | Mid-Senior |
| **Frontend Dev** | React, Next.js, TypeScript | TailwindCSS, Zustand | Mid-Senior |
| **Full-stack Dev** | Java + React/Next.js | Docker, PostgreSQL | Senior |
| **DevOps Engineer** | Docker, Kubernetes, CI/CD | AWS, Terraform, Monitoring | Mid-Senior |
| **QA Engineer** | Testing strategies, Automation | JUnit, Cypress, Performance | Mid |

---

# 2. BACKEND DEVELOPMENT

## 2.1. Java Core (Bắt buộc)

### Mức độ: Intermediate - Advanced

| Topic | Kiến thức cần nắm | Mức độ quan trọng |
|-------|-------------------|-------------------|
| **Java Fundamentals** | OOP, Collections, Generics, Streams, Lambda | P0 |
| **Java 17+ Features** | Records, Sealed Classes, Pattern Matching | P1 |
| **Concurrency** | Thread, ExecutorService, CompletableFuture | P1 |
| **Memory Management** | Heap, Stack, GC tuning | P2 |
| **JVM Internals** | Class loading, JIT compilation | P2 |

```java
// Ví dụ: Sử dụng Record (Java 17+) cho DTO
public record StudentDTO(
    Long id,
    String name,
    String email,
    LocalDate enrollmentDate
) {}

// Ví dụ: Stream API với Collectors
Map<String, List<Student>> studentsByClass = students.stream()
    .filter(s -> s.isActive())
    .collect(Collectors.groupingBy(Student::getClassName));

// Ví dụ: CompletableFuture cho async operations
CompletableFuture<NotificationResult> future = CompletableFuture
    .supplyAsync(() -> sendZaloNotification(parent, message))
    .thenApply(response -> processResponse(response))
    .exceptionally(ex -> handleError(ex));
```

## 2.2. Spring Framework (Bắt buộc)

### Mức độ: Advanced

| Component | Kiến thức cần nắm | Ứng dụng trong KiteClass |
|-----------|-------------------|--------------------------|
| **Spring Core** | IoC, DI, AOP, Bean lifecycle | Foundation |
| **Spring Boot** | Auto-configuration, Starters, Properties | All services |
| **Spring MVC** | REST API, Exception handling, Validation | API development |
| **Spring Data JPA** | Repository, Query methods, Specifications | Database access |
| **Spring Security** | JWT, OAuth2, RBAC | Authentication/Authorization |
| **Spring Cloud Gateway** | Routing, Filters, Rate limiting | API Gateway |
| **Spring WebFlux** | Reactive programming (optional) | High-concurrency scenarios |

```java
// Ví dụ: REST Controller với Validation
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentDTO> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        StudentDTO student = studentService.create(request, principal.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(student);
    }

    @GetMapping
    public Page<StudentDTO> getStudents(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {

        return studentService.findAll(search, pageable);
    }
}

// Ví dụ: Spring Data JPA Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE s.tenantId = :tenantId AND s.active = true")
    Page<Student> findActiveByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT s FROM Student s JOIN s.enrollments e WHERE e.classEntity.id = :classId")
    List<Student> findByClassId(@Param("classId") Long classId);

    boolean existsByEmailAndTenantId(String email, Long tenantId);
}

// Ví dụ: Service với Transaction
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;

    @Transactional
    public StudentDTO create(CreateStudentRequest request, Long tenantId) {
        // Validate
        if (studentRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
            throw new DuplicateEmailException(request.email());
        }

        // Create student
        Student student = Student.builder()
            .name(request.name())
            .email(request.email())
            .phone(request.phone())
            .tenantId(tenantId)
            .active(true)
            .build();

        student = studentRepository.save(student);

        // Initialize gamification
        gamificationService.initializeStudent(student);

        // Send welcome notification
        notificationService.sendWelcome(student);

        return StudentDTO.from(student);
    }
}
```

## 2.3. Spring Cloud Gateway (Quan trọng)

```java
// Gateway Configuration
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Core Service routes
            .route("core-service", r -> r
                .path("/api/v1/classes/**", "/api/v1/students/**", "/api/v1/attendance/**")
                .filters(f -> f
                    .addRequestHeader("X-Tenant-Id", "#{tenantId}")
                    .circuitBreaker(c -> c.setName("coreServiceCB").setFallbackUri("/fallback"))
                    .requestRateLimiter(l -> l.setRateLimiter(redisRateLimiter())))
                .uri("lb://core-service"))

            // Engagement Service routes (optional)
            .route("engagement-service", r -> r
                .path("/api/v1/gamification/**", "/api/v1/forum/**", "/api/v1/parent-portal/**")
                .filters(f -> f.addRequestHeader("X-Tenant-Id", "#{tenantId}"))
                .uri("lb://engagement-service"))
            .build();
    }
}

// JWT Authentication Filter
@Component
public class JwtAuthenticationFilter implements GatewayFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());

        if (token != null && tokenProvider.validateToken(token)) {
            Claims claims = tokenProvider.getClaims(token);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Roles", claims.get("roles", String.class))
                .header("X-Tenant-Id", claims.get("tenantId", String.class))
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }
}
```

## 2.4. Database Access (JPA/Hibernate)

| Topic | Kiến thức cần nắm | Mức độ |
|-------|-------------------|--------|
| **Entity Mapping** | @Entity, @Table, @Column, @Id | P0 |
| **Relationships** | @OneToMany, @ManyToOne, @ManyToMany, Lazy/Eager | P0 |
| **Query Methods** | Derived queries, @Query, Native queries | P0 |
| **Specifications** | Dynamic queries, Criteria API | P1 |
| **N+1 Problem** | Fetch strategies, EntityGraph, JOIN FETCH | P0 |
| **Auditing** | @CreatedDate, @LastModifiedDate, @CreatedBy | P1 |
| **Multi-tenancy** | Discriminator column, Schema-per-tenant | P1 |

```java
// Ví dụ: Entity với Multi-tenancy
@Entity
@Table(name = "students")
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "deleted = false")  // Soft delete
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;  // Multi-tenancy

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "student")
    private List<GamificationPoint> points = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private boolean deleted = false;
}

// Ví dụ: Giải quyết N+1 với EntityGraph
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"enrollments", "enrollments.classEntity"})
    @Query("SELECT s FROM Student s WHERE s.tenantId = :tenantId")
    List<Student> findAllWithEnrollments(@Param("tenantId") Long tenantId);

    // Hoặc dùng JOIN FETCH
    @Query("SELECT DISTINCT s FROM Student s " +
           "LEFT JOIN FETCH s.enrollments e " +
           "LEFT JOIN FETCH e.classEntity " +
           "WHERE s.tenantId = :tenantId")
    List<Student> findAllWithEnrollmentsJoinFetch(@Param("tenantId") Long tenantId);
}
```

## 2.5. API Design (REST)

| Topic | Kiến thức cần nắm |
|-------|-------------------|
| **REST Principles** | Resources, HTTP methods, Status codes |
| **URL Design** | Naming conventions, Versioning |
| **Request/Response** | DTO patterns, Pagination, HATEOAS |
| **Error Handling** | Global exception handler, Error responses |
| **Documentation** | OpenAPI/Swagger |
| **Validation** | Bean Validation, Custom validators |

```java
// Ví dụ: Global Exception Handler
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (a, b) -> a
            ));

        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_FAILED",
            "Validation failed",
            errors,
            LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "ACCESS_DENIED",
            "You don't have permission to access this resource",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
```

## 2.6. Message Queue (RabbitMQ)

```java
// Ví dụ: Publisher
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishAttendanceNotification(AttendanceEvent event) {
        rabbitTemplate.convertAndSend(
            "notifications.exchange",
            "attendance.marked",
            event
        );
    }

    public void publishPaymentReminder(PaymentReminderEvent event) {
        rabbitTemplate.convertAndSend(
            "notifications.exchange",
            "payment.reminder",
            event
        );
    }
}

// Ví dụ: Consumer
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ZaloNotificationService zaloService;
    private final EmailService emailService;

    @RabbitListener(queues = "notifications.attendance")
    public void handleAttendance(AttendanceEvent event) {
        if (event.status() == AttendanceStatus.ABSENT) {
            // Notify parent via Zalo
            zaloService.sendAbsentNotification(
                event.parentPhone(),
                event.studentName(),
                event.className(),
                event.date()
            );
        }
    }

    @RabbitListener(queues = "notifications.payment")
    public void handlePaymentReminder(PaymentReminderEvent event) {
        // Send reminder via Zalo + Email
        zaloService.sendPaymentReminder(event);
        emailService.sendPaymentReminder(event);
    }
}
```

## 2.7. Caching (Redis)

```java
// Ví dụ: Cache Configuration
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("students",
                config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("classes",
                config.entryTtl(Duration.ofHours(2)))
            .build();
    }
}

// Ví dụ: Service với Cache
@Service
public class ClassService {

    @Cacheable(value = "classes", key = "#tenantId + ':' + #classId")
    public ClassDTO getClass(Long tenantId, Long classId) {
        return classRepository.findByIdAndTenantId(classId, tenantId)
            .map(ClassDTO::from)
            .orElseThrow(() -> new ClassNotFoundException(classId));
    }

    @CacheEvict(value = "classes", key = "#tenantId + ':' + #classId")
    @Transactional
    public ClassDTO updateClass(Long tenantId, Long classId, UpdateClassRequest request) {
        // Update logic
    }

    @CacheEvict(value = "classes", allEntries = true)
    @Transactional
    public void deleteClass(Long tenantId, Long classId) {
        // Delete logic
    }
}
```

---

# 3. FRONTEND DEVELOPMENT

## 3.1. JavaScript/TypeScript (Bắt buộc)

### Mức độ: Advanced

| Topic | Kiến thức cần nắm | Mức độ |
|-------|-------------------|--------|
| **ES6+ Features** | Arrow functions, Destructuring, Spread, Modules | P0 |
| **Async Programming** | Promises, async/await, Error handling | P0 |
| **TypeScript** | Types, Interfaces, Generics, Type guards | P0 |
| **Functional Programming** | map, filter, reduce, Pure functions | P1 |

```typescript
// Ví dụ: TypeScript Types cho KiteClass
interface Student {
  id: number;
  name: string;
  email: string;
  phone?: string;
  enrollments: Enrollment[];
  gamificationPoints: number;
  badges: Badge[];
}

interface Enrollment {
  id: number;
  classId: number;
  className: string;
  enrolledAt: Date;
  status: 'active' | 'completed' | 'dropped';
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp: string;
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

// Ví dụ: API Client với TypeScript
class ApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  setToken(token: string) {
    this.token = token;
  }

  async get<T>(path: string): Promise<T> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new ApiError(response.status, await response.text());
    }

    return response.json();
  }

  async post<T, R>(path: string, body: T): Promise<R> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      throw new ApiError(response.status, await response.text());
    }

    return response.json();
  }

  private getHeaders(): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    return headers;
  }
}
```

## 3.2. React (Bắt buộc)

### Mức độ: Advanced

| Topic | Kiến thức cần nắm | Mức độ |
|-------|-------------------|--------|
| **Components** | Functional components, Props, Children | P0 |
| **Hooks** | useState, useEffect, useContext, useReducer, useMemo, useCallback | P0 |
| **Custom Hooks** | Reusable logic extraction | P0 |
| **State Management** | Context API, Zustand, React Query | P0 |
| **Performance** | Memoization, Code splitting, Lazy loading | P1 |
| **Forms** | React Hook Form, Validation | P0 |
| **Testing** | Jest, React Testing Library | P1 |

```typescript
// Ví dụ: Custom Hook cho Attendance
function useAttendance(classId: number, date: string) {
  const queryClient = useQueryClient();

  const { data: attendance, isLoading, error } = useQuery({
    queryKey: ['attendance', classId, date],
    queryFn: () => api.get<Attendance[]>(`/classes/${classId}/attendance?date=${date}`),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  const markAttendanceMutation = useMutation({
    mutationFn: (data: MarkAttendanceRequest) =>
      api.post(`/classes/${classId}/attendance`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attendance', classId, date] });
      toast.success('Điểm danh thành công!');
    },
    onError: (error) => {
      toast.error('Có lỗi xảy ra: ' + error.message);
    },
  });

  return {
    attendance,
    isLoading,
    error,
    markAttendance: markAttendanceMutation.mutate,
    isMarking: markAttendanceMutation.isPending,
  };
}

// Ví dụ: Component sử dụng Hook
function AttendanceSheet({ classId, date }: AttendanceSheetProps) {
  const { attendance, isLoading, markAttendance, isMarking } = useAttendance(classId, date);
  const [selectedStudents, setSelectedStudents] = useState<Set<number>>(new Set());

  if (isLoading) return <LoadingSpinner />;

  const handleSubmit = () => {
    const records = attendance!.map(record => ({
      studentId: record.studentId,
      status: selectedStudents.has(record.studentId) ? 'PRESENT' : 'ABSENT',
    }));

    markAttendance({ date, records });
  };

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Điểm danh ngày {date}</h2>

      <div className="divide-y">
        {attendance?.map(record => (
          <div key={record.studentId} className="flex items-center py-2">
            <Checkbox
              checked={selectedStudents.has(record.studentId)}
              onCheckedChange={(checked) => {
                setSelectedStudents(prev => {
                  const next = new Set(prev);
                  if (checked) next.add(record.studentId);
                  else next.delete(record.studentId);
                  return next;
                });
              }}
            />
            <span className="ml-3">{record.studentName}</span>
          </div>
        ))}
      </div>

      <Button onClick={handleSubmit} disabled={isMarking}>
        {isMarking ? 'Đang lưu...' : 'Lưu điểm danh'}
      </Button>
    </div>
  );
}
```

## 3.3. Next.js 14 (Bắt buộc)

### Mức độ: Intermediate - Advanced

| Topic | Kiến thức cần nắm | Mức độ |
|-------|-------------------|--------|
| **App Router** | Layouts, Pages, Loading, Error boundaries | P0 |
| **Data Fetching** | Server Components, Client Components, Streaming | P0 |
| **Server Actions** | Form handling, Mutations | P1 |
| **Caching** | Static, Dynamic, ISR, Revalidation | P1 |
| **Middleware** | Authentication, Redirects | P0 |
| **Optimization** | Image, Font, Script optimization | P1 |

```typescript
// Ví dụ: App Router Structure
// app/
// ├── (auth)/
// │   ├── login/page.tsx
// │   └── layout.tsx
// ├── (dashboard)/
// │   ├── layout.tsx
// │   ├── page.tsx
// │   ├── students/
// │   │   ├── page.tsx
// │   │   └── [id]/page.tsx
// │   └── classes/
// │       └── page.tsx
// └── api/
//     └── auth/[...nextauth]/route.ts

// Ví dụ: Server Component với Data Fetching
// app/(dashboard)/students/page.tsx
async function StudentsPage() {
  const students = await getStudents(); // Server-side fetch

  return (
    <div>
      <h1>Danh sách học viên</h1>
      <Suspense fallback={<StudentTableSkeleton />}>
        <StudentTable students={students} />
      </Suspense>
    </div>
  );
}

// Ví dụ: Middleware cho Authentication
// middleware.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { getToken } from 'next-auth/jwt';

export async function middleware(request: NextRequest) {
  const token = await getToken({ req: request });
  const isAuthPage = request.nextUrl.pathname.startsWith('/login');

  if (!token && !isAuthPage) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  if (token && isAuthPage) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico).*)'],
};
```

## 3.4. TailwindCSS & Shadcn/UI (Bắt buộc)

```typescript
// Ví dụ: Component với Tailwind + Shadcn
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

function StudentCard({ student }: { student: Student }) {
  return (
    <Card className="hover:shadow-lg transition-shadow">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg">{student.name}</CardTitle>
        <Badge variant={student.status === 'active' ? 'default' : 'secondary'}>
          {student.status === 'active' ? 'Đang học' : 'Nghỉ'}
        </Badge>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          <p className="text-sm text-muted-foreground">{student.email}</p>
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium">Điểm tích lũy:</span>
            <span className="text-primary font-bold">{student.gamificationPoints}</span>
          </div>
          <div className="flex gap-1 flex-wrap">
            {student.badges.map(badge => (
              <Badge key={badge.id} variant="outline" className="text-xs">
                {badge.name}
              </Badge>
            ))}
          </div>
        </div>
        <div className="mt-4 flex gap-2">
          <Button variant="outline" size="sm">Xem chi tiết</Button>
          <Button size="sm">Chỉnh sửa</Button>
        </div>
      </CardContent>
    </Card>
  );
}
```

## 3.5. State Management

```typescript
// Ví dụ: Zustand Store
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => void;
  setUser: (user: User) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,

      login: async (credentials) => {
        const response = await api.post<LoginResponse>('/auth/login', credentials);
        set({
          user: response.user,
          token: response.token,
          isAuthenticated: true,
        });
        api.setToken(response.token);
      },

      logout: () => {
        set({ user: null, token: null, isAuthenticated: false });
        api.setToken(null);
      },

      setUser: (user) => set({ user }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ token: state.token, user: state.user }),
    }
  )
);

// Ví dụ: React Query cho Server State
// hooks/useStudents.ts
export function useStudents(classId?: number) {
  return useQuery({
    queryKey: ['students', { classId }],
    queryFn: async () => {
      const params = classId ? `?classId=${classId}` : '';
      return api.get<PaginatedResponse<Student>>(`/students${params}`);
    },
  });
}

export function useCreateStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateStudentRequest) => api.post<Student>('/students', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
    },
  });
}
```

---

# 4. DATABASE & DATA

## 4.1. PostgreSQL (Bắt buộc)

### Mức độ: Intermediate

| Topic | Kiến thức cần nắm | Mức độ |
|-------|-------------------|--------|
| **SQL Fundamentals** | SELECT, JOIN, Subqueries, Aggregations | P0 |
| **DDL** | CREATE, ALTER, DROP, Constraints | P0 |
| **Indexes** | B-tree, GIN, GiST, Partial indexes | P1 |
| **Performance** | EXPLAIN ANALYZE, Query optimization | P1 |
| **Transactions** | ACID, Isolation levels | P1 |
| **JSON Support** | JSONB, JSON operators | P1 |
| **Full-text Search** | tsvector, tsquery | P2 |

```sql
-- Ví dụ: Query với Performance Optimization
-- Bad: N+1 problem
SELECT * FROM students WHERE class_id = 1;
-- Then loop to get enrollments

-- Good: Single query with JOIN
SELECT
    s.id,
    s.name,
    s.email,
    array_agg(DISTINCT e.class_id) as class_ids,
    array_agg(DISTINCT b.name) as badges,
    COALESCE(SUM(gp.points), 0) as total_points
FROM students s
LEFT JOIN enrollments e ON s.id = e.student_id AND e.status = 'active'
LEFT JOIN student_badges sb ON s.id = sb.student_id
LEFT JOIN badges b ON sb.badge_id = b.id
LEFT JOIN gamification_points gp ON s.id = gp.student_id
WHERE s.tenant_id = 1 AND s.active = true
GROUP BY s.id, s.name, s.email
ORDER BY total_points DESC
LIMIT 20 OFFSET 0;

-- Ví dụ: Index Strategy
CREATE INDEX idx_students_tenant_active ON students(tenant_id) WHERE active = true;
CREATE INDEX idx_enrollments_student_status ON enrollments(student_id, status);
CREATE INDEX idx_attendance_class_date ON attendance(class_id, attendance_date);
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE status = 'pending';

-- Ví dụ: JSONB cho flexible data
ALTER TABLE classes ADD COLUMN metadata JSONB DEFAULT '{}';

-- Query JSONB
SELECT * FROM classes
WHERE metadata->>'room' = 'A101'
  AND (metadata->>'capacity')::int > 20;

-- Ví dụ: Full-text Search
ALTER TABLE courses ADD COLUMN search_vector tsvector;

CREATE INDEX idx_courses_search ON courses USING GIN(search_vector);

UPDATE courses SET search_vector =
    setweight(to_tsvector('simple', coalesce(name, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(description, '')), 'B');

-- Search query
SELECT * FROM courses
WHERE search_vector @@ plainto_tsquery('simple', 'toán lớp 10')
ORDER BY ts_rank(search_vector, plainto_tsquery('simple', 'toán lớp 10')) DESC;
```

## 4.2. Database Design Principles

| Principle | Mô tả | Áp dụng |
|-----------|-------|---------|
| **Normalization** | 3NF cho OLTP | Transaction data |
| **Denormalization** | Cho reporting/read-heavy | Dashboard, Reports |
| **Multi-tenancy** | Tenant ID column | All tenant-specific tables |
| **Soft Delete** | deleted_at column | Preserve data integrity |
| **Audit Trail** | created_at, updated_at, created_by | All tables |

---

# 5. DEVOPS & DEPLOYMENT

## 5.1. Docker (Bắt buộc)

### Mức độ: Intermediate

| Topic | Kiến thức cần nắm | Mức độ |
|-------|-------------------|--------|
| **Dockerfile** | Multi-stage builds, Layer optimization | P0 |
| **Docker Compose** | Services, Networks, Volumes | P0 |
| **Images** | Build, Tag, Push, Registry | P0 |
| **Networking** | Bridge, Host, Overlay | P1 |
| **Volumes** | Named, Bind mounts | P0 |
| **Security** | Non-root user, Secrets | P1 |

```dockerfile
# Ví dụ: Multi-stage Dockerfile cho Spring Boot
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: Run as non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy artifact
COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# Ví dụ: docker-compose.yml cho KiteClass Instance
version: '3.8'

services:
  user-gateway:
    build:
      context: ./user-gateway
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/kiteclass
      - SPRING_REDIS_HOST=redis
      - CORE_SERVICE_URL=http://core-service:8081
      - ENGAGEMENT_SERVICE_URL=http://engagement-service:8082
      - ENGAGEMENT_ENABLED=${ENGAGEMENT_ENABLED:-false}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  core-service:
    build: ./core-service
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/kiteclass
      - SPRING_REDIS_HOST=redis
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  engagement-service:
    build: ./engagement-service
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/kiteclass
    profiles:
      - engagement  # Only start if profile is activated
    depends_on:
      postgres:
        condition: service_healthy

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    environment:
      - NEXT_PUBLIC_API_URL=http://user-gateway:8080
    depends_on:
      - user-gateway

  postgres:
    image: postgres:15-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    environment:
      - POSTGRES_DB=kiteclass
      - POSTGRES_USER=${DB_USER}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d kiteclass"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes

volumes:
  postgres_data:
  redis_data:

networks:
  default:
    name: kiteclass-network
```

## 5.2. Kubernetes (Production)

### Mức độ: Intermediate

| Topic | Kiến thức cần nắm | Mức độ |
|-------|-------------------|--------|
| **Core Concepts** | Pods, Deployments, Services, ConfigMaps, Secrets | P0 |
| **Networking** | Ingress, Network Policies | P1 |
| **Storage** | PersistentVolumes, StorageClasses | P1 |
| **Scaling** | HPA, VPA | P1 |
| **Monitoring** | Prometheus, Grafana | P1 |
| **Helm** | Charts, Values, Releases | P1 |

```yaml
# Ví dụ: Kubernetes Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-gateway
  namespace: kiteclass-{{ .Values.instanceId }}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: user-gateway
  template:
    metadata:
      labels:
        app: user-gateway
    spec:
      containers:
        - name: user-gateway
          image: kiteclass/user-gateway:{{ .Values.version }}
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "kubernetes"
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: url
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: user-gateway
  namespace: kiteclass-{{ .Values.instanceId }}
spec:
  selector:
    app: user-gateway
  ports:
    - port: 8080
      targetPort: 8080
```

## 5.3. CI/CD (GitHub Actions)

```yaml
# Ví dụ: .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: mvn test -B

      - name: Upload coverage
        uses: codecov/codecov-action@v3

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: staging
    steps:
      - name: Deploy to Staging
        run: |
          # Deploy using kubectl or helm
          helm upgrade --install kiteclass ./helm \
            --namespace staging \
            --set image.tag=${{ github.sha }}

  deploy-production:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production
    steps:
      - name: Deploy to Production
        run: |
          helm upgrade --install kiteclass ./helm \
            --namespace production \
            --set image.tag=${{ github.sha }}
```

## 5.4. Cloud Services

| Service | Provider Options | Kiến thức cần nắm |
|---------|------------------|-------------------|
| **Compute** | AWS EC2, DigitalOcean Droplets | Instance types, Pricing |
| **Kubernetes** | AWS EKS, DO Kubernetes | Managed K8s |
| **Database** | AWS RDS, DO Managed DB | PostgreSQL managed |
| **Storage** | AWS S3, CloudFlare R2 | Object storage |
| **CDN** | CloudFlare | Caching, DDoS protection |
| **DNS** | CloudFlare | DNS management |

---

# 6. SECURITY

## 6.1. Authentication & Authorization

| Topic | Kiến thức cần nắm | Mức độ |
|-------|-------------------|--------|
| **JWT** | Structure, Signing, Validation, Refresh tokens | P0 |
| **OAuth 2.0** | Authorization Code, PKCE | P1 |
| **RBAC** | Roles, Permissions, Hierarchies | P0 |
| **Password Security** | Bcrypt, Argon2, Salt | P0 |
| **Session Management** | Token storage, Rotation | P1 |

```java
// Ví dụ: JWT Token Provider
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(UserPrincipal user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles())
            .claim("tenantId", user.getTenantId())
            .claim("permissions", user.getPermissions())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS512)
            .compact();
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
```

## 6.2. OWASP Top 10 Protection

| Vulnerability | Mitigation | Implementation |
|---------------|------------|----------------|
| **Injection** | Parameterized queries | JPA/Hibernate |
| **Broken Auth** | JWT, Rate limiting | Spring Security |
| **Sensitive Data** | Encryption at rest/transit | TLS, bcrypt |
| **XXE** | Disable DTD | Jackson config |
| **Broken Access** | RBAC, Resource-level auth | @PreAuthorize |
| **Security Misconfig** | Secure defaults | Spring Security |
| **XSS** | Output encoding | React auto-escaping |
| **Insecure Deserialization** | Input validation | DTO validation |
| **Vulnerable Components** | Dependency scanning | Dependabot |
| **Logging & Monitoring** | Comprehensive logging | ELK Stack |

---

# 7. TESTING

## 7.1. Testing Pyramid

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            TESTING PYRAMID                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                              ▲                                                   │
│                             /│\     E2E Tests (Cypress)                          │
│                            / │ \    5-10% - Slow, Expensive                     │
│                           /  │  \                                                │
│                          /   │   \                                               │
│                         /────┼────\  Integration Tests (Spring Test)             │
│                        /     │     \ 20-30% - Medium speed                       │
│                       /      │      \                                            │
│                      /───────┼───────\  Unit Tests (JUnit, Jest)                 │
│                     /        │        \ 60-70% - Fast, Cheap                     │
│                    /─────────┼─────────\                                         │
│                   ──────────────────────                                         │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 7.2. Backend Testing

```java
// Ví dụ: Unit Test với JUnit 5 + Mockito
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createStudent_Success() {
        // Given
        CreateStudentRequest request = new CreateStudentRequest(
            "Nguyen Van A", "a@example.com", "0901234567"
        );
        Long tenantId = 1L;

        when(studentRepository.existsByEmailAndTenantId(request.email(), tenantId))
            .thenReturn(false);
        when(studentRepository.save(any(Student.class)))
            .thenAnswer(invocation -> {
                Student s = invocation.getArgument(0);
                s.setId(1L);
                return s;
            });

        // When
        StudentDTO result = studentService.create(request, tenantId);

        // Then
        assertThat(result.name()).isEqualTo("Nguyen Van A");
        assertThat(result.email()).isEqualTo("a@example.com");
        verify(gamificationService).initializeStudent(any(Student.class));
    }

    @Test
    void createStudent_DuplicateEmail_ThrowsException() {
        // Given
        CreateStudentRequest request = new CreateStudentRequest(
            "Nguyen Van A", "existing@example.com", "0901234567"
        );
        Long tenantId = 1L;

        when(studentRepository.existsByEmailAndTenantId(request.email(), tenantId))
            .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> studentService.create(request, tenantId))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("existing@example.com");
    }
}

// Ví dụ: Integration Test với Spring Boot Test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStudent_Integration() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
            "Test Student", "test@example.com", "0901234567"
        );

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Student"))
            .andExpect(jsonPath("$.email").value("test@example.com"));

        assertThat(studentRepository.findByEmail("test@example.com")).isPresent();
    }
}
```

## 7.3. Frontend Testing

```typescript
// Ví dụ: Jest + React Testing Library
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import StudentCard from './StudentCard';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
  },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('StudentCard', () => {
  const mockStudent: Student = {
    id: 1,
    name: 'Nguyen Van A',
    email: 'a@example.com',
    status: 'active',
    gamificationPoints: 150,
    badges: [{ id: 1, name: 'Top Performer' }],
  };

  it('renders student information correctly', () => {
    render(<StudentCard student={mockStudent} />, { wrapper });

    expect(screen.getByText('Nguyen Van A')).toBeInTheDocument();
    expect(screen.getByText('a@example.com')).toBeInTheDocument();
    expect(screen.getByText('150')).toBeInTheDocument();
    expect(screen.getByText('Top Performer')).toBeInTheDocument();
  });

  it('shows active badge for active student', () => {
    render(<StudentCard student={mockStudent} />, { wrapper });

    expect(screen.getByText('Đang học')).toBeInTheDocument();
  });

  it('calls onEdit when edit button clicked', () => {
    const onEdit = jest.fn();
    render(<StudentCard student={mockStudent} onEdit={onEdit} />, { wrapper });

    fireEvent.click(screen.getByText('Chỉnh sửa'));

    expect(onEdit).toHaveBeenCalledWith(mockStudent);
  });
});

// Ví dụ: Cypress E2E Test
// cypress/e2e/attendance.cy.ts
describe('Attendance Flow', () => {
  beforeEach(() => {
    cy.login('teacher@example.com', 'password');
    cy.visit('/teacher/attendance');
  });

  it('should mark attendance successfully', () => {
    // Select class
    cy.get('[data-testid="class-select"]').click();
    cy.contains('Toán 10A').click();

    // Mark students present
    cy.get('[data-testid="student-checkbox"]').first().check();
    cy.get('[data-testid="student-checkbox"]').eq(1).check();

    // Submit
    cy.get('[data-testid="submit-attendance"]').click();

    // Verify success
    cy.contains('Điểm danh thành công').should('be.visible');
  });
});
```

---

# 8. SOFT SKILLS & PROCESSES

## 8.1. Agile/Scrum

| Concept | Kiến thức cần nắm |
|---------|-------------------|
| **Sprint** | 2-week iterations |
| **Ceremonies** | Planning, Daily, Review, Retro |
| **Artifacts** | Product Backlog, Sprint Backlog, Increment |
| **Estimation** | Story Points, Planning Poker |
| **Metrics** | Velocity, Burndown |

## 8.2. Git Workflow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            GIT FLOW                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  main ────●────────────────●────────────────●────────────────●─────►            │
│           │                ▲                ▲                ▲                   │
│           │                │                │                │                   │
│  develop ─┼──●──●──●──●────┼────●──●──●─────┼────●──●────────┼─────►            │
│           │  │             │    │           │    │           │                   │
│           │  │  feature/   │    │           │    │           │                   │
│           │  └──auth ──────┘    │           │    │           │                   │
│           │                     │           │    │           │                   │
│           │        feature/     │           │    │           │                   │
│           │        billing ─────┘           │    │           │                   │
│           │                                 │    │           │                   │
│           │                    hotfix/      │    │           │                   │
│           │                    critical ────┴────┘           │                   │
│           │                                                  │                   │
│           │                                      release/    │                   │
│           │                                      v1.0 ───────┘                   │
│                                                                                  │
│  Conventions:                                                                    │
│  • feature/ABC-123-short-description                                            │
│  • bugfix/ABC-456-fix-something                                                  │
│  • hotfix/critical-security-fix                                                 │
│  • release/v1.2.0                                                               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 8.3. Code Review

| Aspect | Checklist |
|--------|-----------|
| **Functionality** | Code does what it's supposed to |
| **Design** | Follows SOLID, DRY, KISS |
| **Readability** | Clear naming, good structure |
| **Security** | No vulnerabilities |
| **Performance** | No obvious bottlenecks |
| **Tests** | Adequate coverage |
| **Documentation** | Updated if needed |

## 8.4. Communication

| Skill | Importance |
|-------|------------|
| **Technical Writing** | Documentation, PR descriptions |
| **Verbal Communication** | Standups, Reviews |
| **Active Listening** | Requirements gathering |
| **Feedback** | Code review, Retros |

---

# 9. LEARNING ROADMAP

## 9.1. Roadmap theo Level

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         LEARNING ROADMAP                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  JUNIOR (0-2 years) - 3-6 tháng                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Java Core (OOP, Collections, Streams)                                         │
│  □ Spring Boot basics (REST API, JPA)                                            │
│  □ SQL fundamentals                                                              │
│  □ React/Next.js basics                                                          │
│  □ Git basics                                                                    │
│  □ Docker basics                                                                 │
│                                                                                  │
│  MID-LEVEL (2-4 years) - 3-6 tháng                                               │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ Spring Security, JWT                                                          │
│  □ Advanced JPA (N+1, caching)                                                   │
│  □ React Query, State management                                                 │
│  □ Testing (Unit, Integration)                                                   │
│  □ Docker Compose                                                                │
│  □ CI/CD basics                                                                  │
│                                                                                  │
│  SENIOR (4+ years) - 6-12 tháng                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  □ System Design                                                                 │
│  □ Microservices patterns                                                        │
│  □ Kubernetes                                                                    │
│  □ Performance optimization                                                      │
│  □ Security in depth                                                             │
│  □ Team leadership                                                               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 9.2. Recommended Learning Path

| Week | Backend Track | Frontend Track | DevOps Track |
|------|---------------|----------------|--------------|
| 1-2 | Java refresher | TypeScript deep-dive | Docker fundamentals |
| 3-4 | Spring Boot core | React hooks mastery | Docker Compose |
| 5-6 | Spring Data JPA | Next.js App Router | GitHub Actions |
| 7-8 | Spring Security | React Query | Kubernetes basics |
| 9-10 | API design | Form handling | Monitoring (Prometheus) |
| 11-12 | Testing | Testing | Cloud deployment |

---

# 10. TÀI LIỆU THAM KHẢO

## 10.1. Official Documentation

| Technology | URL |
|------------|-----|
| Spring Boot | https://docs.spring.io/spring-boot/docs/current/reference/html/ |
| Spring Security | https://docs.spring.io/spring-security/reference/ |
| Next.js | https://nextjs.org/docs |
| React | https://react.dev |
| PostgreSQL | https://www.postgresql.org/docs/ |
| Docker | https://docs.docker.com |
| Kubernetes | https://kubernetes.io/docs/ |

## 10.2. Recommended Books

| Book | Author | Topic |
|------|--------|-------|
| Effective Java | Joshua Bloch | Java best practices |
| Spring in Action | Craig Walls | Spring Framework |
| Clean Code | Robert Martin | Code quality |
| Designing Data-Intensive Applications | Martin Kleppmann | System design |
| The Pragmatic Programmer | Hunt & Thomas | Software craftsmanship |

## 10.3. Online Courses

| Platform | Course | Topic |
|----------|--------|-------|
| Udemy | Spring Boot Master Class | Spring Boot |
| Frontend Masters | Complete Intro to React | React |
| Pluralsight | Docker Deep Dive | Docker |
| A Cloud Guru | Kubernetes | Kubernetes |

## 10.4. Practice Platforms

| Platform | Purpose |
|----------|---------|
| LeetCode | Algorithm practice |
| HackerRank | Coding challenges |
| GitHub | Open source contribution |
| Codewars | Kata exercises |

---

*Tài liệu được tạo bởi: Claude Assistant*
*Ngày: 23/12/2025*
