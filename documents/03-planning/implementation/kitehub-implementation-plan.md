# KITEHUB IMPLEMENTATION PLAN - Platform-Level Services

**Version:** 1.0
**Created:** 2026-01-30
**Service:** KiteHub (Platform-level)
**Purpose:** Multi-tenant management, subscription, AI services, payment

**Tham chiếu:**
- `system-architecture-v3-final.md` (PHẦN 6B-6F)
- `architecture-clarification-qa.md` (PART 1-5)
- `backend-implementation-plan-v2.md` (KiteClass instance-level)

---

## MỤC LỤC

1. [Tổng quan Kiến trúc](#tổng-quan-kiến-trúc)
2. [Phase 1: Multi-Tenant Infrastructure](#phase-1-multi-tenant-infrastructure)
3. [Phase 2: Subscription Management](#phase-2-subscription-management)
4. [Phase 3: VietQR Payment (KiteHub Level)](#phase-3-vietqr-payment-kitehub-level)
5. [Phase 4: AI Branding Service](#phase-4-ai-branding-service)
6. [Phase 5: Email & Notification Service](#phase-5-email--notification-service)
7. [Phase 6: Admin Portal Backend](#phase-6-admin-portal-backend)
8. [Phase 7: API Gateway](#phase-7-api-gateway)
9. [Deployment & Infrastructure](#deployment--infrastructure)

---

# TỔNG QUAN KIẾN TRÚC

## KiteHub vs KiteClass

```
┌─────────────────────────────────────────────────────────┐
│  KITEHUB (Platform Level)                               │
│  - Multi-tenant management                              │
│  - Subscription & billing                               │
│  - AI Branding Service (centralized)                    │
│  - Email & Notification                                 │
│  - Admin Portal                                         │
│  - API Gateway                                          │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │Instance 1│  │Instance 2│  │Instance N│            │
│  │(KiteClass│  │(KiteClass│  │(KiteClass│            │
│  │  Core)   │  │  Core)   │  │  Core)   │            │
│  └──────────┘  └──────────┘  └──────────┘            │
│                                                         │
│  Each instance: Independent database, users, data      │
└─────────────────────────────────────────────────────────┘
```

## Tech Stack

```yaml
Platform Services (KiteHub):
  Framework: Spring Boot 3.2+
  Language: Java 17
  Database: PostgreSQL 15 (shared for platform data)
  Cache: Redis 7
  Message Queue: RabbitMQ
  AI Services: OpenAI API (GPT-4 Vision, DALL-E 3)
  Email: AWS SES
  Storage: AWS S3
  Payment: VietQR API

Architecture:
  Pattern: Microservices
  Deployment: Kubernetes (EKS)
  Service Mesh: Istio (optional)
```

## Service Breakdown

| Service | Responsibility | Database | Scaling |
|---------|----------------|----------|---------|
| **kitehub-subscription** | Subscription management, trial tracking | PostgreSQL | Horizontal |
| **kitehub-payment** | VietQR payment processing | PostgreSQL | Horizontal |
| **kitehub-branding** | AI branding generation (GPT-4, DALL-E) | PostgreSQL + S3 | Vertical (GPU) |
| **kitehub-email** | Email & notifications (AWS SES) | PostgreSQL (logs) | Horizontal |
| **kitehub-admin** | Admin portal backend | PostgreSQL | Horizontal |
| **kitehub-gateway** | API Gateway, routing | Redis (rate limit) | Horizontal |

---

# PHASE 1: MULTI-TENANT INFRASTRUCTURE

**Duration:** 1 tuần
**Dependencies:** None

## 1.1. Instance Entity

```java
// com/kitehub/platform/domain/entity/Instance.java
package com.kitehub.platform.domain.entity;

import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Instance (Tenant) entity
 *
 * Each instance = 1 KiteClass organization
 */
@Entity
@Table(name = "instances", indexes = {
    @Index(name = "idx_instances_subdomain", columnList = "subdomain", unique = true),
    @Index(name = "idx_instances_owner", columnList = "owner_id")
})
@Getter
@Setter
public class Instance extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String subdomain; // customer1.kiteclass.com

    @Column(name = "custom_domain", length = 255)
    private String customDomain; // mydomain.com (PREMIUM only)

    @Column(nullable = false, length = 200)
    private String organizationName;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId; // CENTER_OWNER user ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PricingTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstanceStatus status; // TRIAL, ACTIVE, SUSPENDED, DELETED

    // Database connection info
    @Column(name = "database_url", nullable = false, length = 500)
    private String databaseUrl;

    @Column(name = "database_username", nullable = false, length = 100)
    private String databaseUsername;

    @Column(name = "database_password", nullable = false, length = 255)
    private String databasePassword; // Encrypted

    // Trial tracking
    @Column(name = "trial_started_at")
    private LocalDateTime trialStartedAt;

    @Column(name = "trial_expires_at")
    private LocalDateTime trialExpiresAt;

    // Subscription tracking
    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    /**
     * Check if instance is on trial
     */
    public boolean isOnTrial() {
        return InstanceStatus.TRIAL.equals(this.status) &&
               this.trialExpiresAt != null &&
               this.trialExpiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Get days left in trial
     */
    public long getTrialDaysLeft() {
        if (!isOnTrial()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(),
            this.trialExpiresAt
        );
    }

    /**
     * Check if instance is active
     */
    public boolean isActive() {
        return InstanceStatus.ACTIVE.equals(this.status) ||
               (isOnTrial() && getTrialDaysLeft() > 0);
    }
}

// InstanceStatus.java
public enum InstanceStatus {
    TRIAL,      // 14-day trial period
    ACTIVE,     // Paid subscription active
    SUSPENDED,  // Subscription expired or payment failed
    DELETED     // Soft deleted
}
```

## 1.2. Instance Provisioning Service

```java
// com/kitehub/platform/service/InstanceProvisioningService.java
package com.kitehub.platform.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Instance Provisioning Service
 *
 * Handles instance creation, database provisioning, trial setup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstanceProvisioningService {

    private final InstanceRepository instanceRepo;
    private final DatabaseProvisioningService databaseService;
    private final InstanceConfigService instanceConfigService;

    /**
     * Create new instance with 14-day trial
     *
     * @param ownerId Owner user ID
     * @param organizationName Organization name
     * @param subdomain Subdomain (e.g., "customer1")
     * @return Created instance
     */
    @Transactional
    public Instance createTrialInstance(
        UUID ownerId,
        String organizationName,
        String subdomain
    ) {
        // Validate subdomain availability
        if (instanceRepo.existsBySubdomain(subdomain)) {
            throw new ConflictException("Subdomain already exists: " + subdomain);
        }

        // Provision dedicated database for instance
        DatabaseProvisioningResult dbResult = databaseService.provisionDatabase(subdomain);

        // Create instance
        Instance instance = new Instance();
        instance.setOwnerId(ownerId);
        instance.setOrganizationName(organizationName);
        instance.setSubdomain(subdomain);
        instance.setTier(PricingTier.BASIC); // Default to BASIC during trial
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setTrialStartedAt(LocalDateTime.now());
        instance.setTrialExpiresAt(LocalDateTime.now().plusDays(14)); // 14-day trial
        instance.setDatabaseUrl(dbResult.getJdbcUrl());
        instance.setDatabaseUsername(dbResult.getUsername());
        instance.setDatabasePassword(encrypt(dbResult.getPassword()));

        instanceRepo.save(instance);

        // Initialize instance configuration (feature flags)
        instanceConfigService.initializeTrialConfig(instance.getId());

        // Run Flyway migrations on instance database
        databaseService.runMigrations(instance);

        log.info("Trial instance created: {} ({})", organizationName, subdomain);

        return instance;
    }

    /**
     * Activate paid subscription
     */
    @Transactional
    public void activateSubscription(
        UUID instanceId,
        PricingTier tier,
        UUID subscriptionId
    ) {
        Instance instance = instanceRepo.findById(instanceId)
            .orElseThrow(() -> new NotFoundException("Instance not found"));

        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setTier(tier);
        instance.setSubscriptionId(subscriptionId);
        instance.setSubscriptionExpiresAt(LocalDateTime.now().plusMonths(1)); // 1 month

        instanceRepo.save(instance);

        // Update instance config với tier mới
        instanceConfigService.updateTierConfig(instanceId, tier);

        log.info("Subscription activated for instance {}: {}", instanceId, tier);
    }

    /**
     * Suspend instance (payment failed or expired)
     */
    @Transactional
    public void suspendInstance(UUID instanceId, String reason) {
        Instance instance = instanceRepo.findById(instanceId)
            .orElseThrow(() -> new NotFoundException("Instance not found"));

        instance.setStatus(InstanceStatus.SUSPENDED);
        instanceRepo.save(instance);

        log.warn("Instance suspended: {} - Reason: {}", instanceId, reason);
    }

    private String encrypt(String password) {
        // Use AES encryption
        // Implementation omitted for brevity
        return password; // TODO: Implement encryption
    }
}
```

## 1.3. Subdomain Routing Configuration

```java
// com/kitehub/gateway/routing/SubdomainRouter.java
package com.kitehub.gateway.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Subdomain Router
 *
 * Routes requests based on subdomain:
 * - customer1.kiteclass.com → Instance 1
 * - customer2.kiteclass.com → Instance 2
 * - mydomain.com → Instance N (custom domain)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubdomainRouterFilter extends AbstractGatewayFilterFactory<SubdomainRouterFilter.Config> {

    private final InstanceService instanceService;

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String host = exchange.getRequest().getURI().getHost();

            // Extract subdomain or custom domain
            Instance instance = resolveInstance(host);

            if (instance == null || !instance.isActive()) {
                throw new InstanceNotAvailableException("Instance not available: " + host);
            }

            // Add instance ID to headers
            exchange.getRequest().mutate()
                .header("X-Instance-Id", instance.getId().toString())
                .header("X-Instance-Tier", instance.getTier().name())
                .build();

            log.debug("Routed request to instance: {} ({})", instance.getOrganizationName(), host);

            return chain.filter(exchange);
        };
    }

    private Instance resolveInstance(String host) {
        // Check if custom domain
        Instance instance = instanceService.findByCustomDomain(host);
        if (instance != null) {
            return instance;
        }

        // Extract subdomain (customer1.kiteclass.com → customer1)
        if (host.endsWith(".kiteclass.com")) {
            String subdomain = host.substring(0, host.indexOf(".kiteclass.com"));
            return instanceService.findBySubdomain(subdomain);
        }

        return null;
    }

    public static class Config {
        // Configuration properties
    }
}
```

**Tasks Phase 1:**
- [x] Instance entity
- [x] Instance provisioning service
- [x] Database provisioning (per-instance)
- [x] Subdomain routing
- [x] Custom domain support (PREMIUM)
- [x] Trial expiration tracking

**Deliverables:**
- Multi-tenant infrastructure ready
- Instance provisioning API
- Subdomain routing functional
- Per-instance database isolation

---

# PHASE 2: SUBSCRIPTION MANAGEMENT

**Duration:** 1 tuần
**Dependencies:** Phase 1
**Reference:** PHẦN 6E (Trial System), PART 4

## 2.1. Subscription Entity

```java
// com/kitehub/platform/domain/entity/Subscription.java
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PricingTier tier;

    @Column(nullable = false)
    private Long monthlyPrice; // VND

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "current_period_start", nullable = false)
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd;

    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    /**
     * Check if subscription is active
     */
    public boolean isActive() {
        return SubscriptionStatus.ACTIVE.equals(this.status) &&
               this.currentPeriodEnd.isAfter(LocalDateTime.now());
    }
}

public enum SubscriptionStatus {
    TRIAL,      // Trial period
    ACTIVE,     // Paid and active
    PAST_DUE,   // Payment failed, grace period
    CANCELED,   // User canceled
    EXPIRED     // Trial or subscription expired
}
```

## 2.2. Subscription Lifecycle Service

```java
// com/kitehub/platform/service/SubscriptionLifecycleService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleService {

    private final SubscriptionRepository subscriptionRepo;
    private final InstanceRepository instanceRepo;
    private final EmailService emailService;

    /**
     * Cron job: Check trial expirations
     *
     * Runs daily at 00:00
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void checkTrialExpirations() {
        LocalDateTime now = LocalDateTime.now();

        // Find instances with expiring trials
        List<Instance> expiringInstances = instanceRepo.findByTrialExpiresBetween(
            now,
            now.plusDays(4) // Day 11, 12, 13, 14
        );

        for (Instance instance : expiringInstances) {
            long daysLeft = instance.getTrialDaysLeft();

            // Send expiration warnings
            if (daysLeft == 3 || daysLeft == 1) {
                emailService.sendTrialExpirationWarning(instance, daysLeft);
                log.info("Sent trial expiration warning: {} ({} days left)",
                    instance.getOrganizationName(), daysLeft);
            }
        }

        // Find expired trials (grace period Day +1, +2, +3)
        List<Instance> expiredTrials = instanceRepo.findByTrialExpiresBetween(
            now.minusDays(3),
            now
        );

        for (Instance instance : expiredTrials) {
            handleExpiredTrial(instance);
        }
    }

    /**
     * Handle expired trial
     *
     * - Day +1, +2, +3: Grace period (read-only)
     * - Day +4: Suspend instance
     * - Day +90: Delete data
     */
    private void handleExpiredTrial(Instance instance) {
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
            instance.getTrialExpiresAt(),
            LocalDateTime.now()
        );

        if (daysOverdue <= 3) {
            // Grace period: Set read-only mode
            instanceConfigService.setReadOnlyMode(instance.getId(), true);
            emailService.sendGracePeriodNotice(instance, daysOverdue);
            log.info("Instance in grace period: {} (day +{})",
                instance.getOrganizationName(), daysOverdue);

        } else if (daysOverdue <= 90) {
            // Suspend instance
            instance.setStatus(InstanceStatus.SUSPENDED);
            instanceRepo.save(instance);
            emailService.sendInstanceSuspended(instance);
            log.warn("Instance suspended: {}", instance.getOrganizationName());

        } else {
            // Delete data (90 days retention)
            instance.setStatus(InstanceStatus.DELETED);
            instance.setDeletedAt(LocalDateTime.now());
            instanceRepo.save(instance);
            log.warn("Instance data deleted: {}", instance.getOrganizationName());
        }
    }

    /**
     * Upgrade trial to paid subscription
     */
    @Transactional
    public Subscription upgradeToPaid(
        UUID instanceId,
        PricingTier tier,
        UUID paymentOrderId
    ) {
        Instance instance = instanceRepo.findById(instanceId)
            .orElseThrow(() -> new NotFoundException("Instance not found"));

        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setInstanceId(instanceId);
        subscription.setTier(tier);
        subscription.setMonthlyPrice(tier.getPriceVND());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));

        subscriptionRepo.save(subscription);

        // Update instance
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setTier(tier);
        instance.setSubscriptionId(subscription.getId());
        instance.setSubscriptionExpiresAt(subscription.getCurrentPeriodEnd());
        instanceRepo.save(instance);

        // Send confirmation email
        emailService.sendSubscriptionActivated(instance, subscription);

        log.info("Subscription upgraded: {} → {}", instance.getOrganizationName(), tier);

        return subscription;
    }
}
```

**Tasks Phase 2:**
- [x] Subscription entity & lifecycle
- [x] Trial expiration tracking (Day 11, 13, 14)
- [x] Grace period (Day +1, +2, +3)
- [x] Data retention (90 days)
- [x] Upgrade flow (trial → paid)
- [x] Email notifications

**Deliverables:**
- Subscription management working
- Trial expiration emails sent
- Grace period read-only mode
- 90-day data retention enforced

---

# PHASE 3: VIETQR PAYMENT (KITEHUB LEVEL)

**Duration:** 1 tuần
**Dependencies:** Phase 1, 2
**Reference:** PHẦN 6F (VietQR Payment), PART 5

**See:** `backend-implementation-plan-v2.md` Phase 7 for detailed VietQR implementation.

## Key Points for KiteHub:

```java
// KiteHub-specific payment configuration
public class KiteHubPaymentConfig {
    public static final String KITEHUB_BANK_BIN = "970415"; // Vietcombank
    public static final String KITEHUB_ACCOUNT = "1234567890";
    public static final String KITEHUB_ACCOUNT_NAME = "CONG TY TNHH KITECLASS";
}

// Payment flow
@Service
public class KiteHubPaymentService {

    /**
     * Create subscription payment order
     */
    public PaymentOrder createSubscriptionOrder(
        UUID instanceId,
        PricingTier tier
    ) {
        // Generate VietQR for KiteHub bank account
        // Content: "KITEHUB {orderId} {ownerEmail}"
        // Amount: 499k, 999k, or 1499k
    }

    /**
     * Admin confirms payment manually (MVP)
     */
    public void confirmPayment(String orderId) {
        // Mark order as PAID
        // Activate subscription
        // Upgrade instance tier
    }
}
```

**Deliverables:**
- VietQR payment for subscriptions
- Admin verification panel
- Subscription activation on payment

---

# PHASE 4: AI BRANDING SERVICE

**Duration:** 2 tuần
**Dependencies:** Phase 1
**Reference:** PHẦN 6C (AI Branding), PART 2

## 4.1. AI Service Integration

```java
// com/kitehub/branding/service/AIBrandingService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class AIBrandingService {

    private final OpenAIClient openAIClient;
    private final S3StorageService s3Service;
    private final BrandingJobRepository jobRepo;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    /**
     * Generate branding assets using GPT-4 Vision + DALL-E 3
     *
     * Input: Logo image
     * Output: 10+ assets (profile images, hero images, banners, OG image)
     */
    @Async
    public CompletableFuture<BrandingAssets> generateBrandingAssets(
        String jobId,
        MultipartFile logoFile,
        String organizationName,
        String language
    ) {
        try {
            // Update job status
            updateJobProgress(jobId, 5, "Analyzing logo...");

            // Step 1: Analyze logo với GPT-4 Vision
            LogoAnalysis analysis = analyzeLogoWithGPT4Vision(logoFile, organizationName);
            updateJobProgress(jobId, 20, "Logo analyzed. Generating images...");

            // Step 2: Generate profile images (cutout, circle, square)
            Map<String, String> profileImages = generateProfileImages(logoFile, analysis);
            updateJobProgress(jobId, 40, "Profile images generated.");

            // Step 3: Generate hero images (3 variations)
            List<String> heroImages = generateHeroImages(analysis, organizationName, language);
            updateJobProgress(jobId, 60, "Hero images generated.");

            // Step 4: Generate brand logos (light, dark)
            List<String> brandLogos = generateBrandLogos(logoFile, analysis);
            updateJobProgress(jobId, 75, "Brand logos generated.");

            // Step 5: Generate social media banners (Facebook, YouTube)
            List<String> banners = generateSocialBanners(analysis, organizationName);
            updateJobProgress(jobId, 85, "Social banners generated.");

            // Step 6: Generate OG image
            String ogImage = generateOGImage(analysis, organizationName);
            updateJobProgress(jobId, 95, "OG image generated.");

            // Step 7: Generate marketing copy (headline, tagline, description)
            MarketingCopy copy = generateMarketingCopy(analysis, organizationName, language);
            updateJobProgress(jobId, 100, "Completed!");

            // Assemble result
            BrandingAssets assets = BrandingAssets.builder()
                .profileImages(profileImages)
                .heroImages(heroImages)
                .brandLogos(brandLogos)
                .banners(banners)
                .ogImage(ogImage)
                .marketingCopy(copy)
                .build();

            // Mark job as completed
            completeJob(jobId, assets);

            return CompletableFuture.completedFuture(assets);

        } catch (Exception e) {
            log.error("Failed to generate branding assets for job {}: {}", jobId, e.getMessage());
            failJob(jobId, e.getMessage());
            throw new AIBrandingException("Branding generation failed", e);
        }
    }

    /**
     * Analyze logo với GPT-4 Vision
     */
    private LogoAnalysis analyzeLogoWithGPT4Vision(
        MultipartFile logoFile,
        String organizationName
    ) {
        // Convert image to base64
        String base64Image = encodeImageToBase64(logoFile);

        // Call GPT-4 Vision API
        String prompt = String.format(
            "Analyze this logo for %s. Extract: " +
            "1. Primary colors (hex codes), " +
            "2. Typography style, " +
            "3. Design theme (modern/traditional/playful/etc.), " +
            "4. Target audience, " +
            "5. Brand personality",
            organizationName
        );

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-4-vision-preview")
            .messages(List.of(
                ChatMessage.builder()
                    .role("user")
                    .content(List.of(
                        ContentPart.text(prompt),
                        ContentPart.image(base64Image)
                    ))
                    .build()
            ))
            .maxTokens(1000)
            .build();

        ChatCompletionResponse response = openAIClient.createChatCompletion(request);
        String analysisText = response.getChoices().get(0).getMessage().getContent();

        // Parse analysis (simplified)
        LogoAnalysis analysis = new LogoAnalysis();
        analysis.setColors(extractColors(analysisText));
        analysis.setTheme(extractTheme(analysisText));
        // ... parse other fields

        return analysis;
    }

    /**
     * Generate hero images với DALL-E 3
     */
    private List<String> generateHeroImages(
        LogoAnalysis analysis,
        String organizationName,
        String language
    ) {
        List<String> heroImages = new ArrayList<>();

        // Generate 3 variations
        for (int i = 1; i <= 3; i++) {
            String prompt = buildHeroImagePrompt(analysis, organizationName, language, i);

            ImageGenerationRequest request = ImageGenerationRequest.builder()
                .model("dall-e-3")
                .prompt(prompt)
                .n(1)
                .size("1792x1024") // Hero image dimensions
                .quality("hd")
                .build();

            ImageGenerationResponse response = openAIClient.createImage(request);
            String imageUrl = response.getData().get(0).getUrl();

            // Download and upload to S3
            String s3Url = downloadAndUploadToS3(imageUrl, "hero-" + i + ".jpg");
            heroImages.add(s3Url);
        }

        return heroImages;
    }

    /**
     * Build hero image prompt
     */
    private String buildHeroImagePrompt(
        LogoAnalysis analysis,
        String organizationName,
        String language,
        int variation
    ) {
        String basePrompt = String.format(
            "Create a professional hero banner image for %s, an education center. " +
            "Theme: %s. Colors: %s. " +
            "Style: Modern, clean, inspiring. " +
            "Include: Students learning, bright classroom, technology. " +
            "No text, no logos.",
            organizationName,
            analysis.getTheme(),
            String.join(", ", analysis.getColors())
        );

        // Add variation-specific details
        return switch (variation) {
            case 1 -> basePrompt + " Focus on happy students collaborating.";
            case 2 -> basePrompt + " Focus on teacher explaining on whiteboard.";
            case 3 -> basePrompt + " Focus on students using tablets/computers.";
            default -> basePrompt;
        };
    }

    /**
     * Generate marketing copy với GPT-4
     */
    private MarketingCopy generateMarketingCopy(
        LogoAnalysis analysis,
        String organizationName,
        String language
    ) {
        String prompt = String.format(
            "Generate marketing copy in %s for %s, an education center. " +
            "Brand theme: %s. Target audience: Parents looking for quality education. " +
            "Generate: 1) Headline (1 sentence, catchy), " +
            "2) Tagline (short phrase), " +
            "3) Description (2-3 sentences).",
            language.equals("vi") ? "Vietnamese" : "English",
            organizationName,
            analysis.getTheme()
        );

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(List.of(
                ChatMessage.builder()
                    .role("user")
                    .content(prompt)
                    .build()
            ))
            .temperature(0.9) // Creative
            .maxTokens(500)
            .build();

        ChatCompletionResponse response = openAIClient.createChatCompletion(request);
        String copyText = response.getChoices().get(0).getMessage().getContent();

        // Parse (simplified)
        MarketingCopy copy = new MarketingCopy();
        copy.setHeadline(extractHeadline(copyText));
        copy.setTagline(extractTagline(copyText));
        copy.setDescription(extractDescription(copyText));

        return copy;
    }

    // Helper methods omitted for brevity...
}
```

**Tasks Phase 4:**
- [x] OpenAI client integration
- [x] GPT-4 Vision logo analysis
- [x] DALL-E 3 image generation
- [x] S3 storage for assets
- [x] Job queue & progress tracking
- [x] Webhook notifications

**Deliverables:**
- AI branding service functional
- 10+ assets generated per job
- Multi-language support (vi, en)
- 5-minute average generation time

---

# PHASE 5: EMAIL & NOTIFICATION SERVICE

**Duration:** 3 ngày
**Dependencies:** Phase 2
**Reference:** PART 5 (AWS SES)

```java
// com/kitehub/email/service/EmailService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final AmazonSimpleEmailService sesClient;
    private final TemplateEngine templateEngine;

    @Value("${email.from}")
    private String fromEmail;

    /**
     * Send trial expiration warning
     */
    public void sendTrialExpirationWarning(Instance instance, long daysLeft) {
        String subject = String.format(
            "⚠️ Còn %d ngày sử dụng thử KiteClass",
            daysLeft
        );

        Map<String, Object> variables = Map.of(
            "organizationName", instance.getOrganizationName(),
            "daysLeft", daysLeft,
            "upgradeUrl", "https://kiteclass.com/upgrade?instance=" + instance.getId()
        );

        String htmlBody = templateEngine.process("trial-expiration-warning", variables);

        sendEmail(instance.getOwnerEmail(), subject, htmlBody);
    }

    /**
     * Send payment confirmation
     */
    public void sendPaymentConfirmation(PaymentOrder order, Subscription subscription) {
        String subject = "✅ Thanh toán thành công - KiteClass";

        Map<String, Object> variables = Map.of(
            "orderId", order.getOrderId(),
            "amount", order.getAmount(),
            "tier", subscription.getTier().getDisplayName(),
            "expiresAt", subscription.getCurrentPeriodEnd()
        );

        String htmlBody = templateEngine.process("payment-confirmation", variables);

        sendEmail(order.getUserEmail(), subject, htmlBody);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            SendEmailRequest request = new SendEmailRequest()
                .withSource(fromEmail)
                .withDestination(new Destination().withToAddresses(to))
                .withMessage(new Message()
                    .withSubject(new Content().withData(subject))
                    .withBody(new Body().withHtml(new Content().withData(htmlBody)))
                );

            sesClient.sendEmail(request);
            log.info("Email sent to {}: {}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
```

**Email Templates:**
- `trial-expiration-warning.html` (Day 11, 13, 14, +1, +2, +3)
- `payment-confirmation.html`
- `subscription-activated.html`
- `instance-suspended.html`
- `welcome-email.html`

**Deliverables:**
- AWS SES integration
- 5+ email templates
- Email sending service
- Unsubscribe links (GDPR)

---

# PHASE 6: ADMIN PORTAL BACKEND

**Duration:** 1 tuần
**Dependencies:** All previous phases

```java
// com/kitehub/admin/controller/AdminInstanceController.java
@RestController
@RequestMapping("/api/v1/admin/instances")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminInstanceController {

    private final InstanceService instanceService;

    /**
     * List all instances
     */
    @GetMapping
    public ResponseEntity<Page<InstanceDTO>> listInstances(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) InstanceStatus status
    ) {
        Page<InstanceDTO> instances = instanceService.findAll(status, page, size);
        return ResponseEntity.ok(instances);
    }

    /**
     * Get instance details
     */
    @GetMapping("/{instanceId}")
    public ResponseEntity<InstanceDetailsDTO> getInstanceDetails(
        @PathVariable UUID instanceId
    ) {
        InstanceDetailsDTO details = instanceService.getDetails(instanceId);
        return ResponseEntity.ok(details);
    }

    /**
     * Suspend instance
     */
    @PostMapping("/{instanceId}/suspend")
    public ResponseEntity<Void> suspendInstance(
        @PathVariable UUID instanceId,
        @RequestBody SuspendRequest request
    ) {
        instanceService.suspend(instanceId, request.getReason());
        return ResponseEntity.ok().build();
    }

    /**
     * Reactivate instance
     */
    @PostMapping("/{instanceId}/reactivate")
    public ResponseEntity<Void> reactivateInstance(
        @PathVariable UUID instanceId
    ) {
        instanceService.reactivate(instanceId);
        return ResponseEntity.ok().build();
    }
}

// Admin payment verification
@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminPaymentController {

    @GetMapping("/pending")
    public ResponseEntity<List<PaymentOrderDTO>> getPendingPayments() {
        // Return all pending payment orders
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirmPayment(
        @PathVariable String orderId,
        @RequestBody PaymentConfirmRequest request
    ) {
        // Manually confirm payment
        // Activate subscription
    }
}
```

**Admin APIs:**
- Instance management (list, details, suspend, reactivate)
- Subscription management (view, extend, cancel)
- Payment verification (pending, confirm, reject)
- User management (view, suspend)
- Analytics dashboard (instance stats, revenue)

**Deliverables:**
- Admin backend APIs
- Instance management
- Payment verification panel
- Analytics endpoints

---

# PHASE 7: API GATEWAY

**Duration:** 3 ngày
**Dependencies:** None (can be parallel)

```yaml
# Spring Cloud Gateway configuration
spring:
  cloud:
    gateway:
      routes:
        # Public APIs (no auth)
        - id: public-routes
          uri: lb://kiteclass-core
          predicates:
            - Path=/api/v1/public/**
          filters:
            - RateLimit=100,1m

        # Authenticated APIs
        - id: instance-apis
          uri: lb://kiteclass-core
          predicates:
            - Path=/api/v1/**
          filters:
            - JwtAuthenticationFilter
            - RateLimit=1000,1m

        # Admin APIs
        - id: admin-apis
          uri: lb://kitehub-admin
          predicates:
            - Path=/api/v1/admin/**
          filters:
            - AdminAuthenticationFilter
            - RateLimit=500,1m
```

**Features:**
- Subdomain routing
- JWT authentication
- Rate limiting
- Request logging
- Circuit breaker

---

# DEPLOYMENT & INFRASTRUCTURE

## Kubernetes Deployment

```yaml
# kitehub-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kitehub-subscription
spec:
  replicas: 3
  selector:
    matchLabels:
      app: kitehub-subscription
  template:
    metadata:
      labels:
        app: kitehub-subscription
    spec:
      containers:
      - name: kitehub-subscription
        image: kiteclass/kitehub-subscription:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: kitehub-secrets
              key: database-url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

## Infrastructure Checklist

- [ ] PostgreSQL RDS (Multi-AZ)
- [ ] Redis ElastiCache (cluster mode)
- [ ] RabbitMQ (Amazon MQ)
- [ ] S3 buckets (branding assets)
- [ ] CloudFront CDN
- [ ] Route53 DNS
- [ ] EKS cluster
- [ ] Load balancers
- [ ] Auto-scaling groups
- [ ] Monitoring (CloudWatch, Prometheus)
- [ ] Logging (ELK stack)
- [ ] Backup & disaster recovery

---

# SUMMARY

**KiteHub Services:**
1. ✅ Multi-Tenant Infrastructure
2. ✅ Subscription Management
3. ✅ VietQR Payment
4. ✅ AI Branding Service
5. ✅ Email & Notification
6. ✅ Admin Portal
7. ✅ API Gateway

**Total Estimated Duration:** 8-10 tuần (2-2.5 tháng)

**Ready for:**
- Instance provisioning
- Trial management (14 days)
- Subscription lifecycle
- VietQR payment processing
- AI branding generation
- Email notifications
- Admin management

**Next Steps:**
1. Setup infrastructure (EKS, RDS, Redis)
2. Implement Phase 1 (Multi-tenant)
3. Deploy incrementally (phase by phase)
4. Integration testing với KiteClass
5. Load testing & optimization
