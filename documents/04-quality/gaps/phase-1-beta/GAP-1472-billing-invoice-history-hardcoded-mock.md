# GAP-1472: KH-3 billing — "Lịch sử hóa đơn" hardcoded MOCK_INVOICES, thanh toán thật không hiển thị

**Status:** 🟡 PARTIAL (code shipped — Option A wire-to-real; chờ human G2 walk verify live)
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-17 (KH-3 G2 billing walk)
**Affects:** kitehub-frontend `(customer)/billing/page.tsx` (+ `types/payment.ts`)

## Problem

Trang billing của Owner (`/billing`) render khối "Lịch sử hóa đơn" từ một mảng
**hardcoded** `MOCK_INVOICES` (đặt ngay trong `page.tsx`) thay vì dữ liệu thật.
Các hàng giả này (`KHB-2026-04-001` / `499.000đ` / gói "PRO" / "3 trung tâm") có
nhiều vấn đề:

1. **Không khớp pricing thật** — gói thật là FREE/PREMIUM (PREMIUM = 1.500.000đ),
   không có gói "PRO" và không có giá 499.000đ.
2. **Không tải/thanh toán được** — số hóa đơn `KHB-` là bịa, không trỏ tới giao
   dịch thật nào.
3. **Thanh toán thật KHÔNG xuất hiện** — quan trọng nhất: một payment đã
   COMPLETED thật ở backend cũng không hiện ở đây, vì FE chưa bao giờ được nối
   vào dữ liệu thật. Backend ĐÃ có endpoint
   `GET /api/platform/payments/subscription/{subscriptionId}` →
   `List<PaymentResponse>` (`@PreAuthorize` OWNER_OR_STAFF) — chỉ là FE chưa gọi.

KPI cards ("Còn phải thu" / "Đã thanh toán" / "Quá hạn") cũng tính từ mock nên
sai hoàn toàn.

## Proposed Fix

**Option A (beta-pragmatic — ĐÃ SHIP wave này):** nối "Lịch sử hóa đơn" vào dữ
liệu payment thật.
- Dùng hook đã có sẵn `usePaymentHistory(subscriptionId)` (gọi
  `GET /api/platform/payments/subscription/{id}`) — `endpoints.payments.bySubscription`
  đã tồn tại; hook đã tồn tại trong `hooks/use-payments.ts`.
- Lấy `subscriptionId = useActiveSubscription(instanceId).id`.
- Bỏ `MOCK_INVOICES` + helper mock. Map `PaymentResponse[]` thật vào list +
  detail + KPI:
  - Row: nhãn = `txnRef` (fallback `transactionId` / short id), ngày = `createdAt`
    (vi-VN), số tiền = `amountVnd` (formatVNCurrency), badge `status` thật.
  - Detail panel: panel real-payment đơn giản (status / số tiền / phương thức /
    mã giao dịch / nội dung CK / thông tin ngân hàng nếu có); nếu PENDING giữ
    nguyên link "Tiếp tục thanh toán" → `/billing/payment/[id]` (giữ QR linkage).
  - KPI: paid = Σ COMPLETED; outstanding = Σ PENDING; overdue = 0.
  - Empty state thân thiện "Chưa có hóa đơn nào"; có loading + error state.
- KHÔNG bịa field không có trong `PaymentResponse` (không tier/period/center-count,
  không số `KHB-`, không khối hóa đơn GTGT/VAT).

**Option B (DEFER — Phase 1.5):** sinh invoice entity thật + đánh số `KHB-` +
hóa đơn điện tử GTGT (Nghị định 123/2020) + PDF. Ngoài scope beta.

## Acceptance Criteria

- [ ] `MOCK_INVOICES` + helper mock bị xóa khỏi `billing/page.tsx`.
- [ ] List "Lịch sử hóa đơn" hiển thị `PaymentResponse[]` thật từ
      `GET /api/platform/payments/subscription/{id}`.
- [ ] KPI cards tính từ payment thật (paid = Σ COMPLETED; outstanding = Σ PENDING).
- [ ] Một payment COMPLETED thật xuất hiện trong list (verify live human G2 walk).
- [ ] Empty state khi chưa có payment (không crash, không show mock).
- [ ] Loading + error state xử lý đúng.
- [ ] Không bịa field ngoài `PaymentResponse`.
- [ ] `pnpm --filter kitehub-frontend build` + `lint` + test billing PASS.

## Degraded / hidden (documented)

- Khối "Tải PDF" / hóa đơn GTGT (VAT) của G6 `InvoiceDetail` **không dùng** ở
  panel real-payment vì `PaymentResponse` không có nguồn dữ liệu thật cho MST /
  học sinh / dòng VAT → bỏ qua thay vì bịa. Receipt thật
  (`GET /payments/{id}/receipt`, COMPLETED-only) để dành Option B.
- "Quá hạn" KPI = 0 cố định: `PaymentResponse` không expose field expiry nên
  không suy ra overdue được một cách trung thực.

## Related

- Discovered in: 2026-06-17 KH-3 G2 billing walk
- BE endpoint (no change): `PaymentController#getPaymentsBySubscription`
- Cross-flow mock-data sweep (cùng wave): `branding/page.tsx` MOCK_THEME (DEFER —
  Phase 1 AI branding mock-by-design) + `instances/_lifecycle-mock.ts` (DEFER —
  separate concern)
- Option B (full invoice gen) DEFERRED Phase 1.5
