# KiteHub Database Provisioning Service

**Created:** 2026-03-10
**Version:** 1.0
**Purpose:** Document automated database provisioning for KiteClass instances including naming strategy, credentials management, connection pooling, and operational procedures.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Provisioning Workflow](#2-provisioning-workflow)
3. [Database Naming Strategy](#3-database-naming-strategy)
4. [Credentials Management](#4-credentials-management)
5. [Connection Pooling](#5-connection-pooling)
6. [Database Creation](#6-database-creation)
7. [Flyway Migrations](#7-flyway-migrations)
8. [Backup & Restore](#8-backup--restore)
9. [Monitoring & Health Checks](#9-monitoring--health-checks)
10. [Cleanup & Deprovisioning](#10-cleanup--deprovisioning)

---

## 1. Overview

### 1.1. Architecture Pattern

**Strategy:** **Database-per-Tenant** (Complete Isolation)

**Rationale:**
- **Security:** One customer's data breach doesn't affect others (physical separation)
- **Compliance:** Easier to meet data residency requirements (can place database in specific region/country)
- **Performance:** No query overhead from multi-tenant WHERE clauses
- **Scaling:** Can move one instance to different database server independently
- **Backup/Restore:** Per-customer granularity

**Trade-offs:**
- **Cost:** Higher than shared database (~$15-30/month per RDS instance vs $0.50/month shared)
- **Management:** More databases to monitor, backup, and maintain
- **Connection Pooling:** Need to manage N connection pools (addressed in Section 5)

---

### 1.2. Current Implementation Status (MVP)

**File:** `kitehub-subscription/service/DatabaseProvisioningService.java`

**Implemented (✅):**
- Database name generation (`kiteclass_{uuid_short}`)
- Username generation (`kiteclass_{uuid_short}_user`)
- Secure password generation (32 characters, Base64 URL-encoded)
- Database URL building (`jdbc:postgresql://host:port/dbname`)
- Instance entity persistence (database credentials stored)

**TODO (❌ - Production Requirements):**
- Physical database creation (requires PostgreSQL admin connection)
- Flyway migrations execution (schema creation, seed data)
- Password encryption (AES-256-GCM with master key)
- Database health checks (connectivity verification)
- Backup automation (pg_dump to S3)
- Database cleanup/deletion (drop database + revoke permissions)

**Current Behavior (Development/MVP):**
- Database provisioning is **simulated** (logs indicate success but no physical DB created)
- Passwords stored **plain text** (INSECURE - development only)
- No migrations run (instance database would be empty if it existed)

**Production Readiness:**
- Framework exists and is testable
- All TODO items must be implemented before production deployment
- Requires PostgreSQL superuser credentials or RDS admin access

---

## 2. Provisioning Workflow

### 2.1. End-to-End Instance Creation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                  INSTANCE PROVISIONING WORKFLOW                  │
└─────────────────────────────────────────────────────────────────┘

Step 1: User Signup (Trial)
─────────────────────────────
Frontend → POST /api/platform/subscriptions/create-trial
Request:
{
  "subdomain": "customer1",
  "organizationName": "Customer 1 School",
  "ownerId": "uuid",
  "tier": "FREE"
}

↓

Step 2: Instance Record Creation
─────────────────────────────────
Service: InstanceService.createTrialInstance()

Actions:
- Validate subdomain uniqueness
- Create Instance entity
- Set temporary credentials: "pending"
- Set status: TRIAL
- Set trial expiry: now() + 14 days
- Save to database (generates instance.id)

↓

Step 3: Database Provisioning
──────────────────────────────
Service: DatabaseProvisioningService.provisionDatabase(instanceId)

Actions:
- Generate database name: kiteclass_a1b2c3d4
- Generate username: kiteclass_a1b2c3d4_user
- Generate secure password: (32-char random)
- Build database URL: jdbc:postgresql://localhost:5433/kiteclass_a1b2c3d4

TODO (not yet implemented):
- Create PostgreSQL database (requires admin connection)
- Create database user with restricted permissions
- Grant privileges to user

↓

Step 4: Flyway Migrations (TODO)
─────────────────────────────────
Service: FlywayMigrationService.runMigrations(databaseUrl, username, password)

Actions:
- Connect to newly created database
- Run all V*.sql migrations from kiteclass-core
- Create schema (students, teachers, courses, classes, etc.)
- Seed default data (admin user, system settings)

Migration files from: kiteclass/kiteclass-core/src/main/resources/db/migration/

↓

Step 5: Update Instance Credentials
────────────────────────────────────
Service: DatabaseProvisioningService

Actions:
- Encrypt password (TODO: AES-256-GCM)
- Update Instance entity:
  - databaseUrl = "jdbc:postgresql://..."
  - databaseUsername = "kiteclass_a1b2c3d4_user"
  - databasePassword = "{encrypted}"
- Save instance

↓

Step 6: Kubernetes Deployment (TODO - Future)
──────────────────────────────────────────────
Service: KubernetesService.deployInstance(instanceId, dbCredentials)

Actions:
- Create namespace: kiteclass-{instanceId}
- Create Secret with database credentials
- Deploy KiteClass services (gateway, core, frontend)
- Create Ingress for subdomain routing
- Wait for health checks to pass

↓

Step 7: Instance Activation
────────────────────────────
- Update instance.status = TRIAL (already set in Step 2)
- Return instance details to frontend
- User can now access: https://customer1.kitehub.me

Response:
{
  "id": "a1b2c3d4-...",
  "subdomain": "customer1",
  "status": "TRIAL",
  "trialExpiresAt": "2026-03-24T10:00:00Z",
  "trialDaysLeft": 14,
  "isActive": true
}
```

---

### 2.2. Error Handling & Rollback

**Scenario 1: Database Creation Fails**
```java
try {
    databaseProvisioningService.provisionDatabase(saved.getId());
} catch (Exception e) {
    log.error("Failed to provision database for instance: {}", saved.getId(), e);
    // Continue - database credentials will remain "pending"
    // Admin must manually provision or retry
}
```

**Current Behavior:**
- Instance is created with status=TRIAL
- Database credentials remain "pending"
- Instance is unusable but tracked in system
- Admin can retry provisioning manually

**Production Behavior (TODO):**
- Rollback instance creation if database provisioning fails
- Or mark instance as PROVISIONING_FAILED status
- Implement retry mechanism with exponential backoff
- Alert admin via PagerDuty/Slack

---

## 3. Database Naming Strategy

### 3.1. Database Name Format

**Pattern:** `kiteclass_{uuid_short}`

**Implementation:**
```java
private String generateDatabaseName(UUID instanceId) {
    // Use first 8 characters of UUID (without hyphens)
    String uuidShort = instanceId.toString().replace("-", "").substring(0, 8);
    return "kiteclass_" + uuidShort;
}
```

**Examples:**
| Instance ID | UUID Short | Database Name |
|-------------|------------|---------------|
| `a1b2c3d4-e5f6-7890-abcd-ef1234567890` | `a1b2c3d4` | `kiteclass_a1b2c3d4` |
| `f9e8d7c6-b5a4-3210-9876-543210fedcba` | `f9e8d7c6` | `kiteclass_f9e8d7c6` |

**Benefits:**
- **Unique:** UUID guarantees global uniqueness
- **Short:** 8 characters keeps connection strings manageable
- **Sortable:** Alphabetically sortable by creation order (roughly)
- **Readable:** Easy to identify in database listings
- **Collision-free:** First 8 chars of UUID have negligible collision probability

**PostgreSQL Naming Constraints:**
- Max length: 63 characters (PostgreSQL identifier limit)
- `kiteclass_` prefix = 10 chars
- UUID short = 8 chars
- **Total = 18 chars** ✅ (well under limit)

---

### 3.2. Username Format

**Pattern:** `kiteclass_{uuid_short}_user`

**Implementation:**
```java
private String generateUsername(UUID instanceId) {
    String uuidShort = instanceId.toString().replace("-", "").substring(0, 8);
    return "kiteclass_" + uuidShort + "_user";
}
```

**Example:** `kiteclass_a1b2c3d4_user`

**Why Separate User?**
- **Principle of Least Privilege:** Instance database user doesn't need superuser permissions
- **Security:** Compromised instance can't affect other databases
- **Permissions Isolation:** `GRANT ALL PRIVILEGES ON DATABASE kiteclass_a1b2c3d4 TO kiteclass_a1b2c3d4_user`

---

## 4. Credentials Management

### 4.1. Password Generation

**Requirements:**
- Cryptographically secure random
- Sufficient entropy (prevent brute force)
- URL-safe (for potential query string usage)

**Implementation:**
```java
private static final SecureRandom SECURE_RANDOM = new SecureRandom();
private static final int PASSWORD_LENGTH = 32;

private String generateSecurePassword() {
    byte[] randomBytes = new byte[PASSWORD_LENGTH];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
}
```

**Output Example:** `Kq7Jx9ZmP3vR8sN2LtYf6Wb5DcHgT4Ua`

**Entropy Calculation:**
- 32 bytes = 256 bits of entropy
- Base64 encoding → ~43 characters
- Brute force attacks: 2^256 combinations (infeasible)

---

### 4.2. Password Encryption (TODO)

**Current Status:** ❌ **NOT IMPLEMENTED** (stores plain text)

**Production Requirement:** AES-256-GCM encryption with master key

**Planned Implementation:**

#### Option A: Application-Level Encryption (Recommended)

```java
@Service
public class EncryptionService {

    @Value("${encryption.master-key}")
    private String masterKey; // Retrieved from AWS Secrets Manager

    /**
     * Encrypt password using AES-256-GCM.
     *
     * @param plainPassword plain text password
     * @return encrypted password (Base64-encoded)
     */
    public String encryptPassword(String plainPassword) {
        try {
            // Generate random IV (Initialization Vector)
            byte[] iv = new byte[12]; // GCM recommended IV size
            SecureRandom.getInstanceStrong().nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(
                Base64.getDecoder().decode(masterKey), "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    /**
     * Decrypt password using AES-256-GCM.
     *
     * @param encryptedPassword encrypted password (Base64-encoded)
     * @return plain text password
     */
    public String decryptPassword(String encryptedPassword) {
        try {
            // Decode Base64
            byte[] combined = Base64.getDecoder().decode(encryptedPassword);

            // Extract IV and ciphertext
            byte[] iv = new byte[12];
            byte[] ciphertext = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, ciphertext, 0, ciphertext.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(
                Base64.getDecoder().decode(masterKey), "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }
}
```

**Master Key Storage:**
- **Development:** Environment variable or application.yml
- **Production:** AWS Secrets Manager or HashiCorp Vault
- **Rotation:** Master key rotated every 90 days (re-encrypt all passwords)

#### Option B: Database-Level Encryption

Use PostgreSQL `pgcrypto` extension:
```sql
-- Store encrypted
INSERT INTO instances (database_password)
VALUES (pgp_sym_encrypt('password', 'master-key'));

-- Retrieve decrypted
SELECT pgp_sym_decrypt(database_password::bytea, 'master-key')
FROM instances WHERE id = 'uuid';
```

**Recommendation:** Use Option A (application-level) for better portability and key management control.

---

### 4.3. Credentials Storage

**Instance Entity Fields:**
```java
@Column(name = "database_url", length = 500, nullable = false)
private String databaseUrl;  // Plain text (safe to expose)

@Column(name = "database_username", length = 100, nullable = false)
private String databaseUsername;  // Plain text (safe to expose)

@Column(name = "database_password", length = 255, nullable = false)
private String databasePassword;  // MUST BE ENCRYPTED (currently NOT encrypted)
```

**Database Table:**
```sql
CREATE TABLE platform.instances (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    organization_name VARCHAR(255) NOT NULL,
    database_url VARCHAR(500) NOT NULL,
    database_username VARCHAR(100) NOT NULL,
    database_password VARCHAR(255) NOT NULL,  -- ENCRYPTED in production
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Security Considerations:**
- ✅ Database URL is safe to log (contains no credentials)
- ✅ Username is safe to expose (low-privilege user)
- ❌ **CRITICAL:** Password must NEVER be logged or exposed in API responses
- ✅ Application code should decrypt password only when establishing connection

---

## 5. Connection Pooling

### 5.1. Challenge: Managing N Database Connections

**Scenario:** 100 active instances × 10 connections per pool = 1000 total connections

**Problem:**
- Cannot create 1000 connections at application startup (memory, latency)
- Need on-demand connection pool creation
- Need pool cleanup when instance deleted

---

### 5.2. Dynamic DataSource Management

**Implementation:** `MultiTenantDataSourceConfig`

**File:** `kitehub-subscription/config/MultiTenantDataSourceConfig.java`

**Pattern:** Lazy-initialized connection pools cached in ConcurrentHashMap

```java
@Configuration
public class MultiTenantDataSourceConfig {

    private final Map<UUID, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private final InstanceRepository instanceRepository;
    private final EncryptionService encryptionService; // TODO: Implement

    /**
     * Get or create DataSource for instance.
     * Thread-safe lazy initialization.
     *
     * @param instanceId instance UUID
     * @return HikariDataSource for instance
     */
    public DataSource getDataSource(UUID instanceId) {
        return dataSources.computeIfAbsent(instanceId, id -> {
            log.info("Creating DataSource for instance: {}", id);

            // Fetch instance credentials
            Instance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + id));

            // Decrypt password
            String plainPassword = encryptionService.decryptPassword(instance.getDatabasePassword());

            // Configure HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(instance.getDatabaseUrl());
            config.setUsername(instance.getDatabaseUsername());
            config.setPassword(plainPassword);
            config.setMaximumPoolSize(10);           // 10 connections per instance
            config.setMinimumIdle(2);                // Keep 2 idle connections
            config.setConnectionTimeout(30000);      // 30 seconds
            config.setIdleTimeout(600000);           // 10 minutes
            config.setMaxLifetime(1800000);          // 30 minutes
            config.setPoolName("HikariPool-" + id.toString().substring(0, 8));

            // Validation query
            config.setConnectionTestQuery("SELECT 1");

            return new HikariDataSource(config);
        });
    }

    /**
     * Close DataSource for instance (called on instance deletion).
     *
     * @param instanceId instance UUID
     */
    public void closeDataSource(UUID instanceId) {
        HikariDataSource dataSource = dataSources.remove(instanceId);
        if (dataSource != null) {
            log.info("Closing DataSource for instance: {}", instanceId);
            dataSource.close();
        }
    }

    /**
     * Get all active DataSources (for monitoring).
     *
     * @return set of instance IDs with active pools
     */
    public Set<UUID> getActiveDataSources() {
        return new HashSet<>(dataSources.keySet());
    }
}
```

**Usage in Service:**
```java
@Service
public class StudentService {

    private final MultiTenantDataSourceConfig dataSourceConfig;

    public List<Student> getStudentsForInstance(UUID instanceId) {
        DataSource ds = dataSourceConfig.getDataSource(instanceId);
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        return jdbc.query("SELECT * FROM students", new StudentRowMapper());
    }
}
```

---

### 5.3. Connection Pool Sizing

**Per-Instance Pool Configuration:**
```yaml
hikari:
  maximumPoolSize: 10      # Max connections per instance
  minimumIdle: 2           # Always keep 2 idle connections warm
  connectionTimeout: 30000 # 30 seconds to get connection from pool
  idleTimeout: 600000      # Close idle connections after 10 minutes
  maxLifetime: 1800000     # Recycle connections after 30 minutes
```

**Total Connection Budget:**

| Instances | Pool Size | Total Connections | PostgreSQL max_connections | Headroom |
|-----------|-----------|-------------------|---------------------------|----------|
| 10 | 10 | 100 | 200 | 100 |
| 50 | 10 | 500 | 1000 | 500 |
| 100 | 10 | 1000 | 2000 | 1000 |
| 200 | 10 | 2000 | 4000 | 2000 |

**PostgreSQL Configuration:**
```sql
-- Check current max_connections
SHOW max_connections;

-- Update max_connections (requires restart)
ALTER SYSTEM SET max_connections = 2000;
```

**Monitoring Metrics:**
- Active connection pools: `dataSources.size()`
- Total connections in use: Sum of `pool.getActiveConnections()`
- Idle connections: Sum of `pool.getIdleConnections()`

---

## 6. Database Creation

### 6.1. Option A: CloudSQL/RDS Auto-Provisioning (Production)

**Use Case:** Large-scale deployment (100+ instances)

**Strategy:** Create separate RDS/CloudSQL instance per tenant

**Pros:**
- **Complete isolation:** Physical database servers
- **Independent scaling:** Resize one instance without affecting others
- **Regional placement:** Place database in customer's preferred region
- **Backup granularity:** Per-instance snapshots

**Cons:**
- **Cost:** $15-30/month per RDS db.t4g.micro instance
- **Management overhead:** More databases to monitor

**Implementation (AWS RDS):**
```java
@Service
public class RDSProvisioningService {

    private final AmazonRDS rdsClient;

    public String provisionRDSInstance(UUID instanceId, String dbName) {
        CreateDBInstanceRequest request = new CreateDBInstanceRequest()
            .withDBInstanceIdentifier(dbName)
            .withDBInstanceClass("db.t4g.micro")
            .withEngine("postgres")
            .withEngineVersion("15.4")
            .withMasterUsername("postgres")
            .withMasterUserPassword(generateSecurePassword())
            .withAllocatedStorage(20) // 20 GB
            .withStorageType("gp3")
            .withBackupRetentionPeriod(7)
            .withPreferredBackupWindow("03:00-04:00")
            .withPreferredMaintenanceWindow("Mon:04:00-Mon:05:00")
            .withPubliclyAccessible(false)
            .withVpcSecurityGroupIds("sg-xxxxx")
            .withDBSubnetGroupName("kiteclass-db-subnet-group");

        CreateDBInstanceResult result = rdsClient.createDBInstance(request);

        // Wait for instance to be available (5-10 minutes)
        waitForDBInstanceAvailable(dbName);

        // Get endpoint
        DBInstance instance = describeDBInstance(dbName);
        String endpoint = instance.getEndpoint().getAddress();
        int port = instance.getEndpoint().getPort();

        return String.format("jdbc:postgresql://%s:%d/%s", endpoint, port, "postgres");
    }
}
```

**Cost Estimate (per instance):**
- RDS db.t4g.micro: $0.018/hour × 730 hours = **$13.14/month**
- Storage (20 GB gp3): $0.138/GB × 20 GB = **$2.76/month**
- **Total: ~$16/month per instance**

---

### 6.2. Option B: Shared PostgreSQL Server (MVP/Small Scale)

**Use Case:** MVP, development, or small deployments (<50 instances)

**Strategy:** All instances share one PostgreSQL server, isolated by separate databases

**Pros:**
- **Cost-effective:** $50-100/month for one db.r6g.large can host 50+ instances
- **Simpler management:** One database server to monitor
- **Faster provisioning:** Creating database takes seconds

**Cons:**
- **Noisy neighbor:** One instance's load can affect others
- **Single point of failure:** All instances down if PostgreSQL fails
- **Connection limits:** PostgreSQL max_connections shared across all instances

**Implementation:**
```java
@Service
public class SharedPostgreSQLProvisioningService {

    @Value("${database.master.host}")
    private String masterHost;

    @Value("${database.master.port}")
    private int masterPort;

    @Value("${database.master.username}")
    private String masterUsername;

    @Value("${database.master.password}")
    private String masterPassword;

    /**
     * Create database and user on shared PostgreSQL server.
     *
     * @param dbName database name (e.g., kiteclass_a1b2c3d4)
     * @param username database user (e.g., kiteclass_a1b2c3d4_user)
     * @param password secure password
     * @return database URL
     */
    public String createDatabase(String dbName, String username, String password) {
        String masterUrl = String.format("jdbc:postgresql://%s:%d/postgres",
            masterHost, masterPort);

        try (Connection conn = DriverManager.getConnection(masterUrl, masterUsername, masterPassword);
             Statement stmt = conn.createStatement()) {

            // Create database
            stmt.execute(String.format("CREATE DATABASE %s", dbName));
            log.info("Created database: {}", dbName);

            // Create user
            String createUserSql = String.format(
                "CREATE USER %s WITH PASSWORD '%s'",
                username, password.replace("'", "''"));  // Escape single quotes
            stmt.execute(createUserSql);
            log.info("Created user: {}", username);

            // Grant privileges
            stmt.execute(String.format("GRANT ALL PRIVILEGES ON DATABASE %s TO %s",
                dbName, username));
            log.info("Granted privileges to user: {}", username);

            return String.format("jdbc:postgresql://%s:%d/%s", masterHost, masterPort, dbName);

        } catch (SQLException e) {
            log.error("Failed to create database: {}", dbName, e);
            throw new RuntimeException("Database creation failed", e);
        }
    }

    /**
     * Drop database and user.
     *
     * @param dbName database name
     * @param username database user
     */
    public void dropDatabase(String dbName, String username) {
        String masterUrl = String.format("jdbc:postgresql://%s:%d/postgres",
            masterHost, masterPort);

        try (Connection conn = DriverManager.getConnection(masterUrl, masterUsername, masterPassword);
             Statement stmt = conn.createStatement()) {

            // Terminate active connections to database
            String terminateConnections = String.format(
                "SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
                "WHERE datname = '%s' AND pid <> pg_backend_pid()",
                dbName);
            stmt.execute(terminateConnections);

            // Drop database
            stmt.execute(String.format("DROP DATABASE IF EXISTS %s", dbName));
            log.info("Dropped database: {}", dbName);

            // Drop user
            stmt.execute(String.format("DROP USER IF EXISTS %s", username));
            log.info("Dropped user: {}", username);

        } catch (SQLException e) {
            log.error("Failed to drop database: {}", dbName, e);
            throw new RuntimeException("Database deletion failed", e);
        }
    }
}
```

**Master Credentials (Secrets Manager):**
```yaml
# application.yml (reference secrets)
database:
  master:
    host: ${DB_MASTER_HOST:localhost}
    port: ${DB_MASTER_PORT:5433}
    username: ${DB_MASTER_USERNAME}  # From AWS Secrets Manager
    password: ${DB_MASTER_PASSWORD}  # From AWS Secrets Manager
```

---

### 6.3. Recommendation

| Deployment Size | Recommended Option | Rationale |
|-----------------|-------------------|-----------|
| **MVP/Development** | Option B (Shared PostgreSQL) | Cost-effective, fast provisioning |
| **< 50 instances** | Option B (Shared PostgreSQL) | One db.r6g.large sufficient |
| **50-100 instances** | Hybrid (critical customers on RDS, others shared) | Balance cost vs isolation |
| **> 100 instances** | Option A (RDS per instance) | Scalability, isolation, compliance |

**Migration Path:**
1. Start with Option B (shared PostgreSQL)
2. Monitor resource usage and noisy neighbor issues
3. Migrate high-value customers to dedicated RDS instances
4. Eventually move all instances to RDS as revenue grows

---

## 7. Flyway Migrations

### 7.1. Purpose

**Goal:** Create schema and seed data in newly provisioned instance databases

**Migration Files Location:** `kiteclass/kiteclass-core/src/main/resources/db/migration/`

**Expected Migrations:**
```
V1__create_users_table.sql
V2__create_students_table.sql
V3__create_teachers_table.sql
V4__create_courses_table.sql
V5__create_classes_table.sql
V6__create_attendance_table.sql
V7__create_invoices_table.sql
V8__create_payments_table.sql
V9__seed_default_settings.sql
V10__seed_admin_user.sql
```

---

### 7.2. Implementation (TODO)

```java
@Service
public class FlywayMigrationService {

    /**
     * Run Flyway migrations on instance database.
     *
     * @param databaseUrl JDBC URL
     * @param username database username
     * @param password database password (plain text)
     */
    public void runMigrations(String databaseUrl, String username, String password) {
        log.info("Running Flyway migrations on database: {}", databaseUrl);

        Flyway flyway = Flyway.configure()
            .dataSource(databaseUrl, username, password)
            .locations("classpath:db/migration")  // Use KiteClass Core migrations
            .baselineOnMigrate(true)              // Baseline if database already exists
            .validateOnMigrate(true)
            .load();

        MigrateResult result = flyway.migrate();

        log.info("Flyway migrations completed: {} migrations applied", result.migrationsExecuted);
    }
}
```

**Integration:**
```java
public DatabaseCredentials provisionDatabase(UUID instanceId) {
    // ... (database creation code)

    // Run migrations
    flywayMigrationService.runMigrations(databaseUrl, username, password);

    // ... (save credentials)
}
```

---

### 7.3. Seed Data

**Default Admin User:**
```sql
-- V10__seed_admin_user.sql
INSERT INTO users (id, email, full_name, role, password_hash)
VALUES (
    gen_random_uuid(),
    'admin@example.com',
    'System Administrator',
    'ADMIN',
    '$2a$10$...'  -- BCrypt hash of default password
);
```

**Security Note:**
- Default password should be generated randomly per instance
- Send welcome email with temporary password (force change on first login)

---

## 8. Backup & Restore

### 8.1. Automated Backup Strategy (TODO)

**Frequency:** Daily (3 AM UTC)

**Retention Policy:**
- Daily backups: 7 days
- Weekly backups: 4 weeks
- Monthly backups: 12 months

**Storage:** AWS S3 bucket `s3://kiteclass-backups/{instance-id}/{date}.sql.gz`

---

### 8.2. Backup Implementation

```java
@Service
public class BackupService {

    @Scheduled(cron = "0 0 3 * * *")  // Every day at 3 AM
    public void backupAllInstanceDatabases() {
        List<Instance> activeInstances = instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.ACTIVE);

        log.info("Starting daily backup for {} instances", activeInstances.size());

        for (Instance instance : activeInstances) {
            try {
                backupInstanceDatabase(instance);
            } catch (Exception e) {
                log.error("Failed to backup instance: {}", instance.getId(), e);
                // Alert admin via PagerDuty
            }
        }
    }

    private void backupInstanceDatabase(Instance instance) {
        String dbName = extractDatabaseName(instance.getDatabaseUrl());
        String backupFileName = String.format("%s_%s.sql",
            instance.getId(), LocalDate.now());
        String backupPath = "/tmp/" + backupFileName;

        // Run pg_dump
        String[] command = {
            "pg_dump",
            "-h", instance.getDatabaseHost(),
            "-U", instance.getDatabaseUsername(),
            "-d", dbName,
            "-f", backupPath,
            "--no-owner",
            "--no-acl"
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", decryptPassword(instance.getDatabasePassword()));

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("pg_dump failed with exit code: " + exitCode);
        }

        // Compress
        gzipFile(backupPath);

        // Upload to S3
        s3Client.putObject(
            "kiteclass-backups",
            String.format("%s/%s.gz", instance.getId(), backupFileName),
            new File(backupPath + ".gz")
        );

        // Cleanup local file
        Files.delete(Paths.get(backupPath + ".gz"));

        log.info("Backup completed for instance: {}", instance.getId());
    }
}
```

---

### 8.3. Restore Procedure

**Manual Restore (Admin):**
```bash
# 1. Download backup from S3
aws s3 cp s3://kiteclass-backups/{instance-id}/2026-03-10.sql.gz .

# 2. Decompress
gunzip 2026-03-10.sql.gz

# 3. Drop and recreate database (if needed)
psql -h localhost -U postgres -c "DROP DATABASE kiteclass_a1b2c3d4"
psql -h localhost -U postgres -c "CREATE DATABASE kiteclass_a1b2c3d4"

# 4. Restore
psql -h localhost -U kiteclass_a1b2c3d4_user -d kiteclass_a1b2c3d4 < 2026-03-10.sql
```

**Self-Service Restore (Future Feature):**
- Allow CENTER_OWNER to request point-in-time restore
- Create new instance from backup
- Preserve original instance (avoid data loss)

---

## 9. Monitoring & Health Checks

### 9.1. Database Health Check (TODO)

```java
public boolean checkDatabaseHealth(UUID instanceId) {
    Instance instance = instanceRepository.findById(instanceId)
        .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

    if (instance.getDatabaseUrl() == null) {
        return false;
    }

    try {
        DataSource ds = dataSourceConfig.getDataSource(instanceId);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next() && rs.getInt(1) == 1;
        }
    } catch (SQLException e) {
        log.error("Database health check failed for instance: {}", instanceId, e);
        return false;
    }
}
```

**Scheduled Health Checks:**
```java
@Scheduled(fixedRate = 300000)  // Every 5 minutes
public void checkAllDatabaseHealth() {
    List<Instance> activeInstances = instanceRepository.findByStatusIn(
        List.of(InstanceStatus.ACTIVE, InstanceStatus.TRIAL));

    for (Instance instance : activeInstances) {
        boolean healthy = checkDatabaseHealth(instance.getId());
        if (!healthy) {
            log.warn("Database unhealthy for instance: {}", instance.getId());
            // Alert admin via PagerDuty
        }
    }
}
```

---

### 9.2. Monitoring Metrics

**Key Metrics:**
| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| Database Provisioning Success Rate | % of successful DB creations | < 95% |
| Average Provisioning Time | Time from request to ready | > 5 minutes |
| Active Connection Pools | Number of instances with pools | N/A (informational) |
| Total Database Connections | Sum across all instances | > 80% of max_connections |
| Backup Success Rate | % of successful daily backups | < 100% |
| Database Health Check Failures | Instances with failed health checks | > 0 |

**Metrics Endpoint:**
```java
@GetMapping("/actuator/metrics/database-provisioning")
public Map<String, Object> getProvisioningMetrics() {
    return Map.of(
        "active_connection_pools", dataSourceConfig.getActiveDataSources().size(),
        "total_instances", instanceRepository.count(),
        "provisioning_success_rate", calculateSuccessRate(),
        "avg_provisioning_time_ms", calculateAvgProvisioningTime()
    );
}
```

---

## 10. Cleanup & Deprovisioning

### 10.1. Soft Delete (Trial Expiration)

**Scenario:** Trial expires, no payment received

**Workflow:**
1. **Scheduled Job** (runs daily at 1 AM):
   ```java
   @Scheduled(cron = "0 0 1 * * *")
   public void suspendExpiredTrials() {
       List<Instance> expired = instanceRepository.findExpiredTrials(LocalDateTime.now());
       for (Instance instance : expired) {
           instance.suspend();  // status = SUSPENDED
           instanceRepository.save(instance);
       }
   }
   ```

2. **Instance Suspended:**
   - Status: `SUSPENDED`
   - Database retained (instance owner can restore by upgrading)
   - Access blocked (TenantResolverFilter returns 503)

3. **Grace Period:** 30 days before hard delete

---

### 10.2. Hard Delete (User Request or Final Cleanup)

**Workflow:**
```java
public void deleteInstance(UUID instanceId) {
    Instance instance = instanceRepository.findById(instanceId)
        .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

    // 1. Close connection pool
    dataSourceConfig.closeDataSource(instanceId);

    // 2. Backup database before deletion
    backupService.backupInstanceDatabase(instance);

    // 3. Drop database and user
    String dbName = extractDatabaseName(instance.getDatabaseUrl());
    sharedPostgreSQLService.dropDatabase(dbName, instance.getDatabaseUsername());

    // 4. Soft delete instance record (audit trail)
    instance.softDelete();
    instance.setStatus(InstanceStatus.DELETED);
    instanceRepository.save(instance);

    log.info("Deleted instance: {} (database: {})", instanceId, dbName);
}
```

**Safety Measures:**
- ✅ Always backup before deletion (uploaded to S3)
- ✅ Soft delete instance record (preserves audit trail)
- ✅ Require admin confirmation for manual deletions
- ❌ **NEVER** hard delete instance records (keep for analytics/compliance)

---

### 10.3. Cleanup Scheduled Job

```java
@Scheduled(cron = "0 0 2 * * *")  // Every day at 2 AM
public void cleanupSuspendedInstances() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

    // Find instances suspended for > 30 days
    List<Instance> toDelete = instanceRepository
        .findByStatusAndUpdatedAtBefore(InstanceStatus.SUSPENDED, cutoff);

    log.info("Found {} instances to permanently delete", toDelete.size());

    for (Instance instance : toDelete) {
        try {
            deleteInstance(instance.getId());
        } catch (Exception e) {
            log.error("Failed to delete instance: {}", instance.getId(), e);
        }
    }
}
```

---

## Summary

### Implementation Checklist

**MVP (Current):**
- ✅ Database naming strategy
- ✅ Username/password generation
- ✅ Database URL building
- ✅ Instance entity persistence
- ✅ Connection pool framework

**Production Requirements (TODO):**
- ❌ Physical database creation (PostgreSQL admin connection)
- ❌ Flyway migrations execution
- ❌ Password encryption (AES-256-GCM)
- ❌ Database health checks
- ❌ Automated backups (pg_dump + S3)
- ❌ Cleanup/deprovisioning
- ❌ Monitoring & alerting

**Estimated Effort:**
- Database creation implementation: 8 hours
- Flyway integration: 4 hours
- Encryption implementation: 6 hours
- Backup/restore: 8 hours
- **Total: ~26 hours (3-4 days)**

---

**Last Updated:** 2026-03-10
**Author:** Infrastructure Team
**Status:** Draft v1.0 (MVP framework in place, production TODOs identified)
