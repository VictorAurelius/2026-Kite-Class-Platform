# 04 — Component Gaps

12 components NOT in Round 1 bundle but **required** for production. Round 1 distilled tokens + 6 page archetypes; these are the missing building blocks.

**Use this when:** Claude Design picks Round 2 deliverables. Each component → 1 HTML demo file with 4 states (default / loading / empty/error / success) + 1 spec `.md`.

---

## Existing component inventory (DO NOT redesign these)

**KiteHub UI:** 17 shadcn/ui base + 4 custom (gradient-button, gradient-text, page-header, section-title) + 34 feature components.
**KiteClass UI:** 22 shadcn/ui base (incl. calendar, popover, sheet, skeleton, tooltip, form, radio-group, avatar, confirm-dialog, toast/toaster) + 123 feature components.

Both apps have these established patterns — Round 2 should compose, not redesign:
- shadcn primitives (button, card, input, dialog, dropdown-menu, select, switch, tabs, badge, alert)
- TanStack Table (data tables)
- React Hook Form + Zod (forms)
- Sonner toast (KH) / shadcn toast (KC)
- Framer Motion (KH only — animations)

---

## Component gap list (12 missing)

### G1. Bulk Import Drop-zone + Job Tracker

| Spec | Detail |
|------|--------|
| **Why missing** | Backend complete, frontend has zero entry point (GAP-137 P0) |
| **Used in** | KC `/students` (entry button) → modal/full-page flow |
| **Persona** | P3 Medium Center Admin, P5 K–12 Principal |
| **Sub-components** | (a) Drop-zone (drag-drop xlsx, click-to-browse), (b) Preview table with validation errors highlighted, (c) Commit progress bar, (d) Error report download |
| **States** | empty → file-selected → validating → preview-with-errors → committing → success / partial-success / failure |
| **VN UX** | Sample file download link `Tải file mẫu (.xlsx)`. Errors localized: `Dòng 23: Số điện thoại không hợp lệ`. |
| **Constraints** | Max 5 MB, ≤ 10k rows, batch insert 500/txn |
| **Reference flow** | `05-business-flows.md` Flow #2 |

### G2. Attendance Roster (Mark P/V/M/L)

| Spec | Detail |
|------|--------|
| **Why missing** | Round 1 bundle didn't recreate this — repo has `attendance-form-list.tsx` but design doesn't show pattern |
| **Used in** | KC `/classes/[id]/attendance` |
| **Persona** | Teacher (homeroom + subject) |
| **Sub-components** | (a) Class header (date, session #, total students), (b) Student row (avatar, name, MST, 4-button toggle P/V/M/L), (c) Quick "mark all P" button, (d) Save bar (n changes) |
| **States** | default → marking (optimistic UI) → saving → saved (lock + edit-history) → error (retry) |
| **VN UX** | Color codes: P=green / V=blue / M=red / L=amber. Show attendance rate inline. Late minutes input. Excuse note inline. |
| **Touch UX** | Big tap targets (44×44 min) — used on tablet at center |
| **Reference flow** | `05-business-flows.md` Flow #3 |

### G3. Gradebook Entry Grid

| Spec | Detail |
|------|--------|
| **Why missing** | Component doesn't exist in repo yet — needed for grade entry flow |
| **Used in** | KC `/classes/[id]/grades` (new route, post-Round 2) |
| **Persona** | Teacher |
| **Sub-components** | (a) Grade column header (assignment, weight %, max score), (b) Student × Assignment grid (sticky first col), (c) Inline edit cell, (d) Auto-calc final column, (e) Bulk export to xlsx |
| **States** | default → editing-cell → validation-error (above max) → saving → saved → finalize-confirm → finalized (read-only) |
| **VN UX** | Score 0-10 decimal allowed (8.5, 9.25). Honor row at bottom: Xuất sắc/Giỏi/Khá/TB/Yếu count. Late penalty banner. |
| **Reference flow** | `05-business-flows.md` Flow #4 |

### G4. Class Schedule Manager

| Spec | Detail |
|------|--------|
| **Why missing** | Repo has class-edit but no recurring schedule UI |
| **Used in** | KC `/classes/[id]/schedule` (new) |
| **Persona** | Teacher / Owner |
| **Sub-components** | (a) Week-view grid (Mon-Sun × 6am-10pm), (b) Drag-create slot, (c) Recurrence picker (weekly, biweekly, custom), (d) Conflict warning (other class same time), (e) Holiday/break overlay |
| **States** | empty → creating → conflict-error → saved → editing → past-locked |
| **VN UX** | First day of week = Monday (Vietnamese convention, NOT Sunday). Vietnamese holidays auto-overlay (Tết, 30/4, 2/9). |
| **Reference flow** | linked to attendance flow |

### G5. Payment Method Selector (VN Multi-Gateway)

| Spec | Detail |
|------|--------|
| **Why missing** | Repo has `payment-method-selector.tsx` partial — design doesn't fully spec VN gateways |
| **Used in** | KC `/billing/[id]/pay`, KH `/billing/upgrade` |
| **Persona** | All paying users |
| **Sub-components** | (a) Method radio cards (VNPay/MoMo/ZaloPay/Bank/Cash), (b) QR display panel (200×200), (c) Amount confirmation row (`Bạn sẽ thanh toán: 199.000đ`), (d) Trust marker strip, (e) Redirect notice |
| **States** | method-selecting → loading-qr → qr-displayed → expired (15 min) → checking → success → failure-retry |
| **VN UX** | See `02-vietnamese-ux-musts.md` §4. QR code dominant for MoMo/ZaloPay. VNPay = redirect notice. Bank = account info card with copy buttons. |
| **Reference flow** | Flow #5 |

### G6. Invoice Detail Panel

| Spec | Detail |
|------|--------|
| **Why missing** | Repo has `invoice-detail-panels.tsx` but design bundle didn't ship spec |
| **Used in** | KC `/billing/[id]`, KH `/billing/payment/[id]` (33/128 currently 🔴) |
| **Persona** | Owner/Admin/Student/Parent |
| **Sub-components** | (a) Invoice header (number, status pill, issue date, due date), (b) Line items (course name, qty, unit price, line total), (c) Discount/scholarship breakdown, (d) VAT row (if applicable), (e) Total + balance due, (f) Action buttons (Pay / Download PDF / Email) |
| **States** | loading → pending-payment → partial-paid (installment) → paid → overdue (red banner) → void |
| **VN UX** | VN format: `Số HĐ: KH-2026-04-001`. VN tax format if invoice-VAT. Date `dd/MM/yyyy`. |

### G7. Parent Invite Flow Card

| Spec | Detail |
|------|--------|
| **Why missing** | Repo has `parent-invite-form.tsx` but no design system version |
| **Used in** | KC `/parent-invite/[token]` (redemption), admin "invite parent" modal |
| **Persona** | Pa. Parent (redeemer), Admin (sender) |
| **Sub-components** | (a) Email input + send button, (b) Pending invites list (resend / cancel), (c) Token expiry countdown (24h), (d) Redemption form (parent creates account) |
| **States** | sending → sent → expired-link → already-redeemed → success-account-created |
| **VN UX** | Welcome message: `Bạn được mời theo dõi học tập của con [tên học sinh]` |

### G8. Attendance Calendar (Teacher Month-View)

| Spec | Detail |
|------|--------|
| **Why missing** | shadcn `calendar.tsx` exists but generic; need teacher-specific overlay |
| **Used in** | KC `/attendance` overview, `/classes/[id]/attendance` history |
| **Persona** | Teacher / Owner |
| **Sub-components** | (a) Month grid (5×7), (b) Day cell with session count + attendance rate %, (c) Color heatmap (red <80%, amber 80-95%, green ≥95%), (d) Click → navigate to that day's roster |
| **States** | loading → loaded → no-sessions → past-locked (read-only) |
| **VN UX** | Monday-first week. Vietnamese holiday markers. |

### G9. Instance Lifecycle Status

| Spec | Detail |
|------|--------|
| **Why missing** | Bundle has wizard but no run-state visualization. KH `/instances/[id]` is 33/128 🔴 |
| **Used in** | KH `/instances/[id]`, KH `/admin/instances/[id]` |
| **Persona** | P2 Owner (waiting for provisioning), Internal admin (debugging) |
| **Sub-components** | (a) State timeline (NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED with optional FAILED branch), (b) Per-step status (pending/active/done/error), (c) Live progress (SSE-driven), (d) Retry button on FAILED, (e) Logs panel (admin only) |
| **States** | All 6 states must render: NOT_STARTED, INITIALIZING, GENERATING, DEPLOYED, FAILED, REGENERATING |
| **VN UX** | Friendly copy: `Đang tạo trang web cho trung tâm của bạn... ⏳ ~30 giây`. Error: `Có lỗi xảy ra. Đội ngũ kỹ thuật đã được thông báo.` |
| **Reference** | `documents/01-business/kitehub/instance-provisioning/use-cases.md` |

### G10. Payment Status Timeline

| Spec | Detail |
|------|--------|
| **Why missing** | Implicit in admin payments table; no dedicated timeline |
| **Used in** | KH `/admin/payments`, KC `/billing/[id]` |
| **Persona** | Internal admin, Owner |
| **Sub-components** | (a) Vertical timeline (Invoice → Pending → Processing → Paid/Failed → Refunded), (b) Per-event timestamp + actor + note, (c) Retry button on failed |
| **States** | All event combinations |

### G11. Theme Customization Live Preview

| Spec | Detail |
|------|--------|
| **Why missing** | Bundle had AI Branding playground but Round 2 needs integrated wizard step |
| **Used in** | KH `/branding/wizard` step 6 (Preview-approve) |
| **Persona** | P2 Center Owner |
| **Sub-components** | (a) Color picker (HSL or HEX), (b) Font heading/body picker (3 preset combos), (c) Border radius slider, (d) Live preview iframe (left = preview, right = controls), (e) Per-resource approve toggle (logo / colors / banner / hero) |
| **States** | unsaved-changes → saving → saved → reset-to-default-confirm → quality-gate-failed (<70 score) |
| **VN UX** | Resource labels: `Logo`, `Bảng màu`, `Banner trang chủ`, `Hero` |
| **Reference rule** | `.claude/rules/ai-branding-guidelines.md` §4.2 (Preview before commit) |

### G12. Student List with Bulk Actions

| Spec | Detail |
|------|--------|
| **Why missing** | Repo has table but no design spec for bulk-action bar pattern |
| **Used in** | KC `/students`, KC `/teachers`, KH `/admin/instances` |
| **Persona** | Owner/Admin |
| **Sub-components** | (a) Header with search + filters (status, class, enrollment date), (b) Checkbox column (select all / per row), (c) Sticky bulk-action bar when ≥1 selected (Export / Archive / Assign to class / Delete), (d) Pagination (50/100/200 per page), (e) Empty state with CTA |
| **States** | empty (CTA: bulk import OR add manually) → loaded → selecting → bulk-confirm-dialog → action-running → action-done |
| **VN UX** | Empty state copy: `Chưa có học sinh nào. [Nhập danh sách từ Excel] hoặc [Thêm thủ công]`. Sort by surname (Vietnamese name convention). |

---

## Component-vs-screen mapping

How the 12 components compose into priority screens (`03-screen-inventory.md`):

| Component | Used in screens |
|-----------|----------------|
| G1 Bulk Import | KC `/students`, KH `/admin/instances` (bulk tenant import) |
| G2 Attendance Roster | KC `/classes/[id]/attendance` |
| G3 Gradebook | KC `/classes/[id]/grades` (new) |
| G4 Schedule Manager | KC `/classes/[id]/schedule` (new) |
| G5 Payment Method Selector | KC `/billing/[id]/pay`, KH `/billing/upgrade` |
| G6 Invoice Detail | KC `/billing/[id]`, KH `/billing/payment/[id]` |
| G7 Parent Invite | KC `/parent-invite/[token]`, admin invite modal |
| G8 Attendance Calendar | KC `/attendance`, KC `/classes/[id]/attendance` |
| G9 Instance Lifecycle | KH `/instances/[id]`, KH `/admin/instances/[id]` |
| G10 Payment Timeline | KH `/admin/payments`, KC `/billing/[id]` |
| G11 Theme Live Preview | KH `/branding/wizard` step 6 |
| G12 Bulk Actions Bar | KC `/students`, KC `/teachers`, KH `/admin/instances`, KH `/admin/payments` |

---

## What this list does NOT include

These are intentionally OUT of Round 2 scope:

- Charts beyond sparkline (recharts already in KH; Round 2 designs use it as-is)
- Rich-text editor (course description, blog) — separate vendor decision
- Video player (lessons) — out of MVP scope
- File upload generic — shadcn pattern reused, no custom design needed
- Notification bell + center — defer to Round 3
- Search global / command palette — Direction B kiteclass-pro covers it
