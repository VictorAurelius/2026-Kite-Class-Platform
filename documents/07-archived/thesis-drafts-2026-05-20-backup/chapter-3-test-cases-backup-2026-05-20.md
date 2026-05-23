---
title: Chương 3 §3.3.2-§3.3.5 — 3 sample test cases + audit định kỳ (BACKUP)
chapter: 3
section: test-cases-backup
audience: dev
last-updated: 2026-05-20
status: archived
---

# Backup — Chương 3 §3.3.2-§3.3.5 (snapshot 2026-05-20)

Backup này preserve nội dung của 3 sample test case code (JWT unit / RLS integration / Outbox E2E) + §3.3.5 Kết quả audit định kỳ trước khi Wave 102.5 follow-up xóa chúng khỏi Ch.3 main flow.

**Lý do backup:** User direction Wave 102.5 follow-up 2026-05-20 — 'lược bỏ trình bày code kiểm thử + §3.3.5 Kết quả audit định kỳ (đề cập đến wave)'. Per khung-chuẩn báo cáo cử nhân CNTT, Ch.3 không yêu cầu code presentation detailed; test pyramid §3.3.1 narrative + §3.3.6 tóm tắt kết quả đủ cho defense.

---

### 3.3.2 Ca kiểm thử 1 — Unit test xác thực JWT

Bối cảnh: Test verify filter JWT của kitehub-gateway extract đúng `TenantContext` và role guard từ token hợp lệ, đồng thời reject token sai chữ ký với HTTP 401.

**Bảng 3.1.** Đặc tả ca kiểm thử unit test cho luồng JWT authentication.

| Thuộc tính | Giá trị |
|---|---|
| Tên test | `JwtAuthenticationGatewayFilterTest.validToken_propagatesIdentityHeaders` |
| Module | `kitehub-gateway` / `filter` package |
| Mục tiêu | Verify filter propagate đúng `X-User-Id`, `X-User-Roles`, `X-User-Email` header xuống downstream khi JWT hợp lệ |
| Setup | Tạo JWT với secret 32-byte + 3 claim (sub, role, email); mock `GatewayFilterChain` |
| Expected | Header được set đúng cho downstream request; chain.filter() được gọi |
| Loại test | Unit test (Mockito mock dependencies) |
| Thời gian chạy | 80-150 ms/test |
| Verdict | PASS |

```java
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGatewayFilterTest {

    private static final String TEST_SECRET = "test-jwt-secret-must-be-at-least-32-bytes-long-for-hs256-algorithm";
    private JwtAuthenticationGatewayFilter filter;

    @BeforeEach
    void setUp() {
        // Khởi tạo filter với secret hợp lệ
        filter = new JwtAuthenticationGatewayFilter(TEST_SECRET);
    }

    @Test
    @DisplayName("Token hợp lệ thì propagate X-User-Id, X-User-Roles, X-User-Email header")
    void validToken_propagatesIdentityHeaders() {
        // Arrange: tạo JWT hợp lệ với 3 claim
        String token = Jwts.builder()
                .subject("user-uuid-123")
                .claim("role", "PLATFORM_ADMIN")
                .claim("email", "admin@kitehub.me")
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()))
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/admin/beta-requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert: header X-User-Id / X-User-Roles / X-User-Email được set đúng
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerHttpRequest mutated = captor.getValue().getRequest();
        assertThat(mutated.getHeaders().getFirst("X-User-Id")).isEqualTo("user-uuid-123");
        assertThat(mutated.getHeaders().getFirst("X-User-Roles")).isEqualTo("PLATFORM_ADMIN");
        assertThat(mutated.getHeaders().getFirst("X-User-Email")).isEqualTo("admin@kitehub.me");
    }

    @Test
    @DisplayName("Token chữ ký sai thì trả HTTP 401 Unauthorized")
    void invalidSignature_returns401() {
        // Arrange: tạo token với secret khác (giả lập attacker forge token)
        String forgedToken = Jwts.builder()
                .subject("attacker-uuid")
                .signWith(Keys.hmacShaKeyFor("different-secret-32-bytes-long-attacker-attempt".getBytes()))
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/admin/beta-requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // Act
        filter.filter(exchange, chain).block();

        // Assert: response status = 401 và chain.filter() không được gọi
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }
}
```

Source: `kitehub/kitehub-gateway/src/test/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilterTest.java`

Pattern minh họa: Unit test cô lập filter logic với mock `GatewayFilterChain`, kiểm thử cả happy path lẫn unhappy path (token bị forge). Thời gian execute 80-150 ms/test, phù hợp với inner-loop developer feedback. Ca test này thuộc lớp foundation của test pyramid — đảm bảo logic core (parse JWT + propagate identity) hoạt động đúng trong isolation, không phụ thuộc database hay network.

### 3.3.3 Ca kiểm thử 2 — Integration test RLS NULL Force-Fail

Bối cảnh: Test verify Postgres Row-Level Security policy reject query khi `TenantContext` chưa được set — đảm bảo default-deny semantic. Test sử dụng Testcontainers Postgres real (không được dùng H2 thay thế vì H2 không support `set_config` và RLS policy — bug class này invisible với unit test mock vì Mockito không reproduce được Postgres GUC + RLS policy behavior).

**Bảng 3.2.** Đặc tả ca kiểm thử integration test cho RLS NULL force-fail.

| Thuộc tính | Giá trị |
|---|---|
| Tên test | `TenantRlsNullForceFailIT.rls_nullForceFail_returnsZeroRows` |
| Module | `kiteclass-core` / `datasource` package |
| Mục tiêu | Verify RLS reject query khi GUC `app.current_tenant_id` chưa set (NULL force-fail) |
| Setup | Testcontainers PostgreSQL 16 + Flyway apply migrations V1-V60 + seed 2 students thuộc 2 tenant khác nhau |
| Expected | Query không có TenantContext trả về danh sách rỗng (default-deny); query với TenantContext = TENANT_A chỉ trả 1 row của tenant A |
| Loại test | Integration test (Spring Boot Test + Testcontainers) |
| Thời gian chạy | 8-12 giây/test |
| Verdict | PASS |

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TenantRlsNullForceFailIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kiteclass_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EntityManager entityManager;

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @BeforeEach
    @Transactional
    void seedDataAcrossTenants() {
        // Seed 2 hoc sinh thuoc 2 tenant khac nhau (bypass RLS bang cach set GUC trong setup)
        entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, false)")
                .setParameter("tid", TENANT_A.toString())
                .getSingleResult();
        Student studentA = new Student("Nguyễn Văn An", "an@skyedu.vn", TENANT_A);
        entityManager.persist(studentA);

        entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, false)")
                .setParameter("tid", TENANT_B.toString())
                .getSingleResult();
        Student studentB = new Student("Trần Thị Bình", "binh@quangminh.edu.vn", TENANT_B);
        entityManager.persist(studentB);
    }

    @Test
    @DisplayName("RLS reject query khi TenantContext chưa được set — default-deny semantic")
    void rls_nullForceFail_returnsZeroRows() {
        // Arrange: khong set TenantContext (gia lap background job quen runAs)
        TenantContext.clear();

        // Act: query toan bo students (khong co TenantContext)
        List<Student> result = studentRepository.findAll();

        // Assert: RLS reject moi row vi GUC chua duoc set (NULL force-fail)
        // Default-deny: thay vi leak cross-tenant data, tra ve danh sach rong
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("RLS chỉ trả về row của tenant hiện tại khi TenantContext = TENANT_A")
    void rls_setTenantA_returnsOnlyTenantARows() {
        // Arrange: set TenantContext = TENANT_A
        TenantContext.runAs(TENANT_A, () -> {
            // Act
            List<Student> result = studentRepository.findAll();

            // Assert: chi co students thuoc TENANT_A, khong bao gom TENANT_B
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("an@skyedu.vn");
            assertThat(result.get(0).getTenantId()).isEqualTo(TENANT_A);
        });
    }
}
```

Source: `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/datasource/TenantRlsNullForceFailIT.java`

Pattern minh họa: Integration test với Testcontainers Postgres real DB session validate hành vi RLS NULL force-fail — bug class này invisible với unit test mock. Sự cố production admin-login 500 (xem báo cáo RCA `2026-05-16-admin-login-500-rca.md`) đã chứng minh tầm quan trọng của Testcontainers thay vì H2 in-memory. Migration V60 thực hiện ENABLE ROW LEVEL SECURITY cùng policy `USING (instance_id = current_setting('app.current_tenant_id')::uuid)` trên các bảng tenant-scoped. Đây là tầng phòng vệ Layer 3 trong defense-in-depth — ngay cả khi Layer 1 (application filter) và Layer 2 (JPA filter) bị bypass do bug hoặc raw SQL, Layer 3 vẫn từ chối truy cập cross-tenant.

### 3.3.4 Ca kiểm thử 3 — End-to-end test Outbox dispatcher

Bối cảnh: Test verify `SubscriptionOutboxDispatcher` đảm bảo at-least-once delivery khi RabbitMQ publish thất bại — dispatcher phải retry ở cycle tiếp theo (backoff 5 phút). Ca test này verify ba thành phần của Outbox Pattern hoạt động đúng dưới điều kiện failure.

**Bảng 3.3.** Đặc tả ca kiểm thử E2E cho Outbox dispatcher retry.

| Thuộc tính | Giá trị |
|---|---|
| Tên test | `OutboxDispatcherE2EIT.outbox_retryAfterBackoff_whenPublishFails` |
| Module | `kitehub-subscription` / Outbox dispatcher |
| Mục tiêu | Verify dispatcher retry với backoff khi RabbitMQ tạm thời down + publish thành công sau khi RMQ recovery |
| Setup | Testcontainers Postgres + RabbitMQ container + 1 outbox row PENDING |
| Expected | Attempt 1 fail (RMQ stopped) → backoff 1 phút → restart RMQ → attempt 2 PASS với `dispatched_at` set |
| Loại test | End-to-end test (multi-container integration) |
| Thời gian chạy | 70-90 giây/test |
| Verdict | PASS |

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OutboxDispatcherE2EIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("outbox.dispatcher.batch-size", () -> "10");
        registry.add("outbox.dispatcher.backoff-min-minutes", () -> "1"); // shorter for test
    }

    @Autowired
    private SubscriptionOutboxRepository outboxRepository;

    @Autowired
    private SubscriptionOutboxDispatcher dispatcher;

    @Autowired
    private RabbitListenerTestHarness harness;

    @Test
    @DisplayName("E2E: Outbox event được publish đúng routing key và payload sau dispatcher cycle")
    @Transactional
    void outbox_publishesEventToRabbitMQ_afterDispatcherCycle() throws Exception {
        // Arrange: tao outbox event chua dispatch
        SubscriptionOutboxEvent event = new SubscriptionOutboxEvent(
                "BETA_APPROVED",
                "email.beta.approved",
                "{"tenantId":"sky-edu-uuid","recipient":"hong.tran@skyedu.vn","claimCode":"123456"}"
        );
        outboxRepository.save(event);
        assertThat(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc()).hasSize(1);

        // Act: trigger dispatcher cycle 1 lan
        dispatcher.dispatch();

        // Assert: event da duoc publish va row dispatched_at da duoc set
        SubscriptionOutboxEvent dispatched = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(dispatched.getDispatchedAt()).isNotNull();
        assertThat(outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc()).isEmpty();

        // Verify RabbitMQ nhan dung message qua test harness
        Message received = harness.next(EmailQueueConfig.EMAIL_EXCHANGE, "email.beta.approved");
        assertThat(received).isNotNull();
        assertThat(new String(received.getBody())).contains("hong.tran@skyedu.vn");
        assertThat(new String(received.getBody())).contains("123456");
    }

    @Test
    @DisplayName("E2E: Outbox retry khi publish fail và backoff trong cycle tiếp theo")
    @Transactional
    void outbox_retryAfterBackoff_whenPublishFails() throws Exception {
        // Arrange: stop RabbitMQ de gia lap publish fail
        rabbitmq.stop();
        SubscriptionOutboxEvent event = new SubscriptionOutboxEvent(
                "TENANT_PROVISIONED",
                "email.tenant.provisioned",
                "{"tenantId":"sky-edu-uuid"}"
        );
        outboxRepository.save(event);

        // Act: dispatcher attempt 1 - fail (RabbitMQ down)
        dispatcher.dispatch();
        SubscriptionOutboxEvent attempt1 = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(attempt1.getDispatchedAt()).isNull(); // Van chua dispatched

        // Restart RabbitMQ
        rabbitmq.start();

        // Wait backoff window (1 phut trong test)
        Thread.sleep(Duration.ofMinutes(1).plusSeconds(5).toMillis());

        // Act: dispatcher attempt 2 - sau backoff, RMQ da up
        dispatcher.dispatch();

        // Assert: event publish thanh cong trong attempt 2
        SubscriptionOutboxEvent attempt2 = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(attempt2.getDispatchedAt()).isNotNull();
    }
}
```

Source: `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/outbox/OutboxDispatcherE2EIT.java`

Pattern minh họa: End-to-end test với 2 container (Postgres + RabbitMQ) verify reliability invariant của Outbox Pattern — at-least-once delivery khi broker tạm thời unavailable. Đây là loại test mà unit test mock không thể replicate (Mockito không reproduce được hành vi network failure + recovery). Test kết hợp với metric Prometheus `outbox_dispatcher_failed_total` và `outbox_dispatcher_lag_seconds` để verify observability pipeline cũng hoạt động đúng. Khi DLQ không rỗng, alert SNS fires tới email `support@kitehub.me`. Thời gian execute 70-90 giây/test (bao gồm container startup + Thread.sleep backoff window).

### 3.3.5 Kết quả audit chất lượng định kỳ

KiteHub Platform áp dụng quy trình audit chất lượng định kỳ theo cadence hàng quý với 4 dimension chính. Audit định kỳ là minh chứng quan trọng cho thấy hệ thống được duy trì chất lượng liên tục thay vì chỉ đo lường một lần khi ship.

Unit test coverage: Báo cáo coverage được thu thập tự động qua Jacoco plugin trên mỗi CI build. Theo kết quả audit chất lượng tổng quát Wave 98 (2026-05-19) đạt mức 90/110 điểm B+ (pass tier Phase 1 BETA ngưỡng ≥80 với buffer +10 điểm, đáp ứng ngưỡng PROD MAJOR ≥85 với buffer +5 điểm). Áp dụng framework đo lường nội bộ, không thay thế chuẩn ngoài. Coverage trung bình các module business-critical: kitehub-subscription khoảng 78% line / 72% branch (mục tiêu ≥75% line); kitehub-platform khoảng 76% line / 70% branch; kitehub-branding khoảng 73% line / 68% branch (mục tiêu ≥70% line — đạt); kitehub-email khoảng 71% line / 65% branch (slightly below target — follow-up task); kiteclass-core khoảng 80% line / 74% branch.

Security audit: Báo cáo security audit Wave 94c (2026-05-18) đạt 93/100 điểm A theo định dạng v2 audit format mandatory — gồm 27 control evidence block per OWASP Top 10 2021. Mỗi block bao gồm 4 phần: Command run + Output + Verdict + Evidence artifact ID. Coverage: A01 Broken Access Control thực hiện qua RLS NULL force-fail enforce default-deny + JWT role guard `@PreAuthorize` declarative; A02 Cryptographic Failures qua HS256 256-bit secret + TLS 1.3 termination tại ALB + Cloudflare DNSSEC; A03 Injection qua parameterized SQL (`set_config` parameter binding) + JPA `@Query` named parameter + Bean Validation `@Valid`; A09 Security Logging qua V60 immutable admin_audit_logs PDPL Article 11 tamper-proof; cùng 23 control khác chi tiết trong báo cáo audit.

Performance baseline: Báo cáo performance Wave 85 (2026-05-15) đạt 86/100 điểm B+. Cite per-endpoint p95 latency target (đo từ public probe): `POST /api/v1/auth/login` target p95 dưới 300 ms, đo được khoảng 280 ms PASS; `GET /api/v1/admin/beta-requests` target p95 dưới 500 ms, đo được khoảng 340 ms PASS; `POST /api/v1/auth/request-beta-access` target p95 dưới 500 ms, đo được khoảng 310 ms PASS. Database query overhead RLS khoảng 2-3 ms trung bình per query (acceptable trong target dưới 5%). HikariCP pool utilization trung bình 60%, không có connection leak detected. 3 CloudWatch alarm wired (CPU trên 80%, RDS connections trên 80%, ALB 5xx trên 1%).

API contract audit: Báo cáo API contract audit Wave 98 đạt 76/100 điểm C FAIL (do 2 P0 sub-checks về EmailController URL drift và PreferencesController zero IT). Đã được khắc phục qua cluster cải tiến drift detection CI script và bổ sung integration tests cho PreferencesController. Audit suite tiếp theo dự kiến đạt 82/100 PASS.

Cadence audit suite quarterly: Theo quy định nội bộ, các audit suite chạy lại trong vòng 3 ngày sau mỗi wave closure cho category áp dụng (UI / Business / API / Security / Performance / Ops). Quality audit /110 và quarterly retention (kéo dài 90 ngày) áp dụng cho mọi wave. Cadence này đảm bảo finding mới được track kịp thời, không tích lũy debt không nhìn thấy.
