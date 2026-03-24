# Storage — API Contract

> Extracted from: `StorageController`, `PresignedUploadRequest`, `PresignedUploadResponse`, `FileMetadataResponse`, `QuotaUsageResponse`
> Base path: `/api/v1/storage`

## Endpoints

### POST `/api/v1/storage/upload-url`
Generate presigned upload URL. Ref: UC-STR-01
- **Headers:** `X-User-Id` (Long, required), `X-Tenant-Id` (UUID, required)
- **Request:** `PresignedUploadRequest`
  - `fileName` (String, required, max 500)
  - `fileSize` (Long, required, min 1)
  - `mimeType` (String, required, max 100)
  - `fileType` (enum: IMAGE | DOCUMENT | VIDEO | AUDIO | OTHER, required)
  - `accessLevel` (enum: PUBLIC | PRIVATE | TENANT, default PRIVATE)
- **Response:** `ApiResponse<PresignedUploadResponse>` (201)
  - `fileId` (Long) — DB record ID for confirm step
  - `uploadUrl` (String) — presigned S3 PUT URL (30min TTL)
  - `expiresAt` (Instant) — URL expiration timestamp
- **Errors:** `400` FILE_TYPE_NOT_ALLOWED, `400` FILE_SIZE_EXCEEDS_MAXIMUM, `507` STORAGE_QUOTA_EXCEEDED

---

### POST `/api/v1/storage/{fileId}/confirm`
Confirm file upload after client uploads to S3. Ref: UC-STR-01
- **Path:** `fileId` (Long, required)
- **Response:** `ApiResponse<FileMetadataResponse>` (200)
  - `id` (Long), `uploaderId` (Long), `originalName` (String), `fileSize` (Long)
  - `mimeType` (String), `fileType` (FileType), `accessLevel` (AccessLevel)
  - `status` (StorageStatus — will be CONFIRMED), `createdAt` (Instant)
- **Errors:** `404` FILE_NOT_FOUND, `404` FILE_NOT_FOUND_IN_S3, `409` FILE_NOT_PENDING, `410` FILE_UPLOAD_EXPIRED

---

### GET `/api/v1/storage/{fileId}/download-url`
Generate presigned download URL. Ref: UC-STR-02
- **Headers:** `X-User-Id` (Long, required), `X-Tenant-Id` (UUID, required)
- **Path:** `fileId` (Long, required)
- **Response:** `ApiResponse<String>` (200) — presigned S3 GET URL (5min TTL)
- **Errors:** `404` FILE_NOT_FOUND, `403` FILE_ACCESS_DENIED, `409` FILE_NOT_CONFIRMED

---

### DELETE `/api/v1/storage/{fileId}`
Soft delete file. Ref: UC-STR-03
- **Path:** `fileId` (Long, required)
- **Response:** `ApiResponse<Void>` (204)
- **Errors:** `404` FILE_NOT_FOUND

---

### GET `/api/v1/storage/quota`
Get tenant storage quota usage. Ref: UC-STR-04
- **Headers:** `X-Tenant-Id` (UUID, required)
- **Response:** `ApiResponse<QuotaUsageResponse>` (200)
  - `tier` (StorageTier: FREE | BASIC | PRO | ENTERPRISE)
  - `usedBytes` (Long)
  - `quotaBytes` (Long)
  - `remainingBytes` (Long)
  - `usagePercentage` (Double — 0 to 100)
- **Errors:** None (auto-creates FREE tier quota if missing)

---

## Enums

| Enum | Values |
|------|--------|
| FileType | IMAGE, DOCUMENT, VIDEO, AUDIO, OTHER |
| AccessLevel | PUBLIC, PRIVATE, TENANT |
| StorageStatus | PENDING, CONFIRMED, EXPIRED, DELETED |
| StorageTier | FREE (1 GB), BASIC (10 GB), PRO (50 GB), ENTERPRISE (100 GB) |

## Verification Chain

| Rule | Use Case | Endpoint | Controller Method | Test |
|------|----------|----------|-------------------|------|
| BR-STR-001..009 | UC-STR-01 | POST /upload-url + POST /{fileId}/confirm | generateUploadUrl, confirmUpload | StorageIntegrationTest |
| BR-STR-011..013 | UC-STR-02 | GET /{fileId}/download-url | generateDownloadUrl | StorageIntegrationTest |
| BR-STR-007 | UC-STR-03 | DELETE /{fileId} | deleteFile | StorageIntegrationTest |
| BR-STR-008 | UC-STR-04 | GET /quota | getQuotaUsage | StorageIntegrationTest |
| BR-STR-006 | UC-STR-05 | N/A (scheduler) | StorageCleanupScheduler | StorageIntegrationTest |
