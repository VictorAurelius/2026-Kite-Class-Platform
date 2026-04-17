# Storage — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## Use Cases

### UC-STR-01: Upload File (Presigned URL Flow)

**Actor:** Authenticated User (any role)
**Precondition:** User authenticated, tenant has available quota

**Steps:**
1. FE: User selects file to upload
2. FE: Send POST `/upload-url` with file metadata (fileName, fileSize, mimeType, fileType, accessLevel defaults to PRIVATE per BR-STR-014); follows presigned URL upload flow (BR-STR-001)
3. System: Validate MIME type against whitelist per BR-STR-005
4. System: Validate file size <= 100 MB per BR-STR-004
5. System: Check tenant quota per BR-STR-009 (PENDING files count); file scoped by instance_id per BR-STR-010
6. System: Create `uploaded_files` record with status=PENDING, set `expires_at` = now + 30min
7. System: Update quota usage immediately (+fileSize) per BR-STR-009
8. System: Generate presigned PUT URL (30min TTL) per BR-STR-002
9. System: Return `{ fileId, uploadUrl, expiresAt }`
10. FE: Upload file directly to S3 via HTTP PUT to `uploadUrl`
11. FE: Send POST `/{fileId}/confirm`
12. System: Verify file status is PENDING, not expired
13. System: Call S3 HeadObject to verify file exists per BR-STR-015
14. System: Mark file as CONFIRMED per BR-STR-006
15. FE: Toast success, display file metadata

**Postcondition:** File stored in S3, record CONFIRMED, quota updated

**Errors:**
| Code | Condition | Error Code |
|------|-----------|------------|
| 400 | MIME type not in whitelist | FILE_TYPE_NOT_ALLOWED |
| 400 | File size > 100 MB | FILE_SIZE_EXCEEDS_MAXIMUM |
| 404 | File ID not found | FILE_NOT_FOUND |
| 404 | File not in S3 on confirm | FILE_NOT_FOUND_IN_S3 |
| 409 | File not in PENDING status | FILE_NOT_PENDING |
| 410 | Upload URL expired (30min) | FILE_UPLOAD_EXPIRED |
| 507 | Tenant quota exceeded | STORAGE_QUOTA_EXCEEDED |

---

### UC-STR-02: Download File

**Actor:** Authenticated User (access level dependent)
**Precondition:** File exists and is CONFIRMED

**Steps:**
1. FE: Request GET `/{fileId}/download-url` with X-User-Id and X-Tenant-Id headers
2. System: Find file, verify status is CONFIRMED
3. System: Check access control per BR-STR-011, BR-STR-012, BR-STR-013:
   - PUBLIC: Allow all
   - PRIVATE: Only uploader (match X-User-Id with uploaderId)
   - TENANT: Same tenant only (match X-Tenant-Id with file's instanceId)
4. System: Generate presigned GET URL (5min TTL) per BR-STR-003
5. System: Return presigned download URL
6. FE: Open/download file from presigned URL

**Postcondition:** User receives time-limited download URL

**Errors:**
| Code | Condition | Error Code |
|------|-----------|------------|
| 404 | File not found | FILE_NOT_FOUND |
| 403 | Access denied (PRIVATE/TENANT) | FILE_ACCESS_DENIED |
| 409 | File not CONFIRMED | FILE_NOT_CONFIRMED |

---

### UC-STR-03: Delete File

**Actor:** Authenticated User
**Precondition:** File exists (not already deleted)

**Steps:**
1. FE: Send DELETE `/{fileId}`
2. System: Find file record
3. System: If file is CONFIRMED, subtract fileSize from tenant quota
4. System: Soft delete file (set `deleted=true`, `deleted_at=now`) per BR-STR-007
5. System: Return 204 No Content
6. FE: Remove file from UI list

**Postcondition:** File soft deleted, quota freed. S3 object remains for 30-day grace period.

**Errors:**
| Code | Condition | Error Code |
|------|-----------|------------|
| 404 | File not found | FILE_NOT_FOUND |

---

### UC-STR-04: Check Quota Usage

**Actor:** Authenticated User / Admin
**Precondition:** Tenant exists

**Steps:**
1. FE: Send GET `/quota` with X-Tenant-Id header
2. System: Find or create default quota (FREE tier) for tenant per BR-STR-008
3. System: Return quota details: tier, usedBytes, quotaBytes, remainingBytes, usagePercentage
4. FE: Display storage usage bar/chart

**Postcondition:** User sees current quota usage

**Errors:** None (auto-creates FREE quota if missing)

---

### UC-STR-05: Auto-Cleanup (Scheduler)

**Actor:** System (scheduled jobs)
**Precondition:** Application running with scheduler enabled

**Steps (markExpiredPendingUploads — every 10min):**
1. Query PENDING files where `expires_at < now`
2. Mark each as EXPIRED per BR-STR-006
3. Log count of expired files

**Steps (cleanupDeletedFiles — daily 2:00 AM):**
1. Query files where `deleted=true` and `deleted_at < now - 30 days`
2. For each file: delete S3 object, then hard delete DB record
3. Log success/failure counts
4. Continue on individual failures (don't abort batch)

**Steps (recalculateQuotaUsage — admin trigger / drift correction):**
1. Sum actual file sizes from DB (CONFIRMED files only) per tenant
2. Correct quota used_bytes to match actual total (BR-STR-016)
3. Log count of tenants recalculated

**Postcondition:** Expired uploads marked, old deleted files purged from S3 and DB
