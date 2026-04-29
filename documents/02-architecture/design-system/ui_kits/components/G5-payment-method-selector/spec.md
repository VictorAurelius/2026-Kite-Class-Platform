# G5 — Payment Method Selector (VN Multi-Gateway)

**Component gap:** G5 per `dossier/04-component-gaps.md` §G5
**Flow ref:** `dossier/05-business-flows.md` Flow #5 (Payment & invoice)
**Used by screens:** KC `/billing/[id]/pay`, KH `/billing/upgrade`
**Persona:** All paying users (P2 Owner, Student, Parent)

---

## Purpose

Pick a payment method, view amount confirmation, and execute payment. Replaces partial `payment-method-selector.tsx` repo pattern with a complete VN-aware UX: wallet-first ordering, QR-dominant for MoMo/ZaloPay, redirect notice for VNPay, copy-paste account info for Bank transfer, cash for offline-first centers.

Per `dossier/02-vietnamese-ux-musts.md` §4: VN payment is **wallet-first, not card-first**. UI reflects that.

---

## State machine

```
   ┌─ method-selecting ─┐
   │                    ▼
   │             [VNPay]──→ redirect-notice ──→ external gateway ──→ success | failure-retry
   │             [MoMo / ZaloPay]──→ loading-qr ──→ qr-displayed ──┬──→ success
   │                                                  │             └──→ expired (15min) ──→ method-selecting
   │             [Bank transfer]──→ bank-info ──→ "đã chuyển khoản" ──→ pending-reconcile ──→ success
   │             [Cash]──→ admin-only confirm ──────────────────────→ success
   └─ failure-retry ───────────────────────────────────→ method-selecting
```

---

## Props (TypeScript-ish)

```ts
interface PaymentMethodSelectorProps {
  invoice: {
    number: string;        // "KH-2026-04-001"
    courseName: string;    // "Khóa luyện thi THPT Quốc gia 2026 - Toán"
    amount: number;        // 1500000 (in VND, integer — no decimals)
    discount?: number;     // 100000
    expiresAt?: Date;      // QR expiry
  };
  availableMethods: PaymentMethod[];  // ordered: VNPay, MoMo, ZaloPay, Bank, Cash (typical)
  state: "method-selecting" | "loading-qr" | "qr-displayed" | "expired" | "checking" | "success" | "failure-retry";
  selectedMethod?: PaymentMethod;
  onSelectMethod: (m: PaymentMethod) => void;
  onConfirm: () => Promise<void>;
  onPaymentDone: () => Promise<void>;  // user clicks "Đã thanh toán xong"
}

type PaymentMethod = "VNPAY" | "MOMO" | "ZALOPAY" | "BANK" | "CASH";
```

---

## States

| State | When | Key UI |
|-------|------|--------|
| `default` (`method-selecting`) | First entry | 5 method cards, amount confirm row, primary "Xác nhận & thanh toán" CTA |
| `loading-qr` | After MoMo/ZaloPay selected | Spinner + "Đang tạo mã QR…" |
| `qr-displayed` | QR generated | 200×200 QR + countdown 14:32 + "Đã thanh toán xong" button + "Hủy giao dịch" |
| `expired` | 15 min passed | "Mã QR đã hết hạn" + "Tạo mã QR mới" CTA |
| `success` | Webhook confirmed | Confetti + "Thanh toán thành công" + receipt download CTA + invoice number |
| `failure-retry` | Gateway error | "Giao dịch thất bại" + reason + "Thử lại" CTA |

---

## Vietnamese UX considerations

Per `dossier/02-vietnamese-ux-musts.md` §4:

- Currency: `1.500.000đ` (lowercase đ, dot separator). Discount row: `-100.000đ` (early bird).
- Amount confirmation: `Bạn sẽ thanh toán: **1.500.000đ**`
- Method ordering (wallet-first): MoMo (35%) → VNPay (30%) → ZaloPay (10%) → Bank transfer (20%) → Cash (5%) — but display as `[VNPay] [MoMo] [ZaloPay] [Chuyển khoản] [Tiền mặt]` to match `dossier/02` §4 table order
- Method logos: distinct colors (sky blue gradient for VNPay, magenta for MoMo, blue square for ZaloPay)
- Trust strip: `✓ Bảo mật bởi VNPay/MoMo  ✓ Không lưu thông tin thẻ`
- VNPay redirect notice: `Bạn sẽ được chuyển đến VNPay để hoàn tất thanh toán`
- QR section: 200×200 minimum, 4mm quiet zone, instructions `Mở app ${method} → Quét mã → Xác nhận`
- Bank info: account number + account name + amount + transfer code, each with copy button (lucide `copy` icon → click → toast `Đã sao chép`)
- Cash: admin-only — show "Đánh dấu đã nhận tiền mặt" — student/parent see "Liên hệ trung tâm để thanh toán"
- Receipt: `Tải biên lai (.pdf)` after success
- Min payment validation: `100.000đ` minimum (mock validates client-side)
- Sentence case for headings: `Chọn phương thức thanh toán` (NOT `Choose Payment Method`)

---

## Accessibility

- Method radio cards: `role="radiogroup" aria-label="Phương thức thanh toán"`
- QR code: `<img alt="Mã QR thanh toán MoMo cho hóa đơn KH-2026-04-001, số tiền 1.500.000đ">`
- Countdown timer: `aria-live="polite"` updates every minute (NOT every second to avoid noise)
- Focus order: methods → amount confirm → CTA
- 4.5:1 minimum contrast on currency display (mono-spaced for visual scan)
- Trust markers icon + text pair (no color-only signal)

---

## Component reuse

- shadcn `RadioGroup` for method picker
- shadcn `Button` (primary for CTA, outline for "Hủy giao dịch")
- shadcn `Dialog` for cancel confirmation
- shadcn `Tooltip` for trust marker explanations
- lucide icons: `Wallet`, `QrCode`, `Building2`, `Banknote`, `CreditCard`, `Copy`, `CheckCircle2`, `XCircle`, `RefreshCw`
- date-fns `formatDistanceToNow(expiresAt, { locale: vi })` for "còn lại 14:32"
- QR code library: `qrcode.react` (port-time decision; HTML mock uses static SVG QR placeholder)
