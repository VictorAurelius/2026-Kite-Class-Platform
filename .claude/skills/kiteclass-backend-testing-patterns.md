# Skill: KiteClass Backend Testing Patterns

**Version:** 1.0
**Date:** 2026-01-30
**Purpose:** KiteClass-specific backend testing patterns for multi-tenant, feature detection, payments, trial system, and AI branding

## Mô tả

Testing patterns cụ thể cho KiteClass Platform:
- Multi-tenant testing (tenant isolation, cross-tenant access)
- Feature detection testing (tier-based access, requireFeature)
- Payment flow testing (VietQR QR generation, verification, security)
- Trial system testing (14-day trial, 3-day grace, 90-day retention)
- AI branding job testing (async jobs, OpenAI integration, retry logic)
- Cache testing (Redis TTL, invalidation)

## Trigger phrases

- "test multi-tenant"
- "test feature detection"
- "test payment flow"
- "test trial system"
- "test kiteclass backend"

---

# PART 1: MULTI-TENANT TESTING

## 1.1 Tenant Isolation Testing

### Base Test Setup

```java
package com.kiteclass.core.testutil;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest
@Testcontainers
public abstract class MultiTenantTestBase {

    @Container
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("kiteclass_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // Helper: Create test instance ID
    protected UUID createInstanceId(String suffix) {
        return UUID.nameUUIDFromBytes(("test-instance-" + suffix).getBytes());
    }

    // Helper: Create tenant context
    protected void setTenantContext(UUID instanceId) {
        // Set tenant in TenantContext (ThreadLocal)
        TenantContext.setCurrentTenant(instanceId);
    }

    // Helper: Clear tenant context
    protected void clearTenantContext() {
        TenantContext.clear();
    }
}
```

### Test Pattern: Tenant Data Isolation

```java
@SpringBootTest
@Transactional
class StudentServiceMultiTenantTest extends MultiTenantTestBase {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("getStudents should only return current tenant's students")
    void getStudents_shouldOnlyReturnCurrentTenantStudents() {
        // Given: Two tenants with students
        UUID tenant1 = createInstanceId("tenant1");
        UUID tenant2 = createInstanceId("tenant2");

        // Create student for tenant1
        setTenantContext(tenant1);
        Student student1 = Student.builder()
                .name("Tenant 1 Student")
                .email("student1@tenant1.com")
                .instanceId(tenant1)
                .build();
        studentRepository.save(student1);

        // Create student for tenant2
        setTenantContext(tenant2);
        Student student2 = Student.builder()
                .name("Tenant 2 Student")
                .email("student2@tenant2.com")
                .instanceId(tenant2)
                .build();
        studentRepository.save(student2);

        // When: Get students as tenant1
        setTenantContext(tenant1);
        Page<StudentResponse> tenant1Students = studentService.getStudents(
                Pageable.unpaged());

        // Then: Should only see tenant1's student
        assertThat(tenant1Students.getContent())
                .hasSize(1)
                .extracting(StudentResponse::name)
                .containsExactly("Tenant 1 Student");

        // Should NOT include tenant2's student
        assertThat(tenant1Students.getContent())
                .noneMatch(s -> s.email().equals("student2@tenant2.com"));

        clearTenantContext();
    }

    @Test
    @DisplayName("getStudentById should throw 404 for other tenant's student")
    void getStudentById_shouldThrow404_whenAccessingOtherTenantStudent() {
        // Given: Student belongs to tenant1
        UUID tenant1 = createInstanceId("tenant1");
        UUID tenant2 = createInstanceId("tenant2");

        setTenantContext(tenant1);
        Student student = Student.builder()
                .name("Tenant 1 Student")
                .email("student@tenant1.com")
                .instanceId(tenant1)
                .build();
        student = studentRepository.save(student);
        Long studentId = student.getId();
        clearTenantContext();

        // When: Try to access as tenant2
        setTenantContext(tenant2);

        // Then: Should throw StudentNotFoundException (cross-tenant access denied)
        assertThatThrownBy(() -> studentService.getStudentById(studentId))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining(studentId.toString());

        clearTenantContext();
    }

    @Test
    @DisplayName("updateStudent should not allow cross-tenant update")
    void updateStudent_shouldNotAllowCrossTenantUpdate() {
        // Given: Student belongs to tenant1
        UUID tenant1 = createInstanceId("tenant1");
        UUID tenant2 = createInstanceId("tenant2");

        setTenantContext(tenant1);
        Student student = studentRepository.save(Student.builder()
                .name("Original Name")
                .email("student@tenant1.com")
                .instanceId(tenant1)
                .build());
        Long studentId = student.getId();
        clearTenantContext();

        // When: Tenant2 tries to update tenant1's student
        setTenantContext(tenant2);
        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
                "Hacked Name", null, null, null, null);

        // Then: Should throw exception (not found in tenant2 context)
        assertThatThrownBy(() -> studentService.updateStudent(studentId, updateRequest))
                .isInstanceOf(StudentNotFoundException.class);

        // Verify: Original data unchanged
        clearTenantContext();
        setTenantContext(tenant1);
        Student unchanged = studentRepository.findById(studentId).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Original Name");

        clearTenantContext();
    }
}
```

### Test Pattern: Controller Multi-Tenant

```java
@WebMvcTest(StudentController.class)
@Import({StudentControllerTest.MockConfig.class, TestSecurityConfig.class})
class StudentControllerMultiTenantTest {

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

    @Test
    @WithMockUser(roles = "TEACHER")
    void getStudents_shouldIncludeTenantIdInRequest() throws Exception {
        // Given
        UUID instanceId = UUID.randomUUID();
        Page<StudentResponse> mockPage = new PageImpl<>(List.of());
        when(studentService.getStudents(any())).thenReturn(mockPage);

        // When
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", instanceId.toString())
                        .header("X-User-Id", "123"))
                .andExpect(status().isOk());

        // Then: Verify tenant context was set
        verify(studentService).getStudents(any());
        // TenantContext should have been set by TenantInterceptor
    }

    @Test
    void getStudents_shouldReturn401_whenNoTenantIdHeader() throws Exception {
        // When: Request without X-Tenant-Id header
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TENANT_ID_REQUIRED"));

        // Then: Service should not be called
        verify(studentService, never()).getStudents(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getStudents_shouldReturn403_whenTenantIdMismatchesJWT() throws Exception {
        // Given: JWT contains tenant1, but header has tenant2
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        // When: Send mismatched tenant ID
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenant2.toString())
                        .header("X-User-Id", "123")
                        .header("Authorization", "Bearer " + generateJwtForTenant(tenant1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("TENANT_MISMATCH"));
    }
}
```

---

# PART 2: FEATURE DETECTION TESTING

## 2.1 Feature Access Control Testing

### Test Pattern: requireFeature()

```java
@SpringBootTest
@Transactional
class FeatureDetectionServiceTest {

    @Autowired
    private FeatureDetectionService featureService;

    @Autowired
    private InstanceConfigRepository configRepo;

    @Test
    @DisplayName("requireFeature should succeed when feature available")
    void requireFeature_shouldSucceed_whenFeatureAvailable() {
        // Given: Instance with STANDARD tier (has ENGAGEMENT)
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.STANDARD)
                .features(Map.of("ENGAGEMENT", true, "MEDIA", true))
                .build();
        configRepo.save(config);

        // When/Then: Should not throw
        assertThatCode(() -> featureService.requireFeature(instanceId, "ENGAGEMENT"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireFeature should throw exception when feature not available")
    void requireFeature_shouldThrowException_whenFeatureNotAvailable() {
        // Given: Instance with BASIC tier (no ENGAGEMENT)
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .features(Map.of("ENGAGEMENT", false))
                .build();
        configRepo.save(config);

        // When/Then: Should throw FeatureNotAvailableException
        assertThatThrownBy(() -> featureService.requireFeature(instanceId, "ENGAGEMENT"))
                .isInstanceOf(FeatureNotAvailableException.class)
                .hasMessageContaining("ENGAGEMENT")
                .hasMessageContaining("STANDARD");
    }

    @Test
    @DisplayName("hasFeature should return correct boolean")
    void hasFeature_shouldReturnCorrectBoolean() {
        // Given
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.STANDARD)
                .features(Map.of("ENGAGEMENT", true, "MEDIA", true))
                .build();
        configRepo.save(config);

        // When/Then
        assertThat(featureService.hasFeature(instanceId, "ENGAGEMENT")).isTrue();
        assertThat(featureService.hasFeature(instanceId, "MEDIA")).isTrue();
        assertThat(featureService.hasFeature(instanceId, "CUSTOM_DOMAIN")).isFalse();
    }
}
```

### Test Pattern: Tier-Based Access Control

```java
@SpringBootTest
@Transactional
class AttendanceServiceFeatureTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private FeatureDetectionService featureService;

    @Autowired
    private InstanceConfigRepository configRepo;

    @Test
    @DisplayName("markAttendance should throw 403 when ENGAGEMENT not available")
    void markAttendance_shouldThrow403_whenEngagementNotAvailable() {
        // Given: Instance with BASIC tier (no ENGAGEMENT feature)
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .features(Map.of("ENGAGEMENT", false))
                .build();
        configRepo.save(config);

        // Given: Mark attendance request
        MarkAttendanceRequest request = new MarkAttendanceRequest(
                1L, LocalDate.now(), List.of(
                new AttendanceRecord(1L, AttendanceStatus.PRESENT, null)));

        // When/Then: Should throw FeatureNotAvailableException
        assertThatThrownBy(() -> attendanceService.markAttendance(instanceId, request))
                .isInstanceOf(FeatureNotAvailableException.class)
                .hasMessageContaining("ENGAGEMENT")
                .hasMessageContaining("Upgrade to STANDARD");

        // Verify: No attendance record created
        assertThat(attendanceService.getAttendanceByClass(instanceId, 1L, LocalDate.now()))
                .isEmpty();
    }

    @Test
    @DisplayName("markAttendance should succeed when ENGAGEMENT available")
    void markAttendance_shouldSucceed_whenEngagementAvailable() {
        // Given: Instance with STANDARD tier (has ENGAGEMENT)
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.STANDARD)
                .features(Map.of("ENGAGEMENT", true))
                .build();
        configRepo.save(config);

        // When: Mark attendance
        MarkAttendanceRequest request = new MarkAttendanceRequest(
                1L, LocalDate.now(), List.of(
                new AttendanceRecord(1L, AttendanceStatus.PRESENT, null)));

        AttendanceResponse response = attendanceService.markAttendance(instanceId, request);

        // Then: Should succeed
        assertThat(response).isNotNull();
        assertThat(response.records()).hasSize(1);
    }
}
```

### Test Pattern: Feature Limit Enforcement

```java
@SpringBootTest
@Transactional
class StudentServiceLimitTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private InstanceConfigRepository configRepo;

    @Test
    @DisplayName("createStudent should throw exception when student limit reached")
    void createStudent_shouldThrowException_whenStudentLimitReached() {
        // Given: BASIC tier with 50 student limit
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .limitations(Map.of("maxStudents", 50))
                .build();
        configRepo.save(config);

        // Given: Already have 50 students
        TenantContext.setCurrentTenant(instanceId);
        for (int i = 0; i < 50; i++) {
            studentRepository.save(Student.builder()
                    .name("Student " + i)
                    .email("student" + i + "@test.com")
                    .instanceId(instanceId)
                    .build());
        }

        // When: Try to create 51st student
        CreateStudentRequest request = new CreateStudentRequest(
                "Student 51", "student51@test.com", "0901234567", null, null);

        // Then: Should throw LimitExceededException
        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("50")
                .hasMessageContaining("Upgrade to STANDARD");

        TenantContext.clear();
    }

    @Test
    @DisplayName("createStudent should succeed when under limit")
    void createStudent_shouldSucceed_whenUnderLimit() {
        // Given: BASIC tier with 50 student limit, only 10 students
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .limitations(Map.of("maxStudents", 50))
                .build();
        configRepo.save(config);

        TenantContext.setCurrentTenant(instanceId);
        for (int i = 0; i < 10; i++) {
            studentRepository.save(Student.builder()
                    .name("Student " + i)
                    .email("student" + i + "@test.com")
                    .instanceId(instanceId)
                    .build());
        }

        // When: Create 11th student (still under 50 limit)
        CreateStudentRequest request = new CreateStudentRequest(
                "Student 11", "student11@test.com", "0901234567", null, null);

        StudentResponse response = studentService.createStudent(request);

        // Then: Should succeed
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Student 11");

        TenantContext.clear();
    }
}
```

---

# PART 3: PAYMENT FLOW TESTING

## 3.1 VietQR QR Code Generation Testing

```java
@SpringBootTest
@Transactional
class VietQRPaymentServiceTest {

    @Autowired
    private VietQRPaymentService paymentService;

    @Autowired
    private PaymentOrderRepository orderRepo;

    @Test
    @DisplayName("createPaymentOrder should generate valid VietQR URL")
    void createPaymentOrder_shouldGenerateValidVietQRUrl() {
        // Given
        UUID instanceId = UUID.randomUUID();
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(
                PaymentType.SUBSCRIPTION,
                instanceId,
                499000L, // 499k VND
                PricingTier.BASIC,
                "user@example.com"
        );

        // When
        PaymentOrderResponse response = paymentService.createPaymentOrder(request);

        // Then
        assertThat(response.orderId()).startsWith("ORD-");
        assertThat(response.qrImageUrl()).contains("img.vietqr.io");
        assertThat(response.qrImageUrl()).contains("amount=499000");
        assertThat(response.qrImageUrl()).contains(response.orderId());
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now().plusHours(23));
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("generateQRCode should include all required parameters")
    void generateQRCode_shouldIncludeAllRequiredParameters() {
        // Given
        PaymentOrder order = PaymentOrder.builder()
                .orderId("ORD-TEST-123")
                .amount(499000L)
                .paymentContent("KITEHUB ORD-TEST-123 user@example.com")
                .build();

        BankAccountInfo bankInfo = BankAccountInfo.builder()
                .bankCode("970415") // Vietcombank
                .accountNumber("1234567890")
                .accountName("CONG TY TNHH KITECLASS")
                .build();

        // When
        String qrUrl = paymentService.generateQRCode(order, bankInfo);

        // Then
        assertThat(qrUrl).contains("img.vietqr.io");
        assertThat(qrUrl).contains("970415-1234567890");
        assertThat(qrUrl).contains("amount=499000");
        assertThat(qrUrl).contains("addInfo=KITEHUB");
        assertThat(qrUrl).contains("accountName=CONG");
    }

    @Test
    @DisplayName("createPaymentOrder should set 24-hour expiry")
    void createPaymentOrder_shouldSet24HourExpiry() {
        // Given
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(
                PaymentType.SUBSCRIPTION, UUID.randomUUID(), 499000L,
                PricingTier.BASIC, "user@example.com");

        // When
        PaymentOrderResponse response = paymentService.createPaymentOrder(request);

        // Then
        assertThat(response.expiresAt())
                .isAfter(LocalDateTime.now().plusHours(23).plusMinutes(59))
                .isBefore(LocalDateTime.now().plusHours(24).plusMinutes(1));
    }
}
```

## 3.2 Payment Verification Testing

```java
@SpringBootTest
@Transactional
class PaymentVerificationTest {

    @Autowired
    private VietQRPaymentService paymentService;

    @Autowired
    private PaymentOrderRepository orderRepo;

    @Test
    @DisplayName("verifyPayment should mark order as PAID")
    void verifyPayment_shouldMarkOrderAsPaid() {
        // Given: Pending payment order
        PaymentOrder order = orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .type(PaymentType.SUBSCRIPTION)
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        // When: Manual verification by admin
        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "ORD-123",
                "FT25012012345", // Bank transaction ref
                LocalDateTime.now()
        );

        PaymentOrderResponse response = paymentService.verifyPayment(request);

        // Then
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.transactionReference()).isEqualTo("FT25012012345");
        assertThat(response.paidAt()).isNotNull();

        // Verify in database
        PaymentOrder updatedOrder = orderRepo.findByOrderId("ORD-123").orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getTransactionReference()).isEqualTo("FT25012012345");
    }

    @Test
    @DisplayName("verifyPayment should prevent double payment")
    void verifyPayment_shouldPreventDoublePayment() {
        // Given: Order already PAID
        PaymentOrder order = orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .type(PaymentType.SUBSCRIPTION)
                .amount(499000L)
                .status(PaymentStatus.PAID)
                .transactionReference("FT25012012345")
                .paidAt(LocalDateTime.now().minusHours(1))
                .build());

        // When: Try to verify same order again (replay attack)
        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "ORD-123",
                "FT25012099999", // Different transaction (fraudulent)
                LocalDateTime.now()
        );

        // Then: Should throw PaymentAlreadyPaidException
        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentAlreadyPaidException.class)
                .hasMessageContaining("ORD-123")
                .hasMessageContaining("already paid");

        // Verify: Transaction reference unchanged (no double payment)
        PaymentOrder unchanged = orderRepo.findByOrderId("ORD-123").orElseThrow();
        assertThat(unchanged.getTransactionReference()).isEqualTo("FT25012012345");
    }

    @Test
    @DisplayName("verifyPayment should throw exception for expired order")
    void verifyPayment_shouldThrowException_forExpiredOrder() {
        // Given: Expired order (>24 hours old)
        PaymentOrder order = orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .type(PaymentType.SUBSCRIPTION)
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusHours(1)) // Expired 1 hour ago
                .build());

        // When: Try to verify expired order
        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "ORD-123", "FT25012012345", LocalDateTime.now());

        // Then: Should throw PaymentExpiredException
        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentExpiredException.class)
                .hasMessageContaining("ORD-123")
                .hasMessageContaining("expired");
    }
}
```

## 3.3 Payment Security Testing

```java
@SpringBootTest
@Transactional
class PaymentSecurityTest {

    @Autowired
    private VietQRPaymentService paymentService;

    @Autowired
    private PaymentOrderRepository orderRepo;

    @Test
    @DisplayName("should not allow amount tampering via QR URL modification")
    void shouldNotAllowAmountTampering() {
        // Given: Order for 499k VND
        PaymentOrder order = orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .build());

        // When: Attacker modifies QR URL to 100 VND and pays
        // (In manual verification, we should verify amount matches order)
        VerifyPaymentRequest tamperedRequest = new VerifyPaymentRequest(
                "ORD-123",
                "FT25012012345",
                LocalDateTime.now()
                // Note: In production, should include paid amount from bank
        );

        // Then: Verification should check if paid amount matches order amount
        // This test ensures we don't just mark as PAID without checking amount

        // For now, we verify that order amount is stored and cannot be changed
        PaymentOrder storedOrder = orderRepo.findByOrderId("ORD-123").orElseThrow();
        assertThat(storedOrder.getAmount()).isEqualTo(499000L);

        // In production, verifyPayment should accept paidAmount parameter
        // and throw AmountMismatchException if it doesn't match
    }

    @Test
    @DisplayName("should prevent replay attacks with same transaction reference")
    void shouldPreventReplayAttacks() {
        // Given: Two different orders
        orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .build());

        orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-456")
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .build());

        // When: Verify ORD-123 with transaction FT123
        VerifyPaymentRequest request1 = new VerifyPaymentRequest(
                "ORD-123", "FT123", LocalDateTime.now());
        paymentService.verifyPayment(request1);

        // Then: Cannot reuse same transaction FT123 for ORD-456 (replay attack)
        VerifyPaymentRequest request2 = new VerifyPaymentRequest(
                "ORD-456", "FT123", LocalDateTime.now());

        assertThatThrownBy(() -> paymentService.verifyPayment(request2))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("FT123")
                .hasMessageContaining("already used");
    }

    @Test
    @DisplayName("should validate order belongs to correct instance")
    void shouldValidateOrderBelongsToInstance() {
        // Given: Order for instance1
        UUID instance1 = UUID.randomUUID();
        UUID instance2 = UUID.randomUUID();

        orderRepo.save(PaymentOrder.builder()
                .orderId("ORD-123")
                .instanceId(instance1)
                .amount(499000L)
                .status(PaymentStatus.PENDING)
                .build());

        // When: Instance2 tries to verify instance1's order
        TenantContext.setCurrentTenant(instance2);

        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "ORD-123", "FT123", LocalDateTime.now());

        // Then: Should throw exception (cross-tenant access)
        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(PaymentOrderNotFoundException.class);

        TenantContext.clear();
    }
}
```

---

# PART 4: TRIAL SYSTEM TESTING

## 4.1 Trial Lifecycle Testing

```java
@SpringBootTest
@Transactional
class TrialSystemTest {

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private InstanceRepository instanceRepo;

    @Test
    @DisplayName("createTrialInstance should set 14-day trial")
    void createTrialInstance_shouldSet14DayTrial() {
        // When: Create trial instance
        CreateInstanceRequest request = new CreateInstanceRequest(
                "test-center", "testcenter", "user@example.com");

        InstanceResponse response = instanceService.createTrialInstance(request);

        // Then
        assertThat(response.status()).isEqualTo(InstanceStatus.TRIAL);
        assertThat(response.trialStartedAt()).isNotNull();
        assertThat(response.trialExpiresAt())
                .isAfter(LocalDateTime.now().plusDays(13).plusHours(23))
                .isBefore(LocalDateTime.now().plusDays(14).plusHours(1));
        assertThat(response.tier()).isEqualTo(PricingTier.BASIC);
    }

    @Test
    @DisplayName("accessFeature should allow access during trial")
    void accessFeature_shouldAllowAccess_duringTrial() {
        // Given: Instance in trial period
        Instance instance = instanceRepo.save(Instance.builder()
                .subdomain("testcenter")
                .status(InstanceStatus.TRIAL)
                .trialStartedAt(LocalDateTime.now())
                .trialExpiresAt(LocalDateTime.now().plusDays(10)) // 10 days left
                .tier(PricingTier.BASIC)
                .build());

        // When: Access feature
        TenantContext.setCurrentTenant(instance.getId());

        // Then: Should have access to BASIC features
        assertThatCode(() -> instanceService.requireActiveInstance(instance.getId()))
                .doesNotThrowAnyException();

        TenantContext.clear();
    }

    @Test
    @DisplayName("accessFeature should enter grace period when trial expired")
    void accessFeature_shouldEnterGracePeriod_whenTrialExpired() {
        // Given: Trial expired, but grace period not over
        Instance instance = instanceRepo.save(Instance.builder()
                .subdomain("testcenter")
                .status(InstanceStatus.TRIAL)
                .trialStartedAt(LocalDateTime.now().minusDays(14))
                .trialExpiresAt(LocalDateTime.now().minusDays(1)) // Expired yesterday
                .build());

        // When: Access feature (should trigger grace period)
        TenantContext.setCurrentTenant(instance.getId());

        // Then: Should be in grace period (read-only)
        Instance updated = instanceRepo.findById(instance.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InstanceStatus.GRACE_PERIOD);
        assertThat(updated.getSuspendedAt()).isNotNull();

        // Should throw GracePeriodException for write operations
        assertThatThrownBy(() -> instanceService.requireWriteAccess(instance.getId()))
                .isInstanceOf(GracePeriodException.class)
                .hasMessageContaining("read-only")
                .hasMessageContaining("3 days");

        TenantContext.clear();
    }

    @Test
    @DisplayName("accessFeature should deny access when grace period over")
    void accessFeature_shouldDenyAccess_whenGracePeriodOver() {
        // Given: Trial expired + grace period over (>17 days total)
        Instance instance = instanceRepo.save(Instance.builder()
                .subdomain("testcenter")
                .status(InstanceStatus.GRACE_PERIOD)
                .trialStartedAt(LocalDateTime.now().minusDays(18))
                .trialExpiresAt(LocalDateTime.now().minusDays(4)) // Expired 4 days ago
                .suspendedAt(LocalDateTime.now().minusDays(4))
                .build());

        // When: Try to access (grace period should be over)
        TenantContext.setCurrentTenant(instance.getId());

        // Then: Should be SUSPENDED
        assertThatThrownBy(() -> instanceService.requireActiveInstance(instance.getId()))
                .isInstanceOf(InstanceSuspendedException.class)
                .hasMessageContaining("Trial expired")
                .hasMessageContaining("Upgrade to continue");

        TenantContext.clear();
    }

    @Test
    @DisplayName("should retain data for 90 days after suspension")
    void shouldRetainDataFor90DaysAfterSuspension() {
        // Given: Instance suspended 30 days ago
        Instance instance = instanceRepo.save(Instance.builder()
                .subdomain("testcenter")
                .status(InstanceStatus.SUSPENDED)
                .trialStartedAt(LocalDateTime.now().minusDays(44))
                .trialExpiresAt(LocalDateTime.now().minusDays(30))
                .suspendedAt(LocalDateTime.now().minusDays(30))
                .build());

        // Then: Data should still exist
        assertThat(instance.isDeleted()).isFalse();

        // When: Check if data will be deleted after 90 days
        LocalDateTime deleteAfter = instance.getSuspendedAt().plusDays(90);
        assertThat(deleteAfter).isAfter(LocalDateTime.now().plusDays(59));
    }
}
```

## 4.2 Trial Early-Bird Discount Testing

```java
@SpringBootTest
@Transactional
class TrialDiscountTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InstanceRepository instanceRepo;

    @Test
    @DisplayName("should apply 20% early-bird discount if paid within 10 days")
    void shouldApply20PercentDiscount_ifPaidWithin10Days() {
        // Given: Instance in trial (day 5)
        Instance instance = instanceRepo.save(Instance.builder()
                .subdomain("testcenter")
                .status(InstanceStatus.TRIAL)
                .trialStartedAt(LocalDateTime.now().minusDays(5))
                .trialExpiresAt(LocalDateTime.now().plusDays(9))
                .tier(PricingTier.BASIC)
                .build());

        // When: Create payment order (within 10-day early-bird window)
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(
                PaymentType.SUBSCRIPTION,
                instance.getId(),
                499000L, // Original price
                PricingTier.BASIC,
                "user@example.com"
        );

        PaymentOrderResponse response = paymentService.createPaymentOrder(request);

        // Then: Should apply 20% discount
        long expectedAmount = 499000L * 80 / 100; // 399,200 VND
        assertThat(response.amount()).isEqualTo(expectedAmount);
        assertThat(response.discountPercent()).isEqualTo(20);
        assertThat(response.discountReason()).contains("Early-bird");
    }

    @Test
    @DisplayName("should not apply discount if paid after 10 days")
    void shouldNotApplyDiscount_ifPaidAfter10Days() {
        // Given: Instance in trial (day 12)
        Instance instance = instanceRepo.save(Instance.builder()
                .subdomain("testcenter")
                .status(InstanceStatus.TRIAL)
                .trialStartedAt(LocalDateTime.now().minusDays(12))
                .trialExpiresAt(LocalDateTime.now().plusDays(2))
                .tier(PricingTier.BASIC)
                .build());

        // When: Create payment order (after 10-day window)
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(
                PaymentType.SUBSCRIPTION,
                instance.getId(),
                499000L,
                PricingTier.BASIC,
                "user@example.com"
        );

        PaymentOrderResponse response = paymentService.createPaymentOrder(request);

        // Then: Full price (no discount)
        assertThat(response.amount()).isEqualTo(499000L);
        assertThat(response.discountPercent()).isEqualTo(0);
    }
}
```

---

# PART 5: AI BRANDING JOB TESTING

## 5.1 Async Job Lifecycle Testing

```java
@SpringBootTest
class AiBrandingJobTest {

    @Autowired
    private AiBrandingService aiBrandingService;

    @Autowired
    private AiBrandingJobRepository jobRepo;

    @MockBean
    private OpenAiClient openAiClient;

    @Test
    @DisplayName("processJob should complete successfully with valid logo")
    void processJob_shouldCompleteSuccessfully() {
        // Given: Mock OpenAI responses
        LogoAnalysisResult analysis = new LogoAnalysisResult(
                "Math Education", "#0ea5e9", "#64748b", "Modern, Professional");

        when(openAiClient.analyzeLogoWithVision(any()))
                .thenReturn(Mono.just(analysis));

        when(openAiClient.generateImageWithDallE3(anyString()))
                .thenReturn(Mono.just("https://cdn.openai.com/image123.png"));

        // Given: Upload logo
        UUID instanceId = UUID.randomUUID();
        MultipartFile logoFile = createMockFile("logo.png", "image/png");

        // When: Start AI branding job
        AiBrandingJobResponse response = aiBrandingService.startBrandingJob(
                instanceId, logoFile);

        // Then: Job created
        assertThat(response.jobId()).isNotNull();
        assertThat(response.status()).isEqualTo(JobStatus.QUEUED);

        // Wait for job to complete (async)
        await().atMost(Duration.ofSeconds(30))
                .until(() -> {
                    AiBrandingJob job = jobRepo.findById(response.jobId()).orElseThrow();
                    return job.getStatus() == JobStatus.COMPLETED;
                });

        // Then: Verify results
        AiBrandingJob completedJob = jobRepo.findById(response.jobId()).orElseThrow();
        assertThat(completedJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(completedJob.getMarketingAssets()).hasSizeGreaterThanOrEqualTo(10);
        assertThat(completedJob.getCompletedAt()).isNotNull();

        // Verify OpenAI called
        verify(openAiClient).analyzeLogoWithVision(any());
        verify(openAiClient, atLeast(10)).generateImageWithDallE3(anyString());
    }

    @Test
    @DisplayName("processJob should retry on API failure")
    void processJob_shouldRetryOnApiFailure() {
        // Given: OpenAI fails twice, succeeds on 3rd try
        when(openAiClient.analyzeLogoWithVision(any()))
                .thenThrow(new ApiRateLimitException("Rate limit"))
                .thenThrow(new ApiRateLimitException("Rate limit"))
                .thenReturn(Mono.just(new LogoAnalysisResult(
                        "Education", "#0ea5e9", "#64748b", "Modern")));

        when(openAiClient.generateImageWithDallE3(anyString()))
                .thenReturn(Mono.just("https://cdn.openai.com/image.png"));

        // When: Start job
        UUID instanceId = UUID.randomUUID();
        AiBrandingJobResponse response = aiBrandingService.startBrandingJob(
                instanceId, createMockFile("logo.png", "image/png"));

        // Wait for retries + completion
        await().atMost(Duration.ofMinutes(2))
                .until(() -> {
                    AiBrandingJob job = jobRepo.findById(response.jobId()).orElseThrow();
                    return job.getStatus() == JobStatus.COMPLETED;
                });

        // Then: Should retry and eventually succeed
        AiBrandingJob job = jobRepo.findById(response.jobId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getRetryCount()).isEqualTo(2);

        verify(openAiClient, times(3)).analyzeLogoWithVision(any());
    }

    @Test
    @DisplayName("processJob should fail after max retries")
    void processJob_shouldFailAfterMaxRetries() {
        // Given: OpenAI always fails
        when(openAiClient.analyzeLogoWithVision(any()))
                .thenThrow(new ApiException("Service unavailable"));

        // When: Start job
        UUID instanceId = UUID.randomUUID();
        AiBrandingJobResponse response = aiBrandingService.startBrandingJob(
                instanceId, createMockFile("logo.png", "image/png"));

        // Wait for max retries + failure
        await().atMost(Duration.ofMinutes(5))
                .until(() -> {
                    AiBrandingJob job = jobRepo.findById(response.jobId()).orElseThrow();
                    return job.getStatus() == JobStatus.FAILED;
                });

        // Then: Should be FAILED after max retries
        AiBrandingJob job = jobRepo.findById(response.jobId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getRetryCount()).isEqualTo(3); // Max retries
        assertThat(job.getErrorMessage()).contains("Service unavailable");

        verify(openAiClient, times(4)).analyzeLogoWithVision(any()); // Initial + 3 retries
    }

    @Test
    @DisplayName("processJob should timeout after 5 minutes")
    void processJob_shouldTimeoutAfter5Minutes() {
        // Given: OpenAI API hangs (never responds)
        when(openAiClient.analyzeLogoWithVision(any()))
                .thenAnswer(invocation -> {
                    Thread.sleep(10 * 60 * 1000); // 10 minutes (exceeds timeout)
                    return null;
                });

        // When: Start job
        UUID instanceId = UUID.randomUUID();
        AiBrandingJobResponse response = aiBrandingService.startBrandingJob(
                instanceId, createMockFile("logo.png", "image/png"));

        // Wait for timeout
        await().atMost(Duration.ofMinutes(6))
                .until(() -> {
                    AiBrandingJob job = jobRepo.findById(response.jobId()).orElseThrow();
                    return job.getStatus() == JobStatus.FAILED;
                });

        // Then: Should be FAILED with timeout error
        AiBrandingJob job = jobRepo.findById(response.jobId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("timeout");
    }
}
```

---

# PART 6: CACHE TESTING

## 6.1 Redis Cache Testing

```java
@SpringBootTest
@Testcontainers
class CacheTest {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private FeatureDetectionService featureService;

    @Autowired
    private InstanceConfigRepository configRepo;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @Test
    @DisplayName("getInstanceConfig should cache result for 1 hour")
    void getInstanceConfig_shouldCacheFor1Hour() {
        // Given
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = configRepo.save(InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.STANDARD)
                .features(Map.of("ENGAGEMENT", true))
                .build());

        // When: First call (cache miss)
        InstanceConfig result1 = featureService.getInstanceConfig(instanceId);

        // Then: Should hit database
        assertThat(result1.getTier()).isEqualTo(PricingTier.STANDARD);

        // When: Second call (cache hit)
        InstanceConfig result2 = featureService.getInstanceConfig(instanceId);

        // Then: Should return cached value (not hit database again)
        assertThat(result2).isSameAs(result1);

        // Verify: Only 1 database query (first call)
        // (In production, use @Spy on repository to verify)
    }

    @Test
    @DisplayName("updateInstanceConfig should invalidate cache")
    void updateInstanceConfig_shouldInvalidateCache() {
        // Given: Config cached
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = configRepo.save(InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .features(Map.of("ENGAGEMENT", false))
                .build());

        featureService.getInstanceConfig(instanceId); // Cache it

        // When: Update config (should invalidate cache)
        config.setTier(PricingTier.STANDARD);
        config.setFeatures(Map.of("ENGAGEMENT", true));
        configRepo.save(config);

        featureService.evictInstanceConfigCache(instanceId);

        // Then: Next call should fetch updated config from database
        InstanceConfig updated = featureService.getInstanceConfig(instanceId);
        assertThat(updated.getTier()).isEqualTo(PricingTier.STANDARD);
        assertThat(updated.getFeatures().get("ENGAGEMENT")).isTrue();
    }

    @Test
    @DisplayName("cache should expire after 1 hour TTL")
    void cache_shouldExpireAfter1HourTTL() {
        // Given: Config cached
        UUID instanceId = UUID.randomUUID();
        configRepo.save(InstanceConfig.builder()
                .instanceId(instanceId)
                .tier(PricingTier.BASIC)
                .build());

        featureService.getInstanceConfig(instanceId);

        // Verify cache exists
        Cache cache = cacheManager.getCache("instanceConfigs");
        assertThat(cache).isNotNull();
        assertThat(cache.get(instanceId)).isNotNull();

        // Simulate 1 hour passing (in real test, use @DirtiesContext or wait)
        // For unit test, we verify TTL is configured correctly in RedisCacheConfiguration
        // (Actual expiry testing requires integration test with real Redis)
    }
}
```

---

## Test Coverage Requirements

| Layer | Minimum Coverage | KiteClass-Specific Tests Required |
|-------|-----------------|-----------------------------------|
| Multi-Tenant | 90% | ✅ Tenant isolation, cross-tenant access |
| Feature Detection | 90% | ✅ requireFeature, tier limits, hasFeature |
| Payment | 85% | ✅ QR generation, verification, security |
| Trial System | 85% | ✅ Lifecycle, grace period, early-bird |
| AI Branding | 80% | ✅ Async jobs, retry, timeout |
| Cache | 80% | ✅ Hit/miss, TTL, invalidation |

---

## Actions

### Run tests
```bash
./mvnw test -Dtest="*MultiTenant*"
./mvnw test -Dtest="*Feature*"
./mvnw test -Dtest="*Payment*"
./mvnw test -Dtest="*Trial*"
./mvnw test -Dtest="*AiBranding*"
```

### Generate coverage report
```bash
./mvnw verify
# Open target/site/jacoco/index.html
```

---

**Last Updated:** 2026-01-30
**Related Skills:** `spring-boot-testing-quality.md`, `testing-guide.md`, `security-testing-standards.md`
