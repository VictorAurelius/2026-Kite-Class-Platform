---
title: Chương 3 — Backup phân tích 5 đoạn mã đại diện (pre Wave 102.5 rework)
audience: dev
status: archived
created: 2026-05-20
related-wave: wave-2026-05-20-102.5-thesis-v1-fix-bundle
related-rule: thesis-content-standard.md C2 — repo-internal retrospective content moved khỏi chapter body
---

# Backup — Phân tích 5 đoạn mã đại diện Chương 3 (Wave 102.5 Bucket E rework)

Đây là backup nội dung gốc §3.3-§3.7 của Chương 3 chứa 5 code snippet analysis. Nội dung này được REMOVE khỏi `chapter-3-implementation.md` main flow theo Wave 102.5 Item 9a (mismatch khung-chuẩn UTC: thesis chương Triển khai theo convention yêu cầu mô tả "kết quả triển khai sản phẩm" hơn là phân tích code mức snippet — code-level discussion thuộc Phụ lục hoặc appendix nếu cần).

Mục đích backup file:
- Preserve technical depth cho future reference / appendix expansion
- Cho phép re-attach vào Phụ lục Wave 102.6 nếu committee yêu cầu
- Documented per `docs-archival-cadence.md` Tier 2 timestamp convention

## Nội dung backup

### Phạm vi năm đoạn mã đại diện (table reference)

| # | Snippet | LOC sample | Pattern minh họa | File source |
|---|---|:---:|---|---|
| 1 | JWT authentication tại gateway | ~80 | Edge security, trust boundary, identity propagation | `JwtAuthenticationGatewayFilter.java:44-123` |
| 2 | Multi-tenant isolation với Postgres RLS | ~80 | AOP, defense-in-depth, default-deny semantic | `TenantAwareDataSourceInterceptor.java:50-129` |
| 3 | Email worker outbox pattern | ~70 | Transactional outbox, scheduled dispatcher, backoff | `SubscriptionOutboxDispatcher.java:50-163` |
| 4 | Beta Access controller cluster | ~120 | 3-tier layering, `@PreAuthorize`, audit aspect | `BetaAccessController.java:62-180` |
| 5 | Frontend page với Next.js App Router | ~40 | Server component, composition, separation of concerns | `(auth)/request-beta-access/page.tsx:1-41` |

### Snippet 1 — JWT Authentication Flow tại Gateway

**Bối cảnh:** KiteHub Gateway (Spring Cloud Gateway, port 8080) là entry point duy nhất cho mọi request từ frontend. Mọi request đi qua filter `JwtAuthenticationGatewayFilter` để verify chữ ký JWT (JSON Web Token, định nghĩa tại IETF RFC 7519 [30]) và truyền identity context (`userId`, `role`, `email`) xuống downstream services qua HTTP header (`X-User-Id`, `X-User-Roles`, `X-User-Email`). Đây là pattern "Trust the Gateway" — downstream services không tự verify JWT, mà tin tưởng header sau khi gateway đã kiểm tra.

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

Phân tích 3 design pattern (Chain of Responsibility filter chain, Fail-fast validation constructor, Trust boundary với `SecurityConfig.XUserRolesHeaderFilter` mapping ở downstream) + trade-offs HS256 vs RS256 (cùng vùng tin cậy VPC AWS Singapore, simplicity ưu tiên cho beta, lộ trình RS256 multi-region) + tham khảo RFC 7519 §6 [30, tr.21] và Spring Security Reference §11.3 [31].

### Snippet 2 — Multi-tenant Query với RLS NULL Force-Fail

**Bối cảnh:** KiteClass là multi-tenant application — mỗi tenant (trường học) chia sẻ cùng database PostgreSQL nhưng dữ liệu phải được cách ly nghiêm ngặt. Kiến trúc dùng 3 lớp phòng vệ (defense-in-depth): application-level filter (`TenantContext` ThreadLocal được set tại request boundary qua `TenantFilterInterceptor`), JPA query filter (`@Filter("tenantFilter")` annotation), database RLS (Row-Level Security policy reads session-local GUC `app.current_tenant_id`).

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
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        if (Boolean.TRUE.equals(TransactionSynchronizationManager.getResource(TENANT_GUC_SET_MARKER))) {
            return;
        }
        UUID tenantId = TenantContext.getCurrentTenant();
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

Phân tích 4 design choice (AOP pointcut bắt mọi `@Transactional` Spring + Jakarta variants, parameterized SQL chống injection, `is_local := true` session-local GUC tự clear khi commit/rollback, default-deny semantic khi `TenantContext` chưa set) + migration `V58__enable_rls_tenant_scoped_tables.sql` + trade-offs RLS vs application-level isolation (defense-in-depth, performance overhead 2-3ms acceptable, PostgreSQL Documentation §5.8 [8, tr.158], OWASP Defense-in-Depth [6]).

### Snippet 3 — Email Worker Outbox Pattern

**Bối cảnh:** KiteHub publish nhiều cross-service events: subscription state changes, beta access approval, branding update, email notification. Mỗi event cần được publish reliably — nếu DB transaction commit nhưng event publish fail, state sẽ bị inconsistent. Outbox Pattern [1] (Section 2.3.4): mỗi event lưu vào bảng `*_outbox` trong cùng transaction với business state, background worker periodically poll và publish tới RabbitMQ. Pattern guarantee at-least-once delivery.

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

Phân tích 5 design choice (`@ConditionalOnProperty` toggle, `@Scheduled fixedDelay` non-overlap, batch size guard, in-memory backoff transient, Micrometer metrics `outbox_undispatched_count` + `outbox_dispatcher_lag_seconds`) + fast-path `SubscriptionEventEmitter` companion + trade-offs Outbox vs direct broker publish (transactional consistency, race condition `FOR UPDATE SKIP LOCKED` PostgreSQL 9.5+ [8], at-least-once delivery AMQP 0-9-1 [4, tr.47]).

### Snippet 4 — Beta Access Controller Cluster (3-Tier REST API)

**Bối cảnh:** Beta Access là feature core của giai đoạn beta — visitors gửi yêu cầu beta access, coordinator (PLATFORM_ADMIN) duyệt qua admin dashboard, hệ thống gửi invite email với 6-digit claim code. Cluster gồm 5 file (Controller + Service + Entity + DTO + Repository) minh họa 3-tier layering pattern theo Domain-Driven Design [18].

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

Source: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java:62-180`

Phân tích 3-tier layering pattern (Controller HTTP+auth+validation+DTO mapping+audit, Service business logic + `@Transactional` boundary + claim code generation + outbox event, Entity JPA persistence + `@CreationTimestamp` + audit trail) + anti-pattern God Service / Fat Controller eliminated + Test-Driven Development [17] mock layering + trade-offs REST vs GraphQL + declarative `@PreAuthorize` SpEL vs manual permission check + tham khảo DDD Evans [18], REST Fielding [33], GraphQL Spec [15], OpenAPI 3.1 [32].

### Snippet 5 — Frontend Page với Next.js App Router

**Bối cảnh:** KiteHub frontend dùng Next.js 14 với App Router pattern (folder-based routing, server components by default). Mỗi page là một `page.tsx` file trong folder tương ứng URL path. Server components render tại server (giảm bundle size + tốt cho SEO), client components có `'use client'` directive khi cần interactivity (form state, event handlers).

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

Phân tích đặc trưng Next.js 14 (App Router folder-based routing với route group `(auth)`, server component default no `'use client'`, Vietnamese content metadata + body, separation of concerns layout vs interactive form, composition pattern primitive components) + flow request submit qua Nginx → AWS ALB → KiteHub Gateway → kitehub-subscription → BetaAccessController + trade-offs App Router vs Pages Router vs CRA SPA + tham khảo Next.js Docs [34], React Server Components RFC [35], Core Web Vitals [36].

## Lý do remove khỏi main flow

Theo Wave 102.5 plan Item 9a + khung-chuẩn audit G7 + `thesis-content-standard.md` rubric v2 §C2:
- Chương Triển khai (Implementation) trong UTC convention cử nhân CNTT tập trung vào **mô tả kết quả triển khai sản phẩm** (UI screenshots, deployment evidence, testing pyramid evidence) hơn là **phân tích code mức snippet**
- Code snippet analysis thuộc phụ lục appendix hoặc bài báo khoa học chuyên đề riêng
- Việc include 5 snippet với 4 sub-section mỗi snippet (Bối cảnh + Snippet + Phân tích + Trade-offs) đẩy page count khoảng 25-30 trang cho riêng Chương 3, vượt budget khi target toàn thesis cử nhân là khoảng 80 trang
- Pattern minh họa (JWT auth, RLS, Outbox, 3-tier, App Router) đã được trình bày kiến trúc tổng quát trong Chương 2; Chương 3 nay focus output triển khai

Nội dung backup này preserve full để Wave 102.6 có thể đánh giá lại có nên đưa vào Phụ lục hay loại bỏ hoàn toàn.
