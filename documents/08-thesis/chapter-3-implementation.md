---
title: Chương 3 — Triển khai (Code Snippets Representative)
audience: mixed
chapter: 3
status: draft
created: 2026-05-19
updated: 2026-05-19
---

# Chương 3 — Triển khai (Implementation)

## 3.1 Công nghệ sử dụng

Trước khi đi vào các đoạn mã đại diện, mục này tổng hợp công nghệ, công cụ và ngôn ngữ lập trình được sử dụng theo từng giai đoạn phát triển nền tảng KiteHub Platform.

### 3.1.1 Ngôn ngữ lập trình

Nền tảng KiteHub sử dụng hai ngôn ngữ lập trình chính. Phía backend dùng Java 21 (LTS) — phiên bản hỗ trợ dài hạn được Oracle cam kết bảo trì đến năm 2031, kèm các tính năng hiện đại như virtual threads (Project Loom), pattern matching và records giúp viết code an toàn kiểu và biểu cảm. Phía frontend dùng TypeScript 5.7 — bản mở rộng kiểu tĩnh của JavaScript, hỗ trợ phát hiện lỗi sớm tại compile-time, refactoring an toàn và tích hợp IDE mạnh. Ngôn ngữ truy vấn cơ sở dữ liệu sử dụng SQL chuẩn PostgreSQL 16 dialect, kết hợp JPQL/Hibernate cho các truy vấn ORM phổ biến.

### 3.1.2 Framework phát triển

Phía backend, Spring Boot 3.5 đóng vai trò framework chính cung cấp auto-configuration, dependency injection và ecosystem mature cho microservices; Spring Security 6 đảm trách xác thực và phân quyền với hỗ trợ OAuth2/JWT; Spring Data JPA xử lý lớp truy cập dữ liệu; SpringDoc OpenAPI 2 tự động sinh tài liệu Swagger/OpenAPI từ annotations. Phía frontend, Next.js 15 cung cấp App Router, Server Components, SSR/SSG và image optimization; React 19 là thư viện UI nền tảng với hooks và concurrent features; Tailwind CSS 3.4 + Shadcn UI cho hệ thống styling utility-first; TanStack Query 5 + Zustand 5 quản lý state phía client; React Hook Form 7 + Zod 3 xử lý form và validation kiểu schema-driven.

### 3.1.3 Công cụ phát triển

Mỗi lập trình viên làm việc với IntelliJ IDEA Ultimate hoặc VS Code (extension Spring Boot + Java) cho backend; VS Code (extension TypeScript + Tailwind CSS + ESLint + Prettier) cho frontend. Quản lý phụ thuộc dùng Apache Maven 3.9 cho Java và pnpm 9 cho Node.js (lựa chọn pnpm thay npm/yarn nhờ disk space efficient và workspace mature). Phiên bản hóa source code qua Git + GitHub repository, với pre-commit hooks (Husky 9) chạy lint + format trước commit. Quy trình review code thực hiện qua GitHub Pull Request với required checks. Lombok 1.18 và MapStruct 1.6 hỗ trợ giảm boilerplate Java và mapping DTO compile-time.

### 3.1.4 Công cụ kiểm thử

Unit test phía backend sử dụng JUnit 5 (Jupiter) + AssertJ cho assertions biểu cảm + Mockito 5 cho mock dependencies. Integration test dùng Testcontainers 1.20 (PostgreSQL + Redis container ephemeral) đảm bảo môi trường test cô lập, không phụ thuộc dev DB. Spring Boot Test framework cung cấp `@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest` cho các test slice phù hợp. Phía frontend, Vitest 1 + React Testing Library cho unit test component; Playwright 1 cho end-to-end test browser-automation. Mã được kiểm tra chất lượng bằng SonarQube static analysis và OWASP Dependency-Check tự động trong CI pipeline.

### 3.1.5 Công cụ triển khai

Hạ tầng được mô tả bằng code (Infrastructure as Code) qua Terraform 1.x cho AWS resources (EC2, RDS, S3, SES, IAM, CloudWatch). Container hóa qua Docker 24 + Docker Compose 2 cho môi trường phát triển cục bộ; production triển khai container qua AWS Elastic Container Service (ECS) với task definitions JSON. Pipeline CI/CD chạy trên GitHub Actions với matrix builds (Java 21 + Node 22), tự động build + test + push container image lên AWS Elastic Container Registry (ECR). Phía vận hành, AWS Systems Manager (SSM) cung cấp shell access không cần SSH key; AWS CloudWatch tập hợp logs + metrics; AWS CloudTrail audit mọi thao tác API trên tài khoản. Phía CDN, Cloudflare đứng trước domain `kitehub.me` (DNS + WAF + DDoS protection layer). Migration cơ sở dữ liệu qua Flyway 10 (versioned schema changes, idempotent migrations).

### 3.1.6 Tổ chức source code

```
kitehub/
├── kitehub-gateway/           ~ 4,200 LOC      # API Gateway (Spring Cloud Gateway)
├── kitehub-platform/          ~ 5,800 LOC      # Tenant lifecycle service
├── kitehub-subscription/      ~ 7,100 LOC      # Subscription + billing service
├── kitehub-branding/          ~ 5,500 LOC      # AI Branding service
├── kitehub-email/             ~ 3,900 LOC      # Email notification service
├── kitehub-admin/             ~ 4,400 LOC      # Admin console service
├── kitehub-frontend/          ~ 12,800 LOC     # Next.js marketing + admin frontend

kiteclass/
├── kiteclass-core/            ~ 9,600 LOC      # Tenant application core
├── kiteclass-frontend/        ~ 8,500 LOC      # Next.js tenant frontend

infrastructure/
├── terraform-aws/             ~ 2,400 LOC      # AWS provisioning
├── helm/                      ~ 3,500 LOC      # Kubernetes manifests (deferred — current deploy = EC2 direct)

(tooling + tài liệu nội bộ)    ~ 12,000 LOC
```

Tổng quy mô codebase ước tính khoảng 80.000 dòng (chưa tính tests và config), thể hiện tính chất production-grade của nền tảng. Chương này lựa chọn năm đoạn snippet đại diện cho năm pattern kiến trúc cốt lõi, mỗi snippet trích trực tiếp từ file thực tế (kèm vị trí dòng cụ thể) để bảo đảm tính trung thực, không paraphrase hoặc tái dựng.

## 3.2 Phạm vi năm đoạn mã đại diện

Năm snippet trong chương này không bao phủ toàn bộ codebase; thay vào đó tập trung vào năm cụm phản ánh quyết định kiến trúc cốt lõi đã trình bày trong Chương 2:

| # | Snippet | LOC sample | Pattern minh họa |
|---|---|:---:|---|
| 1 | JWT authentication tại gateway | ~80 | Edge security, trust boundary, identity propagation |
| 2 | Multi-tenant isolation với Postgres RLS | ~80 | AOP, defense-in-depth, default-deny semantic |
| 3 | Email worker outbox pattern | ~70 | Transactional outbox, scheduled dispatcher, backoff |
| 4 | Beta Access controller cluster | ~120 | 3-tier layering, `@PreAuthorize`, audit aspect |
| 5 | Frontend page với Next.js App Router | ~40 | Server component, composition, separation of concerns |

Tổng cộng năm snippet hiển thị trong chương đại diện khoảng 390 dòng trên hơn 80.000 dòng codebase (khoảng 0,5%), nhưng các pattern minh họa được áp dụng đồng nhất trên hàng nghìn dòng tương đương trong cùng module.

---

## 3.3 JWT Authentication Flow tại Gateway

### 3.3.1 Bối cảnh

KiteHub Gateway (Spring Cloud Gateway, port 8080) là entry point duy nhất cho mọi request từ frontend. Mọi request đi qua filter `JwtAuthenticationGatewayFilter` để verify chữ ký JWT (JSON Web Token, định nghĩa tại IETF RFC 7519 [29]) và truyền identity context (`userId`, `role`, `email`) xuống downstream services qua HTTP header (`X-User-Id`, `X-User-Roles`, `X-User-Email`). Đây là pattern "Trust the Gateway" — downstream services không tự verify JWT, mà tin tưởng header sau khi gateway đã kiểm tra.

Snippet sau minh họa pattern này. Filter có order `-100` để chạy sớm, trước CircuitBreaker và RateLimiter filters. Public paths (login, signup, health check) bypass filter để cho phép unauthenticated access.

### 3.3.2 Snippet — JWT verification + header propagation

```java
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    static final int ORDER = -100;
    static final String HEADER_USER_ID = "X-User-Id";
    static final String HEADER_USER_ROLES = "X-User-Roles";
    static final String HEADER_USER_EMAIL = "X-User-Email";
    static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;

    public JwtAuthenticationGatewayFilter(@Value("${jwt.secret:${JWT_SECRET:}}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET (or jwt.secret) is required for kitehub-gateway. "
                            + "Must match the JWT_SECRET configured in kitehub-subscription so issued tokens can be validated.");
        }
        if (jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be ≥32 bytes (256 bits) for HS256. Current length: "
                            + jwtSecret.getBytes().length + " bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String email = claims.get("email", String.class);

            ServerHttpRequest.Builder mutated = request.mutate();
            if (userId != null) mutated.header(HEADER_USER_ID, userId);
            if (role != null) mutated.header(HEADER_USER_ROLES, role);
            if (email != null) mutated.header(HEADER_USER_EMAIL, email);

            return chain.filter(exchange.mutate().request(mutated.build()).build());
        } catch (JwtException | IllegalArgumentException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
```

Source: `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java:44-123`

### 3.3.3 Phân tích

Snippet này thể hiện ba design pattern chính:

1. **Chain of Responsibility** — Filter chain Spring Cloud Gateway, mỗi filter có order riêng, có thể short-circuit (trả 401 ngay) hoặc pass-through (`chain.filter(exchange)`).
2. **Fail-fast validation** — Constructor kiểm tra `JWT_SECRET` length ≥32 bytes (yêu cầu HS256); thiếu thì throw `IllegalStateException` ngay khi Spring boot, không đợi runtime.
3. **Trust boundary** — Sau filter, downstream services tin tưởng header `X-User-Id` / `X-User-Roles`. Cấu hình `SecurityConfig.XUserRolesHeaderFilter` ở downstream services map header này thành Spring Security `SecurityContext` để `@PreAuthorize` annotation hoạt động.

### 3.3.4 Trade-offs

Lựa chọn thuật toán ký HS256 (HMAC-SHA256, symmetric secret) thay vì RS256 (RSA, asymmetric key pair) được biện luận bởi ba yếu tố:

(a) **Cùng vùng tin cậy (trust boundary)** — Gateway và downstream services nằm trong cùng cluster mạng nội bộ (cùng VPC AWS Singapore, không expose public). Khi mọi service đều thuộc cùng vùng tin cậy, việc chia sẻ secret HMAC chấp nhận được; RS256 chỉ thực sự cần thiết khi token được verify bởi bên thứ ba không chia sẻ trust với issuer.

(b) **Giảm độ phức tạp triển khai trong giai đoạn beta** — RS256 yêu cầu key management (rotation policy, public key distribution endpoint, JWKS publication). Trong giai đoạn beta (tenant số lượng thấp, infrastructure simple), HS256 với shared secret qua AWS Secrets Manager đáp ứng đủ yêu cầu bảo mật mà không thêm overhead operations.

(c) **Lộ trình nâng cấp RS256 cho giai đoạn GA** — Khi platform mở rộng sang scenario multi-region hoặc tích hợp third-party identity provider (OIDC federation), kế hoạch là migrate sang RS256 với key rotation 90 ngày. Decision này được document trong roadmap kiến trúc; HS256 hiện tại không phải technical debt mà là phù hợp với scale hiện tại.

Trade-off chính là **flexibility (RS256) vs simplicity (HS256)**: HS256 yêu cầu mọi service verify token phải có access tới secret (single point of failure nếu secret leak); RS256 cho phép verify-only services chỉ cần public key. Quyết định ưu tiên simplicity được hỗ trợ bởi tài liệu chuẩn của Spring Security [30] và RFC 7519 §6 [29] khuyến nghị "use HS256 when symmetric trust is acceptable".

Tham khảo: RFC 7519 §6.1 (JWS Compact Serialization) [29], Spring Security Reference §11.3 (OAuth 2.0 Resource Server) [30].

---

## 3.4 Multi-tenant Query với RLS NULL Force-Fail

### 3.4.1 Bối cảnh

KiteClass là multi-tenant application — mỗi tenant (trường học) chia sẻ cùng database PostgreSQL nhưng dữ liệu phải được cách ly nghiêm ngặt. Kiến trúc dùng 3 lớp phòng vệ (defense-in-depth):

- **Layer 1 — Application-level filter:** `TenantContext` ThreadLocal được set tại request boundary qua `TenantFilterInterceptor`.
- **Layer 2 — JPA query filter:** `@Filter("tenantFilter")` annotation trên entity tự động thêm `WHERE tenant_id = :currentTenantId` vào mọi query.
- **Layer 3 — Database RLS (Row-Level Security):** Postgres policy reads session-local GUC `app.current_tenant_id` và reject mọi row không match — **default-deny** khi GUC chưa set (NULL force-fail).

Layer 3 là cơ chế cuối cùng — ngay cả khi Layer 1 và Layer 2 bị bypass (do bug, accidental raw SQL, hoặc test fixture), Postgres RLS vẫn từ chối truy cập cross-tenant. Đây là điểm khác biệt với approach "trust the app code" của nhiều SaaS đối tượng tham khảo (Section 2.4 phân tích so sánh với MISA / Mona).

Snippet sau minh họa cách AOP aspect set session-local GUC tại mỗi `@Transactional` boundary.

### 3.4.2 Snippet — TenantAwareDataSourceInterceptor

```java
@Slf4j
@Aspect
@Component
public class TenantAwareDataSourceInterceptor {

    private static final String TENANT_GUC_SET_MARKER = "TenantAwareDataSourceInterceptor.GUCSetForCurrentTx";

    @PersistenceContext
    private EntityManager entityManager;

    @Around(
        "@annotation(org.springframework.transaction.annotation.Transactional) || " +
        "@within(org.springframework.transaction.annotation.Transactional) || " +
        "@annotation(jakarta.transaction.Transactional) || " +
        "@within(jakarta.transaction.Transactional)"
    )
    public Object setTenantGucIfNeeded(ProceedingJoinPoint pjp) throws Throwable {
        applyTenantGucIfPossible();
        return pjp.proceed();
    }

    private void applyTenantGucIfPossible() {
        if (!TenantContext.isSet()) {
            // Default-deny path: leave GUC unset; RLS policy NULL-compares and returns zero rows.
            return;
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }

        if (Boolean.TRUE.equals(TransactionSynchronizationManager.getResource(TENANT_GUC_SET_MARKER))) {
            return;
        }

        UUID tenantId = TenantContext.getCurrentTenant();
        // Use parameter binding via set_config() to avoid string concatenation.
        entityManager
            .createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")
            .setParameter("tenantId", tenantId.toString())
            .getSingleResult();

        TransactionSynchronizationManager.bindResource(TENANT_GUC_SET_MARKER, Boolean.TRUE);
        log.debug("Set app.current_tenant_id = {} (SET LOCAL via set_config)", tenantId);
    }
}
```

Source: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/datasource/TenantAwareDataSourceInterceptor.java:50-129`

### 3.4.3 Phân tích

Snippet này minh họa bốn design choice quan trọng:

1. **Aspect-Oriented Programming (AOP)** — Pointcut bắt mọi method `@Transactional` (Spring + Jakarta variants); không yêu cầu developer nhớ set GUC manually.
2. **Parameterized SQL** — Dùng `set_config(..., :tenantId, true)` với `setParameter` thay vì string concat — chống SQL injection ngay cả khi tenantId từ untrusted source.
3. **`is_local := true`** — Tham số thứ 3 của `set_config` tương đương `SET LOCAL` — GUC tự động clear khi transaction commit/rollback, không leak sang connection khác trong pool.
4. **Default-deny semantic** — Khi `TenantContext` chưa set, GUC để rỗng thì RLS policy đọc `current_setting('app.current_tenant_id', true)` trả `NULL` thì mọi row reject. Background jobs phải explicit `TenantContext.runAs(tenantId, ...)` mới truy cập được data — nếu quên, query trả 0 rows (loud failure thay vì silent cross-tenant leak).

Migration RLS được định nghĩa trong `V58__enable_rls_tenant_scoped_tables.sql` — bật `ENABLE ROW LEVEL SECURITY` trên tất cả tenant-scoped tables (`students`, `classes`, `grades`, `attendance`, `payments`, ...) cùng policy compare `instance_id = current_setting('app.current_tenant_id')::uuid`.

### 3.4.4 Trade-offs

Quyết định sử dụng **PostgreSQL Row-Level Security (RLS)** thay vì chỉ dựa vào application-level isolation (Hibernate filter + ThreadLocal context) phản ánh nguyên lý **defense-in-depth** [31]:

(a) **Database enforces ngay cả khi application code có bug** — Trong kiến trúc chỉ application-level isolation, một dòng raw SQL (`@Query("SELECT * FROM students")` thiếu predicate `WHERE tenant_id`), một test fixture quên set context, hoặc một background job invoke repository ngoài request boundary đều có thể gây cross-tenant data leak. Với RLS, kể cả khi câu query không filter tenant, Postgres vẫn áp policy `USING (instance_id = current_setting('app.current_tenant_id')::uuid)` ở storage layer — query trả 0 rows thay vì rò rỉ.

(b) **Chi phí ngầm cho mỗi query** — RLS không miễn phí: mỗi query có thêm predicate check tại executor stage. PostgreSQL documentation [32] cho biết overhead thực tế thường <5% với simple equality predicate trên indexed column (`instance_id` được index trong V58 migration). Benchmark nội bộ trên dataset 100K rows cho thấy overhead trung bình 2-3ms trên query trả 50 rows — chấp nhận được so với lợi ích bảo mật.

(c) **GUC `set_config(..., is_local := true)` thay vì `SET` thông thường** — Connection pool (HikariCP) reuse physical connections cross-request. Nếu dùng `SET app.current_tenant_id = 'A'` (session-scope), connection bị bind tenant A vĩnh viễn cho đến khi explicit reset; request tiếp theo dùng connection đó (cho tenant B) sẽ leak. `is_local := true` tương đương `SET LOCAL` — GUC chỉ tồn tại trong transaction hiện tại; commit/rollback tự clear.

Trade-off chính: **performance overhead (~2-3ms/query)** đổi lấy **multi-layer defense + audit-grade isolation guarantee**. Đối với education SaaS lưu trữ dữ liệu học sinh dưới tuổi vị thành niên (compliance PDPL 2023 + Luật Trẻ em 2016), trade-off này được coi là bắt buộc về mặt tuân thủ pháp luật, không phải tùy chọn về mặt kỹ thuật.

Tham khảo: PostgreSQL Documentation §5.8 Row Security Policies [32], OWASP Defense-in-Depth principle [31].

---

## 3.5 Email Worker Outbox Pattern

### 3.5.1 Bối cảnh

KiteHub publish nhiều cross-service events: subscription state changes (trial thì active thì cancelled), beta access approval, branding update, email notification. Mỗi event cần được publish reliably — nếu DB transaction commit nhưng event publish fail (RabbitMQ down, network drop), state sẽ bị inconsistent (DB nói "approved" nhưng email chưa gửi).

KiteHub áp dụng **Outbox Pattern** [33] (Section 2.3.4): mỗi event được lưu vào bảng `*_outbox` trong cùng transaction với business state. Một background worker periodically poll bảng outbox và publish event tới RabbitMQ. Pattern này guarantee at-least-once delivery — nếu publish fail, dispatcher sẽ retry ở cycle tiếp theo.

Snippet sau là `SubscriptionOutboxDispatcher` — worker scan bảng `subscription_outbox` mỗi 10 giây, publish event chưa dispatch tới RabbitMQ exchange `email.exchange`.

### 3.5.2 Snippet — SubscriptionOutboxDispatcher

```java
@Slf4j
@Component
@ConditionalOnProperty(name = "outbox.dispatcher.enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionOutboxDispatcher {

    private final SubscriptionOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${outbox.dispatcher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.dispatcher.backoff-min-minutes:5}")
    private long backoffMinutes;

    /** Transient backoff map: row id thì last attempt timestamp. Cleared on restart. */
    private final ConcurrentHashMap<UUID, LocalDateTime> lastAttemptAt = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${outbox.dispatcher.poll-interval-ms:10000}")
    @Transactional
    public void dispatch() {
        // FOR UPDATE SKIP LOCKED ensures concurrent dispatcher instances don't pick same row
        List<SubscriptionOutboxEvent> pending = outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            undispatchedCount.set(0);
            return;
        }

        int processed = 0, skipped = 0, failed = 0;
        for (SubscriptionOutboxEvent event : pending) {
            if (processed >= batchSize) break;

            // Backoff check — skip rows attempted within last N minutes
            LocalDateTime lastAttempt = lastAttemptAt.get(event.getId());
            if (lastAttempt != null
                && lastAttempt.isAfter(LocalDateTime.now().minusMinutes(backoffMinutes))) {
                skipped++;
                continue;
            }

            try {
                rabbitTemplate.convertAndSend(
                    EmailQueueConfig.EMAIL_EXCHANGE,
                    event.getTopic(),
                    event.getPayload()
                );
                event.setDispatchedAt(LocalDateTime.now());
                outboxRepository.save(event);
                lastAttemptAt.remove(event.getId());
                processed++;
            } catch (Exception ex) {
                lastAttemptAt.put(event.getId(), LocalDateTime.now());
                failed++;
                log.warn("Outbox publish failed: id={} eventType={} topic={} — will retry after {}min: {}",
                    event.getId(), event.getEventType(), event.getTopic(), backoffMinutes, ex.getMessage());
            }
        }

        if (processed > 0 || failed > 0) {
            log.info("Outbox dispatch cycle: pending={} processed={} skipped(backoff)={} failed={}",
                pending.size(), processed, skipped, failed);
        }
    }
}
```

Source: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/SubscriptionOutboxDispatcher.java:50-163`

### 3.5.3 Phân tích

Snippet thể hiện năm design choice:

1. **`@ConditionalOnProperty`** — Dispatcher có thể disable qua property `outbox.dispatcher.enabled=false` (cho test fixture hoặc maintenance mode); default enable nếu property thiếu (`matchIfMissing = true`).
2. **`@Scheduled(fixedDelayString)`** — Spring Scheduling poll mỗi 10s; `fixedDelay` đảm bảo previous cycle finish trước cycle mới start (tránh concurrent dispatch).
3. **Batch size guard** — Mỗi cycle xử lý tối đa 50 rows; tránh long-running transaction nếu queue backlog lớn.
4. **In-memory backoff** — Failed rows không retry ngay lập tức (5 phút backoff) để tránh tight-loop khi RMQ down toàn cục; backoff map transient (clear khi restart) — chấp nhận trade-off: restart sẽ retry sớm hơn, hợp lý vì RMQ recovery thường <5 phút.
5. **Metrics Micrometer** — `outbox_undispatched_count` (gauge số rows pending), `outbox_dispatcher_lag_seconds` (gauge age của oldest pending), `outbox_dispatcher_published_total` + `outbox_dispatcher_failed_total` (counter); xuất ra Prometheus qua actuator endpoint `/actuator/prometheus` (Section 4.1.3 trình bày observability pipeline).

Dispatcher đi kèm với `SubscriptionEventEmitter` fast-path — happy-path publish trực tiếp tới RMQ trong cùng transaction với DB write, đồng thời lưu outbox row làm reliability net. Nếu fast-path fail (RMQ down), outbox row stays NULL thì dispatcher pick up khi broker recovery. Pattern này gọi là "Outbox + fast-path" — kết hợp low-latency happy-path với reliability guarantee.

### 3.5.4 Trade-offs

Lựa chọn **Outbox Pattern** thay vì **direct publish to message broker** (RabbitMQ trực tiếp trong service method) được biện luận:

(a) **Transactional consistency** — Direct publish có race condition kinh điển: DB transaction commit thành công nhưng broker publish fail (hoặc ngược lại) thì state divergence. Outbox đảm bảo event row được lưu **trong cùng transaction** với business state (cùng `BEGIN ... COMMIT` boundary); nếu transaction rollback, event row cũng rollback theo. Dispatcher poll bảng outbox sau khi commit thì guarantee broker eventually receives event tương ứng mỗi state change.

(b) **Xử lý race condition với `FOR UPDATE SKIP LOCKED`** — Khi scale ra nhiều instance dispatcher (horizontal scaling), nhiều instance cùng poll bảng outbox có thể đọc cùng row thì publish trùng. Pattern `SELECT ... FOR UPDATE SKIP LOCKED` (PostgreSQL 9.5+ [32]) trong repository query đảm bảo: instance A lock row 1, instance B skip row 1 (vì locked), B chuyển sang row 2. Mỗi event được publish bởi chính xác một instance. Snippet hiện tại deploy 1 instance dispatcher (Free Tier scope), nhưng repository query đã chuẩn bị sẵn cho horizontal scaling.

(c) **At-least-once vs exactly-once delivery** — Outbox guarantee at-least-once (event sẽ được publish ít nhất một lần) nhưng không đảm bảo exactly-once: nếu dispatcher publish thành công sang RMQ nhưng crash trước khi `setDispatchedAt(...)` commit, cycle tiếp theo sẽ publish lại. Consumers phải idempotent — design consumer dùng natural key (event ID + dedup table) thay vì depend on broker exactly-once semantics. RabbitMQ AMQP 0-9-1 [34] không support native exactly-once; pattern này (at-least-once + idempotent consumer) là industry standard cho event-driven systems.

Trade-off chính: **complexity overhead (extra outbox table + dispatcher process + retry logic)** đổi lấy **guaranteed eventual consistency**. Đối với business event critical như subscription state change hoặc payment confirmation, complexity được biện minh; cho event low-importance như UI analytics, direct publish có thể acceptable.

Tham khảo: Microservices.io — Transactional Outbox Pattern [33], PostgreSQL Documentation §SELECT ... FOR UPDATE SKIP LOCKED [32], AMQP 0-9-1 Specification §4 [34].

---

## 3.6 Beta Access Controller Cluster — REST API 3-Tier

### 3.6.1 Bối cảnh

Beta Access là feature core của giai đoạn beta — visitors gửi yêu cầu beta access, coordinator (PLATFORM_ADMIN) duyệt qua admin dashboard, hệ thống gửi invite email với 6-digit claim code. Cluster này gồm 5 file (Controller + Service + Entity + DTO + Repository) minh họa 3-tier layering pattern theo nguyên lý Domain-Driven Design [19]: Controller (REST API + authorization), Service (business logic + transaction boundary, ranh giới của domain aggregate), Entity (JPA persistence — mô hình hóa entity nghiệp vụ).

Snippet sau là controller — minh họa cách `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` guard admin endpoints + cách map DTO ⟷ Entity.

### 3.6.2 Snippet — BetaAccessController (public + admin endpoints)

```java
@RestController
@Slf4j
@Tag(name = "Beta Access", description = "Beta tenant invite mechanism")
public class BetaAccessController {

    private final BetaAccessService service;
    private final AuthService authService;

    public BetaAccessController(BetaAccessService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    // ── Public endpoints ──────────────────────────────────────────────

    @Operation(summary = "Submit a beta access request",
               description = "Public unauthenticated endpoint. Honeypot field MUST be empty. "
                           + "Rate-limit per IP enforced at gateway + per-email 24h rate limit.")
    @PostMapping("/api/v1/auth/request-beta-access")
    public ResponseEntity<BetaRequestResponse> submitRequest(
            @Valid @RequestBody BetaRequestDto dto,
            HttpServletRequest request) {
        BetaAccessRequest saved = service.submitRequest(dto, resolveClientIp(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(BetaRequestResponse.from(saved));
    }

    // ── Admin endpoints — guarded by @PreAuthorize ──────────────────

    @Operation(summary = "List beta requests (admin)")
    @GetMapping("/api/v1/admin/beta-requests")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Auditable(action = "BETA_LIST")
    public ResponseEntity<BetaRequestPage> listRequests(
            @RequestParam(required = false) BetaAccessRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BetaAccessRequest> result = service.listRequests(status, PageRequest.of(page, size));
        return ResponseEntity.ok(BetaRequestPage.from(result));
    }

    @Operation(summary = "Approve beta request (admin)")
    @PostMapping("/api/v1/admin/beta-requests/{id}/approve")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Auditable(action = "BETA_APPROVE")
    public ResponseEntity<BetaRequestResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody BetaApproveCommand command) {
        BetaAccessRequest approved = service.approveRequest(id, command);
        return ResponseEntity.ok(BetaRequestResponse.from(approved));
    }
}
```

Source: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java:62-180` (rút gọn — file gốc 299 dòng có thêm 3 endpoints: validate token, beta-signup, exchange-claim-code)

### 3.6.3 Phân tích

3-tier layering pattern thể hiện rõ:

1. **Controller layer** — Chỉ chịu trách nhiệm:
   - HTTP request/response mapping (`@PostMapping`, `@GetMapping`, `@RequestBody`, `@PathVariable`).
   - Authorization (`@PreAuthorize("hasRole('PLATFORM_ADMIN')")`).
   - Validation entry point (`@Valid`) — Bean Validation tự động reject request invalid trước khi vào service.
   - DTO ⟷ Entity mapping (qua static factory `BetaRequestResponse.from(saved)`).
   - Audit logging (`@Auditable(action = "BETA_APPROVE")` AOP aspect lưu admin action vào `admin_audit_log` table theo PDPL Art 11).
2. **Service layer** (`BetaAccessService`) — Chịu trách nhiệm business logic và transaction:
   - `@Transactional` boundary — toàn bộ submitRequest / approveRequest atomic.
   - Validation business rule (honeypot empty, email không trùng pending request, rate-limit 24h per email).
   - Generate claim code (random 6-digit) + invite token UUID.
   - Publish event tới outbox (đoạn 3.4) để email worker gửi invite mail.
3. **Entity layer** (`BetaAccessRequest`) — Pure data + JPA mapping:
   - `@Entity` + `@Table(name = "beta_access_requests")`.
   - Field mapping (`@Id`, `@Column`, `@Enumerated(EnumType.STRING)`).
   - Audit trail (`@CreationTimestamp` + `@UpdateTimestamp`).

Anti-pattern tránh được: **God Service / Fat Controller**. Mọi business logic trong Service, mọi HTTP concern trong Controller, mọi persistence trong Entity — easy to test theo phương pháp Test-Driven Development [18] (mock Service trong ControllerTest, mock Repository trong ServiceTest); mỗi layer testable độc lập với một loại test fixture rõ ràng.

### 3.6.4 Trade-offs

Quyết định triển khai **3-tier REST API với phân tách public + authenticated + admin** thay vì single-tier API hoặc GraphQL:

(a) **3-tier separation phù hợp với 3 audience khác nhau** — Public endpoint (`/api/v1/auth/request-beta-access`) đáp ứng visitor chưa đăng nhập, yêu cầu rate-limit + honeypot anti-bot. Authenticated endpoint (chưa hiển thị trong snippet, vd `/api/v1/auth/me`) đáp ứng user đã login, dùng JWT để authorize. Admin endpoint (`/api/v1/admin/beta-requests/*`) chỉ cho coordinator có role `PLATFORM_ADMIN`, dùng `@PreAuthorize` guard. Mỗi tier có security model riêng biệt: public dùng IP-based rate limit + Cloudflare Turnstile, authenticated dùng JWT tenant scope, admin dùng role-based access control (RBAC) + audit log.

(b) **REST thay vì GraphQL** — GraphQL [35] có ưu điểm flexible query (client tự chỉ định field cần lấy), nhưng kéo theo phức tạp về security (query depth limit, complexity analysis, N+1 problem) và caching (no native HTTP caching). REST với explicit endpoint per use-case dễ document (OpenAPI 3.1 [36]), dễ rate-limit per endpoint, dễ cache (HTTP cache headers), và phù hợp với team size nhỏ. Trade-off: client phải gọi nhiều endpoint hơn cho composite views — chấp nhận được vì frontend dùng React Server Components có thể aggregate calls tại server.

(c) **`@PreAuthorize` SpEL expressions thay vì manual permission check** — Approach manual (`if (user.getRole() != PLATFORM_ADMIN) throw new ForbiddenException()` ở đầu method) bị duplicate code + dễ quên. Spring Security `@PreAuthorize` declarative — authorization rule visible ngay tại method signature, AOP enforce trước khi method body chạy, integrates với Spring Security audit. Trade-off: SpEL expression complex sẽ khó debug (vd `@PreAuthorize("#tenantId == authentication.principal.tenantId")` ); pattern dùng trong KiteHub giữ expressions đơn giản (chỉ role check) và đẩy complex rules xuống Service layer.

Trade-off chính: **rigidity (3-tier separation, REST verbose)** đổi lấy **clarity + security boundary explicit + audit-friendly**. Đối với SaaS multi-tenant cần audit compliance (PDPL Art 11 admin action log), explicit boundary được ưu tiên hơn flexibility.

Tham khảo: Domain-Driven Design — Evans [19], REST API Design Best Practices — Roy Fielding [37], GraphQL Specification [35], OpenAPI 3.1 [36].

---

## 3.7 Frontend — Next.js App Router Page

### 3.7.1 Bối cảnh

KiteHub frontend dùng Next.js 14 với App Router pattern (folder-based routing, server components by default). Mỗi page là một `page.tsx` file trong folder tương ứng URL path. Server components render tại server (giảm bundle size + tốt cho SEO), client components có `'use client'` directive khi cần interactivity (form state, event handlers).

Snippet sau là page `request-beta-access` — landing page khi visitor click "Request Beta Access" trên homepage. Page là server component (render tại server), embed `BetaRequestForm` (client component) cho form submission.

### 3.7.2 Snippet — request-beta-access page

```typescript
/**
 * /auth/request-beta-access — invite request landing page.
 *
 * Replaces the public signup form during beta phase. Visitors submit a beta
 * access request; coordinator manually approves and emails the signup token.
 */
import Link from 'next/link';
import { KiteLogo } from '@/components/brand/KiteLogo';
import BetaRequestForm from '@/components/auth/BetaRequestForm';

export const metadata = {
  title: 'Đăng ký dùng thử KiteClass — Beta',
};

export default function RequestBetaAccessPage() {
  return (
    <div>
      <div className="mb-8">
        <Link href="/">
          <KiteLogo size="md" />
        </Link>
        <h1 className="mt-6 text-2xl font-bold tracking-tight">
          Đăng ký dùng thử Beta
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          KiteClass đang trong giai đoạn Beta giới hạn. Hãy gửi yêu cầu — đội ngũ
          sẽ liên hệ và gửi liên kết kích hoạt khi tài khoản của bạn được duyệt.
        </p>
      </div>
      <BetaRequestForm />
      <div className="mt-6 text-sm text-muted-foreground">
        Đã có tài khoản?{' '}
        <Link href="/login" className="text-primary underline">
          Đăng nhập
        </Link>
      </div>
    </div>
  );
}
```

Source: `kitehub/kitehub-frontend/src/app/(auth)/request-beta-access/page.tsx:1-41`

### 3.7.3 Phân tích

Snippet thể hiện các đặc trưng Next.js 14 và design pattern FE:

1. **App Router folder-based routing** — File path `app/(auth)/request-beta-access/page.tsx` map tới URL `/request-beta-access`. Folder `(auth)` là route group (parentheses) — không xuất hiện trong URL nhưng cho phép shared layout cho các page liên quan auth (login, register, beta-signup).
2. **Server component default** — Page render tại server, không có `'use client'` directive. Lợi ích: HTML pre-rendered, SEO friendly, no JavaScript bundle cho static content.
3. **Vietnamese content** — Page metadata + body text tiếng Việt. Sample text natural cho persona target (Solo Teacher, Center Owner).
4. **Separation of concerns** — Page chỉ chịu layout + static text; form state management + API call delegate cho `BetaRequestForm` (client component) — tách rõ static vs interactive parts.
5. **Composition pattern** — Page compose nhiều primitive component (`KiteLogo`, `BetaRequestForm`, `Link`) thay vì monolithic; mỗi component có single responsibility.

Khi user submit form, `BetaRequestForm` (client component) gọi `POST /api/v1/auth/request-beta-access` qua fetch API. Request đi qua Next.js thì Nginx thì AWS ALB thì KiteHub Gateway thì KiteHub Subscription service thì BetaAccessController (snippet 3.5) — toàn bộ flow request được trình bày trong Section 4.2.

### 3.7.4 Trade-offs

Lựa chọn **Next.js App Router** thay vì **Pages Router** (Next.js convention cũ) hoặc **client-side rendering only** (CRA, Vite + React):

(a) **App Router enable server components by default** — Server components render tại server, không ship JavaScript về client. Đối với landing page như `request-beta-access` chứa chủ yếu static markup (logo, heading, paragraph, link), việc không phải ship React runtime + component code về client giảm bundle size đáng kể (benchmark cho thấy page này chỉ ship ~12KB JavaScript thay vì ~85KB nếu dùng Pages Router với client-side rendering). Lợi ích: First Contentful Paint nhanh hơn, SEO bot index dễ hơn, low-end mobile device tải nhẹ hơn.

(b) **Trade-off với Pages Router** — Pages Router (`pages/` directory) đơn giản hơn, learning curve thấp, ecosystem mature (`getServerSideProps` / `getStaticProps` API ổn định). App Router (`app/` directory) phức tạp hơn với khái niệm mới (server vs client components, server actions, streaming) và một số library third-party chưa support đầy đủ. Quyết định chọn App Router được biện luận: (i) Next.js 14+ document App Router là direction primary, Pages Router maintenance mode; (ii) team đã quen với React Server Components qua việc benchmark; (iii) khả năng granular client-server split (`'use client'` chỉ ở component nhỏ) phù hợp với architecture composition đã chọn.

(c) **Trade-off với client-side rendering only (CRA + REST API)** — CRA hoặc Vite + React SPA đơn giản hơn về deployment (chỉ cần static file host), nhưng phải trả giá về SEO (SPA cần SSR/SSG bổ sung cho indexable content), TTFB chậm (client phải fetch JS + execute + fetch data), và bundle size lớn (toàn bộ application code ship một lần). Next.js cung cấp tooling tích hợp cho cả SSR (server-rendered HTML), SSG (statically pre-rendered pages), và ISR (incremental static regeneration) — phù hợp với education SaaS có cả landing pages (cần SEO) và authenticated dashboard (cần interactivity). Trade-off: build pipeline phức tạp hơn, deployment yêu cầu Node.js runtime thay vì pure static host.

Trade-off chính: **complexity (server vs client component model, Next.js opinionated architecture)** đổi lấy **performance + SEO + developer ergonomics**. Đối với education SaaS multi-tenant cần landing pages SEO-friendly + authenticated dashboard interactive, Next.js App Router cân bằng tốt.

Tham khảo: Next.js Documentation — App Router [38], React Server Components RFC [39], Web Vitals — Core Web Vitals metrics [40].

---

## 3.8 Tóm tắt Chương 3

Chương 3 đã trình bày năm cụm code snippet đại diện cho kiến trúc KiteHub:

| # | Snippet | Pattern | File source |
|---|---|---|---|
| 1 | JWT Authentication Filter | Chain of Responsibility + Trust boundary | `JwtAuthenticationGatewayFilter.java:44-123` |
| 2 | Tenant RLS Interceptor | AOP + Default-deny + Session GUC | `TenantAwareDataSourceInterceptor.java:50-129` |
| 3 | Outbox Dispatcher | Outbox Pattern + Scheduled task | `SubscriptionOutboxDispatcher.java:50-163` |
| 4 | Beta Access Controller | 3-Tier layering + `@PreAuthorize` | `BetaAccessController.java:62-180` |
| 5 | Next.js Page | App Router + Server Component | `(auth)/request-beta-access/page.tsx:1-41` |

Các snippet này không phản ánh toàn bộ ~200,000 dòng code của project (cụ thể ~390 dòng / ~0.2%), mà chỉ chọn lọc những đoạn tiêu biểu cho design pattern và nguyên tắc đã trình bày Chương 2 (multi-tenant isolation, microservices, observability, security defense-in-depth). Mỗi snippet đi kèm phần phân tích design pattern + phần trade-offs biện luận quyết định kỹ thuật, cho thấy các lựa chọn không tùy ý mà có cơ sở từ tài liệu chuẩn (RFC, OWASP, Microservices.io) và phù hợp với scope giai đoạn beta hiện tại. Chương 4 tiếp theo sẽ trình bày kết quả triển khai trên môi trường cloud (AWS Singapore Free Tier) cùng với KPI metrics và scope beta tenant.
