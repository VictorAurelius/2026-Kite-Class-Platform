# Acceptance Criteria — P2 Small Tutoring Center (Trung tâm nhỏ / Lớp học thêm)

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** P2
**Persona name (VN):** Trung tâm nhỏ / Lớp học thêm
**Persona name (EN):** Small Tutoring Center
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent B (Wave Persona-AC-Template, GAP-151 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real small-center owner + Product Owner sign-off (deferred to GAP-152 Round 1)
**Tier:** 1 Primary
**Tracking:** GAP-151 Phase 1 → GAP-152 → GAP-153

---

## 0. Context

### Scale assumption (from `personas-catalog.md`)
- **Users:** 60 students (target sample), 2 hired teachers + owner (3 teaching staff), 5 classes (multi-subject mix)
- **Data volume:** ~120 parent contacts (1-2 per student), ~200-300 attendance records/week, ~60 monthly invoices, ~5 classes × 2-3 sessions/week = 10-15 sessions/week
- **Usage pattern:** Weekday evenings (17h-21h) + weekend mornings (peak); admin work (collect tuition, send notices) typically Sunday evening or Monday morning before classes resume

### Organization archetype
- **Type:** Trung tâm dạy thêm / Lớp học thêm tư nhân (privately-owned tutoring center, văn-toán-anh-lý-hóa subjects)
- **Hierarchy:** Owner (also teaches) → 1-2 hired Teachers → Students. NO dedicated admin staff. Parents engage directly with Owner via Zalo.
- **Decision-making:** Owner does everything — signup, billing, scheduling, parent communication, teacher payroll. Manages on personal smartphone + spreadsheets today.

### Revenue tier mapping
- **Expected tier:** BASIC → PREMIUM (~500K-2M VND/month tolerance based on student count)
- **Reason:** Small ops with light financial admin needs; BASIC tier covers basic class management; PREMIUM unlocks parent portal + automated reminders that this persona increasingly demands at 60+ students. Below BASIC (FREE/TRIAL) is insufficient for real billing.

### Real-world reviewer profile
- **Acting role:** "Chủ lớp học thêm Toán-Anh tại Hà Nội, 60 học sinh, 2 giáo viên thuê (1 dạy Toán cấp 2, 1 dạy Anh cấp 3) + tự dạy Toán cấp 3, thu học phí 800K-1.5M/tháng/HS, hoạt động 4 năm"
- **Critical concerns:**
  1. **Parent communication via Zalo** — VN parents at this scale expect direct Zalo messages; SMS/email is fallback only
  2. **Teacher commission tracking** — 60% revenue share with hired teachers; must auto-compute monthly to avoid disputes
  3. **Multi-class scheduling** — same teacher teaches 2-3 classes; conflicts must be visible
  4. **Cash + bank transfer billing** — VN parents pay both ways; system must reconcile
  5. **Owner wears all hats** — every workflow must be doable in <5 min, mobile-friendly, with minimum config

---

## AC Categories (6 standardized)

Each AC has format:
- **AC-{CATEGORY}-{NUM}** (3-digit zero-padded ID)
- **Statement** (1 sentence — what must be verifiable)
- **Test** (concrete scenario — reviewer can simulate)
- **Fail signal** (what reviewer observes if system gaps)
- **Status** (PASS / PARTIAL / FAIL — filled at review time, not at AC creation time)
- **Linked gap** (if FAIL → existing GAP-XXX or NEW gap to file)

---

## 1. Onboarding AC

Initial signup → tenant provisioning → first usable state for a small tutoring center.

- [ ] **AC-ONBOARD-001:** Owner can complete signup + tenant provisioning + initial center configuration in ≤30 minutes from a smartphone
  - **Test:** From mobile browser, signup with email/phone → verify OTP → choose BASIC tier → provide center name + address + 5 subjects (Toán/Văn/Anh/Lý/Hóa) → land on first usable dashboard
  - **Fail signal:** Wizard requires desktop, OTP delivery >5 min, mandatory fields exceed what owner has on hand, dashboard shows empty state without CTAs
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-ONBOARD-002:** Owner can add 1-2 hired teachers (with subject assignment + commission rate) in ≤5 minutes per teacher
  - **Test:** Navigate to Teachers → "Add teacher" → input name + phone + subjects taught + commission % (e.g. 60%) → teacher receives invite SMS/Zalo → teacher confirms account
  - **Fail signal:** No commission field on teacher form, no Zalo/SMS invite option, teacher must self-register (owner cannot pre-create), commission rate cannot be set per-teacher
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-057](../../04-quality/gaps/GAP-057-payroll-commission.md) (commission tracking)

- [ ] **AC-ONBOARD-003:** Owner can bulk import 60 students via xlsx (or paste from Excel) in ≤10 minutes including credentials distribution
  - **Test:** Download xlsx template → fill 60 rows (name, parent phone, parent name, class assignment) → upload → preview validation → confirm → student accounts + parent Zalo invites sent
  - **Fail signal:** No xlsx template, parent phone field missing, validation rejects valid VN phone formats, no parent invite step, must add students one-by-one
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-051](../../04-quality/gaps/GAP-051-xlsx-import.md) (xlsx import)

- [ ] **AC-ONBOARD-004:** Owner can create 3-10 classes with multi-subject mix + recurring schedule in a single setup session
  - **Test:** Create class "Toán 9A" → assign teacher + room/online → set recurring schedule (e.g. T2-T4-T6 19h-21h) → set tuition (e.g. 1M VND/month) → save → repeat for 4 more classes
  - **Fail signal:** No recurring schedule (must add each session manually), no tuition field at class level, cannot mix online/offline, class creation takes >5 min each
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md) (multi-subject support)

---

## 2. Daily Operations AC

Recurring workflows after onboarding (multi-class scheduling, attendance, gradebook basics, teacher assignment).

- [ ] **AC-OPS-001:** Owner sees a unified weekly schedule view across all 5 classes with teacher conflicts highlighted
  - **Test:** Open Schedule → view week of 5 classes × 2-3 sessions = 10-15 sessions; assign same teacher to two overlapping slots → system highlights conflict in red
  - **Fail signal:** No unified view (must check per-class), no conflict detection, owner must mentally reconcile teacher assignments
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-002:** Teacher (or owner-as-teacher) takes attendance for a 15-student class in ≤2 minutes from mobile
  - **Test:** Teacher opens class session → roster auto-loads → tap-mark present/absent/late for 15 students → submit → attendance saved + parents of absent students notified
  - **Fail signal:** Manual student lookup, no bulk-default-present, no auto-notify on absence, requires desktop, takes >5 min
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-003:** Owner can record gradebook entries (test scores, assignment marks) per class with weighted average
  - **Test:** In class "Anh 12B" → Gradebook tab → add column "Kiểm tra 15p" (weight 20%) + "Kiểm tra 1 tiết" (weight 30%) + "Cuối kỳ" (weight 50%) → enter scores for 15 students → final average auto-computed per student
  - **Fail signal:** No weighted column types, manual averaging, scores cannot be edited after entry, no per-class gradebook (only global)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-004:** Hired teacher accesses ONLY their own classes (not other teachers' or financial data); owner sees everything
  - **Test:** Teacher A logs in → sees only Class "Toán 9A" + "Toán 9B" they teach; cannot see other teachers' classes, student contact details, tuition amounts, commission report
  - **Fail signal:** Teacher sees all classes, sees financial data, sees other teachers' info, no role separation
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-005:** Owner reschedules a single session (e.g. teacher sick day) and parents auto-notified within 5 minutes
  - **Test:** From schedule view, drag-drop or edit "Toán 9A T4 19h" to "Thứ 7 9h" → confirm reschedule → all 15 affected parents receive Zalo/SMS notification
  - **Fail signal:** Reschedule affects entire recurring series (cannot edit single session), no auto-notify, parents must be told manually
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) (SMS/Zalo notifications)

- [ ] **AC-OPS-006:** Owner can substitute teacher for a session without losing attendance/grade attribution
  - **Test:** Owner-as-teacher takes over Teacher A's "Anh 12B" session for 1 day → marks attendance + uploads handout → original teacher's commission for that session credits to substitute (or owner per config)
  - **Fail signal:** No substitute concept, attendance/grade locks to scheduled teacher, commission attribution wrong
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 3. Financial / Admin AC

Tuition collection (cash + bank transfer), monthly invoice generation, teacher commission tracking, expense tracking, simple receipt issuance.

- [ ] **AC-FIN-001:** Owner generates monthly tuition invoices for all 60 students in ≤5 minutes (batch operation)
  - **Test:** End of month → "Generate invoices" → system enumerates active students × class tuition → preview list (60 invoices) → confirm → invoices saved + Zalo/SMS sent to parents with payment link/instructions
  - **Fail signal:** Must generate one-by-one, no Zalo/SMS dispatch, no payment instructions in invoice, missing students not flagged
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-185](../../04-quality/gaps/GAP-185-billing-vat.md) (billing — VAT subset relevant for company-reimbursing parents)

- [ ] **AC-FIN-002:** Owner records cash payment from parent (most common at this scale) and issues a printable receipt
  - **Test:** Parent pays 1M VND cash for Student A's monthly tuition → owner opens invoice → "Mark paid (cash)" → enter amount + date → print/PDF receipt with center letterhead
  - **Fail signal:** No cash option (only online payment), no printable receipt, receipt missing center info, cannot reconcile with invoice
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-FIN-003:** Owner reconciles bank transfer with invoice (parent transfers to owner's account; system links manually)
  - **Test:** Bank notification of 1.5M from "Nguyen Thi B" → owner opens system → finds matching invoice → "Mark paid (bank transfer)" → enter ref number → invoice closed
  - **Fail signal:** No manual-link option (only auto-match required), no ref number field, cannot handle name mismatch (parent uses different name than registered)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-FIN-004:** System auto-computes teacher commission monthly based on collected revenue per teacher's classes (e.g. 60% of paid tuition)
  - **Test:** End of month → Commission report → see Teacher A: classes "Toán 9A" + "Toán 9B" → collected 25M VND × 60% = 15M VND payout. Teacher B: 18M × 60% = 10.8M. Excludes unpaid invoices.
  - **Fail signal:** Commission based on enrolled (not collected), commission rate not configurable per teacher, no exclusion of unpaid, manual computation required
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-057](../../04-quality/gaps/GAP-057-payroll-commission.md) (payroll/commission)

- [ ] **AC-FIN-005:** Owner can issue VAT invoice (hóa đơn đỏ) on demand for parents requesting reimbursement from employer
  - **Test:** Parent requests VAT invoice → owner opens paid invoice → "Issue VAT invoice" → input company tax code + name → system generates compliant e-invoice format → emailed to parent
  - **Fail signal:** No VAT invoice option, format non-compliant with VN e-invoice regulations, must use external tool
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-185](../../04-quality/gaps/GAP-185-billing-vat.md) (VAT/e-invoice)

---

## 4. Communication AC (stakeholders)

Parent engagement via Zalo (primary at this scale) + SMS fallback; schedule change notifications, attendance alerts, payment reminders.

- [ ] **AC-COMM-001:** Parent of an absent student receives Zalo message within 15 minutes of attendance submission (no separate manual step)
  - **Test:** Teacher marks student "absent" at 19h05 → parent's Zalo receives "Bé A vắng buổi học Toán 9A 19h hôm nay" by 19h20
  - **Fail signal:** No auto-notify, only SMS (parent doesn't read SMS), delay >1 hour, message templates not localized
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) (SMS/Zalo channel)

- [ ] **AC-COMM-002:** Owner sends a broadcast announcement to all parents of one class in ≤2 minutes (e.g. "lớp nghỉ Tết từ 28/1 đến 5/2")
  - **Test:** Class "Toán 9A" → Announcement → type message → choose channel (Zalo / SMS / both) → send → 15 parents receive → owner sees delivery confirmation
  - **Fail signal:** No broadcast tool, must message individually, no delivery confirmation, channel selection unavailable
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) (Zalo broadcast)

- [ ] **AC-COMM-003:** Parent receives auto-reminder for unpaid tuition 3 days before due-date (and again on due-date)
  - **Test:** Invoice due 5th of month → parent gets Zalo reminder on 2nd ("Học phí tháng X còn 1M chưa thanh toán, hạn 5/X") + on 5th ("Hôm nay là hạn chót")
  - **Fail signal:** No auto-reminder, owner must manually chase, reminder text missing amount/date, only via email (parent doesn't check email)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) (auto-reminder via Zalo)

- [ ] **AC-COMM-004:** Parent has a self-service view (web link, no app install needed) to see child's attendance + grades + invoice history
  - **Test:** Parent clicks Zalo link → opens responsive web page → sees their child's last 30 days attendance + last 5 grades + last 3 invoice statuses → no login OR simple OTP login
  - **Fail signal:** No parent self-service, requires app install, requires complex auth (parent gives up), data missing
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md) (parent portal)

---

## 5. Edge Cases AC

Failure scenarios — teacher absent, student transfers between classes, payment plan handling, schedule disruption.

- [ ] **AC-EDGE-001:** Teacher unexpectedly absent — owner can cancel/reschedule session AND notify all 15 parents in <10 minutes
  - **Test:** 17h, Teacher A reports sick → owner opens schedule → cancel "Toán 9A 19h tonight" → choose makeup slot → 15 parents Zalo'd within 5 min of cancellation
  - **Fail signal:** No bulk-cancel-and-notify flow, owner must do separately (cancel + message), parents miss notification, attendance still expects teacher
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-EDGE-002:** Student transfers from "Toán 9A" to "Toán 9B" mid-month — system pro-rates tuition + preserves attendance/grade history
  - **Test:** Move student from 9A (1M/month) to 9B (1.2M/month) on 15th → system prorates: 9A charges 500K (50% month), 9B charges 600K (50% month) → student's prior 9A attendance preserved + visible in 9B profile
  - **Fail signal:** Tuition fully charged twice or zero, attendance lost, requires manual invoice adjustment, history not portable
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-EDGE-003:** Parent requests payment plan (1M/month → 500K × 2 mid-month) — owner can split invoice without breaking commission calculation
  - **Test:** Original 1M invoice → "Split" → 500K due 5th + 500K due 20th → both pay → commission still computes 60% × 1M = 600K to teacher (not double-counted)
  - **Fail signal:** Cannot split, splits double-count for commission, splits lose link to original class
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 6. Exit / Termination AC

Student finishes / drops out, teacher leaves, owner closes center, data export.

- [ ] **AC-EXIT-001:** Student drops out mid-term — owner deactivates student + processes refund (if policy allows) + notifies parent
  - **Test:** Student "B" drops out 20th of month → owner deactivates → unpaid future invoices auto-cancelled → if pre-paid 3 months, owner can issue partial refund record → parent notified via Zalo
  - **Fail signal:** Cannot deactivate (only delete which loses history), future invoices still send, refund untrackable, no parent notification
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-EXIT-002:** Teacher leaves — owner can offboard teacher (final commission paid, classes reassigned, account deactivated) without losing historical attendance/grade attribution
  - **Test:** Teacher A resigns → owner runs "Offboard Teacher" → final commission calculated through last working day → reassign classes to substitute → deactivate Teacher A account → past attendance + grades remain attributed to Teacher A in history
  - **Fail signal:** Deactivation deletes historical attribution, commission stuck open, classes orphaned, must be done manually field-by-field
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-057](../../04-quality/gaps/GAP-057-payroll-commission.md) (final commission settlement)

- [ ] **AC-EXIT-003:** Owner closes center — full data export (students, classes, attendance, grades, invoices, payments, commission ledger) in standard format within 7 days of request
  - **Test:** Owner requests "Export all data" → within 7 days, receives downloadable archive (xlsx + PDF combined): student roster, full attendance log, full grade log, full invoice/payment ledger, commission history → archive is complete + reviewable
  - **Fail signal:** Export missing categories, format proprietary (cannot open in Excel/Google Sheets), >7 day delay, partial data, contracts/PII included unredacted
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## Scoring

**Total ACs:** 25 (sum across 6 categories: 4 Onboard + 6 Ops + 5 Fin + 4 Comm + 3 Edge + 3 Exit)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Persona fully supported (production-ready for this persona) |
| 60-84% | ⚠️ Persona partially supported (usable but with gaps; defer GA for this persona) |
| 30-59% | 🔴 Persona NOT supported (major gaps; not production-ready) |
| <30% | ❌ Persona NOT viable (fundamental misfit; consider deferring to Tier 2/3 or out-of-scope) |

---

## Gap Linkage Summary

Gather all FAIL/PARTIAL ACs with linked gaps into one table for review report digest:

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-002 | TBD | [GAP-057](../../04-quality/gaps/GAP-057-payroll-commission.md) | TBD | TBD |
| AC-ONBOARD-003 | TBD | [GAP-051](../../04-quality/gaps/GAP-051-xlsx-import.md) | TBD | TBD |
| AC-ONBOARD-004 | TBD | [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md) | TBD | TBD |
| AC-OPS-005 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-FIN-001 | TBD | [GAP-185](../../04-quality/gaps/GAP-185-billing-vat.md) | TBD | TBD |
| AC-FIN-004 | TBD | [GAP-057](../../04-quality/gaps/GAP-057-payroll-commission.md) | TBD | TBD |
| AC-FIN-005 | TBD | [GAP-185](../../04-quality/gaps/GAP-185-billing-vat.md) | TBD | TBD |
| AC-COMM-001 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-COMM-002 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-COMM-003 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-COMM-004 | TBD | [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md) | TBD | TBD |
| AC-EXIT-002 | TBD | [GAP-057](../../04-quality/gaps/GAP-057-payroll-commission.md) | TBD | TBD |

**New gaps to file** (FAIL ACs without existing gap — go through `audit-to-gap-pipeline.md` Step 2.5 state-check before filing):
- TBD — surfaced at GAP-152 Round 1 review. Likely candidates: substitute-teacher attribution (AC-OPS-006), mid-term student transfer pro-rate (AC-EDGE-002), payment-plan splitting without commission double-count (AC-EDGE-003), full data-export-on-close compliance bundle (AC-EXIT-003).

---

## Cross-References

- **Persona source:** [`../personas-catalog.md`](../personas-catalog.md) §P2 Small Tutoring Center
- **Sibling persona ACs:** P0 Solo Teacher, P1 Family Class, P3 Medium Center, P5 K-12 School (Wave Persona-AC-Template Agents A/C/D parallel deliverables)
- **Review skill:** [`../../../.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
- **Review reports:** [`../persona-reviews/`](../persona-reviews/) (output of GAP-152 quarterly reviews)
- **AC framework gap:** [GAP-151](../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (this template)
- **Review execution gap:** [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check
- **Cross-link reference gaps:**
  - [GAP-051](../../04-quality/gaps/GAP-051-xlsx-import.md) — bulk xlsx import (60 students)
  - [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md) — parent portal (RELEVANT at 60-student scale)
  - [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md) — multi-subject (5 subjects văn-toán-anh-lý-hóa)
  - [GAP-057](../../04-quality/gaps/GAP-057-payroll-commission.md) — teacher commission (60% revenue share)
  - [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) — Zalo/SMS (PRIMARY parent comm channel)
  - [GAP-185](../../04-quality/gaps/GAP-185-billing-vat.md) — billing + VAT (some parents request hóa đơn đỏ)

---

## Notes for Reviewer (Phase 2 — Domain Expert)

This AC set is calibrated for the **lower end** of P2 (60 students, 2 teachers). Real centers may scale up to 100 students / 3 teachers — most ACs scale linearly, but the following may need re-test at upper bound:
- AC-ONBOARD-003 (xlsx import 100 vs 60 students)
- AC-FIN-001 (batch invoicing 100 vs 60)
- AC-COMM-002 (broadcast 200 contacts vs 120)

If reviewer finds these strain at 100-student scale, consider sub-tier "P2-large" with separate AC adjustments OR file follow-up gap for capacity testing.

**Out-of-scope for P2** (covered by P3/P5 personas, not this AC set):
- Role-based access beyond Owner/Teacher (no admin/accountant role at this scale — Owner does it)
- Multi-course catalog with course materials library (P3+)
- Marketing website / branding (P3+)
- Semester/school-year structure (P5 K-12)
- MOET reporting (P5 K-12)
- Homeroom teacher (GVCN) concept (P5 K-12)

---

## Anti-Patterns to Avoid in Review

| ❌ Don't | ✅ Do |
|---------|------|
| Mark AC PASS because feature exists somewhere in codebase | Verify the persona-specific scenario at 60-student / 2-teacher scale |
| Assume parent will install an app | VN parents at this scale use Zalo only — verify Zalo channel works |
| Test with synthetic English data | Test with VN names, VN phone formats, VND currency, lunar New Year holidays |
| Score "PASS" if commission tracking exists for ENTERPRISE only | Persona-tier match required: BASIC/PREMIUM must support 60% commission tracking |
| Bundle this persona's ACs with P3 (Medium Center) | P2's defining constraint is "no admin staff" — P3 ACs assume admin |

---

## Log

- **2026-04-30** — Initial AC set v1 (25 ACs across 6 categories). Author: Agent B (Wave Persona-AC-Template, GAP-151 Phase 1). Calibrated for 60-student / 2-teacher / 5-subject baseline tutoring center in Hà Nội. Sources: `personas-catalog.md` §P2 + informed-gut research on VN tutoring center operating norms (Zalo-primary parent comm, 60% teacher commission norm, cash + bank transfer billing mix, VAT invoice for company-reimbursing parents). Cross-linked to GAP-051/052/054/057/063/185.
