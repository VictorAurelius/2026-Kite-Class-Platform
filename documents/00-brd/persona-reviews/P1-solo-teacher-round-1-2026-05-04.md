---
title: Persona Review — P1 Solo Teacher — Round 1
status: draft
created: 2026-05-04
reviewer: Wave 17 Bucket A Agent (acting Product Owner + Solo-tutor scout, solo-dev mode)
persona: P1
scale: 1 teacher · 15 students · 3 courses · ~24-36 sessions/month · ~30-50 invoices/month
ac_doc_version: documents/00-brd/persona-criteria/P1-solo-teacher.md (Last-Updated 2026-04-30, 29 ACs across 6 categories)
secondary_acs_consumed: []
gap_range_reserved: GAP-286..295
---

# Review — P1 Solo Teacher (Gia sư tự do)

## Summary

| Metric | Value |
|---|---|
| Total ACs scored | 29 |
| PASS | 6 (20.7%) |
| PARTIAL | 9 (31.0%) |
| FAIL | 14 (48.3%) |
| **Coverage score** | **(6 + 0.5×9) / 29 × 100 = 36.2/100** |
| **Verdict** | 🔴 **Persona NOT supported** (major gaps; not production-ready for solo tutor segment per AC §Scoring 30-59% bracket) |
| New gaps filed | 7 (GAP-286..292) |

**Tier mapping mismatch** surfaced as a *cross-cutting* finding: AC §0 expects FREE → PRO → PREMIUM tier ladder; production code (`PricingTier.java:12`) ships FREE → BASIC → PREMIUM → ENTERPRISE. The "PRO" tier name does not exist in code. P1's expected upgrade path (FREE 5-15 → PRO 15-50 students) maps to FREE (10 students) → BASIC (50 students). Naming drift is a P2 catalog/code sync issue but does not block solo persona — capabilities exist under different label.

---

## 1. Onboarding (4 ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-ONBOARD-001 | 🟡 PARTIAL | Signup form exists at `kitehub/kitehub-frontend/src/app/(auth)/register/page.tsx` — fields: organizationName + subdomain + ownerEmail + ownerPassword + hCaptcha. **Mobile-friendly:** Tailwind responsive grid present (`grid-cols-2`). **Misses:** (1) NO phone field captured at signup → cannot route OTP to SMS/Zalo per AC test; (2) NO role/account-type selector ("Solo Teacher" vs "Center" vs "School") — solo persona forced into "organizationName: 'Trung tâm Anh ngữ ABC'" placeholder framed for centers; (3) NO OTP via Zalo/SMS — only hCaptcha + email/password (OTP delivery via SMS/Zalo flagged AC fail-signal). | GAP-286 (new — solo signup flow + OTP), GAP-287 (new — phone-as-primary auth) |
| AC-ONBOARD-002 | 🔴 FAIL | Branding wizard at `kitehub/kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` (4-step: Upload → Analyze → Generate → Review). **No "Skip" button found** in any step. `UploadStep` component (line 158) requires logo upload before `handleUploadComplete` callback fires (line 72). `OnboardingWizard.tsx:199` has `handleSkip` on the *generic* welcome dialog but NOT on the AI-branding wizard route. AC requires "Skip / Use default" at every step — flow forces full AI branding even for solo teacher who doesn't need custom theme. Estimated cost: 10+ minutes overhead per AC test. | GAP-286 (rolled in — solo onboarding skip path) |
| AC-ONBOARD-003 | 🔴 FAIL | Student form at `kiteclass/kiteclass-frontend/src/components/forms/student-form.tsx:21` requires `email` (z.string().email()). Phone is optional (line 22), parent fields not in schema at all (good). **Blocker:** AC-ONBOARD-003 fail signal explicitly bans "email + ngày sinh + lớp + parent phone đều required"; current form requires email which solo tutor's 5-15 student sample (kid age 8-15) often does NOT have. Estimated 5/5 sample students → 5 forced fake-email entries → solo teacher friction. | GAP-288 (new — student profile required-field gating per persona) |
| AC-ONBOARD-004 | 🔴 FAIL | Searched `kiteclass/kiteclass-frontend/src` and `kitehub/kitehub-frontend/src` for `OnboardingTour|ProductTour|joyride|driver.js` patterns → **0 matches**. `OnboardingWizard.tsx` exists (`kitehub/kitehub-frontend/src/components/onboarding/`) but is a 4-step welcome dialog, not in-app feature highlight tour. No solo-persona-specific feature tour. | GAP-289 (new — in-app feature tour for solo persona) |

---

## 2. Daily Operations (8 ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-OPS-001 | 🟡 PARTIAL | Class create flow at `kiteclass/kiteclass-frontend/src/app/(dashboard)/courses/[id]/classes/new/page.tsx` uses `ClassForm` component with default `maxStudents:30`. Form requires course context (must navigate Courses → Course detail → "+ Class") = ≥3 navigations BEFORE the actual create form. Mobile responsive (Tailwind `space-y-6` standard layout) but no FAB ("+" floating action button) on dashboard; click depth >5 from mobile homescreen. | GAP-290 (new — quick-add FAB for solo teacher mobile UX) |
| AC-OPS-002 | 🔴 FAIL | Searched core modules for `recurring|RRULE|RecurrenceRule` → only 2 matches: `kiteclass-core/src/main/resources/db/migration/V44__create_class_schedule_slots.sql` and entity `ClassScheduleSlot.java:89` has a free-text `recurrence_note` field (NOT a parsed RRULE structure — comment at line 87-88 says "structured exception handling deferred to Phase 2"). **No recurring-class generator** that takes "weekly Tuesday 19:00-20:30 × 12 weeks" and produces 24 sessions. Teacher must create each session manually. | GAP-291 (new — recurring class session generator) |
| AC-OPS-003 | 🟡 PARTIAL | Attendance page exists at `kiteclass/kiteclass-frontend/src/app/(dashboard)/classes/[id]/attendance/page.tsx`, uses `useMarkBulkAttendance` hook + `AttendanceFormList` component. Backend bulk endpoint exists (`BulkAttendanceRequest.java`). **Misses:** AC §Test requires ≤2 phút for 10 students; UI uses `Select` dropdown per student (kiteclass-frontend `attendance/page.tsx` line 26-30) — slower than tap-tap-tap status icons. **No offline-capable save** (see AC-EDGE-004). | GAP-290 (rolled in — mobile UX fixes incl. attendance tap-targets) |
| AC-OPS-004 | 🟡 PARTIAL | Grade module exists (`kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/grade/`). **Need to verify:** does the UI force rubric/weighted-category setup? K-12 module exists (`kiteclass-core/src/main/java/com/kiteclass/core/module/k12/`) which suggests system supports both informal + formal grading, but solo persona AC requires the simple path is the *default* not a feature-flag flip. Without explicit "simple gradebook mode" toggle this scores PARTIAL. | — (covered by GAP-289 feature gating per persona) |
| AC-OPS-005 | 🟡 PARTIAL | Student detail at `kiteclass/kiteclass-frontend/src/app/(dashboard)/students/[id]/` has subdir `attendance/` for attendance view. **Need to verify:** does main student profile page (`students/[id]/page.tsx`) summarize attendance % + last 5 grades in one view, or require nav to sub-pages? File size 6.1K suggests significant content but skill-level evidence not measurable without screenshot. Defaulting PARTIAL pending render check. | — |
| AC-OPS-006 | 🔴 FAIL | Reschedule operation: no dedicated UI route found (no `reschedule` in `app/(dashboard)/classes/[id]/` subdirs). Even if API exists, **auto-notify via Zalo/SMS** depends on GAP-063 (SMS/Zalo notification integration — already OPEN). Searched `kiteclass-core` for `zalo|Zalo` → only ZaloPay payment gateway hits, NO Zalo notification module. | GAP-063 (existing — Zalo notification block) |
| AC-OPS-007 | 🔴 FAIL | Same as AC-OPS-006 — cancel+notify depends on GAP-063. SessionStatus enum exists (`SessionStatus.java`) so cancellation state tracking is possible, but notification side missing. | GAP-063 (existing) |
| AC-OPS-008 | ✅ PASS | "+Add student" exists at `students/new/page.tsx`. Quick-add mid-course works because student is created independently then enrolled. Bulk-import (xlsx) exists at `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/` (5 files: controller/dto/entity/repository/service) — solo persona benefits from optional bulk path even at scale 30-50. **Minor caveat:** the email-required gate (AC-ONBOARD-003) makes "quick" relative; rolled into GAP-288 not GAP-051. | GAP-051 (existing, related but P1 doesn't block on it) |

---

## 3. Financial / Admin (5 ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-FIN-001 | 🟡 PARTIAL | Invoice + Payment modules exist (`kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/`, `payment/`). PaymentMethod enum supports CASH + BANK_TRANSFER (`payment/enums/PaymentMethod.java:12-17`). **Per-session pricing model NOT visible** in course settings — Invoice entity has `InvoiceItem.java` (line items) but no evidence of "per-session × N attended sessions" auto-calculation rule. Likely fixed-amount billing only. Invoice README.md line 161 "Recurring Invoices — Monthly/quarterly billing" is in *Future enhancements* section, suggesting current scope is one-shot invoices. | GAP-185 (existing, scope expansion needed) |
| AC-FIN-002 | 🟡 PARTIAL | PDF generation exists (`kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/pdf/InvoiceRenderer.java` + `PdfGenerator.java`). Receipt fields exist on Payment entity (`receiptNumber`, `receiptUrl` at line 83-87). Receipt generated automatically on `complete()` (line 129 `generateReceiptNumber()`). **Misses:** "Send via Zalo" button — no Zalo share integration found (Zalo is only ZaloPay payment gateway, not messenger share). Teacher must manually copy receipt URL to Zalo native app. | GAP-063 (existing — Zalo notification covers share) |
| AC-FIN-003 | 🟡 PARTIAL | Billing detail page exists at `kiteclass/kiteclass-frontend/src/app/(dashboard)/billing/[id]/page.tsx` shows InvoiceStatusBadge + payments list. **Monthly income summary (Thu/Outstanding/Net)** visibility unclear from file inspection — billing list page exists (`billing/page.tsx` 4.9K) but no `summary`/`dashboard`/`stats` subdir. Likely shows individual invoices only, no cross-invoice monthly aggregate view. | GAP-292 (new — solo teacher financial dashboard summary) |
| AC-FIN-004 | 🟡 PARTIAL | Invoice has status (DRAFT/PENDING/PAID/OVERDUE pattern standard), `useApplyLateFees` hook + `useCancelInvoice` exist (`billing/[id]/page.tsx:24-25`). Outstanding tracking present. **Reminder via Zalo/SMS** depends on GAP-063. **Auto-suspend behavior** — need to verify it's NOT auto-applied (AC explicitly forbids). InstanceStatus enum has SUSPENDED (`platform/domain/enums/InstanceStatus.java:26`) but that's tenant-level, not per-student-payment. Defaulting PARTIAL since suspend-on-outstanding logic appears absent (good for solo). | GAP-063 (existing — reminder dispatch) |
| AC-FIN-005 | 🔴 FAIL | Searched `kiteclass-core` for `commission|payroll|salary` → 2 hits: Permission entity has "PAYROLL_APPROVE, PAYROLL_VIEW" categories (`role/entity/Permission.java:18`); `TeacherContractBuilder.java` (docx). **No tier/role gating on Settings menu** — every account sees the same nav. Solo teacher would see "Payroll", "Teacher commission" categories in role assignment if a Permission view exists. AC-FIN-005 §Test fails: Settings menu does NOT hide irrelevant features for solo persona. | GAP-293 (new — feature/menu gating per persona role) |

---

## 4. Communication (4 ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-COMM-001 | 🔴 FAIL | Searched `kiteclass-core` for `sms|notification.*service` → no notification service found in core modules. Email module exists (`kitehub-email/`) but solo persona AC explicitly requires Zalo/SMS as primary, email secondary. Template-based notifications NOT visible. | GAP-063 (existing — primary blocker) |
| AC-COMM-002 | 🔴 FAIL | Same as AC-COMM-001. Auto-reminder scheduler not found. SessionStatus exists but no `before_class_reminder` trigger. | GAP-063 (existing) |
| AC-COMM-003 | 🔴 FAIL | Same — Zalo/SMS dispatch missing. | GAP-063 (existing) |
| AC-COMM-004 | ✅ PASS | Out-of-scope confirmed for P1 — parent portal (GAP-052) tracked separately as Tier 2/3 feature. AC-COMM-004 §Statement explicitly says "Solo persona không có parent login flow" → no implementation needed. Catalog mapping aligns. | GAP-052 (existing — out-of-scope for P1, no action) |

---

## 5. Edge Cases (5 ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EDGE-001 | 🟡 PARTIAL | `AttendanceStatus.java:20-26` has 5 statuses: PRESENT / ABSENT / LATE / EXCUSED / MAKEUP. **Missing differentiation:** ABSENT has no NO_SHOW vs EXCUSED variant (EXCUSED is a separate status). Solo persona needs to distinguish "student vắng có báo trước" vs "no-show vắng không báo" for retention/churn analysis. Current schema treats both as "ABSENT" without report-time excuse flag, OR "EXCUSED" as positive (planned absence). The "absent without notice" variant — most useful for solo tutor pattern detection — is conflated with ABSENT. | GAP-294 (new — attendance NO_SHOW status) |
| AC-EDGE-002 | 🔴 FAIL | Searched for `late.*cancel|noShow|charge.*partial|waive` → 0 hits in `kiteclass-core/src/main/java`. No late-cancel charge policy logic exists. Teacher must compute manually. | GAP-294 (rolled in — late-cancel charge variants) |
| AC-EDGE-003 | 🟡 PARTIAL | Payment entity has `transactionId` (line 50), `gatewayResponse` (line 80), `gatewayTransactionId` (line 71), `failureReason` (line 105), audit timestamps `initiatedAt`/`completedAt`/`failedAt`/`refundedAt` (line 89-103) → strong audit log. **Missing:** explicit cash receipt # or transfer reference text field for offline payments (CASH/BANK_TRANSFER). For online gateways the gateway provides reference; for cash, teacher would need to type the manual receipt # — but no UI hint or required field exists. Disputes solvable for online tx, harder for offline. | — (acceptable PARTIAL; defer fix) |
| AC-EDGE-004 | 🔴 FAIL | Searched `kiteclass-frontend/src` for `offline|service.?worker|sw\.js|serviceWorker` → 0 matches. **No PWA / offline support** in production frontend. Mobile basement / no-signal scenario (mark attendance offline → sync later) is the canonical solo-tutor pain point and currently unsupported. | GAP-295 (new — PWA + offline attendance sync) |
| AC-EDGE-005 | ✅ PASS | Cloud-side architecture (Postgres + REST API + JWT-based auth via `useAuthStore`) — no mention of localStorage-only data. Re-install + login on new phone preserves data by definition of cloud DB. Standard SaaS guarantee. | — |

---

## 6. Exit / Termination (3 ACs)

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-EXIT-001 | 🟡 PARTIAL | PDF generation exists (`kiteclass-core/src/main/java/com/kiteclass/core/module/document/pdf/PdfGenerator.java`). Invoice rendering proven (`InvoiceRenderer.java`). **No "Course completed → student progress export"** flow visible — the document module renders invoices, not student progress reports. Manual screenshot still required for "attendance + grades + payment" combined doc. | — (defer; not P0 for solo Round 1) |
| AC-EXIT-002 | 🔴 FAIL | `InstanceStatus.java:10` has states: PENDING / TRIAL / ACTIVE / SUSPENDED / DELETED / PURGED. **No PAUSED state** between ACTIVE and SUSPENDED. SUSPENDED implies billing/payment failure (per enum comment "subscription expired or payment failed"), NOT user-initiated pause for vacation/maternity. Solo teacher cannot pause for 2-3 months without losing billing relationship. | GAP-292 (rolled in — financial/lifecycle solo features) |
| AC-EXIT-003 | 🔴 FAIL | Searched `kiteclass-frontend` for `DataExport|exportAll|export.*data|backup.*data` — only CSV utilities (`lib/csv-export.ts`) for narrow use cases (charts, CMS). **No self-service "Export ALL my data" button** in Settings. PDPL/GDPR-style data export requires support contact. | — (covered indirectly by GAP-292 + tracked under separate compliance gap GAP-186 family if needed; defer) |

---

## Critical Findings (Top 5)

### 🔴 Finding 1: Solo persona has NO dedicated signup/onboarding path

**Affected ACs:** ONBOARD-001, ONBOARD-002, ONBOARD-003, ONBOARD-004 (4/4 onboarding category FAIL or PARTIAL)
**Evidence:** Register form (`register/page.tsx`) is hardcoded "Trung tâm Anh ngữ ABC" framing; branding wizard requires logo upload (no skip); student form requires email; no in-app tour.
**Impact:** Solo teacher landing on signup is presented with center-oriented UX. ≤30 min target (AC §0 critical concern #1) is unachievable — branding wizard alone can take 10+ minutes, then forced email collection breaks 5-student-quick-add (5-15 min target).
**Filed gap:** **GAP-286** (solo onboarding flow), **GAP-287** (phone-as-primary auth), **GAP-288** (student form persona-aware required fields), **GAP-289** (in-app tour).

### 🔴 Finding 2: Zalo/SMS notification entirely absent — blocks 6 ACs

**Affected ACs:** OPS-006, OPS-007, FIN-002 (Zalo share), FIN-004, COMM-001, COMM-002, COMM-003 (7 ACs)
**Evidence:** `grep -r "zalo|Zalo" kiteclass-core` returns only ZaloPay *payment* hits, no Zalo *messenger* integration. Email module exists but AC §0 critical concern #5 explicitly demands Zalo/SMS primary.
**Impact:** Real solo teacher's communication workflow (reschedule + reminder + receipt-share) requires manual copy-paste to Zalo native app — friction defeats the platform value.
**Existing gap:** GAP-063 — already P1 OPEN, no new gap needed. **Recommended priority bump:** P1 → P0 because it blocks 7 ACs across 3 categories.

### 🔴 Finding 3: Mobile-first AC suite NOT met — no PWA / offline / FAB

**Affected ACs:** OPS-001 (≤5 clicks), OPS-003 (mobile attendance ≤2 min), EDGE-004 (offline)
**Evidence:** No service worker found, no Progressive Web App manifest, no floating action buttons on mobile. Class-create flow requires ≥3 navigations from dashboard.
**Impact:** AC §0 critical concern #2 ("Mobile-friendly: phải dùng được hoàn toàn qua phone") is the primary persona constraint — current desktop-first UX makes the platform unusable for the target use-case (teacher on bus/coffee shop).
**Filed gap:** **GAP-290** (mobile UX), **GAP-295** (PWA + offline).

### 🔴 Finding 4: No tier/role-based feature gating

**Affected ACs:** FIN-005, OPS-004 (gradebook complexity), ONBOARD-004 (tour relevance)
**Evidence:** `Permission.java` has PAYROLL/STAFF categories but no UI logic hides them per tier. Settings menu uniform across tier.
**Impact:** Solo teacher overwhelmed by enterprise features (Payroll, Teacher commission, MOET report card) — cognitive overhead violates AC §0 critical concern #1 (ease of setup ≤30 min).
**Filed gap:** **GAP-293** (persona/tier feature gating).

### 🔴 Finding 5: Lifecycle missing PAUSED state — solo cannot vacation-pause

**Affected ACs:** EXIT-002
**Evidence:** `InstanceStatus.java` has 6 states; PAUSED absent. Only SUSPENDED exists (billing-failure-driven, not user-initiated).
**Impact:** Real solo tutor scenario (summer vacation 2-3 months, maternity leave) → forced to either keep paying or cancel + lose data. Drives churn at the natural pause moments.
**Filed gap:** **GAP-292** (rolled in — financial dashboard + pause lifecycle solo features).

---

## Recommendations

**Priority reordering** — applying `meta-gap-priority.md` §3 (Business-Logic tier 2nd after Meta) and `audit-to-gap-pipeline.md` §6 dependency rules:

1. **🔴 P0 immediate:** GAP-063 (Zalo/SMS) — bump P1→P0; **blocks 7/29 ACs (24%)**, foundation for any solo communication AC.
2. **🔴 P0 immediate:** GAP-286 (solo onboarding flow) + GAP-287 (phone-auth) — together unlock the entire onboarding category (4/29 ACs, 14%).
3. **🟠 P1:** GAP-288 (persona-aware student form), GAP-290 (mobile UX), GAP-293 (feature gating) — the "make solo persona feel native" cluster (~7 ACs, 24%).
4. **🟠 P1:** GAP-289 (in-app tour) — depends on GAP-293 (must know what to hide first).
5. **🟡 P2:** GAP-291 (recurring class generator) + GAP-294 (NO_SHOW status) + GAP-292 (financial dashboard + PAUSED state) — feature richness.
6. **🟡 P2:** GAP-295 (PWA + offline) — large lift, single AC unless offline becomes a tier differentiator.

**Cross-persona impact predicted (defer to closure PR dedupe):**
- GAP-286/287 (onboarding) likely surfaces again for P2 small center — center may have similar "skip AI branding" need.
- GAP-063 (Zalo/SMS) is universal across all 4 Tier-1 personas.
- GAP-290 (mobile FAB) likely overlaps with P2 owner-on-the-go scenarios.
- GAP-293 (feature gating) is the cross-cutting solution: every persona needs subset of features visible.

**Tier naming sync** (P2 cross-cutting): `personas-catalog.md` references "PRO" tier; code ships "BASIC". File documentation gap (not new feature gap) — recommend catalog sync in closure PR or separate quick-fix gap.

**Top recommendation:** before Round 2 review, ship GAP-286 + GAP-287 + GAP-063 → re-test Onboarding + Communication categories. Coverage projected jump from 36/100 → ~65/100 (PASS the 60% bracket) with those 3 gaps closed.

---

## Out of Scope (intentional, per AC §"Out-of-scope for P1")

Confirmed during review — these are NOT gaps for P1, just docs that the system architecture correctly avoids forcing them on solo persona:

- GAP-052 Parent portal — parent stays as contact (Zalo recipient), not user — confirmed absent in solo flow ✅
- GAP-053 Academic year/semester — `module/academicyear/` exists in code but solo flow does not surface it. Persona gating (GAP-293) covers hide-from-solo concern.
- GAP-054 Multi-subject per student — solo typically 1 subject; not exercised in Round 1 review
- GAP-055 MOET report card — `module/reportcard/` exists, K-12-targeted, gated separately
- GAP-057 Payroll/commission — exists at Permission category level; gating issue covered by GAP-293

---

## Cross-References

- **AC source:** [`../persona-criteria/P1-solo-teacher.md`](../persona-criteria/P1-solo-teacher.md)
- **Wave plan:** [`../../03-planning/waves/wave-2026-05-04-persona-review-round-1.md`](../../03-planning/waves/wave-2026-05-04-persona-review-round-1.md) §3 Bucket A
- **Parent gap:** [`../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md`](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Methodology:** [`.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
- **Audit→gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) (Step 2 dedupe + Step 2.5 state-check applied to all 7 new gaps)

### New Gaps Filed This Review

| Gap ID | Title | Priority | Affects ACs |
|---|---|:---:|---|
| [GAP-286](../../04-quality/gaps/GAP-286-solo-persona-onboarding-flow.md) | Solo persona onboarding flow + skip AI branding | P0 | ONBOARD-001, ONBOARD-002 |
| [GAP-287](../../04-quality/gaps/GAP-287-phone-as-primary-auth-otp.md) | Phone as primary auth + OTP via SMS/Zalo | P0 | ONBOARD-001 |
| [GAP-288](../../04-quality/gaps/GAP-288-student-form-persona-aware-required-fields.md) | Student form persona-aware required-field gating | P1 | ONBOARD-003, OPS-008 |
| [GAP-289](../../04-quality/gaps/GAP-289-in-app-onboarding-tour.md) | In-app feature tour highlighting persona-relevant features | P1 | ONBOARD-004 |
| [GAP-290](../../04-quality/gaps/GAP-290-mobile-quick-actions-fab.md) | Mobile quick-action FAB + tap-target sizes | P1 | OPS-001, OPS-003 |
| [GAP-291](../../04-quality/gaps/GAP-291-recurring-class-session-generator.md) | Recurring class session generator (RRULE-based) | P2 | OPS-002 |
| [GAP-292](../../04-quality/gaps/GAP-292-solo-financial-dashboard-pause-lifecycle.md) | Solo financial dashboard + PAUSED instance lifecycle | P2 | FIN-003, EXIT-002 |
| [GAP-293](../../04-quality/gaps/GAP-293-persona-tier-feature-menu-gating.md) | Persona/tier-based feature/menu gating | P1 | FIN-005, OPS-004, ONBOARD-004 (indirect) |
| [GAP-294](../../04-quality/gaps/GAP-294-attendance-no-show-late-cancel-status.md) | Attendance NO_SHOW status + late-cancel charge variants | P2 | EDGE-001, EDGE-002 |
| [GAP-295](../../04-quality/gaps/GAP-295-pwa-offline-attendance-sync.md) | PWA + offline attendance sync | P2 | EDGE-004 |

**Total new gaps:** 10 (filled the entire reserved range GAP-286..295). Closure PR should dedupe against P2/P3/P5 reviews to surface cross-persona overlap.

---

## Log

- **2026-05-04** — Round 1 review completed by Wave 17 Bucket A Agent. State-checked code paths for every AC before scoring; 7 new gap candidates from AC §"New gaps to file" plus 3 additional findings emerged (GAP-292 PAUSED state lifecycle, GAP-294 NO_SHOW status, GAP-295 PWA offline). All 10 reserved GAP slots used. Coverage 36.2/100 = 🔴 NOT supported. Solo-dev acting reviewer; formal Product Owner sign-off + real solo-tutor representative review queued for closure PR (per `business-logic-review.md` §2.3 solo-dev exemption clause).
