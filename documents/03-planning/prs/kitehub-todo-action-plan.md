# KiteHub TODO Action Plan - Complete Cleanup

**Generated:** 2026-03-12
**Scan Result:** 45 active TODOs in KiteHub, 0 in KiteClass
**Status:** Ready for implementation

---

## 📊 Executive Summary

### KiteClass Status: ✅ CLEAN
- **Active TODOs:** 0
- **TODO(future):** 2 (low priority, not blocking)
  - Invoice→Enrollment sync (event listener)
  - Tenant admin email from settings

### KiteHub Status: ⚠️ 45 TODOs
**Breakdown by Priority:**
- 🔴 **CRITICAL (Security):** 8 TODOs
- 🟡 **HIGH (Core Features):** 15 TODOs
- 🟢 **MEDIUM (Integrations):** 14 TODOs
- 🔵 **LOW (Nice-to-have):** 8 TODOs

**Breakdown by Module:**
- **kitehub-subscription:** 35 TODOs (78%)
- **kitehub-branding:** 7 TODOs (16%)
- **Documentation:** 3 TODOs (7%)

---

## 🔴 CRITICAL PRIORITY - Security (MUST FIX)

### PR 4.16: Database Password Encryption ✅ COMPLETE
**Branch:** `feature/PR-4.16-password-encryption` (merged)
**PR:** #57
**Merged:** 2026-03-12
**Priority:** 🔴 CRITICAL (Security vulnerability)

**Problem:** Database passwords stored in plain text - INSECURE for production

**Implementation Summary:**
- ✅ Created `EncryptionService.java` with AES-256-GCM encryption
- ✅ Updated `DatabaseProvisioningService` to encrypt/decrypt passwords
- ✅ Updated `MultiTenantDataSourceConfig` to decrypt passwords when creating connections
- ✅ Updated `DatabaseCredentials.fromInstance()` to decrypt passwords
- ✅ Implemented webhook signature verification with HMAC-SHA256
- ✅ Added comprehensive tests (14 encryption tests, 9 webhook tests)
- ✅ Fixed integration test issue with "pending" placeholder handling
- ✅ All CI checks passing (74 tests)

**Files Modified:**
- `DatabaseProvisioningService.java`
- `MultiTenantDataSourceConfig.java`
- `DatabaseCredentials.java`
- `PaymentWebhookController.java`
- `EncryptionService.java` (NEW)
- `EncryptionServiceTest.java` (NEW)
- `PaymentWebhookControllerTest.java` (NEW)
- `application.yml` (added encryption & webhook config)
- `application-test.yml` (NEW - test configuration)

**Success Criteria:**
- ✅ Password encrypted before saving to DB
- ✅ Password decrypted when loading DataSource
- ✅ Master key configurable via environment variable
- ✅ Webhook signature verified
- ✅ Tests passing (encryption round-trip)
- ⚠️ Documentation: Master key rotation strategy (TODO future)

---

## 🟡 HIGH PRIORITY - Core Features

### PR 4.17: Database Lifecycle Management ✅ COMPLETE
**Branch:** `feature/PR-4.17-database-lifecycle` (merged)
**PR:** #58
**Merged:** 2026-03-12
**Priority:** 🟡 HIGH (Core functionality)
**Depends on:** PR 4.16 (encryption) ✅

**Problem:** Database provisioning is simulated, not creating real databases

**Implementation Summary:**
- ✅ Created `DatabaseConnectionService` for admin connections
- ✅ Created `FlywayMigrationService` for schema migrations
- ✅ Implemented `createPhysicalDatabase()` - CREATE DATABASE/USER
- ✅ Implemented `dropPhysicalDatabase()` - DROP DATABASE/USER with termination
- ✅ Implemented actual `checkDatabaseHealth()` - SELECT 1 query
- ✅ Added 19 KiteClass schema migrations
- ✅ SQL injection protection (identifier sanitization)
- ✅ Feature flag: `database.lifecycle.enabled` (default: false)
- ✅ Unit tests for all new services
- ⚠️ S3 backup integration (deferred to future PR)
- ⚠️ Testcontainers integration tests (deferred to future PR)

**Implementation Steps:**

#### 1. PostgreSQL Admin Connection (2h)
Create `DatabaseConnectionService.java`:
```java
@Service
public class DatabaseConnectionService {
    @Value("${database.admin.url}")
    private String adminUrl;

    @Value("${database.admin.username}")
    private String adminUsername;

    @Value("${database.admin.password}")
    private String adminPassword;

    public Connection getAdminConnection() throws SQLException {
        return DriverManager.getConnection(adminUrl, adminUsername, adminPassword);
    }
}
```

**Configuration:**
```yaml
database:
  admin:
    url: jdbc:postgresql://localhost:5433/postgres
    username: postgres
    password: ${POSTGRES_ADMIN_PASSWORD}
  master:
    host: localhost
    port: 5433
```

#### 2. Physical Database Creation (2h)
Implement `createPhysicalDatabase()`:
```java
private void createPhysicalDatabase(String dbName, String username, String password) {
    try (Connection conn = connectionService.getAdminConnection();
         Statement stmt = conn.createStatement()) {

        // Create user
        stmt.execute(String.format(
            "CREATE USER %s WITH PASSWORD '%s'", username, password));

        // Create database
        stmt.execute(String.format(
            "CREATE DATABASE %s OWNER %s", dbName, username));

        // Grant privileges
        stmt.execute(String.format(
            "GRANT ALL PRIVILEGES ON DATABASE %s TO %s", dbName, username));

        log.info("Database created: {}", dbName);
    }
}
```

#### 3. Flyway Migrations (2h)
Create `FlywayMigrationService.java`:
```java
@Service
public class FlywayMigrationService {

    public void runMigrations(String url, String username, String password) {
        Flyway flyway = Flyway.configure()
            .dataSource(url, username, password)
            .locations("classpath:db/migration/kiteclass")
            .load();

        flyway.migrate();
        log.info("Migrations completed for: {}", url);
    }
}
```

Create migrations directory:
- `kitehub-subscription/src/main/resources/db/migration/kiteclass/`
- Copy KiteClass migrations: V1__init.sql, V2__attendance.sql, etc.

#### 4. Database Deletion (1h)
Implement `dropPhysicalDatabase()`:
```java
private void dropPhysicalDatabase(String dbName, String username) {
    try (Connection conn = connectionService.getAdminConnection();
         Statement stmt = conn.createStatement()) {

        // Terminate connections
        stmt.execute(String.format(
            "SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
            "WHERE datname = '%s'", dbName));

        // Drop database
        stmt.execute(String.format("DROP DATABASE IF EXISTS %s", dbName));

        // Drop user
        stmt.execute(String.format("DROP USER IF EXISTS %s", username));

        log.info("Database dropped: {}", dbName);
    }
}
```

#### 5. Health Check (1h)
Implement `checkDatabaseHealth()`:
```java
public boolean checkDatabaseHealth(UUID instanceId) {
    Instance instance = instanceRepository.findById(instanceId)
        .orElseThrow();

    try (Connection conn = DriverManager.getConnection(
            instance.getDatabaseUrl(),
            instance.getDatabaseUsername(),
            encryptionService.decrypt(instance.getDatabasePassword()))) {

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT 1");
        return rs.next();

    } catch (SQLException e) {
        log.error("Health check failed for instance {}: {}", instanceId, e.getMessage());
        return false;
    }
}
```

#### 6. Database Backup (2h)
Implement `backupDatabase()`:
```java
private void backupDatabase(String dbName) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String backupFile = String.format("/tmp/%s-%s.sql", dbName, timestamp);

    ProcessBuilder pb = new ProcessBuilder(
        "pg_dump",
        "-h", masterHost,
        "-p", masterPort,
        "-U", adminUsername,
        "-d", dbName,
        "-f", backupFile
    );

    pb.environment().put("PGPASSWORD", adminPassword);

    Process process = pb.start();
    int exitCode = process.waitFor();

    if (exitCode == 0) {
        // Upload to S3
        s3Service.uploadBackup(backupFile, dbName, timestamp);
        log.info("Backup created: {}", backupFile);
    } else {
        throw new RuntimeException("Backup failed with exit code: " + exitCode);
    }
}
```

**Dependencies:**
- Add to `pom.xml`:
  ```xml
  <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
  </dependency>
  <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
  </dependency>
  ```

**Integration Tests:**
```java
@SpringBootTest
@Testcontainers
class DatabaseProvisioningIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withUsername("postgres")
        .withPassword("postgres");

    @Test
    void shouldCreateAndDeleteDatabase() {
        UUID instanceId = UUID.randomUUID();

        // Create
        DatabaseCredentials creds = service.provisionDatabase(instanceId);
        assertThat(creds).isNotNull();

        // Verify connectivity
        boolean healthy = service.checkDatabaseHealth(instanceId);
        assertThat(healthy).isTrue();

        // Delete
        service.deleteDatabase(instanceId);
    }
}
```

**Files Created/Modified:**
- ✅ `DatabaseConnectionService.java` (NEW)
- ✅ `FlywayMigrationService.java` (NEW)
- ✅ `DatabaseProvisioningService.java` (MODIFIED - implemented all methods)
- ✅ `application.yml` (MODIFIED - added database.admin, lifecycle config)
- ✅ `application-test.yml` (MODIFIED - lifecycle.enabled = false)
- ✅ `DatabaseConnectionServiceTest.java` (NEW)
- ✅ `FlywayMigrationServiceTest.java` (NEW)
- ✅ `DatabaseProvisioningServiceTest.java` (MODIFIED - added new mocks)
- ✅ 19 SQL migration files (NEW - KiteClass schema)
- ⚠️ `S3BackupService.java` (DEFERRED to future PR)
- ⚠️ `DatabaseBackupScheduler.java` (DEFERRED to future PR)
- ⚠️ `DatabaseProvisioningIntegrationTest.java` (DEFERRED to future PR)

**Success Criteria:**
- ✅ Real databases created in PostgreSQL (with feature flag)
- ✅ Flyway migrations run on new instances
- ✅ Databases properly deleted with cleanup
- ✅ Health check returns actual connection status
- ✅ SQL injection protection implemented
- ✅ Feature flag for dev/prod modes
- ✅ Unit tests passing (76 total)
- ⚠️ Backups uploaded to S3 (deferred)
- ⚠️ Integration tests with Testcontainers (deferred)

---

### PR 4.18: Payment Service Integration (4-5 hours)
**Branch:** `feature/PR-4.18-payment-integration`
**Priority:** 🟡 HIGH (Referenced as PR 4.6)

**Problem:** Payment records not created, prorated charges not calculated

**TODOs (3 total):**
1. `SubscriptionService.java:165` - Create payment record for prorated charge
2. `SubscriptionService.java:199` - Implement pending tier change
3. `SubscriptionRenewalService.java:59` - Create payment invoice

**Implementation Steps:**

#### 1. Payment Record Creation (2h)
```java
// In SubscriptionService
public SubscriptionResponse upgradeTier(UUID instanceId, SubscriptionTier newTier) {
    // ... existing code ...

    // Calculate prorated charge
    BigDecimal proratedAmount = calculateProratedAmount(
        subscription.getCurrentTier(),
        newTier,
        subscription.getRenewsAt()
    );

    // Create payment record
    Payment payment = Payment.builder()
        .instanceId(instanceId)
        .amount(proratedAmount)
        .currency("VND")
        .paymentMethod(PaymentMethod.BANK_TRANSFER)
        .status(PaymentStatus.PENDING)
        .description(String.format("Prorated charge: %s → %s",
            subscription.getCurrentTier(), newTier))
        .build();

    paymentRepository.save(payment);

    // Update subscription (immediate if paid, pending if not)
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
        subscription.setCurrentTier(newTier);
    } else {
        subscription.setPendingTier(newTier);
        subscription.setPendingPaymentId(payment.getId());
    }

    // ... rest of code ...
}

private BigDecimal calculateProratedAmount(
        SubscriptionTier currentTier,
        SubscriptionTier newTier,
        Instant renewsAt) {

    long daysRemaining = ChronoUnit.DAYS.between(Instant.now(), renewsAt);
    long daysInMonth = 30;

    BigDecimal currentMonthly = currentTier.getMonthlyPrice();
    BigDecimal newMonthly = newTier.getMonthlyPrice();

    BigDecimal proratedCurrent = currentMonthly
        .multiply(BigDecimal.valueOf(daysRemaining))
        .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

    BigDecimal proratedNew = newMonthly
        .multiply(BigDecimal.valueOf(daysRemaining))
        .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

    return proratedNew.subtract(proratedCurrent);
}
```

#### 2. Pending Tier Change (1h)
Add fields to `Subscription` entity:
```java
@Entity
@Table(name = "subscriptions")
public class Subscription {
    // ... existing fields ...

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_tier")
    private SubscriptionTier pendingTier;

    @Column(name = "pending_payment_id")
    private Long pendingPaymentId;
}
```

Migration:
```sql
-- V12__add_pending_tier.sql
ALTER TABLE subscriptions
ADD COLUMN pending_tier VARCHAR(20),
ADD COLUMN pending_payment_id BIGINT;

ALTER TABLE subscriptions
ADD CONSTRAINT fk_pending_payment
FOREIGN KEY (pending_payment_id) REFERENCES payments(id);
```

#### 3. Payment Invoice Creation (2h)
```java
// In SubscriptionRenewalService
private void processRenewal(Subscription subscription) {
    // ... existing code ...

    // Create invoice
    Invoice invoice = Invoice.builder()
        .instanceId(subscription.getInstanceId())
        .subscriptionId(subscription.getId())
        .amount(subscription.getCurrentTier().getMonthlyPrice())
        .currency("VND")
        .dueDate(subscription.getRenewsAt().plus(7, ChronoUnit.DAYS))
        .status(InvoiceStatus.PENDING)
        .items(List.of(
            InvoiceItem.builder()
                .description("Monthly subscription: " + subscription.getCurrentTier())
                .amount(subscription.getCurrentTier().getMonthlyPrice())
                .build()
        ))
        .build();

    invoiceRepository.save(invoice);

    // Send payment reminder email
    emailService.sendPaymentReminder(
        subscription.getInstance().getContactEmail(),
        invoice
    );

    log.info("Invoice created for subscription renewal: {}", subscription.getId());
}
```

**Files to Create/Modify:**
- `Payment.java` (entity - may already exist)
- `PaymentRepository.java`
- `Invoice.java` (entity)
- `InvoiceRepository.java`
- `SubscriptionService.java` (MODIFY)
- `SubscriptionRenewalService.java` (MODIFY)
- `V12__add_pending_tier.sql` (migration)

**Success Criteria:**
- ✅ Prorated charges calculated correctly
- ✅ Payment records created on tier upgrade
- ✅ Pending tier changes stored and processed
- ✅ Invoices generated on renewal
- ✅ Tests covering edge cases (same-day upgrade, downgrade)
- ✅ Database migration V6 for pending tier fields
- ✅ 91 tests passing (7 SubscriptionService + 10 SubscriptionRenewalService)

---

## 🟢 MEDIUM PRIORITY - Integrations

### PR 4.19: Email Service Integration (3-4 hours)
**Branch:** `feature/PR-4.19-email-integration`
**Priority:** 🟢 MEDIUM (Referenced as PR 4.12)

**Problem:** Email notifications stubbed, not sending real emails

**TODOs (7 total):**
1. `TrialExpirationChecker.java:51` - Send trial expired email
2. `TrialExpirationChecker.java:84` - Send trial warning email
3. `SubscriptionExpirationChecker.java:121,127` - Send expiration emails (2 TODOs)
4. `SubscriptionRenewalService.java:66` - Send payment reminder
5. `SubscriptionRenewalService.java:159` - Send suspension notification
6. `README.md:177` - Email notification reference

**Implementation:**

#### 1. Create Email Service Client (2h)
```java
@Service
@RequiredArgsConstructor
public class EmailServiceClient {

    private final RestTemplate restTemplate;

    @Value("${email.service.url}")
    private String emailServiceUrl;

    public void sendTrialExpirationWarning(String email, String instanceName, int daysRemaining) {
        EmailRequest request = EmailRequest.builder()
            .to(email)
            .subject("Your KiteClass trial expires in " + daysRemaining + " days")
            .template("trial-expiration-warning")
            .variables(Map.of(
                "instanceName", instanceName,
                "daysRemaining", daysRemaining,
                "upgradeUrl", "https://kitehub.com/subscription/upgrade"
            ))
            .build();

        restTemplate.postForEntity(
            emailServiceUrl + "/api/v1/emails/send",
            request,
            Void.class
        );
    }

    public void sendTrialExpired(String email, String instanceName) {
        // Similar implementation
    }

    public void sendPaymentReminder(String email, Invoice invoice) {
        // Similar implementation
    }

    public void sendSuspensionNotification(String email, String instanceName) {
        // Similar implementation
    }
}
```

#### 2. Update Schedulers (1h)
Replace TODO comments with actual email calls:
```java
// In TrialExpirationChecker
emailServiceClient.sendTrialExpired(
    instance.getContactEmail(),
    instance.getName()
);

// In SubscriptionExpirationChecker
emailServiceClient.sendPaymentReminder(
    instance.getContactEmail(),
    invoice
);
```

#### 3. Email Templates (1h)
Create templates (if using KiteClass email service, reuse LoggingEmailService pattern):
- `trial-expiration-warning.html`
- `trial-expired.html`
- `payment-reminder.html`
- `subscription-suspended.html`

**Configuration:**
```yaml
email:
  service:
    url: http://localhost:8081 # KiteClass Gateway
```

**Files to Create/Modify:**
- `EmailServiceClient.java` (NEW)
- `EmailRequest.java` (DTO - NEW)
- `TrialExpirationChecker.java` (MODIFY)
- `SubscriptionExpirationChecker.java` (MODIFY)
- `SubscriptionRenewalService.java` (MODIFY)

**Success Criteria:**
- [ ] Trial expiration emails sent
- [ ] Payment reminder emails sent
- [ ] Suspension notifications sent
- [ ] Email failures don't break scheduler logic
- [ ] Tests with mocked email service

---

### PR 4.20: VietQR API Integration (2-3 hours)
**Branch:** `feature/PR-4.20-vietqr-integration`
**Priority:** 🟢 MEDIUM (External integration)

**Problem:** VietQR API stubbed, returning mock data

**TODOs (4 total):**
1. `VietQRService.java:36` - Integrate with real VietQR API
2. `VietQRService.java:48` - Call real VietQR API
3. `VietQRService.java:77` - Integrate with bank API
4. `VietQRService.java:88` - Query bank API to verify

**Implementation:**

#### 1. VietQR API Client (2h)
```java
@Service
@RequiredArgsConstructor
public class VietQRService {

    private final RestTemplate restTemplate;

    @Value("${vietqr.api.url:https://api.vietqr.io}")
    private String apiUrl;

    @Value("${vietqr.api.key}")
    private String apiKey;

    public String generateQRCode(
            BigDecimal amount,
            String description,
            String instanceId) {

        VietQRRequest request = VietQRRequest.builder()
            .accountNo("1234567890") // From config
            .accountName("KITECLASS VIETNAM")
            .acqId("970415") // Vietinbank
            .amount(amount.longValue())
            .addInfo(description)
            .format("text")
            .template("compact")
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<VietQRRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<VietQRResponse> response = restTemplate.postForEntity(
            apiUrl + "/v2/generate",
            entity,
            VietQRResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getData().getQrDataURL();
        }

        throw new RuntimeException("Failed to generate QR code");
    }
}
```

#### 2. Bank Transaction Verification (1h)
```java
public boolean verifyTransaction(String transactionId, BigDecimal expectedAmount) {
    // Option 1: Webhook from bank (preferred)
    // Option 2: Polling bank API (if available)
    // Option 3: Manual verification by admin

    // For MVP: Return true after manual verification period
    log.info("Transaction verification requested: {}", transactionId);

    // TODO: Implement actual bank API integration
    // Most banks don't have public APIs, need business partnership

    return false; // Require manual verification
}
```

**Configuration:**
```yaml
vietqr:
  api:
    url: https://api.vietqr.io
    key: ${VIETQR_API_KEY}
  bank:
    account-no: 1234567890
    account-name: KITECLASS VIETNAM
    bank-id: 970415 # Vietinbank
```

**Files to Create/Modify:**
- `VietQRService.java` (MODIFY)
- `VietQRRequest.java` (DTO - NEW)
- `VietQRResponse.java` (DTO - NEW)
- `VietQRServiceTest.java` (tests with mock API)

**Success Criteria:**
- [ ] Real QR codes generated
- [ ] QR codes contain correct payment info
- [ ] Transaction verification implemented (even if manual)
- [ ] Error handling for API failures
- [ ] Tests with mocked VietQR API

---

### PR 4.21: Branding Content Persistence (2-3 hours)
**Branch:** `feature/PR-4.21-branding-persistence`
**Priority:** 🟢 MEDIUM (Referenced as PR 4.9)

**Problem:** Generated content not persisted, assets not stored

**TODOs (5 total):**
1. `ContentGenerationController.java:63` - Implement content persistence
2. `AssetStorageController.java:87` - Query from BrandingJob entity
3. `AssetStorageController.java:121` - Delete from S3 and update entity
4. `ContentGenerationService.java:163` - Implement proper JSON parsing
5. `OpenAIClient.java:162` - Parse JSON from content

**Implementation:**

#### 1. BrandingJob Entity & Persistence (2h)
```java
@Entity
@Table(name = "branding_jobs")
public class BrandingJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id")
    private UUID instanceId;

    @Enumerated(EnumType.STRING)
    private JobStatus status; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "generated_content", columnDefinition = "TEXT")
    private String generatedContent; // JSON

    @Column(name = "s3_logo_url")
    private String s3LogoUrl;

    @Column(name = "s3_hero_url")
    private String s3HeroUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}

@Repository
public interface BrandingJobRepository extends JpaRepository<BrandingJob, Long> {
    Optional<BrandingJob> findByInstanceId(UUID instanceId);
}
```

#### 2. Update Controllers (1h)
```java
// In ContentGenerationController
@PostMapping("/generate")
public ResponseEntity<ContentGenerationResponse> generateContent(@RequestBody GenerateContentRequest request) {
    // Generate content
    ContentGenerationResponse response = contentGenerationService.generateContent(request);

    // Persist to database
    BrandingJob job = BrandingJob.builder()
        .instanceId(request.getInstanceId())
        .status(JobStatus.COMPLETED)
        .generatedContent(objectMapper.writeValueAsString(response))
        .createdAt(Instant.now())
        .completedAt(Instant.now())
        .build();

    brandingJobRepository.save(job);

    return ResponseEntity.ok(response);
}

// In AssetStorageController
@GetMapping("/assets")
public ResponseEntity<List<AssetResponse>> listAssets(@RequestParam UUID instanceId) {
    BrandingJob job = brandingJobRepository.findByInstanceId(instanceId)
        .orElseThrow(() -> new EntityNotFoundException("No branding job found"));

    List<AssetResponse> assets = new ArrayList<>();
    if (job.getS3LogoUrl() != null) {
        assets.add(new AssetResponse("logo", job.getS3LogoUrl()));
    }
    if (job.getS3HeroUrl() != null) {
        assets.add(new AssetResponse("hero", job.getS3HeroUrl()));
    }

    return ResponseEntity.ok(assets);
}
```

**Files to Create/Modify:**
- `BrandingJob.java` (entity - NEW)
- `BrandingJobRepository.java` (NEW)
- `ContentGenerationController.java` (MODIFY)
- `AssetStorageController.java` (MODIFY)
- `V3__create_branding_jobs.sql` (migration - NEW)

**Success Criteria:**
- [ ] Generated content saved to database
- [ ] S3 URLs stored in BrandingJob
- [ ] Assets queryable by instance ID
- [ ] Asset deletion removes from S3 and DB
- [ ] Tests for persistence logic

---

## 🔵 LOW PRIORITY - Nice-to-have

### PR 4.22: JSON Parsing Improvements (1-2 hours)
**Branch:** `feature/PR-4.22-json-parsing`
**Priority:** 🔵 LOW (Code quality improvement)

**TODOs (2 total):**
1. `ContentGenerationService.java:163` - Proper JSON parsing
2. `OpenAIClient.java:162` - Parse JSON from content

**Current Issue:** Regex-based parsing, brittle and error-prone

**Better Approach:**
```java
// In OpenAIClient
public <T> T parseStructuredOutput(String content, Class<T> clazz) {
    try {
        // OpenAI structured output already returns valid JSON
        return objectMapper.readValue(content, clazz);
    } catch (JsonProcessingException e) {
        log.error("Failed to parse JSON: {}", content);
        throw new RuntimeException("Invalid JSON from OpenAI", e);
    }
}
```

**Files to Modify:**
- `OpenAIClient.java`
- `ContentGenerationService.java`

---

## 📋 Implementation Roadmap

### Phase 1: Critical Security (Week 1)
**Goal:** Fix security vulnerabilities before production

| PR | Priority | Effort | TODOs Resolved |
|----|----------|--------|----------------|
| PR 4.16: Password Encryption | 🔴 CRITICAL | 4-5h | 8 |

**Total:** 4-5 hours, 8 TODOs

---

### Phase 2: Core Features (Week 2-3) ✅ **COMPLETE**
**Goal:** Complete database lifecycle and payment integration

| PR | Priority | Effort | TODOs Resolved | Status |
|----|----------|--------|----------------|--------|
| PR 4.17: Database Lifecycle | 🟡 HIGH | 6-8h | 7 | ✅ Merged (#58) |
| PR 4.18: Payment Integration | 🟡 HIGH | 4-5h | 3 | ✅ Merged (#59) |

**Total:** 10-13 hours, 10 TODOs, **ALL COMPLETE**

---

### Phase 3: External Integrations (Week 4)
**Goal:** Email notifications and payment verification

| PR | Priority | Effort | TODOs Resolved |
|----|----------|--------|----------------|
| PR 4.19: Email Service | 🟢 MEDIUM | 3-4h | 7 |
| PR 4.20: VietQR API | 🟢 MEDIUM | 2-3h | 4 |
| PR 4.21: Branding Persistence | 🟢 MEDIUM | 2-3h | 5 |

**Total:** 7-10 hours, 16 TODOs

---

### Phase 4: Polish (Week 5)
**Goal:** Code quality improvements

| PR | Priority | Effort | TODOs Resolved |
|----|----------|--------|----------------|
| PR 4.22: JSON Parsing | 🔵 LOW | 1-2h | 2 |
| Documentation cleanup | 🔵 LOW | 1h | 2 |

**Total:** 2-3 hours, 4 TODOs

---

## 📊 Summary Statistics

**Total TODOs:** 45
- KiteHub: 45 (100%)
- KiteClass: 0 (0%)

**By Priority:**
- 🔴 CRITICAL: 8 (18%)
- 🟡 HIGH: 10 (22%)
- 🟢 MEDIUM: 16 (36%)
- 🔵 LOW: 4 (9%)
- 📄 Documentation: 7 (16%)

**Total Effort:** 23-31 hours (~4-6 weeks part-time)

**Completion Order:**
1. Week 1: PR 4.16 (Security) ← **START HERE**
2. Week 2-3: PR 4.17-4.18 (Core Features)
3. Week 4: PR 4.19-4.21 (Integrations)
4. Week 5: PR 4.22 (Polish)

---

## ✅ Success Metrics

**After Phase 1 (Week 1):**
- [ ] No plain text passwords in database
- [ ] Webhook signatures verified
- [ ] Security scan passes

**After Phase 2 (Week 3):**
- [ ] Real databases provisioned
- [ ] Payment records created
- [ ] Database backups working

**After Phase 3 (Week 4):**
- [ ] Email notifications sent
- [ ] QR codes generated via API
- [ ] Content persisted to database

**After Phase 4 (Week 5):**
- [ ] 0 TODO comments remaining
- [ ] All tests passing
- [ ] Production-ready

---

## 🚀 Next Steps

**Immediate Action (Today):**
1. Review this plan
2. Approve Phase 1 (PR 4.16 - Password Encryption)
3. Create feature branch: `feature/PR-4.16-password-encryption`
4. Start implementation

**This Week:**
- Complete PR 4.16 (4-5h)
- Test encryption/decryption
- Merge to main
- Update MEMORY.md with encryption pattern

**Next 2 Weeks:**
- PR 4.17: Database Lifecycle (6-8h)
- PR 4.18: Payment Integration (4-5h)

---

**Document Version:** 1.0
**Last Updated:** 2026-03-12
**Author:** Development Team + Claude Sonnet 4.5
**Status:** ✅ Ready for Approval
