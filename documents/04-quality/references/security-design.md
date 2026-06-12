# KiteClass Security Design

**Version:** 1.0
**Created:** 2026-03-09
**Purpose:** Comprehensive security architecture for KiteClass platform
**Status:** Design phase

---

## Table of Contents

1. [Overview](#overview)
2. [Multi-Tenant Isolation](#multi-tenant-isolation)
3. [Encryption](#encryption)
4. [Authentication & Authorization](#authentication--authorization)
5. [Compliance](#compliance)
6. [Audit Logging](#audit-logging)
7. [Incident Response Plan](#incident-response-plan)
8. [Security Checklist](#security-checklist)

---

## Overview

**Security Model:** Defense in depth - multiple layers of security

**Key Principles:**
- **Least Privilege:** Users/services have minimum permissions needed
- **Zero Trust:** Verify every request (never trust, always verify)
- **Data Isolation:** Complete separation between tenant instances
- **Encryption Everywhere:** Data encrypted at rest and in transit
- **Audit Everything:** Log all security-relevant events

**Threat Model:**
- **External Attackers:** Internet-facing APIs (SQL injection, XSS, auth bypass)
- **Malicious Users:** Authenticated users trying to access other instances' data
- **Insider Threats:** Compromised admin accounts
- **Data Leaks:** Accidental exposure of sensitive data (logs, backups)

---

## Multi-Tenant Isolation

### Layer 1: Database-Level Isolation

**Pattern:** Database-per-tenant (strongest isolation)

**How it works:**
- Each KiteClass instance has own PostgreSQL database
- Instance A database: `kiteclass_a1b2c3d4`
- Instance B database: `kiteclass_xyz789ef`
- Physical impossibility for Instance A to query Instance B's data

**Security Guarantee:**
```sql
-- Instance A user trying to access Instance B data
SELECT * FROM kiteclass_xyz789ef.students;
-- Error: permission denied for database kiteclass_xyz789ef
```

**Benefits:**
- ✅ Complete data isolation (no shared tables)
- ✅ No risk of accidental cross-tenant queries
- ✅ Easy compliance audits (show separate databases)
- ✅ Independent backups/restores

**Implementation:**
- Database provisioning service creates isolated database (PR 4.2)
- Each database has own user with restricted permissions
- User `kiteclass_a1b2c3d4_user` can ONLY access `kiteclass_a1b2c3d4` database

---

### Layer 2: Application-Level Isolation

**Pattern:** Hibernate `@Filter` on all entities

**How it works:**
- Every entity has `instance_id` column (UUID foreign key)
- Hibernate filter automatically adds `WHERE instance_id = ?` to all queries
- Filter set from JWT token in request

**Entity Annotation:**
```java
@Entity
@Table(name = "students")
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "instanceId", type = UUID.class)
)
@Filter(name = "tenantFilter", condition = "instance_id = :instanceId")
public class Student extends BaseEntity {
    @Column(name = "instance_id", nullable = false, updatable = false)
    private UUID instanceId;

    // Other fields...
}
```

**Enable Filter (Interceptor):**
```java
@Component
public class TenantFilterInterceptor implements WebRequestInterceptor {

    @Autowired
    private EntityManager entityManager;

    @Override
    public void preHandle(WebRequest request) {
        String instanceIdHeader = request.getHeader("X-Instance-Id");
        if (instanceIdHeader != null) {
            UUID instanceId = UUID.fromString(instanceIdHeader);

            // Enable Hibernate filter
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter")
                .setParameter("instanceId", instanceId);
        }
    }
}
```

**Result:**
```java
// Developer writes:
List<Student> students = studentRepository.findAll();

// Hibernate executes:
SELECT * FROM students WHERE instance_id = 'a1b2c3d4-...' AND deleted = false;
```

**Security Guarantee:**
- Even if developer forgets to add `instance_id` filter, Hibernate adds it automatically
- Prevents accidental cross-tenant data leaks in code

---

### Layer 3: Network-Level Isolation (Production)

**Kubernetes NetworkPolicy:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: instance-isolation
  namespace: kiteclass-instances
spec:
  podSelector:
    matchLabels:
      app: kiteclass-core
      instance-id: a1b2c3d4
  policyTypes:
    - Ingress
    - Egress
  ingress:
    # Only allow traffic from Gateway
    - from:
        - podSelector:
            matchLabels:
              app: kitehub-gateway
      ports:
        - protocol: TCP
          port: 8080
  egress:
    # Only allow traffic to own database
    - to:
        - podSelector:
            matchLabels:
              app: postgres
              database: kiteclass_a1b2c3d4
      ports:
        - protocol: TCP
          port: 5432
```

**Effect:**
- Instance A pods cannot reach Instance B pods
- Instance A can only connect to Instance A database
- Network-level enforcement (even if app has bug)

---

## Encryption

### At Rest (Data Storage)

#### PostgreSQL Transparent Data Encryption (TDE)

**Setup (CloudSQL):**
```bash
# Enable encryption with customer-managed keys (CMEK)
gcloud sql instances patch kiteclass-postgres \
  --disk-encryption-key=projects/PROJECT_ID/locations/REGION/keyRings/KEYRING/cryptoKeys/KEY
```

**What's Encrypted:**
- Database files on disk
- Backups (automated + manual)
- Temporary files during queries

**Key Management:**
- Google Cloud KMS (CloudSQL)
- AWS KMS (RDS)
- Automatic key rotation every 90 days

#### S3 Server-Side Encryption

**Branding Assets (logos, banners):**
```java
PutObjectRequest request = PutObjectRequest.builder()
    .bucket("kitehub-branding-assets")
    .key("instances/a1b2c3d4/logo.png")
    .serverSideEncryption(ServerSideEncryption.AES256) // SSE-S3
    .build();

s3Client.putObject(request, RequestBody.fromFile(logoFile));
```

**Backup Archives:**
```java
PutObjectRequest request = PutObjectRequest.builder()
    .bucket("kiteclass-backups")
    .key("a1b2c3d4/2026-03-09.sql.gz")
    .serverSideEncryption(ServerSideEncryption.AWS_KMS) // SSE-KMS
    .ssekmsKeyId("arn:aws:kms:region:account:key/key-id")
    .build();
```

#### Instance Credentials Encryption

**AES-256-GCM Encryption:**
```java
@Component
public class AES256Encryptor {

    @Value("${encryption.master-key}")
    private String masterKey; // Stored in AWS Secrets Manager

    public String encrypt(String plaintext) {
        // Implementation in database-provisioning.md
        // Uses AES/GCM/NoPadding with random IV
        // Returns Base64(IV + ciphertext)
    }

    public String decrypt(String ciphertext) {
        // Extracts IV, decrypts ciphertext
        // Returns plaintext
    }
}
```

**Storage:**
```sql
-- Instance credentials in kitehub database
CREATE TABLE instances (
    id UUID PRIMARY KEY,
    database_password TEXT NOT NULL  -- Encrypted with AES-256
);
```

**Key Rotation:** Every 90 days (see `database-provisioning.md`)

---

### In Transit (Network Communication)

#### External APIs (HTTPS/TLS 1.3)

**Enforce HTTPS Only:**
```yaml
# Spring Boot application.yml
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3
    certificate: /path/to/cert.pem
    certificate-private-key: /path/to/key.pem
  port: 8443

# Force redirect HTTP → HTTPS
server:
  http:
    port: 8080
  forward-headers-strategy: native
```

**Certificate Management:**
- **Development:** Self-signed certificates (mkcert)
- **Staging/Production:** Let's Encrypt (cert-manager in Kubernetes)
- **Auto-renewal:** cert-manager renews 30 days before expiry

**TLS Configuration:**
```yaml
# Kubernetes Ingress
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/ssl-protocols: "TLSv1.3"
spec:
  tls:
    - hosts:
        - "*.kitehub.me"
      secretName: kiteclass-tls
```

#### Internal Service-to-Service (mTLS)

**Mutual TLS with Istio Service Mesh:**
```yaml
# Istio PeerAuthentication
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: kitehub
spec:
  mtls:
    mode: STRICT  # Require mTLS for all traffic
```

**How it works:**
- Each service has own certificate issued by Istio CA
- Services verify each other's certificates before communicating
- Traffic encrypted with TLS 1.3
- Automatic certificate rotation every 24 hours

**Verification:**
```bash
# Check mTLS is enabled
istioctl authn tls-check kitehub-subscription.kitehub.svc.cluster.local
```

#### Database Connections (SSL Mode)

**PostgreSQL SSL:**
```yaml
# Spring Boot application.yml
spring:
  datasource:
    url: jdbc:postgresql://kitehub-postgres:5432/kitehub?sslmode=require
    username: kitehub
    password: ${DB_PASSWORD}
```

**SSL Modes:**
- `disable`: No encryption (NEVER use in production)
- `require`: Encrypt connection (recommended minimum)
- `verify-ca`: Verify server certificate
- `verify-full`: Verify server cert + hostname (most secure)

---

### Secrets Management

#### Local Development

**Environment Variables:**
```bash
export ENCRYPTION_MASTER_KEY=base64_encoded_32_byte_key
export DB_PASSWORD=kitehub_dev_password
export OPENAI_API_KEY=sk-proj-...
```

**Docker Compose Secrets:**
```yaml
services:
  kitehub-branding:
    environment:
      OPENAI_API_KEY_FILE: /run/secrets/openai_api_key
    secrets:
      - openai_api_key

secrets:
  openai_api_key:
    file: ./secrets/openai_api_key.txt
```

#### Staging/Production

**AWS Secrets Manager:**
```java
@Configuration
public class SecretsConfig {

    @Bean
    public String encryptionMasterKey() {
        SecretsManagerClient client = SecretsManagerClient.create();
        GetSecretValueResponse response = client.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId("kiteclass/encryption-master-key")
                .build()
        );
        return response.secretString();
    }

    @Bean
    public String openaiApiKey() {
        SecretsManagerClient client = SecretsManagerClient.create();
        GetSecretValueResponse response = client.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId("kiteclass/openai-api-key")
                .build()
        );
        return response.secretString();
    }
}
```

**Kubernetes Secrets (Sealed Secrets):**
```yaml
# Generate sealed secret (encrypted)
kubeseal --format=yaml < secret.yaml > sealed-secret.yaml

# Commit sealed-secret.yaml to Git (safe - encrypted)
# Controller decrypts in-cluster and creates Secret
```

**Rotation Schedule:**
- Database passwords: On-demand (manual)
- API keys: Every 90 days
- Encryption master key: Every 90 days
- TLS certificates: Auto-renew 30 days before expiry

---

## Authentication & Authorization

### JWT Strategy

#### Access Token (Short-Lived)

**Lifetime:** 1 hour
**Purpose:** API authentication
**Stored:** Memory (frontend) or localStorage (with XSS protection)

**Claims:**
```json
{
  "sub": "user@example.com",
  "instanceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "role": "TEACHER",
  "exp": 1709971200,
  "iat": 1709967600,
  "iss": "https://api.kitehub.me"
}
```

**Signature Algorithm:** RS256 (RSA with SHA-256)

**Verification:**
```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.public-key}")
    private RSAPublicKey publicKey;

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid JWT token", e);
        }
    }

    public UUID extractInstanceId(String token) {
        Claims claims = validateToken(token);
        return UUID.fromString(claims.get("instanceId", String.class));
    }
}
```

#### Refresh Token (Long-Lived)

**Lifetime:** 30 days
**Purpose:** Obtain new access tokens without re-login
**Stored:** Redis (server-side) + httpOnly cookie (frontend)

**Storage:**
```java
@Service
public class RefreshTokenService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public String generateRefreshToken(String email, UUID instanceId) {
        String refreshToken = UUID.randomUUID().toString();
        String key = "refresh_token:" + refreshToken;

        Map<String, String> data = Map.of(
            "email", email,
            "instanceId", instanceId.toString()
        );

        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, Duration.ofDays(30));

        return refreshToken;
    }

    public Optional<Map<String, String>> validateRefreshToken(String refreshToken) {
        String key = "refresh_token:" + refreshToken;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

        if (data.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(data.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> e.getValue().toString()
            )));
    }

    public void revokeRefreshToken(String refreshToken) {
        redisTemplate.delete("refresh_token:" + refreshToken);
    }
}
```

#### Token Rotation (Sliding Window)

**On Refresh:**
1. Validate old refresh token
2. Revoke old refresh token (delete from Redis)
3. Generate new refresh token
4. Generate new access token
5. Return both to client

**Benefits:**
- Limits damage if refresh token stolen (only valid for 1 use)
- Automatic rotation extends session for active users
- Inactive users auto-logout after 30 days

---

### Service-to-Service Authentication

#### KiteHub Gateway → Platform Services (HMAC-SHA256)

**Purpose:** Prevent unauthorized services from calling platform APIs

**Request Signature:**
```java
@Component
public class HmacRequestSigner {

    @Value("${api.secret-key}")
    private String secretKey;

    public String signRequest(String method, String uri, String body, long timestamp) {
        String payload = method + uri + body + timestamp;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        mac.init(keySpec);

        byte[] signature = mac.doFinal(payload.getBytes());
        return Base64.getEncoder().encodeToString(signature);
    }

    public boolean verifySignature(String method, String uri, String body,
                                    long timestamp, String providedSignature) {
        // Prevent replay attacks (timestamp must be within 5 minutes)
        if (Math.abs(System.currentTimeMillis() / 1000 - timestamp) > 300) {
            return false;
        }

        String expectedSignature = signRequest(method, uri, body, timestamp);
        return MessageDigest.isEqual(
            expectedSignature.getBytes(),
            providedSignature.getBytes()
        );
    }
}
```

**HTTP Headers:**
```http
POST /api/v1/instances HTTP/1.1
Host: kitehub-subscription:8080
X-API-Signature: base64_encoded_hmac
X-API-Timestamp: 1709967600
Content-Type: application/json

{"subdomain": "abc123"}
```

**Verification Filter:**
```java
@Component
@Order(1)
public class HmacAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private HmacRequestSigner signer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        String signature = request.getHeader("X-API-Signature");
        String timestampStr = request.getHeader("X-API-Timestamp");

        if (signature == null || timestampStr == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        long timestamp = Long.parseLong(timestampStr);
        String body = readRequestBody(request);

        boolean valid = signer.verifySignature(
            request.getMethod(),
            request.getRequestURI(),
            body,
            timestamp,
            signature
        );

        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

#### KiteHub → KiteClass Instances (Internal JWT)

**Purpose:** Platform services calling instance-level APIs

**Short-Lived Internal Token:**
- Lifetime: 5 minutes
- Issuer: `kitehub-platform`
- Audience: `kiteclass-core`
- Claims: `instanceId`, `serviceId`

**Generation:**
```java
public String generateInternalToken(UUID instanceId, String serviceId) {
    return Jwts.builder()
        .setSubject(serviceId)
        .claim("instanceId", instanceId.toString())
        .setIssuer("kitehub-platform")
        .setAudience("kiteclass-core")
        .setExpiration(Date.from(Instant.now().plusSeconds(300))) // 5 minutes
        .signWith(privateKey, SignatureAlgorithm.RS256)
        .compact();
}
```

---

### API Key Rotation

**OpenAI API Key:**
```java
@Scheduled(cron = "0 0 0 1 */3 *") // Every 3 months (90 days)
public void rotateOpenAIKey() {
    // 1. Generate new API key in OpenAI dashboard
    // 2. Store new key in Secrets Manager
    String newKey = secretsManager.getSecret("kiteclass/openai-api-key-new");

    // 3. Update application config (zero-downtime swap)
    openAIClient.setApiKey(newKey);

    // 4. Test new key
    OpenAIResponse testResponse = openAIClient.chat("Test message");
    if (!testResponse.isSuccess()) {
        log.error("New API key failed validation, rolling back");
        openAIClient.setApiKey(oldKey);
        return;
    }

    // 5. Promote new key to primary
    secretsManager.updateSecret("kiteclass/openai-api-key", newKey);

    // 6. Revoke old key in OpenAI dashboard (manual step)
    log.info("OpenAI API key rotated successfully");
}
```

**AWS Access Keys:**
- Use IAM roles instead of access keys (when possible)
- If keys required: Rotate every 90 days
- Store in AWS Secrets Manager

---

## Compliance

### GDPR (EU Users)

#### Right to Be Forgotten

**Implementation:**
```java
@Service
public class GdprService {

    @Autowired
    private StudentRepository studentRepository;

    @Transactional
    public void deleteUserData(String email) {
        // Find all instances where user has data
        List<Instance> instances = instanceRepository.findAll();

        for (Instance instance : instances) {
            // Switch to instance database
            DataSource instanceDb = dataSourceConfig.getDataSource(instance.getId());

            // Delete or anonymize user data
            jdbcTemplate.update(
                "DELETE FROM students WHERE email = ?",
                email
            );
            jdbcTemplate.update(
                "DELETE FROM enrollments WHERE student_email = ?",
                email
            );
            jdbcTemplate.update(
                "DELETE FROM payments WHERE customer_email = ?",
                email
            );
        }

        // Delete from platform database
        instanceRepository.deleteByOwnerEmail(email);

        log.info("GDPR deletion completed for user: {}", email);
    }
}
```

**API Endpoint:**
```java
@DeleteMapping("/api/v1/gdpr/delete-my-data")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<Void> deleteMyData(Principal principal) {
    gdprService.deleteUserData(principal.getName());
    return ResponseEntity.noContent().build();
}
```

#### Data Export

**JSON Export:**
```java
@GetMapping("/api/v1/gdpr/export-my-data")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<byte[]> exportMyData(Principal principal) {
    Map<String, Object> userData = gdprService.exportUserData(principal.getName());

    String json = objectMapper.writeValueAsString(userData);
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=my-data.json")
        .contentType(MediaType.APPLICATION_JSON)
        .body(bytes);
}
```

#### Consent Management

**Track Consent:**
```sql
CREATE TABLE user_consents (
    id UUID PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    consent_type VARCHAR(100) NOT NULL,  -- PRIVACY_POLICY, TERMS_OF_SERVICE, MARKETING
    consented_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT
);
```

**Require Consent:**
```java
@PostMapping("/api/v1/auth/register")
public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
    if (!request.isPrivacyPolicyAccepted()) {
        throw new ConsentRequiredException("Privacy policy must be accepted");
    }

    // Record consent
    consentRepository.save(new UserConsent(
        request.getEmail(),
        ConsentType.PRIVACY_POLICY,
        LocalDateTime.now(),
        request.getIpAddress(),
        request.getUserAgent()
    ));

    // Create user
    return userService.register(request);
}
```

---

### Vietnamese Law (Data Residency)

**Decree 13/2023/ND-CP:** Personal data of Vietnamese citizens must be stored in Vietnam

**Compliance Strategy:**
- **Default:** AWS Singapore (ap-southeast-1) - close to Vietnam
- **If required:** Deploy dedicated CloudSQL instance in Vietnam
- **Instance metadata:** Track `data_residency_region` field

**Instance Provisioning:**
```java
public DatabaseCredentials provisionDatabase(UUID instanceId, String region) {
    if ("VN".equals(region)) {
        // Provision in Vietnam-hosted PostgreSQL
        return provisionInRegion(instanceId, "vietnam-db-server");
    } else {
        // Default: Singapore
        return provisionInRegion(instanceId, "singapore-db-server");
    }
}
```

---

### PCI-DSS (Payment Data)

**Rule:** NEVER store credit card numbers

**Compliant Strategy:**
- Use VietQR (QR code-based bank transfer) - no card data
- Payment processor (VNPay, MoMo) handles card data
- Store only: transaction ID, amount, status

**What we DON'T store:**
- ❌ Credit card numbers
- ❌ CVV codes
- ❌ Expiry dates

**What we DO store:**
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL,  -- From payment processor
    amount BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    payment_method VARCHAR(50),             -- VIETQR, BANK_TRANSFER
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Result:** Out of scope for PCI-DSS compliance (delegated to payment processor)

---

## Audit Logging

### What to Log

**Authentication Events:**
- Login success/failure
- Logout
- Password change
- Multi-factor authentication

**Authorization Failures:**
- 403 Forbidden errors
- Role elevation attempts
- Cross-tenant access attempts

**Data Modifications:**
- Student created/updated/deleted
- Course published
- Invoice paid
- Grade changed

**Administrative Actions:**
- User role changed
- Instance suspended/deleted
- System settings modified

**DON'T Log:**
- Passwords (even hashed)
- API keys
- Full credit card numbers
- Social security numbers

---

### Log Format

**Structured JSON:**
```json
{
  "timestamp": "2026-03-09T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.kiteclass.core.security.AuditLogger",
  "event": "STUDENT_CREATED",
  "actor": {
    "email": "teacher@example.com",
    "role": "TEACHER",
    "instanceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  },
  "resource": {
    "type": "Student",
    "id": "student-123",
    "action": "CREATE"
  },
  "metadata": {
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "requestId": "req-xyz789"
  },
  "success": true
}
```

### Implementation

**Audit Logger:**
```java
@Component
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    public void logEvent(AuditEvent event) {
        Map<String, Object> logEntry = Map.of(
            "timestamp", Instant.now().toString(),
            "event", event.getEventType(),
            "actor", Map.of(
                "email", event.getActorEmail(),
                "role", event.getActorRole(),
                "instanceId", event.getInstanceId()
            ),
            "resource", Map.of(
                "type", event.getResourceType(),
                "id", event.getResourceId(),
                "action", event.getAction()
            ),
            "metadata", Map.of(
                "ipAddress", event.getIpAddress(),
                "userAgent", event.getUserAgent()
            ),
            "success", event.isSuccess()
        );

        auditLog.info(objectMapper.writeValueAsString(logEntry));
    }
}
```

**Usage in Service:**
```java
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private AuditLogger auditLogger;

    @Override
    public StudentResponse createStudent(CreateStudentRequest request) {
        Student student = studentRepository.save(/* ... */);

        // Log audit event
        auditLogger.logEvent(AuditEvent.builder()
            .eventType(AuditEventType.STUDENT_CREATED)
            .actorEmail(SecurityContextHolder.getContext().getAuthentication().getName())
            .actorRole(getCurrentUserRole())
            .instanceId(getCurrentInstanceId())
            .resourceType("Student")
            .resourceId(student.getId().toString())
            .action("CREATE")
            .ipAddress(RequestContextHolder.currentRequestAttributes().getRequest().getRemoteAddr())
            .success(true)
            .build());

        return mapper.toResponse(student);
    }
}
```

---

### Log Storage

**Development:**
- Console output (JSON format)
- Rotate daily (100MB max)

**Staging/Production:**
- **CloudWatch Logs** (AWS) or **Cloud Logging** (GCP)
- Retention: 30 days (hot storage)
- **S3 Glacier:** 1 year (cold storage for compliance)

**Search & Analysis:**
```bash
# Find all failed login attempts in last hour
aws logs filter-log-events \
  --log-group-name /kiteclass/audit \
  --filter-pattern '{ $.event = "LOGIN_FAILED" }' \
  --start-time $(date -u -d '1 hour ago' +%s)000
```

---

## Incident Response Plan

### Detection

**Automated Alerts (PagerDuty):**
- 10+ failed logins from same IP in 1 minute (brute force)
- 100+ 403 Forbidden errors in 5 minutes (authorization bypass attempt)
- Database query taking > 10 seconds (potential SQL injection or DoS)
- Unusual admin actions (e.g., midnight user deletion)

**Security Scans:**
- **Trivy:** Scan Docker images for CVEs (CI/CD)
- **OWASP Dependency-Check:** Scan Maven dependencies (weekly)
- **CodeQL:** Static analysis for security issues (GitHub)

---

### Response Steps

#### P0 (Critical - Active Attack)

1. **Assess Severity** (5 minutes)
   - Is data being exfiltrated?
   - Is service down for all users?
   - Is attacker still active?

2. **Contain** (15 minutes)
   - Block attacker IP (CloudFlare WAF)
   - Revoke compromised API keys/tokens
   - Disable affected user accounts

3. **Notify** (30 minutes)
   - Security team via PagerDuty
   - CTO/CEO for P0 incidents
   - Slack #security channel

4. **Investigate** (1-2 hours)
   - Review audit logs for IOC (indicators of compromise)
   - Identify attack vector (SQL injection, XSS, auth bypass)
   - Check for lateral movement (did attacker access other systems?)

5. **Remediate** (2-4 hours)
   - Patch vulnerability
   - Deploy fix to production (emergency deployment)
   - Verify attack no longer works

6. **Post-Mortem** (1 week)
   - Document incident timeline
   - Root cause analysis
   - Action items to prevent recurrence

---

#### P1 (High - Potential Breach)

Similar to P0 but less urgent (e.g., discovered vulnerability with no active exploitation)

---

#### P2 (Medium - Security Issue)

- CVE in dependency (no known exploit)
- Failed security scan
- Address within 7 days

---

### Communication Plan

**Internal:**
- P0: Immediate PagerDuty alert + Slack #security + Email to CTO
- P1: Slack #security + Email within 1 hour
- P2: Jira ticket + Weekly security review

**External (Users):**
- **If data breach:** Email all affected users within 72 hours (GDPR requirement)
- **If service downtime > 1 hour:** Status page update every 15 minutes
- **If vulnerability patched:** Blog post explaining what happened (after fix deployed)

---

### Forensics

**Preserve Evidence:**
```bash
# Snapshot EC2 instance (before terminating)
aws ec2 create-snapshot --volume-id vol-xyz789 --description "Incident forensics 2026-03-09"

# Export audit logs
aws logs create-export-task \
  --log-group-name /kiteclass/audit \
  --from 1709870000000 \
  --to 1709880000000 \
  --destination kiteclass-incident-forensics \
  --destination-prefix incident-2026-03-09/
```

**Analysis:**
- Review application logs around incident time
- Check database query logs for unusual queries
- Inspect network traffic (if captured)

---

## Security Checklist

### Pre-Production

- [ ] All APIs enforce HTTPS (no HTTP)
- [ ] JWT tokens signed with RS256 (asymmetric)
- [ ] Refresh tokens stored in Redis with 30-day expiry
- [ ] Database credentials encrypted with AES-256
- [ ] All secrets in AWS Secrets Manager (not env vars)
- [ ] SQL injection protection (parameterized queries only)
- [ ] XSS protection (CSP headers + input sanitization)
- [ ] CSRF protection (SameSite cookies + CSRF tokens)
- [ ] Rate limiting enabled (100 req/min per IP)
- [ ] Audit logging enabled for all security events
- [ ] Database backups enabled (daily)
- [ ] Incident response plan documented
- [ ] Security contacts defined (PagerDuty)
- [ ] Vulnerability scanning automated (Trivy + OWASP)

### Post-Production

- [ ] Penetration testing completed
- [ ] Bug bounty program launched
- [ ] SOC 2 audit initiated (if enterprise customers)
- [ ] Regular security drills (quarterly)

---

## Related Documentation

- [KiteHub Infrastructure](../03-planning/infrastructure/kitehub-infrastructure.md)
- [Database Provisioning](../03-planning/infrastructure/kitehub-database-provisioning.md)
- [Gateway Implementation](../03-planning/implementation/gateway-implementation-plan.md)

---

**Last Updated:** 2026-03-09
**Status:** Design complete, ready for review
