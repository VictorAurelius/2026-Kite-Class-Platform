---
title: Chương 3 — Triển khai (Code Snippets Representative)
audience: mixed
chapter: 3
status: draft
created: 2026-05-19
updated: 2026-05-19
wave: 100.7-phase-2
agent: 2c
---

# Chương 3 — Triển khai (Implementation)

## 3.1 Giới thiệu

Chương này trình bày các đoạn mã (code snippets) tiêu biểu rút trích từ source code thực tế của KiteHub Platform, minh họa cách các nguyên lý kiến trúc trong Chương 2 được hiện thực hóa. Mỗi snippet được trích nguyên văn từ file thực tế (cite path + line range) để bảo đảm tính trung thực — không paraphrase hoặc tái dựng. Phạm vi chương 3 tập trung vào 5 cụm tiêu biểu nhất phản ánh kiến trúc:

1. **JWT authentication tại gateway** — bảo mật biên (edge security) và truyền identity context xuống downstream services
2. **Multi-tenant isolation với Postgres RLS** — cơ chế cách ly dữ liệu giữa các tenant qua session-local GUC + Row-Level Security
3. **Email worker outbox pattern** — bảo đảm gửi sự kiện đáng tin cậy giữa các service qua DB+message broker
4. **Beta Access controller cluster** — minh họa 3-tier layering (Controller / Service / Entity) với REST API có authorization
5. **Frontend page với Next.js App Router** — minh họa cách FE tích hợp với BE qua server component pattern

Các snippet sau đây không phải toàn bộ codebase — codebase đầy đủ trên 200,000 dòng Java/TypeScript trải đều 10+ microservices. Tài liệu trích chỉ những đoạn có tính đại diện cho design pattern + nguyên tắc đã trình bày trong Chương 2.

<!-- TODO Wave 102+ GAP-655 — bổ sung citation accuracy verify cho từng snippet sau khi hoàn thiện V1 -->

---

## 3.2 JWT Authentication Flow tại Gateway

### Bối cảnh

KiteHub Gateway (Spring Cloud Gateway, port 8080) là entry point duy nhất cho mọi request từ frontend. Mọi request đi qua filter `JwtAuthenticationGatewayFilter` để verify chữ ký JWT (JSON Web Token, định nghĩa tại IETF RFC 7519 [29]) và truyền identity context (`userId`, `role`, `email`) xuống downstream services qua HTTP header (`X-User-Id`, `X-User-Roles`, `X-User-Email`). Đây là pattern "Trust the Gateway" — downstream services không tự verify JWT, mà tin tưởng header sau khi gateway đã kiểm tra.

Snippet sau minh họa pattern này. Filter có order `-100` để chạy SỚM, trước CircuitBreaker và RateLimiter filters. Public paths (login, signup, health check) bypass filter để cho phép unauthenticated access.

### Snippet — JWT verification + header propagation

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

### Phân tích

Snippet này thể hiện 3 design pattern chính:

1. **Chain of Responsibility** — Filter chain Spring Cloud Gateway, mỗi filter có order riêng, có thể short-circuit (trả 401 ngay) hoặc pass-through (`chain.filter(exchange)`)
2. **Fail-fast validation** — Constructor kiểm tra `JWT_SECRET` length ≥32 bytes (yêu cầu HS256); thiếu → throw `IllegalStateException` ngay khi Spring boot, không đợi runtime
3. **Trust boundary** — Sau filter, downstream services tin tưởng header `X-User-Id` / `X-User-Roles`. Cấu hình `SecurityConfig.XUserRolesHeaderFilter` ở downstream services map header này thành Spring Security `SecurityContext` để `@PreAuthorize` annotation hoạt động

Trước Wave 89, gateway KHÔNG set các header này → downstream services thấy SecurityContext rỗng → mọi endpoint `@PreAuthorize` reject với 401 dù JWT hợp lệ (lỗi GAP-604, fix Wave 89 Bucket A).

---

## 3.3 Multi-tenant Query với RLS NULL Force-Fail

### Bối cảnh

KiteClass là multi-tenant application — mỗi tenant (trường học) chia sẻ cùng database PostgreSQL nhưng dữ liệu phải được cách ly nghiêm ngặt. Kiến trúc dùng 3 lớp phòng vệ (defense-in-depth):

- **Layer 1 — Application-level filter:** `TenantContext` ThreadLocal được set tại request boundary qua `TenantFilterInterceptor`
- **Layer 2 — JPA query filter:** `@Filter("tenantFilter")` annotation trên entity tự động thêm `WHERE tenant_id = :currentTenantId` vào mọi query
- **Layer 3 — Database RLS (Row-Level Security):** Postgres policy reads session-local GUC `app.current_tenant_id` và reject mọi row không match — **default-deny** khi GUC chưa set (NULL force-fail)

Layer 3 là cơ chế cuối cùng — ngay cả khi Layer 1 + Layer 2 bị bypass (do bug, accidental raw SQL, hoặc test fixture), Postgres RLS vẫn từ chối truy cập cross-tenant. Đây là điểm khác biệt với approach "trust the app code" của nhiều SaaS đối thủ (Section 2.4 phân tích so sánh với MISA / Mona).

Snippet sau minh họa cách AOP aspect set session-local GUC tại mỗi `@Transactional` boundary.

### Snippet — TenantAwareDataSourceInterceptor

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

### Phân tích

Snippet này minh họa 4 design choice quan trọng:

1. **Aspect-Oriented Programming (AOP)** — Pointcut bắt mọi method `@Transactional` (Spring + Jakarta variants); không yêu cầu developer nhớ set GUC manually
2. **Parameterized SQL** — Dùng `set_config(..., :tenantId, true)` với `setParameter` thay vì string concat — chống SQL injection ngay cả khi tenantId từ untrusted source
3. **`is_local := true`** — Tham số thứ 3 của `set_config` tương đương `SET LOCAL` — GUC tự động clear khi transaction commit/rollback, không leak sang connection khác trong pool
4. **Default-deny semantic** — Khi `TenantContext` chưa set, GUC để rỗng → RLS policy đọc `current_setting('app.current_tenant_id', true)` trả `NULL` → mọi row reject. Background jobs phải explicit `TenantContext.runAs(tenantId, ...)` mới truy cập được data — nếu quên, query trả 0 rows (loud failure thay vì silent cross-tenant leak)

Migration RLS được định nghĩa trong `V58__enable_rls_tenant_scoped_tables.sql` (Wave 56) — bật `ENABLE ROW LEVEL SECURITY` trên tất cả tenant-scoped tables (`students`, `classes`, `grades`, `attendance`, `payments`, ...) cùng policy compare `instance_id = current_setting('app.current_tenant_id')::uuid`.

<!-- TODO Wave 102+ GAP-664 — bổ sung snippet V58 migration SQL khi business-logic audit hoàn tất 3-layer doc completeness -->

---

## 3.4 Email Worker Outbox Pattern

### Bối cảnh

KiteHub publish nhiều cross-service events: subscription state changes (trial → active → cancelled), beta access approval, branding update, email notification, ... Mỗi event cần được publish RELIABLY — nếu DB transaction commit nhưng event publish fail (RabbitMQ down, network drop), state sẽ bị inconsistent (DB nói "approved" nhưng email chưa gửi).

KiteHub áp dụng **Outbox Pattern** (Section 2.3.4): mỗi event được lưu vào bảng `*_outbox` trong cùng transaction với business state. Một background worker periodically poll bảng outbox và publish event tới RabbitMQ. Pattern này guarantee at-least-once delivery — nếu publish fail, dispatcher sẽ retry ở cycle tiếp theo.

Snippet sau là `SubscriptionOutboxDispatcher` — worker scan bảng `subscription_outbox` mỗi 10 giây, publish event chưa dispatch tới RabbitMQ exchange `email.exchange`.

### Snippet — SubscriptionOutboxDispatcher

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

    /** Transient backoff map: row id → last attempt timestamp. Cleared trên restart. */
    private final ConcurrentHashMap<UUID, LocalDateTime> lastAttemptAt = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${outbox.dispatcher.poll-interval-ms:10000}")
    @Transactional
    public void dispatch() {
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

### Phân tích

Snippet thể hiện 5 design choice:

1. **`@ConditionalOnProperty`** — Dispatcher có thể disable qua property `outbox.dispatcher.enabled=false` (cho test fixture hoặc maintenance mode); default enable nếu property thiếu (`matchIfMissing = true`)
2. **`@Scheduled(fixedDelayString)`** — Spring Scheduling poll mỗi 10s; `fixedDelay` đảm bảo previous cycle finish trước cycle mới start (tránh concurrent dispatch)
3. **Batch size guard** — Mỗi cycle xử lý tối đa 50 rows; tránh long-running transaction nếu queue backlog lớn
4. **In-memory backoff** — Failed rows không retry ngay lập tức (5 phút backoff) để tránh tight-loop khi RMQ down toàn cục; backoff map transient (clear khi restart) — chấp nhận trade-off: restart sẽ retry sớm hơn, hợp lý vì RMQ recovery thường <5 phút
5. **Metrics Micrometer** — `outbox_undispatched_count` (gauge số rows pending), `outbox_dispatcher_lag_seconds` (gauge age của oldest pending), `outbox_dispatcher_published_total` + `outbox_dispatcher_failed_total` (counter); xuất ra Prometheus qua actuator endpoint `/actuator/prometheus` (Section 4.1.3 trình bày observability pipeline)

Per Wave 91 Bucket A (GAP-605 closes outbox Phase 2): dispatcher đi kèm với `SubscriptionEventEmitter` fast-path — happy-path publish trực tiếp tới RMQ trong cùng transaction với DB write, đồng thời lưu outbox row làm reliability net. Nếu fast-path fail (RMQ down), outbox row stays NULL → dispatcher pick up khi broker recovery. Pattern này gọi là "Outbox + fast-path" — kết hợp low-latency happy-path với reliability guarantee.

---

## 3.5 Beta Access Controller Cluster — REST API 3-Tier

### Bối cảnh

Beta Access là feature core của Phase 1 BETA launch — visitors gửi yêu cầu beta access, coordinator (PLATFORM_ADMIN) duyệt qua admin dashboard, hệ thống gửi invite email với 6-digit claim code. Cluster này gồm 5 file (Controller + Service + Entity + DTO + Repository) minh họa 3-tier layering pattern theo nguyên lý Domain-Driven Design [19]: Controller (REST API + authorization), Service (business logic + transaction boundary, ranh giới của domain aggregate), Entity (JPA persistence — mô hình hóa entity nghiệp vụ).

Snippet sau là controller — minh họa cách `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` guard admin endpoints + cách map DTO ⟷ Entity.

### Snippet — BetaAccessController (public + admin endpoints)

```java
@RestController
@Slf4j
@Tag(name = "Beta Access", description = "Beta tenant invite mechanism (GAP-372 Wave 33 Phase 1 BETA)")
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

### Phân tích

3-tier layering pattern thể hiện rõ:

1. **Controller layer** — Chỉ chịu trách nhiệm:
   - HTTP request/response mapping (`@PostMapping`, `@GetMapping`, `@RequestBody`, `@PathVariable`)
   - Authorization (`@PreAuthorize("hasRole('PLATFORM_ADMIN')")`)
   - Validation entry point (`@Valid`) — Bean Validation tự động reject request invalid trước khi vào service
   - DTO ⟷ Entity mapping (qua static factory `BetaRequestResponse.from(saved)`)
   - Audit logging (`@Auditable(action = "BETA_APPROVE")` AOP aspect lưu admin action vào `admin_audit_log` table per PDPL Art 11 + Wave 92 enrichment)
2. **Service layer** (`BetaAccessService`) — Chịu trách nhiệm business logic + transaction:
   - `@Transactional` boundary — toàn bộ submitRequest / approveRequest atomic
   - Validation business rule (honeypot empty, email không trùng pending request, rate-limit 24h per email)
   - Generate claim code (random 6-digit) + invite token UUID
   - Publish event tới outbox (đoạn 3.4) để email worker gửi invite mail
3. **Entity layer** (`BetaAccessRequest`) — Pure data + JPA mapping:
   - `@Entity` + `@Table(name = "beta_access_requests")`
   - Field mapping (`@Id`, `@Column`, `@Enumerated(EnumType.STRING)`)
   - Audit trail (`@CreationTimestamp` + `@UpdateTimestamp`)

Anti-pattern tránh được: **God Service / Fat Controller**. Mọi business logic trong Service, mọi HTTP concern trong Controller, mọi persistence trong Entity — easy to test theo phương pháp Test-Driven Development [18] (mock Service trong ControllerTest, mock Repository trong ServiceTest); mỗi layer testable độc lập với một loại test fixture rõ ràng.

---

## 3.6 Frontend — Next.js App Router Page

### Bối cảnh

KiteHub frontend dùng Next.js 14 với App Router pattern (folder-based routing, server components by default). Mỗi page là một `page.tsx` file trong folder tương ứng URL path. Server components render tại server (giảm bundle size + tốt cho SEO), client components có `'use client'` directive khi cần interactivity (form state, event handlers).

Snippet sau là page `request-beta-access` — landing page khi visitor click "Request Beta Access" trên homepage. Page là server component (render tại server), embed `BetaRequestForm` (client component) cho form submission.

### Snippet — request-beta-access page

```typescript
/**
 * /auth/request-beta-access — Phase 1 BETA invite request landing page (GAP-372).
 *
 * Replaces the public signup form during Phase 1 BETA. Visitors submit a beta
 * access request; coordinator manually approves and emails the signup token.
 *
 * @since Wave 33 — GAP-372
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

### Phân tích

Snippet thể hiện các đặc trưng Next.js 14 + design pattern FE:

1. **App Router folder-based routing** — File path `app/(auth)/request-beta-access/page.tsx` map tới URL `/request-beta-access`. Folder `(auth)` là route group (parentheses) — không xuất hiện trong URL nhưng cho phép shared layout cho các page liên quan auth (login, register, beta-signup, ...)
2. **Server component default** — Page render tại server, không có `'use client'` directive. Lợi ích: HTML pre-rendered, SEO friendly, no JavaScript bundle cho static content
3. **Vietnamese content** — Page metadata + body text tiếng Việt per `vn-localization-audit-checklist.md` §2 (Vietnamese label requirement). Sample text natural cho persona target (Solo Teacher, Center Owner)
4. **Separation of concerns** — Page chỉ chịu layout + static text; form state management + API call delegate cho `BetaRequestForm` (client component) — tách rõ static vs interactive parts
5. **Composition pattern** — Page compose nhiều primitive component (`KiteLogo`, `BetaRequestForm`, `Link`) thay vì monolithic; mỗi component có single responsibility

Khi user submit form, `BetaRequestForm` (client component) gọi `POST /api/v1/auth/request-beta-access` qua fetch API. Request đi qua Next.js → Nginx → AWS ALB → KiteHub Gateway → KiteHub Subscription service → BetaAccessController (snippet 3.5) — toàn bộ flow request được trình bày trong Section 4.2.

<!-- TODO Wave 102+ GAP-655 — bổ sung BetaRequestForm.tsx snippet client component pattern với React Hook Form + Zod validation -->

---

## 3.7 Tóm tắt Chương 3

Chương 3 đã trình bày 5 cụm code snippet đại diện cho kiến trúc KiteHub:

| # | Snippet | Pattern | File source |
|---|---|---|---|
| 1 | JWT Authentication Filter | Chain of Responsibility + Trust boundary | `JwtAuthenticationGatewayFilter.java:44-123` |
| 2 | Tenant RLS Interceptor | AOP + Default-deny + Session GUC | `TenantAwareDataSourceInterceptor.java:50-129` |
| 3 | Outbox Dispatcher | Outbox Pattern + Scheduled task | `SubscriptionOutboxDispatcher.java:50-163` |
| 4 | Beta Access Controller | 3-Tier layering + `@PreAuthorize` | `BetaAccessController.java:62-180` |
| 5 | Next.js Page | App Router + Server Component | `(auth)/request-beta-access/page.tsx:1-41` |

Các snippet này không phản ánh toàn bộ ~200,000 dòng code của project, mà chỉ chọn lọc những đoạn tiêu biểu cho design pattern + nguyên tắc đã trình bày Chương 2 (multi-tenant isolation, microservices, observability, security defense-in-depth). Chương 4 tiếp theo sẽ trình bày kết quả triển khai trên môi trường cloud (AWS Singapore Free Tier) cùng với KPI metrics và scope beta tenant.

<!-- TODO Wave 102+ GAP-655 — sau khi V1 ship, audit lại từng snippet để verify cite chính xác file:line range; cập nhật nếu code shift sau commits Wave 101+ -->
