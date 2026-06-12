# KiteHub Database Provisioning Service

**Version:** 1.0
**Created:** 2026-03-09
**Purpose:** Design automatic PostgreSQL database provisioning for new KiteClass instances
**Status:** Design phase (to be implemented in PR 4.2)

---

## Table of Contents

1. [Overview](#overview)
2. [Provisioning Workflow](#provisioning-workflow)
3. [Database Naming Strategy](#database-naming-strategy)
4. [Credentials Management](#credentials-management)
5. [Connection Pooling](#connection-pooling)
6. [Database Creation](#database-creation)
7. [Schema Migration](#schema-migration)
8. [Backup Strategy](#backup-strategy)
9. [Monitoring & Alerts](#monitoring--alerts)
10. [Cleanup & Deprovisioning](#cleanup--deprovisioning)

---

## Overview

**Architecture Pattern:** Database-per-tenant (complete isolation)

**Purpose:** Automatically create and configure PostgreSQL databases for new KiteClass instances when users sign up.

**Key Benefits:**
- **Complete Isolation:** Each instance has own database (no cross-tenant data access risk)
- **Independent Scaling:** Scale individual instance databases without affecting others
- **Compliance:** Easy to meet data residency requirements (e.g., GDPR, Vietnamese Law)
- **Backup/Restore:** Restore single instance without touching other instances
- **Security:** Physical database separation eliminates shared-table vulnerabilities

**Trade-offs:**
- **Cost:** More databases = higher infrastructure cost (mitigated by shared PostgreSQL server for MVP)
- **Complexity:** Managing 100+ databases requires automation (handled by this service)
- **Connection Pooling:** Need per-instance connection pools (solved with HikariCP)

---

## Provisioning Workflow

### End-to-End Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. USER SIGNS UP                                            │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. SUBSCRIPTION SERVICE (PR 4.1)                            │
│    - Create Instance record (status: PROVISIONING)          │
│    - Generate subdomain (e.g., "abc123")                    │
│    - Call DatabaseProvisioningService                       │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. DATABASE PROVISIONING SERVICE (PR 4.2)                   │
│    - Generate unique database name                          │
│    - Create PostgreSQL database                             │
│    - Create database user with restricted permissions       │
│    - Encrypt credentials                                    │
│    - Store in Instance table:                               │
│      * database_url                                         │
│      * database_username                                    │
│      * database_password (encrypted)                        │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. FLYWAY MIGRATION SERVICE (PR 4.2)                        │
│    - Connect to new instance database                       │
│    - Run all V*.sql migrations:                             │
│      * V1__create_students_table.sql                        │
│      * V2__create_teachers_table.sql                        │
│      * V3__create_courses_table.sql                         │
│      * ... (all Core Service migrations)                    │
│    - Seed default data:                                     │
│      * Admin user                                           │
│      * System settings                                      │
│      * Default roles                                        │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. KUBERNETES DEPLOYMENT SERVICE (PR 4.3)                   │
│    - Create Deployment: kiteclass-core-{instanceId}         │
│    - Inject database credentials via Secret                 │
│    - Deploy to namespace: kiteclass-instances               │
│    - Configure environment variables:                       │
│      * SPRING_DATASOURCE_URL={instance_db_url}              │
│      * SPRING_DATASOURCE_USERNAME={username}                │
│      * SPRING_DATASOURCE_PASSWORD={password}                │
│    - Wait for Pod to be Ready                               │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. INSTANCE ACTIVATION                                      │
│    - Update Instance status: ACTIVE                         │
│    - Send welcome email (RabbitMQ → Email Service)          │
│    - User can access: https://abc123.kitehub.me          │
└─────────────────────────────────────────────────────────────┘
```

### Implementation (Java)

**Interface:**
```java
public interface DatabaseProvisioningService {
    /**
     * Provision a new PostgreSQL database for a KiteClass instance
     *
     * @param instanceId UUID of the instance
     * @return DatabaseCredentials with connection details
     * @throws DatabaseProvisioningException if provisioning fails
     */
    DatabaseCredentials provisionDatabase(UUID instanceId);

    /**
     * Deprovision (delete) an instance database
     *
     * @param instanceId UUID of the instance
     * @param backupFirst Whether to backup before deletion
     */
    void deprovisionDatabase(UUID instanceId, boolean backupFirst);

    /**
     * Test connection to an instance database
     *
     * @param credentials Database credentials to test
     * @return true if connection successful
     */
    boolean testConnection(DatabaseCredentials credentials);
}
```

**Implementation:**
```java
@Service
@Slf4j
public class DatabaseProvisioningServiceImpl implements DatabaseProvisioningService {

    @Value("${postgres.master.url}")
    private String masterPostgresUrl; // jdbc:postgresql://kitehub-postgres:5432/postgres

    @Value("${postgres.master.username}")
    private String masterUsername; // postgres

    @Value("${postgres.master.password}")
    private String masterPassword;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private AES256Encryptor encryptor;

    @Autowired
    private FlywayMigrationService flywayService;

    @Override
    @Transactional
    public DatabaseCredentials provisionDatabase(UUID instanceId) {
        log.info("Starting database provisioning for instance: {}", instanceId);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new InstanceNotFoundException(instanceId));

        try {
            // 1. Generate database name (kiteclass_{first_8_chars_of_uuid})
            String dbName = generateDatabaseName(instanceId);
            String dbUsername = dbName + "_user";
            String dbPassword = generateSecurePassword(32);

            // 2. Create database and user in PostgreSQL
            createDatabaseAndUser(dbName, dbUsername, dbPassword);

            // 3. Build connection URL
            String dbUrl = buildDatabaseUrl(dbName);

            // 4. Encrypt password
            String encryptedPassword = encryptor.encrypt(dbPassword);

            // 5. Save credentials to Instance table
            instance.setDatabaseUrl(dbUrl);
            instance.setDatabaseUsername(dbUsername);
            instance.setDatabasePassword(encryptedPassword);
            instance.setStatus(InstanceStatus.PROVISIONING);
            instanceRepository.save(instance);

            // 6. Run Flyway migrations
            DatabaseCredentials credentials = new DatabaseCredentials(
                dbUrl, dbUsername, dbPassword
            );
            flywayService.runMigrations(credentials);

            log.info("Database provisioning successful for instance: {}", instanceId);
            return credentials;

        } catch (Exception e) {
            log.error("Database provisioning failed for instance: {}", instanceId, e);
            instance.setStatus(InstanceStatus.PROVISIONING_FAILED);
            instanceRepository.save(instance);
            throw new DatabaseProvisioningException("Failed to provision database", e);
        }
    }

    private void createDatabaseAndUser(String dbName, String dbUsername, String dbPassword) {
        try (Connection conn = DriverManager.getConnection(
                masterPostgresUrl, masterUsername, masterPassword)) {

            try (Statement stmt = conn.createStatement()) {
                // Create database
                stmt.execute("CREATE DATABASE " + dbName);
                log.info("Database created: {}", dbName);

                // Create user
                String createUserSql = String.format(
                    "CREATE USER %s WITH PASSWORD '%s'",
                    dbUsername, dbPassword
                );
                stmt.execute(createUserSql);
                log.info("User created: {}", dbUsername);

                // Grant privileges
                String grantSql = String.format(
                    "GRANT ALL PRIVILEGES ON DATABASE %s TO %s",
                    dbName, dbUsername
                );
                stmt.execute(grantSql);
                log.info("Privileges granted to user: {}", dbUsername);
            }

        } catch (SQLException e) {
            throw new DatabaseProvisioningException("Failed to create database and user", e);
        }
    }

    private String generateDatabaseName(UUID instanceId) {
        // Use first 8 characters of UUID for short, unique name
        return "kiteclass_" + instanceId.toString().substring(0, 8).replace("-", "");
    }

    private String generateSecurePassword(int length) {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    private String buildDatabaseUrl(String dbName) {
        // For local dev: jdbc:postgresql://kitehub-postgres:5432/{dbName}
        // For production: jdbc:postgresql://cloudsql-instance-ip:5432/{dbName}
        return String.format("jdbc:postgresql://%s:5432/%s",
            masterPostgresUrl.split("/")[2].split(":")[0], // Extract host
            dbName
        );
    }
}
```

---

## Database Naming Strategy

### Format

**Pattern:** `kiteclass_{uuid_short}`

**Example:**
- Instance ID: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- Database name: `kiteclass_a1b2c3d4`

**Username:** `kiteclass_a1b2c3d4_user`

**Why this format:**
1. **Unique:** UUID guarantees uniqueness
2. **Short:** Only 8 characters (keeps connection strings manageable)
3. **Readable:** Clear prefix identifies KiteClass databases
4. **Sortable:** Alphabetically sortable for easy management
5. **No special chars:** Avoids PostgreSQL identifier issues

### Collision Risk

**Probability:** ~0.000001% (1 in 100 million)

**Why it's safe:**
- 8 hex chars = 4.3 billion combinations
- Even with 100,000 instances, collision probability < 0.001%
- Provisioning service checks existence before creating

**Collision Handling:**
```java
private String generateDatabaseName(UUID instanceId) {
    String baseName = "kiteclass_" + instanceId.toString().substring(0, 8).replace("-", "");

    // Check if database already exists
    if (databaseExists(baseName)) {
        // Fallback: use first 12 characters for more uniqueness
        baseName = "kiteclass_" + instanceId.toString().substring(0, 12).replace("-", "");
    }

    return baseName;
}
```

---

## Credentials Management

### Generation

**Password Requirements:**
- Length: 32 characters
- Character set: A-Z, a-z, 0-9, special chars (!@#$%^&*)
- Entropy: ~190 bits (cryptographically strong)

**Implementation:**
```java
private String generateSecurePassword(int length) {
    SecureRandom random = new SecureRandom();
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    StringBuilder password = new StringBuilder();
    for (int i = 0; i < length; i++) {
        password.append(chars.charAt(random.nextInt(chars.length())));
    }
    return password.toString();
}
```

### Encryption (AES-256)

**Master Key Storage:**
- **Local Dev:** Environment variable `ENCRYPTION_MASTER_KEY`
- **Staging/Production:** AWS Secrets Manager or HashiCorp Vault

**Encryption Algorithm:** AES-256-GCM (Galois/Counter Mode)

**Implementation:**
```java
@Component
public class AES256Encryptor {

    @Value("${encryption.master-key}")
    private String masterKey; // 32-byte key from Secrets Manager

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public String encrypt(String plaintext) throws EncryptionException {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                Base64.getDecoder().decode(masterKey), "AES"
            );
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            // Return Base64-encoded
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt", e);
        }
    }

    public String decrypt(String encryptedText) throws EncryptionException {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                Base64.getDecoder().decode(masterKey), "AES"
            );
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt", e);
        }
    }
}
```

### Storage in Database

**Instance Table:**
```sql
CREATE TABLE instances (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(63) UNIQUE NOT NULL,
    database_url TEXT NOT NULL,              -- Plain text (not sensitive)
    database_username VARCHAR(255) NOT NULL, -- Plain text (not sensitive)
    database_password TEXT NOT NULL,         -- Encrypted with AES-256
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Why encrypt password but not URL/username:**
- Password: High value secret (grants full database access)
- URL/Username: Low value info (useless without password)
- Encryption overhead: Only applied where necessary

### Key Rotation

**Frequency:** Every 90 days

**Process:**
1. Generate new master key
2. Decrypt all passwords with old key
3. Re-encrypt with new key
4. Update all Instance records
5. Store new key in Secrets Manager
6. Retire old key

**Implementation:**
```java
@Service
public class KeyRotationService {

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private AES256Encryptor encryptor;

    public void rotateEncryptionKey(String oldMasterKey, String newMasterKey) {
        log.info("Starting encryption key rotation");

        List<Instance> allInstances = instanceRepository.findAll();

        for (Instance instance : allInstances) {
            try {
                // Decrypt with old key
                String password = encryptor.decrypt(
                    instance.getDatabasePassword(),
                    oldMasterKey
                );

                // Re-encrypt with new key
                String reencrypted = encryptor.encrypt(password, newMasterKey);

                // Update instance
                instance.setDatabasePassword(reencrypted);
                instanceRepository.save(instance);

            } catch (Exception e) {
                log.error("Failed to rotate key for instance: {}", instance.getId(), e);
                // Continue with other instances (don't fail entire rotation)
            }
        }

        log.info("Encryption key rotation completed for {} instances", allInstances.size());
    }
}
```

---

## Connection Pooling

### Challenge

Managing 100+ database connections efficiently:
- Naive approach: Create new connection per request → exhausts PostgreSQL max_connections (default: 100)
- Solution: Connection pool per instance with HikariCP

### HikariCP Configuration

**Per-Instance Pool:**
```java
@Configuration
public class DynamicDataSourceConfig {

    private final Map<UUID, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private AES256Encryptor encryptor;

    /**
     * Get or create HikariCP DataSource for an instance
     *
     * @param instanceId UUID of the instance
     * @return HikariDataSource configured for the instance
     */
    public DataSource getDataSource(UUID instanceId) {
        return dataSources.computeIfAbsent(instanceId, this::createDataSource);
    }

    private HikariDataSource createDataSource(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new InstanceNotFoundException(instanceId));

        String decryptedPassword = encryptor.decrypt(instance.getDatabasePassword());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(instance.getDatabaseUrl());
        config.setUsername(instance.getDatabaseUsername());
        config.setPassword(decryptedPassword);

        // Pool configuration
        config.setMaximumPoolSize(10);           // Max 10 connections per instance
        config.setMinimumIdle(2);                // Keep 2 idle connections ready
        config.setConnectionTimeout(30000);      // 30 seconds
        config.setIdleTimeout(600000);           // 10 minutes
        config.setMaxLifetime(1800000);          // 30 minutes
        config.setLeakDetectionThreshold(60000); // Warn if connection held > 60s

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Health check
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    /**
     * Close and remove DataSource for an instance (e.g., when instance is deleted)
     */
    public void removeDataSource(UUID instanceId) {
        HikariDataSource dataSource = dataSources.remove(instanceId);
        if (dataSource != null) {
            dataSource.close();
            log.info("Closed DataSource for instance: {}", instanceId);
        }
    }

    /**
     * Close all DataSources (e.g., during application shutdown)
     */
    @PreDestroy
    public void closeAll() {
        dataSources.values().forEach(HikariDataSource::close);
        log.info("Closed all DataSources");
    }
}
```

### Connection Pool Limits

**Scenario:** 100 active instances

- Per instance: 10 max connections
- Total: 100 × 10 = 1000 concurrent connections

**PostgreSQL Configuration:**
```sql
-- Increase max_connections to handle 100+ instances
ALTER SYSTEM SET max_connections = 2000;

-- Apply changes
SELECT pg_reload_conf();
```

**Why 2000 (not 1000):**
- Headroom for spikes (some instances may briefly exceed 10 connections)
- Administrative connections (pgAdmin, monitoring tools)
- Maintenance operations (backups, migrations)

### Connection Leak Detection

**HikariCP Leak Detection:**
```properties
spring.datasource.hikari.leak-detection-threshold=60000
```

**Effect:** Logs warning if connection held > 60 seconds (indicates leak in application code)

**Example Warning:**
```
WARN HikariPool - Connection leak detection triggered for instance a1b2c3d4, stack trace follows
```

**Response:** Review code to ensure connections are closed properly (use try-with-resources)

---

## Database Creation

### Option A: CloudSQL/RDS Auto-Provisioning (Production Recommended)

**Use Case:** Scalable production environment

**Implementation:**
```java
@Service
public class CloudSQLProvisioningService implements DatabaseProvisioningService {

    @Autowired
    private SQLAdmin sqlAdminClient; // Google Cloud SQL Admin API

    @Override
    public DatabaseCredentials provisionDatabase(UUID instanceId) {
        String dbName = generateDatabaseName(instanceId);

        // Create CloudSQL instance
        DatabaseInstance instance = new DatabaseInstance()
            .setName(dbName)
            .setDatabaseVersion("POSTGRES_15")
            .setRegion("us-central1")
            .setSettings(new Settings()
                .setTier("db-f1-micro")       // 0.6GB RAM, 1 vCPU
                .setBackupConfiguration(new BackupConfiguration()
                    .setEnabled(true)
                    .setStartTime("03:00")     // Daily at 3 AM
                    .setPointInTimeRecoveryEnabled(true)
                )
            );

        Operation operation = sqlAdminClient.instances()
            .insert("kiteclass-project", instance)
            .execute();

        // Wait for creation to complete
        waitForOperation(operation);

        // Get connection details
        String ipAddress = sqlAdminClient.instances()
            .get("kiteclass-project", dbName)
            .execute()
            .getIpAddresses()
            .get(0)
            .getIpAddress();

        String dbUrl = String.format(
            "jdbc:postgresql://%s:5432/%s",
            ipAddress, dbName
        );

        // Generate credentials
        String username = dbName + "_user";
        String password = generateSecurePassword(32);

        // Create user in CloudSQL
        createUserInCloudSQL(dbName, username, password);

        return new DatabaseCredentials(dbUrl, username, password);
    }
}
```

**Cost:** ~$15/month per instance (db-f1-micro)

**Pros:**
- Fully managed (automated backups, patches, monitoring)
- High availability (multi-AZ failover)
- Scalable (upgrade tier without downtime)

**Cons:**
- Higher cost at scale (100 instances = $1500/month)
- Slower provisioning (2-5 minutes vs instant)

---

### Option B: Shared PostgreSQL Server (MVP Recommended)

**Use Case:** Cost-effective MVP, early-stage startup

**Implementation:**
```java
@Service
public class SharedPostgresProvisioningService implements DatabaseProvisioningService {

    @Value("${postgres.master.url}")
    private String masterPostgresUrl;

    @Value("${postgres.master.username}")
    private String masterUsername;

    @Value("${postgres.master.password}")
    private String masterPassword;

    @Override
    public DatabaseCredentials provisionDatabase(UUID instanceId) {
        String dbName = generateDatabaseName(instanceId);
        String dbUsername = dbName + "_user";
        String dbPassword = generateSecurePassword(32);

        try (Connection conn = DriverManager.getConnection(
                masterPostgresUrl, masterUsername, masterPassword)) {

            try (Statement stmt = conn.createStatement()) {
                // Create database
                stmt.execute("CREATE DATABASE " + dbName);

                // Create user with limited permissions
                stmt.execute(String.format(
                    "CREATE USER %s WITH PASSWORD '%s'",
                    dbUsername, dbPassword
                ));

                // Grant privileges ONLY to this database
                stmt.execute(String.format(
                    "GRANT ALL PRIVILEGES ON DATABASE %s TO %s",
                    dbName, dbUsername
                ));

                // Revoke access to other databases
                stmt.execute(String.format(
                    "REVOKE CONNECT ON DATABASE postgres FROM %s",
                    dbUsername
                ));
            }

            String dbUrl = masterPostgresUrl.replace("/postgres", "/" + dbName);
            return new DatabaseCredentials(dbUrl, dbUsername, dbPassword);

        } catch (SQLException e) {
            throw new DatabaseProvisioningException("Failed to create database", e);
        }
    }
}
```

**Cost:** ~$50/month total (single db-n1-standard-2 instance for 100 tenants)

**Pros:**
- Very cost-effective
- Instant provisioning (< 1 second)
- Simple architecture

**Cons:**
- Single point of failure (mitigated with read replicas)
- Shared resources (one tenant's heavy query affects others)
- Manual scaling (upgrade instance size as tenants grow)

**Security Note:** Even though databases share a server, they are **physically isolated** at the database level. PostgreSQL enforces strict access controls.

### Recommendation

**MVP Phase (0-100 instances):** Use Option B (Shared PostgreSQL)
- Lower cost
- Faster iteration
- Easier to manage

**Growth Phase (100-1000 instances):** Migrate to Option A (CloudSQL per instance)
- Better isolation
- Better performance
- Easier to scale

**Migration Strategy:** Implement both options behind interface, switch via feature flag

---

## Schema Migration

### Flyway Integration

**Purpose:** Automatically apply database schema to new instance databases

**Implementation:**
```java
@Service
public class FlywayMigrationService {

    public void runMigrations(DatabaseCredentials credentials) {
        Flyway flyway = Flyway.configure()
            .dataSource(
                credentials.getUrl(),
                credentials.getUsername(),
                credentials.getPassword()
            )
            .locations("classpath:db/migration/core")  // Core Service migrations
            .baselineOnMigrate(true)
            .load();

        // Apply all pending migrations
        MigrateResult result = flyway.migrate();

        log.info("Applied {} migrations to database: {}",
            result.migrationsExecuted, credentials.getUrl());
    }
}
```

### Migration Files Location

**Copy from Core Service:**
```
kiteclass-core/src/main/resources/db/migration/
├── V1__create_students_table.sql
├── V2__create_teachers_table.sql
├── V3__create_courses_table.sql
├── V4__create_classes_table.sql
├── V5__create_enrollments_table.sql
├── V6__create_attendance_table.sql
├── V7__create_invoices_table.sql
├── V8__create_payments_table.sql
├── V9__create_lms_tables.sql
├── V10__create_marketing_tables.sql
└── V11__seed_default_data.sql
```

**Seed Default Data (V11):**
```sql
-- Create default admin user for instance
INSERT INTO users (id, email, password_hash, role, status, created_at)
VALUES (
    gen_random_uuid(),
    'admin@{subdomain}.kitehub.me',
    '$2a$10$...', -- bcrypt hash of temp password
    'ADMIN',
    'ACTIVE',
    NOW()
);

-- Create default system settings
INSERT INTO settings (key, value, created_at)
VALUES
    ('instance.timezone', 'Asia/Ho_Chi_Minh', NOW()),
    ('instance.currency', 'VND', NOW()),
    ('instance.language', 'vi', NOW());

-- Create default roles
INSERT INTO roles (id, name, permissions, created_at)
VALUES
    (gen_random_uuid(), 'STUDENT', '["view_courses", "submit_homework"]', NOW()),
    (gen_random_uuid(), 'TEACHER', '["manage_courses", "grade_homework"]', NOW()),
    (gen_random_uuid(), 'ADMIN', '["*"]', NOW());
```

---

## Backup Strategy

### Automated Backups

**Frequency:** Daily at 3 AM (UTC+7)

**Retention:** 7 days (rolling window)

**Storage:** AWS S3 bucket `s3://kiteclass-backups/`

**Implementation:**
```java
@Service
@Scheduled(cron = "0 0 3 * * *") // 3 AM daily
public class DatabaseBackupService {

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private S3Client s3Client;

    public void backupAllInstances() {
        List<Instance> activeInstances = instanceRepository
            .findByStatus(InstanceStatus.ACTIVE);

        for (Instance instance : activeInstances) {
            try {
                backupInstance(instance);
            } catch (Exception e) {
                log.error("Backup failed for instance: {}", instance.getId(), e);
                // Continue with other instances
            }
        }
    }

    private void backupInstance(Instance instance) {
        String dbName = extractDatabaseName(instance.getDatabaseUrl());
        String backupFileName = String.format(
            "%s/%s/%s.sql.gz",
            instance.getId(),
            LocalDate.now().toString(),
            dbName
        );

        // Run pg_dump
        ProcessBuilder pb = new ProcessBuilder(
            "pg_dump",
            "-h", extractHost(instance.getDatabaseUrl()),
            "-U", instance.getDatabaseUsername(),
            "-d", dbName,
            "-Fc",  // Custom format (compressed)
            "-f", "/tmp/" + backupFileName
        );
        pb.environment().put("PGPASSWORD", decrypt(instance.getDatabasePassword()));
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new BackupException("pg_dump failed with exit code: " + exitCode);
        }

        // Upload to S3
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket("kiteclass-backups")
                .key(backupFileName)
                .storageClass(StorageClass.GLACIER) // Cheaper storage for backups
                .build(),
            RequestBody.fromFile(new File("/tmp/" + backupFileName))
        );

        log.info("Backup completed for instance: {}", instance.getId());
    }
}
```

### Manual Backup (On-Demand)

**Use Case:** Before major migrations or risky operations

**API Endpoint:**
```java
@PostMapping("/api/v1/admin/instances/{instanceId}/backup")
public ResponseEntity<BackupResponse> triggerBackup(@PathVariable UUID instanceId) {
    backupService.backupInstance(instanceId);
    return ResponseEntity.ok(new BackupResponse("Backup started"));
}
```

### Restore Procedure

**Steps:**
1. Download backup from S3
2. Create new database (or drop and recreate existing)
3. Restore using `pg_restore`

**Script:**
```bash
#!/bin/bash
# restore-instance.sh

INSTANCE_ID=$1
BACKUP_DATE=$2  # Format: YYYY-MM-DD

# Download from S3
aws s3 cp \
  s3://kiteclass-backups/$INSTANCE_ID/$BACKUP_DATE/kiteclass_*.sql.gz \
  /tmp/backup.sql.gz

# Extract
gunzip /tmp/backup.sql.gz

# Restore
pg_restore \
  -h kitehub-postgres \
  -U postgres \
  -d kiteclass_$INSTANCE_ID \
  -c \  # Clean (drop) existing objects first
  /tmp/backup.sql

echo "Restore completed for instance: $INSTANCE_ID"
```

---

## Monitoring & Alerts

### Metrics to Track

1. **Provisioning Success Rate**
   - Metric: `database_provisioning_success_total` / `database_provisioning_attempts_total`
   - Target: > 99%
   - Alert: < 95%

2. **Average Provisioning Time**
   - Metric: `database_provisioning_duration_seconds`
   - Target: < 5 seconds (shared PostgreSQL), < 300 seconds (CloudSQL)
   - Alert: > 10 seconds (shared), > 600 seconds (CloudSQL)

3. **Active Instance Count**
   - Metric: `active_instances_total`
   - Purpose: Capacity planning

4. **Connection Pool Usage**
   - Metric: `hikari_active_connections` / `hikari_max_pool_size`
   - Target: < 80%
   - Alert: > 90%

5. **Database Disk Usage**
   - Metric: `pg_database_size_bytes{database="kiteclass_*"}`
   - Alert: > 80% of allocated storage

### Implementation (Prometheus)

**Metrics Endpoint:**
```java
@Component
public class DatabaseMetrics {

    private final Counter provisioningAttempts = Counter.builder("database_provisioning_attempts")
        .description("Total database provisioning attempts")
        .tag("status", "total")
        .register(Metrics.globalRegistry);

    private final Counter provisioningSuccess = Counter.builder("database_provisioning_success")
        .description("Successful database provisioning")
        .register(Metrics.globalRegistry);

    private final Timer provisioningDuration = Timer.builder("database_provisioning_duration")
        .description("Time taken to provision database")
        .register(Metrics.globalRegistry);

    public void recordProvisioningAttempt() {
        provisioningAttempts.increment();
    }

    public void recordProvisioningSuccess() {
        provisioningSuccess.increment();
    }

    public void recordProvisioningDuration(long durationMillis) {
        provisioningDuration.record(durationMillis, TimeUnit.MILLISECONDS);
    }
}
```

### Alerts (PagerDuty)

**Critical:**
- Database provisioning failure (> 5 failures in 10 minutes)
- Connection pool exhausted (> 95% usage)
- Backup failure (daily backup missed)

**Warning:**
- Provisioning taking longer than usual (> 20 seconds for shared PostgreSQL)
- Disk usage > 80%
- Connection leak detected

---

## Cleanup & Deprovisioning

### Soft Delete (Trial Expiration)

**When:** User's trial expires and they don't upgrade

**Action:**
1. Update Instance status: `SUSPENDED`
2. Keep database for 30 days (grace period)
3. Send email: "Your trial expired, upgrade to keep data"

**Implementation:**
```java
@Service
@Scheduled(cron = "0 0 1 * * *") // 1 AM daily
public class TrialExpirationService {

    @Autowired
    private InstanceRepository instanceRepository;

    public void checkExpiredTrials() {
        LocalDateTime now = LocalDateTime.now();
        List<Instance> expiredInstances = instanceRepository
            .findByStatusAndTrialEndsAtBefore(InstanceStatus.ACTIVE, now);

        for (Instance instance : expiredInstances) {
            instance.setStatus(InstanceStatus.SUSPENDED);
            instanceRepository.save(instance);

            // Send email notification
            emailService.sendTrialExpiredEmail(instance);

            log.info("Suspended expired trial instance: {}", instance.getId());
        }
    }
}
```

### Hard Delete (User Request or 30-Day Grace Period)

**When:**
- User explicitly requests deletion
- 30 days after trial expiration (no upgrade)

**Process:**
1. Backup database to S3 (final backup)
2. Drop database: `DROP DATABASE kiteclass_{instance_id}`
3. Drop user: `DROP USER kiteclass_{instance_id}_user`
4. Update Instance status: `DELETED`
5. Remove from connection pool

**Implementation:**
```java
@Override
@Transactional
public void deprovisionDatabase(UUID instanceId, boolean backupFirst) {
    Instance instance = instanceRepository.findById(instanceId)
        .orElseThrow(() -> new InstanceNotFoundException(instanceId));

    try {
        // 1. Final backup
        if (backupFirst) {
            backupService.backupInstance(instance);
        }

        // 2. Drop database and user
        String dbName = extractDatabaseName(instance.getDatabaseUrl());
        String dbUsername = instance.getDatabaseUsername();

        try (Connection conn = DriverManager.getConnection(
                masterPostgresUrl, masterUsername, masterPassword)) {

            try (Statement stmt = conn.createStatement()) {
                // Terminate active connections first
                stmt.execute(String.format(
                    "SELECT pg_terminate_backend(pid) " +
                    "FROM pg_stat_activity " +
                    "WHERE datname = '%s'",
                    dbName
                ));

                // Drop database
                stmt.execute("DROP DATABASE IF EXISTS " + dbName);

                // Drop user
                stmt.execute("DROP USER IF EXISTS " + dbUsername);
            }
        }

        // 3. Remove from connection pool
        dataSourceConfig.removeDataSource(instanceId);

        // 4. Mark as deleted
        instance.setStatus(InstanceStatus.DELETED);
        instanceRepository.save(instance);

        log.info("Deprovisioned database for instance: {}", instanceId);

    } catch (Exception e) {
        log.error("Deprovisioning failed for instance: {}", instanceId, e);
        throw new DatabaseProvisioningException("Failed to deprovision database", e);
    }
}
```

### Cleanup Job (Remove Old Backups)

**Frequency:** Weekly

**Policy:** Delete backups older than 7 days

```java
@Scheduled(cron = "0 0 2 * * SUN") // 2 AM every Sunday
public void cleanupOldBackups() {
    LocalDate cutoffDate = LocalDate.now().minusDays(7);

    // List and delete old backups from S3
    ListObjectsV2Request request = ListObjectsV2Request.builder()
        .bucket("kiteclass-backups")
        .build();

    ListObjectsV2Response response = s3Client.listObjectsV2(request);

    for (S3Object object : response.contents()) {
        LocalDate backupDate = extractDateFromKey(object.key());
        if (backupDate.isBefore(cutoffDate)) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket("kiteclass-backups")
                .key(object.key())
                .build());
        }
    }
}
```

---

## Cost Analysis

### Shared PostgreSQL (MVP)

**Infrastructure:**
- 1× db-n1-standard-2 (2 vCPU, 7.5GB RAM): $150/month
- S3 Glacier storage (100GB backups): $1/month
- **Total:** $151/month

**Per Instance Cost:** $1.51/month (for 100 instances)

---

### CloudSQL Per Instance (Scale)

**Infrastructure:**
- 100× db-f1-micro (0.6GB RAM): $15/month each
- S3 Glacier storage (100GB): $1/month
- **Total:** $1501/month

**Per Instance Cost:** $15/month

---

## Related Documentation

- [KiteHub Infrastructure Design](./kitehub-infrastructure.md)
- [Security Design](../../04-quality/security-design.md)
- [KiteHub Implementation Plan](./kitehub-implementation-plan.md)

---

**Last Updated:** 2026-03-09
**Status:** Design complete, ready for implementation (PR 4.2)
