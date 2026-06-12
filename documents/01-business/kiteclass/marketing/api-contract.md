# Marketing — API Contract

## ContactMessageController

### POST /api/v1/contact
**Use Case:** UC-MKT-01  |  **Auth:** Public  |  **Header:** X-Tenant-Id (required)
```json
// Request
{ "name": "string", "email": "string?", "subject": "string?", "message": "string", "phone": "string?" }
// Response 201
{ "success": true, "message": "Contact message sent successfully", "data": { "id": "long", "name": "string", "email": "string", "subject": "string", "message": "string", "phone": "string", "isRead": false, "readBy": null, "readAt": null, "createdAt": "datetime" } }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Name/message is required; email invalid format (khi có)" — email/subject optional per GAP-1221, subject trống → server default "Liên hệ từ {name}" |

### GET /api/v1/contact-messages
**Use Case:** UC-MKT-02  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Query:** `?isRead=true&page=0&size=20&sort=createdAt,desc`
- **Response 200:** `ApiResponse<PageResponse<ContactMessageResponse>>`

### GET /api/v1/contact-messages/unread-count
**Use Case:** UC-MKT-02  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER  |  **Header:** X-Tenant-Id
- **Response 200:** `ApiResponse<Long>` — unread message count

### PUT /api/v1/contact-messages/{id}/read
**Use Case:** UC-MKT-02  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Query:** `?readBy=teacher@example.com`
- **Response 200:** `ApiResponse<ContactMessageResponse>` with updated read status
- **404:** `CONTACT_MESSAGE_NOT_FOUND`

### DELETE /api/v1/contact-messages/{id}
**Use Case:** UC-MKT-02  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Response 204:** No Content (soft delete)
- **404:** `CONTACT_MESSAGE_NOT_FOUND`

---

## LeadController — `/api/v1/leads`

### POST /api/v1/leads
**Use Case:** UC-MKT-03  |  **Auth:** Public  |  **Header:** X-Tenant-Id (required)
```json
// Request
{ "email": "string", "name": "string", "phone": "string?", "source": "LeadSource?", "courseInterestId": "long?", "message": "string?" }
// Response 201
{ "success": true, "message": "Lead created successfully", "data": { "id": "long", "email": "string", "name": "string", "phone": "string", "source": "LANDING_PAGE", "courseInterestId": "long", "status": "NEW", "message": "string", "createdAt": "datetime" } }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Email/name is required" |
| 409 | DUPLICATE_LEAD | "Lead email already exists" |

**LeadSource enum:** `LANDING_PAGE`, `CONTACT_FORM`, `TRIAL_SIGNUP`, `REFERRAL`, `SOCIAL_MEDIA`, `OTHER`

### GET /api/v1/leads
**Use Case:** UC-MKT-04  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Query:** `?status=NEW&page=0&size=20&sort=createdAt,desc`
- **Response 200:** `ApiResponse<PageResponse<LeadResponse>>`

### GET /api/v1/leads/{id}
**Use Case:** UC-MKT-04  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Response 200:** `ApiResponse<LeadResponse>`
- **404:** `LEAD_NOT_FOUND`

### PUT /api/v1/leads/{id}/status
**Use Case:** UC-MKT-05  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Query:** `?newStatus=CONTACTED`
- **Response 200:** `ApiResponse<LeadResponse>` with updated status
- **404:** `LEAD_NOT_FOUND`

**LeadStatus enum:** `NEW`, `CONTACTED`, `QUALIFIED`, `CONVERTED`, `LOST`, `INVALID`

### PUT /api/v1/leads/{id}
**Use Case:** UC-MKT-04  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
```json
// Request (all fields optional)
{ "email": "string?", "name": "string?", "phone": "string?", "source": "LeadSource?", "courseInterestId": "long?", "message": "string?" }
```
| Status | Code | Message |
|--------|------|---------|
| 404 | LEAD_NOT_FOUND | "Lead not found" |

### DELETE /api/v1/leads/{id}
**Use Case:** UC-MKT-04  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Response 204:** No Content (soft delete)
- **404:** `LEAD_NOT_FOUND`

---

## LandingPageController — `/api/v1/tenants/{tenantId}/landing`

### GET /api/v1/tenants/{tenantId}/landing
**Use Case:** UC-MKT-06  |  **Auth:** Public
- **Response 200:** `ApiResponse<LandingPageResponse>`
```json
{ "success": true, "data": { "heroTitle": "string", "heroSubtitle": "string", "heroImageUrl": "string", "teacherBio": "string", "logoUrl": "string", "tagline": "string", "primaryColor": "#3B82F6", "secondaryColor": "#10B981", "contactEmail": "string", "contactPhone": "string", "address": "string", "facebookUrl": "string", "youtubeUrl": "string", "instagramUrl": "string" } }
```

### PUT /api/v1/tenants/{tenantId}/landing
**Use Case:** UC-MKT-06  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
```json
// Request (all fields optional — partial update)
{ "heroTitle": "string?", "heroSubtitle": "string?", "heroImageUrl": "string?", "teacherBio": "string?", "logoUrl": "string?", "tagline": "string?", "primaryColor": "#hex?", "secondaryColor": "#hex?", "contactEmail": "string?", "contactPhone": "string?", "address": "string?", "facebookUrl": "string?", "youtubeUrl": "string?", "instagramUrl": "string?" }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Color must be hex format #RRGGBB" |
