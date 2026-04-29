# G10 — Payment Status Timeline

**Component gap:** G10 per `documents/02-architecture/design-system/dossier/04-component-gaps.md` §G10
**Flow ref:** `dossier/05-business-flows.md` Flow #5 (VN multi-gateway payment + dispute)
**Used by screens:** KH `/admin/payments`, KC `/billing/[id]`
**Persona:** Pa. Parent (child tuition history), P2 Owner / Admin (payment ops)

---

## Purpose

Visualize the lifecycle of a payment record (issued → pending → paid / partial-paid / overdue / refunded). Replaces implicit status chips in `/admin/payments` table with a dedicated timeline showing **who did what when**, plus actionable CTAs for each state (Pay now, Download receipt, Contact support).

---

## States

| File | State | UI summary |
|------|-------|-----------|
| `states/pending.html` | `Pending` | Due-date countdown (5 days), reminder schedule, "Thanh toán ngay" CTA |
| `states/paid.html` | `Paid` | Full payment success hero + transaction details + receipt download |
| `states/partial-paid.html` | `Partial paid` | 60% progress bar + 2 installments breakdown + remaining-balance CTA |
| `states/overdue.html` | `Overdue` | 8 days late + 5% late fee transparently itemized + escalation timeline |
| `states/refunded.html` | `Refunded` | Refund reason + approver + replacement invoice link |

---

## Vietnamese UX

- **Currency format:** `1.500.000đ` (dot thousand separator + suffix `đ`, NOT VND or ₫)
- **Late fee:** transparent itemization — `Phí trễ (5% × 1 chu kỳ) +75.000đ` BEFORE total
- **Reminder copy:** `Đã gửi nhắc nhở qua Zalo OA (1 lần). Sẽ gửi tiếp nếu còn 3 ngày.`
- **Date format:** `dd/MM/yyyy HH:mm` — `14/10/2026 19:42`
- **Refund reason:** narrative ("Chuyển sang lớp học khác (10A1)") not codes
- **Invoice numbering:** `KC-2026-10-0042` per existing convention
- **Refund mã giao dịch:** `REF-YYYY-MMDD-{12char}` (separate prefix from VNPay)

---

## Timeline pattern

Every state shows the same timeline anchor (Phát hành hóa đơn) and adds events:

```
Issued -> [Reminders] -> Pending / Paid / Partial / Overdue -> [Refund?]
```

Each event has: timestamp (font-mono), actor (system or human), note (1 line). `aria-current="step"` marks current state. Future events are dimmed (`opacity-50`).

---

## Accessibility

- Status chips: `role="status"` + icon + text (color is NOT only signal)
- Overdue alert: `role="alert"` + `aria-live="polite"`
- Progress bar (partial-paid): `role="progressbar"` + `aria-valuenow/min/max` + label
- Timeline: `<ol>` with `aria-label="Timeline trạng thái thanh toán"` + `aria-current="step"` on active
- All CTAs have visible focus indicator + ≥4.5:1 contrast
- Currency reads naturally with screen reader (`đ` suffix, dot decimals)

---

## Reuse

- shadcn `Button` (default + destructive variants for overdue)
- shadcn `Badge` for state pills
- lucide icons: `arrow-left`, `download`, `mail`, `refresh-cw`, `alert-triangle`, `check-circle`
- Receipt PDF generation — `kitehub-billing/src/main/java/.../InvoicePdfService` (existing pattern)
- VNPay IPN webhook log — feeds timeline `payment.confirmed` event via outbox

---

## Self-score

| State | Score |
|-------|------:|
| `pending.html` | 107/128 |
| `paid.html` | 110/128 |
| `partial-paid.html` | 106/128 |
| `overdue.html` | 108/128 |
| `refunded.html` | 105/128 |
| `index.html` (showcase) | 108/128 |
| **Average** | **~107/128** |

All states ≥105/128.

---

## Acceptance criteria

- [x] All 5 states demonstrate distinct visual + textual treatment
- [x] VN currency format `1.500.000đ` everywhere (no VND or USD)
- [x] Late fee itemized BEFORE total (transparency for parents)
- [x] Refund state shows reason + approver + replacement invoice link
- [x] Timeline pattern consistent across all states
- [x] WCAG AA contrast measured + commented
- [x] Vietnamese-only content; informal "bạn" tone for parents
- [x] `role="status"` / `role="alert"` per state semantic level
