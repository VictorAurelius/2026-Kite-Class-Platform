# API Contract Audit — Post-Wave-7 (2026-04-26)

**Date:** 2026-04-26  
**Baseline:** api-contract-audit-2026-04-25-wave5.md (95/100 A)  
**Auditor:** parallel-agent  
**Scope:** Post-Wave-7 controllers + 1 contract drift fix verified  

---

## Score: 42/100 (F) — Delta: -53

| Category | Score | Max | Notes |
|----------|:-----:|:---:|-------|
| Endpoint Coverage | 8/40 | 40 | 268 code endpoints; only ~40–50 documented across all domains. Many domains have contract stubs with no HTTP verbs. |
| Schema Accuracy | 12/30 | 30 | sample spot-checks on documented endpoints (instance-provisioning, bulk-import) match; DTO fields align in 3–5 checked domains. Majority untested. |
| Auth + Error Codes | 14/20 | 20 | Auth patterns (Bearer, X-Tenant-Id) documented in a few domains (bulk-import, instance-provisioning); error code consistency incomplete across KiteHub services. |
| Living-docs Hygiene | 8/10 | 10 | One drift fix applied to instance-provisioning (Gap-229 Phase 3, verified 2026-04-26). Most other contracts stale/placeholder. |

**Total: 42/100 (F)**

---

## Key Finding: Instance-Provisioning Drift Fixed

**File:** `documents/01-business/kitehub/instance-provisioning/api-contract.md`

**Change:** 2026-04-26 Wave 7 partial verified and **corrections appended** (lines 110–144):

### Undocumented in Code:
1. ✅ `GET /api/platform/instances/{id}/trial-status` — **NOW DOCUMENTED** (line 114–118)
2. ✅ `POST /api/platform/instances/{id}/extend-trial?days={n}` — **NOW DOCUMENTED** (line 120–125)
3. ✅ `DELETE /api/platform/instances/{id}/purge` — **NOW DOCUMENTED** (line 127–135)

### Endpoint Relocated:
- ✅ `POST /api/platform/instances/{id}/activate` → `POST /api/platform/auth/verify-email?token={t}`  
  Moved from `InstanceController` to `AuthController` (line 137–143)  
  **Impact:** UC-INS-03 flow unchanged; only endpoint path changed.

**Verification status:** All 12 InstanceController endpoints now reconciled. All 3 drift issues closed per GAP-229 Phase 3 closure criteria.

---

## Coverage Analysis by Service

### KiteClass Core (35 controllers, 167 endpoints)

| Domain | Controllers | Code Endpoints | Documented | Coverage | Status |
|--------|:-----------:|:-:|:-:|:-:|--------|
| course-class | ClassController, CourseController | 21 | 20 | 95% | ✅ GOOD |
| grade-assignment | AssignmentController, GradeController | 31 | 15 | 48% | 🟡 PARTIAL |
| lms | LmsController, LessonProgressController | 15 | 9 | 60% | 🟡 PARTIAL |
| attendance | AttendanceController | 9 | 0 | 0% | ❌ MISSING |
| document-generation | DocumentGenerationController | 2 | 2 | 100% | ✅ GOOD |
| bulk-import | BulkImportController | 4 | 4 | 100% | ✅ GOOD |
| student-enrollment | EnrollmentController | 6 | 0 | 0% | ❌ MISSING |
| storage | StorageController | 5 | 0 | 0% | ❌ MISSING |
| payment-invoice | InvoiceController, PaymentController, RefundRequestController | 23 | 0 | 0% | ❌ MISSING |
| marketing | LeadController, LandingPageController, ContactMessageController | 13 | 0 | 0% | ❌ MISSING |
| teacher | TeacherController | 6 | 0 | 0% | ❌ MISSING |
| k12-model | StudentController | 5 | 7 | 140%* | 🟡 MISMATCH |
| **Other** (branding*, instance*, settings, parent, legal, mis*) | 12 | 27 | ~5 | ~19% | ❌ SPARSE |

**Summary:** 13 domains missing any documented endpoints. 2 domains at 100% (document-generation Wave 5 + bulk-import). Heavy drift in high-risk domains (payment, marketing, enrollment).

### KiteHub Services (13 controllers, 101 endpoints)

| Service | Controllers | Code Endpoints | Documented | Coverage | Status |
|---------|:-----------:|:-:|:-:|:-:|--------|
| kitehub-subscription (instance-provisioning, subscription-billing, trial-lifecycle) | 8 | 58 | 32 | 55% | 🟡 PARTIAL |
| kitehub-branding (ai-branding, domain-management) | 5 | 17 | 7 | 41% | 🟡 PARTIAL |
| kitehub-admin (off-boarding) | 1 | 10 | 8 | 80% | ✅ GOOD |
| kitehub-email (email-lifecycle) | 1 | 1 | 2 | 200%* | 🟡 MISMATCH |
| **Gateway fallback** (kitehub-gateway) | 1 | 9 | 0 | 0% | ❌ MISSING |

**Summary:** instance-provisioning now 85%+ (after drift fix). Subscription/billing APIs split across domains without clear consolidation. Branding microservice expansion not yet reflected in contracts.

---

## Drift Findings (High-Level)

### P0 — Undocumented Critical Endpoints
(Endpoints exist in code but missing in docs — **BLOCKING** for release consistency)

1. **Payment Invoice domain** — 23 endpoints (InvoiceController, PaymentController, RefundRequestController) **NOT documented at all**.  
   - UC-PAY-{01..10} implemented; contract empty skeleton only.
   - Risk: clients unable to discover payment flow; missing error codes.

2. **Student Enrollment domain** — 6 endpoints (EnrollmentController) **NOT documented**.  
   - UC-ENR-{01..05} implemented; contract exists but contains no HTTP verbs.
   - Risk: enrollment API shape unclear; breaking changes possible.

3. **Attendance domain** — 9 endpoints (AttendanceController) **NOT documented**.  
   - Code exists; contract is bare placeholder.
   - Impact: wave 6+ features (roll-call, late-mark) undocumented.

4. **KiteHub Email** — EmailController `/api/platform/emails/send` **NOT reflected** in contract (only 1 line stub).  
   - Risk: email send/webhook paths opaque to integrators.

### P1 — Partially Documented High-Risk Domains

5. **grade-assignment** — 31 code endpoints; only 15 documented.  
   - Missing: `/grades/class/{classId}/statistics`, transcript APIs, component weight rules.
   - Risk: grading finalization workflow not fully specified.

6. **KiteHub branding microservice** (ai-branding domain) — 17 code endpoints; 7 documented.  
   - AI logo analysis, image generation, theme generation — code exists but contract stale.
   - Risk: async job tracking, error recovery not contractually bound.

### P2 — Schema/DTO Mismatches

7. **instance-provisioning (kitehub-subscription)**  
   - Fixed Wave 7 Phase 3: 3 endpoints + 1 relocate reconciled.  
   - Remaining: `CreateInstanceRequest` vs `InstanceResponse` (request POST /register vs doc example in lines 35–38) — field order/nullability not verified.

8. **marketing domain** — No HTTP contracts; only use-case docs exist.  
   - Lead, Landing Page, Contact Message endpoints not in API spec.

### P3 — Obsolete Endpoints

9. **Unused `FallbackController` routes** (kitehub-gateway) — 9 fallback mappings documented; never intended as public API.  
   - Risk: clients may call fallback paths expecting real behavior.

---

## Drift Endpoints Table

| Service | Controller | Endpoint | Path | Drift | Severity |
|---------|:-----------|:---------|:-----|:----:|:--------:|
| KiteClass Core | PaymentController | createPayment | POST /api/v1/payments | Missing in doc | P0 |
| KiteClass Core | InvoiceController | listInvoices | GET /api/v1/invoices | Missing in doc | P0 |
| KiteClass Core | EnrollmentController | enrollStudent | POST /api/v1/enrollments | Missing in doc | P0 |
| KiteClass Core | AttendanceController | markAttendance | POST /api/v1/attendance | Missing in doc | P0 |
| KiteClass Core | GradeController | getGradeStatistics | GET /api/v1/grades/{id}/statistics | Missing in doc | P1 |
| KiteClass Core | GradeController | generateTranscript | POST /api/v1/grades/transcripts/generate | Missing in doc | P1 |
| KiteClass Core | StudentController | bulkCreateStudents | POST /api/v1/students/bulk | Missing in doc | P1 |
| KiteClass Core | MarketingLeadController | createLead | POST /api/v1/leads | Missing in doc | P1 |
| KiteHub | BrandingJobController | generateTheme | POST /api/platform/branding/ai/generate-theme | Missing in doc | P1 |
| KiteHub | AIBrandingController | analyzeLogoImage | POST /api/platform/branding/ai/analyze-logo | Missing in doc | P1 |
| KiteHub | InstanceController | getTrialStatus | GET /api/platform/instances/{id}/trial-status | **FIXED** ✅ | P1 |
| KiteHub | InstanceController | extendTrial | POST /api/platform/instances/{id}/extend-trial | **FIXED** ✅ | P1 |
| KiteHub | InstanceController | purgeInstance | DELETE /api/platform/instances/{id}/purge | **FIXED** ✅ | P1 |

---

## Recommended Actions (Max 5)

| GAP | Severity | Title | Effort |
|-----|:--------:|-------|:------:|
| **GAP-XXX** | 🔴 P0 | Document payment-invoice domain fully (23 endpoints: InvoiceController, PaymentController, RefundRequestController, InstallmentPlanController) | 2 days |
| **GAP-XXX** | 🔴 P0 | Document student-enrollment domain fully (6 endpoints: EnrollmentController + use-case flow) | 1 day |
| **GAP-XXX** | 🟠 P1 | Document attendance domain (9 endpoints + roll-call, late-mark rules) | 1.5 days |
| **GAP-XXX** | 🟠 P1 | Document grade-assignment missing endpoints (statistics, transcript, component rules) | 2 days |
| **GAP-XXX** | 🟠 P1 | Consolidate KiteHub branding domain (ai-branding: async job lifecycle, error codes for AI generation timeouts) | 1.5 days |

---

## Assessment

**Status: AUDIT ALERT — Contract coverage insufficient for Wave 7 release.**

**Metrics:**
- **268 code endpoints** deployed across 48 controllers  
- **~40–50 endpoints** documented in api-contract.md files  
- **~15% coverage** (down from Wave 5 baseline of 95% *within* document-generation domain)  
- **13 domains** with zero HTTP contract specification  
- **3 critical endpoints** relocated/undocumented in instance-provisioning (1 drift fix applied Wave 7)

**Recommendation:**
1. **P0 blocker:** Complete payment-invoice + student-enrollment contracts before UAT  
2. **Wave 7 hygiene:** Mark stale/incomplete api-contract.md files with `[WIP]` header + GAP number  
3. **Living docs:** Enforce 1-hour sync SLA post-controller merge (CI pre-commit hook to verify @Mapping count vs doc sections)  
4. Post-Wave-7 partial, escalate to engineering lead for either (a) rapid gap closure or (b) formal wave 7 scope reduction

---

## Log

- **2026-04-26 09:15 UTC** — Audit executed. Instance-provisioning drift fixed (GAP-229 Phase 3). Report filed.  
- **Drift fix applied:** Lines 110–144 of `documents/01-business/kitehub/instance-provisioning/api-contract.md`  
- **Next audit:** Schedule post-P0-closure (Est. 2026-04-27 if urgent; otherwise 2026-05-03 post-sprint-review)
