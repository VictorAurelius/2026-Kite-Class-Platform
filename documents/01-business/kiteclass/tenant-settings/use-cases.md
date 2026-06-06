# Tenant Settings — Use Cases

> Last verified: 2026-03-24 | Source: `kiteclass-core/module/settings/`

## Use Cases

### UC-TNT-01: Get Tenant Branding

**Actor:** Admin / System / Visitor (public theme endpoint)
**Precondition:** Tenant context resolved from request.

**Steps:**
1. FE: Request `GET /api/v1/settings/branding`
2. System: Resolve `instanceId` from TenantContext
3. System: Find Branding by `instanceId` (deleted=false)
4. System: If not found, return default branding object (per BR-SET-02): displayName="KiteClass" (BR-SET-03), tagline (BR-SET-04), colors #3B82F6 (BR-SET-05), #8B5CF6 (BR-SET-06), #10B981 (BR-SET-07)
5. FE: Apply branding to UI (logo, colors, name)

**Postcondition:** Branding data returned. Default values used if no custom branding exists (not persisted).

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| — | No branding found | Returns defaults, no error |

---

### UC-TNT-02: Update Tenant Branding

**Actor:** Admin
**Precondition:** Admin is authenticated with tenant context.

**Steps:**
1. FE: Display branding form pre-filled with current values
2. Admin: Modifies display name, tagline, colors, contact info, social links
3. System: Validate color format `#RRGGBB` (per BR-SET-08)
4. System: Validate displayName not blank, max 200 (per BR-SET-09), tagline max 500 (per BR-SET-10)
5. System: Validate contactEmail format (per BR-SET-15), contactPhone max 20 (per BR-SET-16), social links max 500 chars each (per BR-SET-17)
6. System: Find existing branding — if not found, create new with defaults + request fields
7. System: If found, PATCH update only provided fields (per BR-SET-14)
8. System: Save and return BrandingResponse
9. FE: Show success toast, update UI

**Postcondition:** Branding record created or updated for tenant (per BR-SET-01); themeConfigJson preserved (BR-SET-13); soft-delete flag available per BR-SET-18.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invalid color format | Color must be `#RRGGBB` hex format |
| 400 | Empty display name | Display name is required |
| 400 | Invalid email format | Contact email must be valid |

---

### UC-TNT-03: Upload Logo / Favicon

**Actor:** Admin
**Precondition:** Admin is authenticated with tenant context.

**Steps:**
1. FE: Display upload button for logo or favicon
2. Admin: Selects image file
3. System: Upload file to S3 storage (per BR-SET-11, BR-SET-12)
4. System: Find or create Branding for tenant
5. System: Set `logo_url` or `favicon_url` with S3 URL (max 500 chars)
6. System: Save and return BrandingResponse
7. FE: Preview uploaded image

**Postcondition:** Logo/favicon URL stored in branding record.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | File too large / invalid type | Upload validation error |
| 500 | S3 upload failure | Storage service error |

---

### UC-TNT-04: Get / Update User Preferences

**Actor:** Authenticated User (any role)
**Precondition:** User is authenticated. UserId in path must match authenticated user.

**Steps:**
1. FE: Request `GET /api/v1/users/{userId}/preferences`
2. System: Verify userId matches authenticated user (permission check)
3. System: Return user preferences (language, theme, notifications)
4. User: Changes language, theme, or notification settings
5. FE: Send `PATCH /api/v1/users/{userId}/preferences` with changed fields only
6. System: Validate language code and theme code format
7. System: PATCH update only provided fields
8. FE: Apply new preferences (language switch, theme toggle)

**Postcondition:** User preferences updated. Partial update supported.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invalid language code | Validation error |
| 400 | Invalid theme code | Validation error |
| 401 | Not authenticated | `USER_NOT_AUTHENTICATED` |
| 403 | UserId mismatch | `USER_ACCESS_DENIED` |

---

### UC-TNT-05: Initialize Default User Preferences

**Actor:** System (on first login or admin trigger)
**Precondition:** User exists but has no preferences record.

**Steps:**
1. System/Admin: Call `POST /api/v1/users/{userId}/preferences/initialize`
2. System: Verify userId matches authenticated user
3. System: Create default preferences (default language, theme, notification settings)
4. System: Return created preferences

**Postcondition:** Default preferences record created for user.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 401 | Not authenticated | `USER_NOT_AUTHENTICATED` |
| 403 | UserId mismatch | `USER_ACCESS_DENIED` |

---

### UC-TNT-06: Get / Update Landing Page

**Actor:** Admin
**Precondition:** Admin is authenticated with tenant context.

**Steps:**
1. FE: Request `GET /api/v1/tenants/{tenantId}/landing`
2. System: Return landing page configuration for tenant
3. Admin: Edits hero section, features, CTA text, etc.
4. FE: Send `PUT /api/v1/tenants/{tenantId}/landing` with full landing page config
5. System: Save landing page configuration
6. FE: Preview updated landing page

**Postcondition:** Landing page configuration saved for tenant.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invalid landing page data | Validation error |

---

## Per-tenant configuration use cases — TenantSettings (GAP-947)

> Last verified: 2026-06-06 | Source: GAP-947, Wave provisioning-1 Bucket F | Code: `kiteclass-core/module/tenantsettings/`

### UC-TSET-01: Xem settings của tenant

**Actor:** Owner / Admin của tenant (trường học)
**Precondition:** Đã đăng nhập, có `X-Tenant-Id` header.

**Steps:**
1. FE gọi `GET /api/v1/tenants/{id}/settings` (`{id}` = tenant id).
2. BE verify `{id}` == tenant hiện tại (X-Tenant-Id).
3. Nếu chưa có bản ghi → auto-create default (timezone `Asia/Ho_Chi_Minh`, locale `vi`, Năm học auto-fill, schoolType `CENTER`).
4. Trả về settings.

**Postcondition:** Settings tồn tại + trả về; Năm học luôn có sẵn (vd `2026-2027`).

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 403 | `{id}` ≠ tenant đăng nhập | TENANT_ACCESS_DENIED (cross-tenant IDOR) |

### UC-TSET-02: Cập nhật settings

**Actor:** Owner / Admin của tenant

**Steps:**
1. FE gọi `PUT /api/v1/tenants/{id}/settings` với các field cần đổi.
2. BE verify `{id}` == tenant hiện tại.
3. Nếu chưa có bản ghi → tạo default trước, rồi merge.
4. Merge provided-field-wins (field null giữ giá trị cũ).
5. Trả về settings đã cập nhật.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 403 | `{id}` ≠ tenant đăng nhập | TENANT_ACCESS_DENIED |
| 400 | academicYear sai format / schoolType ngoài enum / field vượt độ dài | Validation error |

### UC-TSET-03: Default settings khi provision tenant mới

**Actor:** Hệ thống (tenant provisioning flow)

**Steps:**
1. Lần đầu truy cập settings (UC-TSET-01) → default tự sinh.
2. Năm học auto-compute theo thời điểm provision (VN Sep→May).

**Postcondition:** Mọi tenant luôn có settings hợp lệ ngay từ lần truy cập đầu (lazy-create, không cần seed riêng).
