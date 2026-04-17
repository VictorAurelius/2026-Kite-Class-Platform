# Security Audit — SaaS Data Safety (PRs #311-317)

**Date:** 2026-04-17 | **Scope:** NEW code only (backup, purge, admin email, S3)
**Score: 76/100**

## 1. Dependency Vulnerabilities — 17/20

| Finding | Severity | Detail |
|---------|----------|--------|
| AWS SDK v2 outdated | P2 | `2.20.26` — current is 2.31.x. 10+ months behind. Known CVEs in older S3 client. |
| Duplicate JJWT deps | P3 | `jjwt-api/impl/jackson` declared twice in pom.xml (lines 109-124 and 170-187). No security impact but messy. |

**Good:** Spring Boot BOM manages transitive deps. No known P0 CVEs in current versions.

## 2. Secrets & Credentials — 16/20

| Finding | Severity | Detail |
|---------|----------|--------|
| S3 defaults in YAML | **P1** | `access-key: ${S3_ACCESS_KEY:minioadmin}`, `secret-key: ${S3_SECRET_KEY:minioadmin}` (application.yml:155-156). Default credentials ship in artifact. Dev can accidentally deploy with MinIO defaults. |
| DB admin password empty default | P2 | `password: ${DATABASE_ADMIN_PASSWORD:}` — empty default is safe (fails fast) but inconsistent with S3 pattern above. |
| PGPASSWORD in env | P3 | `pb.environment().put("PGPASSWORD", dbPassword)` — standard pg_dump pattern, acceptable. Process-scoped, not visible in `ps`. |

**Good:** All secrets use `${ENV_VAR:default}` pattern. JWT secret has no default. Encryption key has no default.

## 3. OWASP Top 10 — 15/20

| Finding | Severity | Detail |
|---------|----------|--------|
| **Command injection via databaseName** | **P1** | `DatabaseBackupService.executePgDump(String databaseName)` passes `databaseName` as `-d` argument to `ProcessBuilder`. Uses list-based args (not shell concatenation) which **mitigates shell injection**. However, `databaseName` is extracted from `instance.getDatabaseUrl()` via string split — no regex validation that it matches `^[a-zA-Z0-9_]+$`. A corrupted DB URL in the instances table could pass unexpected args to pg_dump. |
| No S3 key path traversal check | P2 | `BackupStorageService.uploadBackup(key, ...)` — key built from `instanceId + databaseName + timestamp`. No check for `../` in key. Low risk since key is constructed server-side, not from user input. |
| `@RequestBody Map<String, Boolean>` unvalidated | P2 | `AdminEmailController.updateConfig()` accepts raw Map — no size limit, no key whitelist. Attacker could send 100K keys to exhaust memory. |
| Flyway `validate-on-migrate: false` | P2 | Disables checksum validation — tampered migrations won't be detected. Comment says "prior repair runs" — should re-enable after cleanup. |

**Good:** ProcessBuilder uses list args (no shell). UUID path params auto-validated by Spring. `@Valid` on trigger request.

## 4. Auth & Access Control — 12/20

| Finding | Severity | Detail |
|---------|----------|--------|
| **No auth on admin endpoints** | **P0** | `AdminEmailController` at `/api/platform/admin/emails/**` — **zero** `@PreAuthorize`, `@Secured`, or `SecurityFilterChain` in entire kitehub-subscription module. No `spring-boot-starter-security` dependency (only `spring-security-crypto` for hashing). |
| **No auth on purge endpoint** | **P0** | `DELETE /api/platform/instances/{id}/purge` — permanently destroys data. No role check. Comment says "admin only" but not enforced. |
| **No auth on extend-trial** | **P1** | `POST /api/platform/instances/{id}/extend-trial` — no role check. Any caller can extend any trial. |
| Gateway routes unauthenticated | **P1** | Gateway `application.yml` routes `/api/platform/admin/**` to kitehub-admin service, but `/api/platform/admin/emails/**` goes to kitehub-subscription (port 8080 direct). Gateway has no JWT filter on these routes. |

**This is the most critical category.** The entire admin API surface is unprotected.

## 5. Data Safety — 16/20

| Finding | Severity | Detail |
|---------|----------|--------|
| Purge continues after partial failure | P2 | `executePurge()` catches each step independently and continues. If DB drop fails but backups delete succeeds, data is in inconsistent state (no DB, no backups). Should track partial failures and allow retry. |
| `emailLogsDeleted` counter always 0 | P3 | `emailLogsDeleted` initialized to 0 but never incremented (line 143 vs 176). Cosmetic bug in PurgeResult. |

**Good:**
- Purge verifies COMPLETED backup exists before proceeding (line 129)
- Backup uses SHA-256 checksums with verification
- Backup retention cleanup is orderly (keep N most recent)
- Destructive ops logged with instanceId, subdomain, details
- Instance must be DELETED status before purge (double-checked)

---

## Score Summary

| Category | Score | Notes |
|----------|-------|-------|
| Dependency Vulnerabilities | 17/20 | Outdated AWS SDK |
| Secrets & Credentials | 16/20 | Default MinIO creds in YAML |
| OWASP Top 10 | 15/20 | databaseName not regex-validated |
| Auth & Access Control | 12/20 | **P0: No auth on admin/purge endpoints** |
| Data Safety | 16/20 | Solid backup-before-purge |
| **TOTAL** | **76/100** | |

## P0 Action Items

1. **Add Spring Security** to kitehub-subscription with role-based access:
   - `/api/platform/admin/**` -> ROLE_ADMIN
   - `DELETE .../purge` -> ROLE_ADMIN
   - `POST .../extend-trial` -> ROLE_ADMIN
   - `POST /api/platform/instances` -> authenticated
   - `POST /api/platform/instances/register` -> public (self-service)
2. **Validate databaseName** with regex `^[a-zA-Z0-9_]{1,63}$` before passing to pg_dump.
3. **Remove default credentials** from S3 config (`minioadmin` defaults).
4. **Upgrade AWS SDK** from 2.20.26 to latest 2.31.x.
