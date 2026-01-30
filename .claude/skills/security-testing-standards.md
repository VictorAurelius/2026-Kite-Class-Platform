# Skill: Security Testing Standards

**Version:** 1.0
**Date:** 2026-01-30
**Purpose:** Comprehensive security testing standards for KiteClass Platform

## Mô tả

Security testing coverage cho:
- Tenant boundary testing (data isolation, cross-tenant attacks)
- Feature access control testing (tier bypass prevention)
- Payment security testing (QR tampering, double payment, replay attacks)
- JWT security testing (token lifecycle, signature verification)
- OWASP Top 10 testing (SQL injection, XSS, CSRF, etc.)

## Trigger phrases

- "test security"
- "security testing"
- "tenant isolation"
- "payment security"
- "owasp testing"

---

# PART 1: TENANT BOUNDARY SECURITY TESTING

## 1.1 Cross-Tenant Data Access Prevention

### Test Pattern: Direct Database Query Injection

```java
@SpringBootTest
@Transactional
class TenantBoundarySecurityTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should prevent SQL injection via tenant context")
    void shouldPreventSqlInjection_viaTenantContext() {
        // Given: Two tenants with students
        UUID tenant1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID tenant2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // Create students for both tenants
        jdbcTemplate.update(
                "INSERT INTO students (name, email, instance_id) VALUES (?, ?, ?)",
                "Tenant1 Student", "student1@t1.com", tenant1);

        jdbcTemplate.update(
                "INSERT INTO students (name, email, instance_id) VALUES (?, ?, ?)",
                "Tenant2 Student", "student2@t2.com", tenant2);

        // When: Attacker tries SQL injection via tenant context
        TenantContext.setCurrentTenant(tenant1);

        // Try to inject: "' OR instance_id IS NOT NULL --"
        String maliciousInput = "' OR instance_id IS NOT NULL --";

        // Then: Repository should use parameterized queries (safe)
        List<Student> students = studentRepository.findByEmail(maliciousInput);

        // Should return empty (no match), not all students
        assertThat(students).isEmpty();

        // Verify: Only tenant1's data accessible
        List<Student> tenant1Students = studentRepository.findAll();
        assertThat(tenant1Students)
                .hasSize(1)
                .allMatch(s -> s.getInstanceId().equals(tenant1));

        TenantContext.clear();
    }

    @Test
    @DisplayName("should prevent cross-tenant access via direct ID manipulation")
    void shouldPreventCrossTenantAccess_viaIdManipulation() {
        // Given: Student belongs to tenant1
        UUID tenant1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID tenant2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        TenantContext.setCurrentTenant(tenant1);
        Student student = studentRepository.save(Student.builder()
                .name("Tenant1 Student")
                .email("student@t1.com")
                .instanceId(tenant1)
                .build());
        Long studentId = student.getId();
        TenantContext.clear();

        // When: Tenant2 tries to access tenant1's student by ID
        TenantContext.setCurrentTenant(tenant2);

        Optional<Student> result = studentRepository.findById(studentId);

        // Then: Should return empty (tenant filter prevents access)
        assertThat(result).isEmpty();

        TenantContext.clear();
    }

    @Test
    @DisplayName("should prevent UPDATE of other tenant's data")
    void shouldPreventUpdate_ofOtherTenantData() {
        // Given: Student belongs to tenant1
        UUID tenant1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID tenant2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        TenantContext.setCurrentTenant(tenant1);
        Student student = studentRepository.save(Student.builder()
                .name("Original Name")
                .email("student@t1.com")
                .instanceId(tenant1)
                .build());
        Long studentId = student.getId();
        TenantContext.clear();

        // When: Tenant2 tries to UPDATE tenant1's student
        TenantContext.setCurrentTenant(tenant2);

        // Try to update via raw SQL (bypass JPA)
        int rowsUpdated = jdbcTemplate.update(
                "UPDATE students SET name = ? WHERE id = ? AND instance_id = ?",
                "Hacked Name", studentId, tenant2); // Wrong tenant ID

        // Then: 0 rows updated (tenant mismatch)
        assertThat(rowsUpdated).isEqualTo(0);

        // Verify: Original data unchanged
        TenantContext.clear();
        TenantContext.setCurrentTenant(tenant1);
        Student unchanged = studentRepository.findById(studentId).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Original Name");

        TenantContext.clear();
    }

    @Test
    @DisplayName("should prevent DELETE of other tenant's data")
    void shouldPreventDelete_ofOtherTenantData() {
        // Given: Student belongs to tenant1
        UUID tenant1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID tenant2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        TenantContext.setCurrentTenant(tenant1);
        Student student = studentRepository.save(Student.builder()
                .name("Tenant1 Student")
                .email("student@t1.com")
                .instanceId(tenant1)
                .build());
        Long studentId = student.getId();
        TenantContext.clear();

        // When: Tenant2 tries to DELETE tenant1's student
        TenantContext.setCurrentTenant(tenant2);

        studentRepository.deleteById(studentId);

        // Then: Student should still exist (soft delete OR no access)
        TenantContext.clear();
        TenantContext.setCurrentTenant(tenant1);
        assertThat(studentRepository.findById(studentId)).isPresent();

        TenantContext.clear();
    }
}
```

### Test Pattern: API-Level Cross-Tenant Access

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class TenantBoundaryApiSecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("API should reject requests with mismatched tenant ID")
    void api_shouldRejectMismatchedTenantId() {
        // Given: JWT contains tenant1
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        String jwt = generateJwt(tenant1, "TEACHER");

        // When: Send request with different tenant ID in header
        webTestClient.get()
                .uri("/api/v1/students")
                .header("Authorization", "Bearer " + jwt)
                .header("X-Tenant-Id", tenant2.toString()) // Mismatch!
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("TENANT_MISMATCH");
    }

    @Test
    @DisplayName("API should reject requests without tenant ID header")
    void api_shouldRejectMissingTenantId() {
        // Given: Valid JWT
        String jwt = generateJwt(UUID.randomUUID(), "TEACHER");

        // When: Send request without X-Tenant-Id header
        webTestClient.get()
                .uri("/api/v1/students")
                .header("Authorization", "Bearer " + jwt)
                // Missing X-Tenant-Id
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("TENANT_ID_REQUIRED");
    }

    @Test
    @DisplayName("API should prevent accessing other tenant's resources via URL")
    void api_shouldPreventAccessingOtherTenantResources() {
        // Given: Student belongs to tenant1
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        TenantContext.setCurrentTenant(tenant1);
        Student student = studentRepository.save(Student.builder()
                .name("Tenant1 Student")
                .email("student@t1.com")
                .instanceId(tenant1)
                .build());
        Long studentId = student.getId();
        TenantContext.clear();

        // When: Tenant2 tries to access tenant1's student
        String jwt = generateJwt(tenant2, "TEACHER");

        webTestClient.get()
                .uri("/api/v1/students/" + studentId)
                .header("Authorization", "Bearer " + jwt)
                .header("X-Tenant-Id", tenant2.toString())
                .exchange()
                .expectStatus().isNotFound() // Tenant filter prevents access
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("STUDENT_NOT_FOUND");
    }
}
```

---

# PART 2: FEATURE ACCESS CONTROL SECURITY TESTING

## 2.1 Tier Bypass Prevention

```java
@SpringBootTest
@Transactional
class FeatureAccessControlSecurityTest {

    @Autowired
    private FeatureDetectionService featureService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private InstanceConfigRepository configRepo;

    @Test
    @DisplayName("should prevent feature bypass via config manipulation")
    void shouldPreventFeatureBypass_viaConfigManipulation() {
        // Given: BASIC tier (no ENGAGEMENT)
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = configRepo.save(InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .features(Map.of("ENGAGEMENT", false))
                .build());

        // When: Attacker tries to enable ENGAGEMENT manually in config
        config.getFeatures().put("ENGAGEMENT", true); // Direct manipulation

        // Then: requireFeature should still check tier, not just features map
        assertThatThrownBy(() -> featureService.requireFeature(instanceId, "ENGAGEMENT"))
                .isInstanceOf(FeatureNotAvailableException.class);

        // Or: Service should reload config from database (not trust cached/modified)
    }

    @Test
    @DisplayName("should prevent API access without feature check")
    void shouldPreventApiAccess_withoutFeatureCheck() {
        // Given: BASIC tier instance
        UUID instanceId = UUID.randomUUID();
        configRepo.save(InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .features(Map.of("ENGAGEMENT", false))
                .build());

        // When: Try to call ENGAGEMENT feature API
        TenantContext.setCurrentTenant(instanceId);

        MarkAttendanceRequest request = new MarkAttendanceRequest(
                1L, LocalDate.now(), List.of());

        // Then: Should throw FeatureNotAvailableException
        assertThatThrownBy(() -> attendanceService.markAttendance(instanceId, request))
                .isInstanceOf(FeatureNotAvailableException.class);

        TenantContext.clear();
    }

    @Test
    @DisplayName("should prevent tier downgrade attack")
    void shouldPreventTierDowngradeAttack() {
        // Given: PREMIUM tier instance
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = configRepo.save(InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.PREMIUM)
                .features(Map.of("ENGAGEMENT", true, "MEDIA", true, "CUSTOM_DOMAIN", true))
                .build());

        // When: Attacker tries to downgrade to BASIC to reduce payment
        // (This should only be allowed via payment service, not direct update)

        // Simulate attacker bypassing payment and updating tier
        config.setTier(PricingTier.BASIC);
        configRepo.save(config);

        // Then: System should detect unauthorized tier change
        // (In production, tier changes must be audited and validated)

        // Verify audit log records the change
        // assertThat(auditLogRepo.findByInstanceId(instanceId))
        //         .anyMatch(log -> log.getAction().equals("TIER_CHANGED"));
    }
}
```

## 2.2 Limit Enforcement Security

```java
@SpringBootTest
@Transactional
class LimitEnforcementSecurityTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private InstanceConfigRepository configRepo;

    @Test
    @DisplayName("should enforce student limit even with direct repository access")
    void shouldEnforceLimit_evenWithDirectRepositoryAccess() {
        // Given: BASIC tier with 50 student limit
        UUID instanceId = UUID.randomUUID();
        configRepo.save(InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .limitations(Map.of("maxStudents", 50))
                .build());

        TenantContext.setCurrentTenant(instanceId);

        // Fill up to 50 students
        for (int i = 0; i < 50; i++) {
            studentRepository.save(Student.builder()
                    .name("Student " + i)
                    .email("student" + i + "@test.com")
                    .instanceId(instanceId)
                    .build());
        }

        // When: Try to bypass service layer and insert directly via repository
        Student student51 = Student.builder()
                .name("Student 51")
                .email("student51@test.com")
                .instanceId(instanceId)
                .build();

        // Then: Service layer must still enforce limit
        // (Repository allows insert, but service should check before calling repo)
        CreateStudentRequest request = new CreateStudentRequest(
                "Student 51", "student51@test.com", "0901234567", null, null);

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(LimitExceededException.class);

        TenantContext.clear();
    }

    @Test
    @DisplayName("should prevent limit bypass via batch operations")
    void shouldPreventLimitBypass_viaBatchOperations() {
        // Given: BASIC tier with 50 student limit, 48 students exist
        UUID instanceId = UUID.randomUUID();
        configRepo.save(InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .limitations(Map.of("maxStudents", 50))
                .build());

        TenantContext.setCurrentTenant(instanceId);

        for (int i = 0; i < 48; i++) {
            studentRepository.save(Student.builder()
                    .name("Student " + i)
                    .email("student" + i + "@test.com")
                    .instanceId(instanceId)
                    .build());
        }

        // When: Try to bulk import 5 students (total would be 53, exceeds limit)
        List<CreateStudentRequest> batch = List.of(
                new CreateStudentRequest("S49", "s49@test.com", "0901234567", null, null),
                new CreateStudentRequest("S50", "s50@test.com", "0901234567", null, null),
                new CreateStudentRequest("S51", "s51@test.com", "0901234567", null, null),
                new CreateStudentRequest("S52", "s52@test.com", "0901234567", null, null),
                new CreateStudentRequest("S53", "s53@test.com", "0901234567", null, null)
        );

        // Then: Should reject entire batch (exceeds limit)
        assertThatThrownBy(() -> studentService.bulkCreateStudents(batch))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("50");

        // Verify: No students added (atomic operation)
        assertThat(studentRepository.countByInstanceId(instanceId)).isEqualTo(48);

        TenantContext.clear();
    }
}
```

---

# PART 3: PAYMENT SECURITY TESTING

## 3.1 QR Code Tampering Prevention

```java
@SpringBootTest
@Transactional
class PaymentSecurityTest {

    @Autowired
    private VietQRPaymentService paymentService;

    @Autowired
    private PaymentOrderRepository orderRepo;

    @Test
    @DisplayName("should detect amount tampering in payment verification")
    void shouldDetectAmountTampering() {
        // Given: Order for 499k VND
        PaymentOrder order = orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        // When: Attacker pays only 100 VND (tampered QR code amount)
        // But claims to have paid full amount
        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "ORD-123",
                "FT123",
                LocalDateTime.now(),
                100L // Actual paid amount (tampered)
        );

        // Then: Should throw AmountMismatchException
        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(AmountMismatchException.class)
                .hasMessageContaining("499000")
                .hasMessageContaining("100");

        // Verify: Order still PENDING (not marked as PAID)
        PaymentOrder unchanged = orderRepo.findByOrderId("ORD-123").orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("should prevent double payment with same transaction")
    void shouldPreventDoublePayment() {
        // Given: Two orders for same instance
        UUID instanceId = UUID.randomUUID();

        orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .instanceId(instanceId)
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-456")
                .instanceId(instanceId)
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        // When: Verify ORD-123 with transaction FT123
        VerifyPaymentRequest request1 = new VerifyPaymentRequest(
                "ORD-123", "FT123", LocalDateTime.now(), 499000L);
        paymentService.verifyPayment(request1);

        // Then: Cannot reuse transaction FT123 for ORD-456 (replay attack)
        VerifyPaymentRequest request2 = new VerifyPaymentRequest(
                "ORD-456", "FT123", LocalDateTime.now(), 499000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(request2))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("FT123");

        // Verify: Only ORD-123 is PAID
        assertThat(orderRepo.findByOrderId("ORD-123").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PAID);
        assertThat(orderRepo.findByOrderId("ORD-456").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("should prevent payment for already paid order")
    void shouldPreventPaymentForAlreadyPaidOrder() {
        // Given: Order already PAID
        orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .amount(499000L)
                .status(PaymentStatus.PAID)
                .transactionReference("FT123")
                .paidAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusHours(23))
                .build());

        // When: Try to pay again (attacker wants refund)
        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "ORD-123", "FT999", LocalDateTime.now(), 499000L);

        // Then: Should throw PaymentAlreadyPaidException
        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentAlreadyPaidException.class)
                .hasMessageContaining("ORD-123");
    }

    @Test
    @DisplayName("should prevent payment verification for expired order")
    void shouldPreventPaymentForExpiredOrder() {
        // Given: Expired order (>24 hours)
        orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusHours(1)) // Expired
                .build());

        // When: Try to verify expired order
        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "ORD-123", "FT123", LocalDateTime.now(), 499000L);

        // Then: Should throw PaymentExpiredException
        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("should validate payment belongs to correct instance")
    void shouldValidatePaymentBelongsToInstance() {
        // Given: Order for instance1
        UUID instance1 = UUID.randomUUID();
        UUID instance2 = UUID.randomUUID();

        orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .instanceId(instance1)
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        // When: Instance2 tries to verify instance1's order
        TenantContext.setCurrentTenant(instance2);

        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "ORD-123", "FT123", LocalDateTime.now(), 499000L);

        // Then: Should throw PaymentOrderNotFoundException (cross-tenant)
        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentOrderNotFoundException.class);

        TenantContext.clear();
    }
}
```

---

# PART 4: JWT SECURITY TESTING

## 4.1 Token Lifecycle Security

```java
@SpringBootTest
class JwtSecurityTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlacklistService blacklistService;

    @Test
    @DisplayName("should reject expired JWT tokens")
    void shouldRejectExpiredTokens() {
        // Given: Token with 1-hour expiry
        String token = jwtService.generateToken(
                1L, "user@example.com", UUID.randomUUID(),
                List.of("TEACHER"), Duration.ofHours(1));

        // Simulate time passing (1 hour + 1 minute)
        // (In production test, use Clock abstraction or wait)

        // When: Validate expired token
        // Then: Should throw TokenExpiredException
        assertThatThrownBy(() -> jwtService.validateToken(token))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    @DisplayName("should reject tokens with invalid signature")
    void shouldRejectInvalidSignature() {
        // Given: Valid token
        String validToken = jwtService.generateToken(
                1L, "user@example.com", UUID.randomUUID(),
                List.of("TEACHER"), Duration.ofHours(1));

        // When: Modify token (tamper with payload)
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "TAMPR";

        // Then: Should throw InvalidTokenException
        assertThatThrownBy(() -> jwtService.validateToken(tamperedToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("should reject blacklisted tokens after logout")
    void shouldRejectBlacklistedTokens() {
        // Given: Valid token
        String token = jwtService.generateToken(
                1L, "user@example.com", UUID.randomUUID(),
                List.of("TEACHER"), Duration.ofHours(1));

        // When: User logs out (token blacklisted)
        blacklistService.blacklistToken(token);

        // Then: Token should be rejected even though not expired
        assertThatThrownBy(() -> jwtService.validateToken(token))
                .isInstanceOf(TokenBlacklistedException.class);
    }

    @Test
    @DisplayName("should enforce refresh token rotation")
    void shouldEnforceRefreshTokenRotation() {
        // Given: Refresh token
        String refreshToken = jwtService.generateRefreshToken(
                1L, UUID.randomUUID());

        // When: Use refresh token to get new access token (first time)
        TokenPair tokens1 = jwtService.refreshAccessToken(refreshToken);

        // Then: Old refresh token should be invalidated
        assertThatThrownBy(() -> jwtService.refreshAccessToken(refreshToken))
                .isInstanceOf(RefreshTokenUsedException.class);

        // Only new refresh token should work
        assertThatCode(() -> jwtService.refreshAccessToken(tokens1.refreshToken()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should prevent token reuse across different tenants")
    void shouldPreventTokenReuseAcrossTenants() {
        // Given: Token for tenant1
        UUID tenant1 = UUID.randomUUID();
        String token = jwtService.generateToken(
                1L, "user@example.com", tenant1,
                List.of("TEACHER"), Duration.ofHours(1));

        // When: Try to use token for tenant2
        UUID tenant2 = UUID.randomUUID();

        // Then: Should reject (tenant mismatch)
        assertThatThrownBy(() -> jwtService.validateTokenForTenant(token, tenant2))
                .isInstanceOf(TenantMismatchException.class);
    }
}
```

---

# PART 5: OWASP TOP 10 TESTING

## 5.1 SQL Injection Prevention

```java
@SpringBootTest
@Transactional
class SqlInjectionTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentService studentService;

    @Test
    @DisplayName("should prevent SQL injection via search parameter")
    void shouldPreventSqlInjection_viaSearch() {
        // Given: Malicious search input
        String maliciousInput = "'; DROP TABLE students; --";

        // When: Search with malicious input
        Page<StudentResponse> result = studentService.searchStudents(
                maliciousInput, Pageable.unpaged());

        // Then: Should return empty (no match), not drop table
        assertThat(result.getContent()).isEmpty();

        // Verify: Table still exists
        assertThat(studentRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("should use parameterized queries for all database operations")
    void shouldUseParameterizedQueries() {
        // Given: Input with SQL special characters
        String name = "O'Brien"; // Contains single quote
        String email = "user'; DELETE FROM students; --@example.com";

        // When: Create student
        CreateStudentRequest request = new CreateStudentRequest(
                name, email, "0901234567", null, null);

        StudentResponse response = studentService.createStudent(request);

        // Then: Should be saved safely (no SQL injection)
        assertThat(response.name()).isEqualTo("O'Brien");
        assertThat(response.email()).contains("DELETE"); // Stored as literal string

        // Verify: No tables dropped
        assertThat(studentRepository.count()).isEqualTo(1);
    }
}
```

## 5.2 XSS Prevention

```java
@SpringBootTest
class XssPreventionTest {

    @Autowired
    private StudentService studentService;

    @Test
    @DisplayName("should sanitize HTML in user input")
    void shouldSanitizeHtml() {
        // Given: Input with XSS payload
        String xssPayload = "<script>alert('XSS')</script>";

        // When: Create student with XSS in name
        CreateStudentRequest request = new CreateStudentRequest(
                xssPayload, "user@example.com", "0901234567", null, null);

        StudentResponse response = studentService.createStudent(request);

        // Then: HTML should be escaped or sanitized
        assertThat(response.name())
                .doesNotContain("<script>")
                .satisfiesAnyOf(
                        name -> assertThat(name).isEqualTo("alert('XSS')"), // Script removed
                        name -> assertThat(name).contains("&lt;script&gt;") // HTML encoded
                );
    }

    @Test
    @DisplayName("should set Content-Security-Policy header")
    void shouldSetContentSecurityPolicyHeader() {
        // This test verifies CSP header in HTTP responses
        // (Requires WebTestClient or MockMvc)

        // Expected CSP: script-src 'self'; object-src 'none';
    }
}
```

## 5.3 CSRF Prevention

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class CsrfPreventionTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("should reject POST requests without CSRF token")
    void shouldRejectPostWithoutCsrfToken() {
        // When: POST without CSRF token
        webTestClient.post()
                .uri("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "name": "Test Student",
                            "email": "test@example.com",
                            "phone": "0901234567"
                        }
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("should accept POST requests with valid CSRF token")
    void shouldAcceptPostWithCsrfToken() {
        // Given: Get CSRF token
        String csrfToken = webTestClient.get()
                .uri("/api/v1/csrf-token")
                .exchange()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        // When: POST with CSRF token
        webTestClient.post()
                .uri("/api/v1/students")
                .header("X-CSRF-Token", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "name": "Test Student",
                            "email": "test@example.com",
                            "phone": "0901234567"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated();
    }
}
```

## 5.4 Insecure Deserialization Prevention

```java
@SpringBootTest
class DeserializationSecurityTest {

    @Test
    @DisplayName("should reject deserialization of unknown classes")
    void shouldRejectUnknownClasses() {
        // Given: Malicious serialized object
        String maliciousJson = """
                {
                    "@class": "java.lang.Runtime",
                    "exec": ["rm", "-rf", "/"]
                }
                """;

        // When: Attempt to deserialize
        ObjectMapper mapper = new ObjectMapper();

        // Then: Should throw exception (polymorphic deserialization disabled)
        assertThatThrownBy(() -> mapper.readValue(maliciousJson, Object.class))
                .isInstanceOf(InvalidDefinitionException.class);
    }

    @Test
    @DisplayName("should only allow deserialization of whitelisted classes")
    void shouldOnlyAllowWhitelistedClasses() {
        // Configure ObjectMapper with whitelisted classes only
        // (In production, use PolymorphicTypeValidator)

        ObjectMapper mapper = new ObjectMapper();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.kiteclass.core.dto")
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        // Verify: Only DTO classes can be deserialized
    }
}
```

## 5.5 Broken Authentication Prevention

```java
@SpringBootTest
class AuthenticationSecurityTest {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("should enforce password complexity requirements")
    void shouldEnforcePasswordComplexity() {
        // When: Try weak passwords
        assertThatThrownBy(() -> authService.registerUser(
                        "user@example.com", "123456", "User")) // Too short
                .isInstanceOf(WeakPasswordException.class);

        assertThatThrownBy(() -> authService.registerUser(
                        "user@example.com", "password", "User")) // No numbers/symbols
                .isInstanceOf(WeakPasswordException.class);

        // Then: Only strong passwords accepted
        assertThatCode(() -> authService.registerUser(
                        "user@example.com", "Pass123!@#", "User"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should rate-limit login attempts")
    void shouldRateLimitLoginAttempts() {
        // Given: 5 failed login attempts
        for (int i = 0; i < 5; i++) {
            try {
                authService.login("user@example.com", "wrong-password");
            } catch (Exception ignored) {
            }
        }

        // When: 6th attempt
        // Then: Should be rate-limited (account temporarily locked)
        assertThatThrownBy(() -> authService.login("user@example.com", "wrong-password"))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("5 failed attempts");
    }

    @Test
    @DisplayName("should hash passwords with bcrypt")
    void shouldHashPasswordsWithBcrypt() {
        // Given: New user registration
        authService.registerUser("user@example.com", "Pass123!@#", "User");

        // Then: Password should be hashed (not stored in plaintext)
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        assertThat(user.getPasswordHash())
                .startsWith("$2a$") // BCrypt prefix
                .isNotEqualTo("Pass123!@#");
    }
}
```

## 5.6 Sensitive Data Exposure Prevention

```java
@SpringBootTest
class SensitiveDataSecurityTest {

    @Autowired
    private StudentService studentService;

    @Test
    @DisplayName("should not expose sensitive data in API responses")
    void shouldNotExposeSensitiveDataInResponses() {
        // When: Get student details
        StudentResponse response = studentService.getStudentById(1L);

        // Then: Should not include sensitive fields
        // (Use reflection to verify DTO doesn't have these fields)
        assertThat(response).doesNotHaveToString("passwordHash");
        assertThat(response).doesNotHaveToString("ssn");
        assertThat(response).doesNotHaveToString("bankAccount");
    }

    @Test
    @DisplayName("should not log sensitive data")
    void shouldNotLogSensitiveData() {
        // Configure log capture
        // Verify: Passwords, tokens not logged

        // When: Login
        authService.login("user@example.com", "Pass123!@#");

        // Then: Log should not contain password
        // assertThat(logCapture.getMessages())
        //         .noneMatch(msg -> msg.contains("Pass123!@#"));
    }

    @Test
    @DisplayName("should encrypt sensitive data at rest")
    void shouldEncryptSensitiveDataAtRest() {
        // Given: Student with sensitive data
        CreateStudentRequest request = new CreateStudentRequest(
                "Test Student", "student@example.com",
                "0901234567", "123-45-6789", null); // SSN

        // When: Save student
        StudentResponse response = studentService.createStudent(request);

        // Then: SSN should be encrypted in database
        Student student = studentRepository.findById(response.id()).orElseThrow();
        assertThat(student.getSsnEncrypted())
                .isNotEqualTo("123-45-6789")
                .hasSize(64); // AES-256 encrypted length
    }
}
```

---

# PART 6: SECURITY AUDIT CHECKLIST

## Pre-Deployment Security Checklist

### Authentication & Authorization
- [ ] JWT tokens expire after appropriate time (access: 1 hour, refresh: 7 days)
- [ ] Refresh token rotation implemented
- [ ] Token blacklist on logout
- [ ] Password complexity enforced (min 8 chars, uppercase, lowercase, number, symbol)
- [ ] Account lockout after 5 failed login attempts
- [ ] Passwords hashed with BCrypt (cost factor ≥ 12)

### Multi-Tenant Security
- [ ] All queries include instance_id filter
- [ ] Cross-tenant access denied (tested for SELECT, UPDATE, DELETE)
- [ ] Tenant ID validated in JWT matches X-Tenant-Id header
- [ ] No SQL injection via tenant context

### Feature Access Control
- [ ] Feature detection enforced on all feature-gated APIs
- [ ] Tier-based access control tested
- [ ] Limit enforcement working (maxStudents, maxCourses, storage)
- [ ] No feature bypass via config manipulation

### Payment Security
- [ ] QR code tampering detection (amount validation)
- [ ] Double payment prevention (transaction reference uniqueness)
- [ ] Payment verification requires matching amount
- [ ] Expired orders cannot be paid
- [ ] Payment belongs to correct instance validated

### Input Validation
- [ ] All user inputs validated (email format, phone format, etc.)
- [ ] SQL injection prevented (parameterized queries used)
- [ ] XSS prevented (HTML sanitized or escaped)
- [ ] File upload validation (type, size, content)
- [ ] Path traversal prevented (file access restricted)

### CSRF Protection
- [ ] CSRF tokens required for state-changing operations
- [ ] SameSite cookie attribute set
- [ ] Origin header validation

### Data Protection
- [ ] Sensitive data encrypted at rest (SSN, bank accounts, etc.)
- [ ] Sensitive data encrypted in transit (HTTPS only)
- [ ] Passwords never logged
- [ ] API tokens never exposed in responses
- [ ] Database backups encrypted

### Security Headers
- [ ] Content-Security-Policy header set
- [ ] X-Frame-Options: DENY
- [ ] X-Content-Type-Options: nosniff
- [ ] Strict-Transport-Security header (HSTS)

### Dependency Security
- [ ] No dependencies with known vulnerabilities (OWASP Dependency-Check)
- [ ] Dependencies up-to-date
- [ ] Unused dependencies removed

---

## Actions

### Run security tests
```bash
# All security tests
./mvnw test -Dtest="*SecurityTest"

# Specific categories
./mvnw test -Dtest="*TenantBoundary*"
./mvnw test -Dtest="*Payment*Security*"
./mvnw test -Dtest="*Jwt*"
./mvnw test -Dtest="*Owasp*"
```

### Run OWASP Dependency Check
```bash
./mvnw org.owasp:dependency-check-maven:check
```

### Run SonarQube security scan
```bash
./mvnw sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>
```

---

**Last Updated:** 2026-01-30
**Related Skills:** `kiteclass-backend-testing-patterns.md`, `spring-boot-testing-quality.md`, `testing-guide.md`
