# Notification & Email — Use Cases

> Last verified: 2026-03-24 | Source: `kiteclass-core/module/marketing/`, `kiteclass-core/common/service/email/`

## Use Cases

### UC-NTF-01: Submit Contact Message

**Actor:** Visitor (unauthenticated)
**Precondition:** Landing page with contact form is accessible.

**Steps:**
1. FE: Display contact form (name, email, subject, message)
2. Visitor: Fills in form and submits
3. System: Saves ContactMessage with `instance_id` = tenant (per EM-07)
4. System: Calls `emailService.sendContactNotification(adminEmail, name, email, subject, message)` (per EM-04, BR-MKT-003)
5. System: Admin email from config `contact.admin-email`, default `admin@kitehub.me` (per EM-05)
6. System: If email fails, logs error but returns success (per EM-06)
7. FE: Show success toast

**Postcondition:** ContactMessage saved. Admin notified via email (best-effort).

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Missing required fields | Validation error |
| — | Email send failure | Silently logged, operation succeeds |

---

### UC-NTF-02: Manage Contact Messages (Admin)

**Actor:** Admin
**Precondition:** Admin is authenticated.

**Steps:**
1. FE: Load message list via `GET /api/v1/contact-messages` with pagination
2. FE: Display unread count badge via `GET /api/v1/contact-messages/unread-count`
3. Admin: Clicks message to read details
4. System: Mark as read via `PUT /api/v1/contact-messages/{id}/read`
5. Admin: Optionally deletes message via `DELETE /api/v1/contact-messages/{id}`

**Postcondition:** Message marked as read or deleted.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Message not found | EntityNotFoundException |

---

### UC-NTF-03: Register Lead / Trial Interest

**Actor:** Visitor (unauthenticated)
**Precondition:** Landing page with lead registration form.

**Steps:**
1. FE: Display lead registration form
2. Visitor: Submits name, email, phone, etc.
3. System: Validate email uniqueness within tenant (per EM-08, BR-MKT-002)
4. System: Save Lead with `instance_id` = tenant (per EM-07)
5. System: Call `emailService.sendLeadConfirmation(email, name)` — subject: "Thank you for your interest - KiteClass" (per BR-MKT-004)
6. System: If email fails, logs error but returns success (per EM-06)
7. FE: Show confirmation message

**Postcondition:** Lead record created. Confirmation email sent (best-effort).

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Duplicate email in tenant | `LEAD_EMAIL_ALREADY_EXISTS` |
| 400 | Missing required fields | Validation error |

---

### UC-NTF-04: Manage Leads (Admin)

**Actor:** Admin
**Precondition:** Admin is authenticated.

**Steps:**
1. FE: Load leads list via `GET /api/v1/leads` with pagination
2. Admin: View lead detail via `GET /api/v1/leads/{id}`
3. Admin: Update lead status via `PUT /api/v1/leads/{id}/status` (e.g., NEW -> CONTACTED -> CONVERTED)
4. Admin: Update lead info via `PUT /api/v1/leads/{id}`
5. Admin: Delete lead via `DELETE /api/v1/leads/{id}`

**Postcondition:** Lead updated or deleted.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Lead not found | EntityNotFoundException |
| 400 | Duplicate email on update | `LEAD_EMAIL_ALREADY_EXISTS` |

---

## Notes

- **LoggingEmailService** is the default implementation — logs all emails, sends nothing (per EM-02)
- **Production** requires swapping to SMTP or external provider (per EM-03)
- **Email failure isolation** is a core design principle — business ops never fail due to email (per EM-06)
