# Instance Provisioning — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `kitehub.trial`, `database`, `encryption`

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| INS-01 | Trial duration | 14 ngày | `kitehub.trial.duration-days` |
| INS-02 | Max trials per owner | 1 | `kitehub.trial.max-per-owner` |
| INS-03 | Trial warning days | 3, 1 | `kitehub.trial.warning-days` |
| INS-04 | Trial midpoint email | Ngày 7 | `kitehub.trial.midpoint-day` |
| INS-05 | Reserved subdomains count | 27 tên | RESERVED_SUBDOMAINS constant |
| INS-06 | Subdomain validation | Lowercase, không trong reserved list | validateSubdomainNotReserved() |
| INS-07 | Subdomain uniqueness | Per non-deleted instances | existsBySubdomainAndDeletedFalse() |
| INS-08 | Custom domain tier | PREMIUM và ENTERPRISE only | canUseCustomDomain() |
| INS-09 | Default tier on register | FREE | PricingTier.FREE |
| INS-10 | DB name format | kiteclass_{uuid_first_8_chars} | generateDatabaseName() |
| INS-11 | DB username format | kiteclass_{uuid_first_8_chars}_user | generateUsername() |
| INS-12 | DB password | 32 bytes SecureRandom, Base64 URL-encoded | generateSecurePassword() |
| INS-13 | DB password encryption | AES-256-GCM | `encryption.algorithm` |
| INS-14 | DB lifecycle toggle | Disabled by default (simulation) | `database.lifecycle.enabled` |
| INS-15 | Soft delete | deleted flag + status=DELETED + đóng DataSource | deleteInstance() |
| INS-16 | Email on activation | Welcome email | sendWelcomeEmail() |

## Reserved Subdomains (27 names)

```
admin, api, www, mail, ftp, smtp,
test, staging, dev, demo, app,
billing, support, help, docs,
status, cdn, assets, static,
ns1, ns2, mx, pop, imap,
dashboard, portal, login, register
```

## Instance Lifecycle States

```
PENDING → TRIAL → ACTIVE → SUSPENDED → DELETED
                    ↑                     |
                    |_____________________|
                    (reactivation before retention expiry)
```

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
