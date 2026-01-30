# INTEGRATION TESTING STRATEGY

**Version:** 1.0
**Created:** 2026-01-30
**Purpose:** Integration testing cho KiteClass & KiteHub services

**Tham chiếu:**
- `backend-implementation-plan-v2.md`
- `kitehub-implementation-plan.md`
- `expand-services-implementation.md`

---

## MỤC LỤC

1. [Testing Philosophy](#testing-philosophy)
2. [Test Environment Setup](#test-environment-setup)
3. [Feature Detection Tests](#feature-detection-tests)
4. [AI Branding Tests](#ai-branding-tests)
5. [VietQR Payment Tests](#vietqr-payment-tests)
6. [Guest & Trial System Tests](#guest--trial-system-tests)
7. [Expand Services Tests](#expand-services-tests)
8. [E2E Test Scenarios](#e2e-test-scenarios)
9. [Performance Testing](#performance-testing)
10. [CI/CD Integration](#cicd-integration)

---

# TESTING PHILOSOPHY

## Test Pyramid

```
┌────────────────────────────────────┐
│     E2E Tests (UI + API)           │  ← 10% (Critical user flows)
│          ~50 tests                 │
├────────────────────────────────────┤
│     Integration Tests              │  ← 30% (Service interactions)
│          ~200 tests                │
├────────────────────────────────────┤
│     Unit Tests                     │  ← 60% (Business logic)
│          ~500 tests                │
└────────────────────────────────────┘
```

## Coverage Goals

- **Unit Tests:** 80%+ coverage
- **Integration Tests:** Critical paths covered
- **E2E Tests:** Key user journeys covered
- **Performance Tests:** Response time < 200ms (P95)

---

# TEST ENVIRONMENT SETUP

## Docker Compose for Testing

```yaml
# docker-compose.test.yml
version: '3.8'

services:
  postgres-test:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: kiteclass_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "54320:5432" # Different port to avoid conflicts
    tmpfs:
      - /var/lib/postgresql/data # In-memory for speed

  redis-test:
    image: redis:7-alpine
    ports:
      - "63790:6379"

  mockserver:
    image: mockserver/mockserver:latest
    ports:
      - "10800:1080"
    environment:
      MOCKSERVER_INITIALIZATION_JSON_PATH: /config/expectations.json
    volumes:
      - ./test/mockserver:/config
```

## Spring Boot Test Configuration

```java
// src/test/java/com/kiteclass/core/config/TestConfig.java
@TestConfiguration
@EnableAutoConfiguration
public class TestConfig {

    @Bean
    @Primary
    public DataSource testDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://localhost:54320/kiteclass_test")
            .username("test")
            .password("test")
            .build();
    }

    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(63790);
        return new LettuceConnectionFactory(config);
    }
}
```

---

# FEATURE DETECTION TESTS

## Integration Tests

```java
// src/test/java/com/kiteclass/core/service/FeatureDetectionIntegrationTest.java
@SpringBootTest
@Transactional
class FeatureDetectionIntegrationTest {

    @Autowired
    private FeatureDetectionService featureService;

    @Autowired
    private InstanceConfigRepository instanceConfigRepo;

    @Test
    @DisplayName("BASIC tier should not have ENGAGEMENT feature")
    void basicTier_noEngagement() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.BASIC);

        // When
        boolean hasEngagement = featureService.hasFeature(instanceId, "ENGAGEMENT");

        // Then
        assertThat(hasEngagement).isFalse();
    }

    @Test
    @DisplayName("STANDARD tier should have ENGAGEMENT and MEDIA")
    void standardTier_hasEngagementAndMedia() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.STANDARD);

        // When & Then
        assertThat(featureService.hasFeature(instanceId, "ENGAGEMENT")).isTrue();
        assertThat(featureService.hasFeature(instanceId, "MEDIA")).isTrue();
        assertThat(featureService.hasFeature(instanceId, "PREMIUM")).isFalse();
    }

    @Test
    @DisplayName("requireFeature should throw exception for unavailable feature")
    void requireFeature_throwsException() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.BASIC);

        // When & Then
        assertThatThrownBy(() ->
            featureService.requireFeature(instanceId, "ENGAGEMENT")
        )
        .isInstanceOf(FeatureNotAvailableException.class)
        .hasMessageContaining("ENGAGEMENT")
        .hasMessageContaining("STANDARD");
    }

    @Test
    @DisplayName("isWithinLimit should check student limit correctly")
    void isWithinLimit_studentLimit() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.BASIC); // Limit: 50

        // When & Then
        assertThat(featureService.isWithinLimit(instanceId, "maxStudents", 49)).isTrue();
        assertThat(featureService.isWithinLimit(instanceId, "maxStudents", 50)).isFalse();
        assertThat(featureService.isWithinLimit(instanceId, "maxStudents", 51)).isFalse();
    }

    @Test
    @DisplayName("Feature config should be cached")
    void featureConfig_shouldBeCached() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.STANDARD);

        // When
        InstanceConfig config1 = featureService.getInstanceConfig(instanceId);
        InstanceConfig config2 = featureService.getInstanceConfig(instanceId);

        // Then
        assertThat(config1).isSameAs(config2); // Same object from cache
    }

    private UUID createTestInstance(PricingTier tier) {
        UUID instanceId = UUID.randomUUID();

        InstanceConfig config = new InstanceConfig();
        config.setInstanceId(instanceId);
        config.initializeFromTier(tier);

        instanceConfigRepo.save(config);

        return instanceId;
    }
}
```

## API Integration Tests

```java
// src/test/java/com/kiteclass/core/controller/FeatureDetectionControllerIntegrationTest.java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FeatureDetectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getInstanceConfig_returnsCorrectConfig() throws Exception {
        // Given
        UUID instanceId = createTestInstance(PricingTier.STANDARD);

        // When & Then
        mockMvc.perform(get("/api/v1/instance/{instanceId}/config", instanceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value("STANDARD"))
            .andExpect(jsonPath("$.features.engagement").value(true))
            .andExpect(jsonPath("$.features.media").value(true))
            .andExpect(jsonPath("$.features.premium").value(false))
            .andExpect(jsonPath("$.limitations.maxStudents").value(200))
            .andExpect(jsonPath("$.limitations.maxCourses").value(50));
    }

    @Test
    void checkFeature_returnsAvailability() throws Exception {
        // Given
        UUID instanceId = createTestInstance(PricingTier.BASIC);

        // When & Then
        mockMvc.perform(get("/api/v1/instance/{instanceId}/features/ENGAGEMENT", instanceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.feature").value("ENGAGEMENT"))
            .andExpect(jsonPath("$.available").value(false));
    }
}
```

---

# AI BRANDING TESTS

## Mock OpenAI API

```java
// src/test/java/com/kiteclass/core/service/AIBrandingIntegrationTest.java
@SpringBootTest
@AutoConfigureMockMvc
class AIBrandingIntegrationTest {

    @MockBean
    private OpenAIClient openAIClient;

    @Autowired
    private AIBrandingService brandingService;

    @Test
    @DisplayName("AI branding should generate all assets")
    void generateBranding_success() throws Exception {
        // Given
        MockMultipartFile logoFile = new MockMultipartFile(
            "logo",
            "logo.png",
            "image/png",
            "fake-image-content".getBytes()
        );

        // Mock GPT-4 Vision response
        when(openAIClient.createChatCompletion(any()))
            .thenReturn(mockGPT4VisionResponse());

        // Mock DALL-E 3 response
        when(openAIClient.createImage(any()))
            .thenReturn(mockDALLE3Response());

        // When
        CompletableFuture<BrandingAssets> future = brandingService.generateBrandingAssets(
            "job-123",
            logoFile,
            "Test Organization",
            "vi"
        );

        BrandingAssets assets = future.get(10, TimeUnit.SECONDS);

        // Then
        assertThat(assets).isNotNull();
        assertThat(assets.getProfileImages()).hasSize(3); // cutout, circle, square
        assertThat(assets.getHeroImages()).hasSize(3);
        assertThat(assets.getBrandLogos()).hasSize(2); // light, dark
        assertThat(assets.getBanners()).hasSize(2); // Facebook, YouTube
        assertThat(assets.getOgImage()).isNotBlank();
        assertThat(assets.getMarketingCopy()).isNotNull();

        verify(openAIClient, times(1)).createChatCompletion(any()); // GPT-4 Vision
        verify(openAIClient, atLeast(5)).createImage(any()); // DALL-E 3 calls
    }

    @Test
    @DisplayName("Job progress should be tracked")
    void jobProgress_tracked() throws Exception {
        // Given
        String jobId = "job-456";
        MockMultipartFile logoFile = createTestLogoFile();

        when(openAIClient.createChatCompletion(any())).thenReturn(mockGPT4VisionResponse());
        when(openAIClient.createImage(any())).thenReturn(mockDALLE3Response());

        // When
        CompletableFuture<BrandingAssets> future = brandingService.generateBrandingAssets(
            jobId,
            logoFile,
            "Test Org",
            "vi"
        );

        // Wait a bit
        Thread.sleep(500);

        // Check progress
        BrandingJob job = brandingService.getJobStatus(jobId);

        // Then
        assertThat(job.getProgressPercentage()).isGreaterThan(0);
        assertThat(job.getStatus()).isIn(BrandingStatus.PROCESSING, BrandingStatus.COMPLETED);
    }

    private ChatCompletionResponse mockGPT4VisionResponse() {
        // Return mock analysis
        return ChatCompletionResponse.builder()
            .choices(List.of(
                Choice.builder()
                    .message(ChatMessage.builder()
                        .content("Colors: #FF5733, #3498DB. Theme: Modern, professional. Target: Parents.")
                        .build())
                    .build()
            ))
            .build();
    }

    private ImageGenerationResponse mockDALLE3Response() {
        return ImageGenerationResponse.builder()
            .data(List.of(
                ImageData.builder()
                    .url("https://fake-cdn.com/image-123.jpg")
                    .build()
            ))
            .build();
    }
}
```

---

# VIETQR PAYMENT TESTS

## Payment Flow Integration Test

```java
// src/test/java/com/kiteclass/core/service/VietQRPaymentIntegrationTest.java
@SpringBootTest
@Transactional
class VietQRPaymentIntegrationTest {

    @Autowired
    private KiteHubPaymentService paymentService;

    @Autowired
    private PaymentOrderRepository paymentOrderRepo;

    @Test
    @DisplayName("Create subscription order should generate VietQR")
    void createSubscriptionOrder_generatesQR() {
        // Given
        User user = createTestUser();
        PricingTier tier = PricingTier.STANDARD;

        // When
        PaymentOrderResponse response = paymentService.createSubscriptionOrder(user, tier);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isNotBlank();
        assertThat(response.getQrImageUrl()).contains("vietqr.io");
        assertThat(response.getAmount()).isEqualTo(999_000L);
        assertThat(response.getBankName()).isEqualTo("Vietcombank");
        assertThat(response.getAccountNumber()).contains("****"); // Masked
        assertThat(response.getContent()).contains("KITEHUB");
        assertThat(response.getContent()).contains(user.getEmail());

        // Verify order saved
        PaymentOrder order = paymentOrderRepo.findByOrderId(response.getOrderId())
            .orElseThrow();

        assertThat(order.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(order.getExpiresAt()).isBefore(LocalDateTime.now().plusHours(25)); // 24h expiry
    }

    @Test
    @DisplayName("Confirm payment should activate subscription")
    void confirmPayment_activatesSubscription() {
        // Given
        PaymentOrder order = createPendingPaymentOrder();
        String transactionRef = "FT123456";

        // When
        paymentService.confirmPayment(
            order.getOrderId(),
            transactionRef,
            LocalDateTime.now()
        );

        // Then
        PaymentOrder updatedOrder = paymentOrderRepo.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedOrder.getTransactionReference()).isEqualTo(transactionRef);
        assertThat(updatedOrder.getPaidAt()).isNotNull();

        // Verify subscription activated
        Subscription subscription = subscriptionRepo.findByInstanceId(order.getInstanceId())
            .orElseThrow();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getTier()).isEqualTo(order.getTier());
    }

    @Test
    @DisplayName("Double payment confirmation should be rejected")
    void confirmPayment_alreadyPaid_throwsException() {
        // Given
        PaymentOrder order = createPaidPaymentOrder();

        // When & Then
        assertThatThrownBy(() ->
            paymentService.confirmPayment(order.getOrderId(), "REF", LocalDateTime.now())
        )
        .isInstanceOf(InvalidStateException.class)
        .hasMessageContaining("already processed");
    }

    @Test
    @DisplayName("Expired orders should not be confirmable")
    void confirmPayment_expired_throwsException() {
        // Given
        PaymentOrder order = createExpiredPaymentOrder();

        // When & Then
        assertThatThrownBy(() ->
            paymentService.confirmPayment(order.getOrderId(), "REF", LocalDateTime.now())
        )
        .isInstanceOf(PaymentExpiredException.class);
    }
}
```

## VietQR URL Generation Test

```java
@Test
void buildVietQRUrl_correctFormat() {
    // Given
    String bankBin = "970415";
    String accountNo = "1234567890";
    String accountName = "CONG TY TNHH KITECLASS";
    long amount = 499_000L;
    String content = "KITEHUB ORD-123 user@example.com";

    // When
    String qrUrl = VietQRUtil.buildVietQRUrl(
        bankBin,
        accountNo,
        accountName,
        amount,
        content
    );

    // Then
    assertThat(qrUrl).contains("img.vietqr.io/image");
    assertThat(qrUrl).contains(bankBin);
    assertThat(qrUrl).contains(accountNo);
    assertThat(qrUrl).contains("amount=499000");
    assertThat(qrUrl).contains("addInfo=");
    assertThat(qrUrl).contains("accountName=");
}
```

---

# GUEST & TRIAL SYSTEM TESTS

## Trial Lifecycle Test

```java
@SpringBootTest
@Transactional
class TrialSystemIntegrationTest {

    @Autowired
    private SubscriptionLifecycleService subscriptionService;

    @Test
    @DisplayName("Trial should expire after 14 days")
    void trial_expiresAfter14Days() {
        // Given
        Instance instance = createTrialInstance();
        instance.setTrialExpiresAt(LocalDateTime.now().minusDays(1)); // Expired yesterday

        // When
        subscriptionService.handleExpiredTrial(instance);

        // Then
        InstanceConfig config = instanceConfigRepo.findByInstanceId(instance.getId()).orElseThrow();
        assertThat(config.isReadOnlyMode()).isTrue(); // Grace period
    }

    @Test
    @DisplayName("Trial expiration emails should be sent at Day 11, 13, 14")
    void trialExpirationEmails_sent() {
        // Given
        List<Instance> instances = List.of(
            createTrialInstanceWithDaysLeft(3), // Day 11
            createTrialInstanceWithDaysLeft(1), // Day 13
            createTrialInstanceWithDaysLeft(0)  // Day 14
        );

        // When
        subscriptionService.checkTrialExpirations();

        // Then
        verify(emailService, times(3)).sendTrialExpirationWarning(any(), anyLong());
    }
}
```

## Guest Analytics Test

```java
@SpringBootTest
@Transactional
class GuestAnalyticsIntegrationTest {

    @Autowired
    private GuestAnalyticsService analyticsService;

    @Test
    @DisplayName("Guest events should be tracked")
    void guestEvents_tracked() {
        // Given
        String sessionId = "guest_123";
        UUID instanceId = UUID.randomUUID();

        // When
        analyticsService.trackEvent(sessionId, instanceId, "page_view", null, Map.of("page", "/courses"));
        analyticsService.trackEvent(sessionId, instanceId, "contact_click", null, Map.of("method", "facebook"));

        // Then
        List<GuestEvent> events = guestEventRepo.findBySessionId(sessionId);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("page_view");
        assertThat(events.get(1).getEventType()).isEqualTo("contact_click");
    }

    @Test
    @DisplayName("Analytics dashboard should aggregate correctly")
    void analyticsDashboard_aggregates() {
        // Given
        UUID instanceId = UUID.randomUUID();
        createGuestEventsForInstance(instanceId, 100); // 100 events

        // When
        GuestAnalytics analytics = analyticsService.getGuestAnalytics(
            instanceId,
            LocalDate.now().minusDays(7),
            LocalDate.now()
        );

        // Then
        assertThat(analytics.getUniqueVisitors()).isGreaterThan(0);
        assertThat(analytics.getPageViews().getLanding()).isGreaterThan(0);
        assertThat(analytics.getConversionFunnel()).isNotNull();
        assertThat(analytics.getMostViewedCourses()).isNotEmpty();
    }
}
```

---

# EXPAND SERVICES TESTS

## ENGAGEMENT Package Tests

```java
@SpringBootTest
@Transactional
class EngagementFeatureIntegrationTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private FeatureDetectionService featureService;

    @Test
    @DisplayName("BASIC tier should not allow attendance tracking")
    void basicTier_attendanceBlocked() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.BASIC);

        // When & Then
        assertThatThrownBy(() ->
            attendanceService.recordAttendance(
                instanceId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                AttendanceStatus.PRESENT
            )
        )
        .isInstanceOf(FeatureNotAvailableException.class)
        .hasMessageContaining("ENGAGEMENT")
        .hasMessageContaining("STANDARD");
    }

    @Test
    @DisplayName("STANDARD tier should allow attendance tracking")
    void standardTier_attendanceAllowed() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.STANDARD);
        Student student = createTestStudent(instanceId);
        ClassSession session = createTestSession(instanceId);

        // When
        AttendanceRecord record = attendanceService.recordAttendance(
            instanceId,
            student.getId(),
            session.getId(),
            AttendanceStatus.PRESENT
        );

        // Then
        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }
}
```

## MEDIA Package Tests

```java
@SpringBootTest
@Transactional
class MediaFeatureIntegrationTest {

    @Autowired
    private MediaUploadService mediaService;

    @Test
    @DisplayName("BASIC tier should not allow image upload")
    void basicTier_imageUploadBlocked() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.BASIC);
        MockMultipartFile imageFile = createTestImageFile();

        // When & Then
        assertThatThrownBy(() ->
            mediaService.uploadImage(instanceId, imageFile)
        )
        .isInstanceOf(FeatureNotAvailableException.class)
        .hasMessageContaining("MEDIA");
    }

    @Test
    @DisplayName("STANDARD tier should enforce 5GB storage limit")
    void standardTier_5GBLimit() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.STANDARD);

        // Use up 5GB storage
        setStorageUsed(instanceId, 5L * 1024 * 1024 * 1024);

        MockMultipartFile imageFile = createTestImageFile(1024 * 1024); // 1MB

        // When & Then
        assertThatThrownBy(() ->
            mediaService.uploadImage(instanceId, imageFile)
        )
        .isInstanceOf(StorageLimitExceededException.class)
        .hasMessageContaining("5 MB")
        .hasMessageContaining("PREMIUM");
    }

    @Test
    @DisplayName("PREMIUM tier should allow 20GB storage")
    void premiumTier_20GBLimit() {
        // Given
        UUID instanceId = createTestInstance(PricingTier.PREMIUM);

        // When
        long limit = mediaService.getStorageLimit(instanceId);

        // Then
        assertThat(limit).isEqualTo(20L * 1024 * 1024 * 1024); // 20GB
    }
}
```

---

# E2E TEST SCENARIOS

## Scenario 1: Trial Signup → Upgrade Flow

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TrialToUpgradeE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("E2E: Trial signup → Payment → Upgrade to STANDARD")
    void trialSignupToUpgrade() throws Exception {
        // 1. User signs up for trial
        String email = "owner@example.com";
        String organizationName = "Test Center";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/trial-signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "%s",
                    "organizationName": "%s",
                    "password": "password123"
                }
                """.formatted(email, organizationName)))
            .andExpect(status().isCreated())
            .andReturn();

        String token = extractToken(result);
        UUID instanceId = extractInstanceId(result);

        // 2. Verify trial instance created
        mockMvc.perform(get("/api/v1/instance/{instanceId}/config", instanceId)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value("BASIC"))
            .andExpect(jsonPath("$.features.engagement").value(false));

        // 3. Try to use ENGAGEMENT feature (should fail)
        mockMvc.perform(post("/api/v1/attendance")
            .header("Authorization", "Bearer " + token)
            .header("X-Instance-Id", instanceId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Feature Not Available"));

        // 4. Create subscription payment order
        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payment/subscription/create")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tier\": \"STANDARD\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.qrImageUrl").exists())
            .andReturn();

        String orderId = extractOrderId(paymentResult);

        // 5. Admin confirms payment (simulated)
        mockMvc.perform(post("/api/v1/admin/payments/{orderId}/confirm", orderId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"transactionReference\": \"FT123456\"}"))
            .andExpect(status().isOk());

        // 6. Verify tier upgraded
        mockMvc.perform(get("/api/v1/instance/{instanceId}/config", instanceId)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value("STANDARD"))
            .andExpect(jsonPath("$.features.engagement").value(true))
            .andExpect(jsonPath("$.features.media").value(true));

        // 7. Now ENGAGEMENT feature should work
        mockMvc.perform(post("/api/v1/attendance")
            .header("Authorization", "Bearer " + token)
            .header("X-Instance-Id", instanceId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "studentId": "%s",
                    "classSessionId": "%s",
                    "status": "PRESENT"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
            .andExpect(status().isCreated());
    }
}
```

---

# PERFORMANCE TESTING

## Load Test Scenarios

```yaml
# k6-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '5m', target: 50 },   // Stay at 50 users
    { duration: '1m', target: 100 },  // Ramp up to 100 users
    { duration: '5m', target: 100 },  // Stay at 100 users
    { duration: '1m', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests under 200ms
    http_req_failed: ['rate<0.01'],   // Error rate < 1%
  },
};

export default function () {
  // Test feature detection API
  const instanceId = 'test-instance-id';
  const res = http.get(`http://localhost:8080/api/v1/instance/${instanceId}/config`);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(1);
}
```

Run: `k6 run k6-load-test.js`

---

# CI/CD INTEGRATION

## GitHub Actions Workflow

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: kiteclass_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Run unit tests
        run: ./mvnw test

      - name: Run integration tests
        run: ./mvnw verify -P integration-tests

      - name: Generate coverage report
        run: ./mvnw jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml

      - name: SonarQube Scan
        run: ./mvnw sonar:sonar
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

---

# SUMMARY

**Testing Coverage:**
1. ✅ Feature Detection (unit + integration + API tests)
2. ✅ AI Branding (mocked OpenAI, job tracking)
3. ✅ VietQR Payment (order creation, confirmation, expiry)
4. ✅ Guest & Trial System (lifecycle, analytics)
5. ✅ Expand Services (ENGAGEMENT, MEDIA, PREMIUM)
6. ✅ E2E Scenarios (critical user flows)
7. ✅ Performance Tests (load testing with k6)
8. ✅ CI/CD Integration (GitHub Actions)

**Test Metrics:**
- Total tests: ~750 (500 unit + 200 integration + 50 E2E)
- Coverage goal: 80%+
- P95 response time: <200ms
- Error rate: <1%

**Ready for:**
- Continuous Integration
- Continuous Deployment
- Production monitoring

**All 5 documents complete!** 🎉
