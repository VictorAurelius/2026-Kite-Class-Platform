# G6 — Invoice Detail Panel

**Component gap:** G6 per `dossier/04-component-gaps.md` §G6
**Flow ref:** `dossier/05-business-flows.md` Flow #5 (Payment & invoice)
**Used by screens:** KC `/billing/[id]`, KH `/billing/payment/[id]` (currently 33/128 🔴 — main reason for redesign)
**Persona:** Owner / Admin / Student / Parent

---

## Purpose

Show invoice detail with all VN-specific fields: invoice number, status, line items, discount/scholarship, VAT (if applicable), total + balance, action buttons. Variants for `pending-payment`, `paid`, `overdue`, plus `print-view` (A4 portrait, no nav, optionally VN tax invoice format).

Replaces 33/128 KH `/billing/payment/[id]` baseline.

---

## Props (TypeScript-ish)

```ts
interface InvoiceDetailProps {
  invoice: {
    number: string;            // "KH-2026-04-001"
    status: "PENDING_PAYMENT" | "PARTIAL_PAID" | "PAID" | "OVERDUE" | "VOID";
    issueDate: Date;           // 2026-04-01
    dueDate: Date;             // 2026-04-15
    paidDate?: Date;
    items: Array<{
      title: string;            // "Khóa luyện thi THPT Quốc gia 2026 - Toán"
      quantity: number;
      unitPrice: number;        // 4500000
      lineTotal: number;
      meta?: string;            // "Học kỳ I (Sept 2025 - Jan 2026)"
    }>;
    discounts: Array<{ label: string; amount: number }>;  // [{ label: "Giảm giá anh chị em", amount: 200000 }]
    vat?: { rate: number; amount: number };                // { rate: 0.08, amount: 344000 }
    subtotal: number;
    total: number;
    balance: number;            // 0 if paid
    student: { fullName: string; className: string };  // "Lê Minh Tuấn (Lớp 12A1)"
    tenant: { name: string; address: string; mst?: string };  // "Trung tâm Toán Master"
    isVATInvoice?: boolean;     // true → VN tax invoice format
  };
  state: "loading" | "default" | "pending" | "overdue" | "paid" | "print-view";
  onPayNow?: () => void;
  onDownloadPdf?: () => void;
  onSendEmail?: () => void;
}
```

---

## States

| State | When | UI |
|-------|------|-----|
| `loading` | Fetching invoice | Skeleton (header + 3 line items + total row) |
| `default` (`pending-payment`) | Status = PENDING | Status pill **Chờ thanh toán** + "Thanh toán ngay" CTA |
| `paid` | Status = PAID | Status pill **Đã thanh toán** + "Tải biên lai" + "Gửi qua email" |
| `overdue` | dueDate < today AND status != PAID | Red banner "Quá hạn 5 ngày" + late fee row + "Thanh toán ngay" |
| `print-view` | Print/PDF version | A4 portrait, no nav, no buttons, includes VN tax invoice format if `isVATInvoice` |

---

## Vietnamese UX considerations

Per `dossier/02-vietnamese-ux-musts.md`:

- Invoice number format: `Số HĐ: KH-2026-04-001` (`KH` prefix for KiteClass, `KHB` for KiteHub billing)
- Date format: `15/04/2026` short, `15 tháng 4 năm 2026` long for tax invoice
- Currency: `4.500.000đ` lowercase đ
- Tax invoice (`isVATInvoice=true`) requires:
  - Header: `HÓA ĐƠN GIÁ TRỊ GIA TĂNG` (red, centered) + `Mẫu số: 01GTKT0/001` + `Ký hiệu: KH/26E`
  - Buyer + seller MST blocks
  - Sub-total, VAT rate (8% or 10%), VAT amount, total in words
  - Footer: signatures + stamp area
  - Per Nghị định 123/2020/NĐ-CP (e-invoice)
- Status pill copy:
  - PENDING_PAYMENT: `Chờ thanh toán`
  - PARTIAL_PAID: `Trả góp đợt 1/3` (with progress)
  - PAID: `Đã thanh toán`
  - OVERDUE: `Quá hạn`
  - VOID: `Đã hủy`
- Sentence case for headings: `Chi tiết hóa đơn`
- Discounts/scholarship row: `Học bổng khuyến học (-200.000đ)`
- Late fee row: `Phí trễ hạn (5 ngày × 50.000đ)` = `250.000đ`
- Use `bạn` informal you in CTAs

---

## Print view rules

- A4 portrait (210 × 297 mm) → CSS `@page { size: A4 portrait; margin: 18mm 16mm; }`
- Hide nav header + footer + action buttons via `@media print`
- Strip background colors except status pill + section dividers
- Page break: `page-break-inside: avoid` on line item rows
- Print fonts: Inter regular only (no monospace icon fonts that print poorly)
- VN tax invoice: 2-column header, full address blocks, signature row, watermark `LIÊN 2: GIAO KHÁCH HÀNG`

---

## Accessibility

- `<table>` for line items with `<th scope="col">` headers
- Status pill: `role="status"` + icon + text
- Currency cells right-aligned + `tabular-nums` for visual scan
- Print view contrast ≥4.5:1 in greyscale
- Skip-to-actions link for screen reader users

---

## Component reuse

- shadcn `Badge` for status pill (variants: success, warning, destructive, info, secondary)
- shadcn `Button` (primary CTA, outline secondary, ghost tertiary)
- lucide icons: `FileText`, `Download`, `Mail`, `Printer`, `Check`, `Clock`, `AlertTriangle`, `Copy`
- date-fns `format(date, 'dd/MM/yyyy', { locale: vi })`
- For print view: separate `print.css` import or inline `@media print` rules
