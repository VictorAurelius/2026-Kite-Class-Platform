# INTEGRATION TESTING STRATEGY

**Version:** 1.2 (V4.1 - Added LMS + Marketing Integration Tests) ⭐
**Created:** 2026-01-30
**Last Updated:** 2026-02-26 ⭐
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

# KITEHUB ↔ KITECLASS INTEGRATION TESTS

## Overview

KiteHub is the central authentication/instance management service. KiteClass instances communicate with KiteHub for:
- **Authentication:** JWT token validation
- **Instance Config Sync:** Feature flags, tier updates
- **Payment Callbacks:** Subscription activation
- **Trial Management:** Trial expiration, grace period

## Instance Registration Test

```java
@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class KiteHubInstanceRegistrationTest {

    @Autowired
    private KiteHubClient kiteHubClient;

    @Test
    @DisplayName("KiteClass should register with KiteHub on startup")
    void instanceRegistration_success() {
        // Given
        stubFor(post(urlEqualTo("/api/v1/instances/register"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "instanceId": "123e4567-e89b-12d3-a456-426614174000",
                        "apiKey": "kiteclass_test_key",
                        "webhookUrl": "https://kitehub.example.com/webhooks"
                    }
                    """)
            ));

        // When
        InstanceRegistrationResponse response = kiteHubClient.registerInstance(
            "test-instance.kitehub.me",
            "Test School",
            "owner@test.com"
        );

        // Then
        assertThat(response.getInstanceId()).isNotNull();
        assertThat(response.getApiKey()).isNotBlank();
        assertThat(response.getWebhookUrl()).contains("kitehub.example.com");

        // Verify request
        verify(postRequestedFor(urlEqualTo("/api/v1/instances/register"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.subdomain", equalTo("test-instance")))
            .withRequestBody(matchingJsonPath("$.organizationName", equalTo("Test School")))
        );
    }

    @Test
    @DisplayName("Failed registration should retry with exponential backoff")
    void instanceRegistration_retry() {
        // Given
        stubFor(post(urlEqualTo("/api/v1/instances/register"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("First Retry"));

        stubFor(post(urlEqualTo("/api/v1/instances/register"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("First Retry")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("Second Retry"));

        stubFor(post(urlEqualTo("/api/v1/instances/register"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Second Retry")
            .willReturn(aResponse()
                .withStatus(201)
                .withBody("{\"instanceId\": \"123e4567-e89b-12d3-a456-426614174000\"}")
            ));

        // When
        InstanceRegistrationResponse response = kiteHubClient.registerInstance(
            "test-instance.kitehub.me",
            "Test School",
            "owner@test.com"
        );

        // Then
        assertThat(response.getInstanceId()).isNotNull();

        // Verify 3 attempts
        verify(exactly(3), postRequestedFor(urlEqualTo("/api/v1/instances/register")));
    }
}
```

## JWT Token Validation Test

```java
@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class KiteHubJWTValidationTest {

    @Autowired
    private JWTValidationService jwtService;

    @Test
    @DisplayName("Valid JWT from KiteHub should be accepted")
    void jwtValidation_valid() {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .withRequestBody(equalToJson("{\"token\": \"" + token + "\"}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""
                    {
                        "valid": true,
                        "userId": 123,
                        "instanceId": "123e4567-e89b-12d3-a456-426614174000",
                        "roles": ["OWNER"],
                        "email": "owner@school.com"
                    }
                    """)
            ));

        // When
        JWTValidationResult result = jwtService.validateToken(token);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getUserId()).isEqualTo(123L);
        assertThat(result.getInstanceId()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        assertThat(result.getRoles()).contains("OWNER");
    }

    @Test
    @DisplayName("Expired JWT should be rejected")
    void jwtValidation_expired() {
        // Given
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired...";

        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(401)
                .withBody("""
                    {
                        "valid": false,
                        "error": "Token expired"
                    }
                    """)
            ));

        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken(expiredToken))
            .isInstanceOf(TokenExpiredException.class)
            .hasMessageContaining("Token expired");
    }

    @Test
    @DisplayName("JWT validation should use cache to reduce API calls")
    void jwtValidation_cached() {
        // Given
        String token = "valid-token-123";

        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""
                    {
                        "valid": true,
                        "userId": 123,
                        "instanceId": "123e4567-e89b-12d3-a456-426614174000"
                    }
                    """)
            ));

        // When
        JWTValidationResult result1 = jwtService.validateToken(token);
        JWTValidationResult result2 = jwtService.validateToken(token);
        JWTValidationResult result3 = jwtService.validateToken(token);

        // Then
        assertThat(result1.isValid()).isTrue();
        assertThat(result2.isValid()).isTrue();
        assertThat(result3.isValid()).isTrue();

        // Should only call API once (cached)
        verify(exactly(1), postRequestedFor(urlEqualTo("/api/v1/auth/validate")));
    }
}
```

## Subscription Sync Test

```java
@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class KiteHubSubscriptionSyncTest {

    @Autowired
    private SubscriptionSyncService syncService;

    @Autowired
    private InstanceConfigRepository configRepo;

    @Test
    @DisplayName("Subscription upgrade webhook should update instance config")
    void subscriptionUpgrade_webhook() {
        // Given
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
            .instanceId(instanceId)
            .tier(PricingTier.BASIC)
            .build();
        configRepo.save(config);

        // When - Simulate webhook from KiteHub
        SubscriptionWebhookEvent event = SubscriptionWebhookEvent.builder()
            .eventType("subscription.upgraded")
            .instanceId(instanceId)
            .newTier("STANDARD")
            .validUntil(LocalDateTime.now().plusYears(1))
            .transactionId("TXN-123456")
            .build();

        syncService.handleWebhook(event);

        // Then
        InstanceConfig updatedConfig = configRepo.findByInstanceId(instanceId).orElseThrow();
        assertThat(updatedConfig.getTier()).isEqualTo(PricingTier.STANDARD);
        assertThat(updatedConfig.getSubscriptionValidUntil()).isNotNull();
        assertThat(updatedConfig.getFeatures().get("ENGAGEMENT")).isTrue();
        assertThat(updatedConfig.getFeatures().get("MEDIA")).isTrue();
    }

    @Test
    @DisplayName("Trial expiration webhook should enable grace period")
    void trialExpiration_webhook() {
        // Given
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
            .instanceId(instanceId)
            .tier(PricingTier.TRIAL)
            .trialExpiresAt(LocalDateTime.now().plusDays(1))
            .build();
        configRepo.save(config);

        // When - Simulate webhook from KiteHub
        SubscriptionWebhookEvent event = SubscriptionWebhookEvent.builder()
            .eventType("trial.expired")
            .instanceId(instanceId)
            .graceUntil(LocalDateTime.now().plusDays(3))
            .build();

        syncService.handleWebhook(event);

        // Then
        InstanceConfig updatedConfig = configRepo.findByInstanceId(instanceId).orElseThrow();
        assertThat(updatedConfig.isInGracePeriod()).isTrue();
        assertThat(updatedConfig.getGraceExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Subscription cancellation webhook should suspend instance")
    void subscriptionCancelled_webhook() {
        // Given
        UUID instanceId = UUID.randomUUID();
        InstanceConfig config = InstanceConfig.builder()
            .instanceId(instanceId)
            .tier(PricingTier.STANDARD)
            .subscriptionStatus(SubscriptionStatus.ACTIVE)
            .build();
        configRepo.save(config);

        // When - Simulate webhook from KiteHub
        SubscriptionWebhookEvent event = SubscriptionWebhookEvent.builder()
            .eventType("subscription.cancelled")
            .instanceId(instanceId)
            .reason("User requested cancellation")
            .build();

        syncService.handleWebhook(event);

        // Then
        InstanceConfig updatedConfig = configRepo.findByInstanceId(instanceId).orElseThrow();
        assertThat(updatedConfig.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        assertThat(updatedConfig.isReadOnlyMode()).isTrue();
    }

    @Test
    @DisplayName("Webhook signature should be validated")
    void webhook_invalidSignature_rejected() {
        // Given
        SubscriptionWebhookEvent event = SubscriptionWebhookEvent.builder()
            .eventType("subscription.upgraded")
            .instanceId(UUID.randomUUID())
            .newTier("STANDARD")
            .build();

        String invalidSignature = "invalid-signature-123";

        // When & Then
        assertThatThrownBy(() ->
            syncService.handleWebhook(event, invalidSignature)
        )
        .isInstanceOf(InvalidWebhookSignatureException.class)
        .hasMessageContaining("Invalid signature");
    }
}
```

## Feature Config Sync Test

```java
@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class KiteHubFeatureConfigSyncTest {

    @Autowired
    private FeatureConfigSyncService featureSyncService;

    @Test
    @DisplayName("Feature config should sync from KiteHub every hour")
    void featureConfigSync_scheduled() {
        // Given
        UUID instanceId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/v1/instances/" + instanceId + "/features"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""
                    {
                        "tier": "PREMIUM",
                        "features": {
                            "ENGAGEMENT": true,
                            "MEDIA": true,
                            "AI_BRANDING": true,
                            "CUSTOM_THEMES": true
                        },
                        "limitations": {
                            "maxStudents": -1,
                            "maxCourses": -1,
                            "storageGB": 20
                        }
                    }
                    """)
            ));

        // When
        featureSyncService.syncFeaturesForInstance(instanceId);

        // Then
        InstanceConfig config = instanceConfigRepo.findByInstanceId(instanceId).orElseThrow();
        assertThat(config.getTier()).isEqualTo(PricingTier.PREMIUM);
        assertThat(config.getFeatures().get("AI_BRANDING")).isTrue();
        assertThat(config.getLimitations().get("maxStudents")).isEqualTo(-1); // Unlimited

        // Verify cache updated
        InstanceConfig cached = featureSyncService.getCachedConfig(instanceId);
        assertThat(cached).isEqualTo(config);
    }

    @Test
    @DisplayName("Sync failure should not clear existing cache")
    void featureConfigSync_failurePreservesCache() {
        // Given
        UUID instanceId = UUID.randomUUID();

        // Setup existing cached config
        InstanceConfig existingConfig = InstanceConfig.builder()
            .instanceId(instanceId)
            .tier(PricingTier.STANDARD)
            .build();
        instanceConfigRepo.save(existingConfig);
        featureSyncService.cacheConfig(existingConfig);

        // Simulate KiteHub unavailable
        stubFor(get(urlEqualTo("/api/v1/instances/" + instanceId + "/features"))
            .willReturn(aResponse().withStatus(503)));

        // When
        try {
            featureSyncService.syncFeaturesForInstance(instanceId);
        } catch (Exception e) {
            // Expected to fail
        }

        // Then
        InstanceConfig cached = featureSyncService.getCachedConfig(instanceId);
        assertThat(cached).isNotNull();
        assertThat(cached.getTier()).isEqualTo(PricingTier.STANDARD); // Old cache preserved
    }
}
```

## Payment Callback Test

```java
@SpringBootTest
@AutoConfigureWireMock(port = 9999)
class KiteHubPaymentCallbackTest {

    @Autowired
    private PaymentCallbackService callbackService;

    @Test
    @DisplayName("Successful payment should notify KiteHub")
    void paymentSuccess_notifiesKiteHub() {
        // Given
        UUID instanceId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.builder()
            .orderId("ORD-20260130-ABC123")
            .instanceId(instanceId)
            .tier(PricingTier.STANDARD)
            .amount(499000L)
            .status(PaymentStatus.PAID)
            .transactionReference("FT123456")
            .build();

        stubFor(post(urlEqualTo("/api/v1/payments/callback"))
            .willReturn(aResponse().withStatus(200)));

        // When
        callbackService.notifyPaymentSuccess(order);

        // Then
        verify(postRequestedFor(urlEqualTo("/api/v1/payments/callback"))
            .withRequestBody(matchingJsonPath("$.orderId", equalTo("ORD-20260130-ABC123")))
            .withRequestBody(matchingJsonPath("$.instanceId", equalTo(instanceId.toString())))
            .withRequestBody(matchingJsonPath("$.tier", equalTo("STANDARD")))
            .withRequestBody(matchingJsonPath("$.amount", equalTo("499000")))
            .withRequestBody(matchingJsonPath("$.transactionReference", equalTo("FT123456")))
        );
    }

    @Test
    @DisplayName("Payment callback should retry on failure")
    void paymentCallback_retryOnFailure() {
        // Given
        PaymentOrder order = createTestPaymentOrder();

        stubFor(post(urlEqualTo("/api/v1/payments/callback"))
            .inScenario("Payment Callback")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("Retry"));

        stubFor(post(urlEqualTo("/api/v1/payments/callback"))
            .inScenario("Payment Callback")
            .whenScenarioStateIs("Retry")
            .willReturn(aResponse().withStatus(200)));

        // When
        callbackService.notifyPaymentSuccess(order);

        // Then
        verify(exactly(2), postRequestedFor(urlEqualTo("/api/v1/payments/callback")));
    }

    @Test
    @DisplayName("Failed payment should notify KiteHub with reason")
    void paymentFailed_notifiesKiteHub() {
        // Given
        PaymentOrder order = PaymentOrder.builder()
            .orderId("ORD-FAILED-123")
            .status(PaymentStatus.FAILED)
            .failureReason("Bank timeout")
            .build();

        stubFor(post(urlEqualTo("/api/v1/payments/callback"))
            .willReturn(aResponse().withStatus(200)));

        // When
        callbackService.notifyPaymentFailure(order);

        // Then
        verify(postRequestedFor(urlEqualTo("/api/v1/payments/callback"))
            .withRequestBody(matchingJsonPath("$.status", equalTo("FAILED")))
            .withRequestBody(matchingJsonPath("$.failureReason", equalTo("Bank timeout")))
        );
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

# V4.1 LMS + MARKETING INTEGRATION TESTS ⭐ NEW

## LMS Module Tests

### Test: Guest Access Control (Trial Lessons)

```java
@SpringBootTest
@Transactional
class LMSAccessControlIntegrationTest {

    @Autowired
    private LessonService lessonService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Test
    @DisplayName("Guest should access trial lessons without enrollment")
    void guestAccess_trialLesson_success() {
        // Given
        Course course = createCourse();
        CourseModule module = createModule(course);
        Lesson trialLesson = createLesson(module, true); // isTrial = true

        // When - Guest (no auth) accesses trial lesson
        LessonResponse response = lessonService.getLessonById(trialLesson.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isTrial()).isTrue();
        assertThat(response.content()).isNotNull();
    }

    @Test
    @DisplayName("Guest should NOT access paid lessons without enrollment")
    void guestAccess_paidLesson_forbidden() {
        // Given
        Course course = createCourse();
        CourseModule module = createModule(course);
        Lesson paidLesson = createLesson(module, false); // isTrial = false

        // When/Then - Guest accesses paid lesson → 403 Forbidden
        assertThatThrownBy(() -> lessonService.getLessonById(paidLesson.getId()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("ENROLLMENT_REQUIRED");
    }

    @Test
    @DisplayName("Student with enrollment should access all lessons")
    void studentAccess_withEnrollment_success() {
        // Given
        Student student = createStudent();
        Course course = createCourse();
        Enrollment enrollment = createActiveEnrollment(student, course);

        CourseModule module = createModule(course);
        Lesson trialLesson = createLesson(module, true);
        Lesson paidLesson = createLesson(module, false);

        // When - Student accesses both types
        LessonResponse trial = lessonService.getLessonById(trialLesson.getId());
        LessonResponse paid = lessonService.getLessonById(paidLesson.getId());

        // Then
        assertThat(trial).isNotNull();
        assertThat(paid).isNotNull();
    }
}
```

### Test: Learning Progress Tracking

```java
@SpringBootTest
@Transactional
class LearningProgressIntegrationTest {

    @Autowired
    private LessonProgressService progressService;

    @Test
    @DisplayName("Lesson completion should update progress")
    void lessonCompletion_updatesProgress() {
        // Given
        Student student = createStudent();
        Lesson lesson = createLesson(true);

        // When
        progressService.markLessonComplete(student.getId(), lesson.getId());

        // Then
        LessonProgress progress = progressService.getProgress(student.getId(), lesson.getId());
        assertThat(progress.isCompleted()).isTrue();
        assertThat(progress.getProgressPercent()).isEqualTo(100);
        assertThat(progress.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Course progress should calculate correctly")
    void courseProgress_calculatesCorrectly() {
        // Given
        Course course = createCourse();
        CourseModule module = createModule(course);
        Lesson lesson1 = createLesson(module, false);
        Lesson lesson2 = createLesson(module, false);
        Lesson lesson3 = createLesson(module, false);

        Student student = createStudent();
        createActiveEnrollment(student, course);

        // When - Complete 2 out of 3 lessons
        progressService.markLessonComplete(student.getId(), lesson1.getId());
        progressService.markLessonComplete(student.getId(), lesson2.getId());

        // Then
        int progress = progressService.getCourseProgress(student.getId(), course.getId());
        assertThat(progress).isEqualTo(66); // 2/3 * 100 = 66%
    }
}
```

---

## Marketing Module Tests

### Test: Landing Page Management

```java
@SpringBootTest
@Transactional
class LandingPageIntegrationTest {

    @Autowired
    private LandingPageService landingPageService;

    @Test
    @DisplayName("Each tenant should have exactly one landing page")
    void landingPage_onePerTenant() {
        // Given
        UUID tenantId = UUID.randomUUID();

        // When - Create landing page
        UpdateLandingPageRequest request = new UpdateLandingPageRequest(
            "Learn Java with Kiet",
            "Master Java in 3 months",
            "20 years of teaching experience...",
            "https://example.com/hero.jpg",
            "https://example.com/logo.png",
            "Your path to Java mastery",
            "#3B82F6",
            "#10B981"
        );
        landingPageService.updateLandingPage(tenantId, request);

        // Then
        LandingPageResponse page = landingPageService.getLandingPage(tenantId);
        assertThat(page).isNotNull();
        assertThat(page.heroTitle()).isEqualTo("Learn Java with Kiet");

        // When - Update again (should not create duplicate)
        request = request.withHeroTitle("Updated Title");
        landingPageService.updateLandingPage(tenantId, request);

        // Then - Still one record
        long count = landingPageRepository.countByInstanceId(tenantId);
        assertThat(count).isEqualTo(1);
    }
}
```

### Test: Lead Capture & Workflow

```java
@SpringBootTest
@Transactional
class LeadManagementIntegrationTest {

    @Autowired
    private LeadService leadService;

    @Autowired
    private ContactService contactService;

    @Test
    @DisplayName("Contact form should create lead + message")
    void contactForm_createsLeadAndMessage() {
        // Given
        UUID tenantId = UUID.randomUUID();
        ContactFormRequest request = new ContactFormRequest(
            "Nguyen Van A",
            "nguyenvana@gmail.com",
            "0901234567",
            "Tôi muốn đăng ký học Java"
        );

        // When
        contactService.submitContactForm(tenantId, request);

        // Then - Lead created
        Page<LeadResponse> leads = leadService.getLeads(
            tenantId, null, LeadSource.CONTACT_FORM, PageRequest.of(0, 10)
        );
        assertThat(leads.getContent()).hasSize(1);
        assertThat(leads.getContent().get(0).email()).isEqualTo("nguyenvana@gmail.com");
        assertThat(leads.getContent().get(0).status()).isEqualTo(LeadStatus.NEW);

        // And - Contact message created
        Page<ContactMessageResponse> messages = contactService.getMessages(
            tenantId, false, PageRequest.of(0, 10)
        );
        assertThat(messages.getContent()).hasSize(1);
        assertThat(messages.getContent().get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("Lead status workflow should transition correctly")
    void leadStatusWorkflow_transitions() {
        // Given
        Lead lead = createLead(LeadStatus.NEW);

        // When - Admin contacts lead
        leadService.updateLeadStatus(lead.getId(), LeadStatus.CONTACTED);

        // Then
        Lead updated = leadRepository.findById(lead.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LeadStatus.CONTACTED);
        assertThat(updated.getLastContactedAt()).isNotNull();

        // When - Lead converts (signs up as student)
        leadService.convertLead(lead.getId(), createStudent().getId());

        // Then
        Lead converted = leadRepository.findById(lead.getId()).orElseThrow();
        assertThat(converted.getStatus()).isEqualTo(LeadStatus.CONVERTED);
    }
}
```

### Test: Guest-to-Student Conversion Funnel

```java
@SpringBootTest
@Transactional
class ConversionFunnelIntegrationTest {

    @Autowired
    private LeadService leadService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Test
    @DisplayName("Full conversion flow: Guest → Trial → Lead → Student → Enrollment")
    void fullConversionFlow() {
        UUID tenantId = UUID.randomUUID();
        String email = "guest@example.com";

        // Step 1: Guest views trial lesson (create lead)
        CreateLeadRequest trialRequest = new CreateLeadRequest(
            email, "Nguyen Van B", "0901234568",
            LeadSource.TRIAL, null, "Interested in Java course"
        );
        LeadResponse lead = leadService.createLead(tenantId, trialRequest);
        assertThat(lead.status()).isEqualTo(LeadStatus.NEW);

        // Step 2: Admin contacts lead
        leadService.updateLeadStatus(lead.id(), LeadStatus.CONTACTED);

        // Step 3: Guest converts to student (signs up)
        CreateStudentRequest studentRequest = new CreateStudentRequest(
            "Nguyen Van B", email, "0901234568", null, null, null, null
        );
        StudentResponse student = studentService.createStudent(studentRequest);

        // Step 4: Mark lead as converted
        leadService.convertLead(lead.id(), student.id());

        Lead converted = leadRepository.findById(lead.id()).orElseThrow();
        assertThat(converted.getStatus()).isEqualTo(LeadStatus.CONVERTED);

        // Step 5: Create enrollment
        Course course = createCourse();
        EnrollmentRequest enrollRequest = new EnrollmentRequest(
            student.id(), course.getId(), LocalDate.now(), null, null, null
        );
        EnrollmentResponse enrollment = enrollmentService.createEnrollment(enrollRequest);

        // Then - Full funnel complete
        assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(student.email()).isEqualTo(email);
    }
}
```

---

# 7. TRIAL LEARNING SYSTEM INTEGRATION TESTS (V4.1 Phase 2) ⭐ NEW

## 7.1. Trial User Registration & Magic Link Flow

**Test Scenario**: Guest registers for trial → receives magic link → verifies → becomes TRIAL_USER

```java
@Test
void testTrialRegistrationWithMagicLink() {
    // Given - Guest wants to try course
    String email = "trial@example.com";
    CreateLeadRequest request = new CreateLeadRequest(
        email, "Nguyễn Văn Trial", "0912345678", courseId
    );

    // When - Register for trial
    LeadResponse lead = leadService.registerForTrial(request);

    // Then - Lead created with NEW status
    assertThat(lead.status()).isEqualTo(LeadStatus.NEW);
    assertThat(lead.source()).isEqualTo(LeadSource.TRIAL_SIGNUP);
    assertThat(lead.userId()).isNull(); // Not set yet (magic link not verified)

    // When - Gateway generates magic link (mocked)
    String magicToken = gatewayClient.generateMagicLink(email, instanceId);
    assertThat(magicToken).isNotNull();

    // When - User clicks magic link (Gateway verifies token)
    AuthResponse authResponse = gatewayClient.verifyMagicLink(magicToken);
    assertThat(authResponse.role()).isEqualTo("TRIAL_USER");

    // When - Core updates lead with user_id
    UUID userId = extractUserIdFromJwt(authResponse.accessToken());
    lead = leadService.updateLeadUserId(lead.id(), userId);

    // Then - Lead has user_id, ready for trial learning
    assertThat(lead.userId()).isEqualTo(userId);
}
```

**Assertions**:
- Lead created with status NEW
- Magic link token expires in 30 minutes
- JWT contains role = TRIAL_USER
- Lead.user_id updated after verification

---

## 7.2. Trial Quota Enforcement

**Test Scenario**: Trial user accesses lessons → quota tracked → limit enforced

```java
@Test
void testTrialQuotaEnforcement() {
    // Given - Trial user with 3 lessons/day quota
    UUID userId = createTrialUser("trial@example.com");
    Long lesson1Id = createTrialLesson("Lesson 1");
    Long lesson2Id = createTrialLesson("Lesson 2");
    Long lesson3Id = createTrialLesson("Lesson 3");
    Long lesson4Id = createTrialLesson("Lesson 4");

    // When - Access lesson 1
    LessonResponse lesson1 = lessonService.getLessonWithAccessControl(lesson1Id, userId, "TRIAL_USER");
    assertThat(lesson1.remainingQuota()).isEqualTo(2); // 2/3 remaining

    // When - Access lesson 2
    LessonResponse lesson2 = lessonService.getLessonWithAccessControl(lesson2Id, userId, "TRIAL_USER");
    assertThat(lesson2.remainingQuota()).isEqualTo(1); // 1/3 remaining

    // When - Access lesson 3
    LessonResponse lesson3 = lessonService.getLessonWithAccessControl(lesson3Id, userId, "TRIAL_USER");
    assertThat(lesson3.remainingQuota()).isEqualTo(0); // 0/3 remaining (quota exhausted)

    // When - Try to access lesson 4 (quota exceeded)
    assertThatThrownBy(() -> lessonService.getLessonWithAccessControl(lesson4Id, userId, "TRIAL_USER"))
        .isInstanceOf(QuotaExceededException.class)
        .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("TRIAL_QUOTA_EXCEEDED"));

    // When - Check quota status
    QuotaStatus status = trialQuotaService.getQuotaStatus(userId);
    assertThat(status.lessonsAccessed()).isEqualTo(3);
    assertThat(status.quotaLimit()).isEqualTo(3);
    assertThat(status.remaining()).isEqualTo(0);
}
```

**Assertions**:
- Quota increments after each lesson access
- Remaining quota decreases correctly
- QuotaExceededException thrown when limit reached
- QuotaStatus returns accurate counts

---

## 7.3. Trial Access Control (Paid Lesson Restriction)

**Test Scenario**: Trial user tries to access paid lesson → denied

```java
@Test
void testTrialUserCannotAccessPaidLesson() {
    // Given - Trial user
    UUID userId = createTrialUser("trial@example.com");

    // Given - Trial-accessible lesson
    Long trialLessonId = createLesson("Intro", true); // is_trial_accessible = true

    // Given - Paid-only lesson
    Long paidLessonId = createLesson("Advanced", false); // is_trial_accessible = false

    // When - Access trial lesson (allowed)
    LessonResponse trialLesson = lessonService.getLessonWithAccessControl(trialLessonId, userId, "TRIAL_USER");
    assertThat(trialLesson.id()).isEqualTo(trialLessonId);

    // When - Try to access paid lesson (denied)
    assertThatThrownBy(() -> lessonService.getLessonWithAccessControl(paidLessonId, userId, "TRIAL_USER"))
        .isInstanceOf(AccessDeniedException.class)
        .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("TRIAL_USER_PAID_LESSON_ACCESS_DENIED"));
}
```

**Assertions**:
- Trial users can access `is_trial_accessible = true` lessons
- Trial users CANNOT access `is_trial_accessible = false` lessons
- Clear error message for access denial

---

## 7.4. Multi-Tenant Isolation (Trial Users)

**Test Scenario**: Trial user can only access lessons in own tenant

```java
@Test
void testTrialUserMultiTenantIsolation() {
    // Given - Two tenants
    UUID tenant1 = UUID.randomUUID();
    UUID tenant2 = UUID.randomUUID();

    // Given - Trial user in tenant1
    UUID userId = createTrialUserInTenant("trial@tenant1.com", tenant1);

    // Given - Trial lesson in tenant1
    Long tenant1LessonId = createLessonInTenant("Lesson T1", true, tenant1);

    // Given - Trial lesson in tenant2
    Long tenant2LessonId = createLessonInTenant("Lesson T2", true, tenant2);

    // When - Access lesson in own tenant (allowed)
    setTenantContext(tenant1);
    LessonResponse lesson1 = lessonService.getLessonWithAccessControl(tenant1LessonId, userId, "TRIAL_USER");
    assertThat(lesson1.id()).isEqualTo(tenant1LessonId);

    // When - Try to access lesson in other tenant (denied)
    setTenantContext(tenant2);
    assertThatThrownBy(() -> lessonService.getLessonWithAccessControl(tenant2LessonId, userId, "TRIAL_USER"))
        .isInstanceOf(EntityNotFoundException.class)
        .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("LESSON_NOT_FOUND"));

    // Hibernate filter prevents cross-tenant access
}
```

**Assertions**:
- Trial users only see lessons in their tenant
- Cross-tenant access blocked by Hibernate filters
- Clear error (LESSON_NOT_FOUND) for unauthorized access

---

## 7.5. Lead to Student Conversion Flow

**Test Scenario**: Trial user completes payment → converts to STUDENT → progress preserved

```java
@Test
void testLeadConversionWithProgressPreservation() {
    // Given - Trial user with progress
    UUID userId = createTrialUser("trial@example.com");
    Long lesson1Id = createTrialLesson("Lesson 1");
    Long lesson2Id = createTrialLesson("Lesson 2");

    // Given - Trial user completes 2 lessons
    lessonService.getLessonWithAccessControl(lesson1Id, userId, "TRIAL_USER");
    lessonProgressService.updateProgress(lesson1Id, userId, 100); // 100% complete

    lessonService.getLessonWithAccessControl(lesson2Id, userId, "TRIAL_USER");
    lessonProgressService.updateProgress(lesson2Id, userId, 50); // 50% complete

    // When - User completes payment (mocked)
    UUID transactionId = mockPaymentService.processPayment(userId, courseId, 299000);
    assertThat(transactionId).isNotNull();

    // When - Convert lead to student
    Lead lead = leadRepository.findByUserIdAndDeletedFalse(userId).orElseThrow();
    ConvertLeadRequest request = new ConvertLeadRequest(courseId, transactionId);
    ConversionResponse response = leadService.convertToStudent(lead.id(), request);

    // Then - Conversion successful
    assertThat(response.userId()).isEqualTo(userId); // Same user_id
    assertThat(response.newRole()).isEqualTo("STUDENT");
    assertThat(response.enrollmentId()).isNotNull();

    // Then - Lead status updated
    Lead convertedLead = leadRepository.findById(lead.id()).orElseThrow();
    assertThat(convertedLead.getStatus()).isEqualTo(LeadStatus.CONVERTED);
    assertThat(convertedLead.getConvertedAt()).isNotNull();

    // Then - Progress preserved (same user_id)
    List<LessonProgress> progress = lessonProgressRepository.findByUserId(userId);
    assertThat(progress).hasSize(2);
    assertThat(progress).extracting(LessonProgress::getProgressPercent)
        .containsExactlyInAnyOrder(100, 50);

    // Then - User can now access paid lessons
    Long paidLessonId = createLesson("Advanced", false); // is_trial_accessible = false
    LessonResponse paidLesson = lessonService.getLessonWithAccessControl(paidLessonId, userId, "STUDENT");
    assertThat(paidLesson.id()).isEqualTo(paidLessonId); // Now accessible
}
```

**Assertions**:
- Payment verified before conversion
- User role updated: TRIAL_USER → STUDENT (same user_id)
- Lead status: NEW → CONVERTED
- Progress preserved (lesson_progress uses user_id)
- Student can access paid lessons after conversion

---

## 7.6. Daily Quota Reset

**Test Scenario**: Quota resets daily at midnight

```java
@Test
void testDailyQuotaReset() {
    // Given - Trial user exhausts quota on Day 1
    UUID userId = createTrialUser("trial@example.com");
    Long lesson1Id = createTrialLesson("Lesson 1");
    Long lesson2Id = createTrialLesson("Lesson 2");
    Long lesson3Id = createTrialLesson("Lesson 3");
    Long lesson4Id = createTrialLesson("Lesson 4");

    // Day 1: Access 3 lessons
    lessonService.getLessonWithAccessControl(lesson1Id, userId, "TRIAL_USER");
    lessonService.getLessonWithAccessControl(lesson2Id, userId, "TRIAL_USER");
    lessonService.getLessonWithAccessControl(lesson3Id, userId, "TRIAL_USER");

    // Day 1: Quota exceeded
    assertThatThrownBy(() -> lessonService.getLessonWithAccessControl(lesson4Id, userId, "TRIAL_USER"))
        .isInstanceOf(QuotaExceededException.class);

    // When - Next day (simulate by changing quota_date)
    LocalDate nextDay = LocalDate.now().plusDays(1);
    // Quota service creates new record for nextDay

    // Then - Quota reset (new record with lessons_accessed = 0)
    QuotaStatus statusDay2 = trialQuotaService.getQuotaStatus(userId);
    // Note: In real implementation, getQuotaStatus() checks current date
    // For testing, we manually set quota_date or use time mocking (e.g., Clock)

    assertThat(statusDay2.quotaDate()).isEqualTo(nextDay);
    assertThat(statusDay2.lessonsAccessed()).isEqualTo(0);
    assertThat(statusDay2.remaining()).isEqualTo(3); // Reset to 3

    // Day 2: Can access lessons again
    LessonResponse lesson = lessonService.getLessonWithAccessControl(lesson4Id, userId, "TRIAL_USER");
    assertThat(lesson.remainingQuota()).isEqualTo(2); // 1 accessed, 2 remaining
}
```

**Assertions**:
- New quota record created for each day
- Previous day's quota does not affect next day
- Quota resets to default limit (3 lessons)

**Implementation Note**: Use `Clock` bean for time mocking in tests:
```java
@TestConfiguration
static class TestClockConfig {
    @Bean
    public Clock clock() {
        return Clock.fixed(Instant.parse("2026-02-26T00:00:00Z"), ZoneId.systemDefault());
    }
}
```

---

## 7.7. E2E Scenario: Full Trial to Paid Flow

**Test Scenario**: Complete user journey from trial registration to paid student

```java
@Test
void testFullTrialToPaidFlow() {
    // Step 1: Guest registers for trial
    CreateLeadRequest registerRequest = new CreateLeadRequest(
        "guest@example.com", "Nguyễn Văn Guest", "0912345678", courseId
    );
    LeadResponse lead = leadService.registerForTrial(registerRequest);
    assertThat(lead.status()).isEqualTo(LeadStatus.NEW);

    // Step 2: Guest verifies magic link → becomes TRIAL_USER
    String magicToken = gatewayClient.generateMagicLink(lead.email(), instanceId);
    AuthResponse auth = gatewayClient.verifyMagicLink(magicToken);
    UUID userId = extractUserIdFromJwt(auth.accessToken());
    leadService.updateLeadUserId(lead.id(), userId);

    // Step 3: Trial user accesses 3 lessons over 3 days
    Long lesson1 = createTrialLesson("Intro to Java");
    Long lesson2 = createTrialLesson("Variables & Data Types");
    Long lesson3 = createTrialLesson("Control Flow");

    lessonService.getLessonWithAccessControl(lesson1, userId, "TRIAL_USER");
    lessonProgressService.updateProgress(lesson1, userId, 100);

    lessonService.getLessonWithAccessControl(lesson2, userId, "TRIAL_USER");
    lessonProgressService.updateProgress(lesson2, userId, 100);

    lessonService.getLessonWithAccessControl(lesson3, userId, "TRIAL_USER");
    lessonProgressService.updateProgress(lesson3, userId, 75);

    // Step 4: Trial user exhausts quota → sees upgrade prompt
    Long lesson4 = createTrialLesson("Functions");
    assertThatThrownBy(() -> lessonService.getLessonWithAccessControl(lesson4, userId, "TRIAL_USER"))
        .isInstanceOf(QuotaExceededException.class);

    // Step 5: User decides to upgrade → completes payment
    UUID transactionId = mockPaymentService.processPayment(userId, courseId, 299000);

    // Step 6: Convert to paid student
    ConvertLeadRequest convertRequest = new ConvertLeadRequest(courseId, transactionId);
    ConversionResponse conversion = leadService.convertToStudent(lead.id(), convertRequest);

    // Verify conversion
    assertThat(conversion.newRole()).isEqualTo("STUDENT");
    assertThat(conversion.enrollmentId()).isNotNull();

    // Step 7: Student can now access ALL lessons (no quota)
    Long paidLesson = createLesson("Advanced OOP", false); // Paid-only
    LessonResponse lesson = lessonService.getLessonWithAccessControl(paidLesson, userId, "STUDENT");
    assertThat(lesson.id()).isEqualTo(paidLesson);

    // Step 8: Verify progress preserved
    List<LessonProgress> progress = lessonProgressRepository.findByUserId(userId);
    assertThat(progress).hasSize(3); // Lessons 1-3 from trial period
    assertThat(progress).extracting(LessonProgress::getProgressPercent)
        .contains(100, 100, 75); // Progress preserved

    // Full flow complete!
}
```

**Assertions**:
- Each step validates expected state
- Progress tracked throughout trial period
- Quota enforcement works correctly
- Conversion preserves all progress
- Student role grants full access

---

# SUMMARY

**Testing Coverage:**
1. ✅ Feature Detection (unit + integration + API tests)
2. ✅ AI Branding (mocked OpenAI, job tracking)
3. ✅ VietQR Payment (order creation, confirmation, expiry)
4. ✅ Guest & Trial System (lifecycle, analytics)
5. ✅ Expand Services (ENGAGEMENT, MEDIA, PREMIUM)
6. ⭐ **V4.1 LMS Module** (guest access control, progress tracking)
7. ⭐ **V4.1 Marketing Module** (landing page, lead workflow, conversion funnel)
8. ⭐ **V4.1 Phase 2 Trial Learning System** (magic link auth, quota enforcement, access control, conversion flow) ⭐ NEW
9. ✅ E2E Scenarios (critical user flows)
10. ✅ Performance Tests (load testing with k6)
11. ✅ CI/CD Integration (GitHub Actions)

**Trial Learning Test Scenarios (Section 7) ⭐ NEW**:
- 7.1: Trial registration with magic link flow
- 7.2: Trial quota enforcement (3 lessons/day)
- 7.3: Trial access control (paid lesson restriction)
- 7.4: Multi-tenant isolation (trial users)
- 7.5: Lead to student conversion with progress preservation
- 7.6: Daily quota reset logic
- 7.7: E2E scenario (full trial to paid flow)

**Test Metrics:**
- Total tests: ~850+ (550 unit + 240 integration + 60 E2E)
- Coverage goal: 80%+
- P95 response time: <200ms
- Error rate: <1%

**Ready for:**
- Continuous Integration
- Continuous Deployment
- Production monitoring

**All 5 documents complete!** 🎉
