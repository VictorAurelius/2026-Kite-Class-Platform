# Marketing — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## Use Cases

### UC-MKT-01: Submit Contact Message

**Actor:** Website Visitor (anonymous)
**Precondition:** Tenant landing page is accessible

**Steps:**
1. FE: Display contact form on landing page (name, email, phone, subject, message)
2. User: Fill name + message (bắt buộc); email/subject/phone optional per BR-MKT-001 v2 (GAP-1221 — subject trống → server default "Liên hệ từ {name}")
3. System: Validate input fields and size constraints
4. System: Save contact message linked to tenant via `X-Tenant-Id` (BR-MKT-005)
5. System: Send email notification to tenant admin/teacher (BR-MKT-003)
6. FE: Show success toast "Contact message sent successfully"

**Postcondition:** Message created with `isRead = false`, notification email sent

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Name/email/subject/message blank | Validation error details |
| 400 | Email format invalid | "Email is invalid" |
| 400 | Message exceeds 2000 chars | "Message size exceeded" |

---

### UC-MKT-02: Manage Contact Messages

**Actor:** Admin / Teacher
**Precondition:** Authenticated with ADMIN or TEACHER role

**Steps:**
1. FE: Display contact message inbox with unread badge (from unread-count endpoint)
2. User: Filter by read/unread status, paginate and sort
3. System: Return paginated messages sorted by `createdAt desc` by default (BR-MKT-004)
4. User: Click message to view details
5. System: Mark message as read, record `readBy` and `readAt` (BR-MKT-006)
6. User: Optionally delete message
7. System: Soft-delete message (BR-MKT-004)

**Postcondition:** Message list managed, unread count updated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Message ID not found | "Contact message not found" |
| 403 | Insufficient role | "Access denied" |

---

### UC-MKT-03: Register as Lead

**Actor:** Website Visitor (anonymous)
**Precondition:** Tenant landing page is accessible

**Steps:**
1. FE: Display trial registration / interest form (email, name, phone optional max 20 chars per BR-MKT-002, source, courseInterestId optional links to course per BR-MKT-015, message)
2. User: Fill required fields (email, name per BR-MKT-011)
3. System: Validate email uniqueness within tenant (BR-MKT-010)
4. System: Set lead source if provided (BR-MKT-014)
5. System: Save lead with status NEW (BR-MKT-012); soft delete only (BR-MKT-016); endpoint requires only X-Tenant-Id header, no auth (BR-MKT-017)
6. FE: Show success toast "Lead created successfully"

**Postcondition:** Lead created with status NEW, optionally linked to course interest

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Email/name blank | Validation error details |
| 400 | Email format invalid | "Email is invalid" |
| 409 | Email already exists for tenant | "Lead email already exists" |

---

### UC-MKT-04: Manage Leads

**Actor:** Admin / Teacher
**Precondition:** Authenticated with ADMIN or TEACHER role

**Steps:**
1. FE: Display lead list with status filter tabs (NEW, CONTACTED, QUALIFIED, etc.)
2. User: Filter by status, paginate and sort
3. System: Return paginated leads sorted by `createdAt desc` by default
4. User: Click lead to view detail
5. System: Return full lead information including source and course interest
6. User: Update lead info (name, email, phone, source, courseInterestId, message)
7. System: Validate and save updated lead

**Postcondition:** Lead information updated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Lead ID not found | "Lead not found" |
| 403 | Insufficient role | "Access denied" |

---

### UC-MKT-05: Update Lead Status

**Actor:** Admin / Teacher
**Precondition:** Authenticated, lead exists

**Steps:**
1. FE: Show lead detail with current status and available transitions
2. User: Select new status from dropdown (BR-MKT-013)
3. System: Validate status transition (NEW -> CONTACTED -> QUALIFIED -> CONVERTED/LOST)
4. System: Update lead status
5. FE: Show success toast, update status badge

**Postcondition:** Lead status updated, visible in lead list

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Lead not found | "Lead not found" |
| 400 | Invalid status value | "Invalid lead status" |

---

### UC-MKT-06: View & Update Landing Page

**Actor:** Admin / Teacher (update), Website Visitor (view)
**Precondition:** Tenant exists

**Steps:**
1. FE (public): Render landing page using tenant's configuration (BR-MKT-023)
2. FE (admin): Display landing page editor with current values
3. User: Edit hero section (title, subtitle, image), teacher bio, branding (logo, tagline, colors)
4. User: Edit contact info (email, phone, address) and social links (Facebook, YouTube, Instagram)
5. System: Validate color format (BR-MKT-021) and size constraints (BR-MKT-024)
6. System: Find or create one landing page per tenant (BR-MKT-020); partial update — only non-null fields overwritten (BR-MKT-022)
7. FE: Show success toast, preview updated landing page

**Postcondition:** Landing page content updated for tenant

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invalid color hex format | "Color must be hex format #RRGGBB" |
| 400 | Field exceeds size limit | Validation error details |
| 403 | Insufficient role | "Access denied" |
