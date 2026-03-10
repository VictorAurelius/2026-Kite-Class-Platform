# KiteClass Compliance Documentation

**Version:** 1.0
**Created:** 2026-03-10
**Purpose:** Compliance with GDPR, data protection regulations, and security standards
**Status:** Production-ready compliance framework

---

## Table of Contents

1. [GDPR Compliance](#gdpr-compliance)
2. [Data Retention Policies](#data-retention-policies)
3. [Security Audit Checklist](#security-audit-checklist)
4. [Privacy Policy Requirements](#privacy-policy-requirements)
5. [Data Processing Agreements](#data-processing-agreements)
6. [Incident Reporting](#incident-reporting)

---

## GDPR Compliance

### Overview

**General Data Protection Regulation (GDPR)** - EU Regulation 2016/679

**Applicability:** Any KiteClass instance serving EU users must comply with GDPR

**Key Principles:**
1. **Lawfulness, Fairness, Transparency** - Clear privacy policies, user consent
2. **Purpose Limitation** - Data used only for stated purposes
3. **Data Minimization** - Collect only necessary data
4. **Accuracy** - Keep data up-to-date
5. **Storage Limitation** - Delete data when no longer needed
6. **Integrity and Confidentiality** - Secure data processing
7. **Accountability** - Demonstrate compliance

---

### GDPR Rights Implementation

#### Right to Be Informed

**Implementation:**
- Privacy policy displayed during signup
- Clear explanation of data collection and usage
- Links to privacy policy in footer of every page

**Code Implementation:**
```java
@Entity
@Table(name = "user_consents")
public class UserConsent {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private ConsentType type; // PRIVACY_POLICY, MARKETING, DATA_PROCESSING

    @Column(nullable = false)
    private LocalDateTime consentedAt;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    private LocalDateTime revokedAt;
}
```

**API Endpoint:**
```java
@PostMapping("/api/v1/gdpr/consent")
public ResponseEntity<Void> recordConsent(@RequestBody ConsentRequest request) {
    UserConsent consent = new UserConsent();
    consent.setEmail(request.getEmail());
    consent.setType(ConsentType.PRIVACY_POLICY);
    consent.setConsentedAt(LocalDateTime.now());
    consent.setIpAddress(request.getIpAddress());
    consent.setUserAgent(request.getUserAgent());

    consentRepository.save(consent);

    return ResponseEntity.ok().build();
}
```

---

#### Right of Access (Subject Access Request - SAR)

**Users can request all data held about them**

**Implementation:**
```java
@GetMapping("/api/v1/gdpr/export-my-data")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<byte[]> exportUserData(Principal principal) {
    String email = principal.getName();

    Map<String, Object> userData = new HashMap<>();

    // Personal Information
    User user = userRepository.findByEmail(email).orElseThrow();
    userData.put("personal_info", Map.of(
        "name", user.getName(),
        "email", user.getEmail(),
        "phone", user.getPhone(),
        "created_at", user.getCreatedAt()
    ));

    // Students (if parent/teacher)
    List<Student> students = studentRepository.findByUserEmail(email);
    userData.put("students", students.stream()
        .map(this::sanitizeStudentData)
        .collect(Collectors.toList()));

    // Enrollments
    List<Enrollment> enrollments = enrollmentRepository.findByUserEmail(email);
    userData.put("enrollments", enrollments);

    // Payments
    List<Payment> payments = paymentRepository.findByUserEmail(email);
    userData.put("payments", payments.stream()
        .map(p -> Map.of(
            "amount", p.getAmount(),
            "date", p.getCreatedAt(),
            "status", p.getStatus()
            // DO NOT include card numbers
        ))
        .collect(Collectors.toList()));

    // Audit logs
    List<AuditEvent> auditLogs = auditRepository.findByUserEmail(email);
    userData.put("audit_logs", auditLogs);

    // Convert to JSON
    String json = objectMapper.writeValueAsString(userData);
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setContentDispositionFormData("attachment", "my-data.json");

    return ResponseEntity.ok()
        .headers(headers)
        .body(bytes);
}
```

**Response Time:** Within 30 days (GDPR requirement)

---

#### Right to Rectification

**Users can update incorrect personal data**

**Implementation:**
```java
@PutMapping("/api/v1/users/me")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<UserResponse> updateMyProfile(
    @Valid @RequestBody UpdateProfileRequest request,
    Principal principal
) {
    User user = userRepository.findByEmail(principal.getName()).orElseThrow();

    // Allow updates to these fields
    if (request.getName() != null) {
        user.setName(request.getName());
    }
    if (request.getPhone() != null) {
        user.setPhone(request.getPhone());
    }
    if (request.getAddress() != null) {
        user.setAddress(request.getAddress());
    }

    userRepository.save(user);

    // Log audit event
    auditLogger.log(new AuditEvent(
        AuditEventType.USER_PROFILE_UPDATED,
        user.getEmail(),
        "User updated their profile"
    ));

    return ResponseEntity.ok(mapper.toResponse(user));
}
```

---

#### Right to Erasure ("Right to be Forgotten")

**Users can request complete data deletion**

**Implementation:**
```java
@DeleteMapping("/api/v1/gdpr/delete-my-data")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<Void> deleteMyData(Principal principal) {
    String email = principal.getName();

    // Step 1: Verify user identity (may require password confirmation)

    // Step 2: Delete from all instances
    gdprService.deleteUserDataAcrossInstances(email);

    return ResponseEntity.noContent().build();
}

@Service
public class GdprService {

    public void deleteUserDataAcrossInstances(String email) {
        // Find all instances where user has data
        List<Instance> instances = instanceRepository.findByOwnerEmail(email);

        for (Instance instance : instances) {
            // Connect to instance database
            DataSource instanceDb = dataSourceConfig.getDataSource(instance.getId());
            JdbcTemplate jdbcTemplate = new JdbcTemplate(instanceDb);

            // Delete user data (GDPR compliance)
            jdbcTemplate.update("DELETE FROM students WHERE email = ?", email);
            jdbcTemplate.update("DELETE FROM teachers WHERE email = ?", email);
            jdbcTemplate.update("DELETE FROM enrollments WHERE student_email = ?", email);
            jdbcTemplate.update("DELETE FROM audit_logs WHERE user_email = ?", email);

            // Anonymize payment records (keep for financial compliance)
            jdbcTemplate.update(
                "UPDATE payments SET customer_email = 'deleted-user@anonymous.com' WHERE customer_email = ?",
                email
            );
        }

        // Delete from platform database
        userRepository.deleteByEmail(email);
        consentRepository.deleteByEmail(email);

        // Log deletion (for compliance audit)
        auditLogger.log(new AuditEvent(
            AuditEventType.GDPR_DATA_DELETION,
            email,
            "User data deleted per GDPR request"
        ));

        log.info("GDPR deletion completed for user: {}", email);
    }
}
```

**Important:**
- **Financial records**: Anonymize instead of delete (tax compliance requires 7-year retention)
- **Audit logs**: Keep deletion event log (prove compliance)
- **Backups**: Document that user data will be purged from backups within 90 days

---

#### Right to Restrict Processing

**Users can request temporary suspension of data processing**

**Implementation:**
```java
@PostMapping("/api/v1/gdpr/restrict-processing")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<Void> restrictProcessing(
    @RequestBody RestrictionRequest request,
    Principal principal
) {
    User user = userRepository.findByEmail(principal.getName()).orElseThrow();

    // Mark account as restricted
    user.setProcessingRestricted(true);
    user.setRestrictionReason(request.getReason());
    user.setRestrictionStartedAt(LocalDateTime.now());

    userRepository.save(user);

    // Notify administrators
    emailService.sendEmail(
        "admin@kiteclass.com",
        "GDPR Processing Restriction Request",
        "User " + user.getEmail() + " requested processing restriction. Reason: " + request.getReason()
    );

    return ResponseEntity.ok().build();
}
```

**Effect:**
- User can still login
- Data is not modified or deleted
- No automated processing (e.g., marketing emails)
- Admin must review and resolve restriction

---

#### Right to Data Portability

**Users can export data in machine-readable format**

**Implementation:** Same as "Right of Access" but provide multiple formats:
- JSON (default)
- CSV
- XML

```java
@GetMapping("/api/v1/gdpr/export-my-data")
public ResponseEntity<byte[]> exportUserData(
    @RequestParam(defaultValue = "json") String format,
    Principal principal
) {
    Map<String, Object> userData = collectUserData(principal.getName());

    byte[] bytes;
    MediaType mediaType;
    String filename;

    switch (format.toLowerCase()) {
        case "csv":
            bytes = convertToCsv(userData);
            mediaType = MediaType.parseMediaType("text/csv");
            filename = "my-data.csv";
            break;
        case "xml":
            bytes = convertToXml(userData);
            mediaType = MediaType.APPLICATION_XML;
            filename = "my-data.xml";
            break;
        default:
            bytes = convertToJson(userData);
            mediaType = MediaType.APPLICATION_JSON;
            filename = "my-data.json";
    }

    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .body(bytes);
}
```

---

#### Right to Object

**Users can object to data processing for marketing purposes**

**Implementation:**
```java
@PostMapping("/api/v1/gdpr/object-to-marketing")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<Void> objectToMarketing(Principal principal) {
    User user = userRepository.findByEmail(principal.getName()).orElseThrow();

    // Update marketing consent
    user.setMarketingConsent(false);
    user.setMarketingConsentRevokedAt(LocalDateTime.now());

    userRepository.save(user);

    // Remove from marketing lists
    marketingService.removeFromLists(user.getEmail());

    return ResponseEntity.ok().build();
}
```

---

### Data Protection Officer (DPO)

**GDPR Requirement:** Organizations processing large amounts of EU data must appoint a DPO

**DPO Contact:**
- Name: [To be assigned]
- Email: dpo@kiteclass.com
- Responsibilities:
  - Monitor GDPR compliance
  - Handle data breach notifications
  - Serve as contact for supervisory authorities
  - Conduct Data Protection Impact Assessments (DPIAs)

---

## Data Retention Policies

### Retention Periods by Data Type

| Data Type | Retention Period | Reason | Deletion Method |
|-----------|-----------------|--------|-----------------|
| **User Account Data** | Active account + 30 days after deletion | Provide grace period for account recovery | Soft delete → Hard delete after 30 days |
| **Student Records** | 5 years after graduation/withdrawal | Educational compliance (transcript requests) | Archive to cold storage → Delete after 5 years |
| **Financial Records** | 7 years | Tax compliance (IRS/Vietnamese law) | Anonymize user data, keep transaction records |
| **Audit Logs** | 1 year | Security investigations | Archive to S3 Glacier → Delete after 1 year |
| **Application Logs** | 30 days | Troubleshooting | CloudWatch Logs auto-retention |
| **Database Backups** | 30 days | Disaster recovery | Automated deletion by backup tool |
| **Email Communications** | 1 year | Legal disputes | Archive → Delete after 1 year |
| **Consent Records** | Indefinitely | Prove GDPR compliance | Never delete (legal requirement) |

---

### Automated Cleanup Jobs

**Daily Cleanup Job:**
```java
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
public void cleanupExpiredData() {
    LocalDateTime now = LocalDateTime.now();

    // Delete soft-deleted users after 30 days
    LocalDateTime deletionCutoff = now.minusDays(30);
    int deletedUsers = userRepository.deleteByDeletedTrueAndDeletedAtBefore(deletionCutoff);
    log.info("Deleted {} expired user accounts", deletedUsers);

    // Archive old audit logs (> 30 days) to S3
    LocalDateTime archiveCutoff = now.minusDays(30);
    List<AuditEvent> oldLogs = auditRepository.findByCreatedAtBefore(archiveCutoff);
    s3Service.archiveAuditLogs(oldLogs);
    auditRepository.deleteAll(oldLogs);
    log.info("Archived {} old audit logs to S3", oldLogs.size());

    // Delete old OTP tokens (> 1 hour)
    LocalDateTime otpCutoff = now.minusHours(1);
    int deletedOtps = otpTokenRepository.deleteByExpiresAtBefore(otpCutoff);
    log.info("Deleted {} expired OTP tokens", deletedOtps);

    // Delete old password reset tokens (> 24 hours)
    LocalDateTime resetCutoff = now.minusHours(24);
    int deletedResets = passwordResetRepository.deleteByExpiresAtBefore(resetCutoff);
    log.info("Deleted {} expired password reset tokens", deletedResets);
}
```

**Monthly Cleanup Job:**
```java
@Scheduled(cron = "0 0 3 1 * *") // 3 AM on 1st of each month
public void monthlyDataRetentionCleanup() {
    LocalDateTime now = LocalDateTime.now();

    // Delete graduated students after 5 years
    LocalDateTime studentCutoff = now.minusYears(5);
    int archivedStudents = studentRepository.archiveGraduatedStudentsBefore(studentCutoff);
    log.info("Archived {} graduated students (> 5 years)", archivedStudents);

    // Anonymize old payment records (> 7 years, keep for tax)
    LocalDateTime paymentCutoff = now.minusYears(7);
    int anonymizedPayments = paymentRepository.anonymizeOlderThan(paymentCutoff);
    log.info("Anonymized {} old payment records", anonymizedPayments);
}
```

---

## Security Audit Checklist

### Pre-Production Security Audit

**Infrastructure Security:**
- [ ] All services use HTTPS/TLS 1.3
- [ ] Database connections use SSL mode
- [ ] Redis requires authentication
- [ ] S3 buckets are private (not public)
- [ ] Kubernetes NetworkPolicy configured
- [ ] Secrets stored in AWS Secrets Manager (not env vars)
- [ ] IAM roles follow least privilege principle
- [ ] CloudFront WAF enabled (DDoS protection)

**Application Security:**
- [ ] JWT tokens use RS256 signature (not HS256)
- [ ] Passwords hashed with BCrypt (cost factor ≥ 10)
- [ ] Input validation on all endpoints (Jakarta Bean Validation)
- [ ] SQL injection prevention (parameterized queries only)
- [ ] XSS prevention (Content-Security-Policy headers)
- [ ] CSRF protection (SameSite cookies)
- [ ] Rate limiting enabled (100 req/min per IP)
- [ ] Multi-tenant isolation verified (Hibernate filters)

**Data Protection:**
- [ ] Personal data encrypted at rest (TDE enabled)
- [ ] Database credentials rotated every 90 days
- [ ] API keys rotated every 90 days
- [ ] Audit logging enabled for all sensitive operations
- [ ] Automated backups enabled (daily)
- [ ] Backup restoration tested in last 30 days

**Compliance:**
- [ ] Privacy policy published and up-to-date
- [ ] GDPR rights API endpoints implemented
- [ ] Consent tracking in place
- [ ] Data retention policies configured
- [ ] DPO contact information published

---

### Quarterly Security Review

**Conduct every 3 months:**

1. **Vulnerability Scan**
   ```bash
   # Scan Docker images
   trivy image kiteclass/gateway:latest
   trivy image kiteclass/core:latest

   # Scan dependencies
   cd kiteclass-core
   ./mvnw dependency-check:check
   ```

2. **Access Review**
   ```bash
   # Review who has production access
   kubectl get rolebindings -n kiteclass -o yaml

   # Revoke access for former employees
   kubectl delete rolebinding <user>-admin -n kiteclass
   ```

3. **Log Review**
   - Check for unusual activity patterns
   - Review failed login attempts
   - Check for privilege escalation attempts

4. **Certificate Expiration**
   ```bash
   # Check SSL certificate expiration
   echo | openssl s_client -connect api.kiteclass.com:443 2>/dev/null \
     | openssl x509 -noout -dates
   ```

5. **Penetration Testing**
   - Hire external security firm
   - Test for OWASP Top 10 vulnerabilities
   - Fix critical/high findings within 30 days

---

## Privacy Policy Requirements

### Minimum Required Sections

**1. What Data We Collect**
```
- Personal Information: Name, email, phone number, address
- Educational Data: Student grades, attendance, assignments
- Payment Information: Transaction IDs (NOT credit card numbers)
- Usage Data: IP address, browser type, pages visited
- Cookies: Session cookies, analytics cookies
```

**2. How We Use Your Data**
```
- Provide educational services
- Process payments
- Send transactional emails (receipts, notifications)
- Improve our services (analytics)
- Comply with legal obligations
```

**3. Legal Basis for Processing (GDPR)**
```
- Contract Performance: Providing educational services
- Consent: Marketing emails, analytics cookies
- Legitimate Interest: Fraud prevention, security
- Legal Obligation: Financial record retention
```

**4. Data Sharing**
```
- Third-Party Services: OpenAI (AI grading), VNPay (payments), AWS (hosting)
- We do NOT sell your data to advertisers
```

**5. Your Rights**
```
- Right to Access: Export your data
- Right to Rectification: Update your profile
- Right to Erasure: Delete your account
- Right to Restrict Processing: Temporarily suspend processing
- Right to Data Portability: Export in JSON/CSV/XML
- Right to Object: Opt-out of marketing
```

**6. Data Security**
```
- Encryption at rest and in transit
- Regular security audits
- Access controls and authentication
- Incident response procedures
```

**7. Contact Information**
```
- Data Protection Officer: dpo@kiteclass.com
- Privacy Questions: privacy@kiteclass.com
```

---

## Data Processing Agreements (DPA)

### DPA with Third-Party Processors

**Required for GDPR compliance when using sub-processors**

**Sub-Processors:**
1. **AWS** (Infrastructure hosting)
2. **OpenAI** (AI grading/branding)
3. **VNPay** (Payment processing)
4. **Twilio** (SMS notifications)

**DPA Template:**

```
DATA PROCESSING AGREEMENT

Between:
  KiteClass (Data Controller)
  [Third Party] (Data Processor)

1. Subject Matter: [e.g., "Cloud infrastructure hosting"]

2. Data Subjects: Students, teachers, parents, administrators

3. Personal Data Categories:
   - Identification data (name, email)
   - Educational data (grades, attendance)
   - [Other categories]

4. Processing Operations:
   - Storage
   - Retrieval
   - Analysis
   - [Other operations]

5. Security Measures:
   - Encryption (AES-256)
   - Access controls
   - Regular audits
   - Incident notification within 24 hours

6. Sub-Processor Authorization:
   - Processor may only use approved sub-processors
   - Written notice required before adding new sub-processors

7. Data Subject Rights:
   - Processor assists Controller in fulfilling data subject requests

8. Data Return/Deletion:
   - Upon termination, Processor deletes or returns all personal data

9. Liability:
   - Processor liable for damages caused by non-compliance
```

---

## Incident Reporting

### Data Breach Notification Procedure

**GDPR Requirement:** Notify supervisory authority within 72 hours of discovering a breach

**Step 1: Detect Breach (0-4 hours)**
```
- Unauthorized access detected
- Data exfiltration suspected
- Ransomware attack
```

**Step 2: Contain Breach (0-12 hours)**
```bash
# Revoke all access tokens
kubectl exec redis-0 -n kiteclass -- redis-cli FLUSHDB

# Block attacker IP
kubectl apply -f security/block-ip.yaml

# Preserve evidence
kubectl logs deployment/kiteclass-gateway > breach-logs-$(date +%Y%m%d).txt
```

**Step 3: Assess Impact (0-24 hours)**
```
- How many users affected?
- What data was accessed?
- Was data exfiltrated or just accessed?
- Were financial records compromised?
```

**Step 4: Notify Supervisory Authority (Within 72 hours)**

**Email Template:**
```
To: [National Data Protection Authority]
Subject: Data Breach Notification - KiteClass Platform

Dear Sir/Madam,

We are writing to notify you of a personal data breach discovered on [DATE] at [TIME].

Nature of the breach:
- Unauthorized access to student database via SQL injection vulnerability

Data affected:
- Approximately [NUMBER] student records
- Data categories: Names, email addresses, enrollment status
- No financial data compromised

Measures taken:
- Vulnerability patched within 2 hours of discovery
- All affected users notified
- Enhanced monitoring implemented

Contact: dpo@kiteclass.com

Sincerely,
[Data Protection Officer Name]
```

**Step 5: Notify Affected Users (Within 72 hours if high risk)**

**Email Template:**
```
Subject: Important Security Notice - KiteClass

Dear [User],

We are writing to inform you of a security incident that may have affected your account.

What happened:
On [DATE], we discovered unauthorized access to our database. Your email address and name may have been accessed.

What we're doing:
- Fixed the vulnerability
- Enhanced security monitoring
- Reviewing all security procedures

What you should do:
- Change your password immediately
- Enable two-factor authentication
- Watch for suspicious emails

We apologize for this incident and are committed to protecting your data.

Questions? Contact: security@kiteclass.com

Sincerely,
KiteClass Security Team
```

---

## Compliance Monitoring

### Automated Compliance Checks

**Weekly Compliance Report:**
```java
@Scheduled(cron = "0 0 9 * * MON") // Every Monday 9 AM
public void generateComplianceReport() {
    ComplianceReport report = new ComplianceReport();

    // Check consent records
    long usersWithoutConsent = userRepository.countByConsentNull();
    report.setUsersWithoutConsent(usersWithoutConsent);

    // Check data retention compliance
    long overRetentionUsers = userRepository.countDeletedOlderThan30Days();
    report.setUsersOverRetention(overRetentionUsers);

    // Check encryption status
    boolean dbEncryptionEnabled = databaseService.isEncryptionEnabled();
    report.setDatabaseEncryption(dbEncryptionEnabled);

    // Check certificate expiration
    int daysUntilCertExpiry = sslService.getDaysUntilExpiration();
    report.setCertificateDaysLeft(daysUntilCertExpiry);

    // Send report to DPO
    emailService.sendComplianceReport(report, "dpo@kiteclass.com");

    // Alert if non-compliant
    if (!report.isCompliant()) {
        slackService.send("#compliance", "⚠️ Compliance issues detected. Review report.");
    }
}
```

---

## Summary

**Compliance Checklist:**
- ✅ GDPR rights implemented (access, erasure, portability, etc.)
- ✅ Consent tracking and management
- ✅ Data retention policies with automated cleanup
- ✅ Security audit procedures (quarterly reviews)
- ✅ Privacy policy requirements documented
- ✅ Data Processing Agreements with sub-processors
- ✅ Breach notification procedures (72-hour timeline)
- ✅ DPO appointed with public contact info

**Regulatory Compliance:**
- ✅ **GDPR** (EU) - Full implementation
- ✅ **Vietnamese Data Protection Law** (Decree 13/2023/ND-CP)
- ✅ **PCI-DSS** (Payment Card Industry) - Delegated to payment processor
- ✅ **Tax Compliance** - 7-year financial record retention

**Audit Trail:**
- All GDPR requests logged (SAR, erasure, objection)
- Consent changes tracked with timestamps
- Data breaches documented with response actions
- Security reviews conducted quarterly

---

## Related Documentation

- [Security Design](../05-qa-and-best-practices/security-design.md)
- [Data Retention Policy](./data-retention-policy.md)
- [Privacy Policy](./privacy-policy.md)

---

**Last Updated:** 2026-03-10
**Status:** Production-ready compliance framework
