# Instance Provisioning

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| INS-01 | Trial duration | 14 days | `kitehub.trial.duration-days` |
| INS-02 | Max trials per owner | 1 | `kitehub.trial.max-per-owner` |
| INS-03 | Trial warning days | 3, 1 | `kitehub.trial.warning-days` |
| INS-04 | Trial midpoint email day | 7 | `kitehub.trial.midpoint-day` |
| INS-05 | Reserved subdomains count | 27 names | RESERVED_SUBDOMAINS constant |
| INS-06 | Subdomain validation | Lowercase, checked against reserved list | validateSubdomainNotReserved() |
| INS-07 | Subdomain uniqueness | Per non-deleted instances | existsBySubdomainAndDeletedFalse() |
| INS-08 | Custom domain tier restriction | PREMIUM and ENTERPRISE only | canUseCustomDomain() |
| INS-09 | Default tier for registration | FREE | PricingTier.FREE |
| INS-10 | DB name format | kiteclass_{uuid_first_8_chars} | generateDatabaseName() |
| INS-11 | DB username format | kiteclass_{uuid_first_8_chars}_user | generateUsername() |
| INS-12 | DB password | 32 bytes SecureRandom, Base64 URL-encoded | generateSecurePassword() |
| INS-13 | DB password encryption | AES-256-GCM | `encryption.algorithm` |
| INS-14 | DB lifecycle toggle | Disabled by default (simulation mode) | `database.lifecycle.enabled` |
| INS-15 | Soft delete | Sets deleted flag, status=DELETED, closes DataSource pool | deleteInstance() |
| INS-16 | Email on activation | Welcome email with trial info | sendWelcomeEmail() |

## Reserved Subdomains (27 names)

```
admin, api, www, mail, ftp, smtp,
test, staging, dev, demo, app,
billing, support, help, docs,
status, cdn, assets, static,
ns1, ns2, mx, pop, imap,
dashboard, portal, login, register
```

## Flow

### Instance Lifecycle States
```
PENDING -> TRIAL -> ACTIVE -> SUSPENDED -> DELETED
                      ^                      |
                      |______________________|
                      (reactivation possible before retention expiry)
```

### Self-Service Registration (registerInstance)
1. Validate subdomain not reserved
2. Validate subdomain uniqueness (non-deleted)
3. Validate email uniqueness (non-deleted)
4. Generate owner UUID
5. Create instance (tier=FREE, status=TRIAL)
6. Set placeholder DB credentials ("pending")
7. Start trial (14 days from now)
8. Provision database (async, continues on failure)
9. Generate access + refresh tokens
10. Return user info + tokens + instance

### Pending Instance Flow (with email verification)
1. `createPendingInstance()` -> status=PENDING, no DB provisioned
2. Email verification sent externally
3. `activatePendingInstance()` -> start trial, provision DB, send welcome email

### Database Provisioning
1. Check if already provisioned (skip if not "pending")
2. Generate DB name: `kiteclass_{uuid_short}`
3. Generate username: `kiteclass_{uuid_short}_user`
4. Generate secure password (32 bytes, Base64 URL-safe)
5. If lifecycle enabled: CREATE USER, CREATE DATABASE, GRANT ALL, run Flyway migrations
6. If lifecycle disabled: simulate (log only)
7. Encrypt password with AES-256-GCM, save to instance

### Database Deletion
1. If lifecycle enabled: terminate connections, DROP DATABASE, DROP USER
2. If lifecycle disabled: simulate (log only)
3. Backup before deletion deferred until S3 infrastructure ready

## Emails

| Trigger | Template | Method |
|---------|----------|--------|
| Instance activated (PENDING->TRIAL) | welcome | sendWelcomeEmail() |
| Trial expiration warnings | trial-expiration-warning | sendTrialExpirationWarning() |
| Trial expired | trial-expired | sendTrialExpired() |
| Trial midpoint (day 7) | trial-midpoint | sendTrialMidpointEmail() |
| Onboarding (23-25h after start) | onboarding-tips | sendOnboardingTipsEmail() |

## Config

```yaml
kitehub:
  trial:
    duration-days: 14
    max-per-owner: 1
    warning-days: 3,1
    midpoint-day: 7

database:
  master:
    host: ${DATABASE_MASTER_HOST:localhost}
    port: ${DATABASE_MASTER_PORT:5433}
  admin:
    url: ${DATABASE_ADMIN_URL:jdbc:postgresql://localhost:5433/postgres}
    username: ${DATABASE_ADMIN_USERNAME:postgres}
    password: ${DATABASE_ADMIN_PASSWORD:}
  lifecycle:
    enabled: ${DATABASE_LIFECYCLE_ENABLED:false}

encryption:
  master-key: ${ENCRYPTION_MASTER_KEY:}
  algorithm: AES-256-GCM
```
